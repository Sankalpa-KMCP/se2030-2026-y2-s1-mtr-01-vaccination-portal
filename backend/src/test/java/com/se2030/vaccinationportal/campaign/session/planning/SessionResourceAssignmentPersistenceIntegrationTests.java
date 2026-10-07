package com.se2030.vaccinationportal.campaign.session.planning;

import com.se2030.vaccinationportal.campaign.CampaignTestFixtures;


import com.se2030.vaccinationportal.campaign.core.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.center.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.center.model.OperationalResource;
import com.se2030.vaccinationportal.campaign.center.model.ResourceType;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

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
class SessionResourceAssignmentPersistenceIntegrationTests {

    @Autowired private SessionResourceAssignmentRepository assignmentRepository;
    @Autowired private OperationalResourceRepository resourceRepository;
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
    private OperationalResource resource1;
    private OperationalResource resource2;

    @BeforeEach
    void setUp() {
        center = centerRepository.save(new VaccinationCenter("CTR-RES-OV", "Resource Overlap Center", "123 St", "+111", CenterStatus.ACTIVE, 50, 100, "UTC"));
        VaccineType vaccine = vaccineTypeRepository.save(new VaccineType("VAX-RES-OV", "Overlap Vaccine"));
        TargetGroup targetGroup = targetGroupRepository.save(new TargetGroup("TG-RES-OV", "Overlap Target Group"));
        campaign = campaignRepository.save(CampaignTestFixtures.campaign(
                "CMP-RES-OV", "Autumn 2026", "COVID-19",
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

        resource1 = resourceRepository.save(new OperationalResource(center, "STATION-1", ResourceType.VACCINATION_STATION, "Station 1"));
        resource2 = resourceRepository.save(new OperationalResource(center, "STATION-2", ResourceType.VACCINATION_STATION, "Station 2"));
    }

    @Test
    void persistsAssignment() {
        SessionResourceAssignment saved = assignmentRepository.saveAndFlush(
                new SessionResourceAssignment(session1, resource1,
                        Instant.parse("2026-10-01T09:00:00Z"), Instant.parse("2026-10-01T12:00:00Z"))
        );
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void detectsOverlapOnSameResource() {
        // Session 1 assigns resource 1 from 09:00 to 12:00
        assignmentRepository.saveAndFlush(
                new SessionResourceAssignment(session1, resource1,
                        Instant.parse("2026-10-01T09:00:00Z"), Instant.parse("2026-10-01T12:00:00Z"))
        );

        // Candidate 11:00 to 14:00 overlaps with 09:00-12:00
        boolean overlaps = assignmentRepository.existsOverlappingAssignment(
                resource1.getId(),
                null,
                Instant.parse("2026-10-01T11:00:00Z"),
                Instant.parse("2026-10-01T14:00:00Z")
        );
        assertTrue(overlaps);

        // Candidate 08:00 to 10:00 overlaps with 09:00-12:00
        assertTrue(assignmentRepository.existsOverlappingAssignment(
                resource1.getId(),
                null,
                Instant.parse("2026-10-01T08:00:00Z"),
                Instant.parse("2026-10-01T10:00:00Z")
        ));

        // Candidate on different resource2 does NOT overlap
        assertFalse(assignmentRepository.existsOverlappingAssignment(
                resource2.getId(),
                null,
                Instant.parse("2026-10-01T11:00:00Z"),
                Instant.parse("2026-10-01T14:00:00Z")
        ));
    }

    @Test
    void boundaryTouchingDoesNotOverlapInHalfOpenInterval() {
        // Session 1 assigns resource 1 from 09:00 to 12:00
        assignmentRepository.saveAndFlush(
                new SessionResourceAssignment(session1, resource1,
                        Instant.parse("2026-10-01T09:00:00Z"), Instant.parse("2026-10-01T12:00:00Z"))
        );

        // Candidate 12:00 to 15:00 touches boundary at 12:00 -> NOT an overlap in half-open [09:00, 12:00)
        assertFalse(assignmentRepository.existsOverlappingAssignment(
                resource1.getId(),
                null,
                Instant.parse("2026-10-01T12:00:00Z"),
                Instant.parse("2026-10-01T15:00:00Z")
        ));

        // Candidate 06:00 to 09:00 touches boundary at 09:00 -> NOT an overlap
        assertFalse(assignmentRepository.existsOverlappingAssignment(
                resource1.getId(),
                null,
                Instant.parse("2026-10-01T06:00:00Z"),
                Instant.parse("2026-10-01T09:00:00Z")
        ));
    }

    @Test
    void cancelledSessionAssignmentDoesNotBlockResource() {
        // Assign resource 1 to CANCELLED session
        assignmentRepository.saveAndFlush(
                new SessionResourceAssignment(sessionCancelled, resource1,
                        Instant.parse("2026-10-01T09:00:00Z"), Instant.parse("2026-10-01T12:00:00Z"))
        );

        // Candidate 09:00 to 12:00 on resource 1 is NOT blocked because existing session is CANCELLED
        assertFalse(assignmentRepository.existsOverlappingAssignment(
                resource1.getId(),
                null,
                Instant.parse("2026-10-01T09:00:00Z"),
                Instant.parse("2026-10-01T12:00:00Z")
        ));
    }

    @Test
    void excludeAssignmentIdBypassesSelfOnUpdate() {
        SessionResourceAssignment existing = assignmentRepository.saveAndFlush(
                new SessionResourceAssignment(session1, resource1,
                        Instant.parse("2026-10-01T09:00:00Z"), Instant.parse("2026-10-01T12:00:00Z"))
        );

        // Checking overlap excluding self should return false
        assertFalse(assignmentRepository.existsOverlappingAssignment(
                resource1.getId(),
                existing.getId(),
                Instant.parse("2026-10-01T09:00:00Z"),
                Instant.parse("2026-10-01T12:00:00Z")
        ));
    }
}
