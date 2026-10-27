package com.improvedmenus;

import com.google.inject.*;
import java.awt.Canvas;
import net.runelite.api.*;
import net.runelite.api.gameval.InterfaceID;
import org.junit.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class RevealAndRestorationTest
{
    private Client client; private ImprovedMenusConfig config; private MenuEngine engine; private RevealState reveal;
    @Before public void setup()
    {
        client=mock(Client.class); config=mock(ImprovedMenusConfig.class);
        when(config.revealKey()).thenReturn(ImprovedMenusConfig.RevealKey.CONTROL);
        when(config.itemRules()).thenReturn("Drop,*"); when(config.colors()).thenReturn("Sword:Use:#ff0000"); when(config.exceptions()).thenReturn("");
        when(client.getCanvas()).thenReturn(new Canvas() { @Override public boolean hasFocus() { return true; } });
        Injector injector=Guice.createInjector(new AbstractModule() { protected void configure() { bind(net.runelite.client.game.ItemManager.class).toInstance(mock(net.runelite.client.game.ItemManager.class)); bind(Client.class).toInstance(client); bind(ImprovedMenusConfig.class).toInstance(config); } });
        engine=injector.getInstance(MenuEngine.class); reveal=injector.getInstance(RevealState.class); engine.reload();
    }
    @Test public void ctrlRevealPausesOwnedFilters()
    {
        when(client.isKeyPressed(KeyCode.KC_CONTROL)).thenReturn(true);
        MenuEntry drop=TestMenus.entry("Drop","Sword",MenuAction.CC_OP,1,InterfaceID.INVENTORY); Menu menu=TestMenus.menu(drop);
        engine.apply(menu); assertSame(drop,menu.getMenuEntries()[0]);
        when(client.isKeyPressed(KeyCode.KC_CONTROL)).thenReturn(false); engine.apply(menu); assertEquals(0,menu.getMenuEntries().length);
    }
    @Test public void revealCannotRestoreOtherPluginsRemovedOptions()
    { when(client.isKeyPressed(KeyCode.KC_CONTROL)).thenReturn(true); Menu menu=TestMenus.menu(); engine.apply(menu); assertEquals(0,menu.getMenuEntries().length); }
    @Test public void lossOfCanvasFocusDisablesReveal()
    {
        when(client.isKeyPressed(KeyCode.KC_CONTROL)).thenReturn(true); when(client.getCanvas()).thenReturn(new Canvas() { @Override public boolean hasFocus() { return false; } });
        assertFalse(reveal.isRevealed());
    }
    @Test public void configurableModifierAndNoneWork()
    {
        when(config.revealKey()).thenReturn(ImprovedMenusConfig.RevealKey.ALT); when(client.isKeyPressed(KeyCode.KC_ALT)).thenReturn(true); assertTrue(reveal.isRevealed());
        when(config.revealKey()).thenReturn(ImprovedMenusConfig.RevealKey.NONE); assertFalse(reveal.isRevealed());
    }
    @Test public void clearingRestoresExactOriginalOptionIncludingOtherFormatting()
    {
        when(config.enableColors()).thenReturn(true); MenuEntry e=TestMenus.entry("<img=2><col=00ff00>Use</col>","Sword",MenuAction.CC_OP,1,InterfaceID.INVENTORY);
        engine.apply(TestMenus.menu(e)); assertTrue(e.getOption().contains("ff0000")); engine.clear(); assertEquals("<img=2><col=00ff00>Use</col>",e.getOption());
    }
    @Test public void anotherPluginsLaterColorIsNotOverwrittenOnShutdown()
    {
        when(config.enableColors()).thenReturn(true); MenuEntry e=TestMenus.entry("Use","Sword",MenuAction.CC_OP,1,InterfaceID.INVENTORY);
        engine.apply(TestMenus.menu(e)); e.setOption("<col=0000ff>Use</col>"); engine.clear(); assertEquals("<col=0000ff>Use</col>",e.getOption());
    }
    @Test public void remappedEntryIsNotRestored()
    {
        when(config.enableColors()).thenReturn(true); MenuEntry e=TestMenus.entry("Use","Sword",MenuAction.CC_OP,1,InterfaceID.INVENTORY);
        engine.apply(TestMenus.menu(e)); e.setTarget("Different item"); engine.clear(); assertTrue(e.getOption().contains("ff0000"));
    }
    @Test public void configChangeRestoresColorBeforeRebuildingRules()
    {
        when(config.enableColors()).thenReturn(true); MenuEntry e=TestMenus.entry("Use","Sword",MenuAction.CC_OP,1,InterfaceID.INVENTORY);
        engine.apply(TestMenus.menu(e)); when(config.enableColors()).thenReturn(false); engine.reload(); assertEquals("Use",e.getOption());
    }
    private Menu hoveredMenu(MenuEntry... entries)
    {
        Menu menu=TestMenus.menu(entries);
        when(menu.getMenuX()).thenReturn(100); when(menu.getMenuY()).thenReturn(200);
        when(menu.getMenuWidth()).thenReturn(200); when(menu.getMenuHeight()).thenReturn(100);
        when(config.enableColors()).thenReturn(true); when(config.yellowHover()).thenReturn(true);
        when(client.getMouseCanvasPosition()).thenReturn(new Point(120,231));
        return menu;
    }
    @Test public void hoverReleasesOnlyOwnedColorThenRestoresItOnLeave()
    {
        MenuEntry entry=TestMenus.entry("<img=2><col=00ff00>Use</col>","<col=ff9040>Sword</col>",MenuAction.CC_OP,1,InterfaceID.INVENTORY);
        Menu menu=hoveredMenu(entry); engine.applyOpened(menu); String colored=entry.getOption();
        engine.highlightHovered(menu);
        assertEquals("<img=2>Use",entry.getOption()); assertEquals("<col=ff9040>Sword</col>",entry.getTarget());
        when(client.getMouseCanvasPosition()).thenReturn(new Point(90,231)); engine.highlightHovered(menu);
        assertEquals(colored,entry.getOption());
        when(client.getMouseCanvasPosition()).thenReturn(new Point(120,231)); engine.highlightHovered(menu);
        engine.clear(); assertEquals("<img=2><col=00ff00>Use</col>",entry.getOption());
    }
    @Test public void hoverToggleAndOtherPluginChangesAreRespected()
    {
        MenuEntry entry=TestMenus.entry("Use","Sword",MenuAction.CC_OP,1,InterfaceID.INVENTORY);
        Menu menu=hoveredMenu(entry); engine.applyOpened(menu); String colored=entry.getOption();
        engine.highlightHovered(menu); when(config.yellowHover()).thenReturn(false); engine.highlightHovered(menu);
        assertEquals(colored,entry.getOption());
        entry.setOption("<col=0000ff>Use</col>"); when(config.yellowHover()).thenReturn(true); engine.highlightHovered(menu);
        assertEquals("<col=0000ff>Use</col>",entry.getOption()); engine.clear();
        assertEquals("<col=0000ff>Use</col>",entry.getOption());
    }
    @Test public void hoverHandlesRootScrollingAndDoesNotHighlightHeader()
    {
        MenuEntry lower=TestMenus.entry("Use","Sword",MenuAction.CC_OP,1,InterfaceID.INVENTORY);
        MenuEntry upper=TestMenus.entry("Use","Sword",MenuAction.CC_OP,2,InterfaceID.INVENTORY);
        Menu menu=hoveredMenu(lower,upper); when(client.isMenuScrollable()).thenReturn(true); when(client.getMenuScroll()).thenReturn(1);
        engine.applyOpened(menu); engine.highlightHovered(menu);
        assertEquals("Use",lower.getOption()); assertTrue(upper.getOption().contains("ff0000"));
        when(client.getMouseCanvasPosition()).thenReturn(new Point(120,211)); engine.highlightHovered(menu);
        assertTrue(lower.getOption().contains("ff0000"));
    }
    @Test public void staleSubmenuBoundsCannotStealRootHover()
    {
        MenuEntry child=TestMenus.entry("Use","Sword",MenuAction.CC_OP,1,InterfaceID.INVENTORY);
        Menu submenu=hoveredMenu(child);
        MenuEntry parent=TestMenus.entry("Use","Sword",MenuAction.CC_OP,2,InterfaceID.INVENTORY);
        when(parent.getSubMenu()).thenReturn(submenu);
        Menu menu=hoveredMenu(parent);
        engine.applyOpened(menu); String childColor=child.getOption();
        engine.highlightHovered(menu);
        assertEquals("Use",parent.getOption()); assertEquals(childColor,child.getOption());
        verify(submenu,never()).getMenuX(); verify(submenu,never()).getMenuWidth();
        verify(child,never()).onClick(any()); verify(child,never()).setType(any());
        when(client.getMouseCanvasPosition()).thenReturn(new Point(90,231));
        engine.highlightHovered(menu); assertTrue(parent.getOption().contains("ff0000"));
        engine.clear(); assertEquals("Use",parent.getOption()); assertEquals("Use",child.getOption());
    }
    @Test public void submenuOutsideRootKeepsItsColorWithoutReadingStaleBounds()
    {
        MenuEntry child=TestMenus.entry("Use","Sword",MenuAction.CC_OP,1,InterfaceID.INVENTORY);
        Menu submenu=hoveredMenu(child); when(submenu.getMenuX()).thenReturn(350);
        MenuEntry parent=TestMenus.entry("Actions","Sword",MenuAction.CC_OP,2,InterfaceID.INVENTORY);
        when(parent.getSubMenu()).thenReturn(submenu);
        Menu menu=hoveredMenu(parent); when(client.getMouseCanvasPosition()).thenReturn(new Point(370,231));
        engine.applyOpened(menu); String colored=child.getOption(); engine.highlightHovered(menu);
        assertEquals(colored,child.getOption()); verify(submenu,never()).getMenuX();
        engine.reload(); assertEquals("Use",child.getOption());
    }
}
