package com.se2030.vaccinationportal.shared.auth.exception;

public class InvalidRequestedRoleException extends RuntimeException {
    public InvalidRequestedRoleException() {
        super("Requested staff role is not permitted");
    }
}
