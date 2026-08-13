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
import com.se2030.vaccinationportal.campaign.service.VaccinationCampaignService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(CampaignSuspensionIntegrationTests.FixedClockConfig.class)
class CampaignSuspensionIntegrationTests {

    private static final Instant NOW = Instant.parse("2026-08-13T11:45:00Z");

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @Autowired private VaccinationCampaignService campaignService;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private CampaignLifecycleEventRepository eventRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;

    @Test
    void publishedCampaignSuspensionPersistsExactEventAndBlocksIntake() {
        VaccinationCampaign campaign = persistCampaign(CampaignStatus.PUBLISHED, "PUBLISHED");

        VaccinationCampaign suspended = campaignService.suspendCampaign(
                campaign.getId(), "  Emergency cold-chain review  ", "  officer-42  ");

        List<CampaignLifecycleEvent> events =
                eventRepository.findByCampaignIdOrderByCreatedAtAscIdAsc(campaign.getId());
        assertEquals(1, events.size());
        CampaignLifecycleEvent event = events.get(0);
        assertAll(
                () -> assertEquals(CampaignStatus.SUSPENDED, suspended.getStatus()),
                () -> assertEquals(CampaignLifecycleChangeType.SUSPENDED, event.getChangeType()),
                () -> assertEquals(CampaignStatus.PUBLISHED, event.getPreviousStatus()),
                () -> assertEquals(CampaignStatus.SUSPENDED, event.getNewStatus()),
                () -> assertEquals("Emergency cold-chain review", event.getReason()),
                () -> assertEquals("officer-42", event.getActorId()),
                () -> assertEquals(NOW, event.getCreatedAt()),
                () -> assertFalse(suspended.isBookingWindowOpenAt(NOW)),
                () -> assertFalse(campaignRepository.findOpenForBookingAt(NOW).stream()
                        .anyMatch(open -> open.getId().equals(campaign.getId())))
        );
    }

    @Test
    void activeCampaignSuspensionPersistsActiveSourceStatus() {
        VaccinationCampaign campaign = persistCampaign(CampaignStatus.ACTIVE, "ACTIVE");

        VaccinationCampaign suspended = campaignService.suspendCampaign(
                campaign.getId(), "Staff safety hold", "administrator-8");

        CampaignLifecycleEvent event = eventRepository
                .findByCampaignIdOrderByCreatedAtAscIdAsc(campaign.getId()).get(0);
        assertAll(
                () -> assertEquals(CampaignStatus.SUSPENDED, suspended.getStatus()),
                () -> assertEquals(CampaignStatus.ACTIVE, event.getPreviousStatus()),
                () -> assertEquals(CampaignStatus.SUSPENDED, event.getNewStatus()),
                () -> assertEquals(NOW, event.getCreatedAt())
        );
    }

    @ParameterizedTest
    @EnumSource(value = CampaignStatus.class, names = {
            "DRAFT", "SCHEDULED", "SUSPENDED", "COMPLETED", "CANCELLED", "ARCHIVED"
    })
    void invalidSourceIsRejectedWithoutMutationOrEvent(CampaignStatus sourceStatus) {
        VaccinationCampaign campaign = persistCampaign(sourceStatus, "REJECT-" + sourceStatus);

        assertThrows(IllegalStateException.class, () ->
                campaignService.suspendCampaign(campaign.getId(), "Valid reason", "valid-actor"));

        assertAll(
                () -> assertEquals(sourceStatus,
                        campaignRepository.findById(campaign.getId()).orElseThrow().getStatus()),
                () -> assertEquals(0,
                        eventRepository.findByCampaignIdOrderByCreatedAtAscIdAsc(campaign.getId()).size())
        );
    }

    @Test
    void unknownCampaignIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                campaignService.suspendCampaign(999_999_999L, "Valid reason", "valid-actor"));
    }

    @Test
    void malformedCommandInputIsRejectedBeforeCampaignMutation() {
        VaccinationCampaign campaign = persistCampaign(CampaignStatus.PUBLISHED, "INVALID-COMMAND");

        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () ->
                        campaignService.suspendCampaign(null, "Valid reason", "valid-actor")),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        campaignService.suspendCampaign(campaign.getId(), null, "valid-actor")),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        campaignService.suspendCampaign(campaign.getId(), "   ", "valid-actor")),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        campaignService.suspendCampaign(campaign.getId(), "R".repeat(501), "valid-actor")),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        campaignService.suspendCampaign(campaign.getId(), "Valid reason", null)),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        campaignService.suspendCampaign(campaign.getId(), "Valid reason", "   ")),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        campaignService.suspendCampaign(campaign.getId(), "Valid reason", "A".repeat(65)))
        );

        assertAll(
                () -> assertEquals(CampaignStatus.PUBLISHED,
                        campaignRepository.findById(campaign.getId()).orElseThrow().getStatus()),
                () -> assertEquals(0,
                        eventRepository.findByCampaignIdOrderByCreatedAtAscIdAsc(campaign.getId()).size())
        );
    }

    private VaccinationCampaign persistCampaign(CampaignStatus status, String label) {
        String suffix = label + "-" + Long.toUnsignedString(System.nanoTime());
        VaccineType vaccine = vaccineTypeRepository.saveAndFlush(
                new VaccineType("VAX-SUSP-" + suffix, "Suspension Vaccine"));
        TargetGroup group = targetGroupRepository.saveAndFlush(
                new TargetGroup("TG-SUSP-" + suffix, "Suspension Target Group"));
        return campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                "CMP-SUSP-" + suffix, "Suspension Campaign", null,
                vaccine, group,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 31),
                Instant.parse("2026-08-01T00:00:00Z"), Instant.parse("2026-12-30T00:00:00Z"),
                status
        ));
    }
}
