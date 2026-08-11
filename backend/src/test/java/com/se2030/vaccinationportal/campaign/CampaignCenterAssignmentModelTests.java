package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CampaignCenterAssignment;
import com.se2030.vaccinationportal.campaign.model.CampaignCenterAssignmentStatus;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CampaignCenterAssignmentModelTests {

    private static final LocalDate START = LocalDate.of(2026, 9, 15);
    private static final LocalDate END = LocalDate.of(2026, 10, 15);

    private VaccinationCampaign campaign;
    private VaccinationCenter center;

    @BeforeEach
    void setUp() {
        campaign = campaign();
        center = new VaccinationCenter(
                "CTR-ASSIGN-MODEL", "Assignment Center", "1 Health Road", "+94 11 000 0000",
                CenterStatus.ACTIVE, 100, 50, "UTC");
    }

    @Test
    void finiteAndOpenEndedConstructionDefaultActive() {
        CampaignCenterAssignment finite = new CampaignCenterAssignment(campaign, center, START, END);
        CampaignCenterAssignment open = new CampaignCenterAssignment(campaign, center, START, null);

        assertAll(
                () -> assertEquals(campaign, finite.getCampaign()),
                () -> assertEquals(center, finite.getCenter()),
                () -> assertEquals(START, finite.getActiveFrom()),
                () -> assertEquals(END, finite.getActiveTo()),
                () -> assertEquals(CampaignCenterAssignmentStatus.ACTIVE, finite.getStatus()),
                () -> assertNull(open.getActiveTo())
        );
    }

    @Test
    void requiredParentsAndStartAreEnforced() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new CampaignCenterAssignment(null, center, START, END)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new CampaignCenterAssignment(campaign, null, START, END)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new CampaignCenterAssignment(campaign, center, null, END))
        );
    }

    @Test
    void equalAndReversedFinitePeriodsAreRejected() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new CampaignCenterAssignment(campaign, center, START, START)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new CampaignCenterAssignment(campaign, center, END, START))
        );
    }

    @Test
    void effectivenessUsesInclusiveStartAndExclusiveEnd() {
        CampaignCenterAssignment assignment = new CampaignCenterAssignment(campaign, center, START, END);

        assertAll(
                () -> assertFalse(assignment.isEffectiveOn(START.minusDays(1))),
                () -> assertTrue(assignment.isEffectiveOn(START)),
                () -> assertTrue(assignment.isEffectiveOn(END.minusDays(1))),
                () -> assertFalse(assignment.isEffectiveOn(END)),
                () -> assertFalse(assignment.isEffectiveOn(null))
        );
    }

    @Test
    void validUpdateAndEndPreserveOwnership() {
        CampaignCenterAssignment assignment = new CampaignCenterAssignment(campaign, center, START, null);
        LocalDate updatedStart = START.plusDays(2);
        LocalDate updatedEnd = END.plusDays(2);

        assignment.updatePeriod(updatedStart, updatedEnd);
        assignment.endAt(END);

        assertAll(
                () -> assertEquals(campaign, assignment.getCampaign()),
                () -> assertEquals(center, assignment.getCenter()),
                () -> assertEquals(updatedStart, assignment.getActiveFrom()),
                () -> assertEquals(END, assignment.getActiveTo())
        );
    }

    @Test
    void invalidCandidateMutationsLeavePeriodUnchanged() {
        CampaignCenterAssignment assignment = new CampaignCenterAssignment(campaign, center, START, END);

        assertThrows(IllegalArgumentException.class, () -> assignment.updatePeriod(END, START));
        assertEquals(START, assignment.getActiveFrom());
        assertEquals(END, assignment.getActiveTo());

        assertThrows(IllegalArgumentException.class, () -> assignment.endAt(END.plusDays(1)));
        assertEquals(END, assignment.getActiveTo());
    }

    @Test
    void cancellationIsTerminalAndNotEffective() {
        CampaignCenterAssignment assignment = new CampaignCenterAssignment(campaign, center, START, END);

        assignment.cancel();

        assertEquals(CampaignCenterAssignmentStatus.CANCELLED, assignment.getStatus());
        assertFalse(assignment.isEffectiveOn(START));
        assertThrows(IllegalArgumentException.class, assignment::cancel);
        assertThrows(IllegalArgumentException.class, () -> assignment.updatePeriod(START.plusDays(1), END));
        assertThrows(IllegalArgumentException.class, () -> assignment.endAt(END.minusDays(1)));
    }

    @Test
    void mutationSurfaceIsNarrow() {
        var publicMethods = Arrays.stream(CampaignCenterAssignment.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .map(method -> method.getName())
                .toList();

        assertAll(
                () -> assertFalse(publicMethods.contains("setId")),
                () -> assertFalse(publicMethods.contains("setCampaign")),
                () -> assertFalse(publicMethods.contains("setCenter")),
                () -> assertFalse(publicMethods.contains("setCreatedAt")),
                () -> assertFalse(publicMethods.contains("setUpdatedAt")),
                () -> assertFalse(publicMethods.contains("equals")),
                () -> assertFalse(publicMethods.contains("hashCode")),
                () -> assertTrue(publicMethods.contains("updatePeriod")),
                () -> assertTrue(publicMethods.contains("endAt")),
                () -> assertTrue(publicMethods.contains("cancel"))
        );
    }

    private VaccinationCampaign campaign() {
        return new VaccinationCampaign(
                "CMP-ASSIGN-MODEL", "Assignment Model Campaign", null,
                new VaccineType("VAX-ASSIGN-MODEL", "Assignment Vaccine"),
                new TargetGroup("TG-ASSIGN-MODEL", "Assignment Group"),
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31),
                Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-12-30T00:00:00Z"),
                CampaignStatus.DRAFT
        );
    }
}
