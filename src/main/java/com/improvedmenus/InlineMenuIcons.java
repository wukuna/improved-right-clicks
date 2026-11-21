package com.improvedmenus;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.IndexedSprite;
import net.runelite.client.game.ChatIconManager;
import net.runelite.client.util.AsyncBufferedImage;

final class InlineMenuIcons
{
	private static final int MAX_ICONS = 1000;
	private final Client client;
	private final ChatIconManager icons;
	private final List<Slot> slots = new ArrayList<>();
	private int used;

	InlineMenuIcons(Client client, ChatIconManager icons)
	{
		this.client = client;
		this.icons = icons;
	}

	void clear()
	{
		for (int i = 0; i < used; i++)
			slots.get(i).supplier = null;
		used = 0;
	}

	String add(Supplier<BufferedImage> supplier)
	{
		if (used == MAX_ICONS)
			return "";
		if (used == slots.size())
		{
			int id = icons.reserveChatIcon();
			icons.updateChatIcon(id, new BufferedImage(17, 15, BufferedImage.TYPE_INT_ARGB));
			slots.add(new Slot(id));
		}
		Slot slot = slots.get(used++);
		slot.supplier = supplier;
		int index = icons.chatIconIndex(slot.id);
		return index < 0 ? "" : "<img=" + index + ">";
	}

	void refresh()
	{
		for (int i = 0; i < used; i++)
		{
			Slot slot = slots.get(i);
			BufferedImage image = slot.supplier.get();
			if (image != slot.image)
			{
				slot.image = image;
				slot.dirty = true;
				if (image instanceof AsyncBufferedImage)
					((AsyncBufferedImage) image).onLoaded(() -> slot.dirty = true);
			}
			int index = icons.chatIconIndex(slot.id);
			IndexedSprite[] sprites = client.getModIcons();
			if (index < 0 || sprites == null || index >= sprites.length)
				continue;
			if (!slot.dirty && sprites[index] == slot.sprite)
				continue;
			slot.dirty = false;
			BufferedImage copy = new BufferedImage(17, 15, BufferedImage.TYPE_INT_ARGB);
			if (image != null)
			{
				Graphics2D graphics = copy.createGraphics();
				try
				{
					graphics.drawImage(image, 0, 0, 17, 15, null);
				}
				finally
				{
					graphics.dispose();
				}
			}
			icons.updateChatIcon(slot.id, copy);
			slot.sprite = client.getModIcons()[index];
		}
	}

	private static final class Slot
	{
		private final int id;
		private Supplier<BufferedImage> supplier;
		private BufferedImage image;
		private IndexedSprite sprite;
		private volatile boolean dirty = true;

		private Slot(int id)
		{
			this.id = id;
		}
	}
}
