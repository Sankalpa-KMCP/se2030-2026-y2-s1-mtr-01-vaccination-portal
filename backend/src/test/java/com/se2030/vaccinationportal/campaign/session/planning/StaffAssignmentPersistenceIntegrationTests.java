package com.se2030.vaccinationportal.campaign.session.planning;

import com.se2030.vaccinationportal.campaign.CampaignTestFixtures;


import com.se2030.vaccinationportal.campaign.core.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.center.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.center.model.OperationalStaffRoster;
import com.se2030.vaccinationportal.campaign.session.planning.model.StaffAssignment;
import com.se2030.vaccinationportal.campaign.center.model.StaffRole;
import com.se2030.vaccinationportal.campaign.reference.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.session.planning.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.reference.model.VaccineType;
import com.se2030.vaccinationportal.campaign.center.repository.OperationalStaffRosterRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.StaffAssignmentRepository;
import com.se2030.vaccinationportal.campaign.reference.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.core.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.center.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.reference.repository.VaccineTypeRepository;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Module: MF-02 — Vaccination Campaign, Center and Session Management
 * Use Case: UC-10B — Assign Staff / Resources and Approve Capacity
 * Responsibility: Session staff/resource assignment and capacity planning
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StaffAssignmentPersistenceIntegrationTests {

    @Autowired private StaffAssignmentRepository assignmentRepository;
    @Autowired private OperationalStaffRosterRepository rosterRepository;
    @Autowired private VaccinationSessionRepository sessionRepository;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;

    private VaccinationCenter center;
    private VaccinationCampaign campaign;
    private VaccinationSession session1;
    private VaccinationSession session2;
    private VaccinationSession sessionCancelled;
    private OperationalStaffRoster rosterUser1;
    private OperationalStaffRoster rosterUser2;

    @BeforeEach
    void setUp() {
        center = centerRepository.save(new VaccinationCenter("CTR-STAFF-OV", "Staff Overlap Center", "123 St", "+111", CenterStatus.ACTIVE, 50, 100, "UTC"));
        VaccineType vaccine = vaccineTypeRepository.save(new VaccineType("VAX-STAFF-OV", "Overlap Vaccine"));
        TargetGroup targetGroup = targetGroupRepository.save(new TargetGroup("TG-STAFF-OV", "Overlap Target Group"));
        campaign = campaignRepository.save(CampaignTestFixtures.campaign(
                "CMP-STAFF-OV", "Autumn 2026", "COVID-19",
                vaccine, targetGroup,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 1),
                Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-12-01T00:00:00Z"),
                CampaignStatus.DRAFT
        ));

        session1 = sessionRepository.save(new VaccinationSession(
                campaign, center,
                Instant.parse("2026-10-01T09:00:00Z"), Instant.parse("2026-10-01T12:00:00Z"),
                Instant.parse("2026-10-01T08:00:00Z"), "UTC"
        ));

        session2 = sessionRepository.save(new VaccinationSession(
                campaign, center,
                Instant.parse("2026-10-01T11:00:00Z"), Instant.parse("2026-10-01T14:00:00Z"),
                Instant.parse("2026-10-01T08:00:00Z"), "UTC"
        ));

        sessionCancelled = new VaccinationSession(
                campaign, center,
                Instant.parse("2026-10-01T09:00:00Z"), Instant.parse("2026-10-01T12:00:00Z"),
                Instant.parse("2026-10-01T08:00:00Z"), "UTC"
        );
        sessionCancelled.cancel();
        sessionCancelled = sessionRepository.save(sessionCancelled);

        rosterUser1 = rosterRepository.save(new OperationalStaffRoster(center, 501L, StaffRole.VACCINATOR));
        rosterUser2 = rosterRepository.save(new OperationalStaffRoster(center, 502L, StaffRole.VACCINATOR));
    }

    @Test
    void persistsStaffAssignment() {
        StaffAssignment saved = assignmentRepository.saveAndFlush(
                new StaffAssignment(session1, rosterUser1, StaffRole.VACCINATOR,
                        Instant.parse("2026-10-01T09:00:00Z"), Instant.parse("2026-10-01T12:00:00Z"))
        );
        assertNotNull(saved.getId());
        assertEquals(501L, saved.getUserId());
        assertEquals(StaffRole.VACCINATOR, saved.getRole());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void detectsOverlapOnSameUser() {
        // Assign user 501 to session 1 from 09:00 to 12:00
        assignmentRepository.saveAndFlush(
                new StaffAssignment(session1, rosterUser1, StaffRole.VACCINATOR,
                        Instant.parse("2026-10-01T09:00:00Z"), Instant.parse("2026-10-01T12:00:00Z"))
        );

        // Candidate 11:00 to 14:00 for user 501 overlaps with 09:00-12:00
        boolean overlaps = assignmentRepository.existsOverlappingAssignment(
                501L,
                null,
                Instant.parse("2026-10-01T11:00:00Z"),
                Instant.parse("2026-10-01T14:00:00Z")
        );
        assertTrue(overlaps);

        // Candidate for different user 502 does NOT overlap
        assertFalse(assignmentRepository.existsOverlappingAssignment(
                502L,
                null,
                Instant.parse("2026-10-01T11:00:00Z"),
                Instant.parse("2026-10-01T14:00:00Z")
        ));
    }

    @Test
    void boundaryTouchingDoesNotOverlapInHalfOpenInterval() {
        // Assign user 501 to session 1 from 09:00 to 12:00
        assignmentRepository.saveAndFlush(
                new StaffAssignment(session1, rosterUser1, StaffRole.VACCINATOR,
                        Instant.parse("2026-10-01T09:00:00Z"), Instant.parse("2026-10-01T12:00:00Z"))
        );

        // Candidate 12:00 to 15:00 touches boundary at 12:00 -> NOT an overlap in half-open [09:00, 12:00)
        assertFalse(assignmentRepository.existsOverlappingAssignment(
                501L,
                null,
                Instant.parse("2026-10-01T12:00:00Z"),
                Instant.parse("2026-10-01T15:00:00Z")
        ));
    }

    @Test
    void cancelledSessionAssignmentDoesNotBlockStaff() {
        // Assign user 501 to CANCELLED session
        assignmentRepository.saveAndFlush(
                new StaffAssignment(sessionCancelled, rosterUser1, StaffRole.VACCINATOR,
                        Instant.parse("2026-10-01T09:00:00Z"), Instant.parse("2026-10-01T12:00:00Z"))
        );

        // Candidate 09:00 to 12:00 on user 501 is NOT blocked because session is CANCELLED
        assertFalse(assignmentRepository.existsOverlappingAssignment(
                501L,
                null,
                Instant.parse("2026-10-01T09:00:00Z"),
                Instant.parse("2026-10-01T12:00:00Z")
        ));
    }

    @Test
    void detectsOverlap_acrossDifferentCentersForSameUser() {
        VaccinationCenter center2 = centerRepository.save(
                new VaccinationCenter("CTR-STAFF-OV2", "Staff Overlap Center 2", "456 St", "+222", CenterStatus.ACTIVE, 50, 100, "UTC")
        );
        VaccinationSession sessionCenter2 = sessionRepository.save(new VaccinationSession(
                campaign, center2,
                Instant.parse("2026-10-01T11:00:00Z"), Instant.parse("2026-10-01T14:00:00Z"),
                Instant.parse("2026-10-01T08:00:00Z"), "UTC"
        ));
        OperationalStaffRoster rosterUser1Center2 = rosterRepository.save(
                new OperationalStaffRoster(center2, 501L, StaffRole.CHECK_IN_STAFF)
        );

        // Assign user 501 at center 1 for session1 from 09:00 to 12:00
        assignmentRepository.saveAndFlush(
                new StaffAssignment(session1, rosterUser1, StaffRole.VACCINATOR,
                        Instant.parse("2026-10-01T09:00:00Z"), Instant.parse("2026-10-01T12:00:00Z"))
        );

        // Overlap query for user 501 at center 2 from 11:00 to 14:00 MUST detect conflict globally
        assertTrue(assignmentRepository.existsOverlappingAssignment(
                rosterUser1Center2.getUserId(),
                null,
                Instant.parse("2026-10-01T11:00:00Z"),
                Instant.parse("2026-10-01T14:00:00Z")
        ));
    }
}
