package com.improvedmenus;

import java.util.EnumSet;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.WorldType;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.hiscore.HiscoreClient;
import net.runelite.client.hiscore.HiscoreEndpoint;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkill;

@Singleton
class PlayerAccountBadges
{
    static final Pattern IMAGE = Pattern.compile("<img=(\\d+)>");
    private static final EnumSet<ChatMessageType> PLAYER_CHAT = EnumSet.of(
        ChatMessageType.PUBLICCHAT, ChatMessageType.MODCHAT, ChatMessageType.PRIVATECHAT,
        ChatMessageType.MODPRIVATECHAT, ChatMessageType.FRIENDSCHAT, ChatMessageType.CLAN_CHAT,
        ChatMessageType.CLAN_GUEST_CHAT, ChatMessageType.CLAN_GIM_CHAT);
    private static final long MINUTE = 60_000L;
    private final HiscoreClient hiscores;
    private final ClientThread clientThread;
    private final ImprovedMenusConfig config;
    private final LongSupplier clock;
    private final PlayerBadgeCache saved;
    private final BooleanSupplier permitted;
    private final Map<String, Cached> cache = new LinkedHashMap<>();
    private final LinkedHashSet<String> nearbyQueue = new LinkedHashSet<>();
    private int nearbyCursor;
    private int failures;
    private int generation;
    private int pending;
    private long nextRequest;
    private boolean active;
    private String waitingName;
    private boolean waitingExplicit;
    private long waitingUntil;
    private long revision;

    @Inject
    PlayerAccountBadges(HiscoreClient hiscores, ClientThread clientThread, ImprovedMenusConfig config, PlayerBadgeCache saved, Client client)
    {
        this(hiscores, clientThread, config, System::currentTimeMillis, saved, () -> permitted(client));
    }

    PlayerAccountBadges(HiscoreClient hiscores, ClientThread clientThread, ImprovedMenusConfig config, LongSupplier clock, PlayerBadgeCache saved)
    { this(hiscores, clientThread, config, clock, saved, () -> true); }

    PlayerAccountBadges(HiscoreClient hiscores, ClientThread clientThread, ImprovedMenusConfig config, LongSupplier clock,
        PlayerBadgeCache saved, BooleanSupplier permitted)
    {
        this.hiscores = hiscores;
        this.clientThread = clientThread;
        this.config = config;
        this.clock = clock;
        this.saved = saved;
        this.permitted = permitted;
    }

    static boolean permitted(Client client)
    {

        if (client.getGameState() != GameState.LOGGED_IN || client.getVarbitValue(VarbitID.PVP_AREA_CLIENT) != 0
            || client.getVarbitValue(VarbitID.INSIDE_WILDERNESS) != 0 || client.getWorldType() == null) return false;
        for (WorldType type : client.getWorldType())
            if (type == WorldType.PVP || type == WorldType.DEADMAN || type == WorldType.BOUNTY
                || type == WorldType.PVP_ARENA || type == WorldType.LAST_MAN_STANDING
                || type == WorldType.TOURNAMENT_WORLD || type == WorldType.SEASONAL
                || type == WorldType.BETA_WORLD || type == WorldType.NOSAVE_MODE) return false;
        return true;
    }

    void reset(boolean enabled)
    {
        generation++;
        active = enabled;
        cache.clear();
        pending = 0;
        nearbyQueue.clear();
        waitingName = null;
        waitingExplicit = false;
        revision++;
        if (enabled) saved.load(clock.getAsLong()).forEach((name, entry) ->
            put(name, new Cached(entry.badge(), entry.expires())));

    }

    void observe(ChatMessage event)
    {
        if (!active || !permitted.getAsBoolean() || !saved.currentProfile() || !config.playerIronmanIdentificationBeta() || !PLAYER_CHAT.contains(event.getType())) return;
        String name = normalized(event.getName());
        if (name.isEmpty()) return;
        int icon = accountIcon(event.getName());

        if (icon < 0) return;
        Badge badge = new Badge(icon, false);
        long now = clock.getAsLong();
        put(name, new Cached(badge, now + PlayerBadgeCache.lifetime(badge)));
        saved.save(name, badge, now);
        revision++;
    }

    Badge find(String displayName)
    {
        if (!active || !permitted.getAsBoolean() || !saved.currentProfile() || !config.playerIronmanIdentificationBeta()) return null;
        String name = normalized(displayName);
        if (name.isEmpty()) return null;
        long now = clock.getAsLong();
        Cached known = cache.get(name);
        if (known != null && known.expires > now)
            return known.badge;
        cache.remove(name);
        if (waitingExplicit && !name.equals(waitingName) || pending >= 2 || nextRequest != 0 && now < nextRequest)
        {
            if (!waitingExplicit) { waitingName = name; waitingUntil = now + MINUTE; }
            return null;
        }
        start(name, now);
        return null;
    }

    private void start(String name, long now)
    {

        nearbyQueue.remove(name);
        nextRequest = now + 12_000L;
        pending++;
        Lookup request = new Lookup(name, generation, new Cached(null, now + MINUTE));
        request.entry.loading = true;
        put(name, request.entry);
        fetch(request, HiscoreEndpoint.IRONMAN);
    }

    void selected(String displayName)
    {
        if (!active || !permitted.getAsBoolean() || !saved.currentProfile() || !config.playerIronmanIdentificationBeta()) return;
        String name = normalized(displayName);
        if (name.isEmpty()) return;
        Cached known = cache.get(name);
        if (known != null && known.expires > clock.getAsLong() && (known.badge != null || known.loading)) return;
        cache.remove(name);
        waitingName = name; waitingExplicit = true; waitingUntil = clock.getAsLong() + MINUTE;
        tick();
    }

    void tick()
    {
        long now = clock.getAsLong();
        if (cache.entrySet().removeIf(entry -> entry.getValue().expires <= now)) revision++;
        if (!active || !permitted.getAsBoolean() || !saved.currentProfile() || !config.playerIronmanIdentificationBeta())
        { waitingName = null; waitingExplicit = false; nearbyQueue.clear(); return; }
        if (now >= waitingUntil) { waitingName = null; waitingExplicit = false; }
        if (pending >= 2 || now < nextRequest) return;
        if (waitingName != null)
        {
            String name = waitingName; waitingName = null; waitingExplicit = false;
            find(name);
            return;
        }
        while (!nearbyQueue.isEmpty())
        {
            String name = nearbyQueue.iterator().next(); nearbyQueue.remove(name);
            Cached known = cache.get(name);
            if (known != null && known.expires > now) continue;
            start(name, now);
            break;
        }
    }

    void nearby(Collection<String> names)
    {
        if (!active || !permitted.getAsBoolean() || !saved.currentProfile() || !config.playerIronmanIdentificationBeta()) { nearbyQueue.clear(); return; }
        LinkedHashSet<String> present = new LinkedHashSet<>();
        for (String raw : names)
        {
            String name = normalized(raw);
            if (!name.isEmpty()) present.add(name);
        }
        nearbyQueue.retainAll(present);
        if (waitingName != null && !present.contains(waitingName)) { waitingName = null; waitingExplicit = false; }
        long now = clock.getAsLong();
        List<String> candidates = new ArrayList<>(present);
        for (int checked = 0; checked < candidates.size() && nearbyQueue.size() < 64; checked++)
        {

            String name = candidates.get(Math.floorMod(nearbyCursor++, candidates.size()));
            Cached known = cache.get(name);
            if (known != null && known.expires > now) continue;
            nearbyQueue.add(name);
        }
    }

    long revision() { return revision; }

    private boolean current(Lookup request)
    {
        return active && permitted.getAsBoolean() && saved.currentProfile() && request.generation == generation && cache.get(request.name) == request.entry
            && config.playerIronmanIdentificationBeta();
    }

    private void fetch(Lookup request, HiscoreEndpoint endpoint)
    {
        if (!current(request)) { finish(request, null, 2); return; }
        CompletableFuture<HiscoreResult> future;
        try
        {

            future = hiscores.lookupAsync(request.name, endpoint);
        }
        catch (RuntimeException ex)
        {
            failed(request);
            return;
        }
        future.orTimeout(10, TimeUnit.SECONDS).whenComplete((result, error) ->
            clientThread.invokeLater(() -> complete(request, endpoint, result, error)));
    }

    private void complete(Lookup request, HiscoreEndpoint endpoint, HiscoreResult result, Throwable error)
    {
        if (!current(request)) { finish(request, null, 2); return; }

        if (error != null || result != null && (result.getSkill(HiscoreSkill.OVERALL) == null
            || result.getSkill(HiscoreSkill.OVERALL).getLevel() <= 0
            || result.getSkill(HiscoreSkill.OVERALL).getExperience() < 0))
        { failed(request); return; }
        long xp = result == null ? -1 : result.getSkill(HiscoreSkill.OVERALL).getExperience();
        switch (endpoint)
        {
            case IRONMAN:
                if (xp < 0) { finish(request, null, 5); return; }
                request.ironXp = xp;
                fetch(request, HiscoreEndpoint.NORMAL);
                break;
            case NORMAL:

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
        if (current(request))
        {
            if (badge != null || minutes == 5) failures = 0;
            long now = clock.getAsLong();
            put(request.name, new Cached(badge, now + (badge == null ? minutes * MINUTE : PlayerBadgeCache.lifetime(badge))));
            if (badge != null) saved.save(request.name, badge, now);
            revision++;
        }
    }

    private void failed(Lookup request)
    {
        if (current(request))
        {
            failures = Math.min(failures + 1, 5);
            nextRequest = Math.max(nextRequest, clock.getAsLong() + Math.min(300_000L, 15_000L << failures));
        }
        finish(request, null, 2);
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
            return mode + (hiscores ? " - Hiscores estimate" : " - observed in chat");
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
        boolean loading;
        Cached(Badge badge, long expires) { this.badge = badge; this.expires = expires; }
    }
}
