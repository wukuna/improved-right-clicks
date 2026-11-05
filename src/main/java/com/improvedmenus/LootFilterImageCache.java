package com.improvedmenus;

import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.runelite.client.util.AsyncBufferedImage;

final class LootFilterImageCache {
    private static final int LIMIT = 128;
    private final Map<Map<String, Object>, Entry> entries = new LinkedHashMap<>(16, .75f, true);
    private final Consumer<Runnable> clientThread;
    private long generation;
    private long revision;

    private static final class Entry {
        BufferedImage image;
        boolean complete;
        long requested = System.nanoTime();
    }

    LootFilterImageCache(Consumer<Runnable> clientThread) {
        this.clientThread = clientThread;
    }

    long revision() {
        return revision;
    }

    void clear() {
        generation++;
        revision++;
        entries.clear();
    }

    BufferedImage get(Map<String, Object> key, Supplier<?> provider) {
        Entry existing = entries.get(key);
        if (existing != null && (existing.image != null
                || System.nanoTime() - existing.requested < (existing.complete ? 5_000_000_000L : 10_000_000_000L))) {
            return existing.image;
        }
        Entry entry = new Entry();
        entries.put(key, entry);
        if (entries.size() > LIMIT) {
            entries.remove(entries.keySet().iterator().next());
        }
        long version = generation;
        try {
            Object supplied = provider.get();
            if (supplied instanceof AsyncBufferedImage) {
                AsyncBufferedImage image = (AsyncBufferedImage) supplied;
                image.onLoaded(() -> clientThread.accept(() -> complete(key, entry, version, image)));
            } else if (supplied instanceof CompletableFuture) {
                ((CompletableFuture<?>) supplied).whenComplete((image, error) -> clientThread.accept(() ->
                        complete(key, entry, version, error == null && image instanceof BufferedImage
                                ? (BufferedImage) image : null)));
            } else {
                entry.complete = true;
            }
        } catch (RuntimeException error) {
            entry.complete = true;
        }
        return entry.image;
    }

    private void complete(Map<String, Object> key, Entry entry, long version, BufferedImage loaded) {
        if (version != generation || entries.get(key) != entry) {
            return;
        }
        entry.complete = true;
        if (loaded != null && loaded.getWidth() <= 1024 && loaded.getHeight() <= 1024) {
            if (loaded.getWidth() > 17 || loaded.getHeight() > 15) {
                entry.image = new BufferedImage(17, 15, BufferedImage.TYPE_INT_ARGB);
                var graphics = entry.image.createGraphics();
                try {
                    graphics.drawImage(loaded, 0, 0, 17, 15, null);
                } finally {
                    graphics.dispose();
                }
            } else {
                entry.image = new BufferedImage(loaded.getColorModel(), loaded.copyData(null),
                        loaded.isAlphaPremultiplied(), null);
            }
        }
        revision++;
    }
}
