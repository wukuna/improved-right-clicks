package com.improvedmenus;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.*;
import javax.inject.Inject;
import javax.swing.*;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(name="Improved Right-Clicks", description = "Tidy right-click menus with optional misclick protection, item rules, action colours, loot icons, player badges, extra-option indicators and saved profiles.", tags={"menu","bank","inventory","misclick","examine","color"})
public class ImprovedMenusPlugin extends Plugin
{
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private ImprovedMenusConfig config;
    @Inject private EventBus eventBus;
    @Inject private ButtonGuard buttons;
    @Inject private MenuEngine engine;
    @Inject private ProfileStore store;
    @Inject private RenameNotice renameNotice;
    @Inject private ClientToolbar toolbar;
    @Inject private OverlayManager overlays;
    @Inject private MenuPresentation presentation;
    @Inject private OptionCountOverlay optionCounts;
    @Inject private ItemIcons itemIcons;
    @Inject private LootFiltersCompatibility lootCompatibility;
    @Inject private LootFiltersMenuPresentation lootMenu;
    @Inject private PlayerMenuBadges playerBadges;
    @Inject private PlayerAccountBadges accountBadges;
    @Inject private ReleaseNotesNotice releaseNotes;
    private ImprovedMenusPanel panel;
    private NavigationButton navigation;
    private volatile boolean active;
    private Menu colorEditorMenu;
    private MenuEntry colorEditorEntry;
    private Menu colorEditorSubmenu;
    private long lootRevision;
    private long accountRevision;

    @Provides ImprovedMenusConfig provideConfig(ConfigManager manager) { return manager.getConfig(ImprovedMenusConfig.class); }
    @Override protected void startUp()
    {
        active=true;
        presentation.setItemIcons(itemIcons);
        if(itemIcons.filters!=null) { itemIcons.filters.reload(); }
        renameNotice.startUp(); releaseNotes.startUp();
        clientThread.invokeLater(() -> { if (active) { engine.reload(); lootCompatibility.refresh(); playerBadges.start(); accountBadges.reset(true); } });
        buttons.startUp(); eventBus.register(buttons); overlays.add(presentation); overlays.add(optionCounts); overlays.add(playerBadges); presentation.start();
        SwingUtilities.invokeLater(this::updateSidePanel);
    }
    @Override protected void shutDown()
    {
        active=false;
        renameNotice.shutDown(); releaseNotes.shutDown();
        eventBus.unregister(buttons); buttons.shutDown(); overlays.remove(presentation); overlays.remove(optionCounts); overlays.remove(playerBadges);
        clientThread.invoke(() -> { removeColorEditor(); playerBadges.shutdown(); accountBadges.reset(false); presentation.stop(); lootMenu.stop(); lootCompatibility.reset(); engine.clear(); }); store.clearHistory();
        SwingUtilities.invokeLater(() -> {
            if (panel != null) { panel.stop(); panel=null; }
            if (navigation != null) { toolbar.removeNavigation(navigation); navigation=null; }
        });
    }
    @Subscribe(priority=-0.5f) public void onPostMenuSort(PostMenuSort event)
    { if (!client.isMenuOpen()) { engine.apply(client.getMenu()); } }
    @Subscribe(priority=-1) public void onClientTick(ClientTick event)
    {
        if (!active || client.getGameState() != GameState.LOGGED_IN) return;
        accountBadges.tick();
        long revision = accountBadges.revision();
        if (revision != accountRevision)
        {
            accountRevision = revision;
            if (client.isMenuOpen()) decorateMenu();
        }
        if (!client.isMenuOpen()) engine.applyTargetProtections(client.getMenu());
    }
    @Subscribe public void onMenuOptionClicked(MenuOptionClicked event)
    {
        if (!active || event.getMenuAction() != MenuAction.RUNELITE_PLAYER || !"Lookup".equals(event.getMenuOption())) return;
        Player player = event.getMenuEntry().getPlayer();
        if (player != null) accountBadges.selected(player.getName());
    }
    @Subscribe public void onGameTick(GameTick event)
    {
        if (!active) return;
        lootCompatibility.refresh();
        long revision=lootCompatibility.revision();
        if (revision!=lootRevision)
        {
            lootRevision=revision;
            if (client.isMenuOpen()) decorateMenu();
        }
    }
    @Subscribe(priority=-2) public void onMenuOpened(MenuOpened event)
    {
        playerBadges.clear(); presentation.stop(); lootMenu.stop();
        engine.applyOpened(client.getMenu());
        decorateMenu();
        if (config.colorPicker() && client.isKeyPressed(KeyCode.KC_SHIFT)) { addColorEditor(client.getMenu()); }
    }
    @Subscribe public void onConfigChanged(ConfigChanged event)
    {
        if (ImprovedMenusConfig.GROUP.equals(event.getGroup()))
        {
            if (PlayerBadgeCache.KEY.equals(event.getKey())) return;
            if(itemIcons.filters!=null && (event.getKey().equals(FilterIcons.KEY) || event.getKey().equals(FilterIcons.BACKUP))) { itemIcons.filters.reload(); }
            clientThread.invokeLater(() -> { if (active) { accountBadges.reset(true); playerBadges.clear(); presentation.stop(); lootMenu.stop(); lootCompatibility.reset(); lootCompatibility.refresh(); engine.reload(); if(client.isMenuOpen()) decorateMenu(); } });
            if (event.getKey().equals("hideSidePanel")) { SwingUtilities.invokeLater(this::updateSidePanel); }
        }
        else if ("loot-filters".equals(event.getGroup())) refreshLootFilter();
    }
    @Subscribe public void onProfileChanged(net.runelite.client.events.ProfileChanged event) { clientThread.invokeLater(() -> { if (active) { playerBadges.clear(); accountBadges.reset(true); releaseNotes.showIfLoggedIn(); } }); refreshLootFilter(); if(itemIcons.filters!=null) { itemIcons.filters.reload(); } }
    @Subscribe public void onPluginChanged(net.runelite.client.events.PluginChanged event)
    { if ("Loot Filters".equals(event.getPlugin().getName())) refreshLootFilter(); }
    @Subscribe public void onPluginMessage(net.runelite.client.events.PluginMessage event)
    { if ("loot-filters".equals(event.getNamespace()) && "filter-reloaded".equals(event.getName())) refreshLootFilter(); }
    private void refreshLootFilter()
    {
        clientThread.invokeLater(() -> {
            if (!active) return;
            playerBadges.clear(); presentation.stop(); lootMenu.stop(); lootCompatibility.reset(); lootCompatibility.refresh();
            if (client.isMenuOpen()) decorateMenu();
        });
    }
    private void decorateMenu() {
        // Unwind in reverse order before reapplying asynchronously refreshed icons/colors.
        playerBadges.clear(); presentation.stop(); lootMenu.stop();
        lootCompatibility.beginMenu();
        try { lootMenu.apply(client.getMenu()); presentation.decorate(client.getMenu()); playerBadges.decorate(client.getMenu()); }
        finally { lootCompatibility.endMenu(); }
    }
    @Subscribe public void onBeforeMenuRender(BeforeMenuRender event)
    { if (active && client.isMenuOpen()) { engine.highlightHovered(client.getMenu()); } }
    private void updateSidePanel()
    {
        if (!active || config.hideSidePanel())
        {
            if (navigation != null) { toolbar.removeNavigation(navigation); navigation=null; }
            if (panel != null) { panel.stop(); panel=null; }
        }
        else if (navigation == null)
        {
            panel=new ImprovedMenusPanel(store,engine,this::openSettings,lootCompatibility::status);
            navigation=NavigationButton.builder().tooltip("Improved Right-Clicks").icon(icon()).priority(6).panel(panel).build();
            toolbar.addNavigation(navigation);
        }
    }
    private void openSettings()
    {
        net.runelite.client.ui.overlay.Overlay target = new net.runelite.client.ui.overlay.Overlay(this) {
            @Override public java.awt.Dimension render(Graphics2D graphics) { return null; }
        };
        eventBus.post(new net.runelite.client.events.OverlayMenuClicked(
            new net.runelite.client.ui.overlay.OverlayMenuEntry(MenuAction.RUNELITE_OVERLAY_CONFIG,
                net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE,"Improved Right-Clicks"),target));
    }
    @Subscribe public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING || event.getGameState() == GameState.CONNECTION_LOST) accountBadges.reset(active);
        if (event.getGameState() != GameState.LOGGED_IN) { playerBadges.clear(); presentation.stop(); lootMenu.stop(); lootCompatibility.reset(); buttons.reset(); engine.clear(); }
        else { renameNotice.showIfLoggedIn(); releaseNotes.showIfLoggedIn(); }
    }
    @Subscribe(priority=100) public void onChatMessage(ChatMessage event)
    { if (active) accountBadges.observe(event); }
    @Subscribe public void onFocusChanged(FocusChanged event) { if (!event.isFocused()) { clientThread.invoke(buttons::reset); } }

    private void addColorEditor(Menu menu)
    {
        removeColorEditor();
        // Local UI callbacks only. Game actions are never replayed.
        Set<String> seen = new LinkedHashSet<>();
        java.util.List<String[]> choices = new ArrayList<>();
        collect(menu,"",seen,choices,0);
        if (choices.isEmpty()) { return; }
        colorEditorMenu=menu;
        colorEditorEntry=menu.createMenuEntry(0).setOption("Menu colors").setTarget("").setType(MenuAction.RUNELITE);
        Menu sub=colorEditorEntry.createSubMenu(); colorEditorSubmenu=sub;
        for (String[] choice : choices)
        {
            String target=choice[0], option=choice[1];
            sub.createMenuEntry(-1).setOption(option).setTarget(target).setType(MenuAction.RUNELITE).onClick(e -> {
                SwingUtilities.invokeLater(() -> {
                    if (!active) { return; }
                    String old=engine.color(target,option);
                    Color color=JColorChooser.showDialog(panel,"Color " + option + " " + target,old == null ? SettingsCatalog.colorOrDefault(config.defaultPickerColor(),Color.GREEN) : new Color(Integer.parseInt(old,16)));
                    if (color != null && active) { saveColor(target,option,String.format("#%06x",color.getRGB() & 0xffffff)); }
                });
            });
            sub.createMenuEntry(-1).setOption("Reset " + option).setTarget(target).setType(MenuAction.RUNELITE).onClick(e -> saveColor(target,option,null));
        }
    }
    private void removeColorEditor()
    {
        if (colorEditorMenu != null && colorEditorEntry != null && colorEditorEntry.getSubMenu() == colorEditorSubmenu
            && colorEditorEntry.getType() == MenuAction.RUNELITE && colorEditorEntry.getOption().equals("Menu colors"))
        {
            MenuEntry[] entries=colorEditorMenu.getMenuEntries();
            MenuEntry[] kept=Arrays.stream(entries).filter(e -> e != colorEditorEntry).toArray(MenuEntry[]::new);
            if (kept.length != entries.length) { colorEditorMenu.setMenuEntries(kept); }
        }
        colorEditorMenu=null; colorEditorEntry=null; colorEditorSubmenu=null;
    }
    private void collect(Menu menu,String inherited,Set<String> seen,java.util.List<String[]> choices,int depth)
    {
        if (depth > 8) { return; }
        for (MenuEntry entry : menu.getMenuEntries())
        {
            String target=MenuRules.plain(entry.getTarget()); if (target.isEmpty()) { target=inherited; }
            String option=MenuRules.plain(entry.getOption());
            if (entry.getSubMenu() != null) { collect(entry.getSubMenu(),target,seen,choices,depth+1); }
            else if (!target.isEmpty() && !target.contains(":") && !target.contains(";") && !target.contains("*")
                && !option.contains(":") && !option.contains(";") && !option.contains("*") && entry.getType() != MenuAction.CANCEL
                && choices.size() < 40 && seen.add(target + "\n" + option)) { choices.add(new String[]{target,option}); }
        }
    }
    private void saveColor(String target,String option,String color)
    {
        if (!active) { return; }
        String prefix=target + ":" + option + ":";
        java.util.List<String> values = new ArrayList<>();
        for (String raw : config.colors().split(";"))
        { if (!raw.trim().isEmpty() && !raw.trim().regionMatches(true,0,prefix,0,prefix.length())) { values.add(raw.trim()); } }
        if (color != null) { values.add(prefix + color); }
        try { store.apply(Collections.singletonMap("colors",String.join(";",values))); }
        catch (IllegalArgumentException e) { SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(panel,e.getMessage(),"Menu colors",JOptionPane.ERROR_MESSAGE)); }
    }
    private static BufferedImage icon()
    {
        BufferedImage image=new BufferedImage(48,48,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=image.createGraphics(); g.setColor(new Color(38,42,49)); g.fillRoundRect(2,2,44,44,10,10);
        g.setColor(new Color(223,174,65)); for (int y=12;y<40;y+=10) { g.fillRoundRect(10,y,28,4,3,3); } g.dispose(); return image;
    }
}
