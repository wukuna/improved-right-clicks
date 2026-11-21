package com.improvedmenus;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.Rectangle;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.components.TooltipComponent;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

final class OptionCountTooltip extends TooltipComponent
{
	private final String text;
	private final TooltipManager manager;
	private final Client client;
	private Point location = new Point();
	private final Rectangle bounds = new Rectangle();
	private final MenuStyle.Style style;
	private final boolean rightAligned;

	OptionCountTooltip(String text, TooltipManager manager, Client client, Color background)
	{
		this(text, manager, client, background, null, true);
	}

	OptionCountTooltip(String text, TooltipManager manager, Client client, Color background,
			MenuStyle.Style style, boolean rightAligned)
	{
		this.style = style;
		this.rightAligned = rightAligned;
		this.text = text;
		this.manager = manager;
		this.client = client;
		setText(text);
		if (background != null)
		{
			setBackgroundColor(background);
		}
	}

	@Override
	public Rectangle getBounds()
	{
		return bounds;
	}

	@Override
	public void setPreferredLocation(Point point)
	{
		location = new Point(point);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{

		Graphics2D measuring = (Graphics2D) graphics.create();
		Dimension own;
		int width;
		try
		{
			measuring.setClip(0, 0, 0, 0);
			TooltipComponent measure = new TooltipComponent();
			measure.setText(text);
			if (style == null)
				own = measure.render(measuring);
			else
			{
				FontMetrics metrics = measuring.getFontMetrics(style.font);
				own = new Dimension(metrics.stringWidth(MenuRules.plain(text)) + 12, metrics.getHeight() + 8);
			}
			width = own.width;
			for (Tooltip tooltip : manager.getTooltips())
			{
				if (tooltip.getText() != null)
				{
					measure.setText(tooltip.getText());
					width = Math.max(width, measure.render(measuring).width);
				}
				else if (tooltip.getComponent() != null && tooltip.getComponent() != this)
				{
					Rectangle bounds = tooltip.getComponent().getBounds();
					if (bounds != null)
					{
						width = Math.max(width, bounds.width);
					}
				}
			}
		}
		finally
		{
			measuring.dispose();
		}
		int x = Math.max(0, Math.min(location.x + (rightAligned ? width - own.width : 0), client.getCanvasWidth() - own.width));
		int y = Math.max(0, Math.min(location.y, client.getCanvasHeight() - own.height));
		super.setPreferredLocation(new Point(x, y));
		if (style == null)
			super.render(graphics);
		else
		{
			Graphics2D themed = (Graphics2D) graphics.create();
			try
			{
				if (style.showShadow)
					for (int i = 3; i >= 1; i--)
					{
						themed.setColor(new Color(0, 0, 0, (4 - i) * 15));
						themed.fillRoundRect(x + i, y + i, own.width, own.height, style.radius, style.radius);
					}
				themed.setColor(style.background);
				themed.fillRoundRect(x, y, own.width, own.height, style.radius, style.radius);
				if (style.showBorder)
				{
					themed.setColor(style.border);
					themed.drawRoundRect(x, y, own.width - 1, own.height - 1, style.radius, style.radius);
				}
				themed.setFont(style.font);
				int baseline = y + 4 + themed.getFontMetrics().getAscent();
				String plain = MenuRules.plain(text);
				if (style.textShadow)
				{
					themed.setColor(Color.BLACK);
					themed.drawString(plain, x + 7, baseline + 1);
				}
				themed.setColor(style.text);
				themed.drawString(plain, x + 6, baseline);
			}
			finally
			{
				themed.dispose();
			}
		}
		bounds.setBounds(x, y, own.width, own.height);
		return new Dimension(rightAligned ? width : own.width, own.height);
	}
}
