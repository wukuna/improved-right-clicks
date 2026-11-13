package com.improvedmenus;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.runelite.api.ChatMessageType;
import net.runelite.api.IconID;

final class ObservedAccountBadges
{
    static final int CAPACITY = 10000;
    private final Map<String, Observation> observed = new LinkedHashMap<>();
    private final Map<String, Observation> pending = new LinkedHashMap<>();

    static final class Observation
    {
        final Badge badge;
        final long seen;
        Observation(Badge badge, long seen) { this.badge = badge; this.seen = seen; }
    }

    enum Badge
    {
        JAGEX_MODERATOR(IconID.JAGEX_MODERATOR, "Jagex Moderator"),
        PLAYER_MODERATOR(IconID.PLAYER_MODERATOR, "Player Moderator"),
        HARDCORE_GROUP_IRONMAN(IconID.HARDCORE_GROUP_IRONMAN, "Hardcore Group Ironman"),
        UNRANKED_GROUP_IRONMAN(IconID.UNRANKED_GROUP_IRONMAN, "Unranked Group Ironman"),
        GROUP_IRONMAN(IconID.GROUP_IRONMAN, "Group Ironman"),
        HARDCORE_IRONMAN(IconID.HARDCORE_IRONMAN, "Hardcore Ironman"),
        ULTIMATE_IRONMAN(IconID.ULTIMATE_IRONMAN, "Ultimate Ironman"),
        IRONMAN(IconID.IRONMAN, "Ironman");

        final IconID icon;
        final String label;
        Badge(IconID icon, String label) { this.icon = icon; this.label = label; }
    }

    synchronized void observe(ChatMessageType type, String rawName)
    {
        if (type == null || !isIncomingPlayerChat(type)) return;
        String key = key(rawName);
        if (key.isEmpty()) return;
        Badge badge = fromName(rawName);
        Observation previous = observed.get(key);
        long now = System.currentTimeMillis();
        if (previous != null && previous.badge == badge && now - previous.seen < 60000) return;
        Observation next = new Observation(badge, previous == null ? now : Math.max(now, previous.seen + 1));
        observed.remove(key);
        observed.put(key, next);
        pending.remove(key);
        pending.put(key, next);
        trim(observed);
        trim(pending);
    }

    synchronized Badge find(String name)
    {
        Observation observation = observed.get(key(name));
        return observation == null ? null : observation.badge;
    }

    synchronized void clear() { observed.clear(); pending.clear(); }
    synchronized Map<String, Observation> pending() { return new LinkedHashMap<>(pending); }

    synchronized void saved(Map<String, Observation> records, Map<String, Observation> written)
    {
        merge(observed, records);
        written.forEach((name, entry) -> pending.remove(name, entry));
    }

    static void merge(Map<String, Observation> target, Map<String, Observation> incoming)
    {
        incoming.forEach((name, entry) -> {
            Observation old = target.get(name);
            if (old == null || entry.seen > old.seen
                || (entry.seen == old.seen && rank(entry.badge) < rank(old.badge))) target.put(name, entry);
        });
        trim(target);
    }

    private static int rank(Badge badge) { return badge == null ? -1 : badge.ordinal(); }

    private static void trim(Map<String, Observation> records)
    {
        if (records.size() <= CAPACITY) return;
        java.util.List<String> names = new java.util.ArrayList<>(records.keySet());
        names.sort(java.util.Comparator.comparingLong(name -> records.get(name).seen));
        for (int i = 0; i < names.size() - CAPACITY; i++) records.remove(names.get(i));
    }

    static Badge fromName(String name)
    {
        if (name != null)
            for (Badge badge : Badge.values())
                if (name.contains(badge.icon.toString())) return badge;
        return null;
    }

    private static boolean isIncomingPlayerChat(ChatMessageType type)
    {
        switch (type)
        {
            case PUBLICCHAT:
            case MODCHAT:
            case AUTOTYPER:
            case MODAUTOTYPER:
            case PRIVATECHAT:
            case MODPRIVATECHAT:
            case FRIENDSCHAT:
            case CLAN_CHAT:
            case CLAN_GUEST_CHAT:
            case CLAN_GIM_CHAT:
                return true;
            default:
                return false;
        }
    }

    static String key(String name)
    {
        if (name == null) return "";
        String key = MenuRules.plain(name).replace('\u00a0', ' ').replace('_', ' ').trim().toLowerCase(Locale.ROOT);
        return key.matches("[a-z0-9 -]{1,12}") ? key : "";
    }
}
