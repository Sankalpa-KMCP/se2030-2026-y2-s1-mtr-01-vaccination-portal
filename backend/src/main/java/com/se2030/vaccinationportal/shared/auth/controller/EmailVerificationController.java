package com.se2030.vaccinationportal.shared.auth.controller;

import com.se2030.vaccinationportal.shared.auth.dto.EmailVerificationStatusResponse;
import com.se2030.vaccinationportal.shared.auth.dto.VerifyEmailOtpRequest;
import com.se2030.vaccinationportal.shared.auth.exception.EmailAlreadyVerifiedException;
import com.se2030.vaccinationportal.shared.auth.exception.EmailOtpAttemptsExceededException;
import com.se2030.vaccinationportal.shared.auth.exception.EmailOtpExpiredException;
import com.se2030.vaccinationportal.shared.auth.exception.EmailOtpInvalidException;
import com.se2030.vaccinationportal.shared.auth.exception.EmailOtpResendTooSoonException;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import com.se2030.vaccinationportal.shared.auth.service.EmailOtpResendResult;
import com.se2030.vaccinationportal.shared.auth.service.EmailOtpVerificationResult;
import com.se2030.vaccinationportal.shared.auth.service.EmailVerificationService;
import com.se2030.vaccinationportal.shared.auth.service.EmailVerificationStatus;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Principal-scoped citizen email OTP verification endpoints.
 * The verified account is always the authenticated principal's account: no
 * request body ever selects a user, citizen, account, or email address.
 * The transactional service returns domain outcomes; this controller maps them
 * to the structured API error codes so failed-attempt increments stay committed.
 */
@RestController
@RequestMapping("/api/v1/auth/email-verification")
@PreAuthorize("hasRole('CITIZEN')")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    public EmailVerificationController(EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
    }

    @GetMapping("/status")
    public ResponseEntity<EmailVerificationStatusResponse> getStatus(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(toResponse(emailVerificationService.getStatus(requireUserId(principal))));
    }

    @PostMapping("/verify")
    public ResponseEntity<EmailVerificationStatusResponse> verify(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody VerifyEmailOtpRequest request
    ) {
        Long userId = requireUserId(principal);
        EmailOtpVerificationResult result = emailVerificationService.verify(userId, request.otp());
        switch (result) {
            case VERIFIED, ALREADY_VERIFIED -> {
                return ResponseEntity.ok(toResponse(emailVerificationService.getStatus(userId)));
            }
            case INVALID -> throw new EmailOtpInvalidException();
            case EXPIRED -> throw new EmailOtpExpiredException();
            case ATTEMPTS_EXCEEDED -> throw new EmailOtpAttemptsExceededException();
        }
        throw new IllegalStateException("Unhandled verification result: " + result);
    }

    @PostMapping("/resend")
    public ResponseEntity<EmailVerificationStatusResponse> resend(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long userId = requireUserId(principal);
        EmailOtpResendResult result = emailVerificationService.resend(userId);
        switch (result.outcome()) {
            case SENT -> {
                return ResponseEntity.ok(toResponse(emailVerificationService.getStatus(userId)));
            }
            case ALREADY_VERIFIED -> throw new EmailAlreadyVerifiedException();
            case COOLDOWN -> throw new EmailOtpResendTooSoonException(result.retryAfterSeconds());
        }
        throw new IllegalStateException("Unhandled resend outcome: " + result.outcome());
    }

    private static Long requireUserId(UserPrincipal principal) {
        if (principal == null || principal.getUserId() == null) {
            throw new BadCredentialsException("Authenticated citizen identity is unavailable");
        }
        return principal.getUserId();
    }

    private static EmailVerificationStatusResponse toResponse(EmailVerificationStatus status) {
        return new EmailVerificationStatusResponse(status.emailVerified(), status.emailVerifiedAt());
    }
}
