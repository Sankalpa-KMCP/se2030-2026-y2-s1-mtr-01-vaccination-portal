package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.service.VaccinationCampaignService;
import org.junit.jupiter.api.DisplayName;
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

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests verifying the authoritative PUBLISHED -> ACTIVE campaign activation lifecycle transition.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(CampaignActivationIntegrationTests.DynamicClockTestConfig.class)
class CampaignActivationIntegrationTests {

    private static final LocalDate BASE_DATE = LocalDate.of(2026, 9, 15);
    private static MutableClock mutableClock = new MutableClock(BASE_DATE.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @TestConfiguration
    static class DynamicClockTestConfig {
        @Bean
        @Primary
        Clock testClock() {
            return mutableClock;
        }
    }

    static class MutableClock extends Clock {
        private Instant instant;
        private final ZoneOffset zone;

        MutableClock(Instant instant, ZoneOffset zone) {
            this.instant = instant;
            this.zone = zone;
        }

        void setDate(LocalDate date) {
            this.instant = date.atStartOfDay().toInstant(ZoneOffset.UTC);
        }

        @Override
        public ZoneOffset getZone() {
            return zone;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return new MutableClock(instant, ZoneOffset.UTC);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    @Autowired private VaccinationCampaignService campaignService;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;

    @Test
    @DisplayName("Published campaign activates successfully exactly on service start date (boundary test)")
    void publishedCampaignActivatesSuccessfullyOnStartDate() {
        LocalDate startDate = LocalDate.of(2026, 9, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 31);
        mutableClock.setDate(startDate);

        VaccinationCampaign campaign = persistCampaign(CampaignStatus.PUBLISHED, startDate, endDate, "ON-START");

        VaccinationCampaign activated = campaignService.activateCampaign(campaign.getId());

        assertEquals(CampaignStatus.ACTIVE, activated.getStatus());
        VaccinationCampaign persisted = campaignRepository.findById(campaign.getId()).orElseThrow();
        assertEquals(CampaignStatus.ACTIVE, persisted.getStatus());
    }

    @Test
    @DisplayName("Published campaign activates successfully in the middle of its service period")
    void publishedCampaignActivatesSuccessfullyDuringServicePeriod() {
        LocalDate startDate = LocalDate.of(2026, 9, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 31);
        mutableClock.setDate(LocalDate.of(2026, 10, 15));

        VaccinationCampaign campaign = persistCampaign(CampaignStatus.PUBLISHED, startDate, endDate, "MID-PERIOD");

        VaccinationCampaign activated = campaignService.activateCampaign(campaign.getId());

        assertEquals(CampaignStatus.ACTIVE, activated.getStatus());
        VaccinationCampaign persisted = campaignRepository.findById(campaign.getId()).orElseThrow();
        assertEquals(CampaignStatus.ACTIVE, persisted.getStatus());
    }

    @Test
    @DisplayName("Published campaign activates successfully exactly on service end date (boundary test)")
    void publishedCampaignActivatesSuccessfullyOnEndDate() {
        LocalDate startDate = LocalDate.of(2026, 9, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 31);
        mutableClock.setDate(endDate);

        VaccinationCampaign campaign = persistCampaign(CampaignStatus.PUBLISHED, startDate, endDate, "ON-END");

        VaccinationCampaign activated = campaignService.activateCampaign(campaign.getId());

        assertEquals(CampaignStatus.ACTIVE, activated.getStatus());
        VaccinationCampaign persisted = campaignRepository.findById(campaign.getId()).orElseThrow();
        assertEquals(CampaignStatus.ACTIVE, persisted.getStatus());
    }

    @Test
    @DisplayName("Activation before service start date fails and preserves PUBLISHED status")
    void activationBeforeStartDateFails() {
        LocalDate startDate = LocalDate.of(2026, 9, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 31);
        mutableClock.setDate(startDate.minusDays(1));

        VaccinationCampaign campaign = persistCampaign(CampaignStatus.PUBLISHED, startDate, endDate, "BEFORE-START");

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> campaignService.activateCampaign(campaign.getId())
        );

        assertTrue(ex.getMessage().contains("has not been reached"));
        VaccinationCampaign persisted = campaignRepository.findById(campaign.getId()).orElseThrow();
        assertEquals(CampaignStatus.PUBLISHED, persisted.getStatus());
    }

    @Test
    @DisplayName("Activation after service end date fails and preserves PUBLISHED status")
    void activationAfterEndDateFails() {
        LocalDate startDate = LocalDate.of(2026, 9, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 31);
        mutableClock.setDate(endDate.plusDays(1));

        VaccinationCampaign campaign = persistCampaign(CampaignStatus.PUBLISHED, startDate, endDate, "AFTER-END");

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> campaignService.activateCampaign(campaign.getId())
        );

        assertTrue(ex.getMessage().contains("has already passed"));
        VaccinationCampaign persisted = campaignRepository.findById(campaign.getId()).orElseThrow();
        assertEquals(CampaignStatus.PUBLISHED, persisted.getStatus());
    }

    @ParameterizedTest(name = "Activation fails for non-PUBLISHED status {0}")
    @EnumSource(value = CampaignStatus.class, names = {"DRAFT", "SCHEDULED", "ACTIVE", "SUSPENDED", "COMPLETED", "CANCELLED", "ARCHIVED"})
    @DisplayName("Activation fails for all non-PUBLISHED statuses and preserves original status")
    void activationFailsForNonPublishedStatuses(CampaignStatus nonPublishedStatus) {
        LocalDate startDate = LocalDate.of(2026, 9, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 31);
        mutableClock.setDate(LocalDate.of(2026, 10, 1));

        VaccinationCampaign campaign = persistCampaign(nonPublishedStatus, startDate, endDate, "NON-PUB-" + nonPublishedStatus.name());

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> campaignService.activateCampaign(campaign.getId())
        );

        assertTrue(ex.getMessage().contains("Cannot activate campaign in " + nonPublishedStatus + " status"));
        assertTrue(ex.getMessage().contains("Only PUBLISHED campaigns can be activated"));

        VaccinationCampaign persisted = campaignRepository.findById(campaign.getId()).orElseThrow();
        assertEquals(nonPublishedStatus, persisted.getStatus());
    }

    @Test
    @DisplayName("Null campaign ID throws IllegalArgumentException")
    void nullCampaignIdThrowsException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> campaignService.activateCampaign(null)
        );
        assertTrue(ex.getMessage().contains("Campaign ID is required"));
    }

    @Test
    @DisplayName("Non-existent campaign ID throws IllegalArgumentException")
    void nonExistentCampaignIdThrowsException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> campaignService.activateCampaign(999999L)
        );
        assertTrue(ex.getMessage().contains("Campaign not found with ID: 999999"));
    }

    private VaccinationCampaign persistCampaign(CampaignStatus status, LocalDate startDate, LocalDate endDate, String suffix) {
        VaccineType vaccineType = vaccineTypeRepository.save(
                new VaccineType("VAX-ACT-" + suffix, "Vaccine " + suffix, "dose", true)
        );
        TargetGroup targetGroup = targetGroupRepository.save(
                new TargetGroup("TG-ACT-" + suffix, "Target Group " + suffix, true)
        );

        VaccinationCampaign campaign = CampaignTestFixtures.campaign(
                "CMP-ACT-" + suffix,
                "Campaign " + suffix,
                "Description " + suffix,
                vaccineType,
                targetGroup,
                startDate,
                endDate,
                Instant.parse("2026-08-01T08:00:00Z"),
                Instant.parse("2026-12-30T18:00:00Z"),
                status
        );
        return campaignRepository.save(campaign);
    }
}
