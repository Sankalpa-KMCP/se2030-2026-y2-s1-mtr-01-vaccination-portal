package com.se2030.vaccinationportal.campaign.exception;

/**
 * Exception thrown when a requested campaign management domain resource or referenced entity cannot be found.
 */
public class ResourceNotFoundException extends IllegalArgumentException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
