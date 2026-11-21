package com.improvedmenus;

import com.google.gson.Gson;
import java.util.*;
import net.runelite.api.*;
import net.runelite.client.config.ConfigManager;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ItemIconsTest
{
	@Test
	public void rejectsMalformedAndOversizeIconData()
	{
		ItemIcons store = new ItemIcons();
		store.gson = new Gson();
		for (String json : new String[] { "null", "[null]", "[{\"name\":\"Shark\",\"kind\":\"UNKNOWN\"}]",
				"[{\"name\":\"<img=1>\",\"kind\":\"HEART\"}]" })
		{
			try
			{
				store.parse(json);
				fail(json);
			}
			catch (IllegalArgumentException expected)
			{
			}
		}
	}

	@Test
	public void savingAndUndoOnlyWriteOurConfiguration()
	{
		ItemIcons store = new ItemIcons();
		store.gson = new Gson();
		store.configs = mock(ConfigManager.class);
		Map<String, String> data = new HashMap<>();
		when(store.configs.getConfiguration(eq(ImprovedMenusConfig.GROUP), anyString()))
				.thenAnswer(i -> data.get(i.getArgument(1)));
		doAnswer(i ->
		{
			data.put(i.getArgument(1), String.valueOf((Object) i.getArgument(2)));
			return null;
		}).when(store.configs).setConfiguration(eq(ImprovedMenusConfig.GROUP), anyString(), any());
		ItemIcons.Assignment a = new ItemIcons.Assignment();
		a.name = "Shark";
		a.kind = ItemIcons.Kind.HEART;
		store.save(Collections.singletonList(a));
		assertEquals(1, store.snapshot().size());
		store.undo();
		assertTrue(store.snapshot().isEmpty());
		store.undo();
		assertEquals(1, store.snapshot().size());
		verify(store.configs, never()).setConfiguration(eq("lootfilters"), anyString(), any());
		data.put(ItemIcons.KEY, "[]");
		assertTrue(store.snapshot().isEmpty());
	}

	@Test
	public void bothAddsTwoSlotsAndRestoresOtherPluginFormatting()
	{
		MenuPresentation p = presentation(ImprovedMenusConfig.GroundIconSource.BOTH);
		ItemIcons icons = mock(ItemIcons.class);
		ItemIcons.Assignment custom = new ItemIcons.Assignment();
		custom.kind = ItemIcons.Kind.HEART;
		when(icons.find(385)).thenReturn(custom);
		p.setItemIcons(icons);
		MenuEntry e = UserCombinationAuditTest.e("Take", "<col=ff0000>Shark (2)</col>",
				MenuAction.GROUND_ITEM_THIRD_OPTION, 385, 0);
		p.decorate(UserCombinationAuditTest.menu(e));
		assertEquals("<img=4><img=4><col=ff0000>Shark (2)</col>", e.getTarget());
		p.stop();
		assertEquals("<col=ff0000>Shark (2)</col>", e.getTarget());
	}

	@Test
	public void bothDeduplicatesCurrentItemAndCustomLeavesUnassignedItemsAlone()
	{
		MenuPresentation p = presentation(ImprovedMenusConfig.GroundIconSource.BOTH);
		ItemIcons icons = mock(ItemIcons.class);
		ItemIcons.Assignment a = new ItemIcons.Assignment();
		a.kind = ItemIcons.Kind.CURRENT_ITEM;
		when(icons.find(385)).thenReturn(a);
		p.setItemIcons(icons);
		MenuEntry e = UserCombinationAuditTest.e("Take", "Shark", MenuAction.GROUND_ITEM_THIRD_OPTION, 385, 0);
		p.decorate(UserCombinationAuditTest.menu(e));
		assertEquals("<img=4>Shark", e.getTarget());
		p.stop();
		p = presentation(ImprovedMenusConfig.GroundIconSource.CUSTOM);
		p.setItemIcons(mock(ItemIcons.class));
		p.decorate(UserCombinationAuditTest.menu(e));
		assertEquals("Shark", e.getTarget());
	}

	@Test
	public void importedRuleDrivesBothSlotsButNativeNeverEvaluatesFilter()
	{
		ItemIcons library = mock(ItemIcons.class);
		library.filters = mock(FilterIcons.class);
		ItemIcons.Assignment custom = new ItemIcons.Assignment();
		custom.kind = ItemIcons.Kind.HEART;
		when(library.filters.find(any())).thenReturn(custom);
		MenuEntry entry = UserCombinationAuditTest.e("Take", "Shark", MenuAction.GROUND_ITEM_THIRD_OPTION, 385, 0);
		MenuPresentation nativeMode = presentation(ImprovedMenusConfig.GroundIconSource.NATIVE);
		nativeMode.setItemIcons(library);
		nativeMode.decorate(UserCombinationAuditTest.menu(entry));
		verifyNoInteractions(library.filters);
		nativeMode.stop();
		MenuPresentation both = presentation(ImprovedMenusConfig.GroundIconSource.BOTH);
		both.setItemIcons(library);
		both.decorate(UserCombinationAuditTest.menu(entry));
		verify(library.filters).find(entry);
		assertEquals("<img=4><img=4>Shark", entry.getTarget());
		both.stop();
		assertEquals("Shark", entry.getTarget());
	}

	@Test
	public void absentFilterIconOnlyLeavesNativeSlotInNativeOrBoth()
	{
		for (ImprovedMenusConfig.GroundIconSource source : ImprovedMenusConfig.GroundIconSource.values())
		{
			MenuPresentation p = presentation(source);
			ItemIcons library = mock(ItemIcons.class);
			library.filters = mock(FilterIcons.class);
			p.setItemIcons(library);
			MenuEntry e = UserCombinationAuditTest.e("Take", "<col=ff0000>Shark</col>",
					MenuAction.GROUND_ITEM_THIRD_OPTION, 385, 0);
			p.decorate(UserCombinationAuditTest.menu(e));
			assertEquals(source == ImprovedMenusConfig.GroundIconSource.CUSTOM ? "<col=ff0000>Shark</col>"
					: "<img=4><col=ff0000>Shark</col>", e.getTarget());
			p.stop();
			assertEquals("<col=ff0000>Shark</col>", e.getTarget());
		}
	}

	@Test
	public void unavailableCustomImageNeverDrawsNativeInCustomMode()
	{
		for (ImprovedMenusConfig.GroundIconSource source : new ImprovedMenusConfig.GroundIconSource[] {
				ImprovedMenusConfig.GroundIconSource.CUSTOM, ImprovedMenusConfig.GroundIconSource.BOTH })
			for (boolean respect : new boolean[] { false, true })
			{
				Client client = mock(Client.class);
				net.runelite.client.game.ItemManager itemManager = mock(net.runelite.client.game.ItemManager.class);
				MenuPresentation p = presentation(source, client, itemManager, respect);
				ItemIcons library = mock(ItemIcons.class);
				ItemIcons.Assignment a = new ItemIcons.Assignment();
				a.kind = ItemIcons.Kind.IMAGE;
				when(library.find(385)).thenReturn(a);
				p.setItemIcons(library);
				MenuEntry entry = UserCombinationAuditTest.e("Take", "Shark", MenuAction.GROUND_ITEM_THIRD_OPTION, 385,
						0);
				Menu menu = UserCombinationAuditTest.menu(entry);
				when(menu.getMenuWidth()).thenReturn(200);
				when(menu.getMenuHeight()).thenReturn(80);
				when(client.getMenu()).thenReturn(menu);
				when(client.isMenuOpen()).thenReturn(true);
				p.decorate(menu);
				java.awt.Graphics2D graphics = new java.awt.image.BufferedImage(300, 100, 2).createGraphics();
				try
				{
					p.render(graphics);
				}
				finally
				{
					graphics.dispose();
				}
				if (source == ImprovedMenusConfig.GroundIconSource.CUSTOM || respect)
					verifyNoInteractions(itemManager);
				else
					verify(itemManager).getImage(385, Integer.MAX_VALUE, false);
				p.stop();
				assertEquals("Shark", entry.getTarget());
			}
	}

	@Test
	public void linkedIconsRespectAllSourcesAndKeepColorsSeparate()
	{
		for (ImprovedMenusConfig.GroundIconSource source : ImprovedMenusConfig.GroundIconSource.values())
			for (boolean respect : new boolean[] { false, true })
			{
				LootFiltersCompatibility bridge = mock(LootFiltersCompatibility.class);
				when(bridge.findIcon(any())).thenReturn(
						new LootFiltersCompatibility.Result(new java.awt.image.BufferedImage(16, 16, 2), false));
				MenuPresentation p = presentation(source, mock(Client.class),
						mock(net.runelite.client.game.ItemManager.class), true, bridge, respect);
				MenuEntry entry = UserCombinationAuditTest.e("Take", "<col=ff0000>Shark</col>",
						MenuAction.GROUND_ITEM_THIRD_OPTION, 385, 0);
				p.decorate(UserCombinationAuditTest.menu(entry));
				String slots = source == ImprovedMenusConfig.GroundIconSource.BOTH ? "<img=4><img=4>" : "<img=4>";
				assertEquals(slots + "<col=ff0000>Shark</col>", entry.getTarget());
				if (source == ImprovedMenusConfig.GroundIconSource.NATIVE)
					verifyNoInteractions(bridge);
				else
					verify(bridge).findIcon(entry);
				p.stop();
				assertEquals("<col=ff0000>Shark</col>", entry.getTarget());
				when(bridge.findIcon(any())).thenReturn(
						new LootFiltersCompatibility.Result(null, false, true, true, 0, java.awt.Color.RED));
				p.decorate(UserCombinationAuditTest.menu(entry));
				assertEquals(source == ImprovedMenusConfig.GroundIconSource.CUSTOM
						|| source == ImprovedMenusConfig.GroundIconSource.BOTH && respect ? "<col=ff0000>Shark</col>"
								: "<img=4><col=ff0000>Shark</col>",
						entry.getTarget());
				p.stop();
			}
	}

	@Test
	public void respectingFilterVisibilityInBothIsOptionalAndKeepsVisiblePairs()
	{
		assertFalse(new ImprovedMenusConfig()
		{
		}.bothRespectFilterVisibility());
		assertTrue(SettingsCatalog.ALL.containsKey("bothRespectFilterVisibility"));
		for (ImprovedMenusConfig.GroundIconSource source : ImprovedMenusConfig.GroundIconSource.values())
		{
			MenuPresentation p = presentation(source, mock(Client.class),
					mock(net.runelite.client.game.ItemManager.class), true);
			ItemIcons library = mock(ItemIcons.class);
			library.filters = mock(FilterIcons.class);
			p.setItemIcons(library);
			MenuEntry e = UserCombinationAuditTest.e("Take", "Shark", MenuAction.GROUND_ITEM_THIRD_OPTION, 385, 0);
			p.decorate(UserCombinationAuditTest.menu(e));
			assertEquals(source == ImprovedMenusConfig.GroundIconSource.NATIVE ? "<img=4>Shark" : "Shark",
					e.getTarget());
			p.stop();
			ItemIcons.Assignment a = new ItemIcons.Assignment();
			a.kind = ItemIcons.Kind.HEART;
			when(library.filters.find(e)).thenReturn(a);
			p.decorate(UserCombinationAuditTest.menu(e));
			assertEquals(source == ImprovedMenusConfig.GroundIconSource.BOTH ? "<img=4><img=4>Shark" : "<img=4>Shark",
					e.getTarget());
			p.stop();
		}
	}

	private MenuPresentation presentation(ImprovedMenusConfig.GroundIconSource source)
	{
		return presentation(source, mock(Client.class), mock(net.runelite.client.game.ItemManager.class));
	}

	private MenuPresentation presentation(ImprovedMenusConfig.GroundIconSource source, Client client,
			net.runelite.client.game.ItemManager itemManager, boolean respect)
	{
		return presentation(source, client, itemManager, false, mock(LootFiltersCompatibility.class), respect);
	}

	private MenuPresentation presentation(ImprovedMenusConfig.GroundIconSource source, boolean linked,
			LootFiltersCompatibility bridge)
	{
		return presentation(source, mock(Client.class), mock(net.runelite.client.game.ItemManager.class), linked,
				bridge);
	}

	private MenuPresentation presentation(ImprovedMenusConfig.GroundIconSource source, Client client,
			net.runelite.client.game.ItemManager itemManager)
	{
		return presentation(source, client, itemManager, false, mock(LootFiltersCompatibility.class));
	}

	private MenuPresentation presentation(ImprovedMenusConfig.GroundIconSource source, Client client,
			net.runelite.client.game.ItemManager itemManager, boolean linked, LootFiltersCompatibility bridge)
	{
		return presentation(source, client, itemManager, linked, bridge, false);
	}

	private MenuPresentation presentation(ImprovedMenusConfig.GroundIconSource source, Client client,
			net.runelite.client.game.ItemManager itemManager, boolean linked, LootFiltersCompatibility bridge,
			boolean respect)
	{
		ImprovedMenusConfig config = mock(ImprovedMenusConfig.class);
		when(config.groundIcons()).thenReturn(true);
		when(config.groundIconSource()).thenReturn(source);
		when(config.lootFiltersCompatibility()).thenReturn(linked);
		when(config.bothRespectFilterVisibility()).thenReturn(respect);
		net.runelite.client.game.ChatIconManager icons = mock(net.runelite.client.game.ChatIconManager.class);
		when(icons.registerChatIcon(any())).thenReturn(2);
		when(icons.chatIconIndex(2)).thenReturn(4);
		MenuPresentation p = com.google.inject.Guice.createInjector(new com.google.inject.AbstractModule()
		{
			protected void configure()
			{
				bind(LootFiltersCompatibility.class).toInstance(bridge);
				bind(java.util.concurrent.ScheduledExecutorService.class)
						.toInstance(mock(java.util.concurrent.ScheduledExecutorService.class));
				bind(net.runelite.client.config.RuneLiteConfig.class)
						.toInstance(mock(net.runelite.client.config.RuneLiteConfig.class));
				bind(Client.class).toInstance(client);
				bind(ImprovedMenusConfig.class).toInstance(config);
				bind(net.runelite.client.game.ChatIconManager.class).toInstance(icons);
				bind(net.runelite.client.game.ItemManager.class).toInstance(itemManager);
				bind(net.runelite.client.ui.overlay.tooltip.TooltipManager.class)
						.toInstance(mock(net.runelite.client.ui.overlay.tooltip.TooltipManager.class));
			}
		}).getInstance(MenuPresentation.class);
		p.start();
		return p;
	}
}
