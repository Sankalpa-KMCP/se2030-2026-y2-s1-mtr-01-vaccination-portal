package com.se2030.vaccinationportal.shared.auth.exception;

public class StaffRequestNotPendingException extends RuntimeException {
    public StaffRequestNotPendingException() {
        super("Staff account request has already been reviewed");
    }
}
