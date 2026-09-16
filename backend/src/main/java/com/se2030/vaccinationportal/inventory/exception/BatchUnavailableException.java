package com.se2030.vaccinationportal.inventory.exception;

import com.se2030.vaccinationportal.inventory.dependency.BatchAvailabilityReason;

public class BatchUnavailableException extends IllegalStateException {
    private final BatchAvailabilityReason reason;

    public BatchUnavailableException(BatchAvailabilityReason reason) {
        super("Vaccination batch unavailable: " + reason);
        this.reason = reason;
    }

    public BatchAvailabilityReason getReason() { return reason; }
}
