package com.se2030.vaccinationportal.campaign.reference.dto.response;

/**
 * Summary view of a TargetGroup.
 */
public record TargetGroupSummaryResponse(
        Long id,
        String code,
        String description,
        boolean active
) {
}
