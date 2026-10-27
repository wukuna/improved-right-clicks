package com.improvedmenus;

import net.runelite.api.*;
import com.google.inject.*;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ChatIconManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class PresentationTest
{
    @Test public void additionalOptionsExcludeWalkCancelAndLocalControls()
    {
        assertEquals(1,MenuPresentation.additionalCount(new MenuEntry[]{
            TestMenus.entry("Cancel","",MenuAction.CANCEL,-1,0),TestMenus.entry("Walk here","",MenuAction.WALK,-1,0),
            TestMenus.entry("Colors","",MenuAction.RUNELITE,-1,0),TestMenus.entry("Take","Coins",MenuAction.GROUND_ITEM_THIRD_OPTION,-1,0),
            TestMenus.entry("Examine","Coins",MenuAction.EXAMINE_ITEM_GROUND,-1,0)}));
    }
    @Test public void countNeverNegative() { assertEquals(0,MenuPresentation.additionalCount(new MenuEntry[0])); }
    @Test public void examineSwitchRecognizesNpcObjectGroundAndInventoryExamines()
    {
        MenuEntry action=TestMenus.entry("Talk-to","NPC",MenuAction.NPC_FIRST_OPTION,-1,0);
        MenuEntry[] entries={action,TestMenus.entry("Examine","NPC",MenuAction.EXAMINE_NPC,-1,0),
            TestMenus.entry("Examine","Tree",MenuAction.EXAMINE_OBJECT,-1,0),TestMenus.entry("Examine","Coins",MenuAction.EXAMINE_ITEM_GROUND,-1,0),
            TestMenus.entry("<col=ffffff>Examine</col>","Item",MenuAction.CC_OP_LOW_PRIORITY,1,1)};
        assertEquals(4,MenuPresentation.additionalCount(entries,true,true)); assertEquals(0,MenuPresentation.additionalCount(entries,false,true));
    }
    @Test public void widgetsCanBeExcludedWithoutHidingWorldCounts()
    {
        MenuEntry widget=TestMenus.entry("Withdraw","Coins",MenuAction.CC_OP,1,1); when(widget.getWidget()).thenReturn(mock(Widget.class));
        MenuEntry talk=TestMenus.entry("Talk-to","NPC",MenuAction.NPC_FIRST_OPTION,-1,0), examine=TestMenus.entry("Examine","NPC",MenuAction.EXAMINE_NPC,-1,0);
        assertEquals(2,MenuPresentation.additionalCount(new MenuEntry[]{widget,talk,examine},true,true));
        assertEquals(1,MenuPresentation.additionalCount(new MenuEntry[]{widget,talk,examine},true,false));
        assertEquals(0,MenuPresentation.additionalCount(new MenuEntry[]{widget},true,false));
    }
    @Test public void renderHonorsThresholdAndUsesConfiguredTextColor()
    {
        Client client=mock(Client.class); ImprovedMenusConfig config=mock(ImprovedMenusConfig.class); TooltipManager tooltips=mock(TooltipManager.class);
        Menu menu=TestMenus.menu(TestMenus.entry("Talk-to","NPC",MenuAction.NPC_FIRST_OPTION,-1,0),TestMenus.entry("Examine","NPC",MenuAction.EXAMINE_NPC,-1,0));
        when(client.getMenu()).thenReturn(menu);
        when(client.getMouseCanvasPosition()).thenReturn(new Point(10,10)); when(config.optionCount()).thenReturn(true);
        when(config.countExamine()).thenReturn(true); when(config.countOnWidgets()).thenReturn(true); when(config.minimumOptions()).thenReturn(2);
        when(config.optionCountColor()).thenReturn("#123abc");
        MenuPresentation presentation=Guice.createInjector(new AbstractModule() { protected void configure() {
            bind(net.runelite.client.config.RuneLiteConfig.class).toInstance(mock(net.runelite.client.config.RuneLiteConfig.class)); bind(Client.class).toInstance(client); bind(ImprovedMenusConfig.class).toInstance(config); bind(TooltipManager.class).toInstance(tooltips);
            bind(ItemManager.class).toInstance(mock(ItemManager.class)); bind(ChatIconManager.class).toInstance(mock(ChatIconManager.class));
        } }).getInstance(MenuPresentation.class);
        presentation.queueOptionCount(); verifyNoInteractions(tooltips);
        when(config.minimumOptions()).thenReturn(1); presentation.queueOptionCount();
        ArgumentCaptor<Tooltip> tooltip=ArgumentCaptor.forClass(Tooltip.class); verify(tooltips).add(tooltip.capture());
        assertEquals("<col=123abc>+1 options</col>",tooltip.getValue().getText());
        for (ImprovedMenusConfig.CountAnchor anchor : ImprovedMenusConfig.CountAnchor.values())
        {
            clearInvocations(tooltips); when(config.countAnchor()).thenReturn(anchor); presentation.queueOptionCount();
            if (anchor==ImprovedMenusConfig.CountAnchor.ABOVE_LEFT || anchor==ImprovedMenusConfig.CountAnchor.ABOVE_RIGHT)
            { verify(tooltips).addFront(any(Tooltip.class)); verify(tooltips,never()).add(any(Tooltip.class)); }
            else { verify(tooltips).add(any(Tooltip.class)); verify(tooltips,never()).addFront(any(Tooltip.class)); }
        }
        clearInvocations(tooltips); when(client.isMenuOpen()).thenReturn(true); presentation.queueOptionCount(); verifyNoInteractions(tooltips);
    }
    @Test public void colorFallbackHandlesInvalidManualEdits()
    {
        assertEquals(new java.awt.Color(0x123abc),SettingsCatalog.colorOrDefault("#123abc",java.awt.Color.GREEN));
        assertEquals(java.awt.Color.GREEN,SettingsCatalog.colorOrDefault("invalid",java.awt.Color.GREEN));
        assertEquals(java.awt.Color.WHITE,SettingsCatalog.colorOrDefault(null,java.awt.Color.WHITE));
    }
}
