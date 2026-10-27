package com.improvedmenus;

import com.google.gson.Gson;
import java.util.Arrays;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class FilterVisibilityTest {
    @Test public void identicalStacksWithDifferentVisibilityDoNotLeakAnIcon() {
        FilterIcons filter=new FilterIcons(); filter.gson=new Gson(); filter.configs=mock(ConfigManager.class);
        filter.client=mock(Client.class); filter.items=mock(ItemManager.class);
        WorldView view=mock(WorldView.class); Scene scene=mock(Scene.class); Tile tile=mock(Tile.class);
        when(filter.client.getWorldView(anyInt())).thenReturn(view); when(filter.client.getTopLevelWorldView()).thenReturn(view);
        when(view.getScene()).thenReturn(scene); when(scene.getTiles()).thenReturn(new Tile[][][]{{{tile}}});
        when(tile.getLocalLocation()).thenReturn(new LocalPoint(64,64));
        ItemComposition composition=mock(ItemComposition.class); when(composition.getName()).thenReturn("Shark");
        when(composition.getLinkedNoteId()).thenReturn(-1); when(composition.getNote()).thenReturn(-1);
        when(filter.items.getItemComposition(385)).thenReturn(composition); when(filter.client.getItemCount()).thenReturn(50000);
        TileItem small=mock(TileItem.class),large=mock(TileItem.class);
        when(small.getId()).thenReturn(385); when(small.getQuantity()).thenReturn(1);
        when(large.getId()).thenReturn(385); when(large.getQuantity()).thenReturn(10);
        FilterIcons.Bundle bundle=new FilterIcons.Bundle();
        bundle.compressedSource=FilterIcons.compress("apply(true) {icon=Item(995);} rule(quantity:>5) {hidden=true;} rule(true) {}");
        filter.save(filter.prepare(bundle));
        MenuEntry entry=TestMenus.entry("Take","Shark",MenuAction.GROUND_ITEM_THIRD_OPTION,385,0);
        when(entry.getIdentifier()).thenReturn(385); when(entry.getParam0()).thenReturn(0); when(entry.getParam1()).thenReturn(0);
        when(tile.getGroundItems()).thenReturn(Arrays.asList(small)); assertNotNull(filter.find(entry));
        when(tile.getGroundItems()).thenReturn(Arrays.asList(large)); assertNull(filter.find(entry));
        when(tile.getGroundItems()).thenReturn(Arrays.asList(small,large)); assertNull(filter.find(entry));
        when(tile.getGroundItems()).thenReturn(Arrays.asList(large,small)); assertNull(filter.find(entry));
    }
}
