package com.se2030.vaccinationportal.shared.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Production-safe default fallback implementation of AuthLinkDeliveryService.
 * Does not expose or log raw authentication action tokens or complete links.
 * Active by default when console-delivery-enabled is false or omitted.
 */
@Service
@ConditionalOnProperty(
        name = "app.security.auth.console-delivery-enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class NoOpAuthLinkDeliveryService implements AuthLinkDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(NoOpAuthLinkDeliveryService.class);

    @Override
    public void sendEmailVerificationLink(String email, String rawToken) {
        log.debug("[AUTH DELIVERY] Console delivery is disabled. Email verification link withheld.");
    }

    @Override
    public void sendPasswordResetLink(String email, String rawToken) {
        log.debug("[AUTH DELIVERY] Console delivery is disabled. Password reset link withheld.");
    }
}
