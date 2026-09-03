package com.se2030.vaccinationportal.campaign;

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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CampaignLifecycleEventPersistenceIntegrationTests {

    private static final Instant FIRST_AT = Instant.parse("2026-08-13T09:00:00Z");
    private static final Instant SECOND_AT = Instant.parse("2026-08-13T10:00:00Z");

    @Autowired private CampaignLifecycleEventRepository eventRepository;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @PersistenceContext private EntityManager entityManager;

    private VaccinationCampaign campaign;

    @BeforeEach
    void setUp() {
        String suffix = Long.toUnsignedString(System.nanoTime());
        VaccineType vaccine = vaccineTypeRepository.saveAndFlush(
                new VaccineType("VAX-CLE-P-" + suffix, "Campaign Event Persistence Vaccine"));
        TargetGroup group = targetGroupRepository.saveAndFlush(
                new TargetGroup("TG-CLE-P-" + suffix, "Campaign Event Persistence Group"));
        campaign = campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                "CMP-CLE-P-" + suffix, "Campaign Event Persistence", null,
                vaccine, group,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31),
                Instant.parse("2026-08-01T00:00:00Z"), Instant.parse("2026-12-30T00:00:00Z"),
                CampaignStatus.SUSPENDED
        ));
    }

    @Test
    void eventPersistsAndReloadsAllRequiredFields() {
        CampaignLifecycleEvent saved = eventRepository.saveAndFlush(event(
                CampaignStatus.PUBLISHED, "Public-health hold", "admin-21", FIRST_AT));
        Long eventId = saved.getId();
        assertNotNull(eventId);

        entityManager.clear();

        CampaignLifecycleEvent reloaded = eventRepository.findById(eventId).orElseThrow();
        assertAll(
                () -> assertEquals(campaign.getId(), reloaded.getCampaign().getId()),
                () -> assertEquals(CampaignLifecycleChangeType.SUSPENDED, reloaded.getChangeType()),
                () -> assertEquals(CampaignStatus.PUBLISHED, reloaded.getPreviousStatus()),
                () -> assertEquals(CampaignStatus.SUSPENDED, reloaded.getNewStatus()),
                () -> assertEquals("Public-health hold", reloaded.getReason()),
                () -> assertEquals("admin-21", reloaded.getActorId()),
                () -> assertEquals(FIRST_AT, reloaded.getCreatedAt())
        );
    }

    @Test
    void campaignHistoryIsOrderedByTimestampThenId() {
        CampaignLifecycleEvent later = eventRepository.saveAndFlush(event(
                CampaignStatus.ACTIVE, "Later event", "admin-later", SECOND_AT));
        CampaignLifecycleEvent earlierFirst = eventRepository.saveAndFlush(event(
                CampaignStatus.PUBLISHED, "Earlier first", "admin-first", FIRST_AT));
        CampaignLifecycleEvent earlierSecond = eventRepository.saveAndFlush(event(
                CampaignStatus.ACTIVE, "Earlier second", "admin-second", FIRST_AT));

        entityManager.clear();

        List<CampaignLifecycleEvent> history =
                eventRepository.findByCampaignIdOrderByCreatedAtAscIdAsc(campaign.getId());

        assertEquals(List.of(earlierFirst.getId(), earlierSecond.getId(), later.getId()),
                history.stream().map(CampaignLifecycleEvent::getId).toList());
    }

    private CampaignLifecycleEvent event(
            CampaignStatus previousStatus,
            String reason,
            String actorId,
            Instant createdAt
    ) {
        return new CampaignLifecycleEvent(
                campaign,
                CampaignLifecycleChangeType.SUSPENDED,
                previousStatus,
                CampaignStatus.SUSPENDED,
                reason,
                actorId,
                createdAt
        );
    }
}
