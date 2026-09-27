package com.se2030.vaccinationportal.assistant;

import com.se2030.vaccinationportal.assistant.config.AssistantProperties;
import com.se2030.vaccinationportal.assistant.service.InMemoryAssistantRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("InMemoryAssistantRateLimiter Tests")
class AssistantRateLimiterTests {

    private InMemoryAssistantRateLimiter rateLimiter;
    private AssistantProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AssistantProperties();
        properties.getRateLimit().setAnonymousPerMinute(3);
        properties.getRateLimit().setAuthenticatedPerMinute(5);
        rateLimiter = new InMemoryAssistantRateLimiter(properties);
    }

    @Test
    @DisplayName("Anonymous client is limited to anonymousPerMinute quota")
    void testAnonymousRateLimit() {
        String clientKey = "anon:test-hash-1";

        assertTrue(rateLimiter.tryAcquire(clientKey, false));
        assertTrue(rateLimiter.tryAcquire(clientKey, false));
        assertTrue(rateLimiter.tryAcquire(clientKey, false));

        // 4th request must fail
        assertFalse(rateLimiter.tryAcquire(clientKey, false));
    }

    @Test
    @DisplayName("Authenticated client is limited to authenticatedPerMinute quota")
    void testAuthenticatedRateLimit() {
        String clientKey = "auth:101";

        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.tryAcquire(clientKey, true), "Request " + (i + 1) + " should be permitted");
        }

        // 6th request must fail
        assertFalse(rateLimiter.tryAcquire(clientKey, true));
    }

    @Test
    @DisplayName("Separate client keys have isolated rate limit quotas")
    void testClientIsolation() {
        String clientA = "anon:client-a";
        String clientB = "anon:client-b";

        assertTrue(rateLimiter.tryAcquire(clientA, false));
        assertTrue(rateLimiter.tryAcquire(clientA, false));
        assertTrue(rateLimiter.tryAcquire(clientA, false));
        assertFalse(rateLimiter.tryAcquire(clientA, false));

        // Client B is still fresh
        assertTrue(rateLimiter.tryAcquire(clientB, false));
    }
}
