package com.se2030.vaccinationportal.assistant.exception;

/**
 * Thrown when an anonymous or authenticated client exceeds their message rate limit.
 */
public class AssistantRateLimitExceededException extends RuntimeException {
    public AssistantRateLimitExceededException(String message) {
        super(message);
    }
}
