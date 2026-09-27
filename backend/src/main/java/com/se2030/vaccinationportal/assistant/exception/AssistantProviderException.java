package com.se2030.vaccinationportal.assistant.exception;

/**
 * Thrown when an upstream AI provider returns an error or is unavailable.
 */
public class AssistantProviderException extends RuntimeException {
    public AssistantProviderException(String message) {
        super(message);
    }

    public AssistantProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
