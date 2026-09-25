package com.se2030.vaccinationportal.assistant.service;

import com.se2030.vaccinationportal.assistant.config.AssistantProperties;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory sliding window rate limiter implementation with bounded capacity and proactive key cleanup.
 * Enforces per-minute quotas for anonymous vs authenticated actors.
 *
 * <p>NOTE: This rate limiter stores window states purely in JVM heap memory and is suitable only
 * for single-instance application deployments. Clustered deployments must replace this bean with a
 * distributed provider (e.g. Redis / Token Bucket cluster) to coordinate rate limits across multiple nodes.
 */
@Component
public class InMemoryAssistantRateLimiter implements AssistantRateLimiter {

    public static final int MAX_TRACKED_CLIENTS = 10_000;
    private static final long WINDOW_DURATION_MS = 60_000L;
    private static final int CLEANUP_INTERVAL_REQUESTS = 100;

    private final AssistantProperties properties;
    private final Clock clock;
    private final ConcurrentHashMap<String, Deque<Long>> requestWindows = new ConcurrentHashMap<>();
    private final AtomicLong requestCounter = new AtomicLong(0);

    @org.springframework.beans.factory.annotation.Autowired
    public InMemoryAssistantRateLimiter(AssistantProperties properties) {
        this(properties, Clock.systemUTC());
    }

    public InMemoryAssistantRateLimiter(AssistantProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    @Override
    public boolean tryAcquire(String clientKey, boolean authenticated) {
        if (clientKey == null || clientKey.isBlank()) {
            return false;
        }

        long now = clock.millis();
        long windowStart = now - WINDOW_DURATION_MS;

        // Trigger periodic or opportunistic cleanup of expired entries
        if (requestCounter.incrementAndGet() % CLEANUP_INTERVAL_REQUESTS == 0
                || requestWindows.size() > (MAX_TRACKED_CLIENTS / 2)) {
            cleanupExpiredKeys(windowStart);
        }

        // Bounded capacity check: prevent unbounded map expansion under distributed attack
        if (requestWindows.size() >= MAX_TRACKED_CLIENTS && !requestWindows.containsKey(clientKey)) {
            // Fail closed when client tracking capacity is exhausted
            return false;
        }

        int maxRequests = authenticated
                ? properties.getRateLimit().getAuthenticatedPerMinute()
                : properties.getRateLimit().getAnonymousPerMinute();

        AtomicBoolean permitted = new AtomicBoolean(false);

        requestWindows.compute(clientKey, (key, deque) -> {
            if (deque == null) {
                deque = new ArrayDeque<>();
            }

            // Remove timestamps older than the sliding 1-minute window
            while (!deque.isEmpty() && deque.peekFirst() <= windowStart) {
                deque.pollFirst();
            }

            if (deque.size() < maxRequests) {
                deque.addLast(now);
                permitted.set(true);
                return deque;
            }

            // Quota exceeded
            permitted.set(false);
            return deque;
        });

        return permitted.get();
    }

    /**
     * Scans and purges inactive client entries where all timestamps have expired.
     */
    public void cleanupExpiredKeys(long windowStart) {
        requestWindows.keySet().forEach(key ->
                requestWindows.computeIfPresent(key, (k, deque) -> {
                    while (!deque.isEmpty() && deque.peekFirst() <= windowStart) {
                        deque.pollFirst();
                    }
                    return deque.isEmpty() ? null : deque;
                })
        );
    }

    /**
     * Purges all tracked client entries with window boundary derived from current clock time.
     */
    public void cleanup() {
        cleanupExpiredKeys(clock.millis() - WINDOW_DURATION_MS);
    }

    /**
     * Returns the count of active tracked client keys.
     */
    public int getTrackedKeyCount() {
        return requestWindows.size();
    }

    /**
     * Resets all internal rate-limiting tracking (strictly for test fixture isolation).
     */
    public void reset() {
        requestWindows.clear();
        requestCounter.set(0);
    }
}
