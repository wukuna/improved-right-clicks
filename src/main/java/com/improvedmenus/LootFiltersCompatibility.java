package com.improvedmenus;

import com.improvedmenus.ImprovedMenusConfig.CustomIconProvider;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.MenuEntry;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;

@Singleton
class LootFiltersCompatibility
{
	private final Client client;
	private final ImprovedMenusConfig config;
	private final PluginManager plugins;
	private final EventBus events;
	private final LootFilterImageCache images;
	private boolean collecting, iconsEnabled;
	private CustomIconProvider menuProvider = CustomIconProvider.OFF, iconProvider = CustomIconProvider.OFF;
	private final Map<Tile, Map<Integer, Result>> menuResults = new IdentityHashMap<>();
	private final Map<Tile, Map<Integer, Result>> iconResults = new IdentityHashMap<>();
	private volatile String menuStatus = "Loot menu settings are off.", iconStatus = "Custom icon provider is Off.";

	@Inject
	LootFiltersCompatibility(Client client, ClientThread clientThread, ImprovedMenusConfig config,
			PluginManager plugins, EventBus events)
	{
		this.client = client;
		this.config = config;
		this.plugins = plugins;
		this.events = events;
		images = new LootFilterImageCache(clientThread::invokeLater);
	}

	static final class Result
	{
		final BufferedImage image;
		final boolean itemIcon, hidden, hideOverlay;
		final int menuSort;
		final Color menuColor;
		final Map<String, Object> icon;
		final Supplier<?> provider;

		Result(BufferedImage image, boolean itemIcon)
		{
			this(image, itemIcon, false, false, 0, null);
		}

		Result(BufferedImage image, boolean itemIcon, boolean hidden, boolean hideOverlay, int menuSort,
				Color menuColor)
		{
			this(image, itemIcon, hidden, hideOverlay, menuSort, menuColor, null, null);
		}

		Result(BufferedImage image, boolean itemIcon, boolean hidden, boolean hideOverlay, int menuSort,
				Color menuColor, Map<String, Object> icon, Supplier<?> provider)
		{
			this.image = image;
			this.itemIcon = itemIcon;
			this.hidden = hidden;
			this.hideOverlay = hideOverlay;
			this.menuSort = menuSort;
			this.menuColor = menuColor;
			this.icon = icon;
			this.provider = provider;
		}

		boolean hasIcon()
		{
			return !hidden && !hideOverlay && (image != null || icon != null && provider != null);
		}
	}

	String status()
	{
		return iconStatus + " " + menuStatus;
	}

	long revision()
	{
		return images.revision();
	}

	void refresh()
	{
		if (!client.isClientThread())
			return;
		CustomIconProvider nextMenu = CustomIconProvider.OFF;
		if (config.useLootFiltersInMenu())
		{
			if (available(CustomIconProvider.IMPROVED_LOOTING, false))
				nextMenu = CustomIconProvider.IMPROVED_LOOTING;
			else if (available(CustomIconProvider.LOOT_FILTERS, false))
				nextMenu = CustomIconProvider.LOOT_FILTERS;
		}
		CustomIconProvider nextIcon = config.customIconProvider();
		if (nextIcon == null)
			nextIcon = CustomIconProvider.OFF;
		boolean ready = available(nextIcon, true);
		if (nextIcon != iconProvider || ready != iconsEnabled)
		{
			images.clear();
			endMenu();
			iconProvider = nextIcon;
			iconsEnabled = ready;
			iconStatus = ready ? "Custom icons: " + iconProvider + "." : "Enable " + iconProvider + " for custom icons.";
		}
		if (nextMenu != menuProvider)
		{
			endMenu();
			menuProvider = nextMenu;
			menuStatus = "Loot menu settings: " + menuProvider + ".";
		}
		if (iconProvider == CustomIconProvider.OFF)
			iconStatus = "Custom icon provider is Off.";
		if (menuProvider == CustomIconProvider.OFF)
			menuStatus = config.useLootFiltersInMenu() ? "Enable a loot provider to use its menu settings."
					: "Loot menu settings are off.";
	}

	private boolean available(CustomIconProvider provider, boolean iconsOnly)
	{
		if (provider == CustomIconProvider.OFF)
			return false;
		for (Plugin plugin : plugins.getPlugins())
			if ((provider.label.equals(plugin.getName()) || (provider.label + " TEST").equals(plugin.getName()))
					&& plugins.isPluginActive(plugin))
			{
				if (provider == CustomIconProvider.LOOT_FILTERS)
					return true;
				Map<String, Object> capability = new HashMap<>();
				if (iconsOnly)
					capability.put("iconsOnly", true);
				events.post(new PluginMessage(provider.namespace, "provider-status-v1", capability));
				return Boolean.TRUE.equals(capability.get("enabled"));
			}
		return false;
	}

	void reset()
	{
		endMenu();
		images.clear();
		iconsEnabled = false;
		menuProvider = iconProvider = CustomIconProvider.OFF;
		iconStatus = "Custom icon provider is Off.";
		menuStatus = "Open a Take menu to check loot menu settings.";
	}

	void beginMenu()
	{
		refresh();
		menuResults.clear();
		iconResults.clear();
		collecting = true;
	}

	void endMenu()
	{
		menuResults.clear();
		iconResults.clear();
		collecting = false;
	}

	Result findIcon(MenuEntry entry)
	{
		Result result = find(entry, true);
		image(result);
		return result;
	}

	BufferedImage image(Result result)
	{
		if (result == null || !result.hasIcon() || !client.isClientThread())
			return null;
		if (result.image != null)
			return result.image;
		return images.get(result.icon, result.provider);
	}

	Result find(MenuEntry entry)
	{
		return find(entry, false);
	}

	private Result find(MenuEntry entry, boolean forIcons)
	{
		if (!(forIcons ? iconsEnabled : menuProvider != CustomIconProvider.OFF) || !client.isClientThread())
			return null;
		WorldView view = client.getWorldView(entry.getWorldViewId());
		if (view == null || view.getScene() == null)
			return null;
		Tile[][][] tiles = view.getScene().getTiles();
		int plane = view.getPlane(), x = entry.getParam0(), y = entry.getParam1();
		if (tiles == null || plane < 0 || plane >= tiles.length || tiles[plane] == null || x < 0
				|| x >= tiles[plane].length || tiles[plane][x] == null || y < 0 || y >= tiles[plane][x].length
				|| tiles[plane][x][y] == null)
			return null;
		if (!collecting)
			return request(view, x, y, plane, entry.getIdentifier(), forIcons);
		Map<Integer, Result> results = (forIcons ? iconResults : menuResults)
				.computeIfAbsent(tiles[plane][x][y], ignored -> new HashMap<>());
		if (!results.containsKey(entry.getIdentifier()))
			results.put(entry.getIdentifier(), request(view, x, y, plane, entry.getIdentifier(), forIcons));
		return results.get(entry.getIdentifier());
	}

	Result request(WorldView view, int x, int y, int plane, int itemId)
	{
		return request(view, x, y, plane, itemId, false);
	}

	private Result request(WorldView view, int x, int y, int plane, int itemId, boolean forIcons)
	{
		CustomIconProvider provider = forIcons ? iconProvider : menuProvider;
		if (provider == CustomIconProvider.OFF)
			return null;
		Map<String, Object> data = new HashMap<>();
		data.put("worldViewId", view.getId());
		data.put("x", view.getBaseX() + x);
		data.put("y", view.getBaseY() + y);
		data.put("z", plane);
		data.put("itemId", itemId);
		if (forIcons && provider == CustomIconProvider.IMPROVED_LOOTING)
			data.put("iconsOnly", true);
		events.post(new PluginMessage(provider.namespace, "resolved-display-request-v1", data));
		Result result = decode(data, itemId);
		String status = result != null
				? (forIcons ? "Custom icons: " : "Loot menu settings: ") + provider + "."
				: data.containsKey("status")
						? provider + " has no ready, unambiguous display settings for this pile."
						: "This " + provider
								+ " version does not provide resolved display settings. A compatible provider update is required.";
		if (forIcons)
			iconStatus = status;
		else
			menuStatus = status;
		return result;
	}

	static Result decode(Map<String, Object> data, int itemId)
	{
		if (!"ok".equals(data.get("status")) || !(data.get("display") instanceof Map))
			return null;
		Map<?, ?> display = (Map<?, ?>) data.get("display");
		if (!(display.get("hidden") instanceof Boolean) || !(display.get("hideOverlay") instanceof Boolean)
				|| !(display.get("menuSort") instanceof Integer)
				|| display.get("menuTextColor") != null && !(display.get("menuTextColor") instanceof Color))
			return null;
		Map<String, Object> descriptor = null;
		Supplier<?> provider = null;
		boolean same = false;
		if (display.get("icon") != null)
		{
			if (!(display.get("icon") instanceof Map))
				return null;
			Map<?, ?> icon = (Map<?, ?>) display.get("icon");
			Object type = icon.get("type");
			if (!(type instanceof String))
				return null;
			if (icon.get("getImage") instanceof Supplier)
				provider = (Supplier<?>) icon.get("getImage");
			descriptor = new HashMap<>();
			descriptor.put("type", type);
			for (String key : Arrays.asList("id", "index", "quantity", "height", "filename"))
			{
				Object value = icon.get(key);
				if (value != null && !(value instanceof Integer)
						&& !(key.equals("filename") && value instanceof String))
					return null;
				if (value != null)
					descriptor.put(key, value);
			}
			descriptor = Collections.unmodifiableMap(descriptor);
			same = ("item".equals(type) || "current-item".equals(type))
					&& Integer.valueOf(itemId).equals(icon.get("id"));
		}
		return new Result(null, same, (Boolean) display.get("hidden"), (Boolean) display.get("hideOverlay"),
				(Integer) display.get("menuSort"), (Color) display.get("menuTextColor"), descriptor, provider);
	}
}
