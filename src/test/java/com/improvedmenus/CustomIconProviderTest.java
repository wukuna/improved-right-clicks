package com.improvedmenus;

import com.improvedmenus.ImprovedMenusConfig.CustomIconProvider;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.runelite.api.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.plugins.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CustomIconProviderTest
{
	@Test
	public void offIgnoresLegacyOptInAndDoesNotContactEitherProvider()
	{
		Fixture f = new Fixture();
		when(f.config.lootFiltersCompatibility()).thenReturn(true);
		f.bridge.beginMenu();
		assertNull(f.bridge.findIcon(f.entry));
		verify(f.events, never()).post(any(PluginMessage.class));
	}

	@Test
	public void selectedLootFiltersWinsEvenWhenImprovedLootingSharingIsOn()
	{
		Fixture f = new Fixture();
		f.select(CustomIconProvider.LOOT_FILTERS);
		assertNotNull(f.bridge.findIcon(f.entry));
		assertEquals(Collections.singletonList("loot-filters"), f.requests);
		verify(f.config, never()).lootFiltersMenuColors();
	}

	@Test
	public void explicitImprovedLootingIconsWorkWithoutLegacySharing()
	{
		Fixture f = new Fixture();
		f.sharing = false;
		f.select(CustomIconProvider.IMPROVED_LOOTING);
		assertNotNull(f.bridge.findIcon(f.entry));
		assertEquals(Collections.singletonList("improved-looting"), f.requests);
		assertTrue(f.iconsOnly);
		assertNull(f.bridge.find(f.entry));
	}

	@Test
	public void unavailableSelectedProviderDoesNotFallBack()
	{
		Fixture f = new Fixture();
		when(f.plugins.isPluginActive(f.filters)).thenReturn(false);
		f.select(CustomIconProvider.LOOT_FILTERS);
		assertNull(f.bridge.findIcon(f.entry));
		assertTrue(f.requests.isEmpty());
		assertTrue(f.bridge.status().contains("Enable Loot Filters"));
		when(f.plugins.isPluginActive(f.filters)).thenReturn(true);
		f.bridge.beginMenu();
		assertNotNull(f.bridge.findIcon(f.entry));
		when(f.plugins.isPluginActive(f.looting)).thenReturn(false);
		f.select(CustomIconProvider.IMPROVED_LOOTING);
		assertNull(f.bridge.findIcon(f.entry));
		assertEquals(Collections.singletonList("loot-filters"), f.requests);
	}

	@Test
	public void iconAndMenuProvidersHaveIndependentPerPileCaches()
	{
		Fixture f = new Fixture();
		when(f.config.useLootFiltersInMenu()).thenReturn(true);
		f.select(CustomIconProvider.LOOT_FILTERS);
		assertEquals(2, f.bridge.find(f.entry).menuSort);
		assertEquals(-1, f.bridge.findIcon(f.entry).menuSort);
		assertEquals(2, f.bridge.find(f.entry).menuSort);
		assertEquals(-1, f.bridge.findIcon(f.entry).menuSort);
		assertEquals(Arrays.asList("improved-looting", "loot-filters"), f.requests);
	}

	@Test
	public void switchingOffAndChangingProvidersDiscardsCachedAndLateArtwork()
	{
		Fixture f = new Fixture();
		CompletableFuture<BufferedImage> delayed = new CompletableFuture<>();
		f.artwork = delayed;
		f.select(CustomIconProvider.LOOT_FILTERS);
		assertNull(f.bridge.image(f.bridge.findIcon(f.entry)));
		f.select(CustomIconProvider.OFF);
		delayed.complete(Fixture.image(Color.RED));
		assertNull(f.bridge.findIcon(f.entry));
		f.artwork = CompletableFuture.completedFuture(Fixture.image(Color.BLUE));
		f.select(CustomIconProvider.IMPROVED_LOOTING);
		BufferedImage current = f.bridge.image(f.bridge.findIcon(f.entry));
		assertEquals(Color.BLUE.getRGB(), current.getRGB(0, 0));
		f.artwork = CompletableFuture.completedFuture(Fixture.image(Color.GREEN));
		f.select(CustomIconProvider.LOOT_FILTERS);
		current = f.bridge.image(f.bridge.findIcon(f.entry));
		assertEquals(Color.GREEN.getRGB(), current.getRGB(0, 0));
		f.bridge.reset();
		assertNull(f.bridge.findIcon(f.entry));
		f.artwork = CompletableFuture.completedFuture(Fixture.image(Color.YELLOW));
		f.bridge.beginMenu();
		assertEquals(Color.YELLOW.getRGB(), f.bridge.image(f.bridge.findIcon(f.entry)).getRGB(0, 0));
	}

	private static class Fixture
	{
		final Client client = mock(Client.class);
		final ImprovedMenusConfig config = mock(ImprovedMenusConfig.class);
		final PluginManager plugins = mock(PluginManager.class);
		final EventBus events = mock(EventBus.class);
		final Plugin looting = mock(Plugin.class), filters = mock(Plugin.class);
		final MenuEntry entry = mock(MenuEntry.class);
		final List<String> requests = new ArrayList<>();
		final LootFiltersCompatibility bridge;
		boolean sharing = true, iconsOnly;
		CompletableFuture<BufferedImage> artwork = CompletableFuture.completedFuture(image(Color.RED));

		Fixture()
		{
			ClientThread thread = mock(ClientThread.class);
			doAnswer(i -> { ((Runnable) i.getArgument(0)).run(); return null; })
					.when(thread).invokeLater(any(Runnable.class));
			when(client.isClientThread()).thenReturn(true);
			when(config.customIconProvider()).thenReturn(CustomIconProvider.OFF);
			when(looting.getName()).thenReturn("Improved Looting TEST");
			when(filters.getName()).thenReturn("Loot Filters");
			when(plugins.getPlugins()).thenReturn(Arrays.asList(looting, filters));
			when(plugins.isPluginActive(any(Plugin.class))).thenReturn(true);
			WorldView view = mock(WorldView.class);
			Scene scene = mock(Scene.class);
			when(client.getWorldView(0)).thenReturn(view);
			when(view.getScene()).thenReturn(scene);
			when(scene.getTiles()).thenReturn(new Tile[][][] { { { mock(Tile.class) } } });
			when(entry.getIdentifier()).thenReturn(385);
			doAnswer(i ->
			{
				PluginMessage message = i.getArgument(0);
				Map<String, Object> data = message.getData();
				if (message.getName().equals("provider-status-v1"))
					data.put("enabled", sharing || Boolean.TRUE.equals(data.get("iconsOnly")));
				if (message.getName().equals("resolved-display-request-v1"))
				{
					requests.add(message.getNamespace());
					iconsOnly = Boolean.TRUE.equals(data.get("iconsOnly"));
					data.put("status", "ok");
					data.put("display", Map.of("hidden", false, "hideOverlay", false,
							"menuSort", message.getNamespace().equals("loot-filters") ? -1 : 2,
							"icon", Map.of("type", "file", "filename", "same.png",
									"getImage", (Supplier<?>) () -> artwork)));
				}
				return null;
			}).when(events).post(any(PluginMessage.class));
			bridge = new LootFiltersCompatibility(client, thread, config, plugins, events);
		}

		void select(CustomIconProvider provider)
		{
			when(config.customIconProvider()).thenReturn(provider);
			bridge.beginMenu();
		}

		static BufferedImage image(Color color)
		{
			BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
			image.setRGB(0, 0, color.getRGB());
			return image;
		}
	}
}
