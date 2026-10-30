package com.improvedmenus;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.FriendsChatRank;
import net.runelite.api.IndexedSprite;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.client.game.ChatIconManager;
import net.runelite.client.party.PartyService;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

/** Adds cosmetic badges to opened player menus; never changes a native action or its priority. */
@Singleton
final class PlayerMenuBadges extends Overlay
{
    private final Client client;
    private final ImprovedMenusConfig config;
    private final ChatIconManager icons;
    private final PartyService party;
    private static final Pattern IMAGE = Pattern.compile("<img=(\\d+)>");
    private final TooltipManager tooltips;
    private final int[] registrations = {-1, -1, -1, -1};
    private final Map<MenuEntry, Edit> edits = new IdentityHashMap<>();
    private final Map<MenuEntry, String> copies = new IdentityHashMap<>();
    private Menu copyMenu;
    private boolean active;
    private int generation;
    Consumer<String> clipboard = PlayerMenuBadges::copyToClipboard;

    @Inject
    PlayerMenuBadges(Client client, ImprovedMenusConfig config, ChatIconManager icons,
        PartyService party, TooltipManager tooltips)
    {
        this.client = client;
        this.config = config;
        this.icons = icons;
        this.party = party;
        this.tooltips = tooltips;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
    }

    void start()
    {
        active = true;
        // ChatIconManager has no unregister method. Reuse these four registrations after re-enable.
        for (int i = 0; i < registrations.length; i++)
            if (registrations[i] == -1) registrations[i] = icons.registerChatIcon(image(i));
    }

    void shutdown() { active = false; clear(); }

    void clear()
    {
        generation++;
        edits.forEach((entry, edit) -> { if (edit.matches(entry)) entry.setTarget(edit.original); });
        edits.clear();
        if (copyMenu != null)
        {
            for (Map.Entry<MenuEntry, String> copy : copies.entrySet())
            {
                MenuEntry entry = copy.getKey();
                if (entry.getType() == MenuAction.RUNELITE && "Copy name".equals(entry.getOption())
                    && copy.getValue().equals(entry.getTarget())) copyMenu.removeMenuEntry(entry);
            }
        }
        copies.clear();
        copyMenu = null;
    }

    void decorate(Menu menu)
    {
        clear();
        if (!active || menu == null) return;
        Map<String, String> players = new LinkedHashMap<>();
        MenuEntry[] entries = menu.getMenuEntries();
        for (MenuEntry entry : entries)
        {
            Player player = entry.getPlayer();
            if (player == null || player.getName() == null || entry.getTarget() == null) continue;
            String name = MenuRules.plain(player.getName()).replace('\u00a0', ' ');
            String key = normalized(name);
            if (key.isEmpty()) continue;
            players.putIfAbsent(key, name);
            String target = entry.getTarget();
            StringBuilder prefix = new StringBuilder();
            List<String> labels = new ArrayList<>();
            if (config.playerFriendBadge() && player.isFriend()) add(0, "Friend", target, prefix, labels);
            if (config.playerIgnoreBadge() && client.getIgnoreContainer() != null
                && client.getIgnoreContainer().findByName(name) != null) add(1, "Ignored", target, prefix, labels);
            if (config.playerFriendsChatBadge() && player.isFriendsChatMember() && !hasFriendsChatRank(target))
                add(2, "Friends Chat member", target, prefix, labels);
            if (config.playerPartyBadge() && party.isInParty() && party.getMemberByDisplayName(name) != null)
                add(3, "RuneLite party member", target, prefix, labels);
            if (prefix.length() == 0) continue;
            String decorated = prefix + target;
            edits.put(entry, new Edit(entry, target, decorated, String.join("<br>", labels)));
            entry.setTarget(decorated);
        }
        if (!config.playerCopyName()) return;
        copyMenu = menu;
        int callbackGeneration = generation;
        players.forEach((key, name) -> {
            boolean exists = Arrays.stream(entries).anyMatch(e ->
                ("Copy name".equalsIgnoreCase(MenuRules.plain(e.getOption()))
                    || "Copy player name".equalsIgnoreCase(MenuRules.plain(e.getOption())))
                    && key.equals(normalized(e.getTarget())));
            if (exists) return;
            MenuEntry copy = menu.createMenuEntry(0).setOption("Copy name").setTarget(name).setType(MenuAction.RUNELITE)
                .onClick(e -> { if (active && generation == callbackGeneration && config.playerCopyName()) clipboard.accept(name); });
            copies.put(copy, name);
        });
    }

    private void add(int kind, String label, String target, StringBuilder prefix, List<String> labels)
    {
        int index = icons.chatIconIndex(registrations[kind]);
        if (index < 0 || containsIcon(target, index)) return;
        prefix.append("<img=").append(index).append('>');
        labels.add(label);
    }

    private boolean hasFriendsChatRank(String target)
    {
        for (FriendsChatRank rank : FriendsChatRank.values())
        {
            if (rank != FriendsChatRank.UNRANKED)
            {
                int index = icons.getIconNumber(rank);
                if (index >= 0 && containsIcon(target, index)) return true;
            }
        }
        return false;
    }

    private static String normalized(String value)
    {
        if (value == null) return "";
        String name = MenuRules.plain(value).replace('\u00a0', ' ').replace('_', ' ').trim().toLowerCase(Locale.ROOT);
        return name.matches("[a-z0-9 -]{1,12}") ? name : "";
    }

    private boolean containsIcon(String target, int expected)
    {
        IndexedSprite[] sprites = client.getModIcons();
        Matcher matcher = IMAGE.matcher(target);
        while (matcher.find())
        {
            if (matcher.group(1).length() > 8) continue;
            int found = Integer.parseInt(matcher.group(1));
            if (found == expected) return true;
            // Other plugins may register the same image under a different index.
            if (sprites != null && found < sprites.length && expected < sprites.length
                && sameImage(sprites[found], sprites[expected])) return true;
        }
        return false;
    }

    static boolean sameImage(IndexedSprite a, IndexedSprite b)
    {
        if (a == null || b == null || a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()
            || a.getOffsetX() != b.getOffsetX() || a.getOffsetY() != b.getOffsetY()) return false;
        byte[] ap = a.getPixels(), bp = b.getPixels();
        int[] ac = a.getPalette(), bc = b.getPalette();
        if (ap == null || bp == null || ac == null || bc == null || ap.length == 0 || ap.length != bp.length) return false;
        boolean visible = false;
        for (int i = 0; i < ap.length; i++)
        {
            int ai = ap[i] & 255, bi = bp[i] & 255;
            if (ai >= ac.length || bi >= bc.length || (ai == 0) != (bi == 0) || (ai != 0 && ac[ai] != bc[bi])) return false;
            visible |= ai != 0;
        }
        return visible;
    }

    @Override public Dimension render(Graphics2D graphics)
    {
        if (!active || !client.isMenuOpen()) return null;
        Menu menu = client.getMenu();
        if (menu == null) return null;
        int x = client.getMouseCanvasPosition().getX(), y = client.getMouseCanvasPosition().getY();
        if (x < menu.getMenuX() || x >= menu.getMenuX() + menu.getMenuWidth()) return null;
        MenuEntry[] entries = menu.getMenuEntries();
        int scroll = client.isMenuScrollable() ? client.getMenuScroll() : 0;
        for (int i = 0; i < entries.length; i++)
        {
            int top = menu.getMenuY() + 19 + (entries.length - 1 - i - scroll) * 15;
            if (top < menu.getMenuY() + 19 || top + 15 > menu.getMenuY() + menu.getMenuHeight()) continue;
            Edit edit = edits.get(entries[i]);
            if (y >= top && y < top + 15 && edit != null && edit.matches(entries[i]))
                tooltips.add(new Tooltip(edit.label));
        }
        return null;
    }

    static BufferedImage image(int kind)
    {
        BufferedImage image = new BufferedImage(11, 11, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        Color dark = new Color(25, 35, 30);
        if (kind == 0)
        {
            g.setColor(dark); g.fillOval(0, 0, 11, 11);
            g.setColor(new Color(83, 215, 119)); g.fillOval(1, 1, 9, 9);
            g.setColor(dark); g.fillRect(3, 3, 1, 2); g.fillRect(7, 3, 1, 2);
            g.drawLine(3, 6, 4, 7); g.drawLine(4, 7, 6, 7); g.drawLine(6, 7, 7, 6);
        }
        else if (kind == 1)
        {
            g.setColor(dark); g.fillOval(0, 0, 11, 11);
            g.setColor(new Color(239, 112, 112)); g.fillOval(1, 1, 9, 9);
            g.setColor(dark); g.fillRect(3, 5, 5, 2);
        }
        else if (kind == 2)
        {
            g.setColor(dark); g.fillRect(0, 1, 11, 8); g.fillRect(2, 8, 3, 3);
            g.setColor(new Color(104, 211, 234)); g.fillRect(1, 2, 9, 6); g.fillRect(3, 8, 1, 2);
            g.setColor(dark); g.drawLine(3, 4, 7, 4); g.drawLine(3, 6, 6, 6);
        }
        else
        {
            g.setColor(dark); g.fillOval(0, 0, 6, 6); g.fillOval(5, 1, 6, 6); g.fillRect(0, 5, 11, 6);
            g.setColor(new Color(190, 150, 255)); g.fillOval(1, 1, 4, 4); g.fillOval(6, 2, 4, 4);
            g.fillRect(1, 6, 4, 4); g.fillRect(6, 7, 4, 3);
        }
        g.dispose();
        return image;
    }

    private static void copyToClipboard(String name)
    {
        SwingUtilities.invokeLater(() -> {
            try { Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(name), null); }
            catch (IllegalStateException ex)
            { JOptionPane.showMessageDialog(null, "The clipboard is busy. Please try Copy name again.", "Improved Right-Clicks", JOptionPane.INFORMATION_MESSAGE); }
        });
    }

    private static final class Edit
    {
        final String original, decorated, label, option;
        final MenuAction type;
        final int identifier, param0, param1, worldView;
        Edit(MenuEntry entry, String original, String decorated, String label)
        {
            this.original = original; this.decorated = decorated; this.label = label;
            option = entry.getOption(); type = entry.getType(); identifier = entry.getIdentifier();
            param0 = entry.getParam0(); param1 = entry.getParam1(); worldView = entry.getWorldViewId();
        }
        boolean matches(MenuEntry entry)
        {
            // Our hover colors can change the option before badges are restored or refreshed.
            return decorated.equals(entry.getTarget()) && MenuRules.plain(option).equals(MenuRules.plain(entry.getOption())) && entry.getType() == type
                && entry.getIdentifier() == identifier && entry.getParam0() == param0 && entry.getParam1() == param1
                && entry.getWorldViewId() == worldView;
        }
    }
}
