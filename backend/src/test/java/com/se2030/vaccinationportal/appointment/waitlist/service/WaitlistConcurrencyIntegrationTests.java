package com.se2030.vaccinationportal.appointment.waitlist.service;

import com.se2030.vaccinationportal.appointment.booking.service.AppointmentCancellationService;
import com.se2030.vaccinationportal.appointment.testsupport.TestEligibilityLookupPortDouble;


import com.se2030.vaccinationportal.screening.api.eligibility.EligibilityStatus;
import com.se2030.vaccinationportal.appointment.persistence.model.Appointment;
import com.se2030.vaccinationportal.appointment.persistence.model.AppointmentStatus;
import com.se2030.vaccinationportal.appointment.persistence.model.WaitlistEntry;
import com.se2030.vaccinationportal.appointment.persistence.model.WaitlistStatus;
import com.se2030.vaccinationportal.appointment.persistence.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.persistence.repository.CitizenCampaignBookingGuardRepository;
import com.se2030.vaccinationportal.appointment.persistence.repository.WaitlistRepository;
import com.se2030.vaccinationportal.campaign.CampaignTestFixtures;
import com.se2030.vaccinationportal.campaign.core.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.center.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.reference.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.session.planning.model.TimeSlot;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.session.planning.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.reference.model.VaccineType;
import com.se2030.vaccinationportal.campaign.reference.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.TimeSlotRepository;
import com.se2030.vaccinationportal.campaign.core.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.center.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.reference.repository.VaccineTypeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Deterministic concurrency tests validating serialization between waitlist admission
 * ({@link WaitlistJoinService}) and appointment cancellation/promotion
 * ({@link AppointmentCancellationService} / {@link WaitlistPromotionService}).
 * <p>
 * Proves that pessimistic row locking on {@link TimeSlot} rows prevents the race condition
 * where a waiter is admitted and stranded in WAITING status while capacity is freed concurrently.
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(WaitlistConcurrencyIntegrationTests.TestConfig.class)
class WaitlistConcurrencyIntegrationTests {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public TestEligibilityLookupPortDouble eligibilityLookupPort() {
            return new TestEligibilityLookupPortDouble();
        }
    }

    @Autowired
    private WaitlistJoinService waitlistJoinService;

    @Autowired
    private AppointmentCancellationService cancellationService;

    @Autowired
    private WaitlistRepository waitlistRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private CitizenCampaignBookingGuardRepository guardRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private VaccinationSessionRepository sessionRepository;

    @Autowired
    private VaccinationCampaignRepository campaignRepository;

    @Autowired
    private VaccinationCenterRepository centerRepository;

    @Autowired
    private VaccineTypeRepository vaccineTypeRepository;

    @Autowired
    private TargetGroupRepository targetGroupRepository;

    @Autowired
    private TestEligibilityLookupPortDouble eligibilityPortDouble;

    private VaccinationCampaign campaign;
    private VaccinationCenter center;
    private VaccinationSession session;
    private TimeSlot fullSlot;
    private Appointment existingAppointment;
    private final Long citizenWaiter = 7001L;
    private final Long citizenCancelling = 7002L;

    @BeforeEach
    void setUp() {
        cleanDb();
        eligibilityPortDouble.setDefaultStatus(EligibilityStatus.ELIGIBLE);

        VaccineType vax = vaccineTypeRepository.save(new VaccineType("VAX-CC-01", "Concurrency Vaccine"));
        TargetGroup tg = targetGroupRepository.save(new TargetGroup("TG-CC-01", "Concurrency Group"));

        campaign = campaignRepository.save(CampaignTestFixtures.campaign(
                "CMP-CC-01", "Concurrency Campaign", "Testing Waitlist Concurrency",
                vax, tg,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(30),
                Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(30, ChronoUnit.DAYS),
                CampaignStatus.ACTIVE
        ));

        center = centerRepository.save(new VaccinationCenter(
                "CTR-CC-01", "Concurrency Center", "200 Concurrency St", "+12345",
                CenterStatus.ACTIVE, 60, 100, "UTC"
        ));

        Instant baseStart = Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant baseEnd = baseStart.plus(8, ChronoUnit.HOURS);
        Instant bookingClose = baseStart.minus(2, ChronoUnit.HOURS);

        session = new VaccinationSession(campaign, center, baseStart, baseEnd, bookingClose, "UTC");
        session.schedule();
        session = sessionRepository.save(session);

        fullSlot = new TimeSlot(session, baseStart, baseStart.plus(1, ChronoUnit.HOURS), 1);
        fullSlot.setReservedCount(1);
        fullSlot = timeSlotRepository.save(fullSlot);

        existingAppointment = new Appointment(
                citizenCancelling, campaign.getId(), fullSlot.getId(),
                AppointmentStatus.CONFIRMED, Instant.now().minus(1, ChronoUnit.HOURS)
        );
        existingAppointment = appointmentRepository.save(existingAppointment);
    }

    @AfterEach
    void tearDown() {
        cleanDb();
    }

    private void cleanDb() {
        waitlistRepository.deleteAllInBatch();
        appointmentRepository.deleteAllInBatch();
        guardRepository.deleteAllInBatch();
        timeSlotRepository.deleteAllInBatch();
        sessionRepository.deleteAllInBatch();
        campaignRepository.deleteAllInBatch();
        centerRepository.deleteAllInBatch();
        vaccineTypeRepository.deleteAllInBatch();
        targetGroupRepository.deleteAllInBatch();
    }

    @RepeatedTest(5)
    @Timeout(30)
    @DisplayName("Concurrent waitlist join and appointment cancellation never leaves a stranded WAITING entry with open capacity")
    void concurrentWaitlistJoinAndCancellation_neverStrandsWaiter() throws Exception {
        Instant now = Instant.now();

        Callable<WaitlistEntry> joinAction = () ->
                waitlistJoinService.joinWaitlist(citizenWaiter, campaign.getId(), session.getId(), now);

        Callable<Appointment> cancelAction = () ->
                cancellationService.cancelAppointment(existingAppointment.getId(), citizenCancelling, now);

        List<Outcome> outcomes = runConcurrently(joinAction, cancelAction);

        Outcome joinOutcome = outcomes.get(0);
        Outcome cancelOutcome = outcomes.get(1);

        assertTrue(cancelOutcome.success(), "Cancellation must always succeed");

        TimeSlot slotAfter = timeSlotRepository.findById(fullSlot.getId()).orElseThrow();
        Optional<WaitlistEntry> waiterEntryOpt = waitlistRepository.findByCitizenIdAndCampaignIdAndStatus(
                citizenWaiter, campaign.getId(), WaitlistStatus.WAITING);

        if (joinOutcome.success()) {
            // If join succeeded, waiter was committed while slot was locked.
            // When cancellation released capacity and ran promotion, waiter must have been promoted!
            // OR if cancellation ran before promotion, waiter was promoted into the freed slot.
            List<Appointment> waiterAppointments = appointmentRepository.findByCitizenIdAndCampaignIdAndStatusIn(
                    citizenWaiter, campaign.getId(), List.of(AppointmentStatus.CONFIRMED));

            // Waiter must NOT be stranded in WAITING status while slot has capacity
            if (slotAfter.getReservedCount() == 0) {
                assertFalse(waiterEntryOpt.isPresent(), "Waiter cannot be left in WAITING state if slot capacity was freed");
            }
        } else {
            // Join failed because cancellation committed first and freed capacity,
            // so joinWaitlist observed available capacity and threw IllegalStateException!
            assertTrue(
                    joinOutcome.error() instanceof IllegalStateException,
                    "Join must fail with available booking capacity exception if slot was freed before join check: " + joinOutcome.error()
            );
            assertEquals(0, slotAfter.getReservedCount(), "Slot must have 0 reserved count if cancellation freed it and no waiter joined");
            assertTrue(waiterEntryOpt.isEmpty(), "No WAITING entry should exist in DB");
        }
    }

    private List<Outcome> runConcurrently(Callable<?> first, Callable<?> second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            Future<Outcome> firstFuture = executor.submit(contender(first, ready, start));
            Future<Outcome> secondFuture = executor.submit(contender(second, ready, start));

            assertTrue(ready.await(5, TimeUnit.SECONDS), "Contenders did not become ready in time");
            start.countDown();
            return List.of(firstFuture.get(15, TimeUnit.SECONDS), secondFuture.get(15, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Executor did not terminate");
        }
    }

    private Callable<Outcome> contender(Callable<?> action, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) {
                return new Outcome(false, new TimeoutException("Start barrier timed out"));
            }
            try {
                action.call();
                return new Outcome(true, null);
            } catch (Throwable error) {
                return new Outcome(false, error);
            }
        };
    }

    private record Outcome(boolean success, Throwable error) {
    }
}
