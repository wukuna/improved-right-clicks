package com.improvedmenus;

import com.google.gson.Gson;
import com.google.inject.*;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ChatIconManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import org.junit.Test;
import static org.mockito.Mockito.*;

public class PluginLifecycleTest
{
    @Test public void disableReenableDoesNotLeakListenersOverlaysOrIcons() throws Exception
    {
        Client client=mock(Client.class); ClientThread thread=mock(ClientThread.class); ImprovedMenusConfig config=mock(ImprovedMenusConfig.class);
        ConfigManager manager=mock(ConfigManager.class); EventBus bus=mock(EventBus.class); MouseManager mouse=mock(MouseManager.class);
        ItemIcons library=mock(ItemIcons.class); library.configs=manager; library.filters=mock(FilterIcons.class);
        when(library.filters.status()).thenReturn("No imported filter active");
        when(manager.getConfig(ImprovedMenusConfig.class)).thenReturn(config);
        ClientToolbar toolbar=mock(ClientToolbar.class); OverlayManager overlays=mock(OverlayManager.class); ChatIconManager icons=mock(ChatIconManager.class);
        when(config.itemRules()).thenReturn(""); when(config.colors()).thenReturn(""); when(config.exceptions()).thenReturn("");
        doAnswer(i -> { ((Runnable)i.getArgument(0)).run(); return null; }).when(thread).invoke(any(Runnable.class));
        doAnswer(i -> { ((Runnable)i.getArgument(0)).run(); return null; }).when(thread).invokeLater(any(Runnable.class));
        Injector injector=Guice.createInjector(new AbstractModule() { protected void configure() { bind(net.runelite.client.party.PartyService.class).toInstance(mock(net.runelite.client.party.PartyService.class)); bind(net.runelite.client.hiscore.HiscoreClient.class).toInstance(mock(net.runelite.client.hiscore.HiscoreClient.class)); bind(LootFiltersCompatibility.class).toInstance(mock(LootFiltersCompatibility.class));
            bind(net.runelite.client.chat.ChatMessageManager.class).toInstance(mock(net.runelite.client.chat.ChatMessageManager.class));
            bind(net.runelite.client.config.RuneLiteConfig.class).toInstance(mock(net.runelite.client.config.RuneLiteConfig.class)); bind(Client.class).toInstance(client); bind(ClientThread.class).toInstance(thread); bind(ImprovedMenusConfig.class).toInstance(config);
            bind(net.runelite.client.game.SpriteManager.class).toInstance(mock(net.runelite.client.game.SpriteManager.class)); bind(java.util.concurrent.ScheduledExecutorService.class).toInstance(mock(java.util.concurrent.ScheduledExecutorService.class)); bind(ItemIcons.class).toInstance(library); bind(ConfigManager.class).toInstance(manager); bind(Gson.class).toInstance(new Gson()); bind(EventBus.class).toInstance(bus);
            bind(MouseManager.class).toInstance(mouse); bind(ClientToolbar.class).toInstance(toolbar); bind(OverlayManager.class).toInstance(overlays);
            bind(ChatIconManager.class).toInstance(icons); bind(ItemManager.class).toInstance(mock(ItemManager.class)); bind(TooltipManager.class).toInstance(mock(TooltipManager.class));
        } });
        ImprovedMenusPlugin plugin=injector.getInstance(ImprovedMenusPlugin.class);
        for (int i=0;i<2;i++) { plugin.startUp(); SwingUtilities.invokeAndWait(() -> {}); plugin.shutDown(); SwingUtilities.invokeAndWait(() -> {}); }
        verify(mouse,times(2)).registerMouseListener(any()); verify(mouse,times(2)).unregisterMouseListener(any());
        verify(bus,times(2)).register(any(ButtonGuard.class)); verify(bus,times(2)).unregister(any(ButtonGuard.class));
        verify(overlays,times(2)).add(any(OptionCountOverlay.class)); verify(overlays,times(2)).remove(any(OptionCountOverlay.class));
        verify(overlays,times(2)).add(any(MenuPresentation.class)); verify(overlays,times(2)).remove(any(MenuPresentation.class));
        verify(toolbar,times(2)).addNavigation(any(NavigationButton.class)); verify(toolbar,times(2)).removeNavigation(any(NavigationButton.class));
        verify(icons,times(5)).registerChatIcon(any());
        when(config.hideSidePanel()).thenReturn(true);
        plugin.startUp(); SwingUtilities.invokeAndWait(() -> {});
        verify(toolbar,times(2)).addNavigation(any(NavigationButton.class));
        ConfigChanged changed=new ConfigChanged(); changed.setGroup(ImprovedMenusConfig.GROUP); changed.setKey("hideSidePanel");
        when(config.hideSidePanel()).thenReturn(false);
        plugin.onConfigChanged(changed); plugin.onConfigChanged(changed); SwingUtilities.invokeAndWait(() -> {});
        verify(toolbar,times(3)).addNavigation(any(NavigationButton.class));
        when(config.hideSidePanel()).thenReturn(true);
        plugin.onConfigChanged(changed); SwingUtilities.invokeAndWait(() -> {});
        verify(toolbar,times(3)).removeNavigation(any(NavigationButton.class));
        // Hiding the toolbar must not disable menu guards or presentation.
        verify(bus,times(2)).unregister(any(ButtonGuard.class)); verify(overlays,times(2)).remove(any(MenuPresentation.class));
        when(config.hideSidePanel()).thenReturn(false);
        plugin.onConfigChanged(changed); plugin.shutDown(); SwingUtilities.invokeAndWait(() -> {});
        verify(toolbar,times(3)).addNavigation(any(NavigationButton.class));
        verify(bus,times(3)).unregister(any(ButtonGuard.class));
    }
}
