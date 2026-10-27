package com.improvedmenus;

import com.google.gson.Gson;
import com.google.inject.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.*;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PanelTest
{
    @Test public void sidePanelOffersToolsWithoutDuplicateSettings() throws Exception
    {
        Client client=mock(Client.class); ImprovedMenusConfig config=mock(ImprovedMenusConfig.class); ConfigManager manager=mock(ConfigManager.class);
        java.util.Map<String,String> values=new java.util.HashMap<>();
        when(manager.getConfiguration(anyString(),anyString())).thenAnswer(i -> values.get(i.getArgument(0)+"/"+i.getArgument(1)));
        doAnswer(i -> { values.put(i.getArgument(0)+"/"+i.getArgument(1),i.getArgument(2).toString()); return null; }).when(manager).setConfiguration(anyString(),anyString(),any());
        Injector injector=Guice.createInjector(new AbstractModule() { protected void configure() {
            bind(net.runelite.client.game.ItemManager.class).toInstance(mock(net.runelite.client.game.ItemManager.class)); bind(Client.class).toInstance(client); bind(ImprovedMenusConfig.class).toInstance(config); bind(ConfigManager.class).toInstance(manager); bind(Gson.class).toInstance(new Gson());
        } });
        SwingUtilities.invokeAndWait(() -> {
            Runnable openSettings=mock(Runnable.class);
            ImprovedMenusPanel panel=new ImprovedMenusPanel(injector.getInstance(ProfileStore.class),injector.getInstance(MenuEngine.class),openSettings);
            components(panel,JButton.class).stream().filter(b -> "Open Plugin Settings".equals(b.getText())).findFirst().get().doClick();
            verify(openSettings).run();
            panel.run(() -> {});
            assertFalse(components(panel,JLabel.class).stream().anyMatch(label -> label.getText().contains("Saved")));
            panel.applySettings(java.util.Collections.singletonMap("potionEmpty","false"));
            assertTrue(components(panel,JLabel.class).stream().anyMatch(label -> "No changes".equals(label.getText())));
            clearInvocations(manager);
            assertEquals(0,components(panel,JCheckBox.class).size());
            assertEquals(0,components(panel,JTextField.class).size());
            assertEquals(0,components(panel,JSpinner.class).size());
            assertEquals(3,components(panel,JButton.class).stream().filter(b -> java.util.Arrays.asList("Profiles","Import/export","Undo").contains(b.getText())).count());
            verifyNoInteractions(manager);
            ProfileStore store=injector.getInstance(ProfileStore.class);
            panel.applySettings(java.util.Collections.singletonMap("potionEmpty","true"));
            assertTrue(components(panel,JLabel.class).stream().anyMatch(label -> "Settings applied".equals(label.getText())));
            panel.run(() -> {});
            assertTrue(components(panel,JLabel.class).stream().anyMatch(label -> "Settings applied".equals(label.getText())));
            clearInvocations(manager);
            components(panel,JButton.class).stream().filter(b -> "Undo".equals(b.getText())).findFirst().get().doClick();
            verify(manager).setConfiguration("improvedmenus","potionEmpty","false");
            assertTrue(components(panel,JLabel.class).stream().anyMatch(label -> "Undone".equals(label.getText())));
            ItemIcons library=mock(ItemIcons.class); library.configs=manager; library.filters=mock(FilterIcons.class);
            when(library.filters.status()).thenReturn("484 imported rules; 0 embedded images");
            when(manager.getConfig(ImprovedMenusConfig.class)).thenReturn(config);
            when(config.groundIconSource()).thenReturn(ImprovedMenusConfig.GroundIconSource.NATIVE);
            clearInvocations(manager);
            panel.addIconControls(library);
            verify(manager,never()).setConfiguration(anyString(),anyString(),any());
            components(panel,JCheckBox.class).get(0).doClick();
            verify(manager).setConfiguration("improvedmenus","groundIcons",true);
            components(panel,JComboBox.class).get(0).setSelectedItem(ImprovedMenusConfig.GroundIconSource.BOTH);
            verify(manager).setConfiguration("improvedmenus","groundIconSource",ImprovedMenusConfig.GroundIconSource.BOTH);
            panel.setSize(225,850); layout(panel);
            BufferedImage image=new BufferedImage(225,850,BufferedImage.TYPE_INT_RGB); Graphics2D g=image.createGraphics(); panel.printAll(g); g.dispose();
            try { ImageIO.write(image,"png",new File("build/ui-preview.png")); } catch (java.io.IOException e) { throw new AssertionError(e); }
            assertEquals(2,panel.getComponentCount()); panel.stop();
        });
    }
    private static void layout(Container container) { container.doLayout(); for (Component c : container.getComponents()) { if (c instanceof Container) { layout((Container)c); } } }
    private static <T extends Component> java.util.List<T> components(Container container,Class<T> type)
    {
        java.util.List<T> result=new java.util.ArrayList<>();
        for (Component c : container.getComponents()) { if (type.isInstance(c)) { result.add(type.cast(c)); } if (c instanceof Container) { result.addAll(components((Container)c,type)); } }
        return result;
    }
}
