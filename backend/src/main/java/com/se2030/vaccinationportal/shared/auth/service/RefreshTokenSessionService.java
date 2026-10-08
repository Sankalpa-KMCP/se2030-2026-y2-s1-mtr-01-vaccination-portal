package com.se2030.vaccinationportal.shared.auth.service;

import com.se2030.vaccinationportal.shared.auth.exception.AccountPendingApprovalException;
import com.se2030.vaccinationportal.shared.auth.exception.AccountRejectedException;
import com.se2030.vaccinationportal.shared.auth.model.AccountStatus;
import com.se2030.vaccinationportal.shared.auth.model.RefreshTokenSession;
import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import com.se2030.vaccinationportal.shared.auth.repository.RefreshTokenSessionRepository;
import com.se2030.vaccinationportal.shared.auth.repository.UserAccountRepository;
import com.se2030.vaccinationportal.shared.auth.security.AuthActionTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

/**
 * Canonical §17.1 refresh-token session mechanics: opaque single-use refresh
 * tokens issued at login, rotated on every use, grouped into families, stored
 * only as SHA-256 hashes, and protected by reuse detection that revokes the
 * affected family. Session bookkeeping is clock-driven and the rotation claim
 * is an atomic conditional update, so exactly one concurrent request can rotate
 * a given token; every competing presentation is treated as reuse.
 */
@Service
public class RefreshTokenSessionService {

    private static final int REFRESH_TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenSessionRepository refreshTokenSessionRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuthActionTokenService authActionTokenService;
    private final Clock clock;
    private final int refreshTokenExpirationDays;

    public RefreshTokenSessionService(
            RefreshTokenSessionRepository refreshTokenSessionRepository,
            UserAccountRepository userAccountRepository,
            AuthActionTokenService authActionTokenService,
            Clock clock,
            @Value("${app.security.auth.refresh-token-expiration-days:${AUTH_REFRESH_TOKEN_EXPIRATION_DAYS:7}}")
            int refreshTokenExpirationDays
    ) {
        if (refreshTokenExpirationDays < 1) {
            throw new IllegalArgumentException(
                    "app.security.auth.refresh-token-expiration-days must be at least 1 (configured value: "
                            + refreshTokenExpirationDays + ")");
        }
        this.refreshTokenSessionRepository = refreshTokenSessionRepository;
        this.userAccountRepository = userAccountRepository;
        this.authActionTokenService = authActionTokenService;
        this.clock = clock;
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
    }

    public record IssuedRefreshToken(String rawToken, Instant expiresAt) {}

    public record RotatedRefreshSession(UserAccount account, String rawToken, Instant expiresAt) {}

    /**
     * Creates the root session of a new token family for a freshly authenticated
     * account. Only the SHA-256 hash of the opaque token is persisted.
     */
    public IssuedRefreshToken issueRootSession(UserAccount account) {
        Objects.requireNonNull(account, "UserAccount must not be null");
        Instant now = clock.instant();
        String rawToken = generateToken();
        RefreshTokenSession session = new RefreshTokenSession(
                account,
                UUID.randomUUID().toString(),
                authActionTokenService.hashToken(rawToken),
                expiry(now),
                now
        );
        refreshTokenSessionRepository.save(session);
        return new IssuedRefreshToken(rawToken, session.getExpiresAt());
    }

    /**
     * Validates the presented refresh token, claims it for rotation, and mints the
     * successor session in the same family. Reuse of a rotated/revoked token (or
     * losing a concurrent rotation race) revokes the entire family and is rejected.
     * Expired or unknown tokens are rejected without state changes.
     */
    @Transactional(noRollbackFor = BadCredentialsException.class)
    public RotatedRefreshSession rotate(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        Instant now = clock.instant();
        RefreshTokenSession session = refreshTokenSessionRepository
                .findByTokenHash(authActionTokenService.hashToken(rawRefreshToken))
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (session.isRevoked()) {
            markReuseAndRevokeFamily(session, now);
            throw new BadCredentialsException("Invalid refresh token");
        }
        if (!session.getExpiresAt().isAfter(now)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        UserAccount account = userAccountRepository.findById(session.getUser().getId())
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        assertLoginEligible(account);

        if (refreshTokenSessionRepository.claimForRotation(session.getId(), now, now) != 1) {
            // A competing request won the rotation race: approved reuse semantics.
            markReuseAndRevokeFamily(session, now);
            throw new BadCredentialsException("Invalid refresh token");
        }

        String rawToken = generateToken();
        RefreshTokenSession successor = new RefreshTokenSession(
                account,
                session.getTokenFamilyId(),
                authActionTokenService.hashToken(rawToken),
                expiry(now),
                now
        );
        successor.setRotatedFromId(session.getId());
        refreshTokenSessionRepository.save(successor);
        return new RotatedRefreshSession(account, rawToken, successor.getExpiresAt());
    }

    /**
     * Revokes the family the presented refresh token belongs to. Unknown or blank
     * tokens are a no-op so logout stays idempotent and non-enumerating.
     */
    @Transactional
    public void revokeFamilyByRawToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenSessionRepository
                .findByTokenHash(authActionTokenService.hashToken(rawRefreshToken))
                .ifPresent(session -> refreshTokenSessionRepository.revokeActiveSessionsByFamily(
                        session.getTokenFamilyId(), clock.instant()));
    }

    private void markReuseAndRevokeFamily(RefreshTokenSession session, Instant now) {
        refreshTokenSessionRepository.markReuseDetected(session.getId(), now);
        refreshTokenSessionRepository.revokeActiveSessionsByFamily(session.getTokenFamilyId(), now);
    }

    private void assertLoginEligible(UserAccount account) {
        if (account.getAccountStatus() == AccountStatus.PENDING_APPROVAL) {
            throw new AccountPendingApprovalException();
        }
        if (account.getAccountStatus() == AccountStatus.REJECTED) {
            throw new AccountRejectedException();
        }
        if (account.getAccountStatus() == AccountStatus.LOCKED
                || (account.getLockedUntil() != null && clock.instant().isBefore(account.getLockedUntil()))) {
            throw new LockedException("Account is temporarily locked");
        }
        if (account.getAccountStatus() != AccountStatus.ACTIVE
                && account.getAccountStatus() != AccountStatus.PENDING_VERIFICATION) {
            throw new DisabledException("Account is not active for login");
        }
    }

    private Instant expiry(Instant now) {
        return now.plus(Duration.ofDays(refreshTokenExpirationDays));
    }

    private String generateToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
