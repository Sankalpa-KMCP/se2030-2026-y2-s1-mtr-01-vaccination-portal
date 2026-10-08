package com.se2030.vaccinationportal.assistant.controller;

import com.se2030.vaccinationportal.assistant.dto.AssistantErrorResponse;
import com.se2030.vaccinationportal.assistant.exception.AssistantDisabledException;
import com.se2030.vaccinationportal.assistant.exception.AssistantProviderException;
import com.se2030.vaccinationportal.assistant.exception.AssistantRateLimitExceededException;
import com.se2030.vaccinationportal.assistant.exception.AssistantTimeoutException;
import com.se2030.vaccinationportal.assistant.exception.InvalidAssistantAuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller advice translating assistant subsystem exceptions into standardized, secure HTTP responses.
 * Never reveals upstream provider headers, stack traces, prompts, or sensitive internals.
 */
@RestControllerAdvice(basePackages = "com.se2030.vaccinationportal.assistant.controller")
public class AssistantExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AssistantExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AssistantErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.computeIfAbsent(fe.getField(), ignored -> new ArrayList<>())
                    .add(fe.getDefaultMessage());
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                AssistantErrorResponse.ofValidation(
                        HttpStatus.BAD_REQUEST.value(),
                        "Bad Request",
                        "VALIDATION_FAILED",
                        "Input payload validation failed",
                        request.getRequestURI(),
                        fieldErrors
                )
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<AssistantErrorResponse> handleNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                AssistantErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "Bad Request",
                        "MALFORMED_REQUEST",
                        "Malformed assistant request payload",
                        request.getRequestURI()
                )
        );
    }

    @ExceptionHandler(InvalidAssistantAuthenticationException.class)
    public ResponseEntity<AssistantErrorResponse> handleInvalidAuthentication(
            InvalidAssistantAuthenticationException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                AssistantErrorResponse.of(
                        HttpStatus.UNAUTHORIZED.value(),
                        "Unauthorized",
                        "UNAUTHORIZED",
                        ex.getMessage(),
                        request.getRequestURI()
                )
        );
    }

    @ExceptionHandler(AssistantRateLimitExceededException.class)
    public ResponseEntity<AssistantErrorResponse> handleRateLimit(
            AssistantRateLimitExceededException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
                AssistantErrorResponse.of(
                        HttpStatus.TOO_MANY_REQUESTS.value(),
                        "Too Many Requests",
                        "RATE_LIMIT_EXCEEDED",
                        ex.getMessage(),
                        request.getRequestURI()
                )
        );
    }

    @ExceptionHandler(AssistantDisabledException.class)
    public ResponseEntity<AssistantErrorResponse> handleDisabled(
            AssistantDisabledException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                AssistantErrorResponse.of(
                        HttpStatus.SERVICE_UNAVAILABLE.value(),
                        "Service Unavailable",
                        "ASSISTANT_DISABLED",
                        ex.getMessage(),
                        request.getRequestURI()
                )
        );
    }

    @ExceptionHandler(AssistantTimeoutException.class)
    public ResponseEntity<AssistantErrorResponse> handleTimeout(
            AssistantTimeoutException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(
                AssistantErrorResponse.of(
                        HttpStatus.GATEWAY_TIMEOUT.value(),
                        "Gateway Timeout",
                        "ASSISTANT_TIMEOUT",
                        "The assistant request timed out. Please try again.",
                        request.getRequestURI()
                )
        );
    }

    @ExceptionHandler(AssistantProviderException.class)
    public ResponseEntity<AssistantErrorResponse> handleProvider(
            AssistantProviderException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                AssistantErrorResponse.of(
                        HttpStatus.BAD_GATEWAY.value(),
                        "Bad Gateway",
                        "PROVIDER_UNAVAILABLE",
                        "The assistant service is temporarily unavailable. Please try again later.",
                        request.getRequestURI()
                )
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AssistantErrorResponse> handleGeneral(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unhandled assistant exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                AssistantErrorResponse.of(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Internal Server Error",
                        "INTERNAL_ERROR",
                        "An unexpected error occurred while processing the assistant request",
                        request.getRequestURI()
                )
        );
    }
}
