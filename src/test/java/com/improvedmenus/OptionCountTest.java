package com.improvedmenus;

import java.awt.*;
import java.awt.image.BufferedImage;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class OptionCountTest
{
    private MenuEntry entry(MenuAction type) { return TestMenus.entry("Action","Target",type,-1,0); }
    @Test public void playerOnTopDoesNotHideSingleLootOrObjectAction()
    {
        for (MenuAction type : new MenuAction[]{MenuAction.GROUND_ITEM_THIRD_OPTION,MenuAction.GAME_OBJECT_FIRST_OPTION})
        {
            MenuEntry[] entries={entry(type),entry(MenuAction.PLAYER_FIRST_OPTION)};
            assertEquals(1,MenuPresentation.additionalCount(entries,true,true,true,false));
            assertEquals(type,entries[0].getType());
            verify(entries[0],never()).setType(any());
        }
    }
    @Test public void allPlayerActionsAreExcludedIncludingUseAndPluginEntries()
    {
        for (MenuAction type : MenuAction.values())
        {
            if (type.name().contains("PLAYER"))
            {
                assertEquals(type.name(),0,MenuPresentation.additionalCount(new MenuEntry[]{entry(type),entry(type)},true,true,true,false));
            }
        }
    }
    @Test public void walkingHidesNpcAndObjectsButKeepsLootAndWidgets()
    {
        MenuEntry[] entries={entry(MenuAction.EXAMINE_NPC),entry(MenuAction.NPC_FIRST_OPTION),
            entry(MenuAction.GAME_OBJECT_FIRST_OPTION),entry(MenuAction.EXAMINE_OBJECT),
            entry(MenuAction.GROUND_ITEM_THIRD_OPTION),entry(MenuAction.CC_OP),entry(MenuAction.WALK)};
        assertEquals(2,MenuPresentation.additionalCount(entries,true,true,false,true));
        assertEquals(5,MenuPresentation.additionalCount(entries,true,true,false,false));
    }
    @Test public void mixedPlayerWalkObjectAndLootDoesNotSuppressLoot()
    {
        MenuEntry[] entries={entry(MenuAction.GROUND_ITEM_THIRD_OPTION),entry(MenuAction.GAME_OBJECT_FIRST_OPTION),
            entry(MenuAction.WALK),entry(MenuAction.PLAYER_FIRST_OPTION)};
        assertEquals(1,MenuPresentation.additionalCount(entries,true,true,true,true));
    }
    @Test public void finalPreferredObjectIsNotSuppressedByWalkFurtherDown()
    {
        assertEquals(1,MenuPresentation.additionalCount(new MenuEntry[]{entry(MenuAction.EXAMINE_OBJECT),
            entry(MenuAction.WALK),entry(MenuAction.GAME_OBJECT_FIRST_OPTION)},true,true,false,true));
    }
    @Test public void defaultsAndProfileChoicesRemainCompatible()
    {
        ImprovedMenusConfig config=new ImprovedMenusConfig() {};
        assertFalse(config.countHidePlayers()); assertFalse(config.countHideWalkHere());
        assertEquals(ImprovedMenusConfig.CountAnchor.BELOW_LEFT,config.countAnchor());
        for (ImprovedMenusConfig.CountAnchor anchor:ImprovedMenusConfig.CountAnchor.values())
        { SettingsCatalog.ALL.get("countAnchor").validate(anchor.name()); }
    }
    @Test public void rightAnchorReservesStackWidthAndClampsToCanvas()
    {
        Client client=mock(Client.class); when(client.getCanvasWidth()).thenReturn(400); when(client.getCanvasHeight()).thenReturn(100);
        TooltipManager manager=new TooltipManager(); manager.add(new Tooltip("A much wider target action tooltip"));
        OptionCountTooltip row=new OptionCountTooltip("+2 options",manager,client,Color.BLACK);
        manager.add(new Tooltip(row));
        BufferedImage image=new BufferedImage(400,100,BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics=image.createGraphics();
        row.setPreferredLocation(new Point(10,20)); Dimension size=row.render(graphics);
        assertTrue(size.width>graphics.getFontMetrics().stringWidth("+2 options")+8);
        assertEquals(0,image.getRGB(10,20));
        row.setPreferredLocation(new Point(399,99)); row.render(graphics);
        assertNotEquals(0,image.getRGB(398,98));
        assertEquals(2,manager.getTooltips().size());
        graphics.dispose();
    }
}
