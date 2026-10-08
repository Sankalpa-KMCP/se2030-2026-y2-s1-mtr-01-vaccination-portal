package com.se2030.vaccinationportal.campaign.center;

import com.se2030.vaccinationportal.campaign.CampaignTestFixtures;


import com.se2030.vaccinationportal.campaign.center.model.CampaignCenterAssignment;
import com.se2030.vaccinationportal.campaign.center.model.CampaignCenterAssignmentStatus;
import com.se2030.vaccinationportal.campaign.core.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.center.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.reference.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.reference.model.VaccineType;
import com.se2030.vaccinationportal.campaign.center.repository.CampaignCenterAssignmentRepository;
import com.se2030.vaccinationportal.campaign.reference.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.core.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.center.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.reference.repository.VaccineTypeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnitUtil;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CampaignCenterAssignmentPersistenceIntegrationTests {

    private static final LocalDate START = LocalDate.of(2026, 9, 15);
    private static final LocalDate END = LocalDate.of(2026, 10, 15);

    @Autowired private CampaignCenterAssignmentRepository assignmentRepository;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @PersistenceContext private EntityManager entityManager;

    private VaccinationCampaign campaignA;
    private VaccinationCampaign campaignB;
    private VaccinationCenter centerA;
    private VaccinationCenter centerB;

    @BeforeEach
    void setUp() {
        VaccineType vaccine = vaccineTypeRepository.saveAndFlush(
                new VaccineType("VAX-ASSIGN-P", "Assignment Persistence Vaccine"));
        TargetGroup group = targetGroupRepository.saveAndFlush(
                new TargetGroup("TG-ASSIGN-P", "Assignment Persistence Group"));
        campaignA = saveCampaign("CMP-ASSIGN-PA", CampaignStatus.DRAFT, vaccine, group);
        campaignB = saveCampaign("CMP-ASSIGN-PB", CampaignStatus.ACTIVE, vaccine, group);
        centerA = saveCenter("CTR-ASSIGN-PA", CenterStatus.ACTIVE);
        centerB = saveCenter("CTR-ASSIGN-PB", CenterStatus.INACTIVE);
    }

    @Test
    void persistsParentsStatusAuditAndScalarCampaignLookup() {
        CampaignCenterAssignment saved = assignmentRepository.saveAndFlush(
                new CampaignCenterAssignment(campaignA, centerA, START, END));

        CampaignCenterAssignment reloaded = assignmentRepository.findById(saved.getId()).orElseThrow();
        assertEquals(campaignA.getId(), reloaded.getCampaign().getId());
        assertEquals(centerA.getId(), reloaded.getCenter().getId());
        assertEquals(CampaignCenterAssignmentStatus.ACTIVE, reloaded.getStatus());
        assertNotNull(reloaded.getCreatedAt());
        assertNotNull(reloaded.getUpdatedAt());
        assertEquals(campaignA.getId(), assignmentRepository.findCampaignIdById(saved.getId()).orElseThrow());
    }

    @Test
    void parentRelationshipsRemainLazyAfterPersistenceContextClear() {
        CampaignCenterAssignment saved = assignmentRepository.saveAndFlush(
                new CampaignCenterAssignment(campaignA, centerA, START, END));
        Long assignmentId = saved.getId();
        entityManager.clear();

        CampaignCenterAssignment reloaded = assignmentRepository.findById(assignmentId).orElseThrow();
        PersistenceUnitUtil persistenceUnitUtil = entityManager.getEntityManagerFactory()
                .getPersistenceUnitUtil();

        assertFalse(persistenceUnitUtil.isLoaded(reloaded, "campaign"));
        assertFalse(persistenceUnitUtil.isLoaded(reloaded, "center"));
        assertEquals("CMP-ASSIGN-PA", reloaded.getCampaign().getCode());
        assertEquals("CTR-ASSIGN-PA", reloaded.getCenter().getCode());
        assertTrue(persistenceUnitUtil.isLoaded(reloaded, "campaign"));
        assertTrue(persistenceUnitUtil.isLoaded(reloaded, "center"));
    }

    @Test
    void databaseRequiresBothParentRelationships() {
        Instant audit = Instant.parse("2026-09-01T00:00:00Z");
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                "INSERT INTO campaign_center_assignments " +
                        "(campaign_id, center_id, active_from, assignment_status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?)",
                null, centerA.getId(), START, "ACTIVE", audit, audit));
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                "INSERT INTO campaign_center_assignments " +
                        "(campaign_id, center_id, active_from, assignment_status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?)",
                campaignA.getId(), null, START, "ACTIVE", audit, audit));
    }

    @Test
    void campaignAndCenterHistoriesAreDeterministicAndIsolated() {
        CampaignCenterAssignment finiteLong = save(campaignA, centerA, START, END.plusDays(2));
        CampaignCenterAssignment finiteShort = save(campaignA, centerB, START, END);
        CampaignCenterAssignment open = save(campaignA, centerA, START, null);
        CampaignCenterAssignment later = save(campaignA, centerA, START.plusMonths(2), null);
        save(campaignB, centerA, START, END);
        assignmentRepository.flush();

        assertEquals(
                List.of(finiteShort.getId(), finiteLong.getId(), open.getId(), later.getId()),
                assignmentRepository.findCampaignHistory(campaignA.getId()).stream()
                        .map(CampaignCenterAssignment::getId).toList());
        assertEquals(
                List.of(finiteLong.getId(), open.getId(), later.getId()),
                assignmentRepository.findCenterHistory(centerA.getId()).stream()
                        .filter(a -> a.getCampaign().getId().equals(campaignA.getId()))
                        .map(CampaignCenterAssignment::getId).toList());
    }

    @Test
    void effectiveLookupUsesInclusiveStartExclusiveEndAndIgnoresCancelled() {
        CampaignCenterAssignment active = save(campaignA, centerA, START, END);
        CampaignCenterAssignment cancelled = new CampaignCenterAssignment(campaignA, centerB, START, END);
        cancelled.cancel();
        assignmentRepository.saveAndFlush(cancelled);

        assertEquals(active.getId(), effective(campaignA, centerA, START).getId());
        assertEquals(active.getId(), effective(campaignA, centerA, END.minusDays(1)).getId());
        assertTrue(assignmentRepository.findEffectiveAssignmentAt(
                campaignA.getId(), centerA.getId(), CampaignCenterAssignmentStatus.ACTIVE, END).isEmpty());
        assertTrue(assignmentRepository.findEffectiveAssignmentAt(
                campaignA.getId(), centerB.getId(), CampaignCenterAssignmentStatus.ACTIVE, START).isEmpty());
    }

    @Test
    void overlapQueryHandlesFiniteShapesAdjacencyAndPairIndependence() {
        save(campaignA, centerA, START, END);

        List<LocalDate[]> overlaps = List.of(
                new LocalDate[]{START, END},
                new LocalDate[]{START.minusDays(2), START.plusDays(1)},
                new LocalDate[]{END.minusDays(1), END.plusDays(2)},
                new LocalDate[]{START.minusDays(2), END.plusDays(2)},
                new LocalDate[]{START.plusDays(1), END.minusDays(1)}
        );
        for (LocalDate[] candidate : overlaps) {
            assertTrue(overlaps(campaignA, centerA, null, candidate[0], candidate[1]));
        }

        assertFalse(overlaps(campaignA, centerA, null, START.minusDays(2), START));
        assertFalse(overlaps(campaignA, centerA, null, END, END.plusDays(2)));
        assertFalse(overlaps(campaignA, centerB, null, START, END));
        assertFalse(overlaps(campaignB, centerA, null, START, END));
    }

    @Test
    void openEndedAndCancelledPeriodsUseActiveOverlapRules() {
        save(campaignA, centerA, START, null);
        CampaignCenterAssignment cancelled = new CampaignCenterAssignment(campaignA, centerB, START, null);
        cancelled.cancel();
        assignmentRepository.saveAndFlush(cancelled);

        assertTrue(overlaps(campaignA, centerA, null, START.plusYears(10), null));
        assertTrue(overlaps(campaignA, centerA, null, START.minusDays(1), START.plusDays(1)));
        assertFalse(overlaps(campaignA, centerA, null, START.minusDays(2), START));
        assertFalse(overlaps(campaignA, centerB, null, START.plusDays(1), null));
    }

    @Test
    void selfExclusionStillFindsAnotherOverlappingSibling() {
        CampaignCenterAssignment first = save(campaignA, centerA, START, END);
        save(campaignA, centerA, END, END.plusDays(10));
        assignmentRepository.flush();

        assertFalse(overlaps(campaignA, centerA, first.getId(), START, END));
        assertTrue(overlaps(campaignA, centerA, first.getId(), END.minusDays(1), END.plusDays(1)));
    }

    @Test
    void fullSpanCoverageRequiresOneActiveAssignmentAcrossEntireHalfOpenSpan() {
        CampaignCenterAssignment finite = save(campaignA, centerA, START, END);
        CampaignCenterAssignment open = save(campaignB, centerA, START, null);

        assertTrue(covers(campaignA, centerA, START, END));
        assertTrue(covers(campaignA, centerA, START.plusDays(1), END.minusDays(1)));
        assertFalse(covers(campaignA, centerA, START.minusDays(1), END));
        assertFalse(covers(campaignA, centerA, START, END.plusDays(1)));
        assertTrue(covers(campaignB, centerA, START.plusYears(10), START.plusYears(10).plusDays(1)));
        finite.cancel();
        assignmentRepository.saveAndFlush(finite);
        assertFalse(covers(campaignA, centerA, START, END));
        assertTrue(open.getStatus() == CampaignCenterAssignmentStatus.ACTIVE);
    }

    private CampaignCenterAssignment effective(
            VaccinationCampaign campaign,
            VaccinationCenter center,
            LocalDate date
    ) {
        return assignmentRepository.findEffectiveAssignmentAt(
                campaign.getId(), center.getId(), CampaignCenterAssignmentStatus.ACTIVE, date).orElseThrow();
    }

    private boolean overlaps(
            VaccinationCampaign campaign,
            VaccinationCenter center,
            Long excludeId,
            LocalDate from,
            LocalDate to
    ) {
        return assignmentRepository.existsOverlappingAssignment(
                campaign.getId(), center.getId(), CampaignCenterAssignmentStatus.ACTIVE,
                excludeId, from, to);
    }

    private boolean covers(
            VaccinationCampaign campaign,
            VaccinationCenter center,
            LocalDate from,
            LocalDate to
    ) {
        return assignmentRepository.existsActiveAssignmentCovering(
                campaign.getId(), center.getId(), CampaignCenterAssignmentStatus.ACTIVE, from, to);
    }

    private CampaignCenterAssignment save(
            VaccinationCampaign campaign,
            VaccinationCenter center,
            LocalDate from,
            LocalDate to
    ) {
        return assignmentRepository.save(new CampaignCenterAssignment(campaign, center, from, to));
    }

    private VaccinationCampaign saveCampaign(
            String code,
            CampaignStatus status,
            VaccineType vaccine,
            TargetGroup group
    ) {
        return campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                code, "Assignment Campaign " + code, null, vaccine, group,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31),
                Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-12-30T00:00:00Z"), status));
    }

    private VaccinationCenter saveCenter(String code, CenterStatus status) {
        return centerRepository.saveAndFlush(new VaccinationCenter(
                code, "Assignment Center " + code, "1 Health Road", "+94 11 000 0000",
                status, 100, 50, "UTC"));
    }
}
