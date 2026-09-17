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
 * Token entity for citizen email verification.
 * Canonical Section 12.2 and Section 17.1.
 * Tokens are single-use, expiring, and stored strictly as SHA-256 hashes.
 */
@Entity
@Table(name = "email_verification_tokens")
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected EmailVerificationToken() {
    }

    public EmailVerificationToken(UserAccount user, String tokenHash, Instant expiresAt) {
        this.user = Objects.requireNonNull(user, "UserAccount must not be null");
        this.tokenHash = Objects.requireNonNull(tokenHash, "Token hash must not be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "ExpiresAt must not be null");
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public boolean isConsumed() {
        return this.usedAt != null;
    }

    public boolean isExpired(Instant now) {
        return now != null && now.isAfter(this.expiresAt);
    }

    public boolean isValid(Instant now) {
        return !isConsumed() && !isExpired(now);
    }

    public void markUsed(Instant usedAt) {
        this.usedAt = usedAt != null ? usedAt : Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UserAccount getUser() {
        return user;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
