package com.improvedmenus;

import com.google.inject.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.game.ChatIconManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.tooltip.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class OptionCountOrderingTest
{
    @Test public void anchorsSurviveMouseHighlightAndRenderInSameFrame() throws Exception
    {
        Client client=mock(Client.class);
        ImprovedMenusConfig config=mock(ImprovedMenusConfig.class);
        RuneLiteConfig core=mock(RuneLiteConfig.class);
        when(core.overlayBackgroundColor()).thenReturn(Color.BLACK);
        when(client.getCanvasWidth()).thenReturn(500);
        when(client.getCanvasHeight()).thenReturn(300);
        when(client.getMouseCanvasPosition()).thenReturn(new net.runelite.api.Point(50,50));
        net.runelite.api.Menu menu=TestMenus.menu(
            TestMenus.entry("Take","Coins",MenuAction.GROUND_ITEM_FIRST_OPTION,-1,0),
            TestMenus.entry("Examine","Coins",MenuAction.EXAMINE_ITEM_GROUND,-1,0));
        when(client.getMenu()).thenReturn(menu);
        when(config.optionCount()).thenReturn(true);
        when(config.countExamine()).thenReturn(true);
        when(config.countOnWidgets()).thenReturn(true);
        when(config.optionCountColor()).thenReturn("#ff0000");
        Injector injector=Guice.createInjector(new AbstractModule() {
            protected void configure() {
                bind(Client.class).toInstance(client);
                bind(ImprovedMenusConfig.class).toInstance(config);
                bind(RuneLiteConfig.class).toInstance(core);
                bind(ItemManager.class).toInstance(mock(ItemManager.class));
                bind(ChatIconManager.class).toInstance(mock(ChatIconManager.class));
            }
        });
        TooltipManager manager=injector.getInstance(TooltipManager.class);
        Overlay producer=new Overlay() {
            public Dimension render(Graphics2D graphics) {
                // MouseHighlightOverlay inserts at the front during its dynamic pass.
                manager.addFront(new Tooltip("<col=00ff00>Take Coins</col>"));
                return null;
            }
        };
        producer.setPosition(OverlayPosition.DYNAMIC);
        producer.setLayer(OverlayLayer.ABOVE_WIDGETS);
        OptionCountOverlay count=injector.getInstance(OptionCountOverlay.class);
        TooltipOverlay renderer=injector.getInstance(TooltipOverlay.class);
        List<Overlay> overlays=Arrays.asList(renderer,count,producer);
        Field comparator=OverlayManager.class.getDeclaredField("OVERLAY_COMPARATOR");
        comparator.setAccessible(true);
        overlays.sort((Comparator<Overlay>)comparator.get(null));
        assertEquals(Arrays.asList(producer,count,renderer),overlays);
        assertEquals(renderer.getLayer(),count.getLayer());
        for (ImprovedMenusConfig.CountAnchor anchor:ImprovedMenusConfig.CountAnchor.values())
        {
            when(config.countAnchor()).thenReturn(anchor);
            for (int frame=0;frame<2;frame++)
            {
                BufferedImage image=new BufferedImage(500,300,BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics=image.createGraphics();
                try { for (Overlay overlay:overlays) { overlay.render(graphics); } }
                finally { graphics.dispose(); }
                int countY=firstRow(image,Color.RED.getRGB());
                int actionY=firstRow(image,Color.GREEN.getRGB());
                assertTrue("Both tooltips must render in this frame",countY>=0 && actionY>=0);
                boolean above=anchor==ImprovedMenusConfig.CountAnchor.ABOVE_LEFT || anchor==ImprovedMenusConfig.CountAnchor.ABOVE_RIGHT;
                assertEquals(anchor.toString(),above,countY<actionY);
                assertTrue("No stale tooltip in the next frame",manager.getTooltips().isEmpty());
            }
        }
    }

    private static int firstRow(BufferedImage image,int color)
    {
        for (int y=0;y<image.getHeight();y++)
            for (int x=0;x<image.getWidth();x++)
                if (image.getRGB(x,y)==color) { return y; }
        return -1;
    }
}
