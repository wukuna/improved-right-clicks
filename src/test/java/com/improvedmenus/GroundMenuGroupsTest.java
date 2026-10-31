package com.improvedmenus;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import java.util.Arrays;
import java.util.function.Consumer;
import net.runelite.api.*;
import net.runelite.client.game.ItemManager;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class GroundMenuGroupsTest
{
    private MenuEntry ashes()
    {
        MenuEntry entry = TestMenus.entry("Take", "<col=ff9040>Abyssal ashes</col>", MenuAction.GROUND_ITEM_THIRD_OPTION, -1, 0);
        when(entry.getIdentifier()).thenReturn(25775);
        when(entry.getParam0()).thenReturn(40);
        when(entry.getParam1()).thenReturn(50);
        when(entry.getWorldViewId()).thenReturn(-1);
        return entry;
    }

    @Test public void sixAshesBecomeOneRowAndKeepTheFirstDisplayedNativeAction()
    {
        MenuEntry[] entries = new MenuEntry[6];
        Arrays.setAll(entries, i -> ashes());
        Menu menu = TestMenus.menu(entries);
        GroundMenuGroups.collapse(menu);
        assertArrayEquals(new MenuEntry[]{entries[5]}, menu.getMenuEntries());
        assertEquals("<col=ff9040>Abyssal ashes</col> (x6)", entries[5].getTarget());
        for (MenuEntry entry : entries)
        {
            verify(entry, never()).setType(any());
            verify(entry, never()).setIdentifier(anyInt());
            verify(entry, never()).setParam0(anyInt());
            verify(entry, never()).setParam1(anyInt());
            verify(entry, never()).onClick(any());
        }
        GroundMenuGroups.collapse(menu);
        assertEquals("<col=ff9040>Abyssal ashes</col> (x6)", entries[5].getTarget());
    }

    @Test public void actionIdentityTextPriorityAndWorldDifferencesStaySeparate()
    {
        for (int difference = 0; difference < 10; difference++)
        {
            MenuEntry a = ashes(), b = ashes();
            switch (difference)
            {
                case 0: when(b.getParam0()).thenReturn(41); break;
                case 1: when(b.getParam1()).thenReturn(51); break;
                case 2: when(b.getWorldViewId()).thenReturn(1); break;
                case 3: when(b.getIdentifier()).thenReturn(25776); break;
                case 4: when(b.getItemId()).thenReturn(25776); break;
                case 5: when(b.getType()).thenReturn(MenuAction.GROUND_ITEM_FIRST_OPTION); break;
                case 6: b.setOption("Destroy"); break;
                case 7: b.setTarget("<col=00ff00>Abyssal ashes</col>"); break;
                case 8: when(b.isDeprioritized()).thenReturn(true); break;
                default: when(b.isForceLeftClick()).thenReturn(true);
            }
            Menu menu = TestMenus.menu(a, b);
            GroundMenuGroups.collapse(menu);
            assertArrayEquals("difference " + difference, new MenuEntry[]{a, b}, menu.getMenuEntries());
        }
    }

    @Test public void otherActionsCustomCallbacksAndSubmenusAreNeverCollapsed()
    {
        for (MenuAction type : new MenuAction[]{MenuAction.PLAYER_FIRST_OPTION, MenuAction.NPC_FIRST_OPTION,
            MenuAction.GAME_OBJECT_FIRST_OPTION, MenuAction.CC_OP, MenuAction.WIDGET_TARGET_ON_GROUND_ITEM,
            MenuAction.ITEM_USE_ON_GROUND_ITEM, MenuAction.RUNELITE})
        {
            MenuEntry a = ashes(), b = ashes();
            when(a.getType()).thenReturn(type); when(b.getType()).thenReturn(type);
            Menu menu = TestMenus.menu(a, b); GroundMenuGroups.collapse(menu);
            assertEquals(type.name(), 2, menu.getMenuEntries().length);
        }
        MenuEntry callback = ashes(), submenu = ashes();
        Consumer<MenuEntry> action = mock(Consumer.class);
        when(callback.onClick()).thenReturn(action);
        Menu child = TestMenus.menu(ashes(), ashes());
        when(submenu.getSubMenu()).thenReturn(child);
        Menu menu = TestMenus.menu(ashes(), callback, submenu);
        GroundMenuGroups.collapse(menu);
        assertEquals(3, menu.getMenuEntries().length);
        assertEquals(2, submenu.getSubMenu().getMenuEntries().length);
        verifyNoInteractions(action);
    }

    @Test public void keepsOtherRowsInOrderAndGroupsExamineIndependently()
    {
        MenuEntry examine1 = ashes(), examine2 = ashes(), first = ashes(), last = ashes();
        when(examine1.getType()).thenReturn(MenuAction.EXAMINE_ITEM_GROUND); examine1.setOption("Examine");
        when(examine2.getType()).thenReturn(MenuAction.EXAMINE_ITEM_GROUND); examine2.setOption("Examine");
        MenuEntry walk = TestMenus.entry("Walk here", "", MenuAction.WALK, -1, 0);
        MenuEntry npc = TestMenus.entry("Attack", "Demon", MenuAction.NPC_SECOND_OPTION, -1, 0);
        Menu menu = TestMenus.menu(walk, examine1, first, npc, examine2, last);
        GroundMenuGroups.collapse(menu);
        assertArrayEquals(new MenuEntry[]{walk, npc, examine2, last}, menu.getMenuEntries());
        assertTrue(examine2.getTarget().endsWith(" (x2)"));
        assertTrue(last.getTarget().endsWith(" (x2)"));
    }

    @Test public void existingGroundItemsCountsAndDifferentStackQuantitiesRemainIntact()
    {
        for (String suffix : new String[]{" x 6", " (x6)"})
        {
            MenuEntry a = ashes(), b = ashes(); a.setTarget("Abyssal ashes" + suffix); b.setTarget(a.getTarget());
            Menu menu = TestMenus.menu(a, b); GroundMenuGroups.collapse(menu);
            assertArrayEquals(new MenuEntry[]{a, b}, menu.getMenuEntries());
        }
        MenuEntry one = ashes(), two = ashes(); one.setTarget("Chaos rune (10)"); two.setTarget("Chaos rune (20)");
        Menu menu = TestMenus.menu(one, two); GroundMenuGroups.collapse(menu);
        assertEquals(2, menu.getMenuEntries().length);
    }

    @Test public void iconDecorationRefreshRetainsOneCountWithoutRegrouping()
    {
        MenuEntry first = ashes(), lead = ashes(); Menu menu = TestMenus.menu(first, lead);
        GroundMenuGroups.collapse(menu);
        Client client = mock(Client.class); ChatIconFixture fixture = new ChatIconFixture(client);
        fixture.presentation.start();
        fixture.presentation.decorate(menu);
        assertEquals("<img=9><col=ff9040>Abyssal ashes</col> (x2)", lead.getTarget());
        fixture.presentation.decorate(menu);
        assertEquals("<img=9><col=ff9040>Abyssal ashes</col> (x2)", lead.getTarget());
        fixture.presentation.stop();
        assertEquals("<col=ff9040>Abyssal ashes</col> (x2)", lead.getTarget());
    }

    @Test public void disabledAndRevealLeaveDuplicatesAndNormalHoverUntouched()
    {
        Fixture f = new Fixture(); Menu menu = TestMenus.menu(ashes(), ashes());
        f.engine.groupOpened(menu); assertEquals(2, menu.getMenuEntries().length);
        when(f.config.groupGroundItems()).thenReturn(true);
        f.engine.apply(menu); assertEquals(2, menu.getMenuEntries().length);
        when(f.config.revealKey()).thenReturn(ImprovedMenusConfig.RevealKey.CONTROL);
        java.awt.Canvas canvas = mock(java.awt.Canvas.class);
        when(f.client.getCanvas()).thenReturn(canvas); when(canvas.hasFocus()).thenReturn(true);
        when(f.client.isKeyPressed(KeyCode.KC_CONTROL)).thenReturn(true);
        f.engine.groupOpened(menu); assertEquals(2, menu.getMenuEntries().length);
        when(f.client.isKeyPressed(KeyCode.KC_CONTROL)).thenReturn(false);
        f.engine.groupOpened(menu); assertEquals(1, menu.getMenuEntries().length);
    }

    @Test public void filteringRunsBeforeGroupingAndColoredActionsStillRestore()
    {
        Fixture f = new Fixture(); when(f.config.groupGroundItems()).thenReturn(true);
        when(f.config.colors()).thenReturn("Abyssal ashes:Take:#123abc");
        f.engine.reload(); MenuEntry lead = ashes(); Menu menu = TestMenus.menu(ashes(), lead);
        f.engine.applyOpened(menu); f.engine.groupOpened(menu);
        assertEquals("<col=123abc>Take</col>", lead.getOption());
        when(menu.getMenuWidth()).thenReturn(200); when(menu.getMenuHeight()).thenReturn(40);
        when(f.client.getMouseCanvasPosition()).thenReturn(new Point(10, 25));
        f.engine.highlightHovered(menu); assertEquals("Take", lead.getOption());
        f.engine.clear(); assertEquals("Take", lead.getOption());
        when(f.config.itemRules()).thenReturn("[ground] Take,Abyssal ashes"); f.engine.reload();
        menu = TestMenus.menu(ashes(), ashes()); f.engine.applyOpened(menu); f.engine.groupOpened(menu);
        assertEquals(0, menu.getMenuEntries().length);
    }

    private static final class Fixture
    {
        final Client client = mock(Client.class);
        final ImprovedMenusConfig config = mock(ImprovedMenusConfig.class, CALLS_REAL_METHODS);
        final MenuEngine engine;
        Fixture()
        {
            when(config.revealKey()).thenReturn(ImprovedMenusConfig.RevealKey.NONE);
            engine = Guice.createInjector(new AbstractModule() { protected void configure() {
                bind(Client.class).toInstance(client); bind(ImprovedMenusConfig.class).toInstance(config);
                bind(ItemManager.class).toInstance(mock(ItemManager.class));
            } }).getInstance(MenuEngine.class); engine.reload();
        }
    }

    private static final class ChatIconFixture
    {
        final MenuPresentation presentation;
        ChatIconFixture(Client client)
        {
            ImprovedMenusConfig config = mock(ImprovedMenusConfig.class, CALLS_REAL_METHODS);
            when(config.groundIcons()).thenReturn(true);
            net.runelite.client.game.ChatIconManager icons = mock(net.runelite.client.game.ChatIconManager.class);
            when(icons.registerChatIcon(any())).thenReturn(2); when(icons.chatIconIndex(2)).thenReturn(9);
            presentation = Guice.createInjector(new AbstractModule() { protected void configure() {
                bind(Client.class).toInstance(client); bind(ImprovedMenusConfig.class).toInstance(config);
                bind(ItemManager.class).toInstance(mock(ItemManager.class));
                bind(net.runelite.client.game.ChatIconManager.class).toInstance(icons);
                bind(net.runelite.client.ui.overlay.tooltip.TooltipManager.class).toInstance(mock(net.runelite.client.ui.overlay.tooltip.TooltipManager.class));
                bind(LootFiltersCompatibility.class).toInstance(mock(LootFiltersCompatibility.class));
                bind(net.runelite.client.config.RuneLiteConfig.class).toInstance(mock(net.runelite.client.config.RuneLiteConfig.class));
            } }).getInstance(MenuPresentation.class);
        }
    }
}
