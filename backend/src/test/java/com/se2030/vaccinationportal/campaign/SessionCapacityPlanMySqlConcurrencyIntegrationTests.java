package com.se2030.vaccinationportal.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.se2030.vaccinationportal.campaign.dto.request.CreateSessionCapacityPlanRequest;
import com.se2030.vaccinationportal.campaign.model.CampaignCenterAssignment;
import com.se2030.vaccinationportal.campaign.model.CampaignDoseProgramme;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.DoseCategory;
import com.se2030.vaccinationportal.campaign.model.OperatingHours;
import com.se2030.vaccinationportal.campaign.model.OperationalResource;
import com.se2030.vaccinationportal.campaign.model.OperationalResourceStatus;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffRoster;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffStatus;
import com.se2030.vaccinationportal.campaign.model.ResourceType;
import com.se2030.vaccinationportal.campaign.model.SessionCapacityPlan;
import com.se2030.vaccinationportal.campaign.model.SessionResourceAssignment;
import com.se2030.vaccinationportal.campaign.model.StaffAssignment;
import com.se2030.vaccinationportal.campaign.model.StaffRole;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.CampaignCenterAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.CampaignDoseProgrammeRepository;
import com.se2030.vaccinationportal.campaign.repository.CampaignLifecycleEventRepository;
import com.se2030.vaccinationportal.campaign.repository.CenterClosureRepository;
import com.se2030.vaccinationportal.campaign.repository.OperatingHoursRepository;
import com.se2030.vaccinationportal.campaign.repository.OperationalResourceRepository;
import com.se2030.vaccinationportal.campaign.repository.OperationalStaffIdentityRepository;
import com.se2030.vaccinationportal.campaign.repository.OperationalStaffRosterRepository;
import com.se2030.vaccinationportal.campaign.repository.SessionCapacityPlanRepository;
import com.se2030.vaccinationportal.campaign.repository.SessionChangeEventRepository;
import com.se2030.vaccinationportal.campaign.repository.SessionRecurrenceRuleRepository;
import com.se2030.vaccinationportal.campaign.repository.SessionResourceAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.StaffAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.TimeSlotRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.service.SessionCapacityPlanService;
import com.se2030.vaccinationportal.testcontainers.AbstractMySqlTestContainersIntegrationTest;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Production-equivalent MySQL Testcontainers concurrency tests verifying deterministic serialization
 * for session capacity plan revision creation against real MySQL/InnoDB.
 */
class SessionCapacityPlanMySqlConcurrencyIntegrationTests extends AbstractMySqlTestContainersIntegrationTest {

    @Autowired private SessionCapacityPlanService capacityPlanService;
    @Autowired private SessionCapacityPlanRepository capacityPlanRepository;
    @Autowired private SessionResourceAssignmentRepository resourceAssignmentRepository;
    @Autowired private StaffAssignmentRepository staffAssignmentRepository;
    @Autowired private SessionChangeEventRepository sessionChangeEventRepository;
    @Autowired private TimeSlotRepository timeSlotRepository;
    @Autowired private OperationalResourceRepository resourceRepository;
    @Autowired private OperationalStaffRosterRepository staffRosterRepository;
    @Autowired private OperationalStaffIdentityRepository identityRepository;
    @Autowired private VaccinationSessionRepository sessionRepository;
    @Autowired private SessionRecurrenceRuleRepository ruleRepository;
    @Autowired private CampaignCenterAssignmentRepository campaignCenterAssignmentRepository;
    @Autowired private CenterClosureRepository closureRepository;
    @Autowired private OperatingHoursRepository operatingHoursRepository;
    @Autowired private CampaignDoseProgrammeRepository doseProgrammeRepository;
    @Autowired private CampaignLifecycleEventRepository lifecycleEventRepository;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;
    private VaccinationCenter center;
    private VaccinationCampaign campaign;
    private VaccinationSession session;

    private final Instant sessionStart = Instant.parse("2026-10-01T08:00:00Z");
    private final Instant sessionEnd = Instant.parse("2026-10-01T12:00:00Z");

    @BeforeEach
    void setUp() throws Exception {
        assertConnectedToRealMySql();

        transactionTemplate = new TransactionTemplate(transactionManager);
        cleanupDatabase();

        transactionTemplate.executeWithoutResult(status -> {
            VaccineType vaccineType = vaccineTypeRepository.saveAndFlush(
                    new VaccineType("VAX-CONCUR-01", "Concurrency Vaccine", "dose", true)
            );
            TargetGroup targetGroup = targetGroupRepository.saveAndFlush(
                    new TargetGroup("TG-CONCUR-01", "Concurrency Target Group", true)
            );
            center = centerRepository.saveAndFlush(
                    new VaccinationCenter("CTR-CONCUR-A", "Concurrency Center A", "100 Ave A", "555-0101", CenterStatus.ACTIVE, 50, 100, "UTC")
            );

            for (DayOfWeek day : DayOfWeek.values()) {
                operatingHoursRepository.saveAndFlush(new OperatingHours(center, day, LocalTime.of(8, 0), LocalTime.of(20, 0), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)));
            }

            campaign = campaignRepository.saveAndFlush(
                    CampaignTestFixtures.campaign(
                            "CMP-CONCUR-01",
                            "Concurrency Campaign",
                            "Desc",
                            vaccineType,
                            targetGroup,
                            LocalDate.of(2026, 6, 1),
                            LocalDate.of(2026, 12, 31),
                            Instant.parse("2026-05-01T08:00:00Z"),
                            Instant.parse("2026-12-31T18:00:00Z"),
                            CampaignStatus.DRAFT
                    )
            );

            doseProgrammeRepository.saveAndFlush(new CampaignDoseProgramme(
                    campaign, 1, DoseCategory.PRIMARY, "1,2", 21, false, 10,
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 12, 31)
            ));

            session = sessionRepository.saveAndFlush(new VaccinationSession(
                    campaign, center, sessionStart, sessionEnd, Instant.parse("2026-10-01T07:00:00Z"), "UTC"
            ));

            OperationalStaffRoster roster1 = staffRosterRepository.saveAndFlush(
                    new OperationalStaffRoster(center, 201L, StaffRole.VACCINATOR, OperationalStaffStatus.ACTIVE)
            );
            OperationalStaffRoster roster2 = staffRosterRepository.saveAndFlush(
                    new OperationalStaffRoster(center, 202L, StaffRole.VACCINATOR, OperationalStaffStatus.ACTIVE)
            );
            staffAssignmentRepository.saveAndFlush(new StaffAssignment(session, roster1, StaffRole.VACCINATOR, sessionStart, sessionEnd));
            staffAssignmentRepository.saveAndFlush(new StaffAssignment(session, roster2, StaffRole.VACCINATOR, sessionStart, sessionEnd));

            OperationalResource station1 = resourceRepository.saveAndFlush(
                    new OperationalResource(center, "STATION-1", ResourceType.VACCINATION_STATION, "Station 1", OperationalResourceStatus.ACTIVE)
            );
            OperationalResource station2 = resourceRepository.saveAndFlush(
                    new OperationalResource(center, "STATION-2", ResourceType.VACCINATION_STATION, "Station 2", OperationalResourceStatus.ACTIVE)
            );
            resourceAssignmentRepository.saveAndFlush(new SessionResourceAssignment(session, station1, sessionStart, sessionEnd));
            resourceAssignmentRepository.saveAndFlush(new SessionResourceAssignment(session, station2, sessionStart, sessionEnd));
        });
    }

    @AfterEach
    void tearDown() {
        if (transactionTemplate != null) {
            cleanupDatabase();
        }
    }

    private void cleanupDatabase() {
        transactionTemplate.executeWithoutResult(status -> {
            timeSlotRepository.deleteAllInBatch();
            resourceAssignmentRepository.deleteAllInBatch();
            staffAssignmentRepository.deleteAllInBatch();
            capacityPlanRepository.deleteAllInBatch();
            sessionChangeEventRepository.deleteAllInBatch();
            sessionRepository.deleteAllInBatch();
            ruleRepository.deleteAllInBatch();
            resourceRepository.deleteAllInBatch();
            staffRosterRepository.deleteAllInBatch();
            identityRepository.deleteAllInBatch();
            campaignCenterAssignmentRepository.deleteAllInBatch();
            closureRepository.deleteAllInBatch();
            operatingHoursRepository.deleteAllInBatch();
            doseProgrammeRepository.deleteAllInBatch();
            lifecycleEventRepository.deleteAllInBatch();
            campaignRepository.deleteAllInBatch();
            centerRepository.deleteAllInBatch();
            targetGroupRepository.deleteAllInBatch();
            vaccineTypeRepository.deleteAllInBatch();
        });
    }

    @Test
    @Timeout(30)
    @DisplayName("Concurrent plan creation on same DRAFT session produces sequential serialized revisions without collision")
    void concurrentPlanCreation_serializedRevisions() throws Exception {
        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch readyLatch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicBoolean errorOccurred = new AtomicBoolean(false);

        Long sessionId = session.getId();

        Future<?> f1 = executor.submit(() -> {
            readyLatch.countDown();
            try {
                assertTrue(startLatch.await(10, TimeUnit.SECONDS), "Start latch timed out");
                CreateSessionCapacityPlanRequest request1 = new CreateSessionCapacityPlanRequest(
                        2, 48, null, null, null, null, "operator-A"
                );
                capacityPlanService.createCapacityPlan(sessionId, request1);
            } catch (Exception e) {
                errorOccurred.set(true);
                throw new RuntimeException("Thread 1 execution failed", e);
            }
        });

        Future<?> f2 = executor.submit(() -> {
            readyLatch.countDown();
            try {
                assertTrue(startLatch.await(10, TimeUnit.SECONDS), "Start latch timed out");
                CreateSessionCapacityPlanRequest request2 = new CreateSessionCapacityPlanRequest(
                        2, 48, null, null, null, null, "operator-B"
                );
                capacityPlanService.createCapacityPlan(sessionId, request2);
            } catch (Exception e) {
                errorOccurred.set(true);
                throw new RuntimeException("Thread 2 execution failed", e);
            }
        });

        assertTrue(readyLatch.await(10, TimeUnit.SECONDS), "Ready latch timed out");
        startLatch.countDown();

        f1.get(15, TimeUnit.SECONDS);
        f2.get(15, TimeUnit.SECONDS);
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        assertFalse(errorOccurred.get(), "No thread should fail unexpectedly under serialized pessimistic lock");

        List<SessionCapacityPlan> plans = capacityPlanRepository.findBySessionIdOrderByRevisionAsc(sessionId);
        assertEquals(2, plans.size(), "Both concurrent creates should serialize and produce exactly 2 revisions");
        assertEquals(1, plans.get(0).getRevision(), "First revision must be 1");
        assertEquals(2, plans.get(1).getRevision(), "Second revision must be 2");
    }
}
