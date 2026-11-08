package com.improvedmenus;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import java.awt.Canvas;
import java.util.function.Consumer;
import net.runelite.api.Client;
import net.runelite.api.KeyCode;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.client.game.ItemManager;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class GroundExamineOrderingTest
{
    @Test public void examineFollowsAllTakesWithCollapsingOnOrOff()
    {
        for (boolean collapse : new boolean[]{false, true})
        {
            Fixture f = new Fixture();
            when(f.config.groundExamineLast()).thenReturn(true);
            when(f.config.groupGroundItems()).thenReturn(collapse);
            MenuEntry cancel = row("Cancel", "", MenuAction.CANCEL);
            MenuEntry walk = row("Walk here", "", MenuAction.WALK);
            MenuEntry first = take("Potion", false), lead = take("Potion", false);
            MenuEntry bones = take("Big bones", true), horn = take("Elkhorn", true);
            MenuEntry examineFirst = examine("Potion"), examineLead = examine("Potion");
            MenuEntry examineBones = examine("Big bones"), examineHorn = examine("Elkhorn");
            Menu menu = TestMenus.menu(cancel, examineFirst, bones, examineBones,
                horn, examineHorn, examineLead, walk, first, lead);
            f.engine.applyOpened(menu);
            f.engine.groupOpened(menu);
            MenuEntry[] expected = collapse
                ? new MenuEntry[]{cancel, examineBones, examineHorn, examineLead, bones, horn, walk, lead}
                : new MenuEntry[]{cancel, examineFirst, examineBones, examineHorn, examineLead, bones, horn, walk, first, lead};
            assertArrayEquals(expected, menu.getMenuEntries());
            assertEquals(collapse ? "Potion (x2)" : "Potion", lead.getTarget());
            assertEquals(collapse ? "Potion (x2)" : "Potion", examineLead.getTarget());
            assertTrue(bones.isDeprioritized());
            assertTrue(horn.isDeprioritized());
            clearInvocations(menu);
            f.engine.groupOpened(menu);
            assertArrayEquals(expected, menu.getMenuEntries());
            verify(menu, never()).setMenuEntries(any());
        }
    }

    @Test public void hidingStillRemovesExaminesWithCollapsingOnOrOff()
    {
        for (boolean collapse : new boolean[]{false, true})
        {
            Fixture f = new Fixture();
            when(f.config.groundExamineLast()).thenReturn(true);
            when(f.config.examineGround()).thenReturn(true);
            when(f.config.groupGroundItems()).thenReturn(collapse);
            MenuEntry take = take("Bones", true), cancel = row("Cancel", "", MenuAction.CANCEL);
            Menu menu = TestMenus.menu(cancel, examine("Bones"), take, examine("Bones"));
            f.engine.applyOpened(menu);
            f.engine.groupOpened(menu);
            assertArrayEquals(new MenuEntry[]{cancel, take}, menu.getMenuEntries());
        }
    }

    @Test public void disabledAndRevealPreserveNativeOrdering()
    {
        Fixture f = new Fixture();
        MenuEntry[] original = {take("Bones", true), examine("Bones"), take("Potion", false)};
        Menu menu = TestMenus.menu(original);
        f.engine.groupOpened(menu);
        assertArrayEquals(original, menu.getMenuEntries());
        when(f.config.groundExamineLast()).thenReturn(true);
        f.engine.apply(menu);
        assertArrayEquals(original, menu.getMenuEntries());
        Canvas canvas = mock(Canvas.class);
        when(f.client.getCanvas()).thenReturn(canvas);
        when(canvas.hasFocus()).thenReturn(true);
        when(f.config.revealKey()).thenReturn(ImprovedMenusConfig.RevealKey.CONTROL);
        when(f.client.isKeyPressed(KeyCode.KC_CONTROL)).thenReturn(true);
        f.engine.groupOpened(menu);
        assertArrayEquals(original, menu.getMenuEntries());
        when(f.client.isKeyPressed(KeyCode.KC_CONTROL)).thenReturn(false);
        f.engine.groupOpened(menu);
        assertArrayEquals(new MenuEntry[]{original[1], original[0], original[2]}, menu.getMenuEntries());
    }

    @Test public void keepsOtherActionsSubmenusCallbacksAndClickIdentityIntact()
    {
        MenuEntry take = take("<img=4><col=ff0000>Coins</col>", true);
        MenuEntry examine = examine("<img=4><col=ff0000>Coins</col>");
        MenuEntry npc = row("Examine", "Goblin", MenuAction.EXAMINE_NPC);
        MenuEntry object = row("Examine", "Chest", MenuAction.EXAMINE_OBJECT);
        MenuEntry inventory = row("Examine", "Coins", MenuAction.CC_OP);
        MenuEntry parent = row("More", "", MenuAction.RUNELITE);
        Menu child = TestMenus.menu(take("Shark", false), examine("Shark"));
        when(parent.getSubMenu()).thenReturn(child);
        Consumer<MenuEntry> callback = mock(Consumer.class);
        when(examine.onClick()).thenReturn(callback);
        when(examine.getIdentifier()).thenReturn(995);
        when(examine.getParam0()).thenReturn(42);
        when(examine.getParam1()).thenReturn(51);
        when(examine.getWorldViewId()).thenReturn(3);
        when(examine.isForceLeftClick()).thenReturn(true);
        Menu menu = TestMenus.menu(take, npc, examine, object, inventory, parent);
        GroundMenuGroups.examineLast(menu);
        assertArrayEquals(new MenuEntry[]{examine, take, npc, object, inventory, parent}, menu.getMenuEntries());
        assertEquals("<img=4><col=ff0000>Coins</col>", examine.getTarget());
        assertEquals(995, examine.getIdentifier());
        assertEquals(42, examine.getParam0());
        assertEquals(51, examine.getParam1());
        assertEquals(3, examine.getWorldViewId());
        assertSame(callback, examine.onClick());
        assertSame(child, parent.getSubMenu());
        verify(child, never()).setMenuEntries(any());
        verifyNoInteractions(callback);
        for (MenuEntry entry : menu.getMenuEntries())
        {
            verify(entry, never()).setType(any());
            verify(entry, never()).setIdentifier(anyInt());
            verify(entry, never()).setItemId(anyInt());
            verify(entry, never()).setParam0(anyInt());
            verify(entry, never()).setParam1(anyInt());
            verify(entry, never()).setWorldViewId(anyInt());
            verify(entry, never()).setDeprioritized(anyBoolean());
            verify(entry, never()).setForceLeftClick(anyBoolean());
            verify(entry, never()).onClick(any());
        }
    }

    @Test public void cancelAlwaysFollowsExamineAndAbsentCancelStaysAbsent()
    {
        MenuEntry cancel = row("Cancel", "", MenuAction.CANCEL);
        MenuEntry take = take("Shark", false), examine = examine("Shark");
        Menu menu = TestMenus.menu(take, cancel, examine);
        GroundMenuGroups.examineLast(menu);
        assertArrayEquals(new MenuEntry[]{cancel, examine, take}, menu.getMenuEntries());
        menu = TestMenus.menu(take, examine);
        GroundMenuGroups.examineLast(menu);
        assertArrayEquals(new MenuEntry[]{examine, take}, menu.getMenuEntries());
        menu = TestMenus.menu();
        GroundMenuGroups.examineLast(menu);
        verify(menu, never()).setMenuEntries(any());
    }

    private static MenuEntry row(String option, String target, MenuAction type)
    {
        return TestMenus.entry(option, target, type, -1, 0);
    }

    private static MenuEntry examine(String target)
    {
        return row("Examine", target, MenuAction.EXAMINE_ITEM_GROUND);
    }

    private static MenuEntry take(String target, boolean deprioritized)
    {
        MenuEntry entry = row("Take", target, MenuAction.GROUND_ITEM_THIRD_OPTION);
        when(entry.isDeprioritized()).thenReturn(deprioritized);
        return entry;
    }

    private static final class Fixture
    {
        final Client client = mock(Client.class);
        final ImprovedMenusConfig config = mock(ImprovedMenusConfig.class, CALLS_REAL_METHODS);
        final MenuEngine engine;
        Fixture()
        {
            when(config.revealKey()).thenReturn(ImprovedMenusConfig.RevealKey.NONE);
            engine = Guice.createInjector(new AbstractModule() {
                protected void configure() {
                    bind(Client.class).toInstance(client);
                    bind(ImprovedMenusConfig.class).toInstance(config);
                    bind(ItemManager.class).toInstance(mock(ItemManager.class));
                }
            }).getInstance(MenuEngine.class);
            engine.reload();
        }
    }
}
