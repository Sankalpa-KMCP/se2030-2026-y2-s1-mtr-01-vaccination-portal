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
 * Entity mapped to canonical refresh_token_sessions table (V4 migration).
 * Used for session revocation upon password reset (Canonical Section 17.1).
 */
@Entity
@Table(name = "refresh_token_sessions")
public class RefreshTokenSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(name = "token_family_id", nullable = false, length = 64)
    private String tokenFamilyId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    @Column(name = "rotated_from_id")
    private Long rotatedFromId;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "reuse_detected_at")
    private Instant reuseDetectedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RefreshTokenSession() {
    }

    public RefreshTokenSession(UserAccount user, String tokenFamilyId, String tokenHash, Instant expiresAt) {
        this.user = Objects.requireNonNull(user, "UserAccount must not be null");
        this.tokenFamilyId = Objects.requireNonNull(tokenFamilyId, "Token family ID must not be null");
        this.tokenHash = Objects.requireNonNull(tokenHash, "Token hash must not be null");
        this.issuedAt = Instant.now();
        this.expiresAt = Objects.requireNonNull(expiresAt, "ExpiresAt must not be null");
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.issuedAt == null) {
            this.issuedAt = Instant.now();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public void revoke(Instant revokedAt) {
        this.revokedAt = revokedAt != null ? revokedAt : Instant.now();
    }

    public boolean isRevoked() {
        return this.revokedAt != null;
    }

    public Long getId() {
        return id;
    }

    public UserAccount getUser() {
        return user;
    }

    public String getTokenFamilyId() {
        return tokenFamilyId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Long getRotatedFromId() {
        return rotatedFromId;
    }

    public void setRotatedFromId(Long rotatedFromId) {
        this.rotatedFromId = rotatedFromId;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public Instant getReuseDetectedAt() {
        return reuseDetectedAt;
    }

    public void setReuseDetectedAt(Instant reuseDetectedAt) {
        this.reuseDetectedAt = reuseDetectedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
