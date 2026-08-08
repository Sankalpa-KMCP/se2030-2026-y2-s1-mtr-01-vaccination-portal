package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CampaignLocationTarget;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.LocationTargetType;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.CampaignLocationTargetRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.service.CampaignLocationTargetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests verifying CampaignLocationTarget persistence, uniqueness constraints,
 * queries, and service-level administrative targeting rules (WBVP-SE2030 / MF02-BR-04).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CampaignLocationTargetPersistenceIntegrationTests {

    @Autowired
    private VaccineTypeRepository vaccineTypeRepository;

    @Autowired
    private TargetGroupRepository targetGroupRepository;

    @Autowired
    private VaccinationCampaignRepository campaignRepository;

    @Autowired
    private CampaignLocationTargetRepository locationTargetRepository;

    @Autowired
    private CampaignLocationTargetService locationTargetService;

    private VaccinationCampaign campaignA;
    private VaccinationCampaign campaignB;

    @BeforeEach
    void setUp() {
        VaccineType vt = vaccineTypeRepository.saveAndFlush(
                new VaccineType("VAX-MRNA-LOC", "mRNA Location Test", "dose", true)
        );
        TargetGroup tg = targetGroupRepository.saveAndFlush(
                new TargetGroup("TG-ALL-LOC", "General Population Location Test", true)
        );

        campaignA = campaignRepository.saveAndFlush(new VaccinationCampaign(
                "CMP-LOC-A",
                "Campaign A Western",
                "Western province targeting",
                vt,
                tg,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31),
                LocalDateTime.of(2026, 8, 15, 8, 0),
                LocalDateTime.of(2026, 12, 30, 18, 0),
                CampaignStatus.DRAFT
        ));

        campaignB = campaignRepository.saveAndFlush(new VaccinationCampaign(
                "CMP-LOC-B",
                "Campaign B Central",
                "Central province targeting",
                vt,
                tg,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31),
                LocalDateTime.of(2026, 8, 15, 8, 0),
                LocalDateTime.of(2026, 12, 30, 18, 0),
                CampaignStatus.PUBLISHED
        ));
    }

    @Test
    @DisplayName("Persist single and multiple administrative location targets for a campaign")
    void persistMultipleLocationTargetsForCampaign() {
        CampaignLocationTarget target1 = locationTargetService.addTarget(
                campaignA.getId(), LocationTargetType.PROVINCE, "wp"
        );
        CampaignLocationTarget target2 = locationTargetService.addTarget(
                campaignA.getId(), LocationTargetType.DISTRICT, "colombo"
        );
        CampaignLocationTarget target3 = locationTargetService.addTarget(
                campaignA.getId(), LocationTargetType.DISTRICT, "gampaha"
        );

        assertNotNull(target1.getId());
        assertEquals("WP", target1.getTargetCode());
        assertEquals("COLOMBO", target2.getTargetCode());
        assertEquals("GAMPAHA", target3.getTargetCode());

        List<CampaignLocationTarget> allA = locationTargetService.getTargetsForCampaign(campaignA.getId());
        assertEquals(3, allA.size());

        List<CampaignLocationTarget> districtsA = locationTargetService.getTargetsForCampaignAndType(
                campaignA.getId(), LocationTargetType.DISTRICT
        );
        assertEquals(2, districtsA.size());
    }

    @Test
    @DisplayName("Duplicate campaign, targetType, and targetCode violates unique constraint")
    void duplicateLocationTargetViolatesUniqueConstraint() {
        locationTargetService.addTarget(campaignA.getId(), LocationTargetType.PROVINCE, "WP");

        // Service-level duplicate rejection
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                locationTargetService.addTarget(campaignA.getId(), LocationTargetType.PROVINCE, "wp")
        );
        assertTrue(ex.getMessage().contains("Location target already exists"));

        // Direct repository duplicate insertion violates database constraint
        CampaignLocationTarget directDuplicate = new CampaignLocationTarget(
                campaignA, LocationTargetType.PROVINCE, "WP"
        );
        assertThrows(DataIntegrityViolationException.class, () -> {
            locationTargetRepository.saveAndFlush(directDuplicate);
        });
    }

    @Test
    @DisplayName("Same target code with different target types is permitted")
    void sameTargetCodeWithDifferentTypesAllowed() {
        CampaignLocationTarget t1 = locationTargetService.addTarget(
                campaignA.getId(), LocationTargetType.PROVINCE, "CENTRAL"
        );
        CampaignLocationTarget t2 = locationTargetService.addTarget(
                campaignA.getId(), LocationTargetType.HEALTH_REGION, "CENTRAL"
        );

        assertNotNull(t1.getId());
        assertNotNull(t2.getId());
        assertNotEquals(t1.getId(), t2.getId());
        assertEquals(2, locationTargetRepository.findByCampaignId(campaignA.getId()).size());
    }

    @Test
    @DisplayName("Same target type and code across different campaigns is permitted")
    void sameTargetTypeAndCodeAcrossDifferentCampaignsAllowed() {
        CampaignLocationTarget targetA = locationTargetService.addTarget(
                campaignA.getId(), LocationTargetType.PROVINCE, "WP"
        );
        CampaignLocationTarget targetB = locationTargetService.addTarget(
                campaignB.getId(), LocationTargetType.PROVINCE, "WP"
        );

        assertNotNull(targetA.getId());
        assertNotNull(targetB.getId());

        List<CampaignLocationTarget> wpTargets = locationTargetRepository.findByTargetTypeAndTargetCode(
                LocationTargetType.PROVINCE, "WP"
        );
        assertEquals(2, wpTargets.size());
    }

    @Test
    @DisplayName("Exact matching and no administrative hierarchy inference")
    void exactMatchingDoesNotInferHierarchy() {
        locationTargetService.addTarget(campaignA.getId(), LocationTargetType.PROVINCE, "WP");

        // Exact match on targeted type and code
        assertTrue(locationTargetService.isCampaignTargeting(campaignA.getId(), LocationTargetType.PROVINCE, "WP"));
        assertTrue(locationTargetService.isCampaignTargeting(campaignA.getId(), LocationTargetType.PROVINCE, "  wp "));

        // Different code under same type returns false
        assertFalse(locationTargetService.isCampaignTargeting(campaignA.getId(), LocationTargetType.PROVINCE, "CP"));

        // Subordinate or different type is NOT inferred (e.g. DISTRICT=COLOMBO does not imply PROVINCE=WP)
        assertFalse(locationTargetService.isCampaignTargeting(campaignA.getId(), LocationTargetType.DISTRICT, "COLOMBO"));
        assertFalse(locationTargetService.isCampaignTargeting(campaignA.getId(), LocationTargetType.HEALTH_REGION, "WP"));
    }

    @Test
    @DisplayName("Repository lookup by specific campaign, target type, and target code")
    void repositoryLookupByCampaignTypeAndCode() {
        locationTargetService.addTarget(campaignA.getId(), LocationTargetType.CENTER, "CTR-NCMC");

        Optional<CampaignLocationTarget> found = locationTargetRepository.findByCampaignIdAndTargetTypeAndTargetCode(
                campaignA.getId(), LocationTargetType.CENTER, "CTR-NCMC"
        );
        assertTrue(found.isPresent());
        assertEquals("CTR-NCMC", found.get().getTargetCode());

        Optional<CampaignLocationTarget> notFound = locationTargetRepository.findByCampaignIdAndTargetTypeAndTargetCode(
                campaignA.getId(), LocationTargetType.CENTER, "CTR-OTHER"
        );
        assertTrue(notFound.isEmpty());
    }

    @Test
    @DisplayName("Batch addition of targets operates atomically")
    void batchAdditionOperatesAtomically() {
        List<CampaignLocationTargetService.TargetEntry> entries = List.of(
                new CampaignLocationTargetService.TargetEntry(LocationTargetType.DISTRICT, "COLOMBO"),
                new CampaignLocationTargetService.TargetEntry(LocationTargetType.DISTRICT, "GAMPAHA"),
                new CampaignLocationTargetService.TargetEntry(LocationTargetType.CENTER, "CTR-NCMC")
        );

        List<CampaignLocationTarget> added = locationTargetService.addTargets(campaignA.getId(), entries);
        assertEquals(3, added.size());

        List<CampaignLocationTarget> fromDb = locationTargetService.getTargetsForCampaign(campaignA.getId());
        assertEquals(3, fromDb.size());
    }
}
