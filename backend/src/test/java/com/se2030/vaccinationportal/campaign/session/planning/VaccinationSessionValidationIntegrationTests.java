package com.se2030.vaccinationportal.campaign.session.planning;

import com.se2030.vaccinationportal.campaign.CampaignTestFixtures;


import com.se2030.vaccinationportal.campaign.center.model.CampaignCenterAssignment;
import com.se2030.vaccinationportal.campaign.center.model.CampaignCenterAssignmentStatus;
import com.se2030.vaccinationportal.campaign.core.model.CampaignLocationTarget;
import com.se2030.vaccinationportal.campaign.core.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.center.model.CenterClosure;
import com.se2030.vaccinationportal.campaign.center.model.CenterClosureStatus;
import com.se2030.vaccinationportal.campaign.center.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.core.model.LocationTargetType;
import com.se2030.vaccinationportal.campaign.center.model.OperatingHours;
import com.se2030.vaccinationportal.campaign.reference.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.reference.model.VaccineType;
import com.se2030.vaccinationportal.campaign.center.repository.CampaignCenterAssignmentRepository;
import com.se2030.vaccinationportal.campaign.core.repository.CampaignLocationTargetRepository;
import com.se2030.vaccinationportal.campaign.center.repository.CenterClosureRepository;
import com.se2030.vaccinationportal.campaign.center.repository.OperatingHoursRepository;
import com.se2030.vaccinationportal.campaign.reference.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.core.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.center.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.reference.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.session.planning.validation.VaccinationSessionValidator;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VaccinationSessionValidationIntegrationTests {

    @Autowired private VaccinationSessionValidator validator;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @Autowired private CampaignCenterAssignmentRepository assignmentRepository;
    @Autowired private OperatingHoursRepository operatingHoursRepository;
    @Autowired private CenterClosureRepository closureRepository;
    @Autowired private CampaignLocationTargetRepository locationTargetRepository;

    private VaccinationCampaign campaign;
    private VaccinationCenter center;

    // Base date: Wednesday, October 14, 2026
    private static final LocalDate CAMPAIGN_START = LocalDate.of(2026, 10, 1);
    private static final LocalDate CAMPAIGN_END = LocalDate.of(2026, 10, 31);
    private static final LocalDate SESSION_DATE = LocalDate.of(2026, 10, 14);

    @BeforeEach
    void setUp() {
        VaccineType vaccineType = vaccineTypeRepository.saveAndFlush(
                new VaccineType("VAX-SESS-VAL", "Session Val Vaccine"));
        TargetGroup targetGroup = targetGroupRepository.saveAndFlush(
                new TargetGroup("TG-SESS-VAL", "Session Val Target Group"));

        campaign = campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                "CMP-SESS-VAL", "Session Validation Campaign", "Description",
                vaccineType, targetGroup,
                CAMPAIGN_START, CAMPAIGN_END,
                Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-10-31T23:59:59Z"),
                CampaignStatus.DRAFT));

        center = centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-SESS-VAL", "Session Validation Center", "123 Health Ave", "+94 11 123 4567",
                CenterStatus.ACTIVE, 100, 50, "UTC"));

        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                campaign, center, CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));

        // Wednesday OperatingHours: 08:00 to 17:00
        operatingHoursRepository.saveAndFlush(new OperatingHours(
                center, DayOfWeek.WEDNESDAY, LocalTime.of(8, 0), LocalTime.of(17, 0),
                CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));
    }

    @Test
    void validateSessionSchedule_validSchedule_success() {
        Instant startAt = Instant.parse("2026-10-14T09:00:00Z");
        Instant endAt = Instant.parse("2026-10-14T12:00:00Z");
        Instant bookingCloseAt = Instant.parse("2026-10-14T08:00:00Z");

        assertDoesNotThrow(() -> validator.validateSessionSchedule(
                campaign, center, startAt, endAt, bookingCloseAt));
    }

    @Test
    void validateSessionSchedule_nullArguments_throws() {
        Instant startAt = Instant.parse("2026-10-14T09:00:00Z");
        Instant endAt = Instant.parse("2026-10-14T12:00:00Z");
        Instant bookingCloseAt = Instant.parse("2026-10-14T08:00:00Z");

        assertThrows(IllegalArgumentException.class, () ->
                validator.validateSessionSchedule(null, center, startAt, endAt, bookingCloseAt));
        assertThrows(IllegalArgumentException.class, () ->
                validator.validateSessionSchedule(campaign, null, startAt, endAt, bookingCloseAt));
        assertThrows(IllegalArgumentException.class, () ->
                validator.validateSessionSchedule(campaign, center, null, endAt, bookingCloseAt));
        assertThrows(IllegalArgumentException.class, () ->
                validator.validateSessionSchedule(campaign, center, startAt, null, bookingCloseAt));
        assertThrows(IllegalArgumentException.class, () ->
                validator.validateSessionSchedule(campaign, center, startAt, endAt, null));
    }

    @Test
    void validateSessionSchedule_startNotBeforeEnd_throws() {
        Instant startAt = Instant.parse("2026-10-14T12:00:00Z");
        Instant equalEnd = Instant.parse("2026-10-14T12:00:00Z");
        Instant beforeEnd = Instant.parse("2026-10-14T09:00:00Z");
        Instant bookingCloseAt = Instant.parse("2026-10-14T08:00:00Z");

        assertThrows(IllegalArgumentException.class, () ->
                validator.validateSessionSchedule(campaign, center, startAt, equalEnd, bookingCloseAt));
        assertThrows(IllegalArgumentException.class, () ->
                validator.validateSessionSchedule(campaign, center, startAt, beforeEnd, bookingCloseAt));
    }

    @Test
    void validateSessionSchedule_bookingCloseAfterStart_throws() {
        Instant startAt = Instant.parse("2026-10-14T09:00:00Z");
        Instant endAt = Instant.parse("2026-10-14T12:00:00Z");
        Instant bookingCloseAt = Instant.parse("2026-10-14T09:00:01Z");

        assertThrows(IllegalArgumentException.class, () ->
                validator.validateSessionSchedule(campaign, center, startAt, endAt, bookingCloseAt));
    }

    @Test
    void validateSessionSchedule_bookingCloseEqualsStart_success() {
        Instant startAt = Instant.parse("2026-10-14T09:00:00Z");
        Instant endAt = Instant.parse("2026-10-14T12:00:00Z");
        Instant bookingCloseAt = Instant.parse("2026-10-14T09:00:00Z");

        assertDoesNotThrow(() -> validator.validateSessionSchedule(
                campaign, center, startAt, endAt, bookingCloseAt));
    }

    @Test
    void validateSessionSchedule_inactiveCenter_throws() {
        VaccinationCenter inactiveCenter = centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-INACTIVE", "Inactive Center", "456 Closed St", "+94 11 999 9999",
                CenterStatus.INACTIVE, 50, 25, "UTC"));

        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                campaign, inactiveCenter, CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));
        operatingHoursRepository.saveAndFlush(new OperatingHours(
                inactiveCenter, DayOfWeek.WEDNESDAY, LocalTime.of(8, 0), LocalTime.of(17, 0),
                CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));

        Instant startAt = Instant.parse("2026-10-14T09:00:00Z");
        Instant endAt = Instant.parse("2026-10-14T12:00:00Z");
        Instant bookingCloseAt = Instant.parse("2026-10-14T08:00:00Z");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                validator.validateSessionSchedule(campaign, inactiveCenter, startAt, endAt, bookingCloseAt));
        assertTrue(ex.getMessage().contains("ACTIVE"));
    }

    @Test
    void validateSessionSchedule_beforeCampaignServiceStartDate_throws() {
        // Campaign service starts 2026-10-01 (Thursday). Try 2026-09-30 (Wednesday)
        operatingHoursRepository.saveAndFlush(new OperatingHours(
                center, DayOfWeek.WEDNESDAY, LocalTime.of(8, 0), LocalTime.of(17, 0),
                LocalDate.of(2026, 9, 1), CAMPAIGN_END.plusDays(1)));
        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                campaign, center, LocalDate.of(2026, 9, 1), CAMPAIGN_END.plusDays(1)));

        Instant startAt = Instant.parse("2026-09-30T09:00:00Z");
        Instant endAt = Instant.parse("2026-09-30T12:00:00Z");
        Instant bookingCloseAt = Instant.parse("2026-09-30T08:00:00Z");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                validator.validateSessionSchedule(campaign, center, startAt, endAt, bookingCloseAt));
        assertTrue(ex.getMessage().contains("before campaign service start date"));
    }

    @Test
    void validateSessionSchedule_afterCampaignServiceEndDate_throws() {
        // Campaign service ends 2026-10-31 (Saturday). Try 2026-11-04 (Wednesday)
        operatingHoursRepository.saveAndFlush(new OperatingHours(
                center, DayOfWeek.WEDNESDAY, LocalTime.of(8, 0), LocalTime.of(17, 0),
                CAMPAIGN_START, LocalDate.of(2026, 11, 30)));
        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                campaign, center, CAMPAIGN_START, LocalDate.of(2026, 11, 30)));

        Instant startAt = Instant.parse("2026-11-04T09:00:00Z");
        Instant endAt = Instant.parse("2026-11-04T12:00:00Z");
        Instant bookingCloseAt = Instant.parse("2026-11-04T08:00:00Z");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                validator.validateSessionSchedule(campaign, center, startAt, endAt, bookingCloseAt));
        assertTrue(ex.getMessage().contains("after campaign service end date"));
    }

    @Test
    void validateSessionSchedule_exactCampaignBoundaryDates_success() {
        // 2026-10-01 (Thursday) is CAMPAIGN_START
        operatingHoursRepository.saveAndFlush(new OperatingHours(
                center, DayOfWeek.THURSDAY, LocalTime.of(8, 0), LocalTime.of(17, 0),
                CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));
        Instant startFirst = Instant.parse("2026-10-01T08:00:00Z");
        Instant endFirst = Instant.parse("2026-10-01T17:00:00Z");
        assertDoesNotThrow(() -> validator.validateSessionSchedule(
                campaign, center, startFirst, endFirst, startFirst));

        // 2026-10-31 (Saturday) is CAMPAIGN_END
        operatingHoursRepository.saveAndFlush(new OperatingHours(
                center, DayOfWeek.SATURDAY, LocalTime.of(8, 0), LocalTime.of(17, 0),
                CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));
        Instant startLast = Instant.parse("2026-10-31T08:00:00Z");
        Instant endLast = Instant.parse("2026-10-31T17:00:00Z");
        assertDoesNotThrow(() -> validator.validateSessionSchedule(
                campaign, center, startLast, endLast, startLast));
    }

    @Test
    void validateSessionSchedule_missingAssignment_throws() {
        VaccinationCampaign unassignedCampaign = campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                "CMP-NO-ASSIGN", "No Assign Campaign", "Desc",
                campaign.getVaccineType(), campaign.getTargetGroup(),
                CAMPAIGN_START, CAMPAIGN_END,
                Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-10-31T23:59:59Z"),
                CampaignStatus.DRAFT));

        Instant startAt = Instant.parse("2026-10-14T09:00:00Z");
        Instant endAt = Instant.parse("2026-10-14T12:00:00Z");
        Instant bookingCloseAt = Instant.parse("2026-10-14T08:00:00Z");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                validator.validateSessionSchedule(unassignedCampaign, center, startAt, endAt, bookingCloseAt));
        assertTrue(ex.getMessage().contains("not authorized for campaign"));
    }

    @Test
    void validateSessionSchedule_cancelledAssignment_throws() {
        VaccinationCampaign otherCampaign = campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                "CMP-CANC-ASSIGN", "Cancelled Assign Campaign", "Desc",
                campaign.getVaccineType(), campaign.getTargetGroup(),
                CAMPAIGN_START, CAMPAIGN_END,
                Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-10-31T23:59:59Z"),
                CampaignStatus.DRAFT));

        CampaignCenterAssignment cancelledAssignment = new CampaignCenterAssignment(
                otherCampaign, center, CAMPAIGN_START, CAMPAIGN_END.plusDays(1));
        cancelledAssignment.cancel();
        assignmentRepository.saveAndFlush(cancelledAssignment);

        Instant startAt = Instant.parse("2026-10-14T09:00:00Z");
        Instant endAt = Instant.parse("2026-10-14T12:00:00Z");
        Instant bookingCloseAt = Instant.parse("2026-10-14T08:00:00Z");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                validator.validateSessionSchedule(otherCampaign, center, startAt, endAt, bookingCloseAt));
        assertTrue(ex.getMessage().contains("not authorized for campaign"));
    }

    @Test
    void validateSessionSchedule_assignmentStartsAfterSession_throws() {
        VaccinationCampaign otherCampaign = campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                "CMP-LATE-ASSIGN", "Late Assign Campaign", "Desc",
                campaign.getVaccineType(), campaign.getTargetGroup(),
                CAMPAIGN_START, CAMPAIGN_END,
                Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-10-31T23:59:59Z"),
                CampaignStatus.DRAFT));

        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                otherCampaign, center, LocalDate.of(2026, 10, 15), CAMPAIGN_END.plusDays(1)));

        // Session on Oct 14 is before assignment start Oct 15
        Instant startAt = Instant.parse("2026-10-14T09:00:00Z");
        Instant endAt = Instant.parse("2026-10-14T12:00:00Z");
        Instant bookingCloseAt = Instant.parse("2026-10-14T08:00:00Z");

        assertThrows(IllegalArgumentException.class, () ->
                validator.validateSessionSchedule(otherCampaign, center, startAt, endAt, bookingCloseAt));
    }

    @Test
    void validateSessionSchedule_assignmentEndsBeforeSession_throws() {
        VaccinationCampaign otherCampaign = campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                "CMP-EARLY-END", "Early End Assign Campaign", "Desc",
                campaign.getVaccineType(), campaign.getTargetGroup(),
                CAMPAIGN_START, CAMPAIGN_END,
                Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-10-31T23:59:59Z"),
                CampaignStatus.DRAFT));

        // activeTo is exclusive, so activeTo = Oct 14 covers only up to Oct 13
        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                otherCampaign, center, CAMPAIGN_START, LocalDate.of(2026, 10, 14)));

        Instant startAt = Instant.parse("2026-10-14T09:00:00Z");
        Instant endAt = Instant.parse("2026-10-14T12:00:00Z");
        Instant bookingCloseAt = Instant.parse("2026-10-14T08:00:00Z");

        assertThrows(IllegalArgumentException.class, () ->
                validator.validateSessionSchedule(otherCampaign, center, startAt, endAt, bookingCloseAt));
    }

    @Test
    void validateSessionSchedule_openEndedAssignment_success() {
        VaccinationCampaign openCampaign = campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                "CMP-OPEN-ASSIGN", "Open Assign Campaign", "Desc",
                campaign.getVaccineType(), campaign.getTargetGroup(),
                CAMPAIGN_START, CAMPAIGN_END,
                Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-10-31T23:59:59Z"),
                CampaignStatus.DRAFT));

        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                openCampaign, center, CAMPAIGN_START, null));

        Instant startAt = Instant.parse("2026-10-14T09:00:00Z");
        Instant endAt = Instant.parse("2026-10-14T12:00:00Z");
        Instant bookingCloseAt = Instant.parse("2026-10-14T08:00:00Z");

        assertDoesNotThrow(() -> validator.validateSessionSchedule(
                openCampaign, center, startAt, endAt, bookingCloseAt));
    }

    @Test
    void validateSessionSchedule_administrativeTargetWithoutAssignment_throws() {
        VaccinationCampaign targetOnlyCampaign = campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                "CMP-TARGET-ONLY", "Target Only Campaign", "Desc",
                campaign.getVaccineType(), campaign.getTargetGroup(),
                CAMPAIGN_START, CAMPAIGN_END,
                Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-10-31T23:59:59Z"),
                CampaignStatus.DRAFT));

        locationTargetRepository.saveAndFlush(new CampaignLocationTarget(
                targetOnlyCampaign, LocationTargetType.PROVINCE, "Western Province"));

        Instant startAt = Instant.parse("2026-10-14T09:00:00Z");
        Instant endAt = Instant.parse("2026-10-14T12:00:00Z");
        Instant bookingCloseAt = Instant.parse("2026-10-14T08:00:00Z");

        assertThrows(IllegalArgumentException.class, () ->
                validator.validateSessionSchedule(targetOnlyCampaign, center, startAt, endAt, bookingCloseAt));
    }

    @Test
    void validateSessionSchedule_exactOperatingHoursBoundaries_success() {
        // Shift is 08:00 to 17:00
        Instant startAt = Instant.parse("2026-10-14T08:00:00Z");
        Instant endAt = Instant.parse("2026-10-14T17:00:00Z");
        Instant bookingCloseAt = Instant.parse("2026-10-14T08:00:00Z");

        assertDoesNotThrow(() -> validator.validateSessionSchedule(
                campaign, center, startAt, endAt, bookingCloseAt));
    }

    @Test
    void validateSessionSchedule_startBeforeOperatingHours_throws() {
        // Shift opens 08:00. Session starts 07:30
        Instant startAt = Instant.parse("2026-10-14T07:30:00Z");
        Instant endAt = Instant.parse("2026-10-14T12:00:00Z");
        Instant bookingCloseAt = Instant.parse("2026-10-14T07:00:00Z");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                validator.validateSessionSchedule(campaign, center, startAt, endAt, bookingCloseAt));
        assertTrue(ex.getMessage().contains("before center opening time"));
    }

    @Test
    void validateSessionSchedule_endAfterOperatingHours_throws() {
        // Shift closes 17:00. Session ends 17:30
        Instant startAt = Instant.parse("2026-10-14T09:00:00Z");
        Instant endAt = Instant.parse("2026-10-14T17:30:00Z");
        Instant bookingCloseAt = Instant.parse("2026-10-14T08:00:00Z");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                validator.validateSessionSchedule(campaign, center, startAt, endAt, bookingCloseAt));
        assertTrue(ex.getMessage().contains("extends beyond center operating hours"));
    }

    @Test
    void validateSessionSchedule_noOperatingHoursOnWeekday_throws() {
        // Thursday has no operating hours configured yet
        Instant startAt = Instant.parse("2026-10-15T09:00:00Z");
        Instant endAt = Instant.parse("2026-10-15T12:00:00Z");
        Instant bookingCloseAt = Instant.parse("2026-10-15T08:00:00Z");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                validator.validateSessionSchedule(campaign, center, startAt, endAt, bookingCloseAt));
        assertTrue(ex.getMessage().contains("No operating hours configured"));
    }

    @Test
    void validateSessionSchedule_adjacentOperatingHoursShifts_success() {
        VaccinationCenter shiftCenter = centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-ADJACENT", "Adjacent Shifts Center", "789 Shift St", "+94 11 777 7777",
                CenterStatus.ACTIVE, 100, 50, "UTC"));
        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                campaign, shiftCenter, CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));

        // Shift 1: 08:00 to 12:00, Shift 2: 12:00 to 16:00
        operatingHoursRepository.saveAndFlush(new OperatingHours(
                shiftCenter, DayOfWeek.WEDNESDAY, LocalTime.of(8, 0), LocalTime.of(12, 0),
                CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));
        operatingHoursRepository.saveAndFlush(new OperatingHours(
                shiftCenter, DayOfWeek.WEDNESDAY, LocalTime.of(12, 0), LocalTime.of(16, 0),
                CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));

        // Session spanning across the shift boundary (09:00 to 15:00)
        Instant startAt = Instant.parse("2026-10-14T09:00:00Z");
        Instant endAt = Instant.parse("2026-10-14T15:00:00Z");
        Instant bookingCloseAt = Instant.parse("2026-10-14T08:00:00Z");

        assertDoesNotThrow(() -> validator.validateSessionSchedule(
                campaign, shiftCenter, startAt, endAt, bookingCloseAt));
    }

    @Test
    void validateSessionSchedule_splitOperatingHoursWithGap_throws() {
        VaccinationCenter gapCenter = centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-GAP", "Gap Shifts Center", "321 Gap St", "+94 11 888 8888",
                CenterStatus.ACTIVE, 100, 50, "UTC"));
        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                campaign, gapCenter, CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));

        // Shift 1: 08:00 to 12:00, Shift 2: 13:00 to 17:00 (12:00-13:00 gap)
        operatingHoursRepository.saveAndFlush(new OperatingHours(
                gapCenter, DayOfWeek.WEDNESDAY, LocalTime.of(8, 0), LocalTime.of(12, 0),
                CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));
        operatingHoursRepository.saveAndFlush(new OperatingHours(
                gapCenter, DayOfWeek.WEDNESDAY, LocalTime.of(13, 0), LocalTime.of(17, 0),
                CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));

        // Session spanning the gap (09:00 to 15:00)
        Instant startAt = Instant.parse("2026-10-14T09:00:00Z");
        Instant endAt = Instant.parse("2026-10-14T15:00:00Z");
        Instant bookingCloseAt = Instant.parse("2026-10-14T08:00:00Z");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                validator.validateSessionSchedule(campaign, gapCenter, startAt, endAt, bookingCloseAt));
        assertTrue(ex.getMessage().contains("gap in operating hours"));
    }

    @Test
    void validateSessionSchedule_splitOperatingHoursSessionInsideOneShift_success() {
        VaccinationCenter gapCenter = centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-SPLIT-OK", "Split Shifts Center", "321 Gap St", "+94 11 888 8888",
                CenterStatus.ACTIVE, 100, 50, "UTC"));
        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                campaign, gapCenter, CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));

        operatingHoursRepository.saveAndFlush(new OperatingHours(
                gapCenter, DayOfWeek.WEDNESDAY, LocalTime.of(8, 0), LocalTime.of(12, 0),
                CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));
        operatingHoursRepository.saveAndFlush(new OperatingHours(
                gapCenter, DayOfWeek.WEDNESDAY, LocalTime.of(13, 0), LocalTime.of(17, 0),
                CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));

        // Session strictly inside Shift 1 (09:00 to 11:30)
        Instant startAt = Instant.parse("2026-10-14T09:00:00Z");
        Instant endAt = Instant.parse("2026-10-14T11:30:00Z");
        Instant bookingCloseAt = Instant.parse("2026-10-14T08:00:00Z");

        assertDoesNotThrow(() -> validator.validateSessionSchedule(
                campaign, gapCenter, startAt, endAt, bookingCloseAt));
    }

    @Test
    void validateSessionSchedule_ineffectiveOperatingHours_throws() {
        VaccinationCenter expCenter = centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-EXP-HOURS", "Expired Hours Center", "555 Exp St", "+94 11 555 5555",
                CenterStatus.ACTIVE, 100, 50, "UTC"));
        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                campaign, expCenter, CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));

        // Operating hours expired on 2026-10-10
        operatingHoursRepository.saveAndFlush(new OperatingHours(
                expCenter, DayOfWeek.WEDNESDAY, LocalTime.of(8, 0), LocalTime.of(17, 0),
                CAMPAIGN_START, LocalDate.of(2026, 10, 10)));

        // Session on 2026-10-14
        Instant startAt = Instant.parse("2026-10-14T09:00:00Z");
        Instant endAt = Instant.parse("2026-10-14T12:00:00Z");
        Instant bookingCloseAt = Instant.parse("2026-10-14T08:00:00Z");

        assertThrows(IllegalArgumentException.class, () ->
                validator.validateSessionSchedule(campaign, expCenter, startAt, endAt, bookingCloseAt));
    }

    @Test
    void validateSessionSchedule_activeCenterClosureOverlap_throws() {
        closureRepository.saveAndFlush(new CenterClosure(
                center, Instant.parse("2026-10-14T10:00:00Z"), Instant.parse("2026-10-14T11:00:00Z"),
                "Emergency sanitation"));

        Instant startAt = Instant.parse("2026-10-14T09:00:00Z");
        Instant endAt = Instant.parse("2026-10-14T12:00:00Z");
        Instant bookingCloseAt = Instant.parse("2026-10-14T08:00:00Z");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                validator.validateSessionSchedule(campaign, center, startAt, endAt, bookingCloseAt));
        assertTrue(ex.getMessage().contains("overlaps an active center closure"));
    }

    @Test
    void validateSessionSchedule_sessionContainsClosure_throws() {
        closureRepository.saveAndFlush(new CenterClosure(
                center, Instant.parse("2026-10-14T10:00:00Z"), Instant.parse("2026-10-14T11:00:00Z"),
                "Deep clean"));

        Instant startAt = Instant.parse("2026-10-14T09:00:00Z");
        Instant endAt = Instant.parse("2026-10-14T15:00:00Z");
        Instant bookingCloseAt = Instant.parse("2026-10-14T08:00:00Z");

        assertThrows(IllegalArgumentException.class, () ->
                validator.validateSessionSchedule(campaign, center, startAt, endAt, bookingCloseAt));
    }

    @Test
    void validateSessionSchedule_closureContainsSession_throws() {
        closureRepository.saveAndFlush(new CenterClosure(
                center, Instant.parse("2026-10-14T08:00:00Z"), Instant.parse("2026-10-14T18:00:00Z"),
                "Full day closure"));

        Instant startAt = Instant.parse("2026-10-14T09:00:00Z");
        Instant endAt = Instant.parse("2026-10-14T12:00:00Z");
        Instant bookingCloseAt = Instant.parse("2026-10-14T08:00:00Z");

        assertThrows(IllegalArgumentException.class, () ->
                validator.validateSessionSchedule(campaign, center, startAt, endAt, bookingCloseAt));
    }

    @Test
    void validateSessionSchedule_adjacentToClosure_success() {
        // Closure is 12:00 to 14:00
        closureRepository.saveAndFlush(new CenterClosure(
                center, Instant.parse("2026-10-14T12:00:00Z"), Instant.parse("2026-10-14T14:00:00Z"),
                "Lunch break deep clean"));

        // Session 1: 09:00 to 12:00 (ends exactly when closure starts)
        Instant startBefore = Instant.parse("2026-10-14T09:00:00Z");
        Instant endBefore = Instant.parse("2026-10-14T12:00:00Z");
        assertDoesNotThrow(() -> validator.validateSessionSchedule(
                campaign, center, startBefore, endBefore, startBefore));

        // Session 2: 14:00 to 17:00 (starts exactly when closure ends)
        Instant startAfter = Instant.parse("2026-10-14T14:00:00Z");
        Instant endAfter = Instant.parse("2026-10-14T17:00:00Z");
        assertDoesNotThrow(() -> validator.validateSessionSchedule(
                campaign, center, startAfter, endAfter, startAfter));
    }

    @Test
    void validateSessionSchedule_cancelledClosure_success() {
        CenterClosure cancelledClosure = new CenterClosure(
                center, Instant.parse("2026-10-14T10:00:00Z"), Instant.parse("2026-10-14T11:00:00Z"),
                "Cancelled closure");
        cancelledClosure.cancel();
        closureRepository.saveAndFlush(cancelledClosure);

        Instant startAt = Instant.parse("2026-10-14T09:00:00Z");
        Instant endAt = Instant.parse("2026-10-14T12:00:00Z");
        Instant bookingCloseAt = Instant.parse("2026-10-14T08:00:00Z");

        assertDoesNotThrow(() -> validator.validateSessionSchedule(
                campaign, center, startAt, endAt, bookingCloseAt));
    }

    @Test
    void validateSessionSchedule_nonUtcCenterTimezone_usesCenterLocalTime() {
        // Center with Asia/Colombo (UTC+05:30)
        VaccinationCenter colomboCenter = centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-COLOMBO", "Colombo Center", "Colombo 07", "+94 11 222 3333",
                CenterStatus.ACTIVE, 100, 50, "Asia/Colombo"));

        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                campaign, colomboCenter, CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));

        // Wednesday OperatingHours in Colombo time: 08:00 to 17:00
        operatingHoursRepository.saveAndFlush(new OperatingHours(
                colomboCenter, DayOfWeek.WEDNESDAY, LocalTime.of(8, 0), LocalTime.of(17, 0),
                CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));

        // UTC 2026-10-14T03:30:00Z is 09:00:00 in Asia/Colombo (Wednesday)
        // UTC 2026-10-14T06:30:00Z is 12:00:00 in Asia/Colombo (Wednesday)
        Instant startAt = Instant.parse("2026-10-14T03:30:00Z");
        Instant endAt = Instant.parse("2026-10-14T06:30:00Z");
        Instant bookingCloseAt = Instant.parse("2026-10-14T02:30:00Z");

        assertDoesNotThrow(() -> validator.validateSessionSchedule(
                campaign, colomboCenter, startAt, endAt, bookingCloseAt));

        // If we attempt a session whose Colombo time is outside operating hours:
        // UTC 2026-10-14T01:00:00Z is 06:30:00 in Colombo (before 08:00 opening)
        Instant earlyStart = Instant.parse("2026-10-14T01:00:00Z");
        Instant earlyEnd = Instant.parse("2026-10-14T04:00:00Z");
        assertThrows(IllegalArgumentException.class, () ->
                validator.validateSessionSchedule(campaign, colomboCenter, earlyStart, earlyEnd, earlyStart));
    }
}
