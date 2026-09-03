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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests verifying the authoritative SUSPENDED -> PUBLISHED / ACTIVE campaign resumption lifecycle transition.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(CampaignResumptionIntegrationTests.DynamicClockTestConfig.class)
class CampaignResumptionIntegrationTests {

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

        void setInstant(Instant instant) {
            this.instant = instant;
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
    @Autowired private CampaignLifecycleEventRepository campaignLifecycleEventRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;

    @Test
    @DisplayName("Suspended campaign resumes to PUBLISHED before service start date (today < serviceStartDate)")
    void suspendedCampaignResumesToPublishedBeforeStartDate() {
        LocalDate startDate = LocalDate.of(2026, 9, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 31);
        mutableClock.setDate(startDate.minusDays(5));

        VaccinationCampaign campaign = persistCampaign(CampaignStatus.SUSPENDED, startDate, endDate, "RES-BEFORE-START");

        VaccinationCampaign resumed = campaignService.resumeCampaign(campaign.getId(), "Supply issue resolved", "officer-01");

        assertEquals(CampaignStatus.PUBLISHED, resumed.getStatus());
        VaccinationCampaign persisted = campaignRepository.findById(campaign.getId()).orElseThrow();
        assertEquals(CampaignStatus.PUBLISHED, persisted.getStatus());

        List<CampaignLifecycleEvent> events = campaignLifecycleEventRepository.findByCampaignIdOrderByCreatedAtAscIdAsc(campaign.getId());
        assertEquals(1, events.size());
        CampaignLifecycleEvent event = events.get(0);
        assertEquals(CampaignLifecycleChangeType.RESUMED, event.getChangeType());
        assertEquals(CampaignStatus.SUSPENDED, event.getPreviousStatus());
        assertEquals(CampaignStatus.PUBLISHED, event.getNewStatus());
        assertEquals("Supply issue resolved", event.getReason());
        assertEquals("officer-01", event.getActorId());
        assertEquals(mutableClock.instant(), event.getCreatedAt());
    }

    @Test
    @DisplayName("Suspended campaign resumes to ACTIVE exactly on service start date (today == serviceStartDate)")
    void suspendedCampaignResumesToActiveOnStartDateBoundary() {
        LocalDate startDate = LocalDate.of(2026, 9, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 31);
        mutableClock.setDate(startDate);

        VaccinationCampaign campaign = persistCampaign(CampaignStatus.SUSPENDED, startDate, endDate, "RES-ON-START");

        VaccinationCampaign resumed = campaignService.resumeCampaign(campaign.getId(), "Operational hold lifted", "officer-02");

        assertEquals(CampaignStatus.ACTIVE, resumed.getStatus());
        VaccinationCampaign persisted = campaignRepository.findById(campaign.getId()).orElseThrow();
        assertEquals(CampaignStatus.ACTIVE, persisted.getStatus());

        List<CampaignLifecycleEvent> events = campaignLifecycleEventRepository.findByCampaignIdOrderByCreatedAtAscIdAsc(campaign.getId());
        assertEquals(1, events.size());
        CampaignLifecycleEvent event = events.get(0);
        assertEquals(CampaignLifecycleChangeType.RESUMED, event.getChangeType());
        assertEquals(CampaignStatus.SUSPENDED, event.getPreviousStatus());
        assertEquals(CampaignStatus.ACTIVE, event.getNewStatus());
        assertEquals("Operational hold lifted", event.getReason());
        assertEquals("officer-02", event.getActorId());
    }

    @Test
    @DisplayName("Suspended campaign resumes to ACTIVE in the middle of its service period (serviceStartDate < today < serviceEndDate)")
    void suspendedCampaignResumesToActiveDuringServicePeriod() {
        LocalDate startDate = LocalDate.of(2026, 9, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 31);
        mutableClock.setDate(LocalDate.of(2026, 10, 15));

        VaccinationCampaign campaign = persistCampaign(CampaignStatus.SUSPENDED, startDate, endDate, "RES-MID-PERIOD");

        VaccinationCampaign resumed = campaignService.resumeCampaign(campaign.getId(), "Staffing shortage addressed", "officer-03");

        assertEquals(CampaignStatus.ACTIVE, resumed.getStatus());
        VaccinationCampaign persisted = campaignRepository.findById(campaign.getId()).orElseThrow();
        assertEquals(CampaignStatus.ACTIVE, persisted.getStatus());

        List<CampaignLifecycleEvent> events = campaignLifecycleEventRepository.findByCampaignIdOrderByCreatedAtAscIdAsc(campaign.getId());
        assertEquals(1, events.size());
        CampaignLifecycleEvent event = events.get(0);
        assertEquals(CampaignLifecycleChangeType.RESUMED, event.getChangeType());
        assertEquals(CampaignStatus.SUSPENDED, event.getPreviousStatus());
        assertEquals(CampaignStatus.ACTIVE, event.getNewStatus());
        assertEquals("Staffing shortage addressed", event.getReason());
        assertEquals("officer-03", event.getActorId());
    }

    @Test
    @DisplayName("Suspended campaign resumes to ACTIVE exactly on service end date (today == serviceEndDate)")
    void suspendedCampaignResumesToActiveOnEndDateBoundary() {
        LocalDate startDate = LocalDate.of(2026, 9, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 31);
        mutableClock.setDate(endDate);

        VaccinationCampaign campaign = persistCampaign(CampaignStatus.SUSPENDED, startDate, endDate, "RES-ON-END");

        VaccinationCampaign resumed = campaignService.resumeCampaign(campaign.getId(), "Final day operations resumed", "officer-04");

        assertEquals(CampaignStatus.ACTIVE, resumed.getStatus());
        VaccinationCampaign persisted = campaignRepository.findById(campaign.getId()).orElseThrow();
        assertEquals(CampaignStatus.ACTIVE, persisted.getStatus());

        List<CampaignLifecycleEvent> events = campaignLifecycleEventRepository.findByCampaignIdOrderByCreatedAtAscIdAsc(campaign.getId());
        assertEquals(1, events.size());
        CampaignLifecycleEvent event = events.get(0);
        assertEquals(CampaignLifecycleChangeType.RESUMED, event.getChangeType());
        assertEquals(CampaignStatus.SUSPENDED, event.getPreviousStatus());
        assertEquals(CampaignStatus.ACTIVE, event.getNewStatus());
    }

    @Test
    @DisplayName("Resumption after service end date fails and preserves SUSPENDED status with 0 events persisted")
    void resumptionAfterEndDateFailsAndPreservesStatus() {
        LocalDate startDate = LocalDate.of(2026, 9, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 31);
        mutableClock.setDate(endDate.plusDays(1));

        VaccinationCampaign campaign = persistCampaign(CampaignStatus.SUSPENDED, startDate, endDate, "RES-AFTER-END");

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> campaignService.resumeCampaign(campaign.getId(), "Late resumption", "officer-05")
        );

        assertTrue(ex.getMessage().contains("has already passed"));
        VaccinationCampaign persisted = campaignRepository.findById(campaign.getId()).orElseThrow();
        assertEquals(CampaignStatus.SUSPENDED, persisted.getStatus());

        List<CampaignLifecycleEvent> events = campaignLifecycleEventRepository.findByCampaignIdOrderByCreatedAtAscIdAsc(campaign.getId());
        assertTrue(events.isEmpty());
    }

    @ParameterizedTest(name = "Resumption fails for non-SUSPENDED status {0}")
    @EnumSource(value = CampaignStatus.class, names = {"DRAFT", "SCHEDULED", "PUBLISHED", "ACTIVE", "COMPLETED", "CANCELLED", "ARCHIVED"})
    @DisplayName("Resumption fails for all non-SUSPENDED statuses and preserves original status with 0 events persisted")
    void resumptionFailsForNonSuspendedStatuses(CampaignStatus nonSuspendedStatus) {
        LocalDate startDate = LocalDate.of(2026, 9, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 31);
        mutableClock.setDate(LocalDate.of(2026, 10, 1));

        VaccinationCampaign campaign = persistCampaign(nonSuspendedStatus, startDate, endDate, "RES-NON-SUS-" + nonSuspendedStatus.name());

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> campaignService.resumeCampaign(campaign.getId(), "Resume attempt", "officer-06")
        );

        assertTrue(ex.getMessage().contains("Cannot resume campaign in " + nonSuspendedStatus + " status"));
        assertTrue(ex.getMessage().contains("Only SUSPENDED campaigns can be resumed"));

        VaccinationCampaign persisted = campaignRepository.findById(campaign.getId()).orElseThrow();
        assertEquals(nonSuspendedStatus, persisted.getStatus());

        List<CampaignLifecycleEvent> events = campaignLifecycleEventRepository.findByCampaignIdOrderByCreatedAtAscIdAsc(campaign.getId());
        assertTrue(events.isEmpty());
    }

    @Test
    @DisplayName("Null campaign ID throws IllegalArgumentException")
    void nullCampaignIdThrowsException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> campaignService.resumeCampaign(null, "Valid reason", "actor-1")
        );
        assertTrue(ex.getMessage().contains("Campaign ID is required"));
    }

    @Test
    @DisplayName("Non-existent campaign ID throws IllegalArgumentException")
    void nonExistentCampaignIdThrowsException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> campaignService.resumeCampaign(999999L, "Valid reason", "actor-1")
        );
        assertTrue(ex.getMessage().contains("Campaign not found with ID: 999999"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    @DisplayName("Blank or null reason throws IllegalArgumentException and preserves status")
    void blankReasonThrowsException(String blankReason) {
        LocalDate startDate = LocalDate.of(2026, 9, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 31);
        mutableClock.setDate(LocalDate.of(2026, 10, 1));

        VaccinationCampaign campaign = persistCampaign(CampaignStatus.SUSPENDED, startDate, endDate, "RES-BLANK-REASON");

        assertThrows(
                IllegalArgumentException.class,
                () -> campaignService.resumeCampaign(campaign.getId(), blankReason, "actor-1")
        );

        VaccinationCampaign persisted = campaignRepository.findById(campaign.getId()).orElseThrow();
        assertEquals(CampaignStatus.SUSPENDED, persisted.getStatus());
        assertTrue(campaignLifecycleEventRepository.findByCampaignIdOrderByCreatedAtAscIdAsc(campaign.getId()).isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    @DisplayName("Blank or null actorId throws IllegalArgumentException and preserves status")
    void blankActorIdThrowsException(String blankActorId) {
        LocalDate startDate = LocalDate.of(2026, 9, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 31);
        mutableClock.setDate(LocalDate.of(2026, 10, 1));

        VaccinationCampaign campaign = persistCampaign(CampaignStatus.SUSPENDED, startDate, endDate, "RES-BLANK-ACTOR");

        assertThrows(
                IllegalArgumentException.class,
                () -> campaignService.resumeCampaign(campaign.getId(), "Valid reason", blankActorId)
        );

        VaccinationCampaign persisted = campaignRepository.findById(campaign.getId()).orElseThrow();
        assertEquals(CampaignStatus.SUSPENDED, persisted.getStatus());
        assertTrue(campaignLifecycleEventRepository.findByCampaignIdOrderByCreatedAtAscIdAsc(campaign.getId()).isEmpty());
    }

    private VaccinationCampaign persistCampaign(CampaignStatus status, LocalDate startDate, LocalDate endDate, String suffix) {
        VaccineType vaccineType = vaccineTypeRepository.save(
                new VaccineType("VAX-RES-" + suffix, "Vaccine " + suffix, "dose", true)
        );
        TargetGroup targetGroup = targetGroupRepository.save(
                new TargetGroup("TG-RES-" + suffix, "Target Group " + suffix, true)
        );

        VaccinationCampaign campaign = CampaignTestFixtures.campaign(
                "CMP-RES-" + suffix,
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
