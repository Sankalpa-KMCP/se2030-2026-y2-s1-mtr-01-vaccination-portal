package com.se2030.vaccinationportal.shared.auth.service;

/**
 * Service abstraction for delivering email verification and password reset links.
 * Non-SMTP delivery implementation logs links to the console for development and evaluation.
 */
public interface AuthLinkDeliveryService {

    void sendEmailVerificationLink(String email, String rawToken);

    void sendPasswordResetLink(String email, String rawToken);
}
