package com.se2030.vaccinationportal.campaign.session.planning.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.core.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.center.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.center.model.OperationalResource;
import com.se2030.vaccinationportal.campaign.center.model.OperationalResourceStatus;
import com.se2030.vaccinationportal.campaign.center.model.ResourceType;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionResourceAssignment;
import com.se2030.vaccinationportal.campaign.reference.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.session.planning.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.reference.model.VaccineType;
import com.se2030.vaccinationportal.campaign.center.repository.OperationalResourceRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.SessionResourceAssignmentRepository;
import com.se2030.vaccinationportal.campaign.reference.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.core.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.center.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.reference.repository.VaccineTypeRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * Module: MF-02 — Vaccination Campaign, Center and Session Management
 * Use Case: UC-10B — Assign Staff / Resources and Approve Capacity
 * Responsibility: Session staff/resource assignment and capacity planning
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SessionResourceAssignmentServiceIntegrationTests {

    @Autowired private SessionResourceAssignmentService assignmentService;
    @Autowired private SessionResourceAssignmentRepository assignmentRepository;
    @Autowired private VaccinationSessionRepository sessionRepository;
    @Autowired private OperationalResourceRepository resourceRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;

    private VaccinationCenter centerA;
    private VaccinationCenter centerB;
    private VaccinationCampaign campaign;
    private VaccinationSession session;
    private OperationalResource resourceActive;
    private OperationalResource resourceMaintenance;
    private OperationalResource resourceInactive;
    private OperationalResource resourceCenterB;

    private final Instant sessionStart = Instant.parse("2026-09-01T08:00:00Z");
    private final Instant sessionEnd = Instant.parse("2026-09-01T16:00:00Z");

    @BeforeEach
    void setUp() {
        centerA = centerRepository.save(new VaccinationCenter("CTR-01", "Center A", "100 Main", "+1", CenterStatus.ACTIVE, 100, 50, "UTC"));
        centerB = centerRepository.save(new VaccinationCenter("CTR-02", "Center B", "200 Main", "+2", CenterStatus.ACTIVE, 100, 50, "UTC"));

        VaccineType vaccine = vaccineTypeRepository.save(new VaccineType("VAX-RES-01", "Polio Vaccine"));
        TargetGroup targetGroup = targetGroupRepository.save(new TargetGroup("TG-RES-01", "Children Target"));
        campaign = campaignRepository.save(new VaccinationCampaign(
                "POL-26",
                "Polio 2026",
                "Description",
                vaccine,
                targetGroup,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-09-30T23:59:59Z")
        ));

        session = sessionRepository.save(new VaccinationSession(campaign, centerA, sessionStart, sessionEnd, sessionStart.minusSeconds(3600), "UTC"));

        resourceActive = resourceRepository.save(new OperationalResource(centerA, "STATION-1", ResourceType.VACCINATION_STATION, "Station 1", OperationalResourceStatus.ACTIVE));
        resourceMaintenance = resourceRepository.save(new OperationalResource(centerA, "STATION-2", ResourceType.VACCINATION_STATION, "Station 2", OperationalResourceStatus.MAINTENANCE));
        resourceInactive = resourceRepository.save(new OperationalResource(centerA, "STATION-3", ResourceType.VACCINATION_STATION, "Station 3", OperationalResourceStatus.INACTIVE));
        resourceCenterB = resourceRepository.save(new OperationalResource(centerB, "STATION-B", ResourceType.VACCINATION_STATION, "Station B", OperationalResourceStatus.ACTIVE));
    }

    @Test
    @DisplayName("createAssignment - succeeds within session operating window")
    void createAssignment_withinSessionWindow_succeeds() {
        Instant start = Instant.parse("2026-09-01T08:00:00Z");
        Instant end = Instant.parse("2026-09-01T12:00:00Z");

        SessionResourceAssignment assignment = assignmentService.createAssignment(session.getId(), resourceActive.getId(), start, end);
        assertNotNull(assignment.getId());
        assertEquals(session.getId(), assignment.getSession().getId());
        assertEquals(resourceActive.getId(), assignment.getResource().getId());
        assertEquals(start, assignment.getStartAt());
        assertEquals(end, assignment.getEndAt());
    }

    @Test
    @DisplayName("createAssignment - touching half-open intervals succeed without conflict")
    void createAssignment_touchingIntervals_succeed() {
        Instant t1 = Instant.parse("2026-09-01T08:00:00Z");
        Instant t2 = Instant.parse("2026-09-01T12:00:00Z");
        Instant t3 = Instant.parse("2026-09-01T16:00:00Z");

        SessionResourceAssignment a1 = assignmentService.createAssignment(session.getId(), resourceActive.getId(), t1, t2);
        SessionResourceAssignment a2 = assignmentService.createAssignment(session.getId(), resourceActive.getId(), t2, t3);

        assertNotNull(a1.getId());
        assertNotNull(a2.getId());
    }

    @Test
    @DisplayName("createAssignment - rejected when interval falls outside session window")
    void createAssignment_outsideSessionWindow_throwsException() {
        // Starts before session
        assertThrows(IllegalArgumentException.class, () -> assignmentService.createAssignment(
                session.getId(), resourceActive.getId(), sessionStart.minusSeconds(60), sessionEnd
        ));

        // Ends after session
        assertThrows(IllegalArgumentException.class, () -> assignmentService.createAssignment(
                session.getId(), resourceActive.getId(), sessionStart, sessionEnd.plusSeconds(60)
        ));

        // Start not before end
        assertThrows(IllegalArgumentException.class, () -> assignmentService.createAssignment(
                session.getId(), resourceActive.getId(), sessionEnd, sessionStart
        ));
    }

    @Test
    @DisplayName("createAssignment - rejected when session is not in DRAFT status")
    void createAssignment_nonDraftSession_throwsException() {
        ReflectionTestUtils.setField(session, "lifecycleStatus", SessionLifecycleStatus.CANCELLED);
        sessionRepository.save(session);

        assertThrows(IllegalStateException.class, () -> assignmentService.createAssignment(
                session.getId(), resourceActive.getId(), sessionStart, sessionEnd
        ));
    }

    @Test
    @DisplayName("createAssignment - rejected when resource belongs to a different center")
    void createAssignment_resourceWrongCenter_throwsException() {
        assertThrows(IllegalStateException.class, () -> assignmentService.createAssignment(
                session.getId(), resourceCenterB.getId(), sessionStart, sessionEnd
        ));
    }

    @Test
    @DisplayName("createAssignment - rejected when resource is in MAINTENANCE or INACTIVE status")
    void createAssignment_inactiveResource_throwsException() {
        assertThrows(IllegalStateException.class, () -> assignmentService.createAssignment(
                session.getId(), resourceMaintenance.getId(), sessionStart, sessionEnd
        ));

        assertThrows(IllegalStateException.class, () -> assignmentService.createAssignment(
                session.getId(), resourceInactive.getId(), sessionStart, sessionEnd
        ));
    }

    @Test
    @DisplayName("createAssignment - rejected when resource has overlapping active assignment")
    void createAssignment_overlappingAssignment_throwsException() {
        Instant t1 = Instant.parse("2026-09-01T08:00:00Z");
        Instant t2 = Instant.parse("2026-09-01T12:00:00Z");
        assignmentService.createAssignment(session.getId(), resourceActive.getId(), t1, t2);

        // Overlapping interval [10:00, 14:00)
        Instant overlapStart = Instant.parse("2026-09-01T10:00:00Z");
        Instant overlapEnd = Instant.parse("2026-09-01T14:00:00Z");

        assertThrows(IllegalStateException.class, () -> assignmentService.createAssignment(
                session.getId(), resourceActive.getId(), overlapStart, overlapEnd
        ));
    }

    @Test
    @DisplayName("getAssignmentsForSession - returns deterministic order startAt ASC, id ASC")
    void getAssignmentsForSession_returnsDeterministicOrder() {
        Instant t1 = Instant.parse("2026-09-01T08:00:00Z");
        Instant t2 = Instant.parse("2026-09-01T12:00:00Z");
        Instant t3 = Instant.parse("2026-09-01T16:00:00Z");

        OperationalResource anotherActive = resourceRepository.save(new OperationalResource(centerA, "STATION-ALT", ResourceType.VACCINATION_STATION, "Alt Station", OperationalResourceStatus.ACTIVE));

        SessionResourceAssignment a2 = assignmentService.createAssignment(session.getId(), resourceActive.getId(), t2, t3);
        SessionResourceAssignment a1 = assignmentService.createAssignment(session.getId(), anotherActive.getId(), t1, t2);

        List<SessionResourceAssignment> list = assignmentService.getAssignmentsForSession(session.getId());
        assertEquals(2, list.size());
        assertEquals(a1.getId(), list.get(0).getId());
        assertEquals(a2.getId(), list.get(1).getId());
    }

    @Test
    @DisplayName("getAssignment and deleteAssignment - session ownership and DRAFT deletion validation")
    void getAndDeleteAssignment_validation() {
        Instant t1 = Instant.parse("2026-09-01T08:00:00Z");
        Instant t2 = Instant.parse("2026-09-01T12:00:00Z");
        SessionResourceAssignment assignment = assignmentService.createAssignment(session.getId(), resourceActive.getId(), t1, t2);

        // Get single
        SessionResourceAssignment retrieved = assignmentService.getAssignment(session.getId(), assignment.getId());
        assertEquals(assignment.getId(), retrieved.getId());

        // Wrong session ID
        assertThrows(ResourceNotFoundException.class, () -> assignmentService.getAssignment(9999L, assignment.getId()));

        // Delete from DRAFT session succeeds and preserves resource entity
        assignmentService.deleteAssignment(session.getId(), assignment.getId());
        assertFalse(assignmentRepository.existsById(assignment.getId()));
        assertTrue(resourceRepository.existsById(resourceActive.getId()));

        // Non-DRAFT delete rejected
        SessionResourceAssignment a2 = assignmentService.createAssignment(session.getId(), resourceActive.getId(), t1, t2);
        ReflectionTestUtils.setField(session, "lifecycleStatus", SessionLifecycleStatus.CANCELLED);
        sessionRepository.save(session);

        assertThrows(IllegalStateException.class, () -> assignmentService.deleteAssignment(session.getId(), a2.getId()));
    }
}
