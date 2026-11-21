package com.improvedmenus;

import com.google.inject.*;
import java.awt.Canvas;
import net.runelite.api.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;
import org.junit.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TargetAndCleanupTest
{
	private Client client;
	private ImprovedMenusConfig config;
	private ItemManager items;
	private Widget selected;
	private MenuEngine engine;

	@Before
	public void setup()
	{
		client = mock(Client.class);
		config = mock(ImprovedMenusConfig.class);
		items = mock(ItemManager.class);
		selected = mock(Widget.class);
		when(config.revealKey()).thenReturn(ImprovedMenusConfig.RevealKey.NONE);
		when(config.itemRules()).thenReturn("");
		when(config.colors()).thenReturn("");
		when(config.exceptions()).thenReturn("");
		when(config.playerUseItems()).thenReturn(TargetProtections.DEFAULT_PLAYER_ITEMS);
		when(config.telegrabNpcNames()).thenReturn("");
		when(client.isWidgetSelected()).thenReturn(true);
		when(client.getSelectedWidget()).thenReturn(selected);
		engine = Guice.createInjector(new AbstractModule()
		{
			protected void configure()
			{
				bind(Client.class).toInstance(client);
				bind(ImprovedMenusConfig.class).toInstance(config);
				bind(ItemManager.class).toInstance(items);
			}
		}).getInstance(MenuEngine.class);
		engine.reload();
	}

	private MenuEntry entry(MenuAction action, String option)
	{
		return TestMenus.entry(option, "Target", action, -1, 0);
	}

	private void selectItem(String name)
	{
		when(selected.getItemId()).thenReturn(100);
		ItemComposition item = mock(ItemComposition.class);
		when(item.getMembersName()).thenReturn(name);
		when(items.getItemComposition(100)).thenReturn(item);
	}

	private MenuEntry npc(int id, String name)
	{
		MenuEntry entry = entry(MenuAction.WIDGET_TARGET_ON_NPC, "Cast");
		NPC npc = mock(NPC.class);
		when(npc.getId()).thenReturn(id);
		when(npc.getName()).thenReturn(name);
		when(entry.getNpc()).thenReturn(npc);
		return entry;
	}

	@Test
	public void playerUseSwitchOnlyRemovesItemUse()
	{
		selectItem("Knife");
		when(config.noUsePlayer()).thenReturn(true);
		MenuEntry use = entry(MenuAction.WIDGET_TARGET_ON_PLAYER, "Use"),
				trade = entry(MenuAction.PLAYER_THIRD_OPTION, "Trade with"),
				attack = entry(MenuAction.PLAYER_FIRST_OPTION, "Attack");
		Menu menu = TestMenus.menu(use, trade, attack);
		engine.applyTargetProtections(menu);
		assertArrayEquals(new MenuEntry[] { trade, attack }, menu.getMenuEntries());
	}

	@Test
	public void spellsOnPlayersArePreserved()
	{
		when(config.noUsePlayer()).thenReturn(true);
		when(selected.getItemId()).thenReturn(-1);
		MenuEntry spell = entry(MenuAction.WIDGET_TARGET_ON_PLAYER, "Cast");
		Menu menu = TestMenus.menu(spell);
		engine.applyTargetProtections(menu);
		assertSame(spell, menu.getMenuEntries()[0]);
	}

	@Test
	public void castCannotBeRemovedEvenIfSelectedWidgetHasItemId()
	{
		when(config.noUsePlayer()).thenReturn(true);
		selectItem("Knife");
		Menu menu = TestMenus.menu(entry(MenuAction.WIDGET_TARGET_ON_PLAYER, "Cast"));
		engine.applyTargetProtections(menu);
		assertEquals(1, menu.getMenuEntries().length);
	}

	@Test
	public void allowedQuestItemAndCustomItemArePreserved()
	{
		when(config.noUsePlayer()).thenReturn(true);
		selectItem("<col=ffff00>Weapon store key</col>");
		Menu menu = TestMenus.menu(entry(MenuAction.WIDGET_TARGET_ON_PLAYER, "Use"));
		engine.applyTargetProtections(menu);
		assertEquals(1, menu.getMenuEntries().length);
		selectItem("Knife");
		when(config.playerUseItems()).thenReturn("knife");
		engine.applyTargetProtections(menu);
		assertEquals(1, menu.getMenuEntries().length);
	}

	@Test
	public void missingItemDefinitionFailsOpen()
	{
		when(config.noUsePlayer()).thenReturn(true);
		when(selected.getItemId()).thenReturn(100);
		Menu menu = TestMenus.menu(entry(MenuAction.WIDGET_TARGET_ON_PLAYER, "Use"));
		engine.applyTargetProtections(menu);
		assertEquals(1, menu.getMenuEntries().length);
	}

	@Test
	public void noSelectionAndDisabledSwitchLeaveItemUse()
	{
		selectItem("Knife");
		Menu menu = TestMenus.menu(entry(MenuAction.WIDGET_TARGET_ON_PLAYER, "Use"));
		engine.applyTargetProtections(menu);
		assertEquals(1, menu.getMenuEntries().length);
		when(config.noUsePlayer()).thenReturn(true);
		when(client.isWidgetSelected()).thenReturn(false);
		engine.applyTargetProtections(menu);
		assertEquals(1, menu.getMenuEntries().length);
	}

	@Test
	public void telegrabNpcSwitchPreservesGroundLootAndOtherSpells()
	{
		when(config.noTelegrabNpc()).thenReturn(true);
		when(selected.getId()).thenReturn(InterfaceID.MagicSpellbook.TELEGRAB);
		MenuEntry npc = npc(1, "Goblin"), ground = entry(MenuAction.WIDGET_TARGET_ON_GROUND_ITEM, "Cast");
		Menu menu = TestMenus.menu(npc, ground);
		engine.applyTargetProtections(menu);
		assertArrayEquals(new MenuEntry[] { ground }, menu.getMenuEntries());
		when(selected.getId()).thenReturn(42);
		menu = TestMenus.menu(npc);
		engine.applyTargetProtections(menu);
		assertEquals(1, menu.getMenuEntries().length);
	}

	@Test
	public void usefulTelegrabTargetsAndCustomNamesArePreserved()
	{
		when(config.noTelegrabNpc()).thenReturn(true);
		when(selected.getId()).thenReturn(InterfaceID.MagicSpellbook.TELEGRAB);
		MenuEntry maze = npc(NpcID.MAGICTRAINING_GUARD_MAZE_INCOMPLETE, "Maze Guardian"),
				guard = npc(NpcID.KR_KEEP_GUARD_HAIR, "Guard"), corpse = npc(NpcID.SAILING_TERN_DEAD, "Tern"),
				crate = npc(NpcID.SAILING_CHANCE_ENCOUNTER_LOST_GOODS_WOOD, "Crate"), goat = npc(16298, "Goat"),
				custom = npc(123, "Custom");
		when(config.telegrabNpcNames()).thenReturn("custom");
		Menu menu = TestMenus.menu(maze, guard, corpse, crate, goat, custom);
		engine.applyTargetProtections(menu);
		assertEquals(6, menu.getMenuEntries().length);
	}

	@Test
	public void unknownNpcFailsOpen()
	{
		when(config.noTelegrabNpc()).thenReturn(true);
		when(selected.getId()).thenReturn(InterfaceID.MagicSpellbook.TELEGRAB);
		Menu menu = TestMenus.menu(entry(MenuAction.WIDGET_TARGET_ON_NPC, "Cast"));
		engine.applyTargetProtections(menu);
		assertEquals(1, menu.getMenuEntries().length);
	}

	@Test
	public void cleanupOnlyChangesOpenedMenus()
	{
		when(config.hideCancel()).thenReturn(true);
		when(config.hideWalkHere()).thenReturn(true);
		MenuEntry cancel = entry(MenuAction.CANCEL, "Cancel"), walk = entry(MenuAction.WALK, "Walk here"),
				talk = entry(MenuAction.NPC_FIRST_OPTION, "Talk-to");
		Menu menu = TestMenus.menu(cancel, walk, talk);
		engine.apply(menu);
		assertEquals(3, menu.getMenuEntries().length);
		engine.applyOpened(menu);
		assertArrayEquals(new MenuEntry[] { talk }, menu.getMenuEntries());
	}

	@Test
	public void setHeadingFilteringOnlyChangesOpenedMenusAndRespectsReveal()
	{
		when(config.hideSetHeading()).thenReturn(true);
		MenuEntry heading = entry(MenuAction.SET_HEADING, "Set heading"), walk = entry(MenuAction.WALK, "Walk here");
		Menu menu = TestMenus.menu(heading, walk);
		engine.apply(menu);
		assertEquals(2, menu.getMenuEntries().length);
		engine.applyOpened(menu);
		assertArrayEquals(new MenuEntry[] { walk }, menu.getMenuEntries());
		when(config.revealKey()).thenReturn(ImprovedMenusConfig.RevealKey.CONTROL);
		when(client.isKeyPressed(KeyCode.KC_CONTROL)).thenReturn(true);
		when(client.getCanvas()).thenReturn(new Canvas()
		{
			@Override
			public boolean hasFocus()
			{
				return true;
			}
		});
		menu = TestMenus.menu(heading, walk);
		engine.applyOpened(menu);
		assertEquals(2, menu.getMenuEntries().length);
		when(config.hideSetHeading()).thenReturn(false);
		when(config.revealKey()).thenReturn(ImprovedMenusConfig.RevealKey.NONE);
		menu = TestMenus.menu(heading, walk);
		engine.applyOpened(menu);
		assertEquals(2, menu.getMenuEntries().length);
	}

	@Test
	public void cleanupSwitchesAreIndependent()
	{
		when(config.hideCancel()).thenReturn(true);
		MenuEntry cancel = entry(MenuAction.CANCEL, "Cancel"), walk = entry(MenuAction.WALK, "Walk here");
		Menu menu = TestMenus.menu(cancel, walk);
		engine.applyOpened(menu);
		assertArrayEquals(new MenuEntry[] { walk }, menu.getMenuEntries());
		when(config.hideCancel()).thenReturn(false);
		when(config.hideWalkHere()).thenReturn(true);
		menu = TestMenus.menu(cancel, walk);
		engine.applyOpened(menu);
		assertArrayEquals(new MenuEntry[] { cancel }, menu.getMenuEntries());
	}

	@Test
	public void emptyOpenedMenuRetainsSafeCancel()
	{
		when(config.hideCancel()).thenReturn(true);
		when(config.hideWalkHere()).thenReturn(true);
		MenuEntry cancel = entry(MenuAction.CANCEL, "Cancel");
		Menu menu = TestMenus.menu(cancel, entry(MenuAction.WALK, "Walk here"));
		engine.applyOpened(menu);
		assertArrayEquals(new MenuEntry[] { cancel }, menu.getMenuEntries());
	}

	@Test
	public void heldRevealBypassesNewProtectionsAndCleanup()
	{
		when(config.revealKey()).thenReturn(ImprovedMenusConfig.RevealKey.CONTROL);
		when(client.isKeyPressed(KeyCode.KC_CONTROL)).thenReturn(true);
		when(client.getCanvas()).thenReturn(new Canvas()
		{
			@Override
			public boolean hasFocus()
			{
				return true;
			}
		});
		when(config.noUsePlayer()).thenReturn(true);
		when(config.hideCancel()).thenReturn(true);
		when(config.hideWalkHere()).thenReturn(true);
		selectItem("Knife");
		Menu menu = TestMenus.menu(entry(MenuAction.WIDGET_TARGET_ON_PLAYER, "Use"), entry(MenuAction.CANCEL, "Cancel"),
				entry(MenuAction.WALK, "Walk here"));
		engine.applyTargetProtections(menu);
		engine.applyOpened(menu);
		assertEquals(3, menu.getMenuEntries().length);
	}

	@Test
	public void togglingOffRestoresOptionsInTheNextRebuiltMenu()
	{
		when(config.hideCancel()).thenReturn(true);
		when(config.hideWalkHere()).thenReturn(true);
		when(config.noUsePlayer()).thenReturn(true);
		selectItem("Knife");
		Menu menu = TestMenus.menu(entry(MenuAction.CANCEL, "Cancel"), entry(MenuAction.WALK, "Walk here"),
				entry(MenuAction.WIDGET_TARGET_ON_PLAYER, "Use"), entry(MenuAction.NPC_FIRST_OPTION, "Talk-to"));
		engine.applyOpened(menu);
		assertEquals(1, menu.getMenuEntries().length);
		when(config.hideCancel()).thenReturn(false);
		when(config.hideWalkHere()).thenReturn(false);
		when(config.noUsePlayer()).thenReturn(false);
		menu = TestMenus.menu(entry(MenuAction.CANCEL, "Cancel"), entry(MenuAction.WALK, "Walk here"),
				entry(MenuAction.WIDGET_TARGET_ON_PLAYER, "Use"), entry(MenuAction.NPC_FIRST_OPTION, "Talk-to"));
		engine.reload();
		engine.applyOpened(menu);
		assertEquals(4, menu.getMenuEntries().length);
	}

	@Test
	public void invalidNameListFailsOpen()
	{
		when(config.noUsePlayer()).thenReturn(true);
		selectItem("Knife");
		when(config.playerUseItems()).thenReturn(String.join(",", java.util.Collections.nCopies(201, "Name")));
		Menu menu = TestMenus.menu(entry(MenuAction.WIDGET_TARGET_ON_PLAYER, "Use"));
		engine.applyTargetProtections(menu);
		assertEquals(1, menu.getMenuEntries().length);
	}

	@Test
	public void rightClickCleanupAlsoCleansSubmenusWithoutReorderingActions()
	{
		when(config.hideWalkHere()).thenReturn(true);
		when(config.hideCancel()).thenReturn(true);
		MenuEntry first = entry(MenuAction.NPC_FIRST_OPTION, "Talk-to"),
				second = entry(MenuAction.NPC_SECOND_OPTION, "Shop");
		Menu sub = TestMenus.menu(entry(MenuAction.CANCEL, "Cancel"), first, entry(MenuAction.WALK, "Walk here"),
				second);
		MenuEntry parent = entry(MenuAction.RUNELITE, "Options");
		when(parent.getSubMenu()).thenReturn(sub);
		engine.applyOpened(TestMenus.menu(parent));
		assertArrayEquals(new MenuEntry[] { first, second }, sub.getMenuEntries());
	}

	@Test
	public void potionEmptyGuardCoversAllDosesWithoutHidingDrinkOrUndosedContainers()
	{
		when(config.potionEmpty()).thenReturn(true);
		for (int dose = 1; dose <= 4; dose++)
		{
			MenuEntry empty = TestMenus.entry("Empty", "<col=ff0000>Super restore(" + dose + ")</col>",
					MenuAction.CC_OP, 100, InterfaceID.INVENTORY);
			MenuEntry drink = TestMenus.entry("Drink", "Super restore(" + dose + ")", MenuAction.CC_OP, 100,
					InterfaceID.INVENTORY);
			Menu menu = TestMenus.menu(empty, drink);
			engine.apply(menu);
			assertArrayEquals(new MenuEntry[] { drink }, menu.getMenuEntries());
		}
		Menu menu = TestMenus
				.menu(TestMenus.entry("Empty", "Bucket of water", MenuAction.CC_OP, 100, InterfaceID.INVENTORY));
		engine.apply(menu);
		assertEquals(1, menu.getMenuEntries().length);
	}
}
