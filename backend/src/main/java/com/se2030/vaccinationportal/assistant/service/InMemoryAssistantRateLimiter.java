package com.se2030.vaccinationportal.assistant.service;

import com.se2030.vaccinationportal.assistant.config.AssistantProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sliding window rate limiter implementation.
 * Enforces per-minute quotas for anonymous vs authenticated actors.
 */
@Component
public class InMemoryAssistantRateLimiter implements AssistantRateLimiter {

    private final AssistantProperties properties;
    private final ConcurrentHashMap<String, Deque<Long>> requestWindows = new ConcurrentHashMap<>();

    public InMemoryAssistantRateLimiter(AssistantProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean tryAcquire(String clientKey, boolean authenticated) {
        if (clientKey == null || clientKey.isBlank()) {
            return false;
        }

        int maxRequests = authenticated
                ? properties.getRateLimit().getAuthenticatedPerMinute()
                : properties.getRateLimit().getAnonymousPerMinute();

        long now = System.currentTimeMillis();
        long windowStart = now - 60_000L;

        java.util.concurrent.atomic.AtomicBoolean permitted = new java.util.concurrent.atomic.AtomicBoolean(false);

        requestWindows.compute(clientKey, (key, deque) -> {
            if (deque == null) {
                deque = new ArrayDeque<>();
            }

            // Remove timestamps outside the 1-minute window
            while (!deque.isEmpty() && deque.peekFirst() <= windowStart) {
                deque.pollFirst();
            }

            if (deque.size() < maxRequests) {
                deque.addLast(now);
                permitted.set(true);
            } else {
                permitted.set(false);
            }

            return deque;
        });

        return permitted.get();
    }

    /**
     * Clears all recorded rate limit windows (useful for unit testing).
     */
    public void reset() {
        requestWindows.clear();
    }
}
