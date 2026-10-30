package com.improvedmenus;

import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.ConfigProfile;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PlayerBadgeCacheTest
{
    private final ConfigManager config = mock(ConfigManager.class);
    private final Gson gson = new Gson();
    private final Map<Long, String> profiles = new HashMap<>();
    private final AtomicReference<ConfigProfile> profile = new AtomicReference<>();
    private PlayerBadgeCache cache;
    private static final long NOW = 1_800_000_000_000L;

    @Before public void setup()
    {
        switchProfile(1);
        when(config.getProfile()).thenAnswer(i -> profile.get());
        when(config.getConfiguration(ImprovedMenusConfig.GROUP, PlayerBadgeCache.KEY))
            .thenAnswer(i -> profiles.get(profile.get().getId()));
        doAnswer(i -> { profiles.put(profile.get().getId(), i.getArgument(2)); return null; })
            .when(config).setConfiguration(eq(ImprovedMenusConfig.GROUP), eq(PlayerBadgeCache.KEY), anyString());
        cache = new PlayerBadgeCache(config, gson); cache.load(NOW);
    }

    private void switchProfile(long id)
    {
        ConfigProfile next = mock(ConfigProfile.class); when(next.getId()).thenReturn(id); profile.set(next);
    }
    private PlayerAccountBadges.Badge badge(int icon, boolean inferred)
    { return new PlayerAccountBadges.Badge(icon, inferred); }

    @Test public void survivesNewInstanceWithEvidenceSourceAndExpiry()
    {
        cache.save("alice", badge(2, true), NOW); cache.save("bob", badge(42, false), NOW);
        PlayerBadgeCache restarted = new PlayerBadgeCache(config, gson);
        Map<String, PlayerBadgeCache.Entry> restored = restarted.load(NOW + 1000);
        assertEquals(2, restored.size()); assertTrue(restored.get("alice").badge().hiscores);
        assertEquals(42, restored.get("bob").badge().icon); assertFalse(restored.get("bob").badge().hiscores);
        assertEquals(NOW + 24 * PlayerBadgeCache.HOUR, restored.get("alice").expires());
    }

    @Test public void profileSwitchRejectsStaleWritesAndKeepsCachesSeparate()
    {
        cache.save("alice", badge(3, false), NOW); switchProfile(2);
        assertFalse(cache.currentProfile()); cache.save("bob", badge(2, true), NOW);
        assertFalse(profiles.containsKey(2L)); assertTrue(cache.load(NOW).isEmpty());
        cache.save("carol", badge(10, true), NOW); switchProfile(1);
        Map<String, PlayerBadgeCache.Entry> restored = cache.load(NOW);
        assertEquals(1, restored.size()); assertTrue(restored.containsKey("alice"));
        switchProfile(2); assertTrue(cache.load(NOW).containsKey("carol"));
    }

    @Test public void hardcoreExpiresSoonerAndExpiredEntriesAreNotRestored()
    {
        cache.save("a", badge(10, true), NOW); cache.save("b", badge(42, false), NOW);
        cache.save("c", badge(2, true), NOW); cache.save("d", badge(3, false), NOW);
        Map<String, PlayerBadgeCache.Entry> restored = cache.load(NOW + PlayerBadgeCache.HOUR);
        assertFalse(restored.containsKey("a")); assertEquals(3, restored.size());
        assertEquals(2, cache.load(NOW + 6 * PlayerBadgeCache.HOUR).size());
        assertEquals(1, cache.load(NOW + 24 * PlayerBadgeCache.HOUR).size());
        assertTrue(cache.load(NOW + 7 * 24 * PlayerBadgeCache.HOUR).isEmpty());
    }

    @Test public void chatUpgradeIsSavedImmediatelyWhileRepeatedChatWritesAreDebounced()
    {
        cache.save("alice", badge(2, true), NOW); cache.save("alice", badge(3, false), NOW + 1);
        cache.save("alice", badge(3, false), NOW + 1000);
        verify(config, times(2)).setConfiguration(eq(ImprovedMenusConfig.GROUP), eq(PlayerBadgeCache.KEY), anyString());
        cache.save("alice", badge(3, false), NOW + 6 * 60_000L);
        verify(config, times(3)).setConfiguration(eq(ImprovedMenusConfig.GROUP), eq(PlayerBadgeCache.KEY), anyString());
        assertFalse(cache.load(NOW + 7 * 60_000L).get("alice").badge().hiscores);
    }

    @Test public void boundedStorageExcludesFailuresInvalidNamesAndInventedGroupEstimates()
    {
        cache.save("alice", null, NOW); cache.save("<img=2>x", badge(2, false), NOW);
        cache.save("alice", badge(41, true), NOW); cache.save("alice", badge(999, false), NOW);
        assertTrue(profiles.isEmpty());
        for (int i = 0; i < 260; i++) cache.save("p" + i, badge(2, false), NOW + i);
        Map<String, PlayerBadgeCache.Entry> restored = cache.load(NOW + 1000);
        assertEquals(256, restored.size()); assertFalse(restored.containsKey("p0")); assertTrue(restored.containsKey("p259"));
    }

    @Test public void corruptUnknownOversizedOrFutureCacheCannotInjectBadges()
    {
        for (String json : new String[]{"not json", "null", "{}", "{\"version\":9,\"entries\":[]}", "x".repeat(100001),
            "{\"version\":1,\"entries\":[null,{\"name\":\"alice\",\"icon\":2,\"seenAt\":" + (NOW + 1) + "}]}",
            "{\"version\":1,\"entries\":[{\"name\":\"<img=2>x\",\"icon\":2,\"seenAt\":" + NOW + "}]}"})
        { profiles.put(1L, json); assertTrue(cache.load(NOW).isEmpty()); }
    }
}
