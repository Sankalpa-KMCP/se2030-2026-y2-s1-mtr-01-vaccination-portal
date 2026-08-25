package com.se2030.vaccinationportal.screening.exception;

/**
 * Thrown when a requested MF-01 screening resource cannot be found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
