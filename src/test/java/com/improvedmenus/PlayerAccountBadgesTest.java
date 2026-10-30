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
    private final PlayerBadgeCache saved = mock(PlayerBadgeCache.class);
    private PlayerAccountBadges badges;
    private static final long XP = 1_000_000;
    private static final long MINUTE = 60_000L;

    @Before public void setup()
    {
        when(saved.currentProfile()).thenReturn(true);
        when(config.playerIronmanBadge()).thenReturn(true);
        when(config.playerHiscores()).thenReturn(true);
        doAnswer(i -> { ((Runnable) i.getArgument(0)).run(); return null; }).when(thread).invokeLater(any(Runnable.class));
        when(hiscores.lookupAsync(anyString(), any())).thenAnswer(i -> {
            CompletableFuture<HiscoreResult> future = new CompletableFuture<>();
            responses.put(i.getArgument(0) + ":" + i.getArgument(1), future);
            return future;
        });
        badges = new PlayerAccountBadges(hiscores, thread, config, clock::get, saved);
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
    { badges.reset(true); responses.clear(); clearInvocations(hiscores); clock.addAndGet(13_000L); }

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

    @Test public void observedVariantsReplaceCachedEstimateButMissingHelmetDoesNotEraseEvidence()
    {
        begin(); reply(HARDCORE_IRONMAN, XP); reply(ULTIMATE_IRONMAN, -1);
        for (int icon : new int[]{2, 3, 10, 41, 42, 43})
        {
            chat("<img=" + icon + ">Alice"); PlayerAccountBadges.Badge badge = badges.find("Alice");
            assertEquals(icon, badge.icon); assertFalse(badge.hiscores);
            assertTrue(badge.description().contains("observed")); assertFalse(badge.description().contains("estimate"));
        }
        chat("Alice"); assertEquals(43, badges.find("Alice").icon); verify(hiscores, times(4)).lookupAsync(anyString(), any());
        fresh(); begin(); chat("Alice"); reply(HARDCORE_IRONMAN, XP); reply(ULTIMATE_IRONMAN, -1);
        assertEquals(10, badges.find("Alice").icon); verify(hiscores, times(4)).lookupAsync(anyString(), any());
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
        clock.addAndGet(8 * 24 * 60 * MINUTE); assertNull(badges.find("P256"));
    }

    @Test public void estimatesExpireAndNameNormalizationReusesTheSameLookup()
    {
        begin(); reply(HARDCORE_IRONMAN, -1); reply(ULTIMATE_IRONMAN, XP);
        assertNotNull(badges.find("<col=ffffff> ALICE </col>"));
        verify(hiscores, times(4)).lookupAsync(anyString(), any());
        clock.addAndGet(25 * 60 * MINUTE); assertNull(badges.find("Alice"));
        verify(hiscores, times(2)).lookupAsync("alice", IRONMAN);
        assertEquals("alice bob", PlayerAccountBadges.normalized("<img=2>Alice_Bob"));
        assertEquals("alice bob", PlayerAccountBadges.normalized("Alice\u00a0Bob"));
    }

    @Test public void rateAndConcurrencyAreBoundedWithoutBulkQueueAndCompletionFreesASlot()
    {
        badges.find("Alice"); clock.addAndGet(11_000L); badges.find("Bob");
        verify(hiscores, times(1)).lookupAsync(anyString(), any());
        clock.addAndGet(2_000L); badges.find("Bob");
        clock.addAndGet(13_000L); badges.find("Carol");
        verify(hiscores, times(2)).lookupAsync(anyString(), any());
        reply(IRONMAN, -1); badges.find("Carol"); verify(hiscores, times(3)).lookupAsync(anyString(), any());
    }

    @Test public void anOldGenerationCannotReleaseNewRequestsOrBypassRateLimit()
    {
        badges.find("Alice"); CompletableFuture<HiscoreResult> old = response("alice", IRONMAN);
        badges.reset(true); badges.find("Bob"); verify(hiscores, times(1)).lookupAsync(anyString(), any());
        clock.addAndGet(13_000L); badges.find("Bob");
        clock.addAndGet(13_000L); badges.find("Carol"); old.complete(null);
        clock.addAndGet(13_000L); badges.find("Dave"); verify(hiscores, times(3)).lookupAsync(anyString(), any());
    }

    @Test public void cacheEvictionStopsAnInFlightChain()
    {
        badges.find("Alice"); for (int i = 0; i < 257; i++) chat("<img=2>P" + i);
        reply(IRONMAN, XP); verify(hiscores, times(1)).lookupAsync(anyString(), any()); assertNull(badges.find("Alice"));
    }

    @Test public void unrankedOverallStillProvidesValidXpForComparison()
    {
        badges.find("Alice"); reply(IRONMAN, XP);
        response("alice", NORMAL).complete(new HiscoreResult("Alice",
            Collections.singletonMap(HiscoreSkill.OVERALL, new Skill(-1, 1200, XP))));
        reply(HARDCORE_IRONMAN, -1); reply(ULTIMATE_IRONMAN, -1);
        assertEquals(2, badges.find("Alice").icon);
    }

    @Test public void latestOpenedPlayerWaitsForCooldownInsteadOfBeingDropped()
    {
        badges.find("Alice"); reply(IRONMAN, -1); badges.find("Bob"); badges.find("Carol");
        badges.tick(); verify(hiscores, times(1)).lookupAsync(anyString(), any());
        clock.addAndGet(13_000L); badges.tick();
        verify(hiscores).lookupAsync("carol", IRONMAN); verify(hiscores, never()).lookupAsync("bob", IRONMAN);
        badges.tick(); verify(hiscores, times(2)).lookupAsync(anyString(), any());
    }

    @Test public void explicitLookupTakesPriorityWithoutBypassingLimitsOrChat()
    {
        badges.find("Alice"); reply(IRONMAN, -1); badges.find("Bob");
        badges.selected("Alice"); badges.find("Carol"); clock.addAndGet(13_000L); badges.tick();
        verify(hiscores, times(2)).lookupAsync("alice", IRONMAN);
        verify(hiscores, never()).lookupAsync("bob", IRONMAN); verify(hiscores, never()).lookupAsync("carol", IRONMAN);
        chat("<img=3>Alice"); reply(IRONMAN, XP); badges.selected("Alice");
        assertEquals(3, badges.find("Alice").icon); verify(hiscores, times(2)).lookupAsync(anyString(), any());
    }

    @Test public void deferredRequestExpiresAndIsCancelledByDisableOrReset()
    {
        badges.find("Alice"); reply(IRONMAN, -1); badges.selected("Bob");
        clock.addAndGet(2 * MINUTE); badges.tick(); verify(hiscores, never()).lookupAsync("bob", IRONMAN);
        badges.find("Carol"); badges.selected("Bob"); badges.reset(true);
        clock.addAndGet(13_000L); badges.tick(); verify(hiscores, never()).lookupAsync("bob", IRONMAN);
        badges.find("Dave"); badges.selected("Bob"); when(config.playerHiscores()).thenReturn(false);
        clock.addAndGet(13_000L); badges.tick(); verify(hiscores, never()).lookupAsync("bob", IRONMAN);
    }

    @Test public void restoredBadgesAvoidNetworkAndEstimatedOnesRespectTheirSwitch()
    {
        when(saved.load(anyLong())).thenReturn(Collections.singletonMap("alice", new PlayerBadgeCache.Entry("alice", 3, false, 1)));
        badges.reset(true); assertEquals(3, badges.find("Alice").icon);
        badges.reset(false); badges.reset(true); assertEquals(3, badges.find("Alice").icon);
        verifyNoInteractions(hiscores);
        when(saved.load(anyLong())).thenReturn(Collections.singletonMap("alice", new PlayerBadgeCache.Entry("alice", 10, true, 1)));
        badges.reset(true); when(config.playerHiscores()).thenReturn(false); assertNull(badges.find("Alice"));
        when(config.playerHiscores()).thenReturn(true); assertEquals(10, badges.find("Alice").icon);
        verifyNoInteractions(hiscores);
    }

    @Test public void onlyResolvedHelmetsAreSavedAndCompletionSignalsMenuRefresh()
    {
        long before = badges.revision(); begin(); reply(HARDCORE_IRONMAN, -1); reply(ULTIMATE_IRONMAN, XP);
        assertTrue(badges.revision() > before);
        verify(saved).save(eq("alice"), argThat(b -> b.icon == 3 && b.hiscores), anyLong());
        clearInvocations(saved); fresh(); badges.find("Alice"); reply(IRONMAN, -1);
        verify(saved, never()).save(anyString(), any(), anyLong());
        fresh(); begin(); when(saved.currentProfile()).thenReturn(false); reply(HARDCORE_IRONMAN, XP);
        verify(saved, never()).save(anyString(), any(), anyLong());
    }
}
