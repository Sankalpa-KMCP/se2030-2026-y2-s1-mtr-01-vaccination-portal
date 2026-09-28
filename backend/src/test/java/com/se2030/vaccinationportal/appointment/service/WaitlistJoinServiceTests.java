package com.se2030.vaccinationportal.appointment.service;

import com.se2030.vaccinationportal.appointment.dependency.EligibilityStatus;
import com.se2030.vaccinationportal.appointment.exception.DuplicateActiveAppointmentException;
import com.se2030.vaccinationportal.appointment.exception.DuplicateWaitlistEntryException;
import com.se2030.vaccinationportal.appointment.exception.NotEligibleException;
import com.se2030.vaccinationportal.appointment.model.Appointment;
import com.se2030.vaccinationportal.appointment.model.WaitlistEntry;
import com.se2030.vaccinationportal.appointment.model.WaitlistStatus;
import com.se2030.vaccinationportal.appointment.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.repository.CitizenCampaignBookingGuardRepository;
import com.se2030.vaccinationportal.appointment.repository.WaitlistRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit and integration tests for {@link WaitlistJoinService}.
 * Verifies successful waitlist join, eligibility rejection (MF03-BR-01), duplicate active appointment rejection (MF03-BR-02),
 * duplicate active waitlist entry rejection (MF03-BR-06 / MF03-BR-14), and concurrent duplicate join safety.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(WaitlistJoinServiceTests.TestConfig.class)
class WaitlistJoinServiceTests {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public TestEligibilityLookupPortDouble eligibilityLookupPort() {
            return new TestEligibilityLookupPortDouble();
        }

        @Bean
        public TestSlotCapacityPortDouble slotCapacityPort() {
            return new TestSlotCapacityPortDouble();
        }
    }

    @Autowired
    private WaitlistJoinService waitlistJoinService;

    @Autowired
    private AppointmentBookingService bookingService;

    @Autowired
    private WaitlistRepository waitlistRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private CitizenCampaignBookingGuardRepository guardRepository;

    @Autowired
    private TestEligibilityLookupPortDouble eligibilityPortDouble;

    @Autowired
    private TestSlotCapacityPortDouble slotCapacityPortDouble;

    @BeforeEach
    void setUp() {
        waitlistRepository.deleteAllInBatch();
        appointmentRepository.deleteAllInBatch();
        guardRepository.deleteAllInBatch();
        eligibilityPortDouble.setDefaultStatus(EligibilityStatus.ELIGIBLE);
        // Authoritative campaign/session relationships for R01 admission validation
        slotCapacityPortDouble.setSessionCampaign(3001L, 2001L);
        slotCapacityPortDouble.setSessionCampaign(5001L, 4001L);
    }

    @AfterEach
    void tearDown() {
        waitlistRepository.deleteAllInBatch();
        appointmentRepository.deleteAllInBatch();
        guardRepository.deleteAllInBatch();
        slotCapacityPortDouble.clearSessionCampaignMap();
    }

    @Test
    void joinWaitlist_successfulPath_createsAndPersistsWaitingEntry() {
        Long citizenId = 1001L;
        Long campaignId = 2001L;
        Long sessionId = 3001L;
        Instant now = Instant.parse("2026-10-01T10:00:00Z");

        eligibilityPortDouble.setEligibility(citizenId, campaignId, EligibilityStatus.ELIGIBLE);
        slotCapacityPortDouble.setSessionFull(sessionId, true);

        WaitlistEntry entry = waitlistJoinService.joinWaitlist(citizenId, campaignId, sessionId, now);

        assertNotNull(entry);
        assertNotNull(entry.getId());
        assertEquals(citizenId, entry.getCitizenId());
        assertEquals(campaignId, entry.getCampaignId());
        assertEquals(sessionId, entry.getSessionId());
        assertEquals(WaitlistStatus.WAITING, entry.getStatus());
        assertEquals(now, entry.getJoinedAt());

        // Verify database persistence
        Optional<WaitlistEntry> persisted = waitlistRepository.findByCitizenIdAndCampaignIdAndStatus(
                citizenId, campaignId, WaitlistStatus.WAITING);
        assertTrue(persisted.isPresent());
        assertEquals(entry.getId(), persisted.get().getId());
    }

    @Test
    void joinWaitlist_notEligible_rejectsWithNotEligibleException() {
        Long citizenId = 1002L;
        Long campaignId = 2001L;
        Long sessionId = 3001L;
        Instant now = Instant.now();

        eligibilityPortDouble.setEligibility(citizenId, campaignId, EligibilityStatus.NOT_ELIGIBLE);

        NotEligibleException ex = assertThrows(
                NotEligibleException.class,
                () -> waitlistJoinService.joinWaitlist(citizenId, campaignId, sessionId, now)
        );
        assertTrue(ex.getMessage().contains("not eligible"));

        Optional<WaitlistEntry> persisted = waitlistRepository.findByCitizenIdAndCampaignIdAndStatus(
                citizenId, campaignId, WaitlistStatus.WAITING);
        assertTrue(persisted.isEmpty());
    }

    @Test
    void joinWaitlist_activeAppointmentExists_rejectsWithDuplicateActiveAppointmentException() {
        Long citizenId = 1003L;
        Long campaignId = 2001L;
        Long slotId = 4001L;
        Long sessionId = 3001L;
        Instant now = Instant.now();

        eligibilityPortDouble.setEligibility(citizenId, campaignId, EligibilityStatus.ELIGIBLE);
        slotCapacityPortDouble.setAvailableCapacity(slotId, 5);

        // Citizen creates active appointment first
        Appointment appt = bookingService.bookAppointment(citizenId, campaignId, slotId);
        assertNotNull(appt);

        // Attempting to join waitlist for same campaign throws DuplicateActiveAppointmentException
        DuplicateActiveAppointmentException ex = assertThrows(
                DuplicateActiveAppointmentException.class,
                () -> waitlistJoinService.joinWaitlist(citizenId, campaignId, sessionId, now)
        );
        assertTrue(ex.getMessage().contains("already has an active appointment"));

        Optional<WaitlistEntry> persisted = waitlistRepository.findByCitizenIdAndCampaignIdAndStatus(
                citizenId, campaignId, WaitlistStatus.WAITING);
        assertTrue(persisted.isEmpty());
    }

    @Test
    void joinWaitlist_duplicateWaitingEntry_rejectsWithDuplicateWaitlistEntryException() {
        Long citizenId = 1004L;
        Long campaignId = 2001L;
        Long sessionId = 3001L;
        Instant now = Instant.now();

        eligibilityPortDouble.setEligibility(citizenId, campaignId, EligibilityStatus.ELIGIBLE);

        WaitlistEntry first = waitlistJoinService.joinWaitlist(citizenId, campaignId, sessionId, now);
        assertNotNull(first);

        // Second join attempt for same citizen and campaign throws DuplicateWaitlistEntryException
        DuplicateWaitlistEntryException ex = assertThrows(
                DuplicateWaitlistEntryException.class,
                () -> waitlistJoinService.joinWaitlist(citizenId, campaignId, sessionId, now)
        );
        assertTrue(ex.getMessage().contains("already has an active WAITING waitlist entry"));
    }

    @Test
    @Timeout(30)
    void joinWaitlist_concurrentJoinRequests_exactlyOneSucceedsAndOneGetsDuplicateException() throws Exception {
        Long citizenId = 2001L;
        Long campaignId = 4001L;
        Long sessionId = 5001L;
        Instant now = Instant.now();

        eligibilityPortDouble.setEligibility(citizenId, campaignId, EligibilityStatus.ELIGIBLE);

        List<Outcome> outcomes = runConcurrently(
                () -> waitlistJoinService.joinWaitlist(citizenId, campaignId, sessionId, now),
                () -> waitlistJoinService.joinWaitlist(citizenId, campaignId, sessionId, now)
        );

        long successCount = outcomes.stream().filter(Outcome::success).count();
        long failureCount = outcomes.stream().filter(o -> !o.success()).count();

        assertEquals(1, successCount, "Exactly one waitlist join must succeed for the citizen");
        assertEquals(1, failureCount, "Exactly one waitlist join must be rejected as duplicate");

        Outcome failed = outcomes.stream().filter(o -> !o.success()).findFirst().orElseThrow();
        assertTrue(
                failed.error() instanceof DuplicateWaitlistEntryException || failed.error() instanceof DataIntegrityViolationException,
                "Failed join must throw DuplicateWaitlistEntryException or DataIntegrityViolationException"
        );

        Optional<WaitlistEntry> persisted = waitlistRepository.findByCitizenIdAndCampaignIdAndStatus(
                citizenId, campaignId, WaitlistStatus.WAITING);
        assertTrue(persisted.isPresent());
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
            return List.of(firstFuture.get(10, TimeUnit.SECONDS), secondFuture.get(10, TimeUnit.SECONDS));
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
