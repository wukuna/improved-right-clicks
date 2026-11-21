package com.improvedmenus;

import java.awt.Color;
import java.awt.Font;
import java.util.Map;
import javax.inject.Singleton;
import net.runelite.client.events.PluginMessage;

@Singleton
final class MenuStyle
{
	static final String NAMESPACE = "right-click-styler";
	private volatile Style current;

	void clear()
	{
		current = null;
	}

	Style current()
	{
		return current;
	}

	void accept(PluginMessage event)
	{
		if (!NAMESPACE.equals(event.getNamespace()) || !"style".equals(event.getName()))
			return;
		Map<String, Object> data = event.getData();
		if (Boolean.FALSE.equals(data.get("active")))
		{
			clear();
			return;
		}
		if (!Boolean.TRUE.equals(data.get("active")) || !Integer.valueOf(1).equals(data.get("version"))
				|| !(data.get("font") instanceof Font) || !(data.get("text") instanceof Color)
				|| !(data.get("background") instanceof Color) || !(data.get("border") instanceof Color)
				|| !(data.get("radius") instanceof Integer) || !(data.get("showBorder") instanceof Boolean)
				|| !(data.get("showShadow") instanceof Boolean) || !(data.get("textShadow") instanceof Boolean))
			return;
		Font font = (Font) data.get("font");
		int radius = (Integer) data.get("radius");
		if (font.getSize() < 1 || font.getSize() > 64 || radius < 0 || radius > 64)
			return;
		current = new Style(font, (Color) data.get("text"), (Color) data.get("background"),
				(Color) data.get("border"), radius, (Boolean) data.get("showBorder"),
				(Boolean) data.get("showShadow"), (Boolean) data.get("textShadow"));
	}

	static final class Style
	{
		final Font font;
		final Color text, background, border;
		final int radius;
		final boolean showBorder, showShadow, textShadow;

		Style(Font font, Color text, Color background, Color border, int radius, boolean showBorder,
				boolean showShadow, boolean textShadow)
		{
			this.font = font;
			this.text = text;
			this.background = background;
			this.border = border;
			this.radius = radius;
			this.showBorder = showBorder;
			this.showShadow = showShadow;
			this.textShadow = textShadow;
		}
	}
}
