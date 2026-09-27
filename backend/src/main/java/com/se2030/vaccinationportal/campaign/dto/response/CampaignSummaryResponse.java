package com.se2030.vaccinationportal.campaign.dto.response;

import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import java.time.LocalDate;

/**
 * Compact summary view of a VaccinationCampaign for collections and list endpoints.
 */
public record CampaignSummaryResponse(
        Long id,
        String code,
        String name,
        String vaccineTypeCode,
        String targetGroupCode,
        LocalDate serviceStartDate,
        LocalDate serviceEndDate,
        CampaignStatus status,
        Long questionnaireVersionId
) {
    public CampaignSummaryResponse(
            Long id,
            String code,
            String name,
            String vaccineTypeCode,
            String targetGroupCode,
            LocalDate serviceStartDate,
            LocalDate serviceEndDate,
            CampaignStatus status
    ) {
        this(id, code, name, vaccineTypeCode, targetGroupCode, serviceStartDate, serviceEndDate, status, null);
    }
}
