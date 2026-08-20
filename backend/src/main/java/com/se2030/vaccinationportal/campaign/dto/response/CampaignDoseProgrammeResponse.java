package com.se2030.vaccinationportal.campaign.dto.response;

import com.se2030.vaccinationportal.campaign.model.DoseCategory;
import java.time.LocalDate;

/**
 * Response DTO representing a CampaignDoseProgramme entity.
 */
public record CampaignDoseProgrammeResponse(
        Long id,
        Long campaignId,
        int versionNumber,
        DoseCategory doseCategory,
        String permittedDoseNumbers,
        int minimumIntervalDays,
        boolean previousEvidenceRequired,
        Integer expectedServiceMinutes,
        LocalDate activeFrom,
        LocalDate activeTo
) {}
