package com.se2030.vaccinationportal.assistant.service;

/**
 * Strategy interface for rate limiting assistant requests.
 * Allows pluggable replacement with a distributed limiter (e.g. Redis) in clustered deployments.
 */
public interface AssistantRateLimiter {

    /**
     * Attempts to acquire a rate limit permit for the given client key.
     *
     * @param clientKey     the unique rate limiting key (e.g. user ID or hashed remote address)
     * @param authenticated true if the client is authenticated, false if anonymous
     * @return true if request is permitted, false if rate limit has been exceeded
     */
    boolean tryAcquire(String clientKey, boolean authenticated);
}
