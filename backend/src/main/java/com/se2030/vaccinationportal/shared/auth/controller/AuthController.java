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
import com.se2030.vaccinationportal.shared.auth.exception.AccountPendingApprovalException;
import com.se2030.vaccinationportal.shared.auth.exception.AccountRejectedException;
import com.se2030.vaccinationportal.shared.auth.exception.AuthEmailDeliveryException;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import com.se2030.vaccinationportal.shared.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final String refreshCookieName;
    private final boolean refreshCookieSecure;
    private final long refreshCookieMaxAgeSeconds;

    public AuthController(
            AuthService authService,
            @Value("${app.security.auth.refresh-cookie-name:${AUTH_REFRESH_COOKIE_NAME:refresh_token}}") String refreshCookieName,
            @Value("${app.security.auth.refresh-cookie-secure:${AUTH_REFRESH_COOKIE_SECURE:true}}") boolean refreshCookieSecure,
            @Value("${app.security.auth.refresh-token-expiration-days:${AUTH_REFRESH_TOKEN_EXPIRATION_DAYS:7}}") int refreshTokenExpirationDays
    ) {
        this.authService = authService;
        this.refreshCookieName = refreshCookieName;
        this.refreshCookieSecure = refreshCookieSecure;
        this.refreshCookieMaxAgeSeconds = (long) refreshTokenExpirationDays * 24L * 60L * 60L;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthService.LoginResult result = authService.login(request);
        setRefreshCookie(response, result.refreshToken(), refreshCookieMaxAgeSeconds);
        return ResponseEntity.ok(result.response());
    }

    /**
     * Canonical §17.1 single-use rotation: the presented refresh cookie is consumed
     * and replaced. Reuse of a rotated/revoked token revokes the whole family.
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        try {
            AuthService.RefreshSessionResult result = authService.refresh(extractRefreshCookie(request));
            setRefreshCookie(response, result.refreshToken(), refreshCookieMaxAgeSeconds);
            return ResponseEntity.ok(result.response());
        } catch (BadCredentialsException | LockedException | DisabledException
                | AccountPendingApprovalException | AccountRejectedException ex) {
            clearRefreshCookie(response);
            throw ex;
        }
    }

    /**
     * Revokes the presented token family and clears the cookie. Idempotent: missing
     * or unknown cookies produce the same outward response without disclosure.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(extractRefreshCookie(request));
        clearRefreshCookie(response);
        return ResponseEntity.noContent().build();
    }

    private String extractRefreshCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (refreshCookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void setRefreshCookie(HttpServletResponse response, String rawRefreshToken, long maxAgeSeconds) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                RefreshTokenCookieSupport.build(refreshCookieName, rawRefreshToken, maxAgeSeconds, refreshCookieSecure).toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                RefreshTokenCookieSupport.clear(refreshCookieName, refreshCookieSecure).toString());
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
