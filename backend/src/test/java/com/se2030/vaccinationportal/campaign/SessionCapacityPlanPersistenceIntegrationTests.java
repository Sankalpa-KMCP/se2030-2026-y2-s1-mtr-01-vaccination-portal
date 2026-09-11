package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.SessionCapacityPlan;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.SessionCapacityPlanRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Module: MF-02 — Vaccination Campaign, Center and Session Management
 * Use Case: UC-10B — Assign Staff / Resources and Approve Capacity
 * Responsibility: Session staff/resource assignment and capacity planning
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SessionCapacityPlanPersistenceIntegrationTests {

    @Autowired private SessionCapacityPlanRepository planRepository;
    @Autowired private VaccinationSessionRepository sessionRepository;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;

    private VaccinationCenter center;
    private VaccinationCampaign campaign;
    private VaccinationSession session;

    @BeforeEach
    void setUp() {
        center = centerRepository.save(new VaccinationCenter("CTR-PLAN-01", "Plan Center", "123 Plan St", "+111", CenterStatus.ACTIVE, 60, 120, "UTC"));
        VaccineType vaccine = vaccineTypeRepository.save(new VaccineType("VAX-PLAN-01", "Plan Vaccine"));
        TargetGroup targetGroup = targetGroupRepository.save(new TargetGroup("TG-PLAN-01", "Plan Target Group"));
        campaign = campaignRepository.save(CampaignTestFixtures.campaign(
                "CMP-PLAN-01", "COVID Plan Campaign", "COVID-19",
                vaccine, targetGroup,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31),
                Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-12-30T00:00:00Z"),
                CampaignStatus.DRAFT
        ));
        session = sessionRepository.save(new VaccinationSession(
                campaign, center,
                Instant.parse("2026-10-01T09:00:00Z"), Instant.parse("2026-10-01T12:00:00Z"),
                Instant.parse("2026-10-01T08:00:00Z"), "UTC"
        ));
    }

    @Test
    void persistsPlanAndFindsLatestRevision() {
        SessionCapacityPlan rev1 = planRepository.saveAndFlush(
                new SessionCapacityPlan(session, 1, 180, 10, 3, 4, 3, 3, 54, 54, 60, 15, 0.0, 120, null, "admin-1")
        );
        assertNotNull(rev1.getId());
        assertNotNull(rev1.getCreatedAt());
        assertNotNull(rev1.getUpdatedAt());

        SessionCapacityPlan rev2 = planRepository.saveAndFlush(
                new SessionCapacityPlan(session, 2, 180, 10, 4, 4, 4, 4, 72, 72, 60, 15, 0.0, 120, null, "admin-2")
        );

        SessionCapacityPlan latest = planRepository.findTopBySessionIdOrderByRevisionDesc(session.getId()).orElseThrow();
        assertEquals(2, latest.getRevision());
        assertEquals(72, latest.getApprovedCapacity());
        assertEquals("admin-2", latest.getApprovedBy());

        List<SessionCapacityPlan> history = planRepository.findBySessionIdOrderByRevisionAsc(session.getId());
        assertEquals(2, history.size());
        assertEquals(1, history.get(0).getRevision());
        assertEquals(2, history.get(1).getRevision());
    }

    @Test
    void enforcesUniqueRevisionPerSession() {
        planRepository.saveAndFlush(
                new SessionCapacityPlan(session, 1, 180, 10, 3, 4, 3, 3, 54, 54, 60, 15, 0.0, 120, null, "admin-1")
        );

        // Attempting to save another plan with revision 1 for the same session must fail
        assertThrows(DataIntegrityViolationException.class, () -> {
            planRepository.saveAndFlush(
                    new SessionCapacityPlan(session, 1, 180, 10, 4, 4, 4, 4, 72, 72, 60, 15, 0.0, 120, null, "admin-2")
            );
        });
    }

    @Test
    void persistsNullableCeilingAndOverrideReason() {
        SessionCapacityPlan planNoCeilingWithOverride = planRepository.saveAndFlush(
                new SessionCapacityPlan(session, 1, 180, 10, 3, 4, 3, 3, 54, 60, 60, 15, 0.2, null, "Extra volunteer nurses present", "supervisor-1")
        );

        SessionCapacityPlan reloaded = planRepository.findById(planNoCeilingWithOverride.getId()).orElseThrow();
        assertNull(reloaded.getSessionServiceCeiling());
        assertEquals("Extra volunteer nurses present", reloaded.getOverrideReason());
        assertEquals("supervisor-1", reloaded.getApprovedBy());
        assertEquals(0.2, reloaded.getExpectedAccompanyingPersonsPerBooking(), 0.0001);
    }
}
