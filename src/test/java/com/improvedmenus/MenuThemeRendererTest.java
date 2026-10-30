package com.improvedmenus;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import net.runelite.api.Client;
import net.runelite.api.Menu;
import net.runelite.api.MenuEntry;
import net.runelite.api.Rasterizer;
import net.runelite.api.events.BeforeMenuRender;
import net.runelite.client.ui.FontManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class MenuThemeRendererTest
{
    private final Client client=mock(Client.class);
    private final ImprovedMenusConfig config=mock(ImprovedMenusConfig.class);
    private final Menu menu=mock(Menu.class);
    private final MenuEntry entry=mock(MenuEntry.class);
    private final Rasterizer raster=mock(Rasterizer.class);
    private final MenuThemeRenderer renderer=new MenuThemeRenderer(client,config);

    @Before public void setup()
    {
        when(client.isMenuOpen()).thenReturn(true);when(client.getMenu()).thenReturn(menu);
        when(client.getRasterizer()).thenReturn(raster);
        when(config.menuTheme()).thenReturn(ImprovedMenusConfig.MenuTheme.OSRS);
        when(menu.getMenuEntries()).thenReturn(new MenuEntry[]{entry});
        when(menu.getMenuX()).thenReturn(20);when(menu.getMenuY()).thenReturn(20);
        when(menu.getMenuWidth()).thenReturn(240);when(menu.getMenuHeight()).thenReturn(82);
    }

    @Test public void drawsOnlySurfaceBeforeDelegatingAllRowsToNativeRenderer()
    {
        BeforeMenuRender event=new BeforeMenuRender();renderer.render(event);assertTrue(event.isConsumed());
        InOrder order=inOrder(raster,client);
        order.verify(raster).fillRectangle(20,20,240,82,MenuThemeRenderer.EDGE);
        order.verify(client).drawOriginalMenu(0);
        verify(menu,never()).setMenuEntries(any());verify(entry).getSubMenu();verifyNoMoreInteractions(entry);
        verify(client,never()).setMenuScroll(anyInt());
    }

    @Test public void nativeClosedAndInvalidMenusAreUntouched()
    {
        when(config.menuTheme()).thenReturn(ImprovedMenusConfig.MenuTheme.NATIVE);
        BeforeMenuRender event=new BeforeMenuRender();renderer.render(event);assertFalse(event.isConsumed());
        when(config.menuTheme()).thenReturn(ImprovedMenusConfig.MenuTheme.OSRS);
        when(client.isMenuOpen()).thenReturn(false);renderer.render(event);assertFalse(event.isConsumed());
        when(client.isMenuOpen()).thenReturn(true);when(menu.getMenuWidth()).thenReturn(0);
        renderer.render(event);assertFalse(event.isConsumed());
        verifyNoInteractions(raster);verify(client,never()).drawOriginalMenu(anyInt());
    }

    @Test public void nestedAndAlreadyDrawnMenusRetainTheirRenderer()
    {
        when(entry.getSubMenu()).thenReturn(mock(Menu.class));
        BeforeMenuRender event=new BeforeMenuRender();renderer.render(event);assertFalse(event.isConsumed());
        when(entry.getSubMenu()).thenReturn(null);event.consume();renderer.render(event);
        verifyNoInteractions(raster);verify(client,never()).drawOriginalMenu(anyInt());
    }

    @Test public void changingThemeTakesEffectWithoutLeavingStateToRestore()
    {
        renderer.render(new BeforeMenuRender());clearInvocations(raster,client);
        when(config.menuTheme()).thenReturn(ImprovedMenusConfig.MenuTheme.NATIVE);
        renderer.render(new BeforeMenuRender());verifyNoInteractions(raster);verify(client,never()).drawOriginalMenu(anyInt());
        when(config.menuTheme()).thenReturn(ImprovedMenusConfig.MenuTheme.OSRS);
        renderer.render(new BeforeMenuRender());verify(client).drawOriginalMenu(0);
    }

    @Test public void surfaceMatchesLootCardsAndStaysInsideNativeMenuBounds() throws Exception
    {
        BufferedImage image=new BufferedImage(300,145,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
        g.setColor(new Color(35,38,32));g.fillRect(0,0,300,145);
        doAnswer(call -> {g.setColor(new Color(call.getArgument(4)));g.fillRect(call.getArgument(0),call.getArgument(1),call.getArgument(2),call.getArgument(3));return null;})
            .when(raster).fillRectangle(anyInt(),anyInt(),anyInt(),anyInt(),anyInt());
        // Sample text is for the preview only; production delegates to RuneLite.
        doAnswer(call -> {
            g.setFont(FontManager.getRunescapeBoldFont());g.setColor(new Color(0x5d5447));g.drawString("Choose Option",23,34);
            g.setFont(FontManager.getRunescapeFont());
            String[] names={"Take Abyssal whip","Take Rune platebody","Walk here","Cancel"};
            Color[] colors={new Color(0xc080ff),new Color(0xffdc40),Color.WHITE,Color.WHITE};
            for(int i=0;i<names.length;i++){g.setColor(Color.BLACK);g.drawString(names[i],24,52+15*i);g.setColor(colors[i]);g.drawString(names[i],23,51+15*i);}
            return null;
        }).when(client).drawOriginalMenu(0);
        renderer.render(new BeforeMenuRender());
        assertEquals(new Color(55,44,28).getRGB(),image.getRGB(250,60));
        assertEquals(new Color(155,130,81).getRGB(),image.getRGB(21,21));
        assertEquals(new Color(35,38,32).getRGB(),image.getRGB(19,50));
        assertEquals(new Color(35,38,32).getRGB(),image.getRGB(260,50));
        g.setColor(Color.LIGHT_GRAY);g.drawString("Theme renderer preview - sample rows",15,130);g.dispose();
        ImageIO.write(image,"png",new java.io.File("build/osrs-menu-preview.png"));
    }
}
