package com.se2030.vaccinationportal.vaccination.administration.model;

import java.util.Arrays;

/**
 * Finalized vaccination record fields that may receive an audited correction.
 */
public enum VaccinationCorrectionField {
    VACCINE_BATCH_ID("vaccineBatchId"),
    DOSE_NUMBER("doseNumber"),
    ADMINISTRATION_TIME("administrationTime"),
    NEXT_DOSE_DATE("nextDoseDate"),
    AUTHORIZED_NOTES("authorizedNotes");

    private final String apiName;

    VaccinationCorrectionField(String apiName) {
        this.apiName = apiName;
    }

    public String getApiName() {
        return apiName;
    }

    public static VaccinationCorrectionField fromApiName(String apiName) {
        if (apiName == null) {
            return null;
        }
        String normalized = apiName.trim();
        return Arrays.stream(values())
                .filter(field -> field.apiName.equals(normalized))
                .findFirst()
                .orElse(null);
    }
}
