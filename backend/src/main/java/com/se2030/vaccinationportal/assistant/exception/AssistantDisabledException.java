package com.se2030.vaccinationportal.assistant.exception;

/**
 * Thrown when the AI Assistant is disabled or not configured.
 */
public class AssistantDisabledException extends RuntimeException {
    public AssistantDisabledException(String message) {
        super(message);
    }
}
