package com.improvedmenus;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.Test;
import static org.junit.Assert.*;

public class LootFilterImageProviderTest {
    @Test public void largeProviderImagesAreRetainedOnlyAtMenuSize() {
        var cache=new LootFilterImageCache(Runnable::run);
        BufferedImage original=new BufferedImage(512,512,BufferedImage.TYPE_INT_ARGB);
        var graphics=original.createGraphics();graphics.setColor(Color.MAGENTA);graphics.fillRect(0,0,512,512);graphics.dispose();
        BufferedImage image=cache.get(Map.of("type","file","filename","large.png"),()->CompletableFuture.completedFuture(original));
        assertEquals(17,image.getWidth());assertEquals(15,image.getHeight());assertEquals(Color.MAGENTA.getRGB(),image.getRGB(8,7));
        assertEquals(512,original.getWidth());assertNotSame(original,image);
    }
    @Test public void lateResultsCannotRepopulateCacheAfterReload() {
        List<Runnable> client=new ArrayList<>();var cache=new LootFilterImageCache(client::add);
        Map<String,Object> key=Map.of("type","file","filename","heart.png");
        CompletableFuture<BufferedImage> old=new CompletableFuture<>(),fresh=new CompletableFuture<>();
        assertNull(cache.get(key,()->old));cache.clear();assertNull(cache.get(key,()->fresh));
        BufferedImage oldImage=new BufferedImage(2,2,2),newImage=new BufferedImage(2,2,2);
        old.complete(oldImage);fresh.complete(newImage);
        assertEquals(2,client.size());client.forEach(Runnable::run);
        assertSame(newImage,cache.get(key,()->{throw new AssertionError("Should reuse completed image");}));
    }
    @Test public void pendingRequestsAreDeduplicatedAndFailureDoesNotEscape() {
        var cache=new LootFilterImageCache(Runnable::run);AtomicInteger calls=new AtomicInteger();
        var key=Map.<String,Object>of("type","sprite","id",123,"index",0);
        var pending=new CompletableFuture<BufferedImage>();Supplier<?> provider=()->{calls.incrementAndGet();return pending;};
        for(int i=0;i<20;i++) assertNull(cache.get(key,provider));assertEquals(1,calls.get());
        pending.completeExceptionally(new IllegalArgumentException());assertNull(cache.get(key,provider));
        cache.clear();assertNull(cache.get(key,()->{throw new IllegalStateException();}));
    }
    @Test public void visibilityAndDescriptorDoNotInvokeProviderDuringDecode() {
        AtomicInteger calls=new AtomicInteger();Supplier<?> provider=()->{calls.incrementAndGet();return CompletableFuture.completedFuture(new BufferedImage(2,2,2));};
        Map<String,Object> display=new HashMap<>();display.put("hidden",false);display.put("hideOverlay",false);
        display.put("menuSort",-1);display.put("menuTextColor",Color.CYAN);
        display.put("icon",Map.of("type","current-item","id",385,"quantity",1,"height",16,"getImage",provider));
        Map<String,Object> data=Map.of("status","ok","display",display);
        var visible=LootFiltersCompatibility.decode(data,385);assertTrue(visible.itemIcon);assertTrue(visible.hasIcon());
        assertEquals(Color.CYAN,visible.menuColor);assertEquals(-1,visible.menuSort);
        display.put("hideOverlay",true);assertFalse(LootFiltersCompatibility.decode(data,385).hasIcon());
        display.put("hideOverlay",false);display.put("hidden",true);assertFalse(LootFiltersCompatibility.decode(data,385).hasIcon());
        assertEquals(0,calls.get());
        assertNull(LootFiltersCompatibility.decode(Map.of("status","ambiguous"),385));
    }
}
