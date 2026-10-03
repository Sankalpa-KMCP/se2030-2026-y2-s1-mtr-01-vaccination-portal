package com.se2030.vaccinationportal.shared.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;

/**
 * Cohesive policy for the citizen email OTP verification feature.
 * All verification limits are centralized here so no magic numbers are scattered
 * across controllers, services, or tests. Values are externalized through
 * application configuration with the documented defaults:
 * five minute code lifetime, five wrong attempts, and a sixty second resend cooldown.
 */
@Service
public class EmailOtpPolicy {

    private final Duration codeLifetime;
    private final int maxAttempts;
    private final Duration resendCooldown;

    public EmailOtpPolicy(
            @Value("${app.security.auth.otp-ttl-seconds:${AUTH_OTP_TTL_SECONDS:300}}") long codeLifetimeSeconds,
            @Value("${app.security.auth.otp-max-attempts:${AUTH_OTP_MAX_ATTEMPTS:5}}") int maxAttempts,
            @Value("${app.security.auth.otp-resend-cooldown-seconds:${AUTH_OTP_RESEND_COOLDOWN_SECONDS:60}}") long resendCooldownSeconds
    ) {
        if (codeLifetimeSeconds <= 0) {
            throw new IllegalStateException("app.security.auth.otp-ttl-seconds must be positive");
        }
        if (maxAttempts <= 0) {
            throw new IllegalStateException("app.security.auth.otp-max-attempts must be positive");
        }
        if (resendCooldownSeconds < 0) {
            throw new IllegalStateException("app.security.auth.otp-resend-cooldown-seconds must not be negative");
        }
        this.codeLifetime = Duration.ofSeconds(codeLifetimeSeconds);
        this.maxAttempts = maxAttempts;
        this.resendCooldown = Duration.ofSeconds(resendCooldownSeconds);
    }

    /** How long a freshly issued code may be verified before it expires. */
    public Duration codeLifetime() {
        return codeLifetime;
    }

    /** Maximum number of wrong submissions tolerated before the challenge is locked. */
    public int maxAttempts() {
        return maxAttempts;
    }

    /** Minimum interval between two issued codes for the same account. */
    public Duration resendCooldown() {
        return resendCooldown;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmailOtpPolicy that = (EmailOtpPolicy) o;
        return maxAttempts == that.maxAttempts
                && codeLifetime.equals(that.codeLifetime)
                && resendCooldown.equals(that.resendCooldown);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codeLifetime, maxAttempts, resendCooldown);
    }

    @Override
    public String toString() {
        return "EmailOtpPolicy{"
                + "codeLifetime=" + codeLifetime
                + ", maxAttempts=" + maxAttempts
                + ", resendCooldown=" + resendCooldown
                + '}';
    }
}
