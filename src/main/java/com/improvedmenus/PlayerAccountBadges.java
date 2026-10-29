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

/** Client-thread state. XP comparisons estimate account mode; native chat takes precedence. */
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
        // Up to four sequential requests per name, with no queue of nearby players.
        nextRequest = now + 12_000_000_000L;
        pending++;
        Lookup request = new Lookup(name, generation, new Cached(null, now + MINUTE));
        put(name, request.entry);
        fetch(request, HiscoreEndpoint.IRONMAN);
        return null;
    }

    private boolean current(Lookup request)
    {
        return active && request.generation == generation && cache.get(request.name) == request.entry
            && config.playerIronmanBadge() && config.playerHiscores();
    }

    private void fetch(Lookup request, HiscoreEndpoint endpoint)
    {
        if (!current(request)) { finish(request, null, 2); return; }
        CompletableFuture<HiscoreResult> future;
        try
        {
            // The injected core client uses Jagex's fixed official endpoint and asynchronous HTTP.
            future = hiscores.lookupAsync(request.name, endpoint);
        }
        catch (RuntimeException ex)
        {
            finish(request, null, 2);
            return;
        }
        future.orTimeout(10, TimeUnit.SECONDS).whenComplete((result, error) ->
            clientThread.invokeLater(() -> complete(request, endpoint, result, error)));
    }

    private void complete(Lookup request, HiscoreEndpoint endpoint, HiscoreResult result, Throwable error)
    {
        if (!current(request)) { finish(request, null, 2); return; }
        // Only a 404 (null result from HiscoreClient) means absent. Network errors or
        // malformed responses must not downgrade a Hardcore/Ultimate account to Ironman.
        if (error != null || result != null && (result.getSkill(HiscoreSkill.OVERALL) == null
            || result.getSkill(HiscoreSkill.OVERALL).getRank() <= 0
            || result.getSkill(HiscoreSkill.OVERALL).getExperience() < 0))
        { finish(request, null, 2); return; }
        long xp = result == null ? -1 : result.getSkill(HiscoreSkill.OVERALL).getExperience();
        switch (endpoint)
        {
            case IRONMAN:
                if (xp < 0) { finish(request, null, 5); return; }
                request.ironXp = xp;
                fetch(request, HiscoreEndpoint.NORMAL);
                break;
            case NORMAL:
                // An Ironman listing can survive de-ironing. Inconsistent snapshots
                // also stay blank until a later lookup. Low-level players may be absent here.
                if (xp >= 0 && xp != request.ironXp)
                { finish(request, null, xp > request.ironXp ? 5 : 2); return; }
                fetch(request, HiscoreEndpoint.HARDCORE_IRONMAN);
                break;
            case HARDCORE_IRONMAN:
                if (xp > request.ironXp) { finish(request, null, 2); return; }
                request.hardcoreXp = xp;
                fetch(request, HiscoreEndpoint.ULTIMATE_IRONMAN);
                break;
            case ULTIMATE_IRONMAN:
                boolean hardcore = request.hardcoreXp == request.ironXp;
                boolean ultimate = xp == request.ironXp;
                if (xp > request.ironXp || hardcore && ultimate)
                { finish(request, null, 2); return; }
                // A frozen ranking with equal XP cannot prove current status. Every
                // inferred helmet carries the estimate tooltip; observed chat replaces it.
                finish(request, new Badge(hardcore ? 10 : ultimate ? 3 : 2, true), 15);
                break;
            default:
                finish(request, null, 2);
        }
    }

    private void finish(Lookup request, Badge badge, int minutes)
    {
        if (!active || request.generation != generation) return;
        pending--;
        if (current(request)) put(request.name, new Cached(badge, clock.getAsLong() + minutes * MINUTE));
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
        String markup() { return "<img=" + icon + ">"; }
        String description()
        {
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
            return mode + (hiscores ? " - Hiscores estimate" : " - observed in this session's chat");
        }
    }

    private static final class Lookup
    {
        final String name;
        final int generation;
        final Cached entry;
        long ironXp;
        long hardcoreXp;
        Lookup(String name, int generation, Cached entry)
        { this.name = name; this.generation = generation; this.entry = entry; }
    }

    private static final class Cached
    {
        final Badge badge;
        final long expires;
        Cached(Badge badge, long expires) { this.badge = badge; this.expires = expires; }
    }
}
