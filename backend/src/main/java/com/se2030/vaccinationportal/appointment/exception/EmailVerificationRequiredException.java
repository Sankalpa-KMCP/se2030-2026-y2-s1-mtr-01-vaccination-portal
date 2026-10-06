package com.se2030.vaccinationportal.appointment.exception;

/**
 * Appointment-owned business rejection raised when a citizen without a verified email
 * attempts to acquire an appointment (direct booking or waitlist join).
 * Auth owns the verification state via its public status contract; Appointment owns the
 * rule that a verified email is required to acquire an appointment.
 */
public class EmailVerificationRequiredException extends RuntimeException {
    public EmailVerificationRequiredException() {
        super("Verify your email before booking or joining a waitlist.");
    }
}
