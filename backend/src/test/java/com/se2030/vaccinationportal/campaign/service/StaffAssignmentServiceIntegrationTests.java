package com.se2030.vaccinationportal.campaign.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffRoster;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffStatus;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.StaffAssignment;
import com.se2030.vaccinationportal.campaign.model.StaffRole;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.OperationalStaffRosterRepository;
import com.se2030.vaccinationportal.campaign.repository.StaffAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
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

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StaffAssignmentServiceIntegrationTests {

    @Autowired private StaffAssignmentService staffAssignmentService;
    @Autowired private StaffAssignmentRepository staffAssignmentRepository;
    @Autowired private VaccinationSessionRepository sessionRepository;
    @Autowired private OperationalStaffRosterRepository rosterRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;

    private VaccinationCenter centerA;
    private VaccinationCenter centerB;
    private VaccinationCampaign campaign;
    private VaccinationSession sessionA;
    private VaccinationSession sessionB;
    private OperationalStaffRoster rosterVaccinatorActive;
    private OperationalStaffRoster rosterCheckInActive;
    private OperationalStaffRoster rosterInactive;
    private OperationalStaffRoster rosterCenterB;

    private final Instant sessionStart = Instant.parse("2026-09-01T08:00:00Z");
    private final Instant sessionEnd = Instant.parse("2026-09-01T16:00:00Z");

    @BeforeEach
    void setUp() {
        centerA = centerRepository.save(new VaccinationCenter("CTR-01", "Center A", "100 Main", "+1", CenterStatus.ACTIVE, 100, 50, "UTC"));
        centerB = centerRepository.save(new VaccinationCenter("CTR-02", "Center B", "200 Main", "+2", CenterStatus.ACTIVE, 100, 50, "UTC"));

        VaccineType vaccine = vaccineTypeRepository.save(new VaccineType("VAX-STF-01", "Staff Vaccine"));
        TargetGroup targetGroup = targetGroupRepository.save(new TargetGroup("TG-STF-01", "Staff Target"));
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

        sessionA = sessionRepository.save(new VaccinationSession(campaign, centerA, sessionStart, sessionEnd, sessionStart.minusSeconds(3600), "UTC"));
        sessionB = sessionRepository.save(new VaccinationSession(campaign, centerB, sessionStart, sessionEnd, sessionStart.minusSeconds(3600), "UTC"));

        rosterVaccinatorActive = rosterRepository.save(new OperationalStaffRoster(centerA, 501L, StaffRole.VACCINATOR, OperationalStaffStatus.ACTIVE));
        rosterCheckInActive = rosterRepository.save(new OperationalStaffRoster(centerA, 502L, StaffRole.CHECK_IN_STAFF, OperationalStaffStatus.ACTIVE));
        rosterInactive = rosterRepository.save(new OperationalStaffRoster(centerA, 504L, StaffRole.VACCINATOR, OperationalStaffStatus.INACTIVE));
        rosterCenterB = rosterRepository.save(new OperationalStaffRoster(centerB, 501L, StaffRole.VACCINATOR, OperationalStaffStatus.ACTIVE));
    }

    @Test
    @DisplayName("createAssignment - succeeds within session operating window")
    void createAssignment_withinSessionWindow_succeeds() {
        Instant start = Instant.parse("2026-09-01T08:00:00Z");
        Instant end = Instant.parse("2026-09-01T12:00:00Z");

        StaffAssignment assignment = staffAssignmentService.createAssignment(
                sessionA.getId(), rosterVaccinatorActive.getId(), StaffRole.VACCINATOR, start, end
        );

        assertNotNull(assignment.getId());
        assertEquals(sessionA.getId(), assignment.getSession().getId());
        assertEquals(rosterVaccinatorActive.getId(), assignment.getStaffRoster().getId());
        assertEquals(501L, assignment.getUserId());
        assertEquals(StaffRole.VACCINATOR, assignment.getRole());
        assertEquals(start, assignment.getStartAt());
        assertEquals(end, assignment.getEndAt());
    }

    @Test
    @DisplayName("createAssignment - touching half-open intervals succeed without conflict")
    void createAssignment_touchingIntervals_succeed() {
        Instant t1 = Instant.parse("2026-09-01T08:00:00Z");
        Instant t2 = Instant.parse("2026-09-01T12:00:00Z");
        Instant t3 = Instant.parse("2026-09-01T16:00:00Z");

        StaffAssignment a1 = staffAssignmentService.createAssignment(sessionA.getId(), rosterVaccinatorActive.getId(), StaffRole.VACCINATOR, t1, t2);
        StaffAssignment a2 = staffAssignmentService.createAssignment(sessionA.getId(), rosterVaccinatorActive.getId(), StaffRole.VACCINATOR, t2, t3);

        assertNotNull(a1.getId());
        assertNotNull(a2.getId());
    }

    @Test
    @DisplayName("createAssignment - rejected when interval falls outside session window")
    void createAssignment_outsideSessionWindow_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> staffAssignmentService.createAssignment(
                sessionA.getId(), rosterVaccinatorActive.getId(), StaffRole.VACCINATOR, sessionStart.minusSeconds(60), sessionEnd
        ));

        assertThrows(IllegalArgumentException.class, () -> staffAssignmentService.createAssignment(
                sessionA.getId(), rosterVaccinatorActive.getId(), StaffRole.VACCINATOR, sessionStart, sessionEnd.plusSeconds(60)
        ));

        assertThrows(IllegalArgumentException.class, () -> staffAssignmentService.createAssignment(
                sessionA.getId(), rosterVaccinatorActive.getId(), StaffRole.VACCINATOR, sessionEnd, sessionStart
        ));
    }

    @Test
    @DisplayName("createAssignment - rejected when session is not in DRAFT status")
    void createAssignment_nonDraftSession_throwsException() {
        ReflectionTestUtils.setField(sessionA, "lifecycleStatus", SessionLifecycleStatus.CANCELLED);
        sessionRepository.save(sessionA);

        assertThrows(IllegalStateException.class, () -> staffAssignmentService.createAssignment(
                sessionA.getId(), rosterVaccinatorActive.getId(), StaffRole.VACCINATOR, sessionStart, sessionEnd
        ));
    }

    @Test
    @DisplayName("createAssignment - rejected when roster entry belongs to different center")
    void createAssignment_rosterWrongCenter_throwsException() {
        assertThrows(IllegalStateException.class, () -> staffAssignmentService.createAssignment(
                sessionA.getId(), rosterCenterB.getId(), StaffRole.VACCINATOR, sessionStart, sessionEnd
        ));
    }

    @Test
    @DisplayName("createAssignment - rejected when roster entry is INACTIVE")
    void createAssignment_inactiveRoster_throwsException() {
        assertThrows(IllegalStateException.class, () -> staffAssignmentService.createAssignment(
                sessionA.getId(), rosterInactive.getId(), StaffRole.VACCINATOR, sessionStart, sessionEnd
        ));
    }

    @Test
    @DisplayName("createAssignment - rejected when requested role does not match roster qualification")
    void createAssignment_roleMismatch_throwsException() {
        assertThrows(IllegalStateException.class, () -> staffAssignmentService.createAssignment(
                sessionA.getId(), rosterVaccinatorActive.getId(), StaffRole.CHECK_IN_STAFF, sessionStart, sessionEnd
        ));
    }

    @Test
    @DisplayName("createAssignment - rejected when staff user is already assigned to overlapping window (cross-center)")
    void createAssignment_crossCenterOverlappingUser_throwsException() {
        Instant t1 = Instant.parse("2026-09-01T08:00:00Z");
        Instant t2 = Instant.parse("2026-09-01T12:00:00Z");

        // User 501 assigned at Center A
        staffAssignmentService.createAssignment(sessionA.getId(), rosterVaccinatorActive.getId(), StaffRole.VACCINATOR, t1, t2);

        // Attempt overlapping assignment for User 501 at Center B [10:00, 14:00)
        Instant overlapStart = Instant.parse("2026-09-01T10:00:00Z");
        Instant overlapEnd = Instant.parse("2026-09-01T14:00:00Z");

        assertThrows(IllegalStateException.class, () -> staffAssignmentService.createAssignment(
                sessionB.getId(), rosterCenterB.getId(), StaffRole.VACCINATOR, overlapStart, overlapEnd
        ));
    }

    @Test
    @DisplayName("getAssignmentsForSession - returns deterministic order startAt ASC, id ASC")
    void getAssignmentsForSession_returnsDeterministicOrder() {
        Instant t1 = Instant.parse("2026-09-01T08:00:00Z");
        Instant t2 = Instant.parse("2026-09-01T12:00:00Z");
        Instant t3 = Instant.parse("2026-09-01T16:00:00Z");

        StaffAssignment a2 = staffAssignmentService.createAssignment(sessionA.getId(), rosterVaccinatorActive.getId(), StaffRole.VACCINATOR, t2, t3);
        StaffAssignment a1 = staffAssignmentService.createAssignment(sessionA.getId(), rosterCheckInActive.getId(), StaffRole.CHECK_IN_STAFF, t1, t2);

        List<StaffAssignment> list = staffAssignmentService.getAssignmentsForSession(sessionA.getId());
        assertEquals(2, list.size());
        assertEquals(a1.getId(), list.get(0).getId());
        assertEquals(a2.getId(), list.get(1).getId());
    }

    @Test
    @DisplayName("getAssignment and deleteAssignment - session ownership and DRAFT deletion validation")
    void getAndDeleteAssignment_validation() {
        Instant t1 = Instant.parse("2026-09-01T08:00:00Z");
        Instant t2 = Instant.parse("2026-09-01T12:00:00Z");
        StaffAssignment assignment = staffAssignmentService.createAssignment(
                sessionA.getId(), rosterVaccinatorActive.getId(), StaffRole.VACCINATOR, t1, t2
        );

        // Single retrieval
        StaffAssignment retrieved = staffAssignmentService.getAssignment(sessionA.getId(), assignment.getId());
        assertEquals(assignment.getId(), retrieved.getId());

        // Wrong session ID
        assertThrows(ResourceNotFoundException.class, () -> staffAssignmentService.getAssignment(9999L, assignment.getId()));

        // Delete from DRAFT session succeeds and preserves roster entry
        staffAssignmentService.deleteAssignment(sessionA.getId(), assignment.getId());
        assertFalse(staffAssignmentRepository.existsById(assignment.getId()));
        assertTrue(rosterRepository.existsById(rosterVaccinatorActive.getId()));

        // Non-DRAFT delete rejected
        StaffAssignment a2 = staffAssignmentService.createAssignment(
                sessionA.getId(), rosterVaccinatorActive.getId(), StaffRole.VACCINATOR, t1, t2
        );
        ReflectionTestUtils.setField(sessionA, "lifecycleStatus", SessionLifecycleStatus.CANCELLED);
        sessionRepository.save(sessionA);

        assertThrows(IllegalStateException.class, () -> staffAssignmentService.deleteAssignment(sessionA.getId(), a2.getId()));
    }
}
