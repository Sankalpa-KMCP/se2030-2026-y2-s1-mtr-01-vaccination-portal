package com.se2030.vaccinationportal.appointment.service;

import com.se2030.vaccinationportal.appointment.dependency.EligibilityLookupPort;
import com.se2030.vaccinationportal.appointment.dependency.EligibilityStatus;
import com.se2030.vaccinationportal.appointment.dependency.SlotCapacityPort;
import com.se2030.vaccinationportal.appointment.exception.DuplicateActiveAppointmentException;
import com.se2030.vaccinationportal.appointment.exception.NotEligibleException;
import com.se2030.vaccinationportal.appointment.exception.SlotFullException;
import com.se2030.vaccinationportal.appointment.model.Appointment;
import com.se2030.vaccinationportal.appointment.model.AppointmentStatus;
import com.se2030.vaccinationportal.appointment.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.repository.CitizenCampaignBookingGuardRepository;
import java.util.List;
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
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Service-level unit and integration tests for {@link AppointmentBookingService}.
 * Verifies successful booking, eligibility rejection (MF03-BR-01), duplicate active appointment rejection (MF03-BR-02),
 * slot capacity exhaustion rejection, and atomic last-slot concurrent booking safety (MF03-BR-03).
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(AppointmentBookingServiceTests.TestConfig.class)
class AppointmentBookingServiceTests {

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
    private AppointmentBookingService bookingService;

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
        appointmentRepository.deleteAllInBatch();
        guardRepository.deleteAllInBatch();
        eligibilityPortDouble.setDefaultStatus(EligibilityStatus.ELIGIBLE);
    }

    @AfterEach
    void tearDown() {
        appointmentRepository.deleteAllInBatch();
        guardRepository.deleteAllInBatch();
    }

    @Test
    void bookAppointment_successfulPath_createsAndPersistsConfirmedAppointment() {
        Long citizenId = 1001L;
        Long campaignId = 2001L;
        Long slotId = 3001L;

        eligibilityPortDouble.setEligibility(citizenId, campaignId, EligibilityStatus.ELIGIBLE);
        slotCapacityPortDouble.setAvailableCapacity(slotId, 5);

        Appointment appointment = bookingService.bookAppointment(citizenId, campaignId, slotId);

        assertNotNull(appointment);
        assertNotNull(appointment.getId());
        assertEquals(citizenId, appointment.getCitizenId());
        assertEquals(campaignId, appointment.getCampaignId());
        assertEquals(slotId, appointment.getSlotId());
        assertEquals(AppointmentStatus.CONFIRMED, appointment.getStatus());

        List<Appointment> saved = appointmentRepository.findByCitizenIdAndCampaignIdAndStatusIn(
                citizenId, campaignId, List.of(AppointmentStatus.CONFIRMED));
        assertEquals(1, saved.size());
        assertEquals(appointment.getId(), saved.get(0).getId());
    }

    @Test
    void bookAppointment_notEligible_rejectsWithNotEligibleException() {
        Long citizenId = 1002L;
        Long campaignId = 2001L;
        Long slotId = 3001L;

        eligibilityPortDouble.setEligibility(citizenId, campaignId, EligibilityStatus.NOT_ELIGIBLE);
        slotCapacityPortDouble.setAvailableCapacity(slotId, 5);

        NotEligibleException ex = assertThrows(
                NotEligibleException.class,
                () -> bookingService.bookAppointment(citizenId, campaignId, slotId)
        );
        assertTrue(ex.getMessage().contains("not eligible"));

        List<Appointment> saved = appointmentRepository.findByCitizenIdAndCampaignIdAndStatusIn(
                citizenId, campaignId, List.of(AppointmentStatus.CONFIRMED));
        assertTrue(saved.isEmpty());
    }

    @Test
    void bookAppointment_duplicateActiveAppointment_rejectsWithDuplicateActiveAppointmentException() {
        Long citizenId = 1003L;
        Long campaignId = 2001L;
        Long slotId1 = 3001L;
        Long slotId2 = 3002L;

        eligibilityPortDouble.setEligibility(citizenId, campaignId, EligibilityStatus.ELIGIBLE);
        slotCapacityPortDouble.setAvailableCapacity(slotId1, 5);
        slotCapacityPortDouble.setAvailableCapacity(slotId2, 5);

        // First booking succeeds
        Appointment first = bookingService.bookAppointment(citizenId, campaignId, slotId1);
        assertNotNull(first);

        // Second booking attempt for same citizen and campaign throws DuplicateActiveAppointmentException
        DuplicateActiveAppointmentException ex = assertThrows(
                DuplicateActiveAppointmentException.class,
                () -> bookingService.bookAppointment(citizenId, campaignId, slotId2)
        );
        assertTrue(ex.getMessage().contains("already has an active appointment"));

        List<Appointment> saved = appointmentRepository.findByCitizenIdAndCampaignIdAndStatusIn(
                citizenId, campaignId, List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.CHECKED_IN));
        assertEquals(1, saved.size());
    }

    @Test
    void bookAppointment_slotFull_rejectsWithSlotFullException() {
        Long citizenId = 1004L;
        Long campaignId = 2001L;
        Long slotId = 3002L;

        eligibilityPortDouble.setEligibility(citizenId, campaignId, EligibilityStatus.ELIGIBLE);
        slotCapacityPortDouble.setAvailableCapacity(slotId, 0);

        SlotFullException ex = assertThrows(
                SlotFullException.class,
                () -> bookingService.bookAppointment(citizenId, campaignId, slotId)
        );
        assertTrue(ex.getMessage().contains("capacity reservation failed"));

        List<Appointment> saved = appointmentRepository.findByCitizenIdAndCampaignIdAndStatusIn(
                citizenId, campaignId, List.of(AppointmentStatus.CONFIRMED));
        assertTrue(saved.isEmpty());
    }

    @Test
    @Timeout(30)
    void bookAppointment_lastSlotConcurrentBooking_exactlyOneSucceedsAndOneGetsSlotFullException() throws Exception {
        Long citizen1 = 2001L;
        Long citizen2 = 2002L;
        Long campaignId = 4001L;
        Long slotId = 3003L;

        eligibilityPortDouble.setEligibility(citizen1, campaignId, EligibilityStatus.ELIGIBLE);
        eligibilityPortDouble.setEligibility(citizen2, campaignId, EligibilityStatus.ELIGIBLE);
        // Only 1 slot available
        slotCapacityPortDouble.setAvailableCapacity(slotId, 1);

        List<Outcome> outcomes = runConcurrently(
                () -> bookingService.bookAppointment(citizen1, campaignId, slotId),
                () -> bookingService.bookAppointment(citizen2, campaignId, slotId)
        );

        long successCount = outcomes.stream().filter(Outcome::success).count();
        long failureCount = outcomes.stream().filter(o -> !o.success()).count();

        assertEquals(1, successCount, "Exactly one booking must succeed for the last available slot");
        assertEquals(1, failureCount, "Exactly one booking must fail when capacity is exhausted");

        Outcome failed = outcomes.stream().filter(o -> !o.success()).findFirst().orElseThrow();
        assertInstanceOf(SlotFullException.class, failed.error(), "Failed booking must throw SlotFullException");

        List<Appointment> slotAppointments = appointmentRepository.findBySlotIdAndStatusIn(
                slotId, List.of(AppointmentStatus.CONFIRMED));
        assertEquals(1, slotAppointments.size());
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
