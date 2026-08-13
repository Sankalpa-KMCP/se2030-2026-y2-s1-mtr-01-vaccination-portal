package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CampaignQuestionnaireAssignment;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
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
 * Unit tests verifying domain invariants, validation rules, and boundary evaluation
 * for {@link CampaignQuestionnaireAssignment} (WBVP-SE2030 / MF02-BR-02 / MF02-BR-17).
 */
class CampaignQuestionnaireAssignmentModelTests {

    private VaccinationCampaign sampleCampaign;

    @BeforeEach
    void setUp() {
        VaccineType vt = new VaccineType("VAX-MRNA-Q", "mRNA Vaccine", "dose", true);
        TargetGroup tg = new TargetGroup("TG-ADULT-Q", "Adult Population", true);
        sampleCampaign = CampaignTestFixtures.campaign(
                "CMP-Q-TEST",
                "Questionnaire Test Campaign",
                "Campaign for questionnaire linkage tests",
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
    @DisplayName("Valid campaign questionnaire assignment passes validation")
    void validAssignmentPassesValidation() {
        CampaignQuestionnaireAssignment assignment = new CampaignQuestionnaireAssignment(
                sampleCampaign,
                101L,
                LocalDate.of(2026, 9, 1),
                null
        );

        assertEquals(sampleCampaign, assignment.getCampaign());
        assertEquals(101L, assignment.getQuestionnaireVersionId());
        assertEquals(LocalDate.of(2026, 9, 1), assignment.getActiveFrom());
        assertNull(assignment.getActiveTo());
    }

    @Test
    @DisplayName("Null campaign is rejected")
    void nullCampaignThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new CampaignQuestionnaireAssignment(null, 101L, LocalDate.of(2026, 9, 1), null)
        );
        assertTrue(ex.getMessage().contains("Campaign is required"));
    }

    @Test
    @DisplayName("Null questionnaire version ID is rejected")
    void nullQuestionnaireVersionIdThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new CampaignQuestionnaireAssignment(sampleCampaign, null, LocalDate.of(2026, 9, 1), null)
        );
        assertTrue(ex.getMessage().contains("Questionnaire version ID is required"));
    }

    @Test
    @DisplayName("Non-positive questionnaire version ID is rejected")
    void nonPositiveQuestionnaireVersionIdThrowsException() {
        IllegalArgumentException exZero = assertThrows(IllegalArgumentException.class, () ->
                new CampaignQuestionnaireAssignment(sampleCampaign, 0L, LocalDate.of(2026, 9, 1), null)
        );
        assertTrue(exZero.getMessage().contains("Questionnaire version ID must be positive"));

        IllegalArgumentException exNeg = assertThrows(IllegalArgumentException.class, () ->
                new CampaignQuestionnaireAssignment(sampleCampaign, -5L, LocalDate.of(2026, 9, 1), null)
        );
        assertTrue(exNeg.getMessage().contains("Questionnaire version ID must be positive"));
    }

    @Test
    @DisplayName("Null activeFrom date is rejected")
    void nullActiveFromThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new CampaignQuestionnaireAssignment(sampleCampaign, 101L, null, null)
        );
        assertTrue(ex.getMessage().contains("Active from date is required"));
    }

    @Test
    @DisplayName("Half-open active period requires activeTo after activeFrom")
    void activeToBeforeActiveFromThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new CampaignQuestionnaireAssignment(
                        sampleCampaign,
                        101L,
                        LocalDate.of(2026, 9, 10),
                        LocalDate.of(2026, 9, 9)
                )
        );
        assertTrue(ex.getMessage().contains("must be after active from date"));
    }

    @Test
    @DisplayName("isEffectiveOn evaluates half-open boundaries accurately")
    void isEffectiveOnEvaluatesBoundedRanges() {
        CampaignQuestionnaireAssignment assignment = new CampaignQuestionnaireAssignment(
                sampleCampaign,
                101L,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)
        );

        // Before activeFrom -> false
        assertFalse(assignment.isEffectiveOn(LocalDate.of(2026, 8, 31)));

        // Inclusive start boundary -> true
        assertTrue(assignment.isEffectiveOn(LocalDate.of(2026, 9, 1)));

        // Mid-period -> true
        assertTrue(assignment.isEffectiveOn(LocalDate.of(2026, 9, 15)));

        // Exclusive end boundary -> false
        assertFalse(assignment.isEffectiveOn(LocalDate.of(2026, 9, 30)));

        // After activeTo -> false
        assertFalse(assignment.isEffectiveOn(LocalDate.of(2026, 10, 1)));

        // Null date -> false
        assertFalse(assignment.isEffectiveOn(null));
    }

    @Test
    @DisplayName("Future open-ended assignment is NOT effective before activeFrom date")
    void futureOpenEndedAssignmentNotEffectiveBeforeActiveFrom() {
        CampaignQuestionnaireAssignment futureAssignment = new CampaignQuestionnaireAssignment(
                sampleCampaign,
                102L,
                LocalDate.of(2026, 10, 1),
                null
        );

        // Date before activeFrom -> false
        assertFalse(futureAssignment.isEffectiveOn(LocalDate.of(2026, 9, 30)));

        // Date on activeFrom -> true
        assertTrue(futureAssignment.isEffectiveOn(LocalDate.of(2026, 10, 1)));

        // Date after activeFrom (open-ended) -> true
        assertTrue(futureAssignment.isEffectiveOn(LocalDate.of(2026, 12, 15)));
    }
}
