package com.se2030.vaccinationportal.shared.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Bounded per-account issuance cooldown for public authentication link flows (R06 / F2-ALIAS).
 * <p>
 * Forgot-password and resend-verification requests repeatedly invalidate outstanding links, mint
 * fresh tokens, and dispatch delivery events. This throttle enforces a bounded cooldown per
 * authoritative account identity (immutable {@code user.getId()}) so repeated public requests cannot
 * churn tokens or delivery attempts, while a single legitimate request always passes.
 * </p>
 * <p>
 * Design notes:
 * <ul>
 *   <li>The throttle key is the immutable primary key of the resolved user account ({@code user.getId()}),
 *       applied to existing accounts in {@link AuthService}. This guarantees that multiple database-equivalent
 *       spellings, collation aliases (e.g. case, accents, Unicode under {@code utf8mb4_unicode_ci}), or
 *       alternate lookup routes resolving to the same account map to exactly one throttle identity and
 *       cannot bypass cooldown (F2-ALIAS), while nonexistent identities never consume finite throttle
 *       slots (F2-DOS).</li>
 *   <li>Both public link flows share one key space: alternating between them does not bypass the
 *       cooldown.</li>
 *   <li>State is in-memory and per-instance, mirroring the existing in-memory assistant rate
 *       limiter; a clustered deployment would replace this with a distributed limiter.</li>
 *   <li>Memory is strictly bounded to {@code maxTrackedKeys}: expired entries are purged from the
 *       head in O(1) time. When capacity is saturated with active entries under hostile flooding,
 *       new identities are rejected admission rather than evicting active keys, protecting active
 *       cooldowns against flood-eviction bypass attacks (F2).</li>
 * </ul>
 */
@Service
public class AuthLinkIssuanceThrottle {

    static final int MAX_TRACKED_KEYS = 10_000;

    private final Duration cooldown;
    private final int maxTrackedKeys;
    private final Object lock = new Object();
    private final LinkedHashMap<Long, Instant> lastIssuanceByAccountId;

    @Autowired
    public AuthLinkIssuanceThrottle(
            @Value("${app.security.auth.link-issuance-cooldown-seconds:${AUTH_LINK_ISSUANCE_COOLDOWN_SECONDS:60}}")
            long cooldownSeconds
    ) {
        this(cooldownSeconds, MAX_TRACKED_KEYS);
    }

    AuthLinkIssuanceThrottle(long cooldownSeconds, int maxTrackedKeys) {
        this.cooldown = Duration.ofSeconds(Math.max(0, cooldownSeconds));
        this.maxTrackedKeys = Math.max(1, maxTrackedKeys);
        this.lastIssuanceByAccountId = new LinkedHashMap<>(16, 0.75f, false);
    }

    /**
     * Attempts to record an issuance attempt for the given resolved account identity.
     *
     * @param accountId immutable persisted primary key of the resolved user account
     * @param now       current instant
     * @return {@code true} if the caller may proceed with token rotation and delivery; {@code false}
     *         if the account is still inside its cooldown window
     */
    public boolean tryRecordIssuance(Long accountId, Instant now) {
        if (accountId == null) {
            return true;
        }
        synchronized (lock) {
            purgeExpiredHead(now);
            Instant lastIssuance = lastIssuanceByAccountId.get(accountId);
            if (lastIssuance != null) {
                if (now.isBefore(lastIssuance.plus(cooldown))) {
                    return false;
                }
                // Cooldown expired for this existing key: refresh to tail
                lastIssuanceByAccountId.remove(accountId);
                lastIssuanceByAccountId.put(accountId, now);
                return true;
            }

            // New identity: if capacity is full of active cooldown entries, reject admission
            // without inserting and without evicting active entries (F2 flood attack protection)
            if (lastIssuanceByAccountId.size() >= maxTrackedKeys) {
                return false;
            }

            lastIssuanceByAccountId.put(accountId, now);
            return true;
        }
    }

    public boolean tryRecordIssuance(Long accountId) {
        return tryRecordIssuance(accountId, Instant.now());
    }

    /**
     * Remaining cooldown for the given account identity, for diagnostics only. Never exposed publicly.
     */
    public Duration remainingCooldown(Long accountId, Instant now) {
        if (accountId == null) {
            return Duration.ZERO;
        }
        synchronized (lock) {
            Instant lastIssuance = lastIssuanceByAccountId.get(accountId);
            if (lastIssuance == null) {
                return Duration.ZERO;
            }
            Instant expiresAt = lastIssuance.plus(cooldown);
            return now.isBefore(expiresAt) ? Duration.between(now, expiresAt) : Duration.ZERO;
        }
    }

    public Duration remainingCooldown(Long accountId) {
        return remainingCooldown(accountId, Instant.now());
    }

    public int trackedKeyCount() {
        synchronized (lock) {
            return lastIssuanceByAccountId.size();
        }
    }

    private void purgeExpiredHead(Instant now) {
        Iterator<Map.Entry<Long, Instant>> it = lastIssuanceByAccountId.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, Instant> entry = it.next();
            if (!now.isBefore(entry.getValue().plus(cooldown))) {
                it.remove();
            } else {
                break;
            }
        }
    }
}
