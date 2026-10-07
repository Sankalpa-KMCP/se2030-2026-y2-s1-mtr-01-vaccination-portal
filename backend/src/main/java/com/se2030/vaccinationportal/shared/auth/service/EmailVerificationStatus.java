package com.se2030.vaccinationportal.shared.auth.service;

import java.time.Instant;

/**
 * Minimal email verification state of one account.
 *
 * @param emailVerified    true when the account has a verification timestamp
 * @param emailVerifiedAt  verification timestamp, null while unverified
 */
public record EmailVerificationStatus(boolean emailVerified, Instant emailVerifiedAt) {
}
