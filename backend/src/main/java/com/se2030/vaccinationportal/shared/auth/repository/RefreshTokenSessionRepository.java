package com.se2030.vaccinationportal.shared.auth.repository;

import com.se2030.vaccinationportal.shared.auth.model.RefreshTokenSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenSessionRepository extends JpaRepository<RefreshTokenSession, Long> {

    Optional<RefreshTokenSession> findByTokenHash(String tokenHash);

    List<RefreshTokenSession> findAllByUser_IdAndRevokedAtIsNull(Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE RefreshTokenSession s SET s.revokedAt = :revokedAt WHERE s.user.id = :userId AND s.revokedAt IS NULL")
    int revokeAllActiveSessionsForUser(@Param("userId") Long userId, @Param("revokedAt") Instant revokedAt);

    /**
     * Atomically claims an active refresh session for rotation. Exactly one
     * concurrent request wins (1 updated row); every competing request matches no
     * rows and must treat the presented token as reused.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE RefreshTokenSession s
            SET s.revokedAt = :revokedAt
            WHERE s.id = :id
              AND s.revokedAt IS NULL
              AND s.expiresAt > :now
            """)
    int claimForRotation(@Param("id") Long id, @Param("revokedAt") Instant revokedAt, @Param("now") Instant now);

    /**
     * Revokes every still-active session of a token family (reuse detection and
     * logout semantics).
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE RefreshTokenSession s
            SET s.revokedAt = :revokedAt
            WHERE s.tokenFamilyId = :tokenFamilyId
              AND s.revokedAt IS NULL
            """)
    int revokeActiveSessionsByFamily(@Param("tokenFamilyId") String tokenFamilyId, @Param("revokedAt") Instant revokedAt);

    /**
     * Stamps the first observation of refresh-token reuse on the presented session.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE RefreshTokenSession s
            SET s.reuseDetectedAt = :detectedAt
            WHERE s.id = :id
              AND s.reuseDetectedAt IS NULL
            """)
    int markReuseDetected(@Param("id") Long id, @Param("detectedAt") Instant detectedAt);
}
