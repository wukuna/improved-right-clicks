package com.improvedmenus;

import com.google.inject.*;
import java.util.*;
import net.runelite.api.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;
import org.junit.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class MenuEngineTest
{
	private Client client;
	private ImprovedMenusConfig config;
	private MenuEngine engine;

	@Before
	public void setup()
	{
		client = mock(Client.class);
		config = mock(ImprovedMenusConfig.class);
		when(config.revealKey()).thenReturn(ImprovedMenusConfig.RevealKey.NONE);
		when(config.itemRules()).thenReturn("");
		when(config.colors()).thenReturn("");
		when(config.exceptions()).thenReturn("");
		engine = Guice.createInjector(new AbstractModule()
		{
			protected void configure()
			{
				bind(net.runelite.client.game.ItemManager.class)
						.toInstance(mock(net.runelite.client.game.ItemManager.class));
				bind(Client.class).toInstance(client);
				bind(ImprovedMenusConfig.class).toInstance(config);
			}
		}).getInstance(MenuEngine.class);
		engine.reload();
	}

	private MenuEntry item(String option, String target)
	{
		return TestMenus.entry(option, target, MenuAction.CC_OP, 100, InterfaceID.INVENTORY);
	}

	private MenuEngine.Context context()
	{
		return new MenuEngine.Context();
	}

	private String reason(MenuEntry e, MenuEngine.Context c)
	{
		return engine.reason(e, new MenuEntry[] { e }, MenuRules.plain(e.getTarget()), MenuRules.plain(e.getOption()),
				c);
	}

	@Test
	public void bankGuardOnlyWhileOpen()
	{
		when(config.bankWield()).thenReturn(true);
		MenuEntry e = item("Wield", "Rune sword");
		MenuEngine.Context c = context();
		assertNull(reason(e, c));
		c.bank = true;
		assertEquals("bank protection", reason(e, c));
	}

	@Test
	public void fourBankActionsAreIndependent()
	{
		when(config.bankEat()).thenReturn(true);
		MenuEngine.Context c = context();
		c.bank = true;
		assertNotNull(reason(item("Eat", "Shark"), c));
		assertNull(reason(item("Drink", "Potion(4)"), c));
		assertNull(reason(item("Wear", "Helmet"), c));
	}

	@Test
	public void bankWithdrawAndDepositArePreserved()
	{
		when(config.bankWield()).thenReturn(true);
		when(config.bankEat()).thenReturn(true);
		MenuEngine.Context c = context();
		c.bank = true;
		assertNull(reason(TestMenus.entry("Withdraw-1", "Shark", MenuAction.CC_OP, 100, InterfaceID.BANKMAIN), c));
		assertNull(reason(TestMenus.entry("Deposit-All", "Shark", MenuAction.CC_OP, 100, InterfaceID.BANKSIDE), c));
	}

	@Test
	public void bankSideInventoryGuarded()
	{
		when(config.bankWear()).thenReturn(true);
		MenuEngine.Context c = context();
		c.bank = true;
		assertNotNull(reason(TestMenus.entry("Wear", "Helmet", MenuAction.CC_OP, 100, InterfaceID.BANKSIDE), c));
	}

	@Test
	public void emptyOnlyDosedItems()
	{
		when(config.potionEmpty()).thenReturn(true);
		assertNotNull(reason(item("Empty", "Potion(4)"), context()));
		assertNull(reason(item("Empty", "Bucket of water"), context()));
		assertNull(reason(item("Drink", "Potion(4)"), context()));
	}

	@Test
	public void customRulesDoNotHideNpcPlayerObjectCombatOrUseOnActors()
	{
		when(config.itemRules()).thenReturn("*,*");
		engine.reload();
		for (MenuAction action : new MenuAction[] { MenuAction.NPC_FIRST_OPTION, MenuAction.PLAYER_FIRST_OPTION,
				MenuAction.GAME_OBJECT_FIRST_OPTION, MenuAction.ITEM_USE_ON_NPC, MenuAction.ITEM_USE_ON_PLAYER,
				MenuAction.WIDGET_TARGET_ON_NPC, MenuAction.WIDGET_TARGET_ON_PLAYER })
		{
			assertNull(action.name(),
					reason(TestMenus.entry("Attack", "Rune sword", action, 100, InterfaceID.INVENTORY), context()));
		}
	}

	@Test
	public void customRulesWorkOnGroundAndInventoryItems()
	{
		when(config.itemRules()).thenReturn("Drop,Rune*\nTake,Coins");
		engine.reload();
		assertNotNull(reason(item("Drop", "Rune sword"), context()));
		assertNotNull(reason(TestMenus.entry("Take", "Coins", MenuAction.GROUND_ITEM_THIRD_OPTION, -1, 0), context()));
	}

	@Test
	public void bankRuleDoesNotAffectInventoryOutsideBank()
	{
		when(config.itemRules()).thenReturn("[bank] Eat,Shark");
		engine.reload();
		MenuEngine.Context c = context();
		assertNull(reason(item("Eat", "Shark"), c));
		c.bank = true;
		assertNotNull(reason(item("Eat", "Shark"), c));
		assertNull(reason(TestMenus.entry("Eat", "Shark", MenuAction.GROUND_ITEM_FIRST_OPTION, -1, 0), c));
	}

	@Test
	public void groundRuleDoesNotHideInventoryAction()
	{
		when(config.itemRules()).thenReturn("[ground] *,Coins");
		engine.reload();
		assertNull(reason(item("Examine", "Coins"), context()));
		assertNotNull(reason(TestMenus.entry("Take", "Coins", MenuAction.GROUND_ITEM_THIRD_OPTION, -1, 0), context()));
	}

	@Test
	public void contextualExceptionOnlyBypassesSelectedContext()
	{
		when(config.itemRules()).thenReturn("*,Coins");
		when(config.exceptions()).thenReturn("[ground] Coins");
		engine.reload();
		assertNotNull(reason(item("Examine", "Coins"), context()));
		assertNull(reason(TestMenus.entry("Take", "Coins", MenuAction.GROUND_ITEM_THIRD_OPTION, -1, 0), context()));
	}

	@Test
	public void openingAnAlreadyFilteredMenuPreservesPreviewReason()
	{
		when(config.itemRules()).thenReturn("Drop,*");
		engine.reload();
		Menu menu = TestMenus.menu(item("Drop", "Sword"), item("Use", "Sword"));
		engine.apply(menu);
		String original = engine.diagnostics();
		engine.applyOpened(menu);
		assertEquals(original, engine.diagnostics());
		assertTrue(original.contains("custom item rule"));
	}

	@Test
	public void exceptionsOverrideEveryFilter()
	{
		when(config.potionEmpty()).thenReturn(true);
		when(config.itemRules()).thenReturn("*,*");
		when(config.exceptions()).thenReturn("Potion*");
		engine.reload();
		assertNull(reason(item("Empty", "Potion(4)"), context()));
	}

	@Test
	public void loneNpcExamineKeptAndInteractiveNpcExamineHidden()
	{
		when(config.examineNpc()).thenReturn(true);
		when(config.keepLoneNpc()).thenReturn(true);
		MenuEntry examine = TestMenus.entry("Examine", "Banker", MenuAction.EXAMINE_NPC, -1, 0);
		assertNull(reason(examine, context()));
		MenuEntry bank = TestMenus.entry("Bank", "Banker", MenuAction.NPC_THIRD_OPTION, -1, 0);
		assertNotNull(engine.reason(examine, new MenuEntry[] { examine, bank }, "Banker", "Examine", context()));
	}

	@Test
	public void loneObjectExamineOptionCanBeDisabled()
	{
		when(config.examineObject()).thenReturn(true);
		when(config.keepLoneObject()).thenReturn(false);
		assertNotNull(reason(TestMenus.entry("Examine", "Decoration", MenuAction.EXAMINE_OBJECT, -1, 0), context()));
	}

	@Test
	public void examinesAreIndependentByCategory()
	{
		when(config.examineGround()).thenReturn(true);
		assertNotNull(reason(TestMenus.entry("Examine", "Coins", MenuAction.EXAMINE_ITEM_GROUND, -1, 0), context()));
		assertNull(reason(item("Examine", "Coins"), context()));
	}

	@Test
	public void unknownToolStateFailsOpen()
	{
		when(config.mine()).thenReturn(true);
		assertNull(reason(TestMenus.entry("Mine", "Rocks", MenuAction.GAME_OBJECT_FIRST_OPTION, -1, 0), context()));
	}

	@Test
	public void toolRequirementIsContextualAndTyped()
	{
		when(config.chop()).thenReturn(true);
		when(config.mine()).thenReturn(true);
		MenuEngine.Context c = context();
		c.toolsKnown = true;
		MenuEntry chop = TestMenus.entry("Chop down", "Oak tree", MenuAction.GAME_OBJECT_FIRST_OPTION, -1, 0);
		assertNotNull(reason(chop, c));
		c.axe = true;
		assertNull(reason(chop, c));
		assertNotNull(reason(TestMenus.entry("Mine", "Rocks", MenuAction.GAME_OBJECT_FIRST_OPTION, -1, 0), c));
		c.pick = true;
		assertNull(reason(TestMenus.entry("Mine", "Rocks", MenuAction.GAME_OBJECT_FIRST_OPTION, -1, 0), c));
		assertNull(reason(TestMenus.entry("Mine", "Miner", MenuAction.NPC_FIRST_OPTION, -1, 0), c));
	}

	@Test
	public void toolSpecialNamesAreCovered()
	{
		when(config.chop()).thenReturn(true);
		MenuEngine.Context c = context();
		c.toolsKnown = true;
		assertNotNull(reason(TestMenus.entry("Cut", "Sulliuscep", MenuAction.GAME_OBJECT_FIRST_OPTION, -1, 0), c));
		assertNotNull(reason(TestMenus.entry("Chop", "Bruma roots", MenuAction.GAME_OBJECT_FIRST_OPTION, -1, 0), c));
	}

	@Test
	public void emptyEquipmentContainerStillHidesToolsAndInventoryChangesRestoreActions()
	{
		when(config.chop()).thenReturn(true);
		when(config.mine()).thenReturn(true);
		Player player = mock(Player.class);
		PlayerComposition appearance = mock(PlayerComposition.class);
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		when(client.getLocalPlayer()).thenReturn(player);
		when(player.getPlayerComposition()).thenReturn(appearance);
		int[] body = new int[12];
		Arrays.fill(body, PlayerComposition.KIT_OFFSET + 1);
		when(appearance.getEquipmentIds()).thenReturn(body);
		ItemContainer inventory = mock(ItemContainer.class);
		when(client.getItemContainer(InventoryID.INV)).thenReturn(inventory);
		when(inventory.getItems()).thenReturn(new Item[] { new Item(199, 1) });
		MenuEntry tree = TestMenus.entry("Chop down", "Tree", MenuAction.GAME_OBJECT_FIRST_OPTION, -1, 0);
		MenuEntry rock = TestMenus.entry("Mine", "Rocks", MenuAction.GAME_OBJECT_FIRST_OPTION, -1, 0);
		Menu menu = TestMenus.menu(tree, rock);
		engine.apply(menu);
		assertEquals(0, menu.getMenuEntries().length);
		assertTrue(engine.diagnostics().contains("axe required"));
		assertTrue(engine.diagnostics().contains("pickaxe required"));

		when(inventory.getItems()).thenReturn(new Item[] { new Item(ItemID.BRONZE_AXE, 1) });
		when(tree.isDeprioritized()).thenReturn(true);
		menu = TestMenus.menu(tree, rock);
		engine.apply(menu);
		assertArrayEquals(new MenuEntry[] { tree }, menu.getMenuEntries());
		verify(tree, never()).setDeprioritized(anyBoolean());

		when(inventory.getItems()).thenReturn(new Item[] { new Item(ItemID.BRONZE_PICKAXE, 1) });
		menu = TestMenus.menu(tree, rock);
		engine.apply(menu);
		assertArrayEquals(new MenuEntry[] { rock }, menu.getMenuEntries());
	}

	@Test
	public void absentEquipmentRemainsUnknownUntilTheLoggedInAppearanceConfirmsItIsEmpty()
	{
		when(config.chop()).thenReturn(true);
		ItemContainer inventory = mock(ItemContainer.class);
		when(inventory.getItems()).thenReturn(new Item[0]);
		when(client.getItemContainer(InventoryID.INV)).thenReturn(inventory);
		Player player = mock(Player.class);
		PlayerComposition appearance = mock(PlayerComposition.class);
		when(client.getLocalPlayer()).thenReturn(player);
		when(player.getPlayerComposition()).thenReturn(appearance);
		MenuEntry tree = TestMenus.entry("Chop down", "Tree", MenuAction.GAME_OBJECT_FIRST_OPTION, -1, 0);
		for (GameState state : new GameState[] { GameState.LOGGING_IN, GameState.LOGGED_IN })
		{
			when(client.getGameState()).thenReturn(state);
			for (int[] equipment : new int[][] { null, new int[0],
					{ 0, 0, 0, PlayerComposition.ITEM_OFFSET + ItemID.BRONZE_AXE, 0, 0, 0, 0, 0, 0, 0, 0 } })
			{
				when(appearance.getEquipmentIds()).thenReturn(equipment);
				Menu menu = TestMenus.menu(tree);
				engine.apply(menu);
				assertArrayEquals(new MenuEntry[] { tree }, menu.getMenuEntries());
			}
		}
		when(client.getGameState()).thenReturn(GameState.LOGGING_IN);
		when(appearance.getEquipmentIds()).thenReturn(new int[12]);
		Menu menu = TestMenus.menu(tree);
		engine.apply(menu);
		assertArrayEquals(new MenuEntry[] { tree }, menu.getMenuEntries());
	}

	@Test
	public void houseOnlyOutsideBuildMode()
	{
		when(config.house()).thenReturn(true);
		MenuEntry e = TestMenus.entry("Remove", "Chair", MenuAction.GAME_OBJECT_FIFTH_OPTION, -1, 0);
		MenuEngine.Context c = context();
		assertNull(reason(e, c));
		c.house = true;
		assertNotNull(reason(e, c));
		c.building = true;
		assertNull(reason(e, c));
	}

	@Test
	public void houseDoesNotHideNpcOrInventoryRemove()
	{
		when(config.house()).thenReturn(true);
		MenuEngine.Context c = context();
		c.house = true;
		assertNull(reason(item("Remove", "Chair"), c));
		assertNull(reason(TestMenus.entry("Remove", "Butler", MenuAction.NPC_FIRST_OPTION, -1, 0), c));
	}

	@Test
	public void mahoganyCleanupOnlyCompletedRemove()
	{
		when(config.mahogany()).thenReturn(true);
		MenuEntry e = TestMenus.entry("Remove", "Chair", MenuAction.GAME_OBJECT_FIRST_OPTION, -1, 0);
		when(e.getIdentifier()).thenReturn(40002);
		ObjectComposition base = mock(ObjectComposition.class), active = mock(ObjectComposition.class);
		when(client.getObjectDefinition(40002)).thenReturn(base);
		when(base.getImpostorIds()).thenReturn(new int[] { 1 });
		when(base.getImpostor()).thenReturn(active);
		when(active.getId()).thenReturn(40017);
		assertNull(reason(e, context()));
		when(active.getId()).thenReturn(1);
		assertNotNull(reason(e, context()));
		e.setOption("Search");
		assertNull(reason(e, context()));
	}

	@Test
	public void nullMahoganyTransformFailsOpen()
	{
		when(config.mahogany()).thenReturn(true);
		MenuEntry e = TestMenus.entry("Remove", "Chair", MenuAction.GAME_OBJECT_FIRST_OPTION, -1, 0);
		when(e.getIdentifier()).thenReturn(40002);
		assertNull(reason(e, context()));
	}

	@Test
	public void filteredMenuRetainsOrderIdentityAndCallback()
	{
		when(config.itemRules()).thenReturn("Drop,*");
		engine.reload();
		MenuEntry cancel = TestMenus.entry("Cancel", "", MenuAction.CANCEL, -1, 0), use = item("Use", "Rune sword"),
				drop = item("Drop", "Rune sword");
		java.util.function.Consumer<MenuEntry> callback = e ->
		{
		};
		when(use.onClick()).thenReturn(callback);
		Menu m = TestMenus.menu(cancel, drop, use);
		engine.apply(m);
		assertArrayEquals(new MenuEntry[] { cancel, use }, m.getMenuEntries());
		assertSame(callback, m.getMenuEntries()[1].onClick());
	}

	@Test
	public void colorsAreCosmeticAndIdempotent()
	{
		when(config.enableColors()).thenReturn(true);
		when(config.colors()).thenReturn("*:Attack:#ff0000");
		engine.reload();
		MenuEntry e = TestMenus.entry("<img=1><col=ffffff>Attack</col>", "Goblin", MenuAction.NPC_SECOND_OPTION, -1, 0);
		Menu m = TestMenus.menu(e);
		engine.apply(m);
		engine.apply(m);
		assertEquals("<col=ff0000><img=1>Attack</col>", e.getOption());
		assertEquals(MenuAction.NPC_SECOND_OPTION, e.getType());
		assertSame(e, m.getMenuEntries()[0]);
	}

	@Test
	public void submenusInheritParentTarget()
	{
		when(config.enableColors()).thenReturn(true);
		when(config.colors()).thenReturn("Banker:Collect:#ff0000");
		engine.reload();
		MenuEntry child = TestMenus.entry("Collect", "", MenuAction.RUNELITE, -1, 0),
				parent = TestMenus.entry("More", "Banker", MenuAction.RUNELITE, -1, 0);
		Menu submenu = TestMenus.menu(child);
		when(parent.getSubMenu()).thenReturn(submenu);
		engine.apply(TestMenus.menu(parent));
		assertEquals("<col=ff0000>Collect</col>", child.getOption());
	}

	@Test
	public void invalidConfigDoesNotKeepStaleRules()
	{
		when(config.itemRules()).thenReturn("Drop,*");
		engine.reload();
		assertNotNull(reason(item("Drop", "Sword"), context()));
		when(config.itemRules()).thenReturn("bad");
		engine.reload();
		assertNull(reason(item("Drop", "Sword"), context()));
	}

	@Test
	public void bankClosingAndLogoutResetContextWithoutCaches()
	{
		when(config.bankWield()).thenReturn(true);
		Widget bank = mock(Widget.class);
		when(client.getWidget(InterfaceID.BANKMAIN, 0)).thenReturn(bank);
		MenuEntry e = item("Wield", "Sword");
		Menu m = TestMenus.menu(e);
		engine.apply(m);
		assertEquals(0, m.getMenuEntries().length);
		when(bank.isHidden()).thenReturn(true);
		m = TestMenus.menu(e);
		engine.apply(m);
		assertEquals(1, m.getMenuEntries().length);
		when(client.getWidget(InterfaceID.BANKMAIN, 0)).thenReturn(null);
		engine.apply(m);
		assertEquals(1, m.getMenuEntries().length);
	}

	@Test
	public void carriedAndEquippedToolsAreBothRecognized()
	{
		when(config.chop()).thenReturn(true);
		ItemContainer inv = mock(ItemContainer.class), worn = mock(ItemContainer.class);
		when(inv.getItems()).thenReturn(new Item[] { new Item(ItemID.BRONZE_AXE, 1) });
		when(worn.getItems()).thenReturn(new Item[0]);
		when(client.getItemContainer(InventoryID.INV)).thenReturn(inv);
		when(client.getItemContainer(InventoryID.WORN)).thenReturn(worn);
		MenuEntry e = TestMenus.entry("Chop down", "Tree", MenuAction.GAME_OBJECT_FIRST_OPTION, -1, 0);
		Menu m = TestMenus.menu(e);
		engine.apply(m);
		assertEquals(1, m.getMenuEntries().length);
		when(inv.getItems()).thenReturn(new Item[0]);
		when(worn.getItems()).thenReturn(new Item[] { new Item(ItemID.CRYSTAL_AXE, 1) });
		engine.apply(m);
		assertEquals(1, m.getMenuEntries().length);
		when(worn.getItems()).thenReturn(new Item[0]);
		engine.apply(m);
		assertEquals(0, m.getMenuEntries().length);
	}
}
