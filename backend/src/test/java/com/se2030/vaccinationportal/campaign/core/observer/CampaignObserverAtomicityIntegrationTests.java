package com.se2030.vaccinationportal.campaign.core.observer;

import com.se2030.vaccinationportal.campaign.CampaignTestFixtures;
import com.se2030.vaccinationportal.campaign.core.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.core.model.CampaignLifecycleChangeType;
import com.se2030.vaccinationportal.campaign.core.repository.CampaignLifecycleEventRepository;
import com.se2030.vaccinationportal.campaign.core.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.core.service.VaccinationCampaignService;
import com.se2030.vaccinationportal.campaign.reference.model.VaccineType;
import com.se2030.vaccinationportal.campaign.reference.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.reference.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.reference.repository.TargetGroupRepository;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class CampaignObserverAtomicityIntegrationTests {
    @Autowired private VaccinationCampaignService service;
    @Autowired private CampaignLifecycleSubject subject;
    @Autowired private VaccinationCampaignRepository campaigns;
    @Autowired private CampaignLifecycleEventRepository events;
    @Autowired private VaccineTypeRepository vaccines;
    @Autowired private TargetGroupRepository groups;

    // No test transaction: assertions read persisted state after the service transaction ends.
    @Test
    void suspensionCommitsExactlyOneAuditEntryThroughTheObserver() {
        Long id = createCampaign();
        service.suspendCampaign(id, "Observer demonstration", "admin-observer");
        assertEquals(CampaignStatus.SUSPENDED, campaigns.findById(id).orElseThrow().getStatus());
        var history = events.findByCampaignIdOrderByCreatedAtAscIdAsc(id);
        assertEquals(1, history.size());
        assertEquals(CampaignLifecycleChangeType.SUSPENDED, history.getFirst().getChangeType());
    }

    @Test
    void laterObserverFailureRollsBackBothCampaignAndAlreadyInsertedAudit() {
        Long id = createCampaign();
        CampaignLifecycleObserver failingObserver = event -> {
            if (event.getCampaign().getId().equals(id)) {
                // The audit observer runs first; confirm its insertion exists in this transaction.
                assertEquals(1, events.findByCampaignIdOrderByCreatedAtAscIdAsc(id).size());
                throw new IllegalStateException("Simulated observer failure");
            }
        };
        subject.addObserver(failingObserver);
        try {
            assertThrows(IllegalStateException.class,
                    () -> service.suspendCampaign(id, "Rollback demonstration", "admin-observer"));
        } finally {
            subject.removeObserver(failingObserver);
        }
        assertEquals(CampaignStatus.PUBLISHED, campaigns.findById(id).orElseThrow().getStatus());
        assertTrue(events.findByCampaignIdOrderByCreatedAtAscIdAsc(id).isEmpty());
    }

    private Long createCampaign() {
        String suffix = Long.toUnsignedString(System.nanoTime());
        var vaccine = vaccines.saveAndFlush(new VaccineType("VAX-OBS-" + suffix, "Observer Vaccine"));
        var group = groups.saveAndFlush(new TargetGroup("TG-OBS-" + suffix, "Observer Group"));
        return campaigns.saveAndFlush(CampaignTestFixtures.campaign(
                "CMP-OBS-" + suffix, "Observer Campaign", null, vaccine, group,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31),
                Instant.parse("2026-08-01T00:00:00Z"), Instant.parse("2026-12-30T00:00:00Z"),
                CampaignStatus.PUBLISHED)).getId();
    }
}
