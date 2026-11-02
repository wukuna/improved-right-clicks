package com.improvedmenus;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PanelLayoutTest {
    @Test public void compactTabsKeepSettingsUndoAndDiagnosticsAvailable() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            try {
                UIManager.setLookAndFeel(new net.runelite.client.ui.laf.RuneLiteLAF());
                ProfileStore store=new ProfileStore();MenuEngine engine=new MenuEngine();
                Runnable settings=mock(Runnable.class);
                ImprovedMenusPanel panel=new ImprovedMenusPanel(store,engine,settings,()->"Ready");
                try {
                    panel.setSize(225,650);layout(panel);
                    JTabbedPane tabs=find(panel,JTabbedPane.class);
                    assertEquals(3,tabs.getTabCount());assertEquals(1,tabs.getTabRunCount());
                    assertTrue(tabs.getBoundsAt(2).getMaxX()<=tabs.getWidth());
                    button(panel,"Open plugin settings").doClick();verify(settings).run();
                    tabs.setSelectedIndex(1);button(panel,"Undo last change").doClick();
                    tabs.setSelectedIndex(2);button(panel,"Diagnostics").doClick();
                    assertTrue(find(panel,javax.swing.JTextArea.class).getText().contains("Ready"));
                    tabs.setSelectedIndex(0);layout(panel);
                    BufferedImage image=new BufferedImage(225,650,BufferedImage.TYPE_INT_RGB);
                    Graphics2D graphics=image.createGraphics();panel.printAll(graphics);graphics.dispose();
                    ImageIO.write(image,"png",new java.io.File("build/panel-preview.png"));
                } finally { panel.stop(); }
            } catch(Exception e) {throw new AssertionError(e);}
        });
    }
    private static AbstractButton button(Container parent,String text) {
        for(Component child:parent.getComponents()) {
            if(child instanceof AbstractButton && ((AbstractButton)child).getText().contains(text))return (AbstractButton)child;
            if(child instanceof Container) {AbstractButton found=button((Container)child,text);if(found!=null)return found;}
        }return null;
    }
    private static <T extends Component> T find(Container parent,Class<T> type) {
        for(Component child:parent.getComponents()) {
            if(type.isInstance(child))return type.cast(child);
            if(child instanceof Container) {T found=find((Container)child,type);if(found!=null)return found;}
        }return null;
    }
    private static void layout(Container parent) {
        parent.doLayout();for(Component child:parent.getComponents())if(child instanceof Container)layout((Container)child);
    }
}
