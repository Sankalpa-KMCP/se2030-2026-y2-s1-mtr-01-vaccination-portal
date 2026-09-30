package com.se2030.vaccinationportal.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.se2030.vaccinationportal.campaign.reference.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.reference.model.VaccineType;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.reference.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.core.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.reference.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.reference.service.TargetGroupService;
import com.se2030.vaccinationportal.campaign.core.service.VaccinationCampaignService;
import com.se2030.vaccinationportal.campaign.reference.service.VaccineTypeService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MasterDataManagementIntegrationTests {

    @Autowired
    private VaccineTypeService vaccineTypeService;

    @Autowired
    private TargetGroupService targetGroupService;

    @Autowired
    private VaccinationCampaignService campaignService;

    @Autowired
    private VaccineTypeRepository vaccineTypeRepository;

    @Autowired
    private TargetGroupRepository targetGroupRepository;

    @Autowired
    private VaccinationCampaignRepository campaignRepository;

    @Test
    @DisplayName("VaccineType lifecycle: create, update, deactivate, reactivate and filter by active status")
    void vaccineTypeLifecycle() {
        VaccineType created = vaccineTypeService.createVaccineType("VAX-INT-01", "Integration Vaccine 1", "dose");
        assertThat(created.getId()).isNotNull();
        assertThat(created.isActive()).isTrue();

        // Update
        VaccineType updated = vaccineTypeService.updateVaccineType(created.getId(), "Updated Integration Vaccine", "vial");
        assertThat(updated.getName()).isEqualTo("Updated Integration Vaccine");
        assertThat(updated.getDoseUnit()).isEqualTo("vial");
        assertThat(updated.getCode()).isEqualTo("VAX-INT-01"); // Code immutable

        // Deactivate
        VaccineType deactivated = vaccineTypeService.deactivateVaccineType(created.getId());
        assertThat(deactivated.isActive()).isFalse();

        // Check active list excludes it
        List<VaccineType> activeList = vaccineTypeService.getActiveVaccineTypes();
        assertThat(activeList.stream().noneMatch(v -> v.getCode().equals("VAX-INT-01"))).isTrue();

        // Check all list includes it
        List<VaccineType> allList = vaccineTypeService.getVaccineTypes(true);
        assertThat(allList.stream().anyMatch(v -> v.getCode().equals("VAX-INT-01"))).isTrue();

        // Reactivate
        VaccineType reactivated = vaccineTypeService.activateVaccineType(created.getId());
        assertThat(reactivated.isActive()).isTrue();

        activeList = vaccineTypeService.getActiveVaccineTypes();
        assertThat(activeList.stream().anyMatch(v -> v.getCode().equals("VAX-INT-01"))).isTrue();
    }

    @Test
    @DisplayName("TargetGroup lifecycle: create, update, deactivate, reactivate and filter by active status")
    void targetGroupLifecycle() {
        TargetGroup created = targetGroupService.createTargetGroup("TG-INT-01", "Integration Cohort 1");
        assertThat(created.getId()).isNotNull();
        assertThat(created.isActive()).isTrue();

        // Update
        TargetGroup updated = targetGroupService.updateTargetGroup(created.getId(), "Updated Integration Cohort");
        assertThat(updated.getDescription()).isEqualTo("Updated Integration Cohort");
        assertThat(updated.getCode()).isEqualTo("TG-INT-01"); // Code immutable

        // Deactivate
        TargetGroup deactivated = targetGroupService.deactivateTargetGroup(created.getId());
        assertThat(deactivated.isActive()).isFalse();

        // Check active list excludes it
        List<TargetGroup> activeList = targetGroupService.getActiveTargetGroups();
        assertThat(activeList.stream().noneMatch(t -> t.getCode().equals("TG-INT-01"))).isTrue();

        // Check all list includes it
        List<TargetGroup> allList = targetGroupService.getTargetGroups(true);
        assertThat(allList.stream().anyMatch(t -> t.getCode().equals("TG-INT-01"))).isTrue();

        // Reactivate
        TargetGroup reactivated = targetGroupService.activateTargetGroup(created.getId());
        assertThat(reactivated.isActive()).isTrue();

        activeList = targetGroupService.getActiveTargetGroups();
        assertThat(activeList.stream().anyMatch(t -> t.getCode().equals("TG-INT-01"))).isTrue();
    }

    @Test
    @DisplayName("Cannot create duplicate business codes for VaccineType or TargetGroup")
    void duplicateCodeRejection() {
        vaccineTypeService.createVaccineType("VAX-DUP-01", "Duplicate Test 1", "dose");
        assertThatThrownBy(() -> vaccineTypeService.createVaccineType("VAX-DUP-01", "Duplicate Test 2", "dose"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Vaccine type already exists with code: VAX-DUP-01");

        targetGroupService.createTargetGroup("TG-DUP-01", "Duplicate Group 1");
        assertThatThrownBy(() -> targetGroupService.createTargetGroup("TG-DUP-01", "Duplicate Group 2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Target group already exists with code: TG-DUP-01");
    }

    @Test
    @DisplayName("Campaign creation rejects inactive VaccineType or inactive TargetGroup")
    void campaignCreationRejectsInactiveMasterData() {
        VaccineType vt = vaccineTypeService.createVaccineType("VAX-INACT-CAMP", "Inactive Vax", "dose");
        vaccineTypeService.deactivateVaccineType(vt.getId());
        VaccineType inactiveVt = vaccineTypeRepository.findById(vt.getId()).orElseThrow();

        TargetGroup tg = targetGroupService.createTargetGroup("TG-ACT-CAMP", "Active TG");

        assertThatThrownBy(() -> campaignService.createCampaign(
                "CAMP-INACT-VT",
                "Inactive VT Campaign",
                "Desc",
                inactiveVt,
                tg,
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(20),
                Instant.now().plusSeconds(3600),
                Instant.now().plusSeconds(7200)
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot create campaign with inactive VaccineType");

        // Reactivate VT and deactivate TG
        vaccineTypeService.activateVaccineType(vt.getId());
        VaccineType activeVt = vaccineTypeRepository.findById(vt.getId()).orElseThrow();

        targetGroupService.deactivateTargetGroup(tg.getId());
        TargetGroup inactiveTg = targetGroupRepository.findById(tg.getId()).orElseThrow();

        assertThatThrownBy(() -> campaignService.createCampaign(
                "CAMP-INACT-TG",
                "Inactive TG Campaign",
                "Desc",
                activeVt,
                inactiveTg,
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(20),
                Instant.now().plusSeconds(3600),
                Instant.now().plusSeconds(7200)
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot create campaign with inactive TargetGroup");
    }

    @Test
    @DisplayName("Existing campaign preserves historical reference when master data is deactivated")
    void existingCampaignPreservesHistoricalReference() {
        VaccineType vt = vaccineTypeService.createVaccineType("VAX-HIST-01", "Historical Vax", "dose");
        TargetGroup tg = targetGroupService.createTargetGroup("TG-HIST-01", "Historical TG");

        VaccinationCampaign campaign = campaignService.createCampaign(
                "CAMP-HIST-01",
                "Historical Campaign",
                "Desc",
                vt,
                tg,
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(20),
                Instant.now().plusSeconds(3600),
                Instant.now().plusSeconds(7200)
        );

        // Deactivate master data
        vaccineTypeService.deactivateVaccineType(vt.getId());
        targetGroupService.deactivateTargetGroup(tg.getId());

        // Verify campaign still references the entities and can be loaded
        VaccinationCampaign loaded = campaignRepository.findById(campaign.getId()).orElseThrow();
        assertThat(loaded.getVaccineType().getCode()).isEqualTo("VAX-HIST-01");
        assertThat(loaded.getVaccineType().isActive()).isFalse();
        assertThat(loaded.getTargetGroup().getCode()).isEqualTo("TG-HIST-01");
        assertThat(loaded.getTargetGroup().isActive()).isFalse();
    }
}
