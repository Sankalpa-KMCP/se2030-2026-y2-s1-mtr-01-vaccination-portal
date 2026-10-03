package com.se2030.vaccinationportal.shared.auth.controller;

import com.se2030.vaccinationportal.shared.auth.dto.AuthErrorResponse;
import com.se2030.vaccinationportal.shared.auth.exception.AccountPendingApprovalException;
import com.se2030.vaccinationportal.shared.auth.exception.AccountPendingVerificationException;
import com.se2030.vaccinationportal.shared.auth.exception.AccountRejectedException;
import com.se2030.vaccinationportal.shared.auth.exception.DuplicateEmailException;
import com.se2030.vaccinationportal.shared.auth.exception.EmailAlreadyVerifiedException;
import com.se2030.vaccinationportal.shared.auth.exception.EmailOtpAttemptsExceededException;
import com.se2030.vaccinationportal.shared.auth.exception.EmailOtpExpiredException;
import com.se2030.vaccinationportal.shared.auth.exception.EmailOtpInvalidException;
import com.se2030.vaccinationportal.shared.auth.exception.EmailOtpResendTooSoonException;
import com.se2030.vaccinationportal.shared.auth.exception.InvalidAuthActionTokenException;
import com.se2030.vaccinationportal.shared.auth.exception.InvalidApprovalRoleException;
import com.se2030.vaccinationportal.shared.auth.exception.InvalidRejectionReasonException;
import com.se2030.vaccinationportal.shared.auth.exception.InvalidRequestedRoleException;
import com.se2030.vaccinationportal.shared.auth.exception.StaffRequestNotFoundException;
import com.se2030.vaccinationportal.shared.auth.exception.StaffRequestNotPendingException;
import com.se2030.vaccinationportal.shared.auth.exception.AuthEmailDeliveryException;
import com.se2030.vaccinationportal.shared.auth.exception.TokenAlreadyUsedException;
import com.se2030.vaccinationportal.shared.auth.exception.TokenExpiredException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.se2030.vaccinationportal.shared.auth.controller")
public class AuthExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AuthErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.computeIfAbsent(fieldError.getField(), ignored -> new ArrayList<>())
                    .add(fieldError.getDefaultMessage());
        }
        return response(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Input payload validation failed",
                request,
                fieldErrors
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<AuthErrorResponse> handleUnreadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Malformed authentication request payload", request, null);
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<AuthErrorResponse> handleDuplicateEmail(
            DuplicateEmailException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", exception.getMessage(), request, null);
    }

    @ExceptionHandler(InvalidRequestedRoleException.class)
    public ResponseEntity<AuthErrorResponse> handleInvalidRequestedRole(
            InvalidRequestedRoleException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_REQUESTED_ROLE", exception.getMessage(), request, null);
    }

    @ExceptionHandler(InvalidApprovalRoleException.class)
    public ResponseEntity<AuthErrorResponse> handleInvalidApprovalRole(
            InvalidApprovalRoleException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_APPROVAL_ROLE", exception.getMessage(), request, null);
    }

    @ExceptionHandler(InvalidRejectionReasonException.class)
    public ResponseEntity<AuthErrorResponse> handleInvalidRejectionReason(
            InvalidRejectionReasonException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_REJECTION_REASON", exception.getMessage(), request, null);
    }

    @ExceptionHandler(StaffRequestNotFoundException.class)
    public ResponseEntity<AuthErrorResponse> handleStaffRequestNotFound(
            StaffRequestNotFoundException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.NOT_FOUND, "STAFF_REQUEST_NOT_FOUND", exception.getMessage(), request, null);
    }

    @ExceptionHandler(StaffRequestNotPendingException.class)
    public ResponseEntity<AuthErrorResponse> handleStaffRequestNotPending(
            StaffRequestNotPendingException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.CONFLICT, "STAFF_REQUEST_NOT_PENDING", exception.getMessage(), request, null);
    }

    @ExceptionHandler(AccountPendingApprovalException.class)
    public ResponseEntity<AuthErrorResponse> handlePendingApproval(
            AccountPendingApprovalException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.FORBIDDEN, "ACCOUNT_PENDING_APPROVAL", exception.getMessage(), request, null);
    }

    @ExceptionHandler(AccountPendingVerificationException.class)
    public ResponseEntity<AuthErrorResponse> handlePendingVerification(
            AccountPendingVerificationException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.FORBIDDEN, "ACCOUNT_PENDING_VERIFICATION", exception.getMessage(), request, null);
    }

    @ExceptionHandler(AccountRejectedException.class)
    public ResponseEntity<AuthErrorResponse> handleRejected(
            AccountRejectedException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.FORBIDDEN, "ACCOUNT_REJECTED", exception.getMessage(), request, null);
    }

    @ExceptionHandler(InvalidAuthActionTokenException.class)
    public ResponseEntity<AuthErrorResponse> handleInvalidToken(
            InvalidAuthActionTokenException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_TOKEN", exception.getMessage(), request, null);
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<AuthErrorResponse> handleTokenExpired(
            TokenExpiredException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.BAD_REQUEST, "TOKEN_EXPIRED", exception.getMessage(), request, null);
    }

    @ExceptionHandler(TokenAlreadyUsedException.class)
    public ResponseEntity<AuthErrorResponse> handleTokenAlreadyUsed(
            TokenAlreadyUsedException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.BAD_REQUEST, "TOKEN_ALREADY_USED", exception.getMessage(), request, null);
    }

    @ExceptionHandler(AuthEmailDeliveryException.class)
    public ResponseEntity<AuthErrorResponse> handleEmailDeliveryFailure(
            AuthEmailDeliveryException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.SERVICE_UNAVAILABLE,
                "EMAIL_DELIVERY_FAILED",
                "Email delivery service is currently unavailable. Please try again later.",
                request,
                null
        );
    }

    @ExceptionHandler(EmailOtpInvalidException.class)
    public ResponseEntity<AuthErrorResponse> handleEmailOtpInvalid(
            EmailOtpInvalidException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.BAD_REQUEST, "EMAIL_VERIFICATION_INVALID_CODE", exception.getMessage(), request, null);
    }

    @ExceptionHandler(EmailOtpExpiredException.class)
    public ResponseEntity<AuthErrorResponse> handleEmailOtpExpired(
            EmailOtpExpiredException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.BAD_REQUEST, "EMAIL_VERIFICATION_CODE_EXPIRED", exception.getMessage(), request, null);
    }

    @ExceptionHandler(EmailOtpAttemptsExceededException.class)
    public ResponseEntity<AuthErrorResponse> handleEmailOtpAttemptsExceeded(
            EmailOtpAttemptsExceededException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.BAD_REQUEST, "EMAIL_VERIFICATION_ATTEMPTS_EXCEEDED", exception.getMessage(), request, null);
    }

    @ExceptionHandler(EmailOtpResendTooSoonException.class)
    public ResponseEntity<AuthErrorResponse> handleEmailOtpResendTooSoon(
            EmailOtpResendTooSoonException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.TOO_MANY_REQUESTS,
                "EMAIL_VERIFICATION_RESEND_TOO_SOON",
                exception.getMessage(),
                request,
                null,
                (int) exception.getRetryAfterSeconds()
        );
    }

    @ExceptionHandler(EmailAlreadyVerifiedException.class)
    public ResponseEntity<AuthErrorResponse> handleEmailAlreadyVerified(
            EmailAlreadyVerifiedException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.CONFLICT, "EMAIL_ALREADY_VERIFIED", exception.getMessage(), request, null);
    }

    private ResponseEntity<AuthErrorResponse> response(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request,
            Map<String, List<String>> fieldErrors
    ) {
        return response(status, code, message, request, fieldErrors, null);
    }

    private ResponseEntity<AuthErrorResponse> response(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request,
            Map<String, List<String>> fieldErrors,
            Integer retryAfterSeconds
    ) {
        return ResponseEntity.status(status).body(new AuthErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                request.getRequestURI(),
                fieldErrors,
                retryAfterSeconds
        ));
    }
}
