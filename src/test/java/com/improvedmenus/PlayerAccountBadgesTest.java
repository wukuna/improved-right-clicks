package com.improvedmenus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.hiscore.*;
import org.junit.Before;
import org.junit.Test;
import static net.runelite.client.hiscore.HiscoreEndpoint.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PlayerAccountBadgesTest
{
    private final ImprovedMenusConfig config = mock(ImprovedMenusConfig.class);
    private final HiscoreClient hiscores = mock(HiscoreClient.class);
    private final ClientThread thread = mock(ClientThread.class);
    private final AtomicLong clock = new AtomicLong(1);
    private final Map<String, CompletableFuture<HiscoreResult>> responses = new HashMap<>();
    private PlayerAccountBadges badges;
    private static final long XP = 1_000_000;
    private static final long MINUTE = 60_000_000_000L;

    @Before public void setup()
    {
        when(config.playerIronmanBadge()).thenReturn(true);
        when(config.playerHiscores()).thenReturn(true);
        doAnswer(i -> { ((Runnable) i.getArgument(0)).run(); return null; }).when(thread).invokeLater(any(Runnable.class));
        when(hiscores.lookupAsync(anyString(), any())).thenAnswer(i -> {
            CompletableFuture<HiscoreResult> future = new CompletableFuture<>();
            responses.put(i.getArgument(0) + ":" + i.getArgument(1), future);
            return future;
        });
        badges = new PlayerAccountBadges(hiscores, thread, config, clock::get);
        badges.reset(true);
    }

    private static HiscoreResult ranked(long xp)
    { return new HiscoreResult("Alice", Collections.singletonMap(HiscoreSkill.OVERALL, new Skill(5, 2000, xp))); }
    private CompletableFuture<HiscoreResult> response(String name, HiscoreEndpoint endpoint)
    {
        CompletableFuture<HiscoreResult> future = responses.get(name + ":" + endpoint);
        assertNotNull("Request must already have started: " + name + ":" + endpoint, future);
        return future;
    }
    private void reply(HiscoreEndpoint endpoint, long xp)
    { response("alice", endpoint).complete(xp < 0 ? null : ranked(xp)); }
    private void begin()
    { assertNull(badges.find("Alice")); reply(IRONMAN, XP); reply(NORMAL, XP); }
    private void chat(String name)
    { badges.observe(new ChatMessage(null, ChatMessageType.PUBLICCHAT, name, "hi", "", 0)); }
    private void fresh()
    { badges.reset(true); responses.clear(); clearInvocations(hiscores); clock.addAndGet(13_000_000_000L); }

    @Test public void matchingXpInfersAllThreeModesAndLabelsEveryHelmet()
    {
        long[][] examples = {{-1, -1, 2}, {XP - 1, -1, 2}, {-1, XP - 1, 2}, {XP, -1, 10}, {-1, XP, 3}};
        for (long[] example : examples)
        {
            fresh(); begin(); reply(HARDCORE_IRONMAN, example[0]); assertNull(badges.find("Alice"));
            reply(ULTIMATE_IRONMAN, example[1]);
            PlayerAccountBadges.Badge badge = badges.find("Alice");
            assertEquals((int) example[2], badge.icon); assertTrue(badge.hiscores);
            assertEquals("<img=" + example[2] + ">", badge.markup());
            assertTrue(badge.description().endsWith(" - Hiscores estimate")); badges.find("ALICE");
            verify(hiscores, times(4)).lookupAsync(anyString(), any());
        }
    }

    @Test public void zeroXpIsAValidMatchAndLowLevelIronmanNeedNotAppearOnNormalHiscores()
    {
        badges.find("Alice"); reply(IRONMAN, 0); reply(NORMAL, -1);
        reply(HARDCORE_IRONMAN, 0); reply(ULTIMATE_IRONMAN, -1);
        assertEquals(10, badges.find("Alice").icon);
    }

    @Test public void normalXpMustAgreeWhenPresentSoProgressedDeironDoesNotGetHelmet()
    {
        for (long xp : new long[]{XP + 1, XP - 1})
        {
            fresh(); badges.find("Alice"); reply(IRONMAN, XP); reply(NORMAL, xp);
            assertNull(badges.find("Alice")); verify(hiscores, times(2)).lookupAsync(anyString(), any());
        }
    }

    @Test public void contradictorySpecialtyRankingsStayBlank()
    {
        begin(); reply(HARDCORE_IRONMAN, XP + 1);
        assertNull(badges.find("Alice")); verify(hiscores, times(3)).lookupAsync(anyString(), any());
        fresh(); begin(); reply(HARDCORE_IRONMAN, -1); reply(ULTIMATE_IRONMAN, XP + 1);
        assertNull(badges.find("Alice"));
        fresh(); begin(); reply(HARDCORE_IRONMAN, XP); reply(ULTIMATE_IRONMAN, XP);
        assertNull(badges.find("Alice"));
    }

    @Test public void absentIronmanListingStopsWithoutInferringNormalOrGroupMode()
    {
        badges.find("Alice"); reply(IRONMAN, -1); assertNull(badges.find("Alice"));
        verify(hiscores, times(1)).lookupAsync(anyString(), any());
        clock.addAndGet(6 * MINUTE); badges.find("Alice");
        verify(hiscores, times(2)).lookupAsync("alice", IRONMAN);
    }

    @Test public void failedEndpointNeverMasqueradesAsAbsentSpecialtyRanking()
    {
        HiscoreEndpoint[] endpoints = {IRONMAN, NORMAL, HARDCORE_IRONMAN, ULTIMATE_IRONMAN};
        for (int failing = 0; failing < endpoints.length; failing++)
        {
            fresh(); badges.find("Alice");
            for (int i = 0; i < failing; i++) reply(endpoints[i], XP);
            response("alice", endpoints[failing]).completeExceptionally(new IllegalStateException("offline"));
            assertNull(badges.find("Alice")); verify(hiscores, times(failing + 1)).lookupAsync(anyString(), any());
            clock.addAndGet(3 * MINUTE); badges.find("Alice");
            verify(hiscores, times(2)).lookupAsync("alice", IRONMAN);
        }
    }

    @Test public void invalidResponsesAndSynchronousFailureStayBlank()
    {
        HiscoreResult[] invalid = {
            new HiscoreResult("Alice", Collections.emptyMap()),
            new HiscoreResult("Alice", Collections.singletonMap(HiscoreSkill.OVERALL, new Skill(-1, 0, 0))), ranked(-1)
        };
        for (HiscoreResult result : invalid)
        {
            fresh(); begin(); response("alice", HARDCORE_IRONMAN).complete(result);
            assertNull(badges.find("Alice")); verify(hiscores, times(3)).lookupAsync(anyString(), any());
        }
        fresh(); when(hiscores.lookupAsync("alice", NORMAL)).thenThrow(new IllegalStateException("unavailable"));
        badges.find("Alice"); reply(IRONMAN, XP); assertNull(badges.find("Alice"));
        verify(hiscores, times(2)).lookupAsync(anyString(), any());
    }

    @Test public void nativeChatOverridesEveryPendingStageAndStopsFurtherRequests()
    {
        HiscoreEndpoint[] endpoints = {IRONMAN, NORMAL, HARDCORE_IRONMAN, ULTIMATE_IRONMAN};
        for (int stage = 0; stage < endpoints.length; stage++)
        {
            fresh(); badges.find("Alice");
            for (int i = 0; i < stage; i++) reply(endpoints[i], XP);
            chat("<img=42>Alice"); reply(endpoints[stage], XP);
            assertEquals(42, badges.find("Alice").icon); assertFalse(badges.find("Alice").hiscores);
            verify(hiscores, times(stage + 1)).lookupAsync(anyString(), any());
        }
    }

    @Test public void observedVariantsReplaceCachedEstimateAndPlainChatSupersedesOldStatus()
    {
        begin(); reply(HARDCORE_IRONMAN, XP); reply(ULTIMATE_IRONMAN, -1);
        for (int icon : new int[]{2, 3, 10, 41, 42, 43})
        {
            chat("<img=" + icon + ">Alice"); PlayerAccountBadges.Badge badge = badges.find("Alice");
            assertEquals(icon, badge.icon); assertFalse(badge.hiscores);
            assertTrue(badge.description().contains("observed")); assertFalse(badge.description().contains("estimate"));
        }
        chat("Alice"); assertNull(badges.find("Alice")); verify(hiscores, times(4)).lookupAsync(anyString(), any());
        fresh(); begin(); chat("Alice"); reply(HARDCORE_IRONMAN, XP);
        assertNull(badges.find("Alice")); verify(hiscores, times(3)).lookupAsync(anyString(), any());
    }

    @Test public void disableAndProfileChangesDiscardInFlightResults()
    {
        begin(); badges.reset(false); reply(HARDCORE_IRONMAN, XP);
        assertNull(badges.find("Alice")); verify(hiscores, times(3)).lookupAsync(anyString(), any());
        fresh(); begin(); badges.reset(true); reply(HARDCORE_IRONMAN, XP);
        assertNull(badges.find("Alice")); verify(hiscores, times(3)).lookupAsync(anyString(), any());
    }

    @Test public void changingEitherSwitchStopsTheNextEndpointWithoutPublishing()
    {
        begin(); when(config.playerHiscores()).thenReturn(false); reply(HARDCORE_IRONMAN, XP);
        assertNull(badges.find("Alice")); verify(hiscores, times(3)).lookupAsync(anyString(), any());
        when(config.playerHiscores()).thenReturn(true); fresh(); begin();
        when(config.playerIronmanBadge()).thenReturn(false); reply(HARDCORE_IRONMAN, XP);
        assertNull(badges.find("Alice")); verify(hiscores, times(3)).lookupAsync(anyString(), any());
    }

    @Test public void asyncCompletionOnlyAdvancesOnClientThreadAndRechecksObservation()
    {
        List<Runnable> callbacks = new ArrayList<>();
        doAnswer(i -> { callbacks.add(i.getArgument(0)); return null; }).when(thread).invokeLater(any(Runnable.class));
        badges.find("Alice"); reply(IRONMAN, XP);
        assertNull(badges.find("Alice")); verify(hiscores, times(1)).lookupAsync(anyString(), any());
        callbacks.remove(0).run(); reply(NORMAL, XP); callbacks.remove(0).run();
        reply(HARDCORE_IRONMAN, XP); callbacks.remove(0).run(); reply(ULTIMATE_IRONMAN, -1);
        assertNull(badges.find("Alice")); chat("<img=3>Alice"); callbacks.remove(0).run();
        assertEquals(3, badges.find("Alice").icon); assertFalse(badges.find("Alice").hiscores);
    }

    @Test public void noNetworkWhenEitherSwitchIsOffOrNameInvalid()
    {
        when(config.playerHiscores()).thenReturn(false); assertNull(badges.find("Alice"));
        when(config.playerHiscores()).thenReturn(true); when(config.playerIronmanBadge()).thenReturn(false);
        assertNull(badges.find("Alice")); when(config.playerIronmanBadge()).thenReturn(true);
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
        clock.addAndGet(16 * MINUTE); assertNull(badges.find("P256"));
    }

    @Test public void estimatesExpireAndNameNormalizationReusesTheSameLookup()
    {
        begin(); reply(HARDCORE_IRONMAN, -1); reply(ULTIMATE_IRONMAN, XP);
        assertNotNull(badges.find("<col=ffffff> ALICE </col>"));
        verify(hiscores, times(4)).lookupAsync(anyString(), any());
        clock.addAndGet(16 * MINUTE); assertNull(badges.find("Alice"));
        verify(hiscores, times(2)).lookupAsync("alice", IRONMAN);
        assertEquals("alice bob", PlayerAccountBadges.normalized("<img=2>Alice_Bob"));
        assertEquals("alice bob", PlayerAccountBadges.normalized("Alice\u00a0Bob"));
    }

    @Test public void rateAndConcurrencyAreBoundedWithoutBulkQueueAndCompletionFreesASlot()
    {
        badges.find("Alice"); clock.addAndGet(11_000_000_000L); badges.find("Bob");
        verify(hiscores, times(1)).lookupAsync(anyString(), any());
        clock.addAndGet(2_000_000_000L); badges.find("Bob");
        clock.addAndGet(13_000_000_000L); badges.find("Carol");
        verify(hiscores, times(2)).lookupAsync(anyString(), any());
        reply(IRONMAN, -1); badges.find("Carol"); verify(hiscores, times(3)).lookupAsync(anyString(), any());
    }

    @Test public void anOldGenerationCannotReleaseNewRequestsOrBypassRateLimit()
    {
        badges.find("Alice"); CompletableFuture<HiscoreResult> old = response("alice", IRONMAN);
        badges.reset(true); badges.find("Bob"); verify(hiscores, times(1)).lookupAsync(anyString(), any());
        clock.addAndGet(13_000_000_000L); badges.find("Bob");
        clock.addAndGet(13_000_000_000L); badges.find("Carol"); old.complete(null);
        clock.addAndGet(13_000_000_000L); badges.find("Dave"); verify(hiscores, times(3)).lookupAsync(anyString(), any());
    }

    @Test public void cacheEvictionStopsAnInFlightChain()
    {
        badges.find("Alice"); for (int i = 0; i < 257; i++) chat("<img=2>P" + i);
        reply(IRONMAN, XP); verify(hiscores, times(1)).lookupAsync(anyString(), any()); assertNull(badges.find("Alice"));
    }
}
