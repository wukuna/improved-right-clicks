package com.improvedmenus;

import java.awt.Color;
import java.awt.Font;
import java.util.HashMap;
import java.util.Map;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.events.PluginMessage;
import org.junit.Test;
import static org.junit.Assert.*;

public class MenuStyleTest
{
	@Test
	public void receivesLiveUpdatesAndClearsWhenProviderStops()
	{
		MenuStyle receiver = new MenuStyle();
		Map<String, Object> values = style(Color.MAGENTA);
		receiver.accept(new PluginMessage(MenuStyle.NAMESPACE, "style", values));
		assertEquals(Color.MAGENTA, receiver.current().text);
		values.put("text", Color.CYAN);
		assertEquals(Color.MAGENTA, receiver.current().text);
		receiver.accept(new PluginMessage(MenuStyle.NAMESPACE, "style", values));
		assertEquals(Color.CYAN, receiver.current().text);
		receiver.accept(new PluginMessage(MenuStyle.NAMESPACE, "style", Map.of("active", false)));
		assertNull(receiver.current());
	}

	@Test
	public void ignoresUnrelatedIncompleteAndUnsupportedMessages()
	{
		MenuStyle receiver = new MenuStyle();
		Map<String, Object> values = style(Color.RED);
		receiver.accept(new PluginMessage("other", "style", values));
		assertNull(receiver.current());
		values.put("version", 2);
		receiver.accept(new PluginMessage(MenuStyle.NAMESPACE, "style", values));
		assertNull(receiver.current());
		values.put("version", 1);
		values.remove("font");
		receiver.accept(new PluginMessage(MenuStyle.NAMESPACE, "style", values));
		assertNull(receiver.current());
	}

	@Test
	public void keepsSavedSettingsAndMakesFollowingOptIn() throws Exception
	{
		ImprovedMenusConfig defaults = new ImprovedMenusConfig() {};
		assertFalse(defaults.followMenuStyle());
		assertTrue(defaults.enableColors());
		for (String key : new String[]{"colors", "yellowHover", "colorPicker", "defaultPickerColor", "optionCountColor", "menuTheme"})
		{
			ConfigItem item = ImprovedMenusConfig.class.getMethod(key).getAnnotation(ConfigItem.class);
			assertEquals(key, item.keyName());
			assertTrue(key, item.hidden());
			assertTrue(key, SettingsCatalog.ALL.containsKey(key));
		}
		assertFalse(ImprovedMenusConfig.class.getMethod("lootFiltersMenuColors").getAnnotation(ConfigItem.class).hidden());
	}

	static Map<String, Object> style(Color text)
	{
		Map<String, Object> values = new HashMap<>();
		values.put("version", 1);
		values.put("active", true);
		values.put("font", new Font(Font.SANS_SERIF, Font.PLAIN, 14));
		values.put("text", text);
		values.put("background", Color.BLACK);
		values.put("border", Color.GRAY);
		values.put("radius", 4);
		values.put("showBorder", true);
		values.put("showShadow", true);
		values.put("textShadow", false);
		return values;
	}
}
