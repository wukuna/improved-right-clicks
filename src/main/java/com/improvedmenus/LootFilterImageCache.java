package com.improvedmenus;

import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Client-thread-owned cache of independent images supplied asynchronously by Loot Filters. */
final class LootFilterImageCache {
    private static final int LIMIT=128;
    private final Map<Map<String,Object>,Entry> entries=new LinkedHashMap<>(16,.75f,true);
    private final Consumer<Runnable> clientThread;
    private long generation,revision;
    private static final class Entry {
        BufferedImage image;
        boolean complete;
        long requested=System.nanoTime();
    }
    LootFilterImageCache(Consumer<Runnable> clientThread) { this.clientThread=clientThread; }
    long revision() { return revision; }
    void clear() { generation++;revision++;entries.clear(); }
    BufferedImage get(Map<String,Object> key,Supplier<?> provider) {
        Entry existing=entries.get(key);
        if(existing!=null && (existing.image!=null || System.nanoTime()-existing.requested<(existing.complete?5_000_000_000L:10_000_000_000L))) return existing.image;
        Entry entry=new Entry();entries.put(key,entry);
        if(entries.size()>LIMIT) entries.remove(entries.keySet().iterator().next());
        long version=generation;
        try {
            Object supplied=provider.get();
            if(!(supplied instanceof CompletableFuture)) { entry.complete=true;return null; }
            ((CompletableFuture<?>)supplied).whenComplete((image,error)->clientThread.accept(()->{
                if(version!=generation || entries.get(key)!=entry) return;
                entry.complete=true;
                if(error==null && image instanceof BufferedImage) {
                    BufferedImage loaded=(BufferedImage)image;
                    if(loaded.getWidth()<=1024 && loaded.getHeight()<=1024) {
                        // Retain menu-sized pixels, not up to 128 full-size file images.
                        if(loaded.getWidth()>17 || loaded.getHeight()>15) {
                            entry.image=new BufferedImage(17,15,BufferedImage.TYPE_INT_ARGB);
                            var graphics=entry.image.createGraphics();
                            try { graphics.drawImage(loaded,0,0,17,15,null); }
                            finally { graphics.dispose(); }
                        } else entry.image=loaded;
                    }
                }
                revision++;
            }));
        } catch(RuntimeException error) { entry.complete=true; }
        return entry.image;
    }
}
