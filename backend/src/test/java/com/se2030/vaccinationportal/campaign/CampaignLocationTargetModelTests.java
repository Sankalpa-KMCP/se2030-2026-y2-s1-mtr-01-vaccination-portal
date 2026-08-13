package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CampaignLocationTarget;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.LocationTargetType;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying CampaignLocationTarget model constraints, validation,
 * and targetCode normalization (WBVP-SE2030 / MF02-BR-04).
 */
class CampaignLocationTargetModelTests {

    private VaccinationCampaign sampleCampaign;

    @BeforeEach
    void setUp() {
        VaccineType vt = new VaccineType("VAX-COV", "COVID-19 Vaccine", "dose", true);
        TargetGroup tg = new TargetGroup("TG-GENERAL", "General Population", true);
        sampleCampaign = new VaccinationCampaign(
                "CMP-LOC-TEST",
                "Location Test Campaign",
                "Description",
                vt,
                tg,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31),
                Instant.parse("2026-08-15T08:00:00Z"),
                Instant.parse("2026-12-30T18:00:00Z"),
                CampaignStatus.DRAFT
        );
    }

    @Test
    @DisplayName("Valid location target creation and normalization of targetCode using Locale.ROOT")
    void validLocationTargetCreationAndNormalization() {
        CampaignLocationTarget target = new CampaignLocationTarget(
                sampleCampaign,
                LocationTargetType.PROVINCE,
                "  wp "
        );

        assertEquals(sampleCampaign, target.getCampaign());
        assertEquals(LocationTargetType.PROVINCE, target.getTargetType());
        assertEquals("WP", target.getTargetCode(), "targetCode must be trimmed and converted to uppercase");
    }

    @Test
    @DisplayName("Blank or null targetCode is rejected")
    void blankOrNullTargetCodeThrowsException() {
        IllegalArgumentException exNull = assertThrows(IllegalArgumentException.class, () ->
                new CampaignLocationTarget(sampleCampaign, LocationTargetType.DISTRICT, null)
        );
        assertTrue(exNull.getMessage().contains("Target code must not be null or blank"));

        IllegalArgumentException exBlank = assertThrows(IllegalArgumentException.class, () ->
                new CampaignLocationTarget(sampleCampaign, LocationTargetType.DISTRICT, "   ")
        );
        assertTrue(exBlank.getMessage().contains("Target code must not be null or blank"));
    }

    @Test
    @DisplayName("Null targetType is rejected")
    void nullTargetTypeThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new CampaignLocationTarget(sampleCampaign, null, "COLOMBO")
        );
        assertTrue(ex.getMessage().contains("Target type is required"));
    }

    @Test
    @DisplayName("Null campaign is rejected")
    void nullCampaignThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new CampaignLocationTarget(null, LocationTargetType.HEALTH_REGION, "HR-WEST")
        );
        assertTrue(ex.getMessage().contains("Campaign is required"));
    }

    @Test
    @DisplayName("normalizeTargetCode converts lowercase and mixed case with Locale.ROOT")
    void normalizeTargetCodeFormatsConsistently() {
        assertEquals("COLOMBO", CampaignLocationTarget.normalizeTargetCode("colombo"));
        assertEquals("HR-NORTH", CampaignLocationTarget.normalizeTargetCode("  hr-north  "));
        assertEquals("CTR-001", CampaignLocationTarget.normalizeTargetCode("ctr-001"));
    }
}
