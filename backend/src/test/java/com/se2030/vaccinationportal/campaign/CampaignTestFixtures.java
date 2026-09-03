package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;

/** Test-only construction support for persisted historical campaign states. */
public final class CampaignTestFixtures {

    private static final Field STATUS_FIELD = statusField();

    private CampaignTestFixtures() {
    }

    public static VaccinationCampaign campaign(
            String code,
            String name,
            String description,
            VaccineType vaccineType,
            TargetGroup targetGroup,
            LocalDate serviceStartDate,
            LocalDate serviceEndDate,
            Instant bookingOpenAt,
            Instant bookingCloseAt,
            CampaignStatus status
    ) {
        VaccinationCampaign campaign = new VaccinationCampaign(
                code,
                name,
                description,
                vaccineType,
                targetGroup,
                serviceStartDate,
                serviceEndDate,
                bookingOpenAt,
                bookingCloseAt
        );
        forceStatus(campaign, status);
        return campaign;
    }

    public static void forceStatus(VaccinationCampaign campaign, CampaignStatus status) {
        if (campaign == null) {
            throw new IllegalArgumentException("Campaign is required");
        }
        CampaignStatus targetStatus = status != null ? status : CampaignStatus.DRAFT;
        if (campaign.getStatus() == targetStatus) {
            return;
        }
        if (targetStatus == CampaignStatus.PUBLISHED
                && (campaign.getStatus() == CampaignStatus.DRAFT
                || campaign.getStatus() == CampaignStatus.SCHEDULED)) {
            campaign.publish();
            return;
        }
        if (targetStatus == CampaignStatus.SUSPENDED
                && (campaign.getStatus() == CampaignStatus.PUBLISHED
                || campaign.getStatus() == CampaignStatus.ACTIVE)) {
            campaign.suspend();
            return;
        }
        try {
            STATUS_FIELD.set(campaign, targetStatus);
        } catch (IllegalAccessException error) {
            throw new IllegalStateException("Unable to prepare historical campaign status for test", error);
        }
    }

    private static Field statusField() {
        try {
            Field field = VaccinationCampaign.class.getDeclaredField("status");
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException error) {
            throw new ExceptionInInitializerError(error);
        }
    }
}
