package com.se2030.vaccinationportal.shared.auth.service;

import com.se2030.vaccinationportal.shared.auth.model.AccountStatus;
import com.se2030.vaccinationportal.shared.auth.model.EmailOtpChallenge;
import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import com.se2030.vaccinationportal.shared.auth.repository.EmailOtpChallengeRepository;
import com.se2030.vaccinationportal.shared.auth.repository.UserAccountRepository;
import com.se2030.vaccinationportal.shared.auth.security.EmailOtpGenerator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Auth-owned application service for citizen email OTP verification.
 * Owns issue, resend, verify, and verification-status reads. SMTP mechanics stay
 * behind {@link EmailOtpDeliveryService} and are dispatched only after the issuing
 * transaction commits ({@link EmailOtpDeliveryDispatcher}), so no email network call
 * can leave account or challenge persistence half-complete.
 * <p>
 * Domain outcomes are returned as results and mapped to structured API errors by the
 * controller layer, so a wrong code persists its durable attempt increment inside the
 * committed transaction instead of being rolled back by an exception.
 * All time flows through the injected application {@link Clock}.
 */
@Service
@Transactional(readOnly = true)
public class EmailVerificationService {

    private final UserAccountRepository userAccountRepository;
    private final EmailOtpChallengeRepository challengeRepository;
    private final EmailOtpGenerator otpGenerator;
    private final PasswordEncoder passwordEncoder;
    private final EmailOtpPolicy policy;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;

    public EmailVerificationService(
            UserAccountRepository userAccountRepository,
            EmailOtpChallengeRepository challengeRepository,
            EmailOtpGenerator otpGenerator,
            PasswordEncoder passwordEncoder,
            EmailOtpPolicy policy,
            Clock clock,
            ApplicationEventPublisher eventPublisher
    ) {
        this.userAccountRepository = userAccountRepository;
        this.challengeRepository = challengeRepository;
        this.otpGenerator = otpGenerator;
        this.passwordEncoder = passwordEncoder;
        this.policy = policy;
        this.clock = clock;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Creates the initial verification challenge for a freshly registered citizen.
     * Must be called inside the registration transaction so account and challenge
     * commit atomically; the email is dispatched after that transaction commits.
     */
    @Transactional
    public void issueInitialChallenge(Long userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        UserAccount account = loadAccount(userId);
        EmailOtpChallenge existing = challengeRepository.findByUser_Id(userId).orElse(null);
        issueChallenge(account, existing, clock.instant());
    }

    /**
     * Verifies the submitted code for the authenticated account. Wrong submissions
     * increment the durable attempt count; exhausting the budget invalidates the
     * challenge (the citizen may resend later). A successful verification consumes
     * the challenge and marks the account email-verified in the same transaction.
     */
    @Transactional
    public EmailOtpVerificationResult verify(Long userId, String submittedOtp) {
        Objects.requireNonNull(userId, "userId must not be null");
        UserAccount account = loadAccount(userId);
        if (account.getEmailVerifiedAt() != null) {
            return EmailOtpVerificationResult.ALREADY_VERIFIED;
        }

        EmailOtpChallenge challenge = challengeRepository.findByUserIdForUpdate(userId).orElse(null);
        if (challenge == null) {
            return EmailOtpVerificationResult.INVALID;
        }
        if (challenge.isInvalidated()) {
            // A locked challenge stays locked; an email-mismatch invalidation reports as invalid.
            return challenge.isAttemptBudgetExhausted(policy.maxAttempts())
                    ? EmailOtpVerificationResult.ATTEMPTS_EXCEEDED
                    : EmailOtpVerificationResult.INVALID;
        }
        if (challenge.isConsumed()) {
            return EmailOtpVerificationResult.INVALID;
        }

        Instant now = clock.instant();
        // Strict boundary: the code is valid only while now < expiresAt; at exactly
        // expiresAt it is already expired.
        if (challenge.isExpired(now)) {
            return EmailOtpVerificationResult.EXPIRED;
        }
        if (challenge.isAttemptBudgetExhausted(policy.maxAttempts())) {
            challenge.invalidate(now);
            return EmailOtpVerificationResult.ATTEMPTS_EXCEEDED;
        }
        // A code can only ever verify the email address it was issued for.
        if (!challenge.targetEmailMatches(account.getEmail())) {
            challenge.invalidate(now);
            return EmailOtpVerificationResult.INVALID;
        }

        String otp = submittedOtp == null ? "" : submittedOtp.trim();
        // Only well-formed six digit submissions count as verification attempts;
        // anything else is rejected without touching the attempt budget.
        if (!otp.matches("\\d{6}")) {
            return EmailOtpVerificationResult.INVALID;
        }
        if (!passwordEncoder.matches(otp, challenge.getCodeHash())) {
            int attempts = challenge.recordFailedAttempt();
            if (attempts >= policy.maxAttempts()) {
                challenge.invalidate(now);
                return EmailOtpVerificationResult.ATTEMPTS_EXCEEDED;
            }
            return EmailOtpVerificationResult.INVALID;
        }

        challenge.markConsumed(now);
        account.setEmailVerifiedAt(now);
        account.setAccountStatus(AccountStatus.ACTIVE);
        return EmailOtpVerificationResult.VERIFIED;
    }

    /**
     * Replaces the current challenge with a freshly issued code subject to the resend
     * cooldown, and dispatches the new email after the transaction commits. Rotation
     * makes the previous code stop working immediately.
     */
    @Transactional
    public EmailOtpResendResult resend(Long userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        UserAccount account = loadAccount(userId);
        if (account.getEmailVerifiedAt() != null) {
            return EmailOtpResendResult.alreadyVerified();
        }

        Instant now = clock.instant();
        EmailOtpChallenge challenge = challengeRepository.findByUserIdForUpdate(userId).orElse(null);
        if (challenge != null) {
            Instant cooldownEndsAt = challenge.getLastSentAt().plus(policy.resendCooldown());
            if (now.isBefore(cooldownEndsAt)) {
                long retryAfterSeconds = Math.max(1, java.time.Duration.between(now, cooldownEndsAt).toSeconds());
                return EmailOtpResendResult.cooldown(retryAfterSeconds);
            }
        }

        issueChallenge(account, challenge, now);
        return EmailOtpResendResult.sent();
    }

    /** Minimal verification state of the authenticated account. */
    public EmailVerificationStatus getStatus(Long userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        UserAccount account = loadAccount(userId);
        return new EmailVerificationStatus(account.getEmailVerifiedAt() != null, account.getEmailVerifiedAt());
    }

    private void issueChallenge(UserAccount account, EmailOtpChallenge existing, Instant now) {
        String otp = otpGenerator.generate();
        String codeHash = passwordEncoder.encode(otp);
        Instant expiresAt = now.plus(policy.codeLifetime());

        if (existing != null) {
            existing.rotate(codeHash, account.getEmail(), now, expiresAt);
        } else {
            challengeRepository.saveAndFlush(new EmailOtpChallenge(account, codeHash, account.getEmail(), now, expiresAt));
        }

        eventPublisher.publishEvent(new EmailOtpDispatchEvent(account.getId(), account.getEmail(), otp, policy.codeLifetime()));
    }

    private UserAccount loadAccount(Long userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Authenticated account no longer exists"));
    }
}
