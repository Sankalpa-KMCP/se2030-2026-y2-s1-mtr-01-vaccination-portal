package com.se2030.vaccinationportal.campaign.core;

import com.se2030.vaccinationportal.campaign.CampaignTestFixtures;


import com.se2030.vaccinationportal.campaign.core.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.reference.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.reference.model.VaccineType;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CampaignSuspensionDomainTests {

    @Test
    void publishedCampaignCanBeSuspended() {
        VaccinationCampaign campaign = campaign(CampaignStatus.PUBLISHED);

        campaign.suspend();

        assertEquals(CampaignStatus.SUSPENDED, campaign.getStatus());
    }

    @Test
    void activeCampaignCanBeSuspended() {
        VaccinationCampaign campaign = campaign(CampaignStatus.ACTIVE);

        campaign.suspend();

        assertEquals(CampaignStatus.SUSPENDED, campaign.getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = CampaignStatus.class, names = {
            "DRAFT", "SCHEDULED", "SUSPENDED", "COMPLETED", "CANCELLED", "ARCHIVED"
    })
    void unsupportedSourceStatusIsRejectedWithoutMutation(CampaignStatus sourceStatus) {
        VaccinationCampaign campaign = campaign(sourceStatus);

        assertThrows(IllegalStateException.class, campaign::suspend);

        assertEquals(sourceStatus, campaign.getStatus());
    }

    @Test
    void repeatedSuspensionIsRejectedWithoutMutation() {
        VaccinationCampaign campaign = campaign(CampaignStatus.PUBLISHED);
        campaign.suspend();

        assertThrows(IllegalStateException.class, campaign::suspend);

        assertEquals(CampaignStatus.SUSPENDED, campaign.getStatus());
    }

    private VaccinationCampaign campaign(CampaignStatus status) {
        return CampaignTestFixtures.campaign(
                "CMP-SUSP-DOM-" + status,
                "Suspension Domain Campaign",
                null,
                new VaccineType("VAX-SUSP-DOM", "Suspension Domain Vaccine"),
                new TargetGroup("TG-SUSP-DOM", "Suspension Domain Group"),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31),
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-12-30T00:00:00Z"),
                status
        );
    }
}
