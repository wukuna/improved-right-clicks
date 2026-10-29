package com.improvedmenus;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.hiscore.*;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PlayerAccountBadgesTest
{
    private final ImprovedMenusConfig config = mock(ImprovedMenusConfig.class);
    private final HiscoreClient hiscores = mock(HiscoreClient.class);
    private final ClientThread thread = mock(ClientThread.class);
    private final AtomicLong clock = new AtomicLong(1);
    private PlayerAccountBadges badges;
    private CompletableFuture<HiscoreResult> result;

    @Before public void setup()
    {
        when(config.playerIronmanBadge()).thenReturn(true);
        when(config.playerHiscores()).thenReturn(true);
        doAnswer(i -> { ((Runnable) i.getArgument(0)).run(); return null; }).when(thread).invokeLater(any(Runnable.class));
        result = new CompletableFuture<>();
        when(hiscores.lookupAsync(anyString(), eq(HiscoreEndpoint.IRONMAN))).thenAnswer(i -> result);
        badges = new PlayerAccountBadges(hiscores, thread, config, clock::get);
        badges.reset(true);
    }

    private static HiscoreResult ranked()
    { return new HiscoreResult("Alice", Collections.singletonMap(HiscoreSkill.OVERALL, new Skill(5, 2000, 1000000))); }
    private void chat(String name)
    { badges.observe(new ChatMessage(null, ChatMessageType.PUBLICCHAT, name, "hi", "", 0)); }

    @Test public void lookupIsAsyncCachedAndExplicitlyUnverified()
    {
        assertNull(badges.find("Alice"));
        assertNull(badges.find("ALICE"));
        verify(hiscores, times(1)).lookupAsync("alice", HiscoreEndpoint.IRONMAN);
        result.complete(ranked());
        PlayerAccountBadges.Badge badge = badges.find("Alice");
        assertEquals(2, badge.icon); assertTrue(badge.hiscores);
        assertTrue(badge.markup().contains("?")); assertTrue(badge.description().contains("unverified"));
        verify(thread).invokeLater(any(Runnable.class));
        verifyNoMoreInteractions(hiscores);
    }

    @Test public void nativeModeOverridesPendingHiscoresAndSupportsGroupVariants()
    {
        badges.find("Alice");
        chat("<img=42>Alice");
        result.complete(ranked());
        assertEquals(42, badges.find("Alice").icon);
        assertFalse(badges.find("Alice").hiscores);
        for (int icon : new int[]{2, 3, 10, 41, 42, 43})
        { chat("<img=" + icon + ">Alice"); assertEquals(icon, badges.find("Alice").icon); }
    }

    @Test public void plainChatSupersedesOldStatusWithoutTurningMissingHiscoresIntoNormalMode()
    {
        chat("<img=10>Alice"); chat("Alice");
        assertNull(badges.find("Alice")); verifyNoInteractions(hiscores);
        clock.addAndGet(16 * 60_000_000_000L);
        badges.find("Alice"); result.complete(null);
        assertNull(badges.find("Alice"));
        verify(hiscores, times(1)).lookupAsync(anyString(), any());
    }

    @Test public void lookupFailureAndUnrankedResultsStayBlankWithBackoff()
    {
        badges.find("Alice"); result.completeExceptionally(new IllegalStateException("offline"));
        assertNull(badges.find("Alice"));
        verify(hiscores, times(1)).lookupAsync(anyString(), any());
        clock.addAndGet(3 * 60_000_000_000L); result = new CompletableFuture<>();
        badges.find("Alice");
        result.complete(new HiscoreResult("Alice", Collections.singletonMap(HiscoreSkill.OVERALL, new Skill(-1, 0, 0))));
        assertNull(badges.find("Alice"));
    }

    @Test public void disableAndProfileChangesDiscardInFlightResults()
    {
        badges.find("Alice"); badges.reset(false); result.complete(ranked());
        assertNull(badges.find("Alice"));
        badges.reset(true); clock.addAndGet(4_000_000_000L); result = new CompletableFuture<>();
        badges.find("Alice"); badges.reset(true); result.complete(ranked());
        assertNull(badges.find("Alice"));
    }

    @Test public void noNetworkWhenEitherSwitchIsOffOrNameInvalid()
    {
        when(config.playerHiscores()).thenReturn(false); assertNull(badges.find("Alice"));
        when(config.playerHiscores()).thenReturn(true); when(config.playerIronmanBadge()).thenReturn(false);
        assertNull(badges.find("Alice"));
        when(config.playerIronmanBadge()).thenReturn(true);
        assertNull(badges.find("name?player=x")); assertNull(badges.find("way too long player"));
        verifyNoInteractions(hiscores);
    }

    @Test public void onlyPlayerChatIsObservedAndCacheIsBounded()
    {
        when(config.playerHiscores()).thenReturn(false);
        badges.observe(new ChatMessage(null, ChatMessageType.CONSOLE, "<img=10>Alice", "", "", 0));
        assertNull(badges.find("Alice"));
        for (int i = 0; i < 257; i++) chat("<img=2>P" + i);
        assertNull(badges.find("P0")); assertNotNull(badges.find("P256"));
        clock.addAndGet(16 * 60_000_000_000L); assertNull(badges.find("P256"));
    }

    @Test public void rateAndConcurrencyAreBoundedWithoutBulkQueue()
    {
        badges.find("Alice"); badges.find("Bob");
        verify(hiscores, times(1)).lookupAsync(anyString(), any());
        clock.addAndGet(4_000_000_000L); result = new CompletableFuture<>(); badges.find("Bob");
        clock.addAndGet(4_000_000_000L); badges.find("Carol");
        verify(hiscores, times(2)).lookupAsync(anyString(), any());
    }
}
