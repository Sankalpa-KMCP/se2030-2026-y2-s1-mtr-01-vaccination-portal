package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CampaignLifecycleEvent;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.CampaignLifecycleEventRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.service.VaccinationCampaignService;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class CampaignSuspensionAtomicityIntegrationTests {

    @Autowired private VaccinationCampaignService campaignService;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;

    @MockitoBean
    private CampaignLifecycleEventRepository eventRepository;

    @Test
    void eventPersistenceFailureRollsBackCampaignStatus() {
        String suffix = Long.toUnsignedString(System.nanoTime());
        VaccineType vaccine = vaccineTypeRepository.saveAndFlush(
                new VaccineType("VAX-ATOMIC-" + suffix, "Atomic Suspension Vaccine"));
        TargetGroup group = targetGroupRepository.saveAndFlush(
                new TargetGroup("TG-ATOMIC-" + suffix, "Atomic Suspension Group"));
        VaccinationCampaign campaign = campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                "CMP-ATOMIC-" + suffix, "Atomic Suspension Campaign", null,
                vaccine, group,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31),
                Instant.parse("2026-08-01T00:00:00Z"), Instant.parse("2026-12-30T00:00:00Z"),
                CampaignStatus.PUBLISHED
        ));
        Long campaignId = campaign.getId();
        when(eventRepository.save(any(CampaignLifecycleEvent.class)))
                .thenThrow(new IllegalStateException("Simulated lifecycle event persistence failure"));

        assertThrows(IllegalStateException.class, () ->
                campaignService.suspendCampaign(campaignId, "Valid reason", "admin-atomic"));

        assertEquals(CampaignStatus.PUBLISHED,
                campaignRepository.findById(campaignId).orElseThrow().getStatus());
    }
}
