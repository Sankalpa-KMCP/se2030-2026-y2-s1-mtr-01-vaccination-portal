package com.se2030.vaccinationportal.campaign.session.planning;

import com.se2030.vaccinationportal.campaign.CampaignTestFixtures;


import com.se2030.vaccinationportal.campaign.core.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.center.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionChangeEvent;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionChangeType;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionScheduleSnapshot;
import com.se2030.vaccinationportal.campaign.reference.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.session.planning.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.reference.model.VaccineType;
import com.se2030.vaccinationportal.campaign.session.planning.repository.SessionChangeEventRepository;
import com.se2030.vaccinationportal.campaign.reference.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.core.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.center.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.reference.repository.VaccineTypeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SessionChangeEventPersistenceIntegrationTests {

    private static final Instant START_1 = Instant.parse("2026-10-15T09:00:00Z");
    private static final Instant END_1 = Instant.parse("2026-10-15T12:00:00Z");
    private static final Instant BOOKING_CLOSE_1 = Instant.parse("2026-10-15T08:00:00Z");

    private static final Instant START_2 = Instant.parse("2026-10-15T13:00:00Z");
    private static final Instant END_2 = Instant.parse("2026-10-15T16:00:00Z");
    private static final Instant BOOKING_CLOSE_2 = Instant.parse("2026-10-15T12:00:00Z");

    private static final Instant START_3 = Instant.parse("2026-10-16T09:00:00Z");
    private static final Instant END_3 = Instant.parse("2026-10-16T12:00:00Z");
    private static final Instant BOOKING_CLOSE_3 = Instant.parse("2026-10-16T08:00:00Z");

    @Autowired private SessionChangeEventRepository changeEventRepository;
    @Autowired private VaccinationSessionRepository sessionRepository;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @PersistenceContext private EntityManager entityManager;

    private VaccinationSession session;
    private VaccinationCenter center1;
    private VaccinationCenter center2;

    @BeforeEach
    void setUp() {
        VaccineType vaccine = vaccineTypeRepository.saveAndFlush(
                new VaccineType("VAX-AUDIT-P", "Session Audit Persistence Vaccine"));
        TargetGroup group = targetGroupRepository.saveAndFlush(
                new TargetGroup("TG-AUDIT-P", "Session Audit Persistence Group"));
        VaccinationCampaign campaign = campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                "CMP-AUDIT-P", "Session Audit Persistence Campaign", null,
                vaccine, group,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31),
                Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-12-30T00:00:00Z"),
                CampaignStatus.DRAFT));
        center1 = centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-AUDIT-P1", "Audit Center Alpha", "1 Alpha Road", "+94 11 000 0001",
                CenterStatus.ACTIVE, 100, 50, "Asia/Colombo"));
        center2 = centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-AUDIT-P2", "Audit Center Beta", "2 Beta Road", "+94 11 000 0002",
                CenterStatus.ACTIVE, 120, 60, "Asia/Colombo"));

        session = sessionRepository.saveAndFlush(new VaccinationSession(
                campaign, center1, START_1, END_1, BOOKING_CLOSE_1, "Asia/Colombo"));
    }

    @Test
    void persistsAndReloadsSessionChangeEventWithSnapshots() {
        SessionScheduleSnapshot oldSnapshot = SessionScheduleSnapshot.from(session);
        SessionScheduleSnapshot newSnapshot = new SessionScheduleSnapshot(
                center1.getId(),
                "Asia/Colombo",
                START_2,
                END_2,
                BOOKING_CLOSE_2,
                SessionLifecycleStatus.SCHEDULED
        );

        SessionChangeEvent event = new SessionChangeEvent(
                session,
                SessionChangeType.RESCHEDULED,
                oldSnapshot,
                newSnapshot,
                "Clinician shift adjustment",
                12,
                3,
                "staff-nurse-04"
        );

        SessionChangeEvent saved = changeEventRepository.saveAndFlush(event);
        Long eventId = saved.getId();
        assertNotNull(eventId);

        entityManager.clear();

        SessionChangeEvent reloaded = changeEventRepository.findById(eventId).orElseThrow();

        assertAll(
                () -> assertEquals(eventId, reloaded.getId()),
                () -> assertEquals(session.getId(), reloaded.getSession().getId()),
                () -> assertEquals(SessionChangeType.RESCHEDULED, reloaded.getChangeType()),
                () -> assertEquals(oldSnapshot, reloaded.getOldSnapshot()),
                () -> assertEquals(newSnapshot, reloaded.getNewSnapshot()),
                () -> assertEquals("Clinician shift adjustment", reloaded.getReason()),
                () -> assertEquals(12, reloaded.getAffectedAppointmentCount()),
                () -> assertEquals(3, reloaded.getAffectedWaitlistCount()),
                () -> assertEquals("staff-nurse-04", reloaded.getActorId()),
                () -> assertNotNull(reloaded.getCreatedAt())
        );
    }

    @Test
    void persistsMultipleEventsForOneSessionAndDeterminesDeterministicOrder() {
        SessionScheduleSnapshot snap1 = SessionScheduleSnapshot.from(session);
        SessionScheduleSnapshot snap2 = new SessionScheduleSnapshot(
                center1.getId(), "Asia/Colombo", START_2, END_2, BOOKING_CLOSE_2, SessionLifecycleStatus.SCHEDULED);
        SessionScheduleSnapshot snap3 = new SessionScheduleSnapshot(
                center2.getId(), "Asia/Colombo", START_3, END_3, BOOKING_CLOSE_3, SessionLifecycleStatus.SCHEDULED);

        SessionChangeEvent event1 = changeEventRepository.saveAndFlush(new SessionChangeEvent(
                session,
                SessionChangeType.RESCHEDULED,
                snap1,
                snap2,
                "First change: time shift",
                4,
                1,
                "admin-1"
        ));

        SessionChangeEvent event2 = changeEventRepository.saveAndFlush(new SessionChangeEvent(
                session,
                SessionChangeType.CENTER_TRANSFERRED,
                snap2,
                snap3,
                "Second change: facility reallocation",
                8,
                2,
                "admin-2"
        ));

        entityManager.clear();

        List<SessionChangeEvent> history = changeEventRepository.findBySessionIdOrderByCreatedAtAscIdAsc(session.getId());

        assertEquals(2, history.size());
        assertEquals(event1.getId(), history.get(0).getId());
        assertEquals(SessionChangeType.RESCHEDULED, history.get(0).getChangeType());
        assertEquals(event2.getId(), history.get(1).getId());
        assertEquals(SessionChangeType.CENTER_TRANSFERRED, history.get(1).getChangeType());
    }

    @Test
    void deletingEventDoesNotRemoveParentSession() {
        SessionScheduleSnapshot snap1 = SessionScheduleSnapshot.from(session);
        SessionScheduleSnapshot snap2 = new SessionScheduleSnapshot(
                center1.getId(), "Asia/Colombo", START_2, END_2, BOOKING_CLOSE_2, SessionLifecycleStatus.SCHEDULED);

        SessionChangeEvent event = changeEventRepository.saveAndFlush(new SessionChangeEvent(
                session,
                SessionChangeType.RESCHEDULED,
                snap1,
                snap2,
                "Temporary adjustment",
                0,
                0,
                "admin-1"
        ));

        Long eventId = event.getId();
        Long sessionId = session.getId();

        entityManager.flush();
        entityManager.clear();

        changeEventRepository.deleteById(eventId);
        changeEventRepository.flush();
        entityManager.clear();

        assertTrue(changeEventRepository.findById(eventId).isEmpty());
        assertTrue(sessionRepository.findById(sessionId).isPresent(), "Parent session must survive event deletion");
    }

    @Test
    void findByIdForUpdateAcquiresLockAndFindsSession() {
        Optional<VaccinationSession> lockedSession = sessionRepository.findByIdForUpdate(session.getId());

        assertTrue(lockedSession.isPresent());
        assertEquals(session.getId(), lockedSession.get().getId());
        assertEquals("Asia/Colombo", lockedSession.get().getScheduleTimezone());
    }

    @Test
    void findByIdForUpdateReturnsEmptyForNonExistentId() {
        Optional<VaccinationSession> result = sessionRepository.findByIdForUpdate(999999L);

        assertTrue(result.isEmpty());
    }
}
