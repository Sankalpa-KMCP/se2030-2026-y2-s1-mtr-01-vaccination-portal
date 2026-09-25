package com.se2030.vaccinationportal.assistant.exception;

/**
 * Thrown when an upstream AI provider invocation times out.
 */
public class AssistantTimeoutException extends RuntimeException {
    public AssistantTimeoutException(String message) {
        super(message);
    }

    public AssistantTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
