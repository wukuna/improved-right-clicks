package com.improvedmenus;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.ConfigProfile;

/** Bounded, profile-scoped observations using RuneLite configuration, never direct file access. */
@Singleton
class PlayerBadgeCache
{
    static final String KEY = "playerBadgeCacheV1";
    static final long HOUR = 3_600_000L;
    private final ConfigManager config;
    private final Gson gson;
    private final Map<String, Entry> entries = new LinkedHashMap<>();
    private long profileId;
    private boolean loaded;

    @Inject PlayerBadgeCache(ConfigManager config, Gson gson)
    { this.config = config; this.gson = gson; }

    Map<String, Entry> load(long now)
    {
        entries.clear();
        ConfigProfile profile = config.getProfile();
        loaded = profile != null;
        if (!loaded) return new LinkedHashMap<>();
        profileId = profile.getId();
        String json = config.getConfiguration(ImprovedMenusConfig.GROUP, KEY);
        if (json == null || json.length() > 100_000) return new LinkedHashMap<>();
        try
        {
            Saved saved = gson.fromJson(json, Saved.class);
            if (saved == null || saved.version != 1 || saved.entries == null) return new LinkedHashMap<>();
            for (Entry entry : saved.entries)
            {
                if (entry == null || entry.name == null || !entry.name.equals(PlayerAccountBadges.normalized(entry.name))
                    || entry.name.isEmpty() || !validIcon(entry.icon, entry.hiscores)
                    || entry.seenAt <= 0 || entry.seenAt > now || entry.expires() <= now) continue;
                entries.remove(entry.name);
                entries.put(entry.name, entry);
                trim();
            }
        }
        catch (RuntimeException ex) { entries.clear(); }
        return new LinkedHashMap<>(entries);
    }

    void save(String name, PlayerAccountBadges.Badge badge, long now)
    {
        if (!currentProfile() || badge == null || name == null || name.isEmpty() || !name.equals(PlayerAccountBadges.normalized(name))
            || !validIcon(badge.icon, badge.hiscores) || now <= 0) return;
        entries.values().removeIf(entry -> entry.expires() <= now);
        Entry previous = entries.get(name);
        // Busy chat should not rewrite the saved profile for every message.
        if (previous != null && previous.icon == badge.icon && previous.hiscores == badge.hiscores
            && now - previous.seenAt < 5 * 60_000L) return;
        entries.remove(name);
        entries.put(name, new Entry(name, badge.icon, badge.hiscores, now));
        trim();
        Saved saved = new Saved(); saved.entries = new ArrayList<>(entries.values());
        config.setConfiguration(ImprovedMenusConfig.GROUP, KEY, gson.toJson(saved));
    }

    boolean currentProfile()
    {
        ConfigProfile profile = config.getProfile();
        return loaded && profile != null && profile.getId() == profileId;
    }

    private void trim()
    { while (entries.size() > 256) entries.remove(entries.keySet().iterator().next()); }

    private static boolean validIcon(int icon, boolean hiscores)
    { return icon == 2 || icon == 3 || icon == 10 || !hiscores && (icon == 41 || icon == 42 || icon == 43); }

    static long lifetime(PlayerAccountBadges.Badge badge)
    {
        boolean hardcore = badge.icon == 10 || badge.icon == 42;
        return badge.hiscores ? (hardcore ? HOUR : 24 * HOUR) : (hardcore ? 6 * HOUR : 7 * 24 * HOUR);
    }

    static final class Entry
    {
        String name;
        int icon;
        boolean hiscores;
        long seenAt;
        Entry(String name, int icon, boolean hiscores, long seenAt)
        { this.name = name; this.icon = icon; this.hiscores = hiscores; this.seenAt = seenAt; }
        PlayerAccountBadges.Badge badge() { return new PlayerAccountBadges.Badge(icon, hiscores); }
        long expires() { return seenAt + lifetime(badge()); }
    }

    private static final class Saved
    {
        int version = 1;
        List<Entry> entries;
    }
}
