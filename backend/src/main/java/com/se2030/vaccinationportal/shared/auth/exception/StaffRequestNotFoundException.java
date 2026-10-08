package com.se2030.vaccinationportal.shared.auth.exception;

public class StaffRequestNotFoundException extends RuntimeException {
    public StaffRequestNotFoundException(Long id) {
        super("Staff account request " + id + " was not found");
    }
}
