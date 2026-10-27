package com.improvedmenus;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.runelite.api.*;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.menuentryswapper.MenuEntrySwapperConfig;
import net.runelite.client.plugins.menuentryswapper.MenuEntrySwapperPlugin;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SwapperConfigurationCompatibilityTest
{
    @Mock private Client client;
    @Mock private ItemManager items;
    @Mock private ConfigManager configs;
    @Mock private MenuEntrySwapperConfig swapperConfig;
    @InjectMocks private MenuEntrySwapperPlugin swapper;
    private ImprovedMenusConfig config;
    private MenuEngine engine;

    @Before public void setup()
    {
        MockitoAnnotations.openMocks(this);
        config = mock(ImprovedMenusConfig.class, CALLS_REAL_METHODS);
        when(config.revealKey()).thenReturn(ImprovedMenusConfig.RevealKey.NONE);
        when(config.enableColors()).thenReturn(true);
        when(config.colors()).thenReturn("*:*:#123abc");
        when(config.examineInventory()).thenReturn(true);
        when(config.examineObject()).thenReturn(true);
        when(config.examineNpc()).thenReturn(true);
        when(config.keepLoneObject()).thenReturn(false);
        when(config.keepLoneNpc()).thenReturn(false);
        when(client.isKeyPressed(KeyCode.KC_SHIFT)).thenReturn(true);
        engine = Guice.createInjector(new AbstractModule() {
            @Override protected void configure() {
                bind(Client.class).toInstance(client);
                bind(ImprovedMenusConfig.class).toInstance(config);
                bind(ItemManager.class).toInstance(items);
            }
        }).getInstance(MenuEngine.class);
        engine.reload();
    }

    private MenuEntry examine(MenuAction type, int group)
    {
        MenuEntry entry = TestMenus.entry("Examine", "Rune sword", type, 100, group);
        when(entry.getIdentifier()).thenReturn(10);
        return entry;
    }

    @Test public void shiftedExamineSurvivesUntilOpenThenNormalCleanupResumes()
    {
        for (MenuEntry anchor : new MenuEntry[]{examine(MenuAction.EXAMINE_OBJECT, 0),
            examine(MenuAction.EXAMINE_NPC, 0), examine(MenuAction.CC_OP, InterfaceID.INVENTORY),
            examine(MenuAction.CC_OP_LOW_PRIORITY, InterfaceID.WORNITEMS)})
        {
            Menu menu = TestMenus.menu(anchor);
            engine.apply(menu);
            assertArrayEquals(new MenuEntry[]{anchor}, menu.getMenuEntries());
            assertEquals("Examine", anchor.getOption());
            engine.applyOpened(menu);
            // Worn Examine is classified as an item just like inventory Examine.
            assertEquals(0, menu.getMenuEntries().length);
        }
    }

    @Test public void holdingShiftRestoresPreviouslyColoredAnchor()
    {
        when(config.examineInventory()).thenReturn(false);
        when(client.isKeyPressed(KeyCode.KC_SHIFT)).thenReturn(false);
        MenuEntry anchor = examine(MenuAction.CC_OP, InterfaceID.INVENTORY);
        Menu menu = TestMenus.menu(anchor);
        engine.apply(menu);
        assertNotEquals("Examine", anchor.getOption());
        when(client.isKeyPressed(KeyCode.KC_SHIFT)).thenReturn(true);
        engine.apply(menu);
        assertEquals("Examine", anchor.getOption());
    }

    @Test public void ordinaryMenusAndOtherProtectionsStillFilter()
    {
        when(client.isKeyPressed(KeyCode.KC_SHIFT)).thenReturn(false);
        Menu menu = TestMenus.menu(examine(MenuAction.CC_OP, InterfaceID.INVENTORY));
        engine.apply(menu);
        assertEquals(0, menu.getMenuEntries().length);
        when(client.isKeyPressed(KeyCode.KC_SHIFT)).thenReturn(true);
        when(config.potionEmpty()).thenReturn(true);
        menu = TestMenus.menu(TestMenus.entry("Empty", "Prayer potion(4)", MenuAction.CC_OP, 100, InterfaceID.INVENTORY));
        engine.apply(menu);
        assertEquals(0, menu.getMenuEntries().length);
    }

    @Test public void realSwapperBuildsControlsBeforeWorkshopHidesExamine()
    {
        when(swapperConfig.leftClickCustomization()).thenReturn(true);
        when(swapperConfig.shiftClickCustomization()).thenReturn(true);
        MenuEntry anchor = examine(MenuAction.CC_OP, InterfaceID.INVENTORY);
        Widget widget = mock(Widget.class);
        when(widget.getId()).thenReturn(InterfaceID.INVENTORY << 16);
        when(anchor.getWidget()).thenReturn(widget);
        ItemComposition item = mock(ItemComposition.class);
        when(item.getId()).thenReturn(100);
        when(item.getInventoryActions()).thenReturn(new String[]{"Wield", null, null, null, "Drop"});
        when(items.getItemComposition(100)).thenReturn(item);
        List<MenuEntry> created = new ArrayList<>();
        Menu menu = TestMenus.menu(anchor);
        when(client.createMenuEntry(anyInt())).thenAnswer(i -> {
            MenuEntry entry = TestMenus.entry("", "", MenuAction.RUNELITE, -1, 0);
            Menu sub = mock(Menu.class);
            when(entry.createSubMenu()).thenReturn(sub);
            when(entry.getSubMenu()).thenReturn(sub);
            when(sub.getMenuEntries()).thenReturn(new MenuEntry[0]);
            when(sub.createMenuEntry(anyInt())).thenAnswer(j -> TestMenus.entry("", "", MenuAction.RUNELITE, -1, 0));
            created.add(entry);
            List<MenuEntry> entries = new ArrayList<>(Arrays.asList(menu.getMenuEntries()));
            entries.add(0, entry);
            menu.setMenuEntries(entries.toArray(new MenuEntry[0]));
            return entry;
        });
        engine.apply(menu);
        MenuOpened event = new MenuOpened();
        event.setMenuEntries(menu.getMenuEntries());
        swapper.onMenuOpened(event);
        assertEquals(2, created.size());
        assertEquals("Swap left-click", created.get(0).getOption());
        assertEquals("Swap shift-click", created.get(1).getOption());
        for (MenuEntry entry : created) { clearInvocations(entry); }
        engine.applyOpened(menu);
        assertEquals(2, menu.getMenuEntries().length);
        assertTrue(Arrays.asList(menu.getMenuEntries()).containsAll(created));
        for (MenuEntry entry : created) { verify(entry, never()).setType(any()); }
    }
}
