package com.se2030.vaccinationportal.shared.auth;

import com.se2030.vaccinationportal.shared.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fail-fast contract for the login brute-force lockout policy: invalid values must
 * abort configuration (application startup) instead of silently weakening or
 * disabling the security control. The service constructor is exercised directly, so
 * a rejected value surfaces as an exception before any collaborator is needed.
 */
class LoginLockoutPolicyValidationTests {

    private static final Instant FIXED_NOW = Instant.parse("2026-01-15T10:00:00Z");

    private AuthService authServiceWithPolicy(int maxFailedLoginAttempts, int lockDurationSeconds) {
        return new AuthService(
                null, null, null, null, null, null, null, null, null, null, null, null,
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC),
                24, 30,
                maxFailedLoginAttempts,
                lockDurationSeconds
        );
    }

    @Test
    @DisplayName("zero failed-attempt threshold is rejected")
    void zeroThreshold_rejected() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                authServiceWithPolicy(0, 900));
        assertTrue(exception.getMessage().contains("app.security.auth.login-max-failed-attempts"));
    }

    @Test
    @DisplayName("negative failed-attempt threshold is rejected")
    void negativeThreshold_rejected() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                authServiceWithPolicy(-3, 900));
        assertTrue(exception.getMessage().contains("app.security.auth.login-max-failed-attempts"));
    }

    @Test
    @DisplayName("zero lock duration is rejected")
    void zeroDuration_rejected() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                authServiceWithPolicy(5, 0));
        assertTrue(exception.getMessage().contains("app.security.auth.login-lock-duration-seconds"));
    }

    @Test
    @DisplayName("negative lock duration is rejected")
    void negativeDuration_rejected() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                authServiceWithPolicy(5, -60));
        assertTrue(exception.getMessage().contains("app.security.auth.login-lock-duration-seconds"));
    }

    @Test
    @DisplayName("approved policy defaults (5 attempts, 900 seconds) initialize successfully")
    void approvedDefaults_accepted() {
        assertDoesNotThrow(() -> authServiceWithPolicy(5, 900));
    }
}
