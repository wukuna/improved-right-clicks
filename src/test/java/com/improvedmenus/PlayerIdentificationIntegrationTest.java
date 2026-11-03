package com.improvedmenus;

import java.util.Arrays;
import java.util.EnumSet;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.eventbus.EventBus;
import org.junit.Test;
import static org.mockito.Mockito.*;

public class PlayerIdentificationIntegrationTest
{
    private static void field(Object owner, String name, Object value) throws Exception
    {
        var field = owner.getClass().getDeclaredField(name); field.setAccessible(true); field.set(owner, value);
    }

    @Test public void gameEventsQueueLoadedPlayersAndRefreshOpenMenusWithoutChangingActions() throws Exception
    {
        ImprovedMenusPlugin plugin = new ImprovedMenusPlugin();
        Client client = mock(Client.class); ImprovedMenusConfig config = mock(ImprovedMenusConfig.class);
        PlayerAccountBadges accounts = mock(PlayerAccountBadges.class);
        PlayerMenuBadges badges = new PlayerMenuBadges(client, config,
            mock(net.runelite.client.game.ChatIconManager.class), mock(net.runelite.client.party.PartyService.class),
            accounts, mock(net.runelite.client.ui.overlay.tooltip.TooltipManager.class));
        badges.start(); when(config.playerIronmanIdentificationBeta()).thenReturn(true);
        when(accounts.find("Alice")).thenReturn(new PlayerAccountBadges.Badge(2, false));
        WorldView view = mock(WorldView.class, RETURNS_DEEP_STUBS);
        Player self = mock(Player.class), alice = mock(Player.class), bob = mock(Player.class);
        when(alice.getName()).thenReturn("Alice"); when(bob.getName()).thenReturn("Bob");
        var loaded = view.players();
        doReturn(Arrays.asList(self, alice, bob).iterator()).when(loaded).iterator();
        when(client.getLocalPlayer()).thenReturn(self); when(client.getTopLevelWorldView()).thenReturn(view);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getWorldType()).thenReturn(EnumSet.of(WorldType.MEMBERS));
        when(config.playerIronmanIdentificationBeta()).thenReturn(true);
        MenuEntry row = TestMenus.entry("Follow", "Alice", MenuAction.PLAYER_SECOND_OPTION, -1, 0);
        when(row.getPlayer()).thenReturn(alice); Menu menu = TestMenus.menu(row);
        when(client.getMenu()).thenReturn(menu); when(client.isMenuOpen()).thenReturn(true);
        field(plugin, "client", client); field(plugin, "config", config); field(plugin, "active", true);
        field(plugin, "accountBadges", accounts); field(plugin, "playerBadges", badges);
        MenuPresentation presentation = new MenuPresentation(); field(presentation, "config", config);
        LootFiltersMenuPresentation lootMenu = new LootFiltersMenuPresentation(); field(lootMenu, "config", config);
        field(plugin, "presentation", presentation); field(plugin, "lootMenu", lootMenu);
        field(plugin, "lootCompatibility", mock(LootFiltersCompatibility.class));
        EventBus bus = new EventBus(error -> { throw new AssertionError(error); }); bus.register(plugin);
        bus.post(new GameTick()); verify(accounts).nearby(Arrays.asList("Alice", "Bob")); verify(accounts).tick();
        when(accounts.revision()).thenReturn(1L); bus.post(new ClientTick()); verify(accounts).find("Alice");

        when(accounts.revision()).thenReturn(2L); bus.post(new ClientTick()); verify(accounts, times(2)).find("Alice");
        when(client.getVarbitValue(net.runelite.api.gameval.VarbitID.INSIDE_WILDERNESS)).thenReturn(1);
        bus.post(new GameTick()); verify(accounts).nearby(java.util.Collections.emptyList());
        bus.post(new ClientTick()); verify(accounts, times(3)).find("Alice");
        org.junit.Assert.assertEquals(MenuAction.PLAYER_SECOND_OPTION, row.getType());
        org.junit.Assert.assertEquals("Follow", row.getOption());
        bus.unregister(plugin);
    }
}
