package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CampaignCenterAssignment;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.OperatingHours;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.CampaignCenterAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.CenterClosureRepository;
import com.se2030.vaccinationportal.campaign.repository.OperatingHoursRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.service.CenterClosureService;
import com.se2030.vaccinationportal.campaign.service.OperatingHoursService;
import com.se2030.vaccinationportal.campaign.service.VaccinationCenterService;
import com.se2030.vaccinationportal.campaign.service.VaccinationSessionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Concurrency integration tests verifying parent VaccinationCenter pessimistic write locking serialization
 * between center configuration mutations (deactivation, operating hours, center closure) and session operations.
 */
@SpringBootTest
@ActiveProfiles("test")
class CenterHoursClosureReverseImpactConcurrencyIntegrationTests {

    @Autowired private VaccinationCenterService centerService;
    @Autowired private OperatingHoursService hoursService;
    @Autowired private CenterClosureService closureService;
    @Autowired private VaccinationSessionService sessionService;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private CampaignCenterAssignmentRepository assignmentRepository;
    @Autowired private OperatingHoursRepository operatingHoursRepository;
    @Autowired private VaccinationSessionRepository sessionRepository;
    @Autowired private CenterClosureRepository closureRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private Clock clock;

    private TransactionTemplate transactionTemplate;
    private Long campaignId;
    private Long centerId;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(status -> {
            sessionRepository.deleteAll();
            closureRepository.deleteAll();
            operatingHoursRepository.deleteAll();
            assignmentRepository.deleteAll();
            campaignRepository.deleteAll();
            centerRepository.deleteAll();

            VaccineType vaccineType = vaccineTypeRepository.findByCode("VAX-REV-CONC")
                    .orElseGet(() -> vaccineTypeRepository.saveAndFlush(new VaccineType("VAX-REV-CONC", "Conc Vaccine", "dose", true)));
            TargetGroup targetGroup = targetGroupRepository.findByCode("TG-REV-CONC")
                    .orElseGet(() -> targetGroupRepository.saveAndFlush(new TargetGroup("TG-REV-CONC", "Conc Target", true)));

            VaccinationCenter center = centerRepository.findByCode("CTR-CONC-1")
                    .orElseGet(() -> centerRepository.saveAndFlush(new VaccinationCenter(
                            "CTR-CONC-1", "Concurrency Center", "123 Main St", "555-0199",
                            CenterStatus.ACTIVE, 100, 50, "Asia/Colombo"
                    )));
            centerId = center.getId();

            LocalDate today = LocalDate.now(clock);
            VaccinationCampaign campaign = campaignRepository.findByCode("CMP-CONC-1")
                    .orElseGet(() -> campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                            "CMP-CONC-1", "Concurrency Campaign", "Desc", vaccineType, targetGroup,
                            today.minusDays(10), today.plusDays(30),
                            Instant.now(clock).minusSeconds(3600), today.plusDays(30).atTime(23, 59, 59).toInstant(ZoneOffset.UTC),
                            CampaignStatus.ACTIVE
                    )));
            campaignId = campaign.getId();

            assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                    campaign, center, today.minusDays(10), today.plusDays(30)
            ));

            for (DayOfWeek day : DayOfWeek.values()) {
                operatingHoursRepository.saveAndFlush(new OperatingHours(
                        center, day, LocalTime.of(8, 0), LocalTime.of(17, 0),
                        today.minusDays(10), today.plusDays(30)
                ));
            }
        });
    }

    @AfterEach
    void tearDown() {
        transactionTemplate.executeWithoutResult(status -> {
            sessionRepository.deleteAll();
            closureRepository.deleteAll();
            operatingHoursRepository.deleteAll();
            assignmentRepository.deleteAll();
            campaignRepository.deleteAll();
            centerRepository.deleteAll();
        });
    }

    @Test
    @DisplayName("Concurrent center deactivation and session creation are serialized by center-row pessimistic locking")
    void concurrentDeactivationAndSessionCreation_areSerialized() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicBoolean deactivationSucceeded = new AtomicBoolean(false);
        AtomicBoolean sessionCreationSucceeded = new AtomicBoolean(false);

        LocalDate today = LocalDate.now(clock);
        LocalDate monday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        ZoneId zoneId = ZoneId.of("Asia/Colombo");
        Instant startAt = monday.atTime(9, 0).atZone(zoneId).toInstant();
        Instant endAt = monday.atTime(12, 0).atZone(zoneId).toInstant();
        assertTrue(startAt.isAfter(clock.instant()), "Deactivation concurrency test requires a future session fixture");

        Future<?> f1 = executor.submit(() -> {
            try {
                startLatch.await();
                transactionTemplate.executeWithoutResult(status -> {
                    centerService.deactivateCenter(centerId);
                    deactivationSucceeded.set(true);
                });
            } catch (Exception ignored) {
            }
        });

        Future<?> f2 = executor.submit(() -> {
            try {
                startLatch.await();
                transactionTemplate.executeWithoutResult(status -> {
                    sessionService.createOneOffDraftSession(campaignId, centerId, startAt, endAt, startAt);
                    sessionCreationSucceeded.set(true);
                });
            } catch (Exception ignored) {
            }
        });

        startLatch.countDown();
        f1.get(10, TimeUnit.SECONDS);
        f2.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertFalse(
                deactivationSucceeded.get() && sessionCreationSucceeded.get(),
                "Center deactivation and session creation on the same center cannot both succeed concurrently"
        );
        assertTrue(
                deactivationSucceeded.get() || sessionCreationSucceeded.get(),
                "At least one concurrent operation must succeed"
        );
    }

    @Test
    @DisplayName("Concurrent center closure creation and session creation are serialized by center-row pessimistic locking")
    void concurrentClosureCreationAndSessionCreation_areSerialized() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicBoolean closureSucceeded = new AtomicBoolean(false);
        AtomicBoolean sessionCreationSucceeded = new AtomicBoolean(false);

        LocalDate today = LocalDate.now(clock);
        LocalDate tuesday = today.with(TemporalAdjusters.next(DayOfWeek.TUESDAY));
        ZoneId zoneId = ZoneId.of("Asia/Colombo");
        Instant startAt = tuesday.atTime(9, 0).atZone(zoneId).toInstant();
        Instant endAt = tuesday.atTime(12, 0).atZone(zoneId).toInstant();
        assertTrue(startAt.isAfter(clock.instant()), "Closure concurrency test requires a future session fixture");

        Future<?> f1 = executor.submit(() -> {
            try {
                startLatch.await();
                transactionTemplate.executeWithoutResult(status -> {
                    closureService.createClosure(centerId, startAt, endAt, "Emergency Maintenance");
                    closureSucceeded.set(true);
                });
            } catch (Exception ignored) {
            }
        });

        Future<?> f2 = executor.submit(() -> {
            try {
                startLatch.await();
                transactionTemplate.executeWithoutResult(status -> {
                    sessionService.createOneOffDraftSession(campaignId, centerId, startAt, endAt, startAt);
                    sessionCreationSucceeded.set(true);
                });
            } catch (Exception ignored) {
            }
        });

        startLatch.countDown();
        f1.get(10, TimeUnit.SECONDS);
        f2.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertFalse(
                closureSucceeded.get() && sessionCreationSucceeded.get(),
                "Center closure and session creation overlapping on the same center cannot both succeed concurrently"
        );
        assertTrue(
                closureSucceeded.get() || sessionCreationSucceeded.get(),
                "At least one operation must succeed"
        );
    }
}
