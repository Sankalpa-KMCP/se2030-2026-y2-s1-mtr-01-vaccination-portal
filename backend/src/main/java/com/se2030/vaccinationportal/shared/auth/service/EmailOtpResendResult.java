package com.se2030.vaccinationportal.shared.auth.service;

/**
 * Result of a principal-scoped resend request.
 *
 * @param outcome            what happened to the request
 * @param retryAfterSeconds  seconds until a resend is allowed; positive for COOLDOWN, 0 otherwise
 */
public record EmailOtpResendResult(Outcome outcome, long retryAfterSeconds) {

    public enum Outcome { SENT, ALREADY_VERIFIED, COOLDOWN }

    public static EmailOtpResendResult sent() {
        return new EmailOtpResendResult(Outcome.SENT, 0);
    }

    public static EmailOtpResendResult alreadyVerified() {
        return new EmailOtpResendResult(Outcome.ALREADY_VERIFIED, 0);
    }

    public static EmailOtpResendResult cooldown(long retryAfterSeconds) {
        return new EmailOtpResendResult(Outcome.COOLDOWN, retryAfterSeconds);
    }
}
