package com.improvedmenus;

import com.google.inject.Provides;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.*;
import javax.inject.Inject;
import javax.swing.*;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(name = "Improved Right-Clicks", description = "Customize right-click menus with misclick protection, loot icons and persistent observed account badges.", tags = {
		"menu", "bank", "inventory", "misclick", "examine", "color" })
public class ImprovedMenusPlugin extends Plugin
{
	@Inject
	private Client client;
	@Inject
	private ClientThread clientThread;
	@Inject
	private ImprovedMenusConfig config;
	@Inject
	private EventBus eventBus;
	@Inject
	private ButtonGuard buttons;
	@Inject
	private MenuEngine engine;
	@Inject
	private ProfileStore store;
	@Inject
	private RenameNotice renameNotice;
	@Inject
	private ClientToolbar toolbar;
	@Inject
	private OverlayManager overlays;
	@Inject
	private MenuPresentation presentation;
	@Inject
	private MenuThemeRenderer menuTheme;
	@Inject
	private MenuStyle menuStyle;
	@Inject
	private OptionCountOverlay optionCounts;
	@Inject
	private ItemIcons itemIcons;
	@Inject
	private LootFiltersCompatibility lootCompatibility;
	@Inject
	private LootFiltersMenuPresentation lootMenu;
	@Inject
	private PlayerMenuBadges playerBadges;
	@Inject
	private AccountBadgeStore accountBadgeStore;
	@Inject
	private ReleaseNotesNotice releaseNotes;
	private ImprovedMenusPanel panel;
	private NavigationButton navigation;
	private volatile boolean active;
	private long lootRevision;

	@Provides
	ImprovedMenusConfig provideConfig(ConfigManager manager)
	{
		return manager.getConfig(ImprovedMenusConfig.class);
	}

	@Override
	protected void startUp()
	{
		active = true;
		accountBadgeStore.start(playerBadges.accountBadges(), this::getPluginDirectory);
		presentation.setItemIcons(itemIcons);
		if (itemIcons.filters != null)
		{
			itemIcons.filters.reload();
		}
		renameNotice.startUp();
		releaseNotes.startUp();
		clientThread.invokeLater(() ->
		{
			if (active)
			{
				engine.reload();
				lootCompatibility.refresh();
				playerBadges.start();
				requestMenuStyle();
			}
		});
		buttons.startUp();
		eventBus.register(buttons);
		eventBus.register(menuTheme);
		overlays.add(optionCounts);
		overlays.add(playerBadges);
		presentation.start();
		SwingUtilities.invokeLater(this::updateSidePanel);
	}

	@Override
	protected void shutDown()
	{
		active = false;
		menuStyle.clear();
		accountBadgeStore.stop();
		eventBus.unregister(menuTheme);
		renameNotice.shutDown();
		releaseNotes.shutDown();
		eventBus.unregister(buttons);
		buttons.shutDown();
		overlays.remove(optionCounts);
		overlays.remove(playerBadges);
		clientThread.invoke(() ->
		{
			playerBadges.shutdown();
			presentation.stop();
			lootMenu.stop();
			lootCompatibility.reset();
			engine.clear();
		});
		store.clearHistory();
		SwingUtilities.invokeLater(() ->
		{
			if (panel != null)
			{
				panel.stop();
				panel = null;
			}
			if (navigation != null)
			{
				toolbar.removeNavigation(navigation);
				navigation = null;
			}
		});
	}

	@Subscribe
	public void onClientShutdown(net.runelite.client.events.ClientShutdown event)
	{
		event.waitFor(accountBadgeStore.flush());
	}

	@Subscribe(priority = 100)
	public void onChatMessage(ChatMessage event)
	{
		if (active && client.getGameState() == GameState.LOGGED_IN)
			playerBadges.observeAccountBadge(event);
	}

	@Subscribe(priority = -0.5f)
	public void onPostMenuSort(PostMenuSort event)
	{
		if (!client.isMenuOpen())
		{
			engine.apply(client.getMenu());
		}
	}

	@Subscribe(priority = -1)
	public void onClientTick(ClientTick event)
	{
		if (!active || client.getGameState() != GameState.LOGGED_IN)
			return;
		if (!client.isMenuOpen())
			engine.applyTargetProtections(client.getMenu());
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (!active)
			return;
		lootCompatibility.refresh();
		long revision = lootCompatibility.revision();
		if (revision != lootRevision)
		{
			lootRevision = revision;
			if (client.isMenuOpen())
				decorateMenu();
		}
	}

	@Subscribe(priority = -2)
	public void onMenuOpened(MenuOpened event)
	{
		playerBadges.clear();
		presentation.stop();
		lootMenu.stop();
		engine.applyOpened(client.getMenu());

		engine.groupOpened(client.getMenu());
		decorateMenu();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (ImprovedMenusConfig.GROUP.equals(event.getGroup()))
		{
			if (itemIcons.filters != null
					&& (event.getKey().equals(FilterIcons.KEY) || event.getKey().equals(FilterIcons.BACKUP)))
			{
				itemIcons.filters.reload();
			}
			clientThread.invokeLater(() ->
			{
				if (active)
				{
					playerBadges.clear();
					presentation.stop();
					lootMenu.stop();
					lootCompatibility.reset();
					lootCompatibility.refresh();
					engine.reload();
					if (client.isMenuOpen())
						decorateMenu();
				}
			});
			if (event.getKey().equals("hideSidePanel"))
			{
				SwingUtilities.invokeLater(this::updateSidePanel);
			}
		}
		else if ("loot-filters".equals(event.getGroup()) || "improvedlooting".equals(event.getGroup()))
			refreshLootFilter();
	}

	@Subscribe
	public void onProfileChanged(net.runelite.client.events.ProfileChanged event)
	{
		menuStyle.clear();
		clientThread.invokeLater(() ->
		{
			if (active)
			{
				playerBadges.clear();
				releaseNotes.showIfLoggedIn();
				requestMenuStyle();
			}
		});
		refreshLootFilter();
		if (itemIcons.filters != null)
		{
			itemIcons.filters.reload();
		}
	}

	@Subscribe
	public void onPluginChanged(net.runelite.client.events.PluginChanged event)
	{
		if ("Right Click Styler".equals(event.getPlugin().getName()))
		{
			menuStyle.clear();
			clientThread.invokeLater(() ->
			{
				if (active)
					requestMenuStyle();
			});
		}
		if ("Loot Filters".equals(event.getPlugin().getName())
				|| "Loot Filters TEST".equals(event.getPlugin().getName())
				|| "Improved Looting".equals(event.getPlugin().getName())
				|| "Improved Looting TEST".equals(event.getPlugin().getName()))
			refreshLootFilter();
	}

	@Subscribe
	public void onPluginMessage(net.runelite.client.events.PluginMessage event)
	{
		if (active)
			menuStyle.accept(event);
		if (("loot-filters".equals(event.getNamespace()) || "improved-looting".equals(event.getNamespace()))
				&& "filter-reloaded".equals(event.getName()))
			refreshLootFilter();
	}

	private void requestMenuStyle()
	{
		eventBus.post(new net.runelite.client.events.PluginMessage(MenuStyle.NAMESPACE, "style-request"));
	}

	private void refreshLootFilter()
	{
		clientThread.invokeLater(() ->
		{
			if (!active)
				return;
			playerBadges.clear();
			presentation.stop();
			lootMenu.stop();
			lootCompatibility.reset();
			lootCompatibility.refresh();
			if (client.isMenuOpen())
				decorateMenu();
		});
	}

	private void decorateMenu()
	{

		playerBadges.clear();
		presentation.stop();
		lootMenu.stop();
		lootCompatibility.beginMenu();
		try
		{
			lootMenu.apply(client.getMenu());
			presentation.decorate(client.getMenu());
			playerBadges.decorate(client.getMenu());
		}
		finally
		{
			lootCompatibility.endMenu();
		}
	}

	@Subscribe(priority = 1)
	public void onBeforeMenuRender(BeforeMenuRender event)
	{
		if (active && client.isMenuOpen())
		{
			presentation.refreshIcons();
			engine.highlightHovered(client.getMenu());
		}
	}

	private void updateSidePanel()
	{
		if (!active || config.hideSidePanel())
		{
			if (navigation != null)
			{
				toolbar.removeNavigation(navigation);
				navigation = null;
			}
			if (panel != null)
			{
				panel.stop();
				panel = null;
			}
		}
		else if (navigation == null)
		{
			panel = new ImprovedMenusPanel(store, engine, this::openSettings, lootCompatibility::status);
			navigation = NavigationButton.builder().tooltip("Improved Right-Clicks").icon(icon()).priority(6)
					.panel(panel).build();
			toolbar.addNavigation(navigation);
		}
	}

	private void openSettings()
	{
		net.runelite.client.ui.overlay.Overlay target = new net.runelite.client.ui.overlay.Overlay(this)
		{
			@Override
			public java.awt.Dimension render(Graphics2D graphics)
			{
				return null;
			}
		};
		eventBus.post(new net.runelite.client.events.OverlayMenuClicked(
				new net.runelite.client.ui.overlay.OverlayMenuEntry(MenuAction.RUNELITE_OVERLAY_CONFIG,
						net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE, "Improved Right-Clicks"),
				target));
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() != GameState.LOGGED_IN)
		{
			playerBadges.clear();
			presentation.stop();
			lootMenu.stop();
			lootCompatibility.reset();
			buttons.reset();
			engine.clear();
		}
		else
		{
			renameNotice.showIfLoggedIn();
			releaseNotes.showIfLoggedIn();
		}
	}

	@Subscribe
	public void onFocusChanged(FocusChanged event)
	{
		if (!event.isFocused())
		{
			clientThread.invoke(buttons::reset);
		}
	}


	private static BufferedImage icon()
	{
		return net.runelite.client.util.ImageUtil.loadImageResource(ImprovedMenusPlugin.class, "sidebar-icon.png");
	}
}
