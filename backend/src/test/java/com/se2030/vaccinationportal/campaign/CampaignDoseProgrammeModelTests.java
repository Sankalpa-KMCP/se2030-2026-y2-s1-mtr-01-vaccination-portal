package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CampaignDoseProgramme;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.DoseCategory;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying CampaignDoseProgramme domain constraints, validation rules,
 * and effective-date boundary logic (WBVP-SE2030 / MF02-BR-02 / MF02-BR-18).
 */
class CampaignDoseProgrammeModelTests {

    private VaccinationCampaign sampleCampaign;

    @BeforeEach
    void setUp() {
        VaccineType vt = new VaccineType("VAX-MRNA", "mRNA Vaccine", "dose", true);
        TargetGroup tg = new TargetGroup("TG-ADULT", "Adults 18+", true);
        sampleCampaign = new VaccinationCampaign(
                "CMP-2026-TEST",
                "Test Campaign",
                "Description",
                vt,
                tg,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31),
                LocalDateTime.of(2026, 8, 15, 8, 0),
                LocalDateTime.of(2026, 12, 30, 18, 0),
                CampaignStatus.DRAFT
        );
    }

    @Test
    @DisplayName("Valid programme creation with defaults sets expectedServiceMinutes to 10")
    void validProgrammeCreationWithDefaults() {
        CampaignDoseProgramme programme = new CampaignDoseProgramme(
                sampleCampaign,
                1,
                DoseCategory.PRIMARY,
                "1,2",
                21,
                false,
                LocalDate.of(2026, 9, 1)
        );

        assertEquals(1, programme.getVersionNumber());
        assertEquals(DoseCategory.PRIMARY, programme.getDoseCategory());
        assertEquals("1,2", programme.getPermittedDoseNumbers());
        assertEquals(21, programme.getMinimumIntervalDays());
        assertFalse(programme.isPreviousEvidenceRequired());
        assertEquals(10, programme.getExpectedServiceMinutes(), "Expected service minutes must default to 10");
        assertEquals(LocalDate.of(2026, 9, 1), programme.getActiveFrom());
        assertNull(programme.getActiveTo());

        List<Integer> parsed = programme.getParsedPermittedDoseNumbers();
        assertEquals(List.of(1, 2), parsed);
    }

    @Test
    @DisplayName("Expected service minutes must be positive integer")
    void expectedServiceMinutesMustBePositive() {
        IllegalArgumentException exZero = assertThrows(IllegalArgumentException.class, () ->
                new CampaignDoseProgramme(
                        sampleCampaign,
                        1,
                        DoseCategory.PRIMARY,
                        "1,2",
                        21,
                        false,
                        0,
                        LocalDate.of(2026, 9, 1),
                        null
                )
        );
        assertTrue(exZero.getMessage().contains("Expected service minutes must be a positive integer"));

        IllegalArgumentException exNeg = assertThrows(IllegalArgumentException.class, () ->
                new CampaignDoseProgramme(
                        sampleCampaign,
                        1,
                        DoseCategory.PRIMARY,
                        "1,2",
                        21,
                        false,
                        -5,
                        LocalDate.of(2026, 9, 1),
                        null
                )
        );
        assertTrue(exNeg.getMessage().contains("Expected service minutes must be a positive integer"));
    }

    @Test
    @DisplayName("Minimum interval days must be non-negative")
    void minimumIntervalDaysMustBeNonNegative() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new CampaignDoseProgramme(
                        sampleCampaign,
                        1,
                        DoseCategory.PRIMARY,
                        "1,2",
                        -1,
                        false,
                        10,
                        LocalDate.of(2026, 9, 1),
                        null
                )
        );
        assertTrue(ex.getMessage().contains("Minimum interval days must be non-negative"));
    }

    @Test
    @DisplayName("Version number must be at least 1")
    void versionNumberMustBeAtLeastOne() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new CampaignDoseProgramme(
                        sampleCampaign,
                        0,
                        DoseCategory.PRIMARY,
                        "1,2",
                        28,
                        false,
                        10,
                        LocalDate.of(2026, 9, 1),
                        null
                )
        );
        assertTrue(ex.getMessage().contains("Version number must be at least 1"));
    }

    @Test
    @DisplayName("Active to date cannot be before active from date")
    void activeToBeforeActiveFromThrowsException() {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 9, 30);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new CampaignDoseProgramme(
                        sampleCampaign,
                        1,
                        DoseCategory.BOOSTER,
                        "3",
                        90,
                        true,
                        15,
                        from,
                        to
                )
        );
        assertTrue(ex.getMessage().contains("cannot be before active from date"));
    }

    @Test
    @DisplayName("Permitted dose numbers validation enforces positive unique integers")
    void permittedDoseNumbersValidationEnforcesCorrectness() {
        // Valid forms
        assertDoesNotThrow(() -> CampaignDoseProgramme.validatePermittedDoseNumbers("1"));
        assertDoesNotThrow(() -> CampaignDoseProgramme.validatePermittedDoseNumbers("1,2"));
        assertDoesNotThrow(() -> CampaignDoseProgramme.validatePermittedDoseNumbers("1, 2, 3"));

        // Blank or null
        assertThrows(IllegalArgumentException.class, () -> CampaignDoseProgramme.validatePermittedDoseNumbers(null));
        assertThrows(IllegalArgumentException.class, () -> CampaignDoseProgramme.validatePermittedDoseNumbers("   "));

        // Invalid format or characters
        assertThrows(IllegalArgumentException.class, () -> CampaignDoseProgramme.validatePermittedDoseNumbers("abc"));
        assertThrows(IllegalArgumentException.class, () -> CampaignDoseProgramme.validatePermittedDoseNumbers("1,TWO"));

        // Non-positive integers
        assertThrows(IllegalArgumentException.class, () -> CampaignDoseProgramme.validatePermittedDoseNumbers("0"));
        assertThrows(IllegalArgumentException.class, () -> CampaignDoseProgramme.validatePermittedDoseNumbers("-1,2"));

        // Duplicates
        assertThrows(IllegalArgumentException.class, () -> CampaignDoseProgramme.validatePermittedDoseNumbers("1,1"));
        assertThrows(IllegalArgumentException.class, () -> CampaignDoseProgramme.validatePermittedDoseNumbers("1,2,1"));
    }

    @Test
    @DisplayName("isEffectiveOn evaluates active date boundaries correctly")
    void isEffectiveOnEvaluatesBoundariesCorrectly() {
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 10, 31);

        CampaignDoseProgramme bounded = new CampaignDoseProgramme(
                sampleCampaign,
                1,
                DoseCategory.PRIMARY,
                "1,2",
                21,
                false,
                10,
                from,
                to
        );

        assertFalse(bounded.isEffectiveOn(LocalDate.of(2026, 8, 31)));
        assertTrue(bounded.isEffectiveOn(LocalDate.of(2026, 9, 1)));
        assertTrue(bounded.isEffectiveOn(LocalDate.of(2026, 10, 15)));
        assertTrue(bounded.isEffectiveOn(LocalDate.of(2026, 10, 31)));
        assertFalse(bounded.isEffectiveOn(LocalDate.of(2026, 11, 1)));

        // Open-ended version
        CampaignDoseProgramme openEnded = new CampaignDoseProgramme(
                sampleCampaign,
                2,
                DoseCategory.BOOSTER,
                "3",
                90,
                true,
                10,
                LocalDate.of(2026, 11, 1),
                null
        );

        assertFalse(openEnded.isEffectiveOn(LocalDate.of(2026, 10, 31)));
        assertTrue(openEnded.isEffectiveOn(LocalDate.of(2026, 11, 1)));
        assertTrue(openEnded.isEffectiveOn(LocalDate.of(2027, 5, 1)));
    }
}
