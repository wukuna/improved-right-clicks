package com.improvedmenus;

import java.awt.Color;
import net.runelite.api.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class LootFiltersMenuPresentationTest {
    @Test public void asynchronousRedecorationRestoresColorsAndIconSpacing() throws Exception {
        when(config.useLootFiltersInMenu()).thenReturn(true);
        when(config.lootFiltersMenuColors()).thenReturn(true);
        when(config.groundIcons()).thenReturn(true);
        when(config.groundIconSource()).thenReturn(ImprovedMenusConfig.GroundIconSource.NATIVE);
        MenuEntry item=take("Shark",385,1,false); Menu menu=TestMenus.menu(item);
        Client client=mock(Client.class); when(client.getMenu()).thenReturn(menu);
        var icons=mock(net.runelite.client.game.ChatIconManager.class);
        when(icons.chatIconIndex(anyInt())).thenReturn(42);
        MenuPresentation iconPresentation=new MenuPresentation();
        field(iconPresentation,"client",client); field(iconPresentation,"config",config); field(iconPresentation,"icons",icons);
        iconPresentation.start();
        ImprovedMenusPlugin plugin=new ImprovedMenusPlugin();
        field(plugin,"client",client); field(plugin,"presentation",iconPresentation);
        field(plugin,"playerBadges",new PlayerMenuBadges(client,config,icons,mock(net.runelite.client.party.PartyService.class),mock(net.runelite.client.ui.overlay.tooltip.TooltipManager.class)));
        field(plugin,"lootMenu",presentation); field(plugin,"lootCompatibility",compatibility);
        var decorate=ImprovedMenusPlugin.class.getDeclaredMethod("decorateMenu"); decorate.setAccessible(true);
        for(int i=0;i<3;i++) {
            decorate.invoke(plugin);
            assertEquals("<img=42><col=00ffff>Shark</col>",item.getTarget());
        }
        when(config.lootFiltersMenuColors()).thenReturn(false); decorate.invoke(plugin);
        assertEquals("<img=42><col=ff0000>Shark</col>",item.getTarget());
        iconPresentation.stop(); presentation.stop();
        assertEquals("<col=ff0000>Shark</col>",item.getTarget());
    }
    private static void field(Object object,String name,Object value) throws Exception {
        var field=object.getClass().getDeclaredField(name);field.setAccessible(true);field.set(object,value);
    }
    private final ImprovedMenusConfig config=mock(ImprovedMenusConfig.class);
    private final LootFiltersCompatibility compatibility=mock(LootFiltersCompatibility.class);
    private final LootFiltersMenuPresentation presentation=com.google.inject.Guice.createInjector(new com.google.inject.AbstractModule() {
        protected void configure() { bind(Client.class).toInstance(mock(Client.class)); bind(ImprovedMenusConfig.class).toInstance(config); bind(LootFiltersCompatibility.class).toInstance(compatibility); }
    }).getInstance(LootFiltersMenuPresentation.class);
    private MenuEntry take(String name,int id,int sort,boolean hidden) {
        MenuEntry entry=TestMenus.entry("<col=00ff00>Take</col>","<col=ff0000>"+name+"</col>",MenuAction.GROUND_ITEM_THIRD_OPTION,id,0);
        when(entry.getIdentifier()).thenReturn(id); when(entry.isDeprioritized()).thenReturn(hidden);
        when(compatibility.find(entry)).thenReturn(new LootFiltersCompatibility.Result(null,false,hidden,hidden,sort,Color.CYAN));
        return entry;
    }
    @Test public void defaultsAndDisabledIntegrationLeaveMenuUntouched() {
        ImprovedMenusConfig defaults=new ImprovedMenusConfig() {};
        assertFalse(defaults.useLootFiltersInMenu()); assertFalse(defaults.lootFiltersMenuColors()); assertFalse(defaults.lootFiltersCompatibility());
        assertEquals(ImprovedMenusConfig.GroundIconSource.NATIVE,defaults.groundIconSource());
        assertEquals("false",SettingsCatalog.ALL.get("useLootFiltersInMenu").defaultValue);
        assertEquals("false",SettingsCatalog.ALL.get("lootFiltersMenuColors").defaultValue);
        MenuEntry item=take("Shark",385,100,true); clearInvocations(compatibility);
        Menu menu=TestMenus.menu(item); presentation.apply(menu);
        verifyNoInteractions(compatibility); verify(menu,never()).setMenuEntries(any());
        assertEquals("<col=ff0000>Shark</col>",item.getTarget());
    }
    @Test public void sortingPreservesObjectsPlayersUnresolvedItemsAndEveryAction() {
        when(config.useLootFiltersInMenu()).thenReturn(true);
        MenuEntry high=take("High",1,Integer.MAX_VALUE,false),low=take("Low",2,Integer.MIN_VALUE,false),hidden=take("Hidden",3,500,true);
        MenuEntry object=TestMenus.entry("Open","Chest",MenuAction.GAME_OBJECT_FIRST_OPTION,9,0);
        MenuEntry player=TestMenus.entry("Follow","Player",MenuAction.PLAYER_FIRST_OPTION,10,0);
        MenuEntry missing=take("Unknown",4,2,false); when(compatibility.find(missing)).thenReturn(null);
        MenuEntry cancel=TestMenus.entry("Cancel","",MenuAction.CANCEL,0,0);
        MenuEntry[] original={cancel,high,object,missing,low,player,hidden}; Menu menu=TestMenus.menu(original);
        presentation.apply(menu);
        assertArrayEquals(new MenuEntry[]{cancel,hidden,object,missing,low,player,high},menu.getMenuEntries());
        assertEquals("<col=ff0000>High</col>",high.getTarget()); assertEquals("<col=00ff00>Take</col>",high.getOption());
        verify(compatibility,never()).find(object); verify(compatibility,never()).find(player);
        for(MenuEntry e:original) { verify(e,never()).setType(any()); verify(e,never()).setDeprioritized(anyBoolean()); }
        presentation.stop(); assertArrayEquals(original,menu.getMenuEntries());
    }
    @Test public void colorsAreSeparatelyOptedInAndRestoredWithoutLosingOtherTags() {
        when(config.useLootFiltersInMenu()).thenReturn(true);
        MenuEntry item=take("Shark",385,1,false); item.setTarget("<img=5><col=ff0000>Shark (2)</col>"); Menu menu=TestMenus.menu(item);
        presentation.apply(menu); assertEquals("<img=5><col=ff0000>Shark (2)</col>",item.getTarget());
        when(config.lootFiltersMenuColors()).thenReturn(true); presentation.apply(menu);
        assertEquals("<col=00ffff><img=5>Shark (2)</col>",item.getTarget()); assertEquals("<col=00ff00>Take</col>",item.getOption());
        presentation.stop(); assertEquals("<img=5><col=ff0000>Shark (2)</col>",item.getTarget());
        presentation.apply(menu); item.setTarget("Other plugin edit"); presentation.stop(); assertEquals("Other plugin edit",item.getTarget());
    }
    @Test public void hiddenRowsKeepExistingColorAndPriorityEvenWhenColorsAreEnabled() {
        when(config.useLootFiltersInMenu()).thenReturn(true); when(config.lootFiltersMenuColors()).thenReturn(true);
        MenuEntry hidden=take("Hidden",3,5,true), visible=take("Visible",4,1,false);
        when(hidden.isDeprioritized()).thenReturn(false);
        Menu menu=TestMenus.menu(hidden,visible); presentation.apply(menu);
        assertArrayEquals(new MenuEntry[]{visible,hidden},menu.getMenuEntries());
        assertEquals("<col=ff0000>Hidden</col>",hidden.getTarget());
        verify(hidden,never()).setDeprioritized(anyBoolean());
    }
    @Test public void cleanupDoesNotUndoOtherPluginOrderingOrReusedEntryIdentity() {
        when(config.useLootFiltersInMenu()).thenReturn(true); when(config.lootFiltersMenuColors()).thenReturn(true);
        MenuEntry high=take("High",1,5,false),low=take("Low",2,1,false);
        Menu menu=TestMenus.menu(high,low); presentation.apply(menu);
        menu.setMenuEntries(new MenuEntry[]{high}); when(high.getWorldViewId()).thenReturn(2);
        String decorated=high.getTarget(); presentation.stop();
        assertArrayEquals(new MenuEntry[]{high},menu.getMenuEntries()); assertEquals(decorated,high.getTarget());
    }
}
