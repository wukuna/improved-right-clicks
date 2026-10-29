package com.improvedmenus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.runelite.api.*;
import net.runelite.client.game.ChatIconManager;
import net.runelite.client.party.PartyService;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PlayerMenuBadgesTest
{
    private final Client client = mock(Client.class);
    private final ImprovedMenusConfig config = mock(ImprovedMenusConfig.class);
    private final ChatIconManager icons = mock(ChatIconManager.class);
    private final PartyService party = mock(PartyService.class);
    private final PlayerAccountBadges accounts = mock(PlayerAccountBadges.class);
    private final TooltipManager tooltips = mock(TooltipManager.class);
    private PlayerMenuBadges badges;
    private Player player;

    @Before public void setup()
    {
        AtomicInteger id = new AtomicInteger();
        when(icons.registerChatIcon(any())).thenAnswer(i -> id.getAndIncrement());
        when(icons.chatIconIndex(anyInt())).thenAnswer(i -> 100 + (int) i.getArgument(0));
        when(icons.getIconNumber(any(FriendsChatRank.class))).thenAnswer(i -> 50 + ((FriendsChatRank) i.getArgument(0)).ordinal());
        player = mock(Player.class); when(player.getName()).thenReturn("Alice");
        when(player.isFriend()).thenReturn(true);
        when(config.playerFriendBadge()).thenReturn(true);
        badges = new PlayerMenuBadges(client, config, icons, party, accounts, tooltips);
        badges.start();
    }

    private MenuEntry entry(String target)
    {
        MenuEntry e = TestMenus.entry("Follow", target, MenuAction.PLAYER_SECOND_OPTION, -1, 0);
        when(e.getPlayer()).thenReturn(player);
        return e;
    }

    @Test public void friendSmileyPreservesClanRankColorsAndNativeActions()
    {
        MenuEntry e = entry("<img=70><col=ff0000>Alice</col> (level-126)");
        Menu menu = TestMenus.menu(e); badges.decorate(menu);
        assertEquals("<img=100><img=70><col=ff0000>Alice</col> (level-126)", e.getTarget());
        verify(e, never()).setType(any()); verify(e, never()).setOption(anyString());
        verify(menu, never()).setMenuEntries(any());
        badges.decorate(menu);
        assertEquals(1, e.getTarget().split("<img=100>", -1).length - 1);
        badges.clear(); assertEquals("<img=70><col=ff0000>Alice</col> (level-126)", e.getTarget());
    }

    @Test public void independentTogglesAndOfflineFriendsRemainUndecorated()
    {
        MenuEntry e = entry("Alice"); Menu menu = TestMenus.menu(e);
        when(player.isFriend()).thenReturn(false); badges.decorate(menu); assertEquals("Alice", e.getTarget());
        when(player.isFriend()).thenReturn(true); when(config.playerFriendBadge()).thenReturn(false);
        when(player.isFriendsChatMember()).thenReturn(true); when(config.playerFriendsChatBadge()).thenReturn(true);
        badges.decorate(menu); assertEquals("<img=102>Alice", e.getTarget());
        when(config.playerFriendsChatBadge()).thenReturn(false); badges.decorate(menu); assertEquals("Alice", e.getTarget());
    }

    @Test public void existingHelmetsAndFriendsChatRanksAreNotDuplicated()
    {
        when(config.playerIronmanBadge()).thenReturn(true);
        when(config.playerFriendsChatBadge()).thenReturn(true); when(player.isFriendsChatMember()).thenReturn(true);
        int rank = icons.getIconNumber(FriendsChatRank.CAPTAIN);
        MenuEntry e = entry("<img=10><img=" + rank + ">Alice"); badges.decorate(TestMenus.menu(e));
        assertEquals("<img=100><img=10><img=" + rank + ">Alice", e.getTarget());
        verifyNoInteractions(accounts);
    }

    @Test public void ignoreAndPartyBadgesHaveIndependentSwitches()
    {
        when(config.playerFriendBadge()).thenReturn(false);
        NameableContainer<Ignore> ignores = mock(NameableContainer.class);
        when(client.getIgnoreContainer()).thenReturn(ignores); when(ignores.findByName("Alice")).thenReturn(mock(Ignore.class));
        when(party.isInParty()).thenReturn(true);
        when(party.getMemberByDisplayName("Alice")).thenReturn(mock(net.runelite.client.party.PartyMember.class));
        MenuEntry e = entry("Alice"); Menu menu = TestMenus.menu(e);
        when(config.playerIgnoreBadge()).thenReturn(true); badges.decorate(menu); assertEquals("<img=101>Alice", e.getTarget());
        when(config.playerPartyBadge()).thenReturn(true); badges.decorate(menu); assertEquals("<img=101><img=103>Alice", e.getTarget());
        when(config.playerIgnoreBadge()).thenReturn(false); badges.decorate(menu); assertEquals("<img=103>Alice", e.getTarget());
        when(party.isInParty()).thenReturn(false); badges.decorate(menu); assertEquals("Alice", e.getTarget());
    }

    @Test public void duplicateRegistrationWithSamePixelsIsRecognized()
    {
        IndexedSprite a = sprite(new byte[]{0, 1}, new int[]{0, 0x55cc77});
        IndexedSprite b = sprite(new byte[]{0, 2}, new int[]{0, 1, 0x55cc77});
        IndexedSprite[] sprites = new IndexedSprite[150]; sprites[100] = a; sprites[140] = b;
        when(client.getModIcons()).thenReturn(sprites);
        MenuEntry e = entry("<img=140>Alice"); badges.decorate(TestMenus.menu(e));
        assertEquals("<img=140>Alice", e.getTarget());
        assertFalse(PlayerMenuBadges.sameImage(a, sprite(new byte[]{0, 1}, new int[]{0, 0xff0000})));
        assertFalse(PlayerMenuBadges.sameImage(sprite(new byte[]{0, 0}, new int[]{0}), sprite(new byte[]{0, 0}, new int[]{0})));
    }

    @Test public void existingCustomHelmetPixelsSuppressHiscoresLookup()
    {
        when(config.playerIronmanBadge()).thenReturn(true);
        IndexedSprite[] sprites = new IndexedSprite[150];
        sprites[10] = sprite(new byte[]{0, 1}, new int[]{0, 0xcc3355}); sprites[140] = sprites[10];
        when(client.getModIcons()).thenReturn(sprites);
        MenuEntry e = entry("<img=140>Alice"); badges.decorate(TestMenus.menu(e));
        assertEquals("<img=100><img=140>Alice", e.getTarget()); verifyNoInteractions(accounts);
    }

    @Test public void thirdPartyChangesAndReusedEntryAreNeverRolledBack()
    {
        MenuEntry e = entry("Alice"); badges.decorate(TestMenus.menu(e));
        e.setTarget("<img=99>Someone else"); badges.clear(); assertEquals("<img=99>Someone else", e.getTarget());
        MenuEntry reused = entry("Alice"); badges.decorate(TestMenus.menu(reused));
        when(reused.getWorldViewId()).thenReturn(1); badges.clear(); assertEquals("<img=100>Alice", reused.getTarget());
    }

    @Test public void unknownOrUnavailableIconsStayBlankAndRegistrationIsReused()
    {
        when(config.playerIronmanBadge()).thenReturn(true);
        when(icons.chatIconIndex(anyInt())).thenReturn(-1);
        MenuEntry e = entry("Alice"); badges.decorate(TestMenus.menu(e)); assertEquals("Alice", e.getTarget());
        badges.shutdown(); badges.start(); verify(icons, times(4)).registerChatIcon(any());
    }

    @Test public void copiesPlainNameOnceAndRemovesOnlyOwnedLocalRows()
    {
        when(config.playerCopyName()).thenReturn(true);
        when(player.getName()).thenReturn("Alice\u00a0Bob");
        List<MenuEntry> entries = new ArrayList<>(Arrays.asList(entry("Alice Bob"), entry("Alice Bob")));
        Menu menu = mock(Menu.class);
        when(menu.getMenuEntries()).thenAnswer(i -> entries.toArray(new MenuEntry[0]));
        when(menu.createMenuEntry(0)).thenAnswer(i -> {
            MenuEntry copy = TestMenus.entry("", "", MenuAction.RUNELITE, -1, 0); entries.add(0, copy); return copy;
        });
        doAnswer(i -> { entries.remove(i.getArgument(0)); return null; }).when(menu).removeMenuEntry(any());
        Consumer<String> clipboard = mock(Consumer.class); badges.clipboard = clipboard;
        badges.decorate(menu); assertEquals(3, entries.size());
        MenuEntry copy = entries.get(0); assertEquals("Copy name", copy.getOption()); assertEquals("Alice Bob", copy.getTarget());
        ArgumentCaptor<Consumer<MenuEntry>> callback = ArgumentCaptor.forClass(Consumer.class);
        verify(copy).onClick(callback.capture()); callback.getValue().accept(copy); verify(clipboard).accept("Alice Bob");
        badges.clear(); assertEquals(2, entries.size()); callback.getValue().accept(copy); verifyNoMoreInteractions(clipboard);
    }

    @Test public void existingCopyNameRowIsKeptWithoutAddingAnother()
    {
        when(config.playerCopyName()).thenReturn(true);
        MenuEntry copy = TestMenus.entry("Copy player name", "<col=ffffff>Alice</col>", MenuAction.RUNELITE, -1, 0);
        Menu menu = TestMenus.menu(copy, entry("Alice")); badges.decorate(menu);
        verify(menu, never()).createMenuEntry(anyInt()); badges.clear(); verify(menu, never()).removeMenuEntry(copy);
    }

    @Test public void inferredHelmetTooltipIdentifiesHiscoresEstimate()
    {
        when(config.playerIronmanBadge()).thenReturn(true);
        when(accounts.find("Alice")).thenReturn(new PlayerAccountBadges.Badge(2, true));
        MenuEntry e = entry("Alice"); Menu menu = TestMenus.menu(e);
        when(client.isMenuOpen()).thenReturn(true); when(client.getMenu()).thenReturn(menu);
        when(menu.getMenuWidth()).thenReturn(300); when(menu.getMenuHeight()).thenReturn(40);
        when(client.getMouseCanvasPosition()).thenReturn(new Point(5, 23));
        badges.decorate(menu); assertTrue(e.getTarget().startsWith("<img=2><img=100>"));
        badges.render(null); verify(tooltips).add(argThat(tooltip -> tooltip.getText().contains("Ironman - Hiscores estimate")));
        badges.shutdown(); badges.render(null); verifyNoMoreInteractions(tooltips);
    }

    private IndexedSprite sprite(byte[] pixels, int[] colors)
    {
        IndexedSprite sprite = mock(IndexedSprite.class); when(sprite.getWidth()).thenReturn(2); when(sprite.getHeight()).thenReturn(1);
        when(sprite.getPixels()).thenReturn(pixels); when(sprite.getPalette()).thenReturn(colors); return sprite;
    }
}
