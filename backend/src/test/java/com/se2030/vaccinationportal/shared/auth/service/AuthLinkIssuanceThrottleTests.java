package com.se2030.vaccinationportal.shared.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the per-account issuance cooldown used by public authentication link flows (R06 / F2-ALIAS).
 * Uses explicit instants so behavior is fully deterministic.
 */
class AuthLinkIssuanceThrottleTests {

    private static final Instant NOW = Instant.parse("2026-09-27T12:00:00Z");
    private static final Instant T_PLUS_59S = NOW.plusSeconds(59);
    private static final Instant T_PLUS_60S = NOW.plusSeconds(60);

    @Test
    void firstRequest_isPermitted() {
        AuthLinkIssuanceThrottle throttle = new AuthLinkIssuanceThrottle(60);
        assertTrue(throttle.tryRecordIssuance(1L, NOW));
    }

    @Test
    void repeatRequest_insideCooldown_isBlocked() {
        AuthLinkIssuanceThrottle throttle = new AuthLinkIssuanceThrottle(60);
        assertTrue(throttle.tryRecordIssuance(1L, NOW));
        assertFalse(throttle.tryRecordIssuance(1L, NOW.plusSeconds(1)));
        assertFalse(throttle.tryRecordIssuance(1L, T_PLUS_59S),
                "A request one second before cooldown expiry must still be blocked");
    }

    @Test
    void cooldownExpiry_permitsLegitimateRetry() {
        AuthLinkIssuanceThrottle throttle = new AuthLinkIssuanceThrottle(60);
        assertTrue(throttle.tryRecordIssuance(1L, NOW));
        assertFalse(throttle.tryRecordIssuance(1L, T_PLUS_59S));
        assertTrue(throttle.tryRecordIssuance(1L, T_PLUS_60S),
                "Once the cooldown expires, the next legitimate request must pass");
    }

    @Test
    void sameAccountId_insideCooldown_isBlockedRegardlessOfLookupRouteOrSpelling() {
        AuthLinkIssuanceThrottle throttle = new AuthLinkIssuanceThrottle(60);
        Long accountId = 42L;

        assertTrue(throttle.tryRecordIssuance(accountId, NOW));
        // Any request resolving to the same account ID hits the identical cooldown key
        assertFalse(throttle.tryRecordIssuance(accountId, T_PLUS_59S),
                "Second request for same accountId inside cooldown must be blocked");
        assertFalse(throttle.tryRecordIssuance(Long.valueOf(42L), T_PLUS_59S),
                "Distinct object instance for same accountId must be blocked");
    }

    @Test
    void differentIdentities_areThrottledIndependently() {
        AuthLinkIssuanceThrottle throttle = new AuthLinkIssuanceThrottle(60);
        assertTrue(throttle.tryRecordIssuance(1L, NOW));
        assertTrue(throttle.tryRecordIssuance(2L, NOW),
                "A different account must never be blocked by another account's cooldown");
    }

    @Test
    void remainingCooldown_isReportedForDiagnostics() {
        AuthLinkIssuanceThrottle throttle = new AuthLinkIssuanceThrottle(60);
        assertEquals(java.time.Duration.ZERO, throttle.remainingCooldown(1L, NOW));
        throttle.tryRecordIssuance(1L, NOW);
        assertEquals(java.time.Duration.ofSeconds(30), throttle.remainingCooldown(1L, NOW.plusSeconds(30)));
        assertEquals(java.time.Duration.ZERO, throttle.remainingCooldown(1L, T_PLUS_60S));
    }

    @Test
    void zeroCooldown_neverBlocks() {
        AuthLinkIssuanceThrottle throttle = new AuthLinkIssuanceThrottle(0);
        assertTrue(throttle.tryRecordIssuance(1L, NOW));
        assertTrue(throttle.tryRecordIssuance(1L, NOW),
                "A zero-length cooldown degenerates to no throttling");
    }

    @Test
    void codexAttackSequence_victimPreservedUnderIdentityFlood_andSecondRequestDenied() {
        int maxCapacity = 5;
        AuthLinkIssuanceThrottle throttle = new AuthLinkIssuanceThrottle(60, maxCapacity);

        // 1. acquire victim
        Long victim = 1L;
        assertTrue(throttle.tryRecordIssuance(victim, NOW), "First request for victim must be permitted");

        // 2. fill limiter to max with distinct active identities
        for (long i = 2; i <= maxCapacity; i++) {
            assertTrue(throttle.tryRecordIssuance(i, NOW),
                    "Filling active entries up to capacity must succeed");
        }
        assertEquals(maxCapacity, throttle.trackedKeyCount(), "Throttle should be at capacity");

        // 3. try additional unique identities during flood
        for (long i = 100; i < 110; i++) {
            assertFalse(throttle.tryRecordIssuance(i, NOW.plusSeconds(5)),
                    "New identities must be rejected admission when capacity is saturated with active entries (F2)");
        }

        // 4. victim remains tracked (NOT evicted by the flood!)
        assertFalse(throttle.remainingCooldown(victim, NOW.plusSeconds(10)).isZero(),
                "Victim key must remain tracked and not evicted by the flood");

        // 5. second victim request inside cooldown remains denied
        assertFalse(throttle.tryRecordIssuance(victim, NOW.plusSeconds(10)),
                "Second victim request inside cooldown must be denied (preventing token/link rotation)");

        // 6. map size never exceeds max
        assertEquals(maxCapacity, throttle.trackedKeyCount(), "Map size must never exceed maxCapacity");
    }

    @Test
    void expiredHeadEntriesArePurged_withoutEvictingUnexpiredEntries() {
        int maxCapacity = 3;
        AuthLinkIssuanceThrottle throttle = new AuthLinkIssuanceThrottle(60, maxCapacity);

        assertTrue(throttle.tryRecordIssuance(1L, NOW));
        assertTrue(throttle.tryRecordIssuance(2L, NOW.plusSeconds(10)));
        assertTrue(throttle.tryRecordIssuance(3L, NOW.plusSeconds(20)));
        assertEquals(3, throttle.trackedKeyCount());

        // At NOW + 65s, account 1 is expired (>60s), but accounts 2 (+10s -> expires at 70s) and 3 (+20s -> expires at 80s) are not
        assertTrue(throttle.tryRecordIssuance(4L, NOW.plusSeconds(65)));

        // account 1 was purged; account 4 was inserted; size remains 3
        assertEquals(3, throttle.trackedKeyCount());
        assertEquals(java.time.Duration.ZERO, throttle.remainingCooldown(1L, NOW.plusSeconds(65)));
        assertFalse(throttle.remainingCooldown(2L, NOW.plusSeconds(65)).isZero());
        assertFalse(throttle.remainingCooldown(3L, NOW.plusSeconds(65)).isZero());
        assertFalse(throttle.remainingCooldown(4L, NOW.plusSeconds(65)).isZero());
    }

    @Test
    void sameKey_concurrentAttempts_admitExactlyOne() throws Exception {
        AuthLinkIssuanceThrottle throttle = new AuthLinkIssuanceThrottle(60, 100);
        int threadCount = 10;
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount);
        java.util.concurrent.CountDownLatch ready = new java.util.concurrent.CountDownLatch(threadCount);
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicInteger admitted = new java.util.concurrent.atomic.AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    if (throttle.tryRecordIssuance(99L, NOW)) {
                        admitted.incrementAndGet();
                    }
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        ready.await();
        start.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS));

        assertEquals(1, admitted.get(), "Concurrent same-key attempts must admit exactly one");
    }

    @Test
    void concurrentUniqueAdmissions_atCapacity_doNotExceedBound() throws Exception {
        int maxCapacity = 10;
        AuthLinkIssuanceThrottle throttle = new AuthLinkIssuanceThrottle(60, maxCapacity);
        int threadCount = 30;
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount);
        java.util.concurrent.CountDownLatch ready = new java.util.concurrent.CountDownLatch(threadCount);
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicInteger admitted = new java.util.concurrent.atomic.AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final long id = i;
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    if (throttle.tryRecordIssuance(id, NOW)) {
                        admitted.incrementAndGet();
                    }
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        ready.await();
        start.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS));

        assertEquals(maxCapacity, admitted.get(), "Admitted unique keys must not exceed maxCapacity");
        assertEquals(maxCapacity, throttle.trackedKeyCount(), "Tracked key count must not exceed maxCapacity");
    }

    @Test
    void nullAccountId_isNotThrottled() {
        AuthLinkIssuanceThrottle throttle = new AuthLinkIssuanceThrottle(60);
        assertTrue(throttle.tryRecordIssuance(null, NOW));
        assertEquals(java.time.Duration.ZERO, throttle.remainingCooldown(null, NOW));
    }
}
