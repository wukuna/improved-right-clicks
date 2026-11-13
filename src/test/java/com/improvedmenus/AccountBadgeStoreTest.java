package com.improvedmenus;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import net.runelite.api.ChatMessageType;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.ClientShutdown;
import net.runelite.client.util.Filepath;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AccountBadgeStoreTest
{
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    private Filepath root() { return Filepath.Unchecked.getRooted(temporary.getRoot().toPath()); }

    private AccountBadgeStore store(ObservedAccountBadges cache)
    {
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        doReturn(mock(ScheduledFuture.class)).when(executor)
            .scheduleWithFixedDelay(any(Runnable.class), eq(0L), eq(5L), eq(TimeUnit.SECONDS));
        AccountBadgeStore store = new AccountBadgeStore(new Gson(), executor);
        store.start(cache, this::root);
        return store;
    }

    private void observe(ObservedAccountBadges cache, String name) { cache.observe(ChatMessageType.PUBLICCHAT, name); }
    private String file() throws IOException
    {
        return Files.readString(temporary.getRoot().toPath().resolve(AccountBadgeStore.FILE_NAME));
    }

    @Test public void allBadgesSurviveNewPluginAndClientInstances() throws Exception
    {
        ObservedAccountBadges first = new ObservedAccountBadges();
        int i = 0;
        for (ObservedAccountBadges.Badge badge : ObservedAccountBadges.Badge.values()) observe(first, badge.icon + "Player" + i++);
        AccountBadgeStore writer = store(first);
        assertTrue(writer.synchronize());
        assertTrue(first.pending().isEmpty());
        ObservedAccountBadges fresh = new ObservedAccountBadges();
        assertTrue(store(fresh).synchronize());
        i = 0;
        for (ObservedAccountBadges.Badge badge : ObservedAccountBadges.Badge.values()) assertSame(badge, fresh.find("PLAYER" + i++));
        assertTrue(file().contains("\"version\":1"));
        assertFalse(file().contains("<img="));
    }

    @Test public void simultaneousClientsMergeNamesAndKeepLatestCorrections() throws Exception
    {
        ObservedAccountBadges a = new ObservedAccountBadges(), b = new ObservedAccountBadges();
        AccountBadgeStore first = store(a), second = store(b);
        first.synchronize(); second.synchronize();
        observe(a, "<img=10>Alice"); first.synchronize();
        observe(b, "<img=3>Bob"); second.synchronize(); first.synchronize();
        assertSame(ObservedAccountBadges.Badge.ULTIMATE_IRONMAN, a.find("Bob"));
        assertSame(ObservedAccountBadges.Badge.HARDCORE_IRONMAN, b.find("Alice"));
        observe(b, "<img=2>Alice"); second.synchronize();
        observe(a, "<img=41>Charlie"); first.synchronize();
        assertSame(ObservedAccountBadges.Badge.IRONMAN, a.find("Alice"));
        observe(b, "Alice"); second.synchronize();
        observe(a, "<img=42>Dan"); first.synchronize();
        ObservedAccountBadges fresh = new ObservedAccountBadges(); store(fresh).synchronize();
        assertNull(fresh.find("Alice"));
        assertSame(ObservedAccountBadges.Badge.ULTIMATE_IRONMAN, fresh.find("Bob"));
        assertSame(ObservedAccountBadges.Badge.GROUP_IRONMAN, fresh.find("Charlie"));
        assertSame(ObservedAccountBadges.Badge.HARDCORE_GROUP_IRONMAN, fresh.find("Dan"));
        assertTrue(file().contains("NONE"));
    }

    @Test public void chatBeforeInitialLoadWinsOverOlderDiskStatus() throws Exception
    {
        root().join(AccountBadgeStore.FILE_NAME).write("{\"version\":1,\"accounts\":{\"alice\":{\"badge\":\"HARDCORE_IRONMAN\",\"seen\":100}}}");
        ObservedAccountBadges cache = new ObservedAccountBadges();
        observe(cache, "Alice");
        assertTrue(store(cache).synchronize());
        assertNull(cache.find("Alice"));
        ObservedAccountBadges fresh = new ObservedAccountBadges(); store(fresh).synchronize();
        assertNull(fresh.find("Alice"));
    }

    @Test public void observationsMadeDuringWriteRemainPending()
    {
        ObservedAccountBadges cache = new ObservedAccountBadges();
        observe(cache, "<img=10>Alice");
        Map<String, ObservedAccountBadges.Observation> writing = cache.pending();
        observe(cache, "<img=2>Alice");
        cache.saved(writing, writing);
        assertSame(ObservedAccountBadges.Badge.IRONMAN, cache.find("Alice"));
        assertSame(ObservedAccountBadges.Badge.IRONMAN, cache.pending().get("alice").badge);
    }

    @Test public void corruptOrFutureFilesArePreservedAndMemoryCacheStillWorks() throws Exception
    {
        for (String contents : new String[]{"{bad json", "{\"version\":2,\"accounts\":{}}", "null"})
        {
            root().join(AccountBadgeStore.FILE_NAME).write(contents);
            ObservedAccountBadges cache = new ObservedAccountBadges(); observe(cache, "<img=2>Alice");
            assertFalse(store(cache).synchronize());
            assertSame(ObservedAccountBadges.Badge.IRONMAN, cache.find("Alice"));
            assertFalse(cache.pending().isEmpty());
            assertEquals(contents, file());
        }
    }

    @Test public void invalidEntriesCannotInjectMenuMarkupOrUnsupportedBadges() throws Exception
    {
        root().join(AccountBadgeStore.FILE_NAME).write("{\"version\":1,\"accounts\":{"
            + "\"<img=1>alice\":{\"badge\":\"IRONMAN\",\"seen\":100},"
            + "\"bob\":{\"badge\":\"ARBITRARY\",\"seen\":100},"
            + "\"charlie\":{\"badge\":\"IRONMAN\",\"seen\":-1},"
            + "\"dan\":{\"badge\":\"IRONMAN\",\"seen\":1.5},"
            + "\"eve\":{\"badge\":\"ULTIMATE_IRONMAN\",\"seen\":100}}}");
        ObservedAccountBadges cache = new ObservedAccountBadges(); assertTrue(store(cache).synchronize());
        for (String name : new String[]{"Alice", "Bob", "Charlie", "Dan"}) assertNull(cache.find(name));
        assertSame(ObservedAccountBadges.Badge.ULTIMATE_IRONMAN, cache.find("Eve"));
    }

    @Test public void oversizedFileIsRejectedWithoutOverwritingIt() throws Exception
    {
        byte[] bytes = new byte[AccountBadgeStore.MAX_BYTES + 1];
        root().join(AccountBadgeStore.FILE_NAME).write(bytes);
        ObservedAccountBadges cache = new ObservedAccountBadges(); observe(cache, "<img=2>Alice");
        assertFalse(store(cache).synchronize());
        assertEquals(bytes.length, root().join(AccountBadgeStore.FILE_NAME).size());
        assertFalse(cache.pending().isEmpty());
    }

    @Test public void concurrentFileLockDefersWriteWithoutLosingPendingObservations() throws Exception
    {
        ObservedAccountBadges cache = new ObservedAccountBadges(); observe(cache, "<img=2>Alice");
        AccountBadgeStore store = store(cache);
        try (FileChannel channel = root().join("account-badges.lock").openFileChannel(StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            FileLock lock = channel.lock())
        {
            assertFalse(store.synchronize());
            assertFalse(cache.pending().isEmpty());
            assertFalse(root().join(AccountBadgeStore.FILE_NAME).exists());
        }
        assertTrue(store.synchronize());
        assertTrue(cache.pending().isEmpty());
    }

    @Test public void failedReplacementPreservesOldFileAndRetriesPendingUpdate() throws Exception
    {
        ObservedAccountBadges cache = new ObservedAccountBadges(); observe(cache, "<img=2>Alice");
        store(cache).synchronize();
        String original = file();
        observe(cache, "<img=10>Alice");
        Filepath directory = spy(root());
        Filepath broken = spy(root().createTempFile("test-", ".tmp"));
        doReturn(broken).when(directory).createTempFile("account-badges-", ".tmp");
        doThrow(new IOException("simulated replacement failure")).when(broken)
            .moveTo(any(Filepath.class), eq(StandardCopyOption.ATOMIC_MOVE), eq(StandardCopyOption.REPLACE_EXISTING));
        AccountBadgeStore failing = new AccountBadgeStore(new Gson(), mock(ScheduledExecutorService.class));
        failing.start(cache, () -> directory);
        assertFalse(failing.synchronize());
        assertEquals(original, file());
        assertFalse(cache.pending().isEmpty());
        assertFalse(broken.exists());
        assertTrue(store(cache).synchronize());
        assertTrue(file().contains("HARDCORE_IRONMAN"));
    }

    @Test public void shutdownEventWaitsForTheBackgroundSave() throws Exception
    {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        try
        {
            ObservedAccountBadges cache = new ObservedAccountBadges();
            AccountBadgeStore store = new AccountBadgeStore(new Gson(), executor);
            store.start(cache, this::root);
            observe(cache, "<img=10>Alice");
            ImprovedMenusPlugin plugin = new ImprovedMenusPlugin();
            java.lang.reflect.Field field = ImprovedMenusPlugin.class.getDeclaredField("accountBadgeStore");
            field.setAccessible(true); field.set(plugin, store);
            EventBus bus = new EventBus(error -> { throw new AssertionError(error); }); bus.register(plugin);
            ClientShutdown event = new ClientShutdown(); bus.post(event);
            assertEquals(1, event.getTasks().size());
            event.getTasks().peek().get(5, TimeUnit.SECONDS);
            assertTrue(file().contains("HARDCORE_IRONMAN"));
            store.stop().get(5, TimeUnit.SECONDS);
            bus.unregister(plugin);
        }
        finally { executor.shutdown(); assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS)); }
    }

    @Test public void cacheMergesKeepOnlyMostRecentTenThousandPlayers()
    {
        Map<String, ObservedAccountBadges.Observation> first = new LinkedHashMap<>(), second = new LinkedHashMap<>();
        for (int i=0; i<ObservedAccountBadges.CAPACITY; i++) first.put("player"+i, new ObservedAccountBadges.Observation(ObservedAccountBadges.Badge.IRONMAN, i+1));
        second.put("newest", new ObservedAccountBadges.Observation(ObservedAccountBadges.Badge.IRONMAN, 20000));
        ObservedAccountBadges.merge(first, second);
        assertEquals(ObservedAccountBadges.CAPACITY, first.size());
        assertFalse(first.containsKey("player0")); assertTrue(first.containsKey("newest"));
    }
}
