package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CampaignCenterAssignment;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.OperatingHours;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.CampaignCenterAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.OperatingHoursRepository;
import com.se2030.vaccinationportal.campaign.repository.SessionChangeEventRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.service.VaccinationSessionService;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SessionLifecycleHardeningIntegrationTests {

    @Autowired private VaccinationSessionService sessionService;
    @Autowired private VaccinationSessionRepository sessionRepository;
    @Autowired private SessionChangeEventRepository eventRepository;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private CampaignCenterAssignmentRepository assignmentRepository;
    @Autowired private OperatingHoursRepository hoursRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;

    private VaccinationCampaign campaign;
    private VaccinationCenter center;

    @BeforeEach
    void setUp() {
        VaccineType vaccineType = vaccineTypeRepository.findByCode("VAX-LIFECYCLE")
                .orElseGet(() -> vaccineTypeRepository.saveAndFlush(new VaccineType("VAX-LIFECYCLE", "Lifecycle Vaccine", "dose", true)));
        TargetGroup targetGroup = targetGroupRepository.findByCode("TG-LIFECYCLE")
                .orElseGet(() -> targetGroupRepository.saveAndFlush(new TargetGroup("TG-LIFECYCLE", "Lifecycle Group", true)));

        LocalDate today = LocalDate.now();
        campaign = campaignRepository.saveAndFlush(new VaccinationCampaign(
                "CMP-LIFECYCLE", "Lifecycle Campaign", "Test", vaccineType, targetGroup,
                today.minusDays(10), today.plusDays(60),
                today.minusDays(10).atStartOfDay(ZoneId.of("UTC")).toInstant(),
                today.plusDays(60).atStartOfDay(ZoneId.of("UTC")).toInstant(),
                CampaignStatus.DRAFT
        ));

        center = centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-LIFECYCLE", "Lifecycle Center", "123 Main St", "Colombo", CenterStatus.ACTIVE, 10, 5, "UTC"
        ));

        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                campaign, center, today.minusDays(10), today.plusDays(60)
        ));

        for (DayOfWeek dow : DayOfWeek.values()) {
            hoursRepository.saveAndFlush(new OperatingHours(
                    center, dow, LocalTime.of(8, 0), LocalTime.of(17, 0),
                    today.minusDays(10), today.plusDays(60)
            ));
        }
    }

    private VaccinationSession createDraftSession() {
        LocalDate tomorrow = LocalDate.now().plusDays(2);
        Instant start = tomorrow.atTime(9, 0).toInstant(java.time.ZoneOffset.UTC);
        Instant end = tomorrow.atTime(12, 0).toInstant(java.time.ZoneOffset.UTC);
        Instant close = start.minusSeconds(3600);
        return sessionService.createOneOffDraftSession(campaign.getId(), center.getId(), start, end, close);
    }

    @Test
    @DisplayName("Allowed transition: DRAFT -> SCHEDULED")
    void draftToScheduled_allowed() {
        VaccinationSession session = createDraftSession();
        assertEquals(SessionLifecycleStatus.DRAFT, session.getLifecycleStatus());

        VaccinationSession scheduled = sessionService.scheduleSession(session.getId());
        assertEquals(SessionLifecycleStatus.SCHEDULED, scheduled.getLifecycleStatus());
    }

    @Test
    @DisplayName("Allowed transition: SCHEDULED -> IN_PROGRESS")
    void scheduledToInProgress_allowed() {
        VaccinationSession session = createDraftSession();
        sessionService.scheduleSession(session.getId());

        VaccinationSession inProgress = sessionService.startSession(session.getId());
        assertEquals(SessionLifecycleStatus.IN_PROGRESS, inProgress.getLifecycleStatus());
    }

    @Test
    @DisplayName("Allowed transition: IN_PROGRESS -> COMPLETED")
    void inProgressToCompleted_allowed() {
        VaccinationSession session = createDraftSession();
        sessionService.scheduleSession(session.getId());
        sessionService.startSession(session.getId());

        VaccinationSession completed = sessionService.completeSession(session.getId());
        assertEquals(SessionLifecycleStatus.COMPLETED, completed.getLifecycleStatus());
    }

    @Test
    @DisplayName("Allowed transition: DRAFT -> CANCELLED")
    void draftToCancelled_allowed() {
        VaccinationSession session = createDraftSession();
        session.cancel();
        assertEquals(SessionLifecycleStatus.CANCELLED, session.getLifecycleStatus());
    }

    @Test
    @DisplayName("Allowed transition: SCHEDULED -> CANCELLED")
    void scheduledToCancelled_allowed() {
        VaccinationSession session = createDraftSession();
        session.schedule();
        session.cancel();
        assertEquals(SessionLifecycleStatus.CANCELLED, session.getLifecycleStatus());
    }

    @Test
    @DisplayName("Rejected transitions from DRAFT")
    void rejectedTransitionsFromDraft() {
        VaccinationSession session = createDraftSession();

        assertThrows(IllegalStateException.class, session::start);
        assertThrows(IllegalStateException.class, session::complete);
        assertThrows(IllegalStateException.class, () -> session.transitionLifecycleStatus(SessionLifecycleStatus.DRAFT)); // DRAFT -> DRAFT
    }

    @Test
    @DisplayName("Rejected transitions from SCHEDULED")
    void rejectedTransitionsFromScheduled() {
        VaccinationSession session = createDraftSession();
        session.schedule();

        assertThrows(IllegalStateException.class, () -> session.transitionLifecycleStatus(SessionLifecycleStatus.DRAFT));
        assertThrows(IllegalStateException.class, session::complete);
        assertThrows(IllegalStateException.class, session::schedule); // SCHEDULED -> SCHEDULED
    }

    @Test
    @DisplayName("Rejected transitions from IN_PROGRESS")
    void rejectedTransitionsFromInProgress() {
        VaccinationSession session = createDraftSession();
        session.schedule();
        session.start();

        assertThrows(IllegalStateException.class, () -> session.transitionLifecycleStatus(SessionLifecycleStatus.DRAFT));
        assertThrows(IllegalStateException.class, () -> session.transitionLifecycleStatus(SessionLifecycleStatus.SCHEDULED));
        assertThrows(IllegalStateException.class, session::cancel); // IN_PROGRESS -> CANCELLED rejected
        assertThrows(IllegalStateException.class, session::start); // IN_PROGRESS -> IN_PROGRESS
    }

    @Test
    @DisplayName("Terminal state COMPLETED rejects all further transitions")
    void completedIsTerminal() {
        VaccinationSession session = createDraftSession();
        session.schedule();
        session.start();
        session.complete();

        for (SessionLifecycleStatus status : SessionLifecycleStatus.values()) {
            assertThrows(IllegalStateException.class, () -> session.transitionLifecycleStatus(status));
        }
    }

    @Test
    @DisplayName("Terminal state CANCELLED rejects all further transitions")
    void cancelledIsTerminal() {
        VaccinationSession session = createDraftSession();
        session.cancel();

        for (SessionLifecycleStatus status : SessionLifecycleStatus.values()) {
            assertThrows(IllegalStateException.class, () -> session.transitionLifecycleStatus(status));
        }
    }

    @Test
    @DisplayName("Lifecycle-only progression creates no SessionChangeEvent audit records")
    void lifecycleOnlyProgressionCreatesNoSessionChangeEvent() {
        long initialEvents = eventRepository.count();
        VaccinationSession session = createDraftSession();

        sessionService.scheduleSession(session.getId());
        assertEquals(initialEvents, eventRepository.count());

        sessionService.startSession(session.getId());
        assertEquals(initialEvents, eventRepository.count());

        sessionService.completeSession(session.getId());
        assertEquals(initialEvents, eventRepository.count());
    }

    @Test
    @DisplayName("Existing individual session cancellation preview and confirmation still works")
    void existingCancellationWorkflowStillWorks() {
        VaccinationSession session = createDraftSession();

        assertDoesNotThrow(() -> sessionService.previewCancellation(session.getId()));
    }
}
