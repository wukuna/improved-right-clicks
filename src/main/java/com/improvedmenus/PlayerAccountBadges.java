package com.improvedmenus;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.hiscore.HiscoreClient;
import net.runelite.client.hiscore.HiscoreEndpoint;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkill;

/** Client-thread state. Hiscores listings are evidence of a ranking, not current account mode. */
@Singleton
class PlayerAccountBadges
{
    static final Pattern IMAGE = Pattern.compile("<img=(\\d+)>");
    private static final EnumSet<ChatMessageType> PLAYER_CHAT = EnumSet.of(
        ChatMessageType.PUBLICCHAT, ChatMessageType.MODCHAT, ChatMessageType.PRIVATECHAT,
        ChatMessageType.MODPRIVATECHAT, ChatMessageType.FRIENDSCHAT, ChatMessageType.CLAN_CHAT,
        ChatMessageType.CLAN_GUEST_CHAT, ChatMessageType.CLAN_GIM_CHAT);
    private static final long MINUTE = 60_000_000_000L;
    private final HiscoreClient hiscores;
    private final ClientThread clientThread;
    private final ImprovedMenusConfig config;
    private final LongSupplier clock;
    private final Map<String, Cached> cache = new LinkedHashMap<>();
    private int generation;
    private int pending;
    private long nextRequest;
    private boolean active;

    @Inject
    PlayerAccountBadges(HiscoreClient hiscores, ClientThread clientThread, ImprovedMenusConfig config)
    {
        this(hiscores, clientThread, config, System::nanoTime);
    }

    PlayerAccountBadges(HiscoreClient hiscores, ClientThread clientThread, ImprovedMenusConfig config, LongSupplier clock)
    {
        this.hiscores = hiscores;
        this.clientThread = clientThread;
        this.config = config;
        this.clock = clock;
    }

    void reset(boolean enabled)
    {
        generation++;
        active = enabled;
        cache.clear();
        pending = 0;
        // Retain the rate limit across settings/profile changes.
    }

    void observe(ChatMessage event)
    {
        if (!active || !config.playerIronmanBadge() || !PLAYER_CHAT.contains(event.getType())) return;
        String name = normalized(event.getName());
        if (name.isEmpty()) return;
        int icon = accountIcon(event.getName());
        // A server chat name without a helmet supersedes any older observation/ranking.
        put(name, new Cached(icon < 0 ? null : new Badge(icon, false), clock.getAsLong() + 15 * MINUTE));
    }

    Badge find(String displayName)
    {
        if (!active || !config.playerIronmanBadge()) return null;
        String name = normalized(displayName);
        if (name.isEmpty()) return null;
        long now = clock.getAsLong();
        Cached known = cache.get(name);
        if (known != null && known.expires > now) return known.badge;
        cache.remove(name);
        if (!config.playerHiscores() || pending >= 2 || nextRequest != 0 && now < nextRequest) return null;
        nextRequest = now + 3_000_000_000L;
        pending++;
        int requestGeneration = generation;
        Cached request = new Cached(null, now + MINUTE);
        put(name, request);
        CompletableFuture<HiscoreResult> future;
        try
        {
            // The injected core client uses Jagex's fixed official endpoint and asynchronous HTTP.
            future = hiscores.lookupAsync(name, HiscoreEndpoint.IRONMAN);
        }
        catch (RuntimeException ex)
        {
            complete(name, request, requestGeneration, null, ex);
            return null;
        }
        future.orTimeout(10, TimeUnit.SECONDS).whenComplete((result, error) ->
            clientThread.invokeLater(() -> complete(name, request, requestGeneration, result, error)));
        return null;
    }

    private void complete(String name, Cached request, int requestGeneration, HiscoreResult result, Throwable error)
    {
        if (!active || requestGeneration != generation) return;
        pending--;
        if (cache.get(name) != request || !config.playerIronmanBadge() || !config.playerHiscores()) return;
        boolean ranked = error == null && result != null && result.getSkill(HiscoreSkill.OVERALL) != null
            && result.getSkill(HiscoreSkill.OVERALL).getRank() > 0;
        put(name, new Cached(ranked ? new Badge(2, true) : null,
            clock.getAsLong() + (error != null ? 2 : ranked ? 15 : 5) * MINUTE));
    }

    private void put(String name, Cached entry)
    {
        cache.remove(name);
        cache.put(name, entry);
        while (cache.size() > 256) cache.remove(cache.keySet().iterator().next());
    }

    static String normalized(String value)
    {
        if (value == null) return "";
        String name = MenuRules.plain(value).replace('\u00a0', ' ').replace('_', ' ').trim().toLowerCase(Locale.ROOT);
        return name.matches("[a-z0-9 -]{1,12}") ? name : "";
    }

    static int accountIcon(String target)
    {
        if (target == null) return -1;
        Matcher matcher = IMAGE.matcher(target);
        while (matcher.find())
        {
            String value = matcher.group(1);
            if (value.equals("2") || value.equals("3") || value.equals("10")
                || value.equals("41") || value.equals("42") || value.equals("43")) return Integer.parseInt(value);
        }
        return -1;
    }

    static final class Badge
    {
        final int icon;
        final boolean hiscores;
        Badge(int icon, boolean hiscores) { this.icon = icon; this.hiscores = hiscores; }
        String markup() { return "<img=" + icon + ">" + (hiscores ? "<col=aaaaaa>?</col>" : ""); }
        String description()
        {
            if (hiscores) return "Ironman Hiscores listing (?) - current mode unverified";
            String mode;
            switch (icon)
            {
                case 3: mode = "Ultimate Ironman"; break;
                case 10: mode = "Hardcore Ironman"; break;
                case 41: mode = "Group Ironman"; break;
                case 42: mode = "Hardcore Group Ironman"; break;
                case 43: mode = "Unranked Group Ironman"; break;
                default: mode = "Ironman";
            }
            return mode + " - observed in this session's chat";
        }
    }

    private static final class Cached
    {
        final Badge badge;
        final long expires;
        Cached(Badge badge, long expires) { this.badge = badge; this.expires = expires; }
    }
}
