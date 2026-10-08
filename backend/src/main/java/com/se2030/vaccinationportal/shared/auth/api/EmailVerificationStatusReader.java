package com.se2030.vaccinationportal.shared.auth.api;

/**
 * Auth-owned contract answering whether an account's email address is verified.
 * Future internal consumers (e.g. Appointment booking enforcement) use this reader;
 * it never exposes Auth entities, repositories, or challenge internals.
 */
public interface EmailVerificationStatusReader {

    boolean isEmailVerified(Long userId);
}
