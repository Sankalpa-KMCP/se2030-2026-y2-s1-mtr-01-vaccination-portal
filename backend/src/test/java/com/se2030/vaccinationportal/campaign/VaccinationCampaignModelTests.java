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
}
