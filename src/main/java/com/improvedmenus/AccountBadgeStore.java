package com.improvedmenus;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.Filepath;

@Slf4j
@Singleton
final class AccountBadgeStore
{
    static final String FILE_NAME = "account-badges.json";
    static final int MAX_BYTES = 2 * 1024 * 1024;
    private final Gson gson;
    private final ScheduledExecutorService executor;
    private final Object ioLock = new Object();
    private volatile ObservedAccountBadges cache;
    private volatile Directory directory;
    private Filepath root;
    private ScheduledFuture<?> task;
    private boolean warned;

    interface Directory { Filepath get() throws IOException; }

    @Inject AccountBadgeStore(Gson gson, ScheduledExecutorService executor)
    {
        this.gson = gson;
        this.executor = executor;
    }

    synchronized void start(ObservedAccountBadges cache, Directory directory)
    {
        if (task != null) return;
        this.cache = cache;
        this.directory = directory;
        task = executor.scheduleWithFixedDelay(this::synchronize, 0, 5, TimeUnit.SECONDS);
    }

    synchronized CompletableFuture<Void> stop()
    {
        if (task != null) task.cancel(false);
        task = null;
        return flush();
    }

    CompletableFuture<Void> flush()
    {
        CompletableFuture<Void> result = new CompletableFuture<>();
        executor.execute(() -> finishFlush(result, 10));
        return result;
    }

    private void finishFlush(CompletableFuture<Void> result, int attempts)
    {
        if (synchronize()) result.complete(null);
        else if (attempts > 1) executor.schedule(() -> finishFlush(result, attempts - 1), 100, TimeUnit.MILLISECONDS);
        else result.completeExceptionally(new IOException("Could not save the account badge cache"));
    }

    boolean synchronize()
    {
        synchronized (ioLock)
        {
            if (cache == null || directory == null) return true;
            try
            {
                if (root == null) root = directory.get();
                root.createDirectories();
                try (FileChannel channel = root.join("account-badges.lock").openFileChannel(
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                    FileLock lock = channel.tryLock())
                {
                    if (lock == null) return false;
                    Filepath file = root.join(FILE_NAME);
                    Map<String, ObservedAccountBadges.Observation> saved = read(file);
                    Map<String, ObservedAccountBadges.Observation> pending = cache.pending();
                    ObservedAccountBadges.merge(saved, pending);
                    if (!pending.isEmpty()) write(file, saved);
                    cache.saved(saved, pending);
                    warned = false;
                    return true;
                }
            }
            catch (OverlappingFileLockException ignored) { }
            catch (IOException | RuntimeException ex)
            {
                if (!warned) log.warn("Unable to synchronize the local account badge cache", ex);
                warned = true;
            }
            return false;
        }
    }

    private Map<String, ObservedAccountBadges.Observation> read(Filepath file) throws IOException
    {
        Map<String, ObservedAccountBadges.Observation> result = new LinkedHashMap<>();
        if (!file.exists()) return result;
        if (file.size() > MAX_BYTES) throw new IOException("Account badge cache is too large");
        StringBuilder text = new StringBuilder();
        try (Reader reader = file.openReader())
        {
            char[] buffer = new char[4096];
            int count;
            while ((count = reader.read(buffer)) != -1)
            {
                if (text.length() + count > MAX_BYTES) throw new IOException("Account badge cache is too large");
                text.append(buffer, 0, count);
            }
        }
        JsonObject document = gson.fromJson(text.toString(), JsonObject.class);
        if (document == null || !document.has("version") || !document.get("version").isJsonPrimitive()
            || !document.getAsJsonPrimitive("version").isNumber()
            || document.get("version").getAsBigDecimal().compareTo(java.math.BigDecimal.ONE) != 0
            || !document.has("accounts") || !document.get("accounts").isJsonObject())
            throw new IOException("Unsupported account badge cache format");
        JsonObject accounts = document.getAsJsonObject("accounts");
        if (accounts.size() > ObservedAccountBadges.CAPACITY) throw new IOException("Too many cached account badges");
        for (Map.Entry<String, JsonElement> row : accounts.entrySet())
        {
            if (!row.getKey().equals(ObservedAccountBadges.key(row.getKey())) || row.getKey().isEmpty()) continue;
            if (!row.getValue().isJsonObject()) continue;
            JsonObject value = row.getValue().getAsJsonObject();
            if (!value.has("badge") || !value.get("badge").isJsonPrimitive()
                || !value.getAsJsonPrimitive("badge").isString()
                || !value.has("seen") || !value.get("seen").isJsonPrimitive()
                || !value.getAsJsonPrimitive("seen").isNumber()) continue;
            try
            {
                String kind = value.get("badge").getAsString();
                ObservedAccountBadges.Badge badge = "NONE".equals(kind) ? null : ObservedAccountBadges.Badge.valueOf(kind);
                long seen = value.get("seen").getAsBigDecimal().longValueExact();
                if (seen <= 0 || seen > System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)) continue;
                result.put(row.getKey(), new ObservedAccountBadges.Observation(badge, seen));
            }
            catch (IllegalArgumentException | ArithmeticException ignored) { }
        }
        return result;
    }

    private void write(Filepath file, Map<String, ObservedAccountBadges.Observation> records) throws IOException
    {
        JsonObject accounts = new JsonObject();
        records.forEach((name, entry) -> {
            JsonObject value = new JsonObject();
            value.addProperty("badge", entry.badge == null ? "NONE" : entry.badge.name());
            value.addProperty("seen", entry.seen);
            accounts.add(name, value);
        });
        JsonObject document = new JsonObject();
        document.addProperty("version", 1);
        document.add("accounts", accounts);
        Filepath temporary = root.createTempFile("account-badges-", ".tmp");
        try
        {
            try (Writer writer = temporary.openWriter()) { gson.toJson(document, writer); }
            try (FileChannel channel = temporary.openFileChannel(StandardOpenOption.WRITE)) { channel.force(true); }
            try { temporary.moveTo(file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ex) { temporary.moveTo(file, StandardCopyOption.REPLACE_EXISTING); }
        }
        finally { temporary.deleteIfExists(); }
    }
}
