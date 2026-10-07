package com.se2030.vaccinationportal.shared.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Dispatches the verification code email only after the transaction that issued
 * the OTP challenge has committed. Delivery failures never roll back or delete a
 * committed account or challenge: the account stays created, the challenge stays
 * present, and the citizen may resend later. Failures are logged by failure class
 * only; the plaintext code is never logged.
 */
@Component
public class EmailOtpDeliveryDispatcher {

    private static final Logger log = LoggerFactory.getLogger(EmailOtpDeliveryDispatcher.class);

    private final EmailOtpDeliveryService deliveryService;

    public EmailOtpDeliveryDispatcher(EmailOtpDeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDispatch(EmailOtpDispatchEvent event) {
        try {
            deliveryService.sendVerificationOtp(event.targetEmail(), event.otp(), event.validity());
        } catch (RuntimeException ex) {
            log.error("[AUTH OTP DELIVERY] Verification code email for account [{}] could not be delivered: {}",
                    event.userId(), ex.getClass().getSimpleName());
        }
    }
}
