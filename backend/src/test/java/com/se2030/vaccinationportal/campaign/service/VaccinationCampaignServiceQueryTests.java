package com.se2030.vaccinationportal.campaign.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.model.CampaignLifecycleChangeType;
import com.se2030.vaccinationportal.campaign.model.CampaignLifecycleEvent;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.CampaignLifecycleEventRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VaccinationCampaignServiceQueryTests {

    @Autowired
    private VaccinationCampaignService campaignService;

    @Autowired
    private VaccinationCampaignRepository campaignRepository;

    @Autowired
    private VaccineTypeRepository vaccineTypeRepository;

    @Autowired
    private TargetGroupRepository targetGroupRepository;

    @Autowired
    private CampaignLifecycleEventRepository lifecycleEventRepository;

    private VaccineType vaccineType;
    private TargetGroup targetGroup;
    private VaccinationCampaign campaign;

    @BeforeEach
    void setUp() {
        vaccineType = vaccineTypeRepository.saveAndFlush(
                new VaccineType("VT-QUERY-TEST", "Query Test Vaccine", "dose", true)
        );
        targetGroup = targetGroupRepository.saveAndFlush(
                new TargetGroup("TG-QUERY-TEST", "Query Test Target Group", true)
        );
        campaign = campaignRepository.saveAndFlush(
                new VaccinationCampaign(
                        "CMP-QUERY-01",
                        "Query Campaign",
                        "Description",
                        vaccineType,
                        targetGroup,
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 12, 31),
                        Instant.parse("2026-08-15T08:00:00Z"),
                        Instant.parse("2026-12-30T18:00:00Z")
                )
        );
    }

    @Test
    @DisplayName("ID-based createCampaign resolves master data entities and creates in DRAFT")
    void createCampaignWithIdsSuccess() {
        VaccinationCampaign created = campaignService.createCampaign(
                "CMP-ID-CREATE",
                "Created via ID Overload",
                "Testing ID resolution",
                vaccineType.getId(),
                targetGroup.getId(),
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 11, 30),
                Instant.parse("2026-09-01T08:00:00Z"),
                Instant.parse("2026-11-29T18:00:00Z")
        );

        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals("CMP-ID-CREATE", created.getCode());
        assertEquals(CampaignStatus.DRAFT, created.getStatus());
        assertEquals(vaccineType.getId(), created.getVaccineType().getId());
        assertEquals(targetGroup.getId(), created.getTargetGroup().getId());
    }

    @Test
    @DisplayName("ID-based createCampaign throws ResourceNotFoundException when VaccineType does not exist")
    void createCampaignMissingVaccineTypeThrows() {
        assertThrows(ResourceNotFoundException.class, () ->
                campaignService.createCampaign(
                        "CMP-MISSING-VT",
                        "Missing VT Campaign",
                        null,
                        999999L,
                        targetGroup.getId(),
                        LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 11, 30),
                        Instant.parse("2026-09-01T08:00:00Z"),
                        Instant.parse("2026-11-29T18:00:00Z")
                )
        );
    }

    @Test
    @DisplayName("ID-based createCampaign throws ResourceNotFoundException when TargetGroup does not exist")
    void createCampaignMissingTargetGroupThrows() {
        assertThrows(ResourceNotFoundException.class, () ->
                campaignService.createCampaign(
                        "CMP-MISSING-TG",
                        "Missing TG Campaign",
                        null,
                        vaccineType.getId(),
                        999999L,
                        LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 11, 30),
                        Instant.parse("2026-09-01T08:00:00Z"),
                        Instant.parse("2026-11-29T18:00:00Z")
                )
        );
    }

    @Test
    @DisplayName("getRequiredCampaignById returns campaign when present, throws ResourceNotFoundException when absent")
    void getRequiredCampaignByIdSemantics() {
        VaccinationCampaign found = campaignService.getRequiredCampaignById(campaign.getId());
        assertNotNull(found);
        assertEquals(campaign.getId(), found.getId());

        assertThrows(ResourceNotFoundException.class, () ->
                campaignService.getRequiredCampaignById(999999L)
        );
    }

    @Test
    @DisplayName("getCampaigns returns all campaigns or filters by status")
    void getCampaignsListAndFilter() {
        List<VaccinationCampaign> all = campaignService.getCampaigns(null);
        assertTrue(all.stream().anyMatch(c -> c.getId().equals(campaign.getId())));

        List<VaccinationCampaign> drafts = campaignService.getCampaigns(CampaignStatus.DRAFT);
        assertTrue(drafts.stream().anyMatch(c -> c.getId().equals(campaign.getId())));

        List<VaccinationCampaign> active = campaignService.getCampaigns(CampaignStatus.ACTIVE);
        assertTrue(active.stream().noneMatch(c -> c.getId().equals(campaign.getId())));
    }

    @Test
    @DisplayName("getCampaignLifecycleEvents returns chronological audit events for existing campaign")
    void getCampaignLifecycleEventsSuccess() {
        CampaignLifecycleEvent event = lifecycleEventRepository.saveAndFlush(
                new CampaignLifecycleEvent(
                        campaign,
                        CampaignLifecycleChangeType.SUSPENDED,
                        CampaignStatus.PUBLISHED,
                        CampaignStatus.SUSPENDED,
                        "Operational hold",
                        "ACTOR-TEST",
                        Instant.now()
                )
        );

        List<CampaignLifecycleEvent> events = campaignService.getCampaignLifecycleEvents(campaign.getId());
        assertEquals(1, events.size());
        assertEquals(event.getId(), events.get(0).getId());
        assertEquals("Operational hold", events.get(0).getReason());
    }

    @Test
    @DisplayName("getCampaignLifecycleEvents throws ResourceNotFoundException when campaign does not exist")
    void getCampaignLifecycleEventsMissingCampaign() {
        assertThrows(ResourceNotFoundException.class, () ->
                campaignService.getCampaignLifecycleEvents(999999L)
        );
    }
}
