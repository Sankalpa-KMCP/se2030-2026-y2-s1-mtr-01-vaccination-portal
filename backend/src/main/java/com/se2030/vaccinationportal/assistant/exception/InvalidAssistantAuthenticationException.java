package com.se2030.vaccinationportal.assistant.exception;

/**
 * Thrown when an invalid or expired authentication token is presented to the assistant messages endpoint.
 * Prevents invalid JWTs from silently downgrading to anonymous execution.
 */
public class InvalidAssistantAuthenticationException extends RuntimeException {
    public InvalidAssistantAuthenticationException(String message) {
        super(message);
    }
}
