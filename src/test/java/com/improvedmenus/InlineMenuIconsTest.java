package com.improvedmenus;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.runelite.api.Client;
import net.runelite.api.IndexedSprite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ChatIconManager;
import net.runelite.client.util.AsyncBufferedImage;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class InlineMenuIconsTest
{
	@Test
	public void reuseUpdatesPixelsWithoutAllocatingAnotherRegistration()
	{
		Fixture f = new Fixture();
		assertEquals("<img=0>", f.pool.add(() -> solid(Color.RED)));
		f.pool.refresh();
		assertEquals(Color.RED.getRGB(), f.images[0].getRGB(8, 7));
		f.pool.clear();
		assertEquals("<img=0>", f.pool.add(() -> solid(Color.BLUE)));
		assertEquals("<img=1>", f.pool.add(() -> solid(Color.GREEN)));
		f.pool.refresh();
		assertEquals(Color.BLUE.getRGB(), f.images[0].getRGB(8, 7));
		assertEquals(Color.GREEN.getRGB(), f.images[1].getRGB(8, 7));
		verify(f.icons, times(2)).reserveChatIcon();
	}

	@Test
	public void unchangedFramesAndClosedMenuDoNotRewriteSprites()
	{
		Fixture f = new Fixture();
		BufferedImage image = solid(Color.RED);
		f.pool.add(() -> image);
		f.pool.refresh();
		clearInvocations(f.icons);
		for (int i = 0; i < 100; i++)
			f.pool.refresh();
		verify(f.icons, never()).updateChatIcon(anyInt(), any());
		f.sprites[0] = mock(IndexedSprite.class);
		f.pool.refresh();
		verify(f.icons).updateChatIcon(eq(0), any());
		f.pool.clear();
		clearInvocations(f.icons);
		f.pool.refresh();
		verifyNoInteractions(f.icons);
	}

	@Test
	public void asyncCompletionRefreshesCurrentImageAndCannotRestoreAnOldAssignment()
	{
		Fixture f = new Fixture();
		ClientThread thread = mock(ClientThread.class);
		AsyncBufferedImage pending = new AsyncBufferedImage(thread, 17, 15, BufferedImage.TYPE_INT_ARGB);
		AtomicReference<BufferedImage> selected = new AtomicReference<>(pending);
		f.pool.add(selected::get);
		f.pool.refresh();
		pending.setRGB(8, 7, Color.RED.getRGB());
		pending.loaded();
		f.pool.refresh();
		assertEquals(Color.RED.getRGB(), f.images[0].getRGB(8, 7));
		selected.set(solid(Color.BLUE));
		pending.loaded();
		f.pool.refresh();
		assertEquals(Color.BLUE.getRGB(), f.images[0].getRGB(8, 7));
	}

	@Test
	public void maximumRegistrationsStayBoundedAcrossMenus()
	{
		Fixture f = new Fixture();
		for (int menu = 0; menu < 3; menu++)
		{
			for (int i = 0; i < 1000; i++)
				assertEquals("<img=" + i + ">", f.pool.add(() -> null));
			assertEquals("", f.pool.add(() -> null));
			f.pool.clear();
		}
		verify(f.icons, times(1000)).reserveChatIcon();
	}

	private static BufferedImage solid(Color color)
	{
		BufferedImage image = new BufferedImage(17, 15, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < 15; y++)
			for (int x = 0; x < 17; x++)
				image.setRGB(x, y, color.getRGB());
		return image;
	}

	private static class Fixture
	{
		final Client client = mock(Client.class);
		final ChatIconManager icons = mock(ChatIconManager.class);
		final IndexedSprite[] sprites = new IndexedSprite[1000];
		final BufferedImage[] images = new BufferedImage[1000];
		final InlineMenuIcons pool = new InlineMenuIcons(client, icons);

		Fixture()
		{
			AtomicInteger next = new AtomicInteger();
			when(icons.reserveChatIcon()).thenAnswer(call -> next.getAndIncrement());
			when(icons.chatIconIndex(anyInt())).thenAnswer(call -> call.getArgument(0));
			when(client.getModIcons()).thenReturn(sprites);
			doAnswer(call ->
			{
				int index = call.getArgument(0);
				images[index] = call.getArgument(1);
				sprites[index] = mock(IndexedSprite.class);
				return null;
			}).when(icons).updateChatIcon(anyInt(), any());
		}
	}
}
