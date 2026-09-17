package com.se2030.vaccinationportal.shared.auth.controller;

import com.se2030.vaccinationportal.shared.auth.dto.CitizenRegistrationRequest;
import com.se2030.vaccinationportal.shared.auth.dto.ForgotPasswordRequest;
import com.se2030.vaccinationportal.shared.auth.dto.LoginRequest;
import com.se2030.vaccinationportal.shared.auth.dto.LoginResponse;
import com.se2030.vaccinationportal.shared.auth.dto.MessageResponse;
import com.se2030.vaccinationportal.shared.auth.dto.RegistrationResponse;
import com.se2030.vaccinationportal.shared.auth.dto.ResendVerificationRequest;
import com.se2030.vaccinationportal.shared.auth.dto.ResetPasswordRequest;
import com.se2030.vaccinationportal.shared.auth.dto.StaffRegistrationRequest;
import com.se2030.vaccinationportal.shared.auth.dto.UserProfileResponse;
import com.se2030.vaccinationportal.shared.auth.dto.VerifyEmailRequest;
import com.se2030.vaccinationportal.shared.auth.exception.AuthEmailDeliveryException;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import com.se2030.vaccinationportal.shared.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register/citizen")
    public ResponseEntity<RegistrationResponse> registerCitizen(
            @Valid @RequestBody CitizenRegistrationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerCitizen(request));
    }

    @PostMapping("/register/staff")
    public ResponseEntity<RegistrationResponse> registerStaff(
            @Valid @RequestBody StaffRegistrationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(authService.registerStaffRequest(request));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<MessageResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return ResponseEntity.ok(authService.verifyEmail(request));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<MessageResponse> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        try {
            return ResponseEntity.ok(authService.resendVerification(request));
        } catch (AuthEmailDeliveryException ex) {
            return ResponseEntity.ok(new MessageResponse(AuthService.GENERIC_RESEND_VERIFICATION_MESSAGE));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            return ResponseEntity.ok(authService.forgotPassword(request));
        } catch (AuthEmailDeliveryException ex) {
            return ResponseEntity.ok(new MessageResponse(AuthService.GENERIC_FORGOT_PASSWORD_MESSAGE));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserProfileResponse response = authService.getCurrentUserProfile(principal);
        return ResponseEntity.ok(response);
    }
}
