package com.improvedmenus;

import java.awt.Color;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import net.runelite.api.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.plugins.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ImprovedLootingAdapterTest
{
    @Test public void lootingOptInUsesOwnNamespaceAndFallsBackToExistingProviderWhenOff()
    {
        Client client=mock(Client.class);when(client.isClientThread()).thenReturn(true);ImprovedMenusConfig config=mock(ImprovedMenusConfig.class);when(config.lootFiltersCompatibility()).thenReturn(true);PluginManager plugins=mock(PluginManager.class);EventBus events=mock(EventBus.class);
        Plugin looting=mock(Plugin.class),filters=mock(Plugin.class);when(looting.getName()).thenReturn("Improved Looting TEST");when(filters.getName()).thenReturn("Loot Filters TEST");when(plugins.getPlugins()).thenReturn(Arrays.asList(looting,filters));when(plugins.isPluginActive(any(Plugin.class))).thenReturn(true);
        AtomicBoolean optIn=new AtomicBoolean(true);List<String> requested=new ArrayList<>();
        doAnswer(i->{PluginMessage message=i.getArgument(0);if(message.getName().equals("provider-status-v1"))message.getData().put("enabled",optIn.get());if(message.getName().equals("resolved-display-request-v1")){requested.add(message.getNamespace());message.getData().put("status","ok");message.getData().put("display",Map.of("hidden",false,"hideOverlay",false,"menuSort",2,"menuTextColor",Color.CYAN));}return null;}).when(events).post(any(PluginMessage.class));
        LootFiltersCompatibility compatibility=new LootFiltersCompatibility(client,mock(ClientThread.class),config,plugins,events);WorldView view=mock(WorldView.class);
        compatibility.refresh();assertEquals(2,compatibility.request(view,0,0,0,385).menuSort);assertEquals("improved-looting",requested.get(0));assertTrue(compatibility.status().contains("Improved Looting"));
        optIn.set(false);compatibility.refresh();compatibility.request(view,0,0,0,385);assertEquals("loot-filters",requested.get(1));assertTrue(compatibility.status().contains("Loot Filters"));
        when(plugins.isPluginActive(any(Plugin.class))).thenReturn(false);compatibility.refresh();assertTrue(compatibility.status().startsWith("Enable"));
    }
}
