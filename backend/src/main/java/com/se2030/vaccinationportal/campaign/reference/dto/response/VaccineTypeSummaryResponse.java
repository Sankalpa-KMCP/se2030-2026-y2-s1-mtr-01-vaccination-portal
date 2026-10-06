package com.se2030.vaccinationportal.campaign.reference.dto.response;

/**
 * Summary view of a VaccineType.
 */
public record VaccineTypeSummaryResponse(
        Long id,
        String code,
        String name,
        String doseUnit,
        boolean active
) {
}
