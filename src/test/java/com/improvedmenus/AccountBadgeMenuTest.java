package com.improvedmenus;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.*;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.game.ChatIconManager;
import net.runelite.client.party.PartyService;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AccountBadgeMenuTest
{
    private static class Fixture
    {
        final Client client = mock(Client.class);
        final ImprovedMenusConfig config = mock(ImprovedMenusConfig.class);
        final ChatIconManager icons = mock(ChatIconManager.class);
        final TooltipManager tooltips = mock(TooltipManager.class);
        final PlayerMenuBadges badges = new PlayerMenuBadges(client, config, icons, mock(PartyService.class), tooltips);
        final IndexedSprite[] sprites = new IndexedSprite[150];
        final MenuEntry entry = TestMenus.entry("Follow", "<col=ffffff>Alice</col> (level-126)", MenuAction.PLAYER_SECOND_OPTION, -1, 0);
        final Menu menu = TestMenus.menu(entry);
        Fixture()
        {
            Player player = mock(Player.class); when(player.getName()).thenReturn("Alice"); when(entry.getPlayer()).thenReturn(player);
            when(config.playerAccountBadge()).thenReturn(true);
            for (ObservedAccountBadges.Badge badge : ObservedAccountBadges.Badge.values()) sprites[badge.icon.getIndex()] = mock(IndexedSprite.class);
            when(client.getModIcons()).thenReturn(sprites); when(client.getMenu()).thenReturn(menu);
            when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
            badges.start();
        }
        ChatMessage chat(String name) { return new ChatMessage(null, ChatMessageType.PUBLICCHAT, name, "Hello", "", 0); }
        void observe(String name) { badges.observeAccountBadge(chat(name)); }
    }

    @Test public void nativeBadgesPreservePlayerActionsAndRestoreWithoutDuplicates()
    {
        Fixture f = new Fixture(); String original = f.entry.getTarget();
        for (ObservedAccountBadges.Badge badge : ObservedAccountBadges.Badge.values())
        {
            f.observe(badge.icon + "Alice"); f.badges.decorate(f.menu);
            assertEquals(badge.icon + original, f.entry.getTarget());
            f.badges.decorate(f.menu); assertEquals(badge.icon + original, f.entry.getTarget());
            f.badges.clear(); assertEquals(original, f.entry.getTarget());
        }
        verify(f.menu, never()).setMenuEntries(any());
        verify(f.menu, never()).createMenuEntry(anyInt());
        verify(f.entry, never()).setType(any()); verify(f.entry, never()).setOption(anyString());
        verify(f.entry, never()).setIdentifier(anyInt()); verify(f.entry, never()).setParam0(anyInt());
        verify(f.entry, never()).setParam1(anyInt()); verify(f.entry, never()).setDeprioritized(anyBoolean());
        verify(f.icons, times(4)).registerChatIcon(any());
    }

    @Test public void unknownDisabledAndUnavailableBadgesStayUnmarked()
    {
        Fixture f = new Fixture(); String original = f.entry.getTarget();
        f.badges.decorate(f.menu); assertEquals(original, f.entry.getTarget());
        when(f.config.playerAccountBadge()).thenReturn(false); f.observe("<img=2>Alice");
        when(f.config.playerAccountBadge()).thenReturn(true); f.badges.decorate(f.menu); assertEquals(original, f.entry.getTarget());
        f.observe("<img=2>Alice"); when(f.client.getModIcons()).thenReturn(null);
        f.badges.decorate(f.menu); assertEquals(original, f.entry.getTarget());
        when(f.client.getModIcons()).thenReturn(new IndexedSprite[1]);
        f.badges.decorate(f.menu); assertEquals(original, f.entry.getTarget());
        when(f.client.getModIcons()).thenReturn(f.sprites); f.badges.decorate(f.menu);
        assertEquals("<img=2>" + original, f.entry.getTarget());
        when(f.config.playerAccountBadge()).thenReturn(false); f.badges.decorate(f.menu); assertEquals(original, f.entry.getTarget());
    }

    @Test public void existingHelmetsCrownsAndClanRankIconsArePreserved()
    {
        Fixture f = new Fixture(); f.observe("<img=10>Alice");
        for (String target : new String[]{"<img=10>Alice", "<img=0>Alice", "<img=2><img=70>Alice"})
        {
            f.entry.setTarget(target); f.badges.decorate(f.menu); assertEquals(target, f.entry.getTarget());
        }
        f.entry.setTarget("<img=70>Alice"); f.badges.decorate(f.menu); assertEquals("<img=10><img=70>Alice", f.entry.getTarget());
    }

    @Test public void equivalentIconsRegisteredByOtherPluginsAreNotDuplicated()
    {
        Fixture f = new Fixture();
        IndexedSprite nativeIcon = f.sprites[10];
        when(nativeIcon.getWidth()).thenReturn(1); when(nativeIcon.getHeight()).thenReturn(1);
        when(nativeIcon.getPixels()).thenReturn(new byte[]{1}); when(nativeIcon.getPalette()).thenReturn(new int[]{0, 0xff0000});
        f.sprites[120] = nativeIcon;
        f.entry.setTarget("<img=120>Alice"); f.observe("<img=10>Alice"); f.badges.decorate(f.menu);
        assertEquals("<img=120>Alice", f.entry.getTarget());
    }

    @Test public void tooltipExplainsLastObservationAndShutdownPreservesTheCache()
    {
        Fixture f = new Fixture(); String original = f.entry.getTarget(); f.observe("<img=10>Alice");
        when(f.client.isMenuOpen()).thenReturn(true); when(f.client.getMouseCanvasPosition()).thenReturn(new Point(5, 23));
        when(f.menu.getMenuWidth()).thenReturn(300); when(f.menu.getMenuHeight()).thenReturn(40);
        f.badges.decorate(f.menu); f.badges.render(null);
        verify(f.tooltips).add(argThat(tooltip -> tooltip.getText().equals("Hardcore Ironman (last observed in chat)")));
        f.badges.shutdown(); assertEquals(original, f.entry.getTarget());
        f.badges.start(); f.badges.decorate(f.menu); assertEquals("<img=10>" + original, f.entry.getTarget());
    }

    @Test public void realEventBusPreservesObservationsAcrossSessionsProfilesAndSettingChanges() throws Exception
    {
        Fixture f = new Fixture(); String original = f.entry.getTarget();
        ImprovedMenusPlugin plugin = new ImprovedMenusPlugin();
        field(plugin, "client", f.client); field(plugin, "config", f.config); field(plugin, "playerBadges", f.badges); field(plugin, "active", true);
        field(plugin, "presentation", new MenuPresentation()); field(plugin, "lootMenu", new LootFiltersMenuPresentation());
        field(plugin, "lootCompatibility", mock(LootFiltersCompatibility.class)); field(plugin, "buttons", mock(ButtonGuard.class));
        ClientThread thread = mock(ClientThread.class); List<Runnable> queued = new ArrayList<>();
        MenuEngine engine = new MenuEngine(); field(engine, "config", f.config); field(plugin, "engine", engine);
        when(f.config.itemRules()).thenReturn(""); when(f.config.colors()).thenReturn(""); when(f.config.exceptions()).thenReturn("");
        net.runelite.client.config.ConfigManager manager = mock(net.runelite.client.config.ConfigManager.class);
        net.runelite.client.chat.ChatMessageManager messages = mock(net.runelite.client.chat.ChatMessageManager.class);
        field(plugin, "releaseNotes", new ReleaseNotesNotice(f.client, thread, manager, messages));
        field(plugin, "renameNotice", new RenameNotice(f.client, thread, manager, messages)); field(plugin, "itemIcons", mock(ItemIcons.class));
        doAnswer(call -> { queued.add(call.getArgument(0)); return null; }).when(thread).invokeLater(any(Runnable.class));
        field(plugin, "clientThread", thread);
        EventBus bus = new EventBus(error -> { throw new AssertionError(error); }); bus.register(plugin);
        for (GameState state : new GameState[]{GameState.HOPPING, GameState.LOGIN_SCREEN, GameState.CONNECTION_LOST})
        {
            bus.post(f.chat("<img=2>Alice")); f.badges.decorate(f.menu); assertEquals("<img=2>" + original, f.entry.getTarget());
            GameStateChanged change = new GameStateChanged(); change.setGameState(state); bus.post(change);
            f.badges.decorate(f.menu); assertEquals("<img=2>" + original, f.entry.getTarget());
        }
        bus.post(f.chat("<img=2>Alice"));
        GameStateChanged loading = new GameStateChanged(); loading.setGameState(GameState.LOADING); bus.post(loading);
        f.badges.decorate(f.menu); assertEquals("<img=2>" + original, f.entry.getTarget());
        bus.post(new ProfileChanged()); queued.forEach(Runnable::run); queued.clear();
        f.badges.decorate(f.menu); assertEquals("<img=2>" + original, f.entry.getTarget());
        bus.post(f.chat("<img=2>Alice")); when(f.config.playerAccountBadge()).thenReturn(false);
        ConfigChanged config = new ConfigChanged(); config.setGroup(ImprovedMenusConfig.GROUP); config.setKey("playerAccountBadge"); bus.post(config);
        queued.forEach(Runnable::run); queued.clear(); when(f.config.playerAccountBadge()).thenReturn(true);
        f.badges.decorate(f.menu); assertEquals("<img=2>" + original, f.entry.getTarget());
        field(plugin, "active", false); bus.post(f.chat("<img=10>Alice"));
        f.badges.decorate(f.menu); assertEquals("<img=2>" + original, f.entry.getTarget()); bus.unregister(plugin);
    }

    private static void field(Object object, String name, Object value) throws Exception
    {
        Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); field.set(object, value);
    }
}
