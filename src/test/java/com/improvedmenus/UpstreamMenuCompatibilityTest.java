package com.improvedmenus;
import com.google.inject.*;
import net.runelite.api.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.game.ItemManager;
import java.util.function.Consumer;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
public class UpstreamMenuCompatibilityTest {
 private MenuEngine engine(){Client c=mock(Client.class);ImprovedMenusConfig cfg=mock(ImprovedMenusConfig.class,CALLS_REAL_METHODS);when(cfg.revealKey()).thenReturn(ImprovedMenusConfig.RevealKey.NONE);when(cfg.enableColors()).thenReturn(true);when(cfg.colors()).thenReturn("*:*:#123abc");when(cfg.examineObject()).thenReturn(true);when(cfg.keepLoneObject()).thenReturn(false);when(cfg.hideCancel()).thenReturn(true);MenuEngine e=Guice.createInjector(new AbstractModule(){protected void configure(){bind(Client.class).toInstance(c);bind(ImprovedMenusConfig.class).toInstance(cfg);bind(ItemManager.class).toInstance(mock(ItemManager.class));}}).getInstance(MenuEngine.class);e.reload();return e;}
 @Test public void recoloringRetainsCustomCallbacksAndAllActionMetadata(){
  MenuEngine e=engine();for(String name:new String[]{"Lookup","Equipment","Predict","Inspect"}){MenuEntry entry=TestMenus.entry(name,"Alice",MenuAction.RUNELITE,-1,0);Consumer<MenuEntry> cb=mock(Consumer.class);when(entry.onClick()).thenReturn(cb);Menu menu=TestMenus.menu(entry);e.apply(menu);assertSame(entry,menu.getMenuEntries()[0]);assertSame(cb,entry.onClick());entry.onClick().accept(entry);verify(cb).accept(entry);verify(entry,never()).setType(any());verify(entry,never()).setIdentifier(anyInt());verify(entry,never()).setParam0(anyInt());verify(entry,never()).setParam1(anyInt());e.clear();assertEquals(name,entry.getOption());}
 }
 @Test public void colorsPreserveSwappedCapeAndBankOrdering(){
  MenuEngine e=engine();for(int group:new int[]{InterfaceID.INVENTORY,InterfaceID.BANKMAIN}){MenuEntry cancel=TestMenus.entry("Cancel","",MenuAction.CANCEL,-1,0),shift=TestMenus.entry("Crafting Guild","Max cape",MenuAction.CC_OP,100,group),left=TestMenus.entry("Home Teleport","Max cape",MenuAction.CC_OP,100,group);MenuEntry[] swapped={cancel,shift,left};Menu menu=TestMenus.menu(swapped);e.apply(menu);assertArrayEquals(swapped,menu.getMenuEntries());assertEquals("Home Teleport",MenuRules.plain(menu.getMenuEntries()[2].getOption()));e.clear();assertEquals("Home Teleport",left.getOption());}
 }
 @Test public void submenuLocationsKeepColorAndCallback(){MenuEngine e=engine();MenuEntry parent=TestMenus.entry("Teleport","Achievement cape",MenuAction.RUNELITE,-1,0),child=TestMenus.entry("Jalsavrah","",MenuAction.RUNELITE,-1,0);Consumer<MenuEntry> cb=mock(Consumer.class);when(child.onClick()).thenReturn(cb);Menu sub=TestMenus.menu(child);when(parent.getSubMenu()).thenReturn(sub);e.apply(TestMenus.menu(parent));assertEquals("<col=123abc>Jalsavrah</col>",child.getOption());assertSame(cb,child.onClick());}
 @Test public void sailsExamineRemovedBeforeOpeningAndCancelFallbackSurvives(){MenuEngine e=engine();MenuEntry sails=TestMenus.entry("Examine sails","Sails",MenuAction.EXAMINE_OBJECT,-1,0),cancel=TestMenus.entry("Cancel","",MenuAction.CANCEL,-1,0);Menu menu=TestMenus.menu(cancel,sails);e.apply(menu);assertArrayEquals(new MenuEntry[]{cancel},menu.getMenuEntries());e.applyOpened(menu);assertArrayEquals(new MenuEntry[]{cancel},menu.getMenuEntries());}
}
