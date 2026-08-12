package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CampaignCenterAssignment;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterClosure;
import com.se2030.vaccinationportal.campaign.model.CenterClosureStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.OperatingHours;
import com.se2030.vaccinationportal.campaign.model.SeriesOccurrenceCategory;
import com.se2030.vaccinationportal.campaign.model.SessionChangeEvent;
import com.se2030.vaccinationportal.campaign.model.SessionChangeType;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.SessionRecurrenceRule;
import com.se2030.vaccinationportal.campaign.model.SessionScheduleSnapshot;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.CampaignCenterAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.CenterClosureRepository;
import com.se2030.vaccinationportal.campaign.repository.OperatingHoursRepository;
import com.se2030.vaccinationportal.campaign.repository.SessionChangeEventRepository;
import com.se2030.vaccinationportal.campaign.repository.SessionRecurrenceRuleRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.service.RecurrenceGenerationResult;
import com.se2030.vaccinationportal.campaign.service.SeriesChangePreview;
import com.se2030.vaccinationportal.campaign.service.SeriesOccurrencePreviewItem;
import com.se2030.vaccinationportal.campaign.service.SessionChangePreview;
import com.se2030.vaccinationportal.campaign.service.SessionImpactReader;
import com.se2030.vaccinationportal.campaign.service.SessionImpactSummary;
import com.se2030.vaccinationportal.campaign.service.SessionOccurrenceGenerationService;
import com.se2030.vaccinationportal.campaign.service.SessionSeriesChangeService;
import com.se2030.vaccinationportal.campaign.service.VaccinationSessionService;
import com.se2030.vaccinationportal.campaign.validation.VaccinationSessionValidator;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VaccinationSessionChangeIntegrationTests {

    @Autowired private VaccinationSessionService sessionService;
    @Autowired private SessionSeriesChangeService seriesChangeService;
    @Autowired private SessionOccurrenceGenerationService generationService;
    @Autowired private VaccinationSessionRepository sessionRepository;
    @Autowired private SessionChangeEventRepository sessionChangeEventRepository;
    @Autowired private SessionRecurrenceRuleRepository ruleRepository;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @Autowired private CampaignCenterAssignmentRepository assignmentRepository;
    @Autowired private OperatingHoursRepository operatingHoursRepository;
    @Autowired private CenterClosureRepository closureRepository;
    @Autowired private VaccinationSessionValidator validator;
    @Autowired private Clock clock;

    private VaccinationCampaign campaign;
    private VaccinationCenter centerA;
    private VaccinationCenter centerB;
    private VaccineType vaccineType;
    private TargetGroup targetGroup;

    private static final LocalDate CAMPAIGN_START = LocalDate.of(2026, 10, 1);
    private static final LocalDate CAMPAIGN_END = LocalDate.of(2026, 10, 31);
    private static final Instant CAMPAIGN_REG_START = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant CAMPAIGN_REG_END = Instant.parse("2026-10-30T00:00:00Z");

    @BeforeEach
    void setUp() {
        vaccineType = vaccineTypeRepository.saveAndFlush(
                new VaccineType("VAX-CHG-TEST", "Change Workflow Vaccine"));
        targetGroup = targetGroupRepository.saveAndFlush(
                new TargetGroup("TG-CHG-TEST", "Change Workflow Target Group"));

        campaign = campaignRepository.saveAndFlush(new VaccinationCampaign(
                "CMP-CHG-TEST", "Change Workflow Campaign", "Testing session change workflows",
                vaccineType, targetGroup,
                CAMPAIGN_START, CAMPAIGN_END,
                CAMPAIGN_REG_START, CAMPAIGN_REG_END,
                CampaignStatus.DRAFT));

        centerA = centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-CHG-A", "Center Alpha", "100 Alpha Street", "+94 11 111 1111",
                CenterStatus.ACTIVE, 200, 100, "UTC"));

        centerB = centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-CHG-B", "Center Beta", "200 Beta Street", "+94 11 222 2222",
                CenterStatus.ACTIVE, 300, 150, "Europe/London"));

        // Authorize both centers for campaign
        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                campaign, centerA, CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));
        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                campaign, centerB, CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));

        // Configure operating hours 08:00 - 18:00 for all days on both centers
        for (DayOfWeek dow : DayOfWeek.values()) {
            operatingHoursRepository.saveAndFlush(new OperatingHours(
                    centerA, dow, LocalTime.of(8, 0), LocalTime.of(18, 0),
                    CAMPAIGN_START.minusMonths(1), null));
            operatingHoursRepository.saveAndFlush(new OperatingHours(
                    centerB, dow, LocalTime.of(8, 0), LocalTime.of(18, 0),
                    CAMPAIGN_START.minusMonths(1), null));
        }
    }

    private VaccinationSession createDraftSession(Instant startAt, Instant endAt, Instant bookingCloseAt) {
        return sessionRepository.saveAndFlush(new VaccinationSession(
                campaign, centerA, startAt, endAt, bookingCloseAt, centerA.getTimezone()));
    }

    @Test
    void draftReschedulePreviewAndConfirmSucceedsWithEventAudit() {
        Instant originalStart = Instant.parse("2026-10-15T09:00:00Z");
        Instant originalEnd = Instant.parse("2026-10-15T12:00:00Z");
        Instant originalClose = Instant.parse("2026-10-15T08:00:00Z");
        VaccinationSession session = createDraftSession(originalStart, originalEnd, originalClose);

        Instant newStart = Instant.parse("2026-10-15T13:00:00Z");
        Instant newEnd = Instant.parse("2026-10-15T16:00:00Z");

        // 1. Preview
        SessionChangePreview preview = sessionService.previewReschedule(session.getId(), newStart, newEnd, null);
        assertAll(
                () -> assertEquals(session.getId(), preview.sessionId()),
                () -> assertEquals(SessionChangeType.RESCHEDULED, preview.changeType()),
                () -> assertEquals(0, preview.affectedAppointmentCount()),
                () -> assertEquals(0, preview.affectedWaitlistCount()),
                () -> assertEquals(centerA.getId(), preview.newSnapshot().getCenterId()),
                () -> assertEquals(newStart, preview.newSnapshot().getStartAt()),
                () -> assertEquals(newEnd, preview.newSnapshot().getEndAt()),
                () -> assertEquals(originalClose, preview.newSnapshot().getBookingCloseAt()), // preserved because <= newStart
                () -> assertNotNull(preview.previewToken())
        );

        // 2. Confirm
        VaccinationSession confirmed = sessionService.confirmReschedule(
                session.getId(),
                newStart,
                newEnd,
                null,
                preview.previewToken(),
                "Operational schedule shift",
                "ACTOR-CLERK-01"
        );

        // Verify session mutated
        assertEquals(newStart, confirmed.getStartAt());
        assertEquals(newEnd, confirmed.getEndAt());
        assertEquals(originalClose, confirmed.getBookingCloseAt());
        assertEquals(centerA.getId(), confirmed.getCenter().getId());
        assertEquals(SessionLifecycleStatus.DRAFT, confirmed.getLifecycleStatus());

        // Verify SessionChangeEvent persisted
        List<SessionChangeEvent> events = sessionChangeEventRepository.findBySessionIdOrderByCreatedAtAscIdAsc(session.getId());
        assertEquals(1, events.size());
        SessionChangeEvent event = events.get(0);
        assertAll(
                () -> assertEquals(SessionChangeType.RESCHEDULED, event.getChangeType()),
                () -> assertEquals(originalStart, event.getOldSnapshot().getStartAt()),
                () -> assertEquals(newStart, event.getNewSnapshot().getStartAt()),
                () -> assertEquals("Operational schedule shift", event.getReason()),
                () -> assertEquals("ACTOR-CLERK-01", event.getActorId()),
                () -> assertEquals(0, event.getAffectedAppointmentCount()),
                () -> assertEquals(0, event.getAffectedWaitlistCount()),
                () -> assertNotNull(event.getCreatedAt())
        );
    }

    @Test
    void draftCenterTransferUpdatesCenterAndTimezoneWithAudit() {
        Instant originalStart = Instant.parse("2026-10-15T09:00:00Z");
        Instant originalEnd = Instant.parse("2026-10-15T12:00:00Z");
        Instant originalClose = Instant.parse("2026-10-15T08:00:00Z");
        VaccinationSession session = createDraftSession(originalStart, originalEnd, originalClose);

        Instant newStart = Instant.parse("2026-10-15T10:00:00Z");
        Instant newEnd = Instant.parse("2026-10-15T13:00:00Z");

        // 1. Preview center transfer
        SessionChangePreview preview = sessionService.previewCenterTransfer(session.getId(), centerB.getId(), newStart, newEnd, null);
        assertEquals(SessionChangeType.CENTER_TRANSFERRED, preview.changeType());
        assertEquals(centerB.getId(), preview.newSnapshot().getCenterId());
        assertEquals("Europe/London", preview.newSnapshot().getScheduleTimezone());

        // 2. Confirm center transfer
        VaccinationSession confirmed = sessionService.confirmCenterTransfer(
                session.getId(),
                centerB.getId(),
                newStart,
                newEnd,
                null,
                preview.previewToken(),
                "Facility equipment maintenance transfer",
                "ACTOR-ADMIN-99"
        );

        assertEquals(centerB.getId(), confirmed.getCenter().getId());
        assertEquals("Europe/London", confirmed.getScheduleTimezone());
        assertEquals(newStart, confirmed.getStartAt());
        assertEquals(newEnd, confirmed.getEndAt());

        List<SessionChangeEvent> events = sessionChangeEventRepository.findBySessionIdOrderByCreatedAtAscIdAsc(session.getId());
        assertEquals(1, events.size());
        assertEquals(SessionChangeType.CENTER_TRANSFERRED, events.get(0).getChangeType());
        assertEquals(centerA.getId(), events.get(0).getOldSnapshot().getCenterId());
        assertEquals(centerB.getId(), events.get(0).getNewSnapshot().getCenterId());
    }

    @Test
    void draftCancellationTransitionsStatusAndCreatesEventAudit() {
        Instant start = Instant.parse("2026-10-15T09:00:00Z");
        Instant end = Instant.parse("2026-10-15T12:00:00Z");
        Instant close = Instant.parse("2026-10-15T08:00:00Z");
        VaccinationSession session = createDraftSession(start, end, close);

        // 1. Preview cancellation
        SessionChangePreview preview = sessionService.previewCancellation(session.getId());
        assertAll(
                () -> assertEquals(SessionChangeType.CANCELLED, preview.changeType()),
                () -> assertEquals(SessionLifecycleStatus.DRAFT, preview.oldSnapshot().getLifecycleStatus()),
                () -> assertEquals(SessionLifecycleStatus.CANCELLED, preview.newSnapshot().getLifecycleStatus()),
                () -> assertEquals(0, preview.affectedAppointmentCount()),
                () -> assertEquals(0, preview.affectedWaitlistCount())
        );

        // 2. Confirm cancellation
        VaccinationSession cancelled = sessionService.confirmCancellation(
                session.getId(),
                preview.previewToken(),
                "Vaccine shipment delayed",
                "ACTOR-COORD-05"
        );

        assertEquals(SessionLifecycleStatus.CANCELLED, cancelled.getLifecycleStatus());

        List<SessionChangeEvent> events = sessionChangeEventRepository.findBySessionIdOrderByCreatedAtAscIdAsc(session.getId());
        assertEquals(1, events.size());
        assertEquals(SessionChangeType.CANCELLED, events.get(0).getChangeType());
        assertEquals("Vaccine shipment delayed", events.get(0).getReason());
        assertEquals("ACTOR-COORD-05", events.get(0).getActorId());
    }

    @Test
    void scheduledSessionChangeUsesRealImpactReaderAndEnforcesCounts() {
        Instant start = Instant.parse("2026-10-15T09:00:00Z");
        Instant end = Instant.parse("2026-10-15T12:00:00Z");
        Instant close = Instant.parse("2026-10-15T08:00:00Z");
        VaccinationSession session = createDraftSession(start, end, close);
        session.transitionLifecycleStatus(SessionLifecycleStatus.SCHEDULED);
        sessionRepository.saveAndFlush(session);

        // Real test impact reader returning 12 appointments and 4 waitlists
        SessionImpactReader testReader = (id) -> new SessionImpactSummary(12, 4);

        VaccinationSessionService customService = new VaccinationSessionService(
                sessionRepository,
                campaignRepository,
                centerRepository,
                sessionChangeEventRepository,
                validator,
                testReader
        );

        Instant newStart = Instant.parse("2026-10-15T14:00:00Z");
        Instant newEnd = Instant.parse("2026-10-15T17:00:00Z");

        SessionChangePreview preview = customService.previewReschedule(session.getId(), newStart, newEnd, null);
        assertEquals(12, preview.affectedAppointmentCount());
        assertEquals(4, preview.affectedWaitlistCount());

        VaccinationSession confirmed = customService.confirmReschedule(
                session.getId(),
                newStart,
                newEnd,
                null,
                preview.previewToken(),
                "Inclement weather reschedule",
                "ACTOR-LEAD-01"
        );

        assertEquals(newStart, confirmed.getStartAt());

        List<SessionChangeEvent> events = sessionChangeEventRepository.findBySessionIdOrderByCreatedAtAscIdAsc(session.getId());
        assertEquals(1, events.size());
        assertEquals(12, events.get(0).getAffectedAppointmentCount());
        assertEquals(4, events.get(0).getAffectedWaitlistCount());
    }

    @Test
    void scheduledSessionChangeRejectsWhenImpactReaderIsUnavailable() {
        Instant start = Instant.parse("2026-10-15T09:00:00Z");
        Instant end = Instant.parse("2026-10-15T12:00:00Z");
        Instant close = Instant.parse("2026-10-15T08:00:00Z");
        VaccinationSession session = createDraftSession(start, end, close);
        session.transitionLifecycleStatus(SessionLifecycleStatus.SCHEDULED);
        sessionRepository.saveAndFlush(session);

        // Standard service without impact reader
        VaccinationSessionService noReaderService = new VaccinationSessionService(
                sessionRepository,
                campaignRepository,
                centerRepository,
                sessionChangeEventRepository,
                validator,
                null
        );

        Instant newStart = Instant.parse("2026-10-15T14:00:00Z");
        Instant newEnd = Instant.parse("2026-10-15T17:00:00Z");

        assertThrows(IllegalStateException.class,
                () -> noReaderService.previewReschedule(session.getId(), newStart, newEnd, null));

        assertThrows(IllegalStateException.class,
                () -> noReaderService.previewCancellation(session.getId()));

        assertThrows(IllegalStateException.class,
                () -> noReaderService.confirmReschedule(session.getId(), newStart, newEnd, null, "dummy-token", "Reason", "ACTOR"));

        assertThrows(IllegalStateException.class,
                () -> noReaderService.confirmCancellation(session.getId(), "dummy-token", "Reason", "ACTOR"));
    }

    @Test
    void stalePreviewRejectionProtectsAgainstConcurrentModificationsAndTampering() {
        Instant start = Instant.parse("2026-10-15T09:00:00Z");
        Instant end = Instant.parse("2026-10-15T12:00:00Z");
        Instant close = Instant.parse("2026-10-15T08:00:00Z");
        VaccinationSession session = createDraftSession(start, end, close);

        Instant newStart1 = Instant.parse("2026-10-15T13:00:00Z");
        Instant newEnd1 = Instant.parse("2026-10-15T16:00:00Z");
        SessionChangePreview preview1 = sessionService.previewReschedule(session.getId(), newStart1, newEnd1, null);

        // 1. Tampered / wrong token
        assertThrows(IllegalStateException.class, () -> sessionService.confirmReschedule(
                session.getId(), newStart1, newEnd1, null, "invalid-tampered-token", "Reason", "ACTOR"));

        // 2. Cancellation token used for reschedule confirm
        SessionChangePreview cancelPreview = sessionService.previewCancellation(session.getId());
        assertThrows(IllegalStateException.class, () -> sessionService.confirmReschedule(
                session.getId(), newStart1, newEnd1, null, cancelPreview.previewToken(), "Reason", "ACTOR"));

        // 3. Underlying session modified after preview1 was generated
        Instant newStart2 = Instant.parse("2026-10-15T14:00:00Z");
        Instant newEnd2 = Instant.parse("2026-10-15T17:00:00Z");
        SessionChangePreview preview2 = sessionService.previewReschedule(session.getId(), newStart2, newEnd2, null);
        sessionService.confirmReschedule(session.getId(), newStart2, newEnd2, null, preview2.previewToken(), "Intermediate change", "ACTOR-1");

        // Attempting to confirm preview1 now must fail because current state changed
        assertThrows(IllegalStateException.class, () -> sessionService.confirmReschedule(
                session.getId(), newStart1, newEnd1, null, preview1.previewToken(), "Attempting stale confirmation", "ACTOR-2"));
    }

    @Test
    void reasonAndActorIdValidationEnforcesDomainConstraints() {
        Instant start = Instant.parse("2026-10-15T09:00:00Z");
        Instant end = Instant.parse("2026-10-15T12:00:00Z");
        Instant close = Instant.parse("2026-10-15T08:00:00Z");
        VaccinationSession session = createDraftSession(start, end, close);

        Instant newStart = Instant.parse("2026-10-15T13:00:00Z");
        Instant newEnd = Instant.parse("2026-10-15T16:00:00Z");
        SessionChangePreview preview = sessionService.previewReschedule(session.getId(), newStart, newEnd, null);

        assertAll(
                // Blank reason
                () -> assertThrows(IllegalArgumentException.class,
                        () -> sessionService.confirmReschedule(session.getId(), newStart, newEnd, null, preview.previewToken(), null, "ACTOR")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> sessionService.confirmReschedule(session.getId(), newStart, newEnd, null, preview.previewToken(), "   ", "ACTOR")),
                // Reason > 500 chars
                () -> assertThrows(IllegalArgumentException.class,
                        () -> sessionService.confirmReschedule(session.getId(), newStart, newEnd, null, preview.previewToken(), "R".repeat(501), "ACTOR")),
                // Blank actor ID
                () -> assertThrows(IllegalArgumentException.class,
                        () -> sessionService.confirmReschedule(session.getId(), newStart, newEnd, null, preview.previewToken(), "Valid Reason", null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> sessionService.confirmReschedule(session.getId(), newStart, newEnd, null, preview.previewToken(), "Valid Reason", "  ")),
                // Actor ID > 64 chars
                () -> assertThrows(IllegalArgumentException.class,
                        () -> sessionService.confirmReschedule(session.getId(), newStart, newEnd, null, preview.previewToken(), "Valid Reason", "A".repeat(65)))
        );
    }

    @Test
    void inProgressCompletedAndCancelledSessionsRejectRescheduleAndCancel() {
        Instant start = Instant.parse("2026-10-15T09:00:00Z");
        Instant end = Instant.parse("2026-10-15T12:00:00Z");
        Instant close = Instant.parse("2026-10-15T08:00:00Z");

        // IN_PROGRESS
        VaccinationSession inProgress = createDraftSession(start, end, close);
        inProgress.schedule();
        inProgress.start();
        sessionRepository.saveAndFlush(inProgress);

        assertThrows(IllegalArgumentException.class, () -> sessionService.previewReschedule(inProgress.getId(), start.plusSeconds(3600), end.plusSeconds(3600), null));
        assertThrows(IllegalArgumentException.class, () -> sessionService.previewCancellation(inProgress.getId()));

        // COMPLETED
        VaccinationSession completed = createDraftSession(start, end, close);
        completed.schedule();
        completed.start();
        completed.complete();
        sessionRepository.saveAndFlush(completed);

        assertThrows(IllegalArgumentException.class, () -> sessionService.previewReschedule(completed.getId(), start.plusSeconds(3600), end.plusSeconds(3600), null));
        assertThrows(IllegalArgumentException.class, () -> sessionService.previewCancellation(completed.getId()));

        // CANCELLED
        VaccinationSession cancelled = createDraftSession(start, end, close);
        cancelled.cancel();
        sessionRepository.saveAndFlush(cancelled);

        assertThrows(IllegalArgumentException.class, () -> sessionService.previewReschedule(cancelled.getId(), start.plusSeconds(3600), end.plusSeconds(3600), null));
        assertThrows(IllegalArgumentException.class, () -> sessionService.previewCancellation(cancelled.getId()));
    }

    @Test
    void centerTransferValidatesCenterStatusAndAssignmentsAndOperatingHoursAndClosures() {
        Instant start = Instant.parse("2026-10-15T09:00:00Z");
        Instant end = Instant.parse("2026-10-15T12:00:00Z");
        Instant close = Instant.parse("2026-10-15T08:00:00Z");
        VaccinationSession session = createDraftSession(start, end, close);

        // 1. INACTIVE Center
        VaccinationCenter inactiveCenter = centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-INACTIVE", "Inactive Center", "300 Inactive Way", "+94 11 333 3333",
                CenterStatus.INACTIVE, 100, 50, "UTC"));
        assertThrows(IllegalArgumentException.class,
                () -> sessionService.previewCenterTransfer(session.getId(), inactiveCenter.getId(), start, end, null));

        // 2. Center without active assignment
        VaccinationCenter unassignedCenter = centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-UNASSIGNED", "Unassigned Center", "400 Unassigned Way", "+94 11 444 4444",
                CenterStatus.ACTIVE, 100, 50, "UTC"));
        assertThrows(IllegalArgumentException.class,
                () -> sessionService.previewCenterTransfer(session.getId(), unassignedCenter.getId(), start, end, null));

        // 3. Center with closure on that day
        closureRepository.saveAndFlush(new CenterClosure(
                centerB, Instant.parse("2026-10-15T00:00:00Z"), Instant.parse("2026-10-15T23:59:59Z"),
                "Power outage maintenance"));
        assertThrows(IllegalArgumentException.class,
                () -> sessionService.previewCenterTransfer(session.getId(), centerB.getId(), start, end, null));
    }

    @Test
    void bookingCloseAtPolicyPreservesOrResetsCorrectly() {
        Instant start = Instant.parse("2026-10-15T14:00:00Z");
        Instant end = Instant.parse("2026-10-15T17:00:00Z");
        Instant initialClose = Instant.parse("2026-10-15T12:00:00Z");
        VaccinationSession session = createDraftSession(start, end, initialClose);

        // Case 1: Rescheduled earlier, so initialClose (12:00) is AFTER newStart (10:00) -> resets to newStart (10:00)
        Instant earlyStart = Instant.parse("2026-10-15T10:00:00Z");
        Instant earlyEnd = Instant.parse("2026-10-15T13:00:00Z");
        SessionChangePreview preview1 = sessionService.previewReschedule(session.getId(), earlyStart, earlyEnd, null);
        assertEquals(earlyStart, preview1.newSnapshot().getBookingCloseAt());

        // Case 2: Rescheduled later, so initialClose (12:00) is BEFORE newStart (15:00) -> preserves initialClose (12:00)
        Instant lateStartValid = Instant.parse("2026-10-15T15:00:00Z");
        Instant lateEndValid = Instant.parse("2026-10-15T18:00:00Z");
        SessionChangePreview preview2 = sessionService.previewReschedule(session.getId(), lateStartValid, lateEndValid, null);
        assertEquals(initialClose, preview2.newSnapshot().getBookingCloseAt());

        // Case 3: Explicit booking close provided and valid
        Instant explicitClose = Instant.parse("2026-10-15T14:00:00Z");
        SessionChangePreview preview3 = sessionService.previewReschedule(session.getId(), lateStartValid, lateEndValid, explicitClose);
        assertEquals(explicitClose, preview3.newSnapshot().getBookingCloseAt());

        // Case 4: Explicit booking close invalid (after start)
        assertThrows(IllegalArgumentException.class,
                () -> sessionService.previewReschedule(session.getId(), lateStartValid, lateEndValid, lateStartValid.plusSeconds(1)));
    }

    @Test
    void generatedOccurrencePreservesProvenanceAndGeneratorRecognizesIndividualOverrides() {
        SessionRecurrenceRule rule = ruleRepository.saveAndFlush(new SessionRecurrenceRule(
                campaign,
                centerA,
                Set.of(DayOfWeek.THURSDAY),
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 22),
                LocalTime.of(9, 0),
                LocalTime.of(12, 0),
                "UTC"
        ));

        // Materialize occurrences for all Thursdays in window (Oct 1, Oct 8, Oct 15, Oct 22) -> 4 occurrences
        RecurrenceGenerationResult genResult = generationService.generateOccurrences(rule.getId());
        assertEquals(4, genResult.createdCount());
        List<VaccinationSession> generatedSessions = genResult.sessions();
        assertEquals(4, generatedSessions.size());

        // Target the 3rd occurrence (Oct 15)
        VaccinationSession targetOccurrence = generatedSessions.stream()
                .filter(s -> s.getOriginOccurrenceDate().equals(LocalDate.of(2026, 10, 15)))
                .findFirst()
                .orElseThrow();

        String originalOccurrenceKey = targetOccurrence.getOccurrenceKey();
        LocalDate originalOriginDate = targetOccurrence.getOriginOccurrenceDate();

        // Perform individual reschedule + center transfer on this occurrence
        Instant newStart = Instant.parse("2026-10-15T13:00:00Z");
        Instant newEnd = Instant.parse("2026-10-15T16:00:00Z");
        SessionChangePreview preview = sessionService.previewCenterTransfer(
                targetOccurrence.getId(), centerB.getId(), newStart, newEnd, null);

        VaccinationSession overriddenOccurrence = sessionService.confirmCenterTransfer(
                targetOccurrence.getId(),
                centerB.getId(),
                newStart,
                newEnd,
                null,
                preview.previewToken(),
                "Individual occurrence facility transfer",
                "ACTOR-SUPERVISOR-01"
        );

        // Verify provenance untouched
        assertEquals(rule.getId(), overriddenOccurrence.getRecurrenceRule().getId());
        assertEquals(originalOccurrenceKey, overriddenOccurrence.getOccurrenceKey());
        assertEquals(originalOriginDate, overriddenOccurrence.getOriginOccurrenceDate());
        assertEquals(centerB.getId(), overriddenOccurrence.getCenter().getId());
        assertEquals("Europe/London", overriddenOccurrence.getScheduleTimezone());
        assertEquals(newStart, overriddenOccurrence.getStartAt());

        // Verify sibling occurrences remain on centerA with original schedules
        List<VaccinationSession> siblings = sessionRepository.findByRecurrenceRuleIdOrderByOriginOccurrenceDateAsc(rule.getId());
        for (VaccinationSession s : siblings) {
            if (!s.getId().equals(overriddenOccurrence.getId())) {
                assertEquals(centerA.getId(), s.getCenter().getId());
                assertEquals("UTC", s.getScheduleTimezone());
                assertEquals(LocalTime.of(9, 0), s.getStartAt().atZone(ZoneId.of("UTC")).toLocalTime());
            }
        }

        // Re-run recurrence generator on the rule: it must recognize the overridden occurrence as already materialized!
        RecurrenceGenerationResult rerunResult = generationService.generateOccurrences(rule.getId());
        assertEquals(0, rerunResult.createdCount());
        assertEquals(4, rerunResult.existingCount());
        assertEquals(4, rerunResult.sessions().size());

        // Overridden occurrence in materialized list is preserved
        VaccinationSession occurrenceAfterRerun = rerunResult.sessions().stream()
                .filter(s -> s.getOriginOccurrenceDate().equals(LocalDate.of(2026, 10, 15)))
                .findFirst()
                .orElseThrow();
        assertEquals(centerB.getId(), occurrenceAfterRerun.getCenter().getId());
        assertEquals(newStart, occurrenceAfterRerun.getStartAt());
    }

    @Test
    void trueProvenanceCorruptionStillRejectsInGenerator() {
        SessionRecurrenceRule rule = ruleRepository.saveAndFlush(new SessionRecurrenceRule(
                campaign,
                centerA,
                Set.of(DayOfWeek.THURSDAY),
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 22),
                LocalTime.of(9, 0),
                LocalTime.of(12, 0),
                "UTC"
        ));

        generationService.generateOccurrences(rule.getId(), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 8));

        // Inject corrupt session belonging to rule where originOccurrenceDate does not match candidate date in key
        VaccinationSession corruptSession = new VaccinationSession(
                campaign,
                centerA,
                rule,
                rule.getGenerationKey() + ":2026-10-15",
                LocalDate.of(2026, 10, 16), // Corrupted origin date mismatch
                Instant.parse("2026-10-15T09:00:00Z"),
                Instant.parse("2026-10-15T12:00:00Z"),
                Instant.parse("2026-10-15T09:00:00Z"),
                "UTC"
        );
        sessionRepository.saveAndFlush(corruptSession);

        // Attempting to generate on rule when key has corrupted origin date provenance must fail
        assertThrows(IllegalStateException.class,
                () -> generationService.generateOccurrences(rule.getId(), LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 15)));
    }

    @Test
    void failedConfirmationRollsBackAllMutationsAndPersistsNoEvent() {
        Instant start = Instant.parse("2026-10-15T09:00:00Z");
        Instant end = Instant.parse("2026-10-15T12:00:00Z");
        Instant close = Instant.parse("2026-10-15T08:00:00Z");
        VaccinationSession session = createDraftSession(start, end, close);

        Instant newStart = Instant.parse("2026-10-15T13:00:00Z");
        Instant newEnd = Instant.parse("2026-10-15T16:00:00Z");
        SessionChangePreview preview = sessionService.previewReschedule(session.getId(), newStart, newEnd, null);

        // Attempt confirm with invalid actor ID (> 64 chars)
        assertThrows(IllegalArgumentException.class, () -> sessionService.confirmReschedule(
                session.getId(),
                newStart,
                newEnd,
                null,
                preview.previewToken(),
                "Valid reason",
                "A".repeat(65)
        ));

        // Verify session state remained original
        VaccinationSession reloaded = sessionRepository.findById(session.getId()).orElseThrow();
        assertEquals(start, reloaded.getStartAt());
        assertEquals(end, reloaded.getEndAt());
        assertEquals(close, reloaded.getBookingCloseAt());

        // Verify zero events persisted
        List<SessionChangeEvent> events = sessionChangeEventRepository.findBySessionIdOrderByCreatedAtAscIdAsc(session.getId());
        assertTrue(events.isEmpty());
    }

    @Test
    void exactNoOpReschedulePreviewIsRejectedWithIllegalArgumentException() {
        Instant start = Instant.parse("2026-10-15T09:00:00Z");
        Instant end = Instant.parse("2026-10-15T12:00:00Z");
        Instant close = Instant.parse("2026-10-15T08:00:00Z");
        VaccinationSession session = createDraftSession(start, end, close);

        // Preview with identical start, end, and null booking close (which defaults to original close)
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                () -> sessionService.previewReschedule(session.getId(), start, end, null));
        assertTrue(ex1.getMessage().contains("no material change"));

        // Preview with identical start, end, and explicit identical close
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                () -> sessionService.previewReschedule(session.getId(), start, end, close));
        assertTrue(ex2.getMessage().contains("no material change"));
    }

    @Test
    void exactNoOpCenterTransferPreviewIsRejectedWithIllegalArgumentException() {
        Instant start = Instant.parse("2026-10-15T09:00:00Z");
        Instant end = Instant.parse("2026-10-15T12:00:00Z");
        Instant close = Instant.parse("2026-10-15T08:00:00Z");
        VaccinationSession session = createDraftSession(start, end, close);

        // Center transfer targeting the same center with identical schedule
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> sessionService.previewCenterTransfer(session.getId(), centerA.getId(), start, end, close));
        assertTrue(ex.getMessage().contains("no material change"));
    }

    @Test
    void exactNoOpConfirmationUnderLocksIsRejectedAndPersistsNoEvent() {
        Instant start = Instant.parse("2026-10-15T09:00:00Z");
        Instant end = Instant.parse("2026-10-15T12:00:00Z");
        Instant close = Instant.parse("2026-10-15T08:00:00Z");
        VaccinationSession session = createDraftSession(start, end, close);

        // Attempt confirm with identical parameters
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> sessionService.confirmReschedule(
                        session.getId(), start, end, close, "any-token", "Testing no-op confirm", "admin-1"));
        assertTrue(ex.getMessage().contains("no material change"));

        // Zero events created
        List<SessionChangeEvent> events = sessionChangeEventRepository.findBySessionIdOrderByCreatedAtAscIdAsc(session.getId());
        assertTrue(events.isEmpty());
    }

    @Test
    void rejectedNoOpDoesNotCauseSessionToBecomePreservedExceptionInSeriesChange() {
        SessionRecurrenceRule rule = ruleRepository.saveAndFlush(new SessionRecurrenceRule(
                campaign, centerA, EnumSet.of(DayOfWeek.MONDAY),
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 12),
                LocalTime.of(9, 0), LocalTime.of(12, 0), "UTC"));

        generationService.generateOccurrences(rule.getId());
        VaccinationSession s1 = sessionRepository.findByRecurrenceRuleIdOrderByOriginOccurrenceDateAsc(rule.getId()).get(0);

        // Attempt exact no-op reschedule on s1 (rejected)
        assertThrows(IllegalArgumentException.class,
                () -> sessionService.previewReschedule(s1.getId(), s1.getStartAt(), s1.getEndAt(), s1.getBookingCloseAt()));

        // Subsequent series change preview
        SessionSeriesChangeService seriesChangeService = new SessionSeriesChangeService(
                ruleRepository, campaignRepository, centerRepository, sessionRepository,
                sessionChangeEventRepository, validator, null, clock);

        SeriesChangePreview preview = seriesChangeService.previewSeriesChange(
                rule.getId(), EnumSet.of(DayOfWeek.MONDAY),
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 12),
                LocalTime.of(10, 0), LocalTime.of(13, 0), "UTC");

        // s1 must be RETAINED, NOT PRESERVED_EXCEPTION
        SeriesOccurrencePreviewItem item1 = preview.occurrenceEntries().get(0);
        assertEquals(SeriesOccurrenceCategory.RETAINED, item1.category());
    }

    @Test
    void genuineRescheduleAndCenterTransferSucceeds() {
        Instant start = Instant.parse("2026-10-15T09:00:00Z");
        Instant end = Instant.parse("2026-10-15T12:00:00Z");
        Instant close = Instant.parse("2026-10-15T08:00:00Z");
        VaccinationSession session = createDraftSession(start, end, close);

        // 1. Genuine reschedule (time shift)
        Instant newStart = Instant.parse("2026-10-15T13:00:00Z");
        Instant newEnd = Instant.parse("2026-10-15T16:00:00Z");
        SessionChangePreview preview1 = sessionService.previewReschedule(session.getId(), newStart, newEnd, null);
        assertNotNull(preview1);
        sessionService.confirmReschedule(session.getId(), newStart, newEnd, null, preview1.previewToken(), "Time change", "admin-1");

        VaccinationSession rescheduled = sessionRepository.findById(session.getId()).orElseThrow();
        assertEquals(newStart, rescheduled.getStartAt());
        assertEquals(newEnd, rescheduled.getEndAt());

        // 2. Genuine center transfer (transfer to centerB)
        SessionChangePreview preview2 = sessionService.previewCenterTransfer(session.getId(), centerB.getId(), newStart, newEnd, null);
        assertNotNull(preview2);
        sessionService.confirmCenterTransfer(session.getId(), centerB.getId(), newStart, newEnd, null, preview2.previewToken(), "Transfer center", "admin-1");

        VaccinationSession transferred = sessionRepository.findById(session.getId()).orElseThrow();
        assertEquals(centerB.getId(), transferred.getCenter().getId());

        List<SessionChangeEvent> events = sessionChangeEventRepository.findBySessionIdOrderByCreatedAtAscIdAsc(session.getId());
        assertEquals(2, events.size());
    }
}
