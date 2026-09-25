package com.se2030.vaccinationportal.assistant;

import com.se2030.vaccinationportal.assistant.config.AssistantProperties;
import com.se2030.vaccinationportal.assistant.service.InMemoryAssistantRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InMemoryAssistantRateLimiter Lifecycle & Clock Tests")
class AssistantRateLimiterLifecycleTests {

    static class MutableTestClock extends Clock {
        private Instant currentInstant;
        private final ZoneId zone;

        public MutableTestClock(Instant initialInstant, ZoneId zone) {
            this.currentInstant = initialInstant;
            this.zone = zone;
        }

        public void advanceSeconds(long seconds) {
            this.currentInstant = this.currentInstant.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableTestClock(this.currentInstant, zone);
        }

        @Override
        public Instant instant() {
            return currentInstant;
        }
    }

    private MutableTestClock testClock;
    private AssistantProperties properties;
    private InMemoryAssistantRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        testClock = new MutableTestClock(Instant.parse("2026-09-25T12:00:00Z"), ZoneId.of("UTC"));
        properties = new AssistantProperties();
        properties.getRateLimit().setAnonymousPerMinute(3);
        properties.getRateLimit().setAuthenticatedPerMinute(5);

        rateLimiter = new InMemoryAssistantRateLimiter(properties, testClock);
    }

    @Test
    @DisplayName("Window expires after 60 seconds and resets client quota")
    void testSlidingWindowExpiry() {
        String clientKey = "anon:client-expiry-test";

        assertTrue(rateLimiter.tryAcquire(clientKey, false));
        assertTrue(rateLimiter.tryAcquire(clientKey, false));
        assertTrue(rateLimiter.tryAcquire(clientKey, false));
        // 4th is blocked in current minute
        assertFalse(rateLimiter.tryAcquire(clientKey, false));

        // Advance clock by 61 seconds
        testClock.advanceSeconds(61);

        // Client can acquire again in the new window
        assertTrue(rateLimiter.tryAcquire(clientKey, false));
    }

    @Test
    @DisplayName("cleanupExpiredKeys purges inactive entries from tracking map")
    void testKeyCleanupLifecycle() {
        String client1 = "anon:client-1";
        String client2 = "anon:client-2";

        assertTrue(rateLimiter.tryAcquire(client1, false));
        assertTrue(rateLimiter.tryAcquire(client2, false));
        assertEquals(2, rateLimiter.getTrackedKeyCount());

        // Advance clock past expiration
        testClock.advanceSeconds(65);

        // Run cleanup
        rateLimiter.cleanup();

        // Expired keys should be purged
        assertEquals(0, rateLimiter.getTrackedKeyCount());
    }

    @Test
    @DisplayName("Concurrent acquisitions execute thread-safely without race conditions")
    void testConcurrentAcquisitions() throws InterruptedException {
        int threads = 10;
        int attemptsPerThread = 5;
        properties.getRateLimit().setAuthenticatedPerMinute(10);

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger totalPermitted = new AtomicInteger(0);

        String clientKey = "auth:concurrent-user";

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < attemptsPerThread; j++) {
                        if (rateLimiter.tryAcquire(clientKey, true)) {
                            totalPermitted.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Exactly 10 permits should have been granted across all 50 concurrent attempts
        assertEquals(10, totalPermitted.get());
    }
}
