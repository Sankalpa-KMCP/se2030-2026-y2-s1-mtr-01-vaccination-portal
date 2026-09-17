package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Domain model validation unit tests verifying MF02-BR-01 date constraints and booking intake logic.
 */
class VaccinationCampaignModelTests {

    private VaccineType sampleVaccineType;
    private TargetGroup sampleTargetGroup;

    @BeforeEach
    void setUp() {
        sampleVaccineType = new VaccineType("VAX-MRNA-01", "COVID-19 mRNA Vaccine", "dose", true);
        sampleTargetGroup = new TargetGroup("TG-ELDERLY-65", "Adults aged 65 and above", true);
    }

    @Test
    @DisplayName("Valid campaign date ranges and booking windows pass validation (MF02-BR-01)")
    void validDatesPassValidation() {
        LocalDate startDate = LocalDate.of(2026, 9, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 31);
        Instant bookingOpen = Instant.parse("2026-08-15T08:00:00Z");
        Instant bookingClose = Instant.parse("2026-12-30T18:00:00Z");

        VaccinationCampaign campaign = CampaignTestFixtures.campaign(
                "CMP-2026-01",
                "National Autumn Vaccination Drive",
                "Phase 1 vaccination campaign",
                sampleVaccineType,
                sampleTargetGroup,
                startDate,
                endDate,
                bookingOpen,
                bookingClose,
                CampaignStatus.DRAFT
        );

        assertDoesNotThrow(campaign::validateDateConsistency);
        assertEquals("CMP-2026-01", campaign.getCode());
        assertEquals(CampaignStatus.DRAFT, campaign.getStatus());
    }

    @Test
    @DisplayName("Service end date equal to service start date violates MF02-BR-01")
    void serviceEndDateEqualToStartDateThrowsException() {
        LocalDate sameDate = LocalDate.of(2026, 9, 1);
        Instant bookingOpen = Instant.parse("2026-08-15T08:00:00Z");
        Instant bookingClose = Instant.parse("2026-09-01T18:00:00Z");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                CampaignTestFixtures.campaign(
                        "CMP-INVALID-01",
                        "Invalid Campaign",
                        null,
                        sampleVaccineType,
                        sampleTargetGroup,
                        sameDate,
                        sameDate,
                        bookingOpen,
                        bookingClose,
                        CampaignStatus.DRAFT
                )
        );

        assertTrue(ex.getMessage().contains("must be after service start date"));
        assertTrue(ex.getMessage().contains("MF02-BR-01"));
    }

    @Test
    @DisplayName("Service end date before service start date violates MF02-BR-01")
    void serviceEndDateBeforeStartDateThrowsException() {
        LocalDate startDate = LocalDate.of(2026, 10, 1);
        LocalDate endDate = LocalDate.of(2026, 9, 1);
        Instant bookingOpen = Instant.parse("2026-08-15T08:00:00Z");
        Instant bookingClose = Instant.parse("2026-09-15T18:00:00Z");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                CampaignTestFixtures.campaign(
                        "CMP-INVALID-02",
                        "Invalid Dates Campaign",
                        null,
                        sampleVaccineType,
                        sampleTargetGroup,
                        startDate,
                        endDate,
                        bookingOpen,
                        bookingClose,
                        CampaignStatus.DRAFT
                )
        );

        assertTrue(ex.getMessage().contains("must be after service start date"));
        assertTrue(ex.getMessage().contains("MF02-BR-01"));
    }

    @Test
    @DisplayName("Booking close timestamp equal to booking open timestamp violates MF02-BR-01")
    void bookingCloseEqualToBookingOpenThrowsException() {
        LocalDate startDate = LocalDate.of(2026, 9, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 31);
        Instant sameTimestamp = Instant.parse("2026-08-15T08:00:00Z");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                CampaignTestFixtures.campaign(
                        "CMP-INVALID-03",
                        "Invalid Window Campaign",
                        null,
                        sampleVaccineType,
                        sampleTargetGroup,
                        startDate,
                        endDate,
                        sameTimestamp,
                        sameTimestamp,
                        CampaignStatus.DRAFT
                )
        );

        assertTrue(ex.getMessage().contains("booking close time"));
        assertTrue(ex.getMessage().contains("must be after booking open time"));
        assertTrue(ex.getMessage().contains("MF02-BR-01"));
    }

    @Test
    @DisplayName("Booking close timestamp before booking open timestamp violates MF02-BR-01")
    void bookingCloseBeforeBookingOpenThrowsException() {
        LocalDate startDate = LocalDate.of(2026, 9, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 31);
        Instant bookingOpen = Instant.parse("2026-08-20T08:00:00Z");
        Instant bookingClose = Instant.parse("2026-08-15T08:00:00Z");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                CampaignTestFixtures.campaign(
                        "CMP-INVALID-04",
                        "Invalid Inverted Window Campaign",
                        null,
                        sampleVaccineType,
                        sampleTargetGroup,
                        startDate,
                        endDate,
                        bookingOpen,
                        bookingClose,
                        CampaignStatus.DRAFT
                )
        );

        assertTrue(ex.getMessage().contains("must be after booking open time"));
        assertTrue(ex.getMessage().contains("MF02-BR-01"));
    }

    @Test
    @DisplayName("Booking close timestamp after campaign service end date violates MF02-BR-01")
    void bookingCloseTimestampAfterServiceEndDateThrowsException() {
        LocalDate startDate = LocalDate.of(2026, 9, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 31);
        Instant bookingOpen = Instant.parse("2026-08-15T08:00:00Z");
        Instant bookingClose = Instant.parse("2027-01-05T18:00:00Z");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                CampaignTestFixtures.campaign(
                        "CMP-INVALID-05",
                        "Booking Close After Service End",
                        null,
                        sampleVaccineType,
                        sampleTargetGroup,
                        startDate,
                        endDate,
                        bookingOpen,
                        bookingClose,
                        CampaignStatus.DRAFT
                )
        );

        assertTrue(ex.getMessage().contains("must not be after campaign service end date"));
        assertTrue(ex.getMessage().contains("MF02-BR-01"));
    }

    @Test
    @DisplayName("Booking close timestamp on campaign service end date passes MF02-BR-01")
    void bookingCloseTimestampOnServiceEndDatePasses() {
        LocalDate startDate = LocalDate.of(2026, 9, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 31);
        Instant bookingOpen = Instant.parse("2026-08-15T08:00:00Z");
        Instant bookingClose = Instant.parse("2026-12-31T23:59:59Z");

        VaccinationCampaign campaign = CampaignTestFixtures.campaign(
                "CMP-VALID-BOUNDARY",
                "Boundary Booking Campaign",
                null,
                sampleVaccineType,
                sampleTargetGroup,
                startDate,
                endDate,
                bookingOpen,
                bookingClose,
                CampaignStatus.DRAFT
        );

        assertDoesNotThrow(campaign::validateDateConsistency);
    }

    @Test
    @DisplayName("isBookingWindowOpenAt evaluates status and temporal boundaries accurately (MF02-BR-03)")
    void bookingWindowEvaluationRespectsStatusAndDates() {
        LocalDate startDate = LocalDate.of(2026, 9, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 31);
        Instant bookingOpen = Instant.parse("2026-08-15T08:00:00Z");
        Instant bookingClose = Instant.parse("2026-12-30T18:00:00Z");

        VaccinationCampaign campaign = CampaignTestFixtures.campaign(
                "CMP-INTAKE-01",
                "Intake Test Campaign",
                null,
                sampleVaccineType,
                sampleTargetGroup,
                startDate,
                endDate,
                bookingOpen,
                bookingClose,
                CampaignStatus.PUBLISHED
        );

        // Before booking opens
        assertFalse(campaign.isBookingWindowOpenAt(Instant.parse("2026-08-15T07:59:00Z")));

        // Exactly at booking open
        assertTrue(campaign.isBookingWindowOpenAt(bookingOpen));

        // Inside booking window
        assertTrue(campaign.isBookingWindowOpenAt(Instant.parse("2026-10-01T12:00:00Z")));

        // Exactly at booking close
        assertTrue(campaign.isBookingWindowOpenAt(bookingClose));

        // After booking close
        assertFalse(campaign.isBookingWindowOpenAt(Instant.parse("2026-12-30T18:01:00Z")));

        // Inactive / non-intake status blocks bookings even during open dates (MF02-BR-03)
        CampaignTestFixtures.forceStatus(campaign, CampaignStatus.SUSPENDED);
        assertFalse(campaign.isBookingWindowOpenAt(Instant.parse("2026-10-01T12:00:00Z")));

        CampaignTestFixtures.forceStatus(campaign, CampaignStatus.DRAFT);
        assertFalse(campaign.isBookingWindowOpenAt(Instant.parse("2026-10-01T12:00:00Z")));

        CampaignTestFixtures.forceStatus(campaign, CampaignStatus.ACTIVE);
        assertTrue(campaign.isBookingWindowOpenAt(Instant.parse("2026-10-01T12:00:00Z")));
    }

    @Test
    @DisplayName("New campaigns start in DRAFT and expose no generic public status mutator")
    void campaignLifecycleSurfaceIsConstrained() {
        VaccinationCampaign campaign = CampaignTestFixtures.campaign(
                "CMP-INTEGRITY-01",
                "Lifecycle Integrity Campaign",
                null,
                sampleVaccineType,
                sampleTargetGroup,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31),
                Instant.parse("2026-08-15T08:00:00Z"),
                Instant.parse("2026-12-30T18:00:00Z"),
                CampaignStatus.DRAFT
        );

        assertEquals(CampaignStatus.DRAFT, campaign.getStatus());
        assertTrue(java.util.Arrays.stream(VaccinationCampaign.class.getConstructors())
                .flatMap(constructor -> java.util.Arrays.stream(constructor.getParameterTypes()))
                .noneMatch(CampaignStatus.class::equals));
        assertTrue(java.util.Arrays.stream(VaccinationCampaign.class.getMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .map(Method::getName)
                .noneMatch(name -> name.equals("setStatus")
                        || name.equals("changeStatus")
                        || name.equals("transitionTo")
                        || name.equals("updateStatus")));
    }

    @Test
    @DisplayName("schedule transitions campaign from DRAFT to SCHEDULED")
    void scheduleTransitionsFromDraftToScheduled() {
        VaccinationCampaign campaign = CampaignTestFixtures.campaign(
                "CMP-SCHED-01",
                "Scheduling Test Campaign",
                "Description",
                sampleVaccineType,
                sampleTargetGroup,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31),
                Instant.parse("2026-08-15T08:00:00Z"),
                Instant.parse("2026-12-30T18:00:00Z"),
                CampaignStatus.DRAFT
        );

        campaign.schedule();
        assertEquals(CampaignStatus.SCHEDULED, campaign.getStatus());
    }

    @Test
    @DisplayName("schedule rejects transition from non-DRAFT statuses")
    void scheduleRejectsNonDraftStatuses() {
        for (CampaignStatus status : CampaignStatus.values()) {
            if (status == CampaignStatus.DRAFT) {
                continue;
            }
            VaccinationCampaign campaign = CampaignTestFixtures.campaign(
                    "CMP-SCHED-" + status.name(),
                    "Scheduling Rejection Test",
                    "Description",
                    sampleVaccineType,
                    sampleTargetGroup,
                    LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 12, 31),
                    Instant.parse("2026-08-15T08:00:00Z"),
                    Instant.parse("2026-12-30T18:00:00Z"),
                    status
            );

            IllegalStateException ex = assertThrows(IllegalStateException.class, campaign::schedule);
            assertTrue(ex.getMessage().contains("Cannot schedule campaign in " + status));
            assertTrue(ex.getMessage().contains("Only DRAFT campaigns can be scheduled"));
        }
    }

    @Test
    @DisplayName("activate transitions campaign from PUBLISHED to ACTIVE")
    void activateTransitionsFromPublishedToActive() {
        VaccinationCampaign campaign = CampaignTestFixtures.campaign(
                "CMP-ACT-01",
                "Activation Test Campaign",
                "Description",
                sampleVaccineType,
                sampleTargetGroup,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31),
                Instant.parse("2026-08-15T08:00:00Z"),
                Instant.parse("2026-12-30T18:00:00Z"),
                CampaignStatus.PUBLISHED
        );

        campaign.activate();
        assertEquals(CampaignStatus.ACTIVE, campaign.getStatus());
    }

    @Test
    @DisplayName("activate rejects transition from non-PUBLISHED statuses")
    void activateRejectsNonPublishedStatuses() {
        for (CampaignStatus status : CampaignStatus.values()) {
            if (status == CampaignStatus.PUBLISHED) {
                continue;
            }
            VaccinationCampaign campaign = CampaignTestFixtures.campaign(
                    "CMP-ACT-" + status.name(),
                    "Activation Rejection Test",
                    "Description",
                    sampleVaccineType,
                    sampleTargetGroup,
                    LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 12, 31),
                    Instant.parse("2026-08-15T08:00:00Z"),
                    Instant.parse("2026-12-30T18:00:00Z"),
                    status
            );

            IllegalStateException ex = assertThrows(IllegalStateException.class, campaign::activate);
            assertTrue(ex.getMessage().contains("Cannot activate campaign in " + status));
            assertTrue(ex.getMessage().contains("Only PUBLISHED campaigns can be activated"));
        }
    }

    @Test
    @DisplayName("resumeToPublished transitions campaign from SUSPENDED to PUBLISHED")
    void resumeToPublishedTransitionsFromSuspendedToPublished() {
        VaccinationCampaign campaign = CampaignTestFixtures.campaign(
                "CMP-RES-PUB-01",
                "Resumption to Published Campaign",
                "Description",
                sampleVaccineType,
                sampleTargetGroup,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31),
                Instant.parse("2026-08-15T08:00:00Z"),
                Instant.parse("2026-12-30T18:00:00Z"),
                CampaignStatus.SUSPENDED
        );

        campaign.resumeToPublished();
        assertEquals(CampaignStatus.PUBLISHED, campaign.getStatus());
    }

    @Test
    @DisplayName("resumeToPublished rejects transition from non-SUSPENDED statuses")
    void resumeToPublishedRejectsNonSuspendedStatuses() {
        for (CampaignStatus status : CampaignStatus.values()) {
            if (status == CampaignStatus.SUSPENDED) {
                continue;
            }
            VaccinationCampaign campaign = CampaignTestFixtures.campaign(
                    "CMP-RES-PUB-" + status.name(),
                    "Resumption Rejection Test",
                    "Description",
                    sampleVaccineType,
                    sampleTargetGroup,
                    LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 12, 31),
                    Instant.parse("2026-08-15T08:00:00Z"),
                    Instant.parse("2026-12-30T18:00:00Z"),
                    status
            );

            IllegalStateException ex = assertThrows(IllegalStateException.class, campaign::resumeToPublished);
            assertTrue(ex.getMessage().contains("Cannot resume campaign in " + status));
            assertTrue(ex.getMessage().contains("Only SUSPENDED campaigns can be resumed"));
        }
    }

    @Test
    @DisplayName("resumeToActive transitions campaign from SUSPENDED to ACTIVE")
    void resumeToActiveTransitionsFromSuspendedToActive() {
        VaccinationCampaign campaign = CampaignTestFixtures.campaign(
                "CMP-RES-ACT-01",
                "Resumption to Active Campaign",
                "Description",
                sampleVaccineType,
                sampleTargetGroup,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31),
                Instant.parse("2026-08-15T08:00:00Z"),
                Instant.parse("2026-12-30T18:00:00Z"),
                CampaignStatus.SUSPENDED
        );

        campaign.resumeToActive();
        assertEquals(CampaignStatus.ACTIVE, campaign.getStatus());
    }

    @Test
    @DisplayName("resumeToActive rejects transition from non-SUSPENDED statuses")
    void resumeToActiveRejectsNonSuspendedStatuses() {
        for (CampaignStatus status : CampaignStatus.values()) {
            if (status == CampaignStatus.SUSPENDED) {
                continue;
            }
            VaccinationCampaign campaign = CampaignTestFixtures.campaign(
                    "CMP-RES-ACT-" + status.name(),
                    "Resumption Rejection Test",
                    "Description",
                    sampleVaccineType,
                    sampleTargetGroup,
                    LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 12, 31),
                    Instant.parse("2026-08-15T08:00:00Z"),
                    Instant.parse("2026-12-30T18:00:00Z"),
                    status
            );

            IllegalStateException ex = assertThrows(IllegalStateException.class, campaign::resumeToActive);
            assertTrue(ex.getMessage().contains("Cannot resume campaign in " + status));
            assertTrue(ex.getMessage().contains("Only SUSPENDED campaigns can be resumed"));
        }
    }

    @Test
    @DisplayName("complete transitions campaign to COMPLETED from ACTIVE, PUBLISHED, or SUSPENDED")
    void completeTransitionsFromAllowedStatuses() {
        for (CampaignStatus source : new CampaignStatus[]{CampaignStatus.ACTIVE, CampaignStatus.PUBLISHED, CampaignStatus.SUSPENDED}) {
            VaccinationCampaign campaign = CampaignTestFixtures.campaign(
                    "CMP-COMP-" + source.name(),
                    "Complete Campaign Test",
                    "Description",
                    sampleVaccineType,
                    sampleTargetGroup,
                    LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 12, 31),
                    Instant.parse("2026-08-15T08:00:00Z"),
                    Instant.parse("2026-12-30T18:00:00Z"),
                    source
            );

            campaign.complete();
            assertEquals(CampaignStatus.COMPLETED, campaign.getStatus());
        }
    }

    @Test
    @DisplayName("complete rejects transition from DRAFT, SCHEDULED, COMPLETED, CANCELLED, ARCHIVED")
    void completeRejectsDisallowedStatuses() {
        for (CampaignStatus status : new CampaignStatus[]{
                CampaignStatus.DRAFT, CampaignStatus.SCHEDULED, CampaignStatus.COMPLETED, CampaignStatus.CANCELLED, CampaignStatus.ARCHIVED
        }) {
            VaccinationCampaign campaign = CampaignTestFixtures.campaign(
                    "CMP-COMP-REJ-" + status.name(),
                    "Complete Rejection Test",
                    "Description",
                    sampleVaccineType,
                    sampleTargetGroup,
                    LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 12, 31),
                    Instant.parse("2026-08-15T08:00:00Z"),
                    Instant.parse("2026-12-30T18:00:00Z"),
                    status
            );

            IllegalStateException ex = assertThrows(IllegalStateException.class, campaign::complete);
            assertTrue(ex.getMessage().contains("Cannot complete campaign in " + status));
            assertTrue(ex.getMessage().contains("Only ACTIVE, PUBLISHED, or SUSPENDED campaigns can be completed"));
        }
    }

    @Test
    @DisplayName("cancel transitions campaign to CANCELLED from non-terminal statuses")
    void cancelTransitionsFromNonTerminalStatuses() {
        for (CampaignStatus source : new CampaignStatus[]{
                CampaignStatus.DRAFT, CampaignStatus.SCHEDULED, CampaignStatus.PUBLISHED, CampaignStatus.ACTIVE, CampaignStatus.SUSPENDED
        }) {
            VaccinationCampaign campaign = CampaignTestFixtures.campaign(
                    "CMP-CAN-" + source.name(),
                    "Cancel Campaign Test",
                    "Description",
                    sampleVaccineType,
                    sampleTargetGroup,
                    LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 12, 31),
                    Instant.parse("2026-08-15T08:00:00Z"),
                    Instant.parse("2026-12-30T18:00:00Z"),
                    source
            );

            campaign.cancel();
            assertEquals(CampaignStatus.CANCELLED, campaign.getStatus());
        }
    }

    @Test
    @DisplayName("cancel rejects transition from operationally closed statuses (COMPLETED, CANCELLED, ARCHIVED)")
    void cancelRejectsOperationallyClosedStatuses() {
        for (CampaignStatus status : new CampaignStatus[]{
                CampaignStatus.COMPLETED, CampaignStatus.CANCELLED, CampaignStatus.ARCHIVED
        }) {
            VaccinationCampaign campaign = CampaignTestFixtures.campaign(
                    "CMP-CAN-REJ-" + status.name(),
                    "Cancel Rejection Test",
                    "Description",
                    sampleVaccineType,
                    sampleTargetGroup,
                    LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 12, 31),
                    Instant.parse("2026-08-15T08:00:00Z"),
                    Instant.parse("2026-12-30T18:00:00Z"),
                    status
            );

            IllegalStateException ex = assertThrows(IllegalStateException.class, campaign::cancel);
            assertTrue(ex.getMessage().contains("Cannot cancel campaign in " + status));
            assertTrue(ex.getMessage().contains("Campaign is already operationally closed"));
        }
    }

    @Test
    @DisplayName("archive transitions campaign to ARCHIVED from COMPLETED and CANCELLED")
    void archiveTransitionsFromCompletedAndCancelled() {
        for (CampaignStatus source : new CampaignStatus[]{CampaignStatus.COMPLETED, CampaignStatus.CANCELLED}) {
            VaccinationCampaign campaign = CampaignTestFixtures.campaign(
                    "CMP-ARC-" + source.name(),
                    "Archive Campaign Test",
                    "Description",
                    sampleVaccineType,
                    sampleTargetGroup,
                    LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 12, 31),
                    Instant.parse("2026-08-15T08:00:00Z"),
                    Instant.parse("2026-12-30T18:00:00Z"),
                    source
            );

            campaign.archive();
            assertEquals(CampaignStatus.ARCHIVED, campaign.getStatus());
        }
    }

    @Test
    @DisplayName("archive rejects transition from non-archivable statuses (DRAFT, SCHEDULED, PUBLISHED, ACTIVE, SUSPENDED, ARCHIVED)")
    void archiveRejectsNonArchivableStatuses() {
        for (CampaignStatus status : new CampaignStatus[]{
                CampaignStatus.DRAFT, CampaignStatus.SCHEDULED, CampaignStatus.PUBLISHED, CampaignStatus.ACTIVE, CampaignStatus.SUSPENDED, CampaignStatus.ARCHIVED
        }) {
            VaccinationCampaign campaign = CampaignTestFixtures.campaign(
                    "CMP-ARC-REJ-" + status.name(),
                    "Archive Rejection Test",
                    "Description",
                    sampleVaccineType,
                    sampleTargetGroup,
                    LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 12, 31),
                    Instant.parse("2026-08-15T08:00:00Z"),
                    Instant.parse("2026-12-30T18:00:00Z"),
                    status
            );

            IllegalStateException ex = assertThrows(IllegalStateException.class, campaign::archive);
            assertTrue(ex.getMessage().contains("Cannot archive campaign in " + status));
            assertTrue(ex.getMessage().contains("Only COMPLETED or CANCELLED campaigns can be archived"));
        }
    }
}
