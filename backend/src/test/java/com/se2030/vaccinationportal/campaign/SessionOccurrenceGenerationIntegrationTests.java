package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CampaignCenterAssignment;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterClosure;
import com.se2030.vaccinationportal.campaign.model.CenterClosureStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.OperatingHours;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.SessionRecurrenceRule;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.CampaignCenterAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.CenterClosureRepository;
import com.se2030.vaccinationportal.campaign.repository.OperatingHoursRepository;
import com.se2030.vaccinationportal.campaign.repository.SessionRecurrenceRuleRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.service.RecurrenceGenerationResult;
import com.se2030.vaccinationportal.campaign.service.SessionOccurrenceGenerationService;
import com.se2030.vaccinationportal.campaign.service.SessionRecurrenceRuleService;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SessionOccurrenceGenerationIntegrationTests {

    @Autowired private SessionOccurrenceGenerationService generationService;
    @Autowired private SessionRecurrenceRuleService ruleService;
    @Autowired private SessionRecurrenceRuleRepository ruleRepository;
    @Autowired private VaccinationSessionRepository sessionRepository;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @Autowired private CampaignCenterAssignmentRepository assignmentRepository;
    @Autowired private OperatingHoursRepository operatingHoursRepository;
    @Autowired private CenterClosureRepository closureRepository;

    private VaccinationCampaign campaign;
    private VaccinationCenter center;
    private VaccineType vaccineType;
    private TargetGroup targetGroup;

    // October 2026 test window
    // Oct 1 = Thursday, Oct 2 = Friday, Oct 5 = Monday, Oct 7 = Wednesday, Oct 14 = Wednesday, Oct 31 = Saturday
    private static final LocalDate CAMPAIGN_START = LocalDate.of(2026, 10, 1);
    private static final LocalDate CAMPAIGN_END = LocalDate.of(2026, 10, 31);
    private static final LocalTime MORNING_START = LocalTime.of(9, 0);
    private static final LocalTime MORNING_END = LocalTime.of(12, 0);

    @BeforeEach
    void setUp() {
        vaccineType = vaccineTypeRepository.saveAndFlush(
                new VaccineType("VAX-GEN-TEST", "Recurrence Gen Vaccine"));
        targetGroup = targetGroupRepository.saveAndFlush(
                new TargetGroup("TG-GEN-TEST", "Recurrence Gen Target Group"));

        // Campaign with booking close set distinctly to Oct 20 (before campaign service end)
        campaign = campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                "CMP-GEN-TEST", "Recurrence Gen Campaign", "Description",
                vaccineType, targetGroup,
                CAMPAIGN_START, CAMPAIGN_END,
                Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-10-20T18:00:00Z"),
                CampaignStatus.DRAFT));

        center = centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-GEN-TEST", "Recurrence Gen Center", "123 Clinic Way", "+94 11 999 8888",
                CenterStatus.ACTIVE, 100, 50, "UTC"));

        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                campaign, center, CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));

        // Setup operating hours for all weekdays 08:00 - 17:00
        for (DayOfWeek day : DayOfWeek.values()) {
            operatingHoursRepository.saveAndFlush(new OperatingHours(
                    center, day, LocalTime.of(8, 0), LocalTime.of(17, 0),
                    CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));
        }
    }

    @Test
    void generatesCompleteConfiguredRuleOccurrencesInChronologicalOrder() {
        // Wednesdays in Oct 2026: Oct 7, 14, 21, 28 (4 occurrences)
        SessionRecurrenceRule rule = ruleService.createRule(
                campaign.getId(), center.getId(), Set.of(DayOfWeek.WEDNESDAY),
                CAMPAIGN_START, CAMPAIGN_END, MORNING_START, MORNING_END, "UTC");

        RecurrenceGenerationResult result = generationService.generateOccurrences(rule.getId());

        assertEquals(4, result.createdCount());
        assertEquals(0, result.existingCount());
        assertEquals(4, result.sessions().size());

        List<LocalDate> expectedDates = List.of(
                LocalDate.of(2026, 10, 7),
                LocalDate.of(2026, 10, 14),
                LocalDate.of(2026, 10, 21),
                LocalDate.of(2026, 10, 28)
        );

        for (int i = 0; i < result.sessions().size(); i++) {
            VaccinationSession session = result.sessions().get(i);
            LocalDate expectedDate = expectedDates.get(i);

            assertEquals(expectedDate, session.getOriginOccurrenceDate());
            assertEquals(rule.getGenerationKey() + ":" + expectedDate, session.getOccurrenceKey());
            assertEquals(SessionLifecycleStatus.DRAFT, session.getLifecycleStatus());
            assertEquals(rule.getId(), session.getRecurrenceRule().getId());
            assertEquals(campaign.getId(), session.getCampaign().getId());
            assertEquals(center.getId(), session.getCenter().getId());
            assertEquals("UTC", session.getScheduleTimezone());

            // AC-2: bookingCloseAt is exactly startAt
            assertEquals(session.getStartAt(), session.getBookingCloseAt());
            assertEquals(ZonedDateTime.of(expectedDate, MORNING_START, ZoneId.of("UTC")).toInstant(), session.getStartAt());
            assertEquals(ZonedDateTime.of(expectedDate, MORNING_END, ZoneId.of("UTC")).toInstant(), session.getEndAt());
        }

        // Database row count verification
        List<VaccinationSession> persisted = sessionRepository.findByRecurrenceRuleIdOrderByOriginOccurrenceDateAsc(rule.getId());
        assertEquals(4, persisted.size());
    }

    @Test
    void bookingCloseAt_strictlyEqualsStartAt_andDoesNotDeriveFromCampaignBookingClose() {
        // Oct 28 startAt is 2026-10-28T09:00:00Z. Campaign bookingCloseAt was set to 2026-10-20T18:00:00Z.
        // Occurrence bookingCloseAt must be 2026-10-28T09:00:00Z, proving it is NOT capped/derived from campaign bookingCloseAt.
        SessionRecurrenceRule rule = ruleService.createRule(
                campaign.getId(), center.getId(), Set.of(DayOfWeek.WEDNESDAY),
                LocalDate.of(2026, 10, 25), LocalDate.of(2026, 10, 31),
                MORNING_START, MORNING_END, "UTC");

        RecurrenceGenerationResult result = generationService.generateOccurrences(rule.getId());
        assertEquals(1, result.createdCount());
        VaccinationSession session = result.sessions().get(0);

        assertEquals(Instant.parse("2026-10-28T09:00:00Z"), session.getStartAt());
        assertEquals(Instant.parse("2026-10-28T09:00:00Z"), session.getBookingCloseAt());
        assertEquals(session.getStartAt(), session.getBookingCloseAt());
    }

    @Test
    void generatesRequestedSubrangeIntersection() {
        // Rule covers Oct 1 to Oct 31 (Wednesdays: 7, 14, 21, 28)
        SessionRecurrenceRule rule = ruleService.createRule(
                campaign.getId(), center.getId(), Set.of(DayOfWeek.WEDNESDAY),
                CAMPAIGN_START, CAMPAIGN_END, MORNING_START, MORNING_END, "UTC");

        // Request only Oct 10 to Oct 22 (should yield Oct 14 and Oct 21)
        RecurrenceGenerationResult result = generationService.generateOccurrences(
                rule.getId(), LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 22));

        assertEquals(2, result.createdCount());
        assertEquals(0, result.existingCount());
        assertEquals(List.of(LocalDate.of(2026, 10, 14), LocalDate.of(2026, 10, 21)),
                result.sessions().stream().map(VaccinationSession::getOriginOccurrenceDate).toList());
    }

    @Test
    void requestedRangeLargerThanRule_isCappedToRuleRange() {
        // Rule covers Oct 7 to Oct 15 (Wednesdays: Oct 7, Oct 14)
        SessionRecurrenceRule rule = ruleService.createRule(
                campaign.getId(), center.getId(), Set.of(DayOfWeek.WEDNESDAY),
                LocalDate.of(2026, 10, 7), LocalDate.of(2026, 10, 15),
                MORNING_START, MORNING_END, "UTC");

        // Requested: Sep 1 to Nov 30
        RecurrenceGenerationResult result = generationService.generateOccurrences(
                rule.getId(), LocalDate.of(2026, 9, 1), LocalDate.of(2026, 11, 30));

        assertEquals(2, result.createdCount());
        assertEquals(List.of(LocalDate.of(2026, 10, 7), LocalDate.of(2026, 10, 14)),
                result.sessions().stream().map(VaccinationSession::getOriginOccurrenceDate).toList());
    }

    @Test
    void requestedRangeDisjointFromRule_returnsEmptySuccessfulResult() {
        SessionRecurrenceRule rule = ruleService.createRule(
                campaign.getId(), center.getId(), Set.of(DayOfWeek.WEDNESDAY),
                LocalDate.of(2026, 10, 7), LocalDate.of(2026, 10, 15),
                MORNING_START, MORNING_END, "UTC");

        RecurrenceGenerationResult result = generationService.generateOccurrences(
                rule.getId(), LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 31));

        assertEquals(0, result.createdCount());
        assertEquals(0, result.existingCount());
        assertTrue(result.sessions().isEmpty());
    }

    @Test
    void invalidRequestedRange_rejects() {
        SessionRecurrenceRule rule = ruleService.createRule(
                campaign.getId(), center.getId(), Set.of(DayOfWeek.WEDNESDAY),
                CAMPAIGN_START, CAMPAIGN_END, MORNING_START, MORNING_END, "UTC");

        assertThrows(IllegalArgumentException.class, () ->
                generationService.generateOccurrences(rule.getId(), LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 10)));
        assertThrows(IllegalArgumentException.class, () ->
                generationService.generateOccurrences(rule.getId(), null, LocalDate.of(2026, 10, 20)));
        assertThrows(IllegalArgumentException.class, () ->
                generationService.generateOccurrences(rule.getId(), LocalDate.of(2026, 10, 10), null));
        assertThrows(IllegalArgumentException.class, () ->
                generationService.generateOccurrences(null, LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 20)));
    }

    @Test
    void repeatedGeneration_isIdempotent_andDoesNotRecreateOrDuplicateSessions() {
        SessionRecurrenceRule rule = ruleService.createRule(
                campaign.getId(), center.getId(), Set.of(DayOfWeek.WEDNESDAY),
                CAMPAIGN_START, CAMPAIGN_END, MORNING_START, MORNING_END, "UTC");

        // First call
        RecurrenceGenerationResult first = generationService.generateOccurrences(rule.getId());
        assertEquals(4, first.createdCount());
        assertEquals(0, first.existingCount());
        List<Long> firstIds = first.sessions().stream().map(VaccinationSession::getId).toList();

        // Second call on entire rule
        RecurrenceGenerationResult second = generationService.generateOccurrences(rule.getId());
        assertEquals(0, second.createdCount());
        assertEquals(4, second.existingCount());
        assertEquals(4, second.sessions().size());
        List<Long> secondIds = second.sessions().stream().map(VaccinationSession::getId).toList();

        assertEquals(firstIds, secondIds);
        assertEquals(4, sessionRepository.findByRecurrenceRuleIdOrderByOriginOccurrenceDateAsc(rule.getId()).size());
    }

    @Test
    void partialSubrangeGeneration_followedByFullGeneration_createsOnlyMissingOccurrences() {
        SessionRecurrenceRule rule = ruleService.createRule(
                campaign.getId(), center.getId(), Set.of(DayOfWeek.WEDNESDAY),
                CAMPAIGN_START, CAMPAIGN_END, MORNING_START, MORNING_END, "UTC");

        // Generate only Oct 7
        RecurrenceGenerationResult part1 = generationService.generateOccurrences(
                rule.getId(), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 10));
        assertEquals(1, part1.createdCount());
        assertEquals(0, part1.existingCount());

        // Now generate full month (should see 1 existing and 3 created)
        RecurrenceGenerationResult part2 = generationService.generateOccurrences(rule.getId());
        assertEquals(3, part2.createdCount());
        assertEquals(1, part2.existingCount());
        assertEquals(4, part2.sessions().size());
    }

    @Test
    void occurrenceKeys_areStable_andDifferentBetweenRulesOnSameDate() {
        SessionRecurrenceRule ruleA = ruleService.createRule(
                campaign.getId(), center.getId(), Set.of(DayOfWeek.WEDNESDAY),
                LocalDate.of(2026, 10, 7), LocalDate.of(2026, 10, 7),
                MORNING_START, MORNING_END, "UTC");

        SessionRecurrenceRule ruleB = ruleService.createRule(
                campaign.getId(), center.getId(), Set.of(DayOfWeek.WEDNESDAY),
                LocalDate.of(2026, 10, 7), LocalDate.of(2026, 10, 7),
                LocalTime.of(13, 0), LocalTime.of(16, 0), "UTC");

        RecurrenceGenerationResult resultA = generationService.generateOccurrences(ruleA.getId());
        RecurrenceGenerationResult resultB = generationService.generateOccurrences(ruleB.getId());

        assertEquals(1, resultA.createdCount());
        assertEquals(1, resultB.createdCount());

        VaccinationSession sessionA = resultA.sessions().get(0);
        VaccinationSession sessionB = resultB.sessions().get(0);

        assertEquals(ruleA.getGenerationKey() + ":2026-10-07", sessionA.getOccurrenceKey());
        assertEquals(ruleB.getGenerationKey() + ":2026-10-07", sessionB.getOccurrenceKey());
        assertFalse(sessionA.getOccurrenceKey().equals(sessionB.getOccurrenceKey()));
    }

    @Test
    void timezoneMismatchBetweenRuleAndCenter_isRejected() {
        VaccinationCenter colomboCenter = centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-COLOMBO", "Colombo Center", "Address", "+94 11 000 0000",
                CenterStatus.ACTIVE, 100, 50, "Asia/Colombo"));

        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                campaign, colomboCenter, CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));

        operatingHoursRepository.saveAndFlush(new OperatingHours(
                colomboCenter, DayOfWeek.WEDNESDAY, LocalTime.of(8, 0), LocalTime.of(17, 0),
                CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));

        // Create rule with UTC timezone targeting Colombo center
        SessionRecurrenceRule rule = ruleService.createRule(
                campaign.getId(), colomboCenter.getId(), Set.of(DayOfWeek.WEDNESDAY),
                CAMPAIGN_START, CAMPAIGN_END, MORNING_START, MORNING_END, "UTC");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> generationService.generateOccurrences(rule.getId()));
        assertTrue(ex.getMessage().contains("does not match center scheduling timezone"));
    }

    @Test
    void nonUtcTimezone_convertsAccuratelyToInstants() {
        VaccinationCenter colomboCenter = centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-CMB-VALID", "Colombo Valid Center", "Address", "+94 11 111 2222",
                CenterStatus.ACTIVE, 100, 50, "Asia/Colombo"));

        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                campaign, colomboCenter, CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));

        operatingHoursRepository.saveAndFlush(new OperatingHours(
                colomboCenter, DayOfWeek.WEDNESDAY, LocalTime.of(8, 0), LocalTime.of(17, 0),
                CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));

        SessionRecurrenceRule rule = ruleService.createRule(
                campaign.getId(), colomboCenter.getId(), Set.of(DayOfWeek.WEDNESDAY),
                LocalDate.of(2026, 10, 7), LocalDate.of(2026, 10, 7),
                LocalTime.of(9, 0), LocalTime.of(12, 0), "Asia/Colombo");

        RecurrenceGenerationResult result = generationService.generateOccurrences(rule.getId());
        assertEquals(1, result.createdCount());
        VaccinationSession session = result.sessions().get(0);

        // Colombo is UTC+05:30 -> 09:00 local is 03:30 UTC
        assertEquals(Instant.parse("2026-10-07T03:30:00Z"), session.getStartAt());
        assertEquals(Instant.parse("2026-10-07T06:30:00Z"), session.getEndAt());
        assertEquals(Instant.parse("2026-10-07T03:30:00Z"), session.getBookingCloseAt());
        assertEquals("Asia/Colombo", session.getScheduleTimezone());
    }

    @Test
    void dstTransitions_explicitResolution_gapRejects_overlapUsesEarlierOffset() {
        VaccinationCenter nyCenter = centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-NY-DST", "New York Center", "Address", "+1 212 555 1234",
                CenterStatus.ACTIVE, 100, 50, "America/New_York"));

        LocalDate marchStart = LocalDate.of(2026, 3, 1);
        LocalDate marchEnd = LocalDate.of(2026, 3, 31);
        VaccinationCampaign marchCampaign = campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                "CMP-MARCH-DST", "March Campaign", "Description",
                vaccineType, targetGroup,
                marchStart, marchEnd,
                Instant.parse("2026-02-01T00:00:00Z"), Instant.parse("2026-03-31T23:59:59Z"),
                CampaignStatus.DRAFT));

        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                marchCampaign, nyCenter, marchStart, marchEnd.plusDays(1)));

        for (DayOfWeek day : DayOfWeek.values()) {
            operatingHoursRepository.saveAndFlush(new OperatingHours(
                    nyCenter, day, LocalTime.of(0, 0), LocalTime.of(23, 59),
                    marchStart, marchEnd.plusDays(1)));
        }

        // In America/New_York, DST spring forward in 2026 occurs on Sunday March 8 at 02:00 -> clocks jump to 03:00.
        // 02:30:00 local does not exist.
        SessionRecurrenceRule gapRule = ruleService.createRule(
                marchCampaign.getId(), nyCenter.getId(), Set.of(DayOfWeek.SUNDAY),
                LocalDate.of(2026, 3, 8), LocalDate.of(2026, 3, 8),
                LocalTime.of(2, 30), LocalTime.of(3, 30), "America/New_York");

        IllegalArgumentException gapEx = assertThrows(IllegalArgumentException.class,
                () -> generationService.generateOccurrences(gapRule.getId()));
        assertTrue(gapEx.getMessage().contains("due to DST transition gap"));

        // Fall back overlap in America/New_York in 2026 occurs on Sunday November 1 at 02:00 -> clocks jump back to 01:00.
        // 01:30:00 local is ambiguous (UTC-4 vs UTC-5). Earlier offset validOffsets.get(0) is UTC-4.
        LocalDate novStart = LocalDate.of(2026, 11, 1);
        LocalDate novEnd = LocalDate.of(2026, 11, 30);
        VaccinationCampaign novCampaign = campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                "CMP-NOV-DST", "November Campaign", "Description",
                vaccineType, targetGroup,
                novStart, novEnd,
                Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-11-30T23:59:59Z"),
                CampaignStatus.DRAFT));

        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                novCampaign, nyCenter, novStart, novEnd.plusDays(1)));

        for (DayOfWeek day : DayOfWeek.values()) {
            operatingHoursRepository.saveAndFlush(new OperatingHours(
                    nyCenter, day, LocalTime.of(0, 0), LocalTime.of(23, 59),
                    novStart, novEnd.plusDays(1)));
        }

        SessionRecurrenceRule overlapRule = ruleService.createRule(
                novCampaign.getId(), nyCenter.getId(), Set.of(DayOfWeek.SUNDAY),
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 1),
                LocalTime.of(1, 30), LocalTime.of(2, 30), "America/New_York");

        RecurrenceGenerationResult overlapResult = generationService.generateOccurrences(overlapRule.getId());
        assertEquals(1, overlapResult.createdCount());
        VaccinationSession overlapSession = overlapResult.sessions().get(0);

        // 2026-11-01 01:30 in America/New_York with validOffsets.get(0) (EDT UTC-4) is 05:30 UTC
        assertEquals(Instant.parse("2026-11-01T05:30:00Z"), overlapSession.getStartAt());
    }

    @Test
    void validatorReuse_rejectsCandidateViolatingCampaignServiceDates() {
        // Campaign service dates are Oct 1 to Oct 31
        // Create rule extending past campaign service dates (e.g. to Nov 7)
        // Set assignment and operating hours to cover Nov 7 to isolate campaign date violation
        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                campaign, center, CAMPAIGN_START, LocalDate.of(2026, 11, 10)));

        for (DayOfWeek day : DayOfWeek.values()) {
            operatingHoursRepository.saveAndFlush(new OperatingHours(
                    center, day, LocalTime.of(8, 0), LocalTime.of(17, 0),
                    CAMPAIGN_START, LocalDate.of(2026, 11, 10)));
        }

        SessionRecurrenceRule rule = ruleService.createRule(
                campaign.getId(), center.getId(), Set.of(DayOfWeek.SATURDAY),
                LocalDate.of(2026, 10, 31), LocalDate.of(2026, 11, 7),
                MORNING_START, MORNING_END, "UTC");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> generationService.generateOccurrences(rule.getId()));
        assertTrue(ex.getMessage().contains("after campaign service end date"));
    }

    @Test
    void validatorReuse_rejectsCandidateWhenCenterIsInactive() {
        center.deactivate();
        centerRepository.saveAndFlush(center);

        SessionRecurrenceRule rule = ruleService.createRule(
                campaign.getId(), center.getId(), Set.of(DayOfWeek.WEDNESDAY),
                CAMPAIGN_START, CAMPAIGN_END, MORNING_START, MORNING_END, "UTC");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> generationService.generateOccurrences(rule.getId()));
        assertTrue(ex.getMessage().contains("Vaccination center must be ACTIVE"));
    }

    @Test
    void validatorReuse_rejectsCandidateWhenOperatingHoursHaveGap() {
        // Clear existing operating hours and create split shifts with gap: 08:00-10:00 and 11:00-17:00
        operatingHoursRepository.deleteAll();
        operatingHoursRepository.saveAndFlush(new OperatingHours(
                center, DayOfWeek.WEDNESDAY, LocalTime.of(8, 0), LocalTime.of(10, 0),
                CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));
        operatingHoursRepository.saveAndFlush(new OperatingHours(
                center, DayOfWeek.WEDNESDAY, LocalTime.of(11, 0), LocalTime.of(17, 0),
                CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));

        // Morning rule (09:00 - 12:00) overlaps the gap between 10:00 and 11:00
        SessionRecurrenceRule rule = ruleService.createRule(
                campaign.getId(), center.getId(), Set.of(DayOfWeek.WEDNESDAY),
                LocalDate.of(2026, 10, 7), LocalDate.of(2026, 10, 7),
                MORNING_START, MORNING_END, "UTC");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> generationService.generateOccurrences(rule.getId()));
        assertTrue(ex.getMessage().contains("gap in operating hours"));
    }

    @Test
    void validatorReuse_rejectsCandidateOverlappingActiveCenterClosure() {
        closureRepository.saveAndFlush(new CenterClosure(
                center,
                Instant.parse("2026-10-14T00:00:00Z"),
                Instant.parse("2026-10-14T23:59:59Z"),
                "Maintenance"));

        SessionRecurrenceRule rule = ruleService.createRule(
                campaign.getId(), center.getId(), Set.of(DayOfWeek.WEDNESDAY),
                LocalDate.of(2026, 10, 14), LocalDate.of(2026, 10, 14),
                MORNING_START, MORNING_END, "UTC");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> generationService.generateOccurrences(rule.getId()));
        assertTrue(ex.getMessage().contains("overlaps an active center closure"));
    }

    @Test
    void batchAtomicity_failsEntireBatch_ifAnyMissingCandidateIsInvalid() {
        // Wednesdays: Oct 7 (valid), Oct 14 (covered by closure -> invalid)
        closureRepository.saveAndFlush(new CenterClosure(
                center,
                Instant.parse("2026-10-14T00:00:00Z"),
                Instant.parse("2026-10-14T23:59:59Z"),
                "Emergency Cleaning"));

        SessionRecurrenceRule rule = ruleService.createRule(
                campaign.getId(), center.getId(), Set.of(DayOfWeek.WEDNESDAY),
                LocalDate.of(2026, 10, 7), LocalDate.of(2026, 10, 14),
                MORNING_START, MORNING_END, "UTC");

        assertThrows(IllegalArgumentException.class, () -> generationService.generateOccurrences(rule.getId()));

        // Verify that Oct 7 was NOT saved (all-or-nothing atomicity for batch)
        List<VaccinationSession> sessions = sessionRepository.findByRecurrenceRuleIdOrderByOriginOccurrenceDateAsc(rule.getId());
        assertTrue(sessions.isEmpty());
    }

    @Test
    void batchAtomicity_preExistingOccurrencesRemainUntouched_whenSubsequentBatchFails() {
        SessionRecurrenceRule rule = ruleService.createRule(
                campaign.getId(), center.getId(), Set.of(DayOfWeek.WEDNESDAY),
                CAMPAIGN_START, CAMPAIGN_END, MORNING_START, MORNING_END, "UTC");

        // First materialize Oct 7 successfully
        RecurrenceGenerationResult first = generationService.generateOccurrences(
                rule.getId(), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 10));
        assertEquals(1, first.createdCount());
        Long oct7Id = first.sessions().get(0).getId();

        // Now add a closure on Oct 14
        closureRepository.saveAndFlush(new CenterClosure(
                center,
                Instant.parse("2026-10-14T00:00:00Z"),
                Instant.parse("2026-10-14T23:59:59Z"),
                "Renovation"));

        // Attempt generating full month (Oct 7 exists, Oct 14 fails validation)
        assertThrows(IllegalArgumentException.class, () -> generationService.generateOccurrences(rule.getId()));

        // Pre-existing Oct 7 session is still intact
        List<VaccinationSession> remaining = sessionRepository.findByRecurrenceRuleIdOrderByOriginOccurrenceDateAsc(rule.getId());
        assertEquals(1, remaining.size());
        assertEquals(oct7Id, remaining.get(0).getId());
    }

    @Test
    void generationRejectsCancelledRule() {
        SessionRecurrenceRule rule = ruleService.createRule(
                campaign.getId(), center.getId(), Set.of(DayOfWeek.WEDNESDAY),
                CAMPAIGN_START, CAMPAIGN_END, MORNING_START, MORNING_END, "UTC");

        ruleService.cancelRule(rule.getId());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> generationService.generateOccurrences(rule.getId()));
        assertTrue(ex.getMessage().contains("must be ACTIVE"));
    }
}
