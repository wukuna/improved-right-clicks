package com.improvedmenus;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.AsyncBufferedImage;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class LootFilterAsyncImageTest {
    private final List<Runnable> tasks = new ArrayList<>();
    private final LootFilterImageCache cache = new LootFilterImageCache(tasks::add);
    private final Map<String, Object> key = Map.of("type", "item", "id", 4151);

    @Test
    public void waitsForPixelsAndCopiesOnClientThread() {
        AsyncBufferedImage source = image(36, 32);
        AtomicInteger calls = new AtomicInteger();
        Supplier<?> provider = () -> {
            calls.incrementAndGet();
            return source;
        };
        assertNull(cache.get(key, provider));
        assertNull(cache.get(key, provider));
        assertEquals(1, calls.get());
        fill(source, Color.CYAN);
        source.loaded();
        assertNull(cache.get(key, provider));
        drain();
        BufferedImage result = cache.get(key, provider);
        assertEquals(17, result.getWidth());
        assertEquals(15, result.getHeight());
        assertEquals(Color.CYAN.getRGB(), result.getRGB(8, 7));
        assertNotSame(source, result);
        assertEquals(1, calls.get());
    }

    @Test
    public void alreadyLoadedSmallImagesAreCopiedWithoutChangingAlpha() {
        AsyncBufferedImage source = image(4, 4);
        source.setRGB(1, 1, 0x804080c0);
        source.loaded();
        assertNull(cache.get(key, () -> source));
        drain();
        BufferedImage result = cache.get(key, () -> source);
        assertNotSame(source, result);
        assertEquals(0x804080c0, result.getRGB(1, 1));
        source.setRGB(1, 1, Color.RED.getRGB());
        assertEquals(0x804080c0, result.getRGB(1, 1));
        result.setRGB(0, 0, Color.BLUE.getRGB());
        assertEquals(0, source.getRGB(0, 0));
    }

    @Test
    public void oldFilterCompletionCannotReplaceNewIcon() {
        AsyncBufferedImage old = image(4, 4);
        AsyncBufferedImage current = image(4, 4);
        assertNull(cache.get(key, () -> old));
        cache.clear();
        assertNull(cache.get(key, () -> current));
        fill(current, Color.GREEN);
        current.loaded();
        drain();
        BufferedImage result = cache.get(key, () -> current);
        long revision = cache.revision();
        fill(old, Color.RED);
        old.loaded();
        drain();
        assertSame(result, cache.get(key, () -> current));
        assertEquals(Color.GREEN.getRGB(), result.getRGB(1, 1));
        assertEquals(revision, cache.revision());
    }

    @Test
    public void evictedRequestsCannotChangeRevisionOrReplaceEntries() {
        AsyncBufferedImage first = image(2, 2);
        assertNull(cache.get(key, () -> first));
        for (int id = 0; id < 128; id++) {
            AsyncBufferedImage pending = image(2, 2);
            assertNull(cache.get(Map.of("type", "sprite", "id", id), () -> pending));
        }
        long revision = cache.revision();
        first.loaded();
        drain();
        assertEquals(revision, cache.revision());
        assertNull(cache.get(key, () -> image(2, 2)));
    }

    @Test
    public void unavailableImageCanBeRequestedAgainAfterInvalidation() {
        assertNull(cache.get(key, () -> null));
        cache.clear();
        AsyncBufferedImage source = image(2, 2);
        source.loaded();
        assertNull(cache.get(key, () -> source));
        drain();
        assertEquals(2, cache.get(key, () -> source).getWidth());
    }

    private AsyncBufferedImage image(int width, int height) {
        ClientThread thread = mock(ClientThread.class);
        doAnswer(call -> {
            tasks.add(call.getArgument(0));
            return null;
        }).when(thread).invokeLater(any(Runnable.class));
        return new AsyncBufferedImage(thread, width, height, BufferedImage.TYPE_INT_ARGB);
    }

    private void drain() {
        while (!tasks.isEmpty()) {
            tasks.remove(0).run();
        }
    }

    private void fill(BufferedImage image, Color color) {
        var graphics = image.createGraphics();
        graphics.setColor(color);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.dispose();
    }
}
