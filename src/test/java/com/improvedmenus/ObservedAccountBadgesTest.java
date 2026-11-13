package com.improvedmenus;

import net.runelite.api.ChatMessageType;
import net.runelite.api.IconID;
import org.junit.Test;
import static org.junit.Assert.*;

public class ObservedAccountBadgesTest
{
    @Test public void identifiesAllEightNativeBadgesAcrossIncomingPlayerChannels()
    {
        for (ChatMessageType type : new ChatMessageType[]{ChatMessageType.PUBLICCHAT, ChatMessageType.MODCHAT,
            ChatMessageType.AUTOTYPER, ChatMessageType.MODAUTOTYPER, ChatMessageType.PRIVATECHAT,
            ChatMessageType.MODPRIVATECHAT, ChatMessageType.FRIENDSCHAT, ChatMessageType.CLAN_CHAT,
            ChatMessageType.CLAN_GUEST_CHAT, ChatMessageType.CLAN_GIM_CHAT})
        {
            ObservedAccountBadges cache = new ObservedAccountBadges();
            for (ObservedAccountBadges.Badge badge : ObservedAccountBadges.Badge.values())
            {
                cache.observe(type, "<col=ffffff>" + badge.icon + "Alice_Bob</col>");
                assertSame(type + ": " + badge, badge, cache.find("alice\u00a0bob"));
            }
        }
    }

    @Test public void outgoingPrivateChatAndSystemMessagesCannotMisidentifyTheRecipient()
    {
        ObservedAccountBadges cache = new ObservedAccountBadges();
        cache.observe(ChatMessageType.PUBLICCHAT, IconID.IRONMAN + "Alice");
        for (ChatMessageType type : new ChatMessageType[]{ChatMessageType.PRIVATECHATOUT, ChatMessageType.CONSOLE,
            ChatMessageType.GAMEMESSAGE, ChatMessageType.CLAN_MESSAGE, ChatMessageType.LOGINLOGOUTNOTIFICATION})
        {
            cache.observe(type, IconID.JAGEX_MODERATOR + "Alice");
            cache.observe(type, IconID.JAGEX_MODERATOR + "Bob");
            assertSame(ObservedAccountBadges.Badge.IRONMAN, cache.find("Alice"));
            assertNull(cache.find("Bob"));
        }
    }

    @Test public void latestChatUpdatesOrRemovesOldStatus()
    {
        ObservedAccountBadges cache = new ObservedAccountBadges();
        cache.observe(ChatMessageType.PUBLICCHAT, IconID.HARDCORE_IRONMAN + "Alice");
        cache.observe(ChatMessageType.PUBLICCHAT, IconID.IRONMAN + "Alice");
        assertSame(ObservedAccountBadges.Badge.IRONMAN, cache.find("Alice"));
        cache.observe(ChatMessageType.PUBLICCHAT, "Alice");
        assertNull(cache.find("Alice"));
    }

    @Test public void onlySupportedNativeAccountIconsAreRecognized()
    {
        ObservedAccountBadges cache = new ObservedAccountBadges();
        for (String name : new String[]{"Alice", "<img=999>Alice", "<img=22>Alice", "<img=110>Alice",
            "<img=210>Alice", "<img=-1>Alice", "<img=999999999999999>Alice", "<img=10x>Alice"})
        {
            cache.observe(ChatMessageType.PUBLICCHAT, name);
            assertNull(name, cache.find("Alice"));
        }
        cache.observe(ChatMessageType.PUBLICCHAT, "<img=999><img=2>Alice");
        assertSame(ObservedAccountBadges.Badge.IRONMAN, cache.find("Alice"));
    }

    @Test public void moderatorCrownTakesPrecedenceWhenMoreThanOneKnownIconExists()
    {
        assertSame(ObservedAccountBadges.Badge.PLAYER_MODERATOR,
            ObservedAccountBadges.fromName(IconID.IRONMAN.toString() + IconID.PLAYER_MODERATOR + "Alice"));
        assertSame(ObservedAccountBadges.Badge.JAGEX_MODERATOR,
            ObservedAccountBadges.fromName(IconID.PLAYER_MODERATOR.toString() + IconID.JAGEX_MODERATOR + "Alice"));
    }

    @Test public void invalidOrMissingNamesAreNotRemembered()
    {
        ObservedAccountBadges cache = new ObservedAccountBadges();
        for (String name : new String[]{null, "", "<img=2>", "<img=2>Alice!", "<img=2>ThisNameIsFarTooLong"})
        {
            cache.observe(ChatMessageType.PUBLICCHAT, name); assertNull(cache.find(name));
        }
        cache.observe(null, "<img=2>Alice"); assertNull(cache.find("Alice"));
    }

    @Test public void observationsAreBoundedAndRecentlyUpdatedNamesSurviveEviction()
    {
        ObservedAccountBadges cache = new ObservedAccountBadges();
        for (int i = 0; i < ObservedAccountBadges.CAPACITY; i++) cache.observe(ChatMessageType.PUBLICCHAT, "<img=2>Player" + i);
        cache.observe(ChatMessageType.PUBLICCHAT, "<img=10>Player0");
        cache.observe(ChatMessageType.PUBLICCHAT, "<img=2>Newest");
        assertSame(ObservedAccountBadges.Badge.HARDCORE_IRONMAN, cache.find("Player0"));
        assertNull(cache.find("Player1"));
        assertSame(ObservedAccountBadges.Badge.IRONMAN, cache.find("Newest"));
        cache.clear(); assertNull(cache.find("Newest")); assertNull(cache.find("Player0"));
    }
}
