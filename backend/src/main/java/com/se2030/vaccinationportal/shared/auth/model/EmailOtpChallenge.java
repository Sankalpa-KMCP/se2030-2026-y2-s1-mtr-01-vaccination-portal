package com.se2030.vaccinationportal.shared.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

/**
 * Current email OTP verification challenge for one citizen account.
 * Canonical Section 12.2 and Section 17.1 (email verification).
 * Exactly one challenge row exists per account; a resend rotates the row in place,
 * so the previous code stops working immediately. Codes are stored strictly as
 * password-hashes, never plaintext, and every challenge is bound to the target
 * email address it verifies.
 * This persistence type is internal to Auth: consumers must use
 * {@code EmailVerificationStatusReader} instead.
 */
@Entity
@Table(name = "email_otp_challenges")
public class EmailOtpChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserAccount user;

    @Column(name = "code_hash", nullable = false, length = 255)
    private String codeHash;

    @Column(name = "target_email", nullable = false, length = 255)
    private String targetEmail;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "invalidated_at")
    private Instant invalidatedAt;

    @Column(name = "last_sent_at", nullable = false)
    private Instant lastSentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected EmailOtpChallenge() {
    }

    public EmailOtpChallenge(UserAccount user, String codeHash, String targetEmail, Instant now, Instant expiresAt) {
        this.user = Objects.requireNonNull(user, "UserAccount must not be null");
        this.codeHash = Objects.requireNonNull(codeHash, "Code hash must not be null");
        this.targetEmail = Objects.requireNonNull(targetEmail, "Target email must not be null");
        this.issuedAt = Objects.requireNonNull(now, "IssuedAt must not be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "ExpiresAt must not be null");
        this.attemptCount = 0;
        this.lastSentAt = now;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    /**
     * Rotates this challenge in place so a resend replaces the previous code:
     * the old hash, target email, expiry, and attempt budget are all reset.
     */
    public void rotate(String newCodeHash, String newTargetEmail, Instant now, Instant newExpiresAt) {
        this.codeHash = Objects.requireNonNull(newCodeHash, "Code hash must not be null");
        this.targetEmail = Objects.requireNonNull(newTargetEmail, "Target email must not be null");
        this.issuedAt = now;
        this.expiresAt = Objects.requireNonNull(newExpiresAt, "ExpiresAt must not be null");
        this.attemptCount = 0;
        this.consumedAt = null;
        this.invalidatedAt = null;
        this.lastSentAt = now;
    }

    public boolean isConsumed() {
        return this.consumedAt != null;
    }

    public boolean isInvalidated() {
        return this.invalidatedAt != null;
    }

    /**
     * The code is valid only while {@code now < expiresAt}; at exactly {@code expiresAt}
     * the challenge is already expired.
     */
    public boolean isExpired(Instant now) {
        return now == null || !now.isBefore(this.expiresAt);
    }

    public boolean isAttemptBudgetExhausted(int maxAttempts) {
        return this.attemptCount >= maxAttempts;
    }

    public void markConsumed(Instant consumedAt) {
        this.consumedAt = consumedAt != null ? consumedAt : Instant.now();
    }

    public void invalidate(Instant now) {
        if (this.invalidatedAt == null) {
            this.invalidatedAt = now != null ? now : Instant.now();
        }
    }

    /** Records one failed verification attempt and returns the new attempt count. */
    public int recordFailedAttempt() {
        this.attemptCount = this.attemptCount + 1;
        return this.attemptCount;
    }

    public boolean targetEmailMatches(String email) {
        return email != null && this.targetEmail.equals(email.trim().toLowerCase(java.util.Locale.ROOT));
    }

    public Long getId() {
        return id;
    }

    public UserAccount getUser() {
        return user;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public String getTargetEmail() {
        return targetEmail;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public Instant getConsumedAt() {
        return consumedAt;
    }

    public Instant getInvalidatedAt() {
        return invalidatedAt;
    }

    public Instant getLastSentAt() {
        return lastSentAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
