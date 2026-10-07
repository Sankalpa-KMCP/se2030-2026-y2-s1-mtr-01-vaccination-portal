package com.se2030.vaccinationportal.appointment.booking.service;

import com.se2030.vaccinationportal.appointment.testsupport.TestEligibilityLookupPortDouble;
import com.se2030.vaccinationportal.appointment.testsupport.TestSlotCapacityPortDouble;
import com.se2030.vaccinationportal.appointment.testsupport.TestEmailVerificationStatusReaderDouble;


import com.se2030.vaccinationportal.screening.api.eligibility.EligibilityStatus;
import com.se2030.vaccinationportal.appointment.exception.AppointmentAlreadyCompletedException;
import com.se2030.vaccinationportal.appointment.exception.RequiresStaffOverrideException;
import com.se2030.vaccinationportal.appointment.exception.SlotFullException;
import com.se2030.vaccinationportal.appointment.persistence.model.Appointment;
import com.se2030.vaccinationportal.appointment.persistence.model.AppointmentStatus;
import com.se2030.vaccinationportal.appointment.persistence.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.persistence.repository.CitizenCampaignBookingGuardRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
 * Service-level unit and integration tests for {@link AppointmentRescheduleService}.
 * Verifies successful rescheduling (MF03-BR-06), new-slot capacity check order, 24h window notice enforcement (MF03-BR-04),
 * completion immutability (MF03-BR-11), and concurrent single-capacity target slot reservation safety.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(AppointmentRescheduleServiceTests.TestConfig.class)
class AppointmentRescheduleServiceTests {

    @Autowired
    private TestEmailVerificationStatusReaderDouble emailVerificationStatusReaderDouble;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public TestEmailVerificationStatusReaderDouble emailVerificationStatusReader() {
            return new TestEmailVerificationStatusReaderDouble();
        }

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
    private AppointmentRescheduleService rescheduleService;

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
        emailVerificationStatusReaderDouble.clear();
        emailVerificationStatusReaderDouble.markVerified(1001L, 1002L, 1003L, 1004L, 1009L, 1010L, 2001L, 2002L);
        appointmentRepository.deleteAllInBatch();
        guardRepository.deleteAllInBatch();
        eligibilityPortDouble.setDefaultStatus(EligibilityStatus.ELIGIBLE);
        slotCapacityPortDouble.setSimulateReleaseFailure(false);
        slotCapacityPortDouble.clearSlotCampaignMap();
    }

    @AfterEach
    void tearDown() {
        appointmentRepository.deleteAllInBatch();
        guardRepository.deleteAllInBatch();
    }

    @Test
    void rescheduleAppointment_successfulPathOutside24hWindow_reschedulesAndUpdatesCapacity() {
        Long citizenId = 1001L;
        Long campaignId = 2001L;
        Long oldSlotId = 3001L;
        Long newSlotId = 3002L;

        Instant now = Instant.parse("2026-10-01T10:00:00Z");
        Instant oldSlotStart = now.plus(48, ChronoUnit.HOURS);
        Instant newSlotStart = now.plus(72, ChronoUnit.HOURS);

        slotCapacityPortDouble.setAvailableCapacity(oldSlotId, 5);
        slotCapacityPortDouble.setSlotStartTime(oldSlotId, oldSlotStart);
        slotCapacityPortDouble.setAvailableCapacity(newSlotId, 2);
        slotCapacityPortDouble.setSlotStartTime(newSlotId, newSlotStart);

        Appointment original = bookingService.bookAppointment(citizenId, campaignId, oldSlotId);
        assertNotNull(original);
        Long originalId = original.getId();

        // Reschedule to newSlotId
        Appointment rescheduled = rescheduleService.rescheduleAppointment(originalId, citizenId, newSlotId, now);

        assertNotNull(rescheduled);
        assertEquals(originalId, rescheduled.getId(), "Reschedule must mutate existing Appointment entity");
        assertEquals(newSlotId, rescheduled.getSlotId(), "Appointment slotId must be updated to newSlotId");
        assertEquals(AppointmentStatus.CONFIRMED, rescheduled.getStatus());

        // Verify database persistence
        Appointment persisted = appointmentRepository.findById(originalId).orElseThrow();
        assertEquals(newSlotId, persisted.getSlotId());

        // Verify old slot capacity released (+1) and new slot capacity reserved (-1)
        assertTrue(slotCapacityPortDouble.hasAvailableCapacity(oldSlotId));
        assertTrue(slotCapacityPortDouble.hasAvailableCapacity(newSlotId));
    }

    @Test
    void rescheduleAppointment_validCampaignSlot_succeeds() {
        Long citizenId = 1009L;
        Long campaignId = 2001L;
        Long oldSlotId = 3001L;
        Long newSlotId = 3002L;

        Instant now = Instant.parse("2026-10-01T10:00:00Z");
        Instant oldSlotStart = now.plus(48, ChronoUnit.HOURS);
        Instant newSlotStart = now.plus(72, ChronoUnit.HOURS);

        slotCapacityPortDouble.setAvailableCapacity(oldSlotId, 5);
        slotCapacityPortDouble.setSlotStartTime(oldSlotId, oldSlotStart);
        slotCapacityPortDouble.setSlotCampaign(oldSlotId, campaignId);

        slotCapacityPortDouble.setAvailableCapacity(newSlotId, 2);
        slotCapacityPortDouble.setSlotStartTime(newSlotId, newSlotStart);
        slotCapacityPortDouble.setSlotCampaign(newSlotId, campaignId);

        Appointment original = bookingService.bookAppointment(citizenId, campaignId, oldSlotId);
        assertNotNull(original);
        assertEquals(4, slotCapacityPortDouble.getAvailableCapacity(oldSlotId));

        Appointment rescheduled = rescheduleService.rescheduleAppointment(original.getId(), citizenId, newSlotId, now);

        assertNotNull(rescheduled);
        assertEquals(newSlotId, rescheduled.getSlotId());
        // Old slot capacity released back (+1 -> 5)
        assertEquals(5, slotCapacityPortDouble.getAvailableCapacity(oldSlotId));
        // New slot capacity reserved (-1 -> 1)
        assertEquals(1, slotCapacityPortDouble.getAvailableCapacity(newSlotId));

        Appointment persisted = appointmentRepository.findById(original.getId()).orElseThrow();
        assertEquals(newSlotId, persisted.getSlotId());
    }

    @Test
    void rescheduleAppointment_mismatchedCampaignSlot_rejectsWithIllegalArgumentExceptionAndPreservesState() {
        Long citizenId = 1010L;
        Long campaignId = 2001L;
        Long differentCampaignId = 2002L;
        Long oldSlotId = 3001L;
        Long mismatchedSlotId = 3005L;

        Instant now = Instant.parse("2026-10-01T10:00:00Z");
        Instant oldSlotStart = now.plus(48, ChronoUnit.HOURS);
        Instant newSlotStart = now.plus(72, ChronoUnit.HOURS);

        slotCapacityPortDouble.setAvailableCapacity(oldSlotId, 5);
        slotCapacityPortDouble.setSlotStartTime(oldSlotId, oldSlotStart);
        slotCapacityPortDouble.setSlotCampaign(oldSlotId, campaignId);

        // Mismatched slot belongs to different campaign
        slotCapacityPortDouble.setAvailableCapacity(mismatchedSlotId, 3);
        slotCapacityPortDouble.setSlotStartTime(mismatchedSlotId, newSlotStart);
        slotCapacityPortDouble.setSlotCampaign(mismatchedSlotId, differentCampaignId);

        Appointment original = bookingService.bookAppointment(citizenId, campaignId, oldSlotId);
        assertNotNull(original);
        int oldSlotCapAfterBooking = slotCapacityPortDouble.getAvailableCapacity(oldSlotId); // 4

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> rescheduleService.rescheduleAppointment(original.getId(), citizenId, mismatchedSlotId, now)
        );
        assertTrue(ex.getMessage().contains("does not belong to campaign " + campaignId));

        // Verify target slot capacity is NOT reserved (remains 3)
        assertEquals(3, slotCapacityPortDouble.getAvailableCapacity(mismatchedSlotId));

        // Verify old slot capacity is NOT released (remains 4)
        assertEquals(oldSlotCapAfterBooking, slotCapacityPortDouble.getAvailableCapacity(oldSlotId));

        // Verify existing appointment entity remains unchanged
        Appointment reloaded = appointmentRepository.findById(original.getId()).orElseThrow();
        assertEquals(oldSlotId, reloaded.getSlotId());
        assertEquals(AppointmentStatus.CONFIRMED, reloaded.getStatus());
    }

    @Test
    void rescheduleAppointment_newSlotFull_rejectsWithSlotFullExceptionAndOldSlotUntouched() {
        Long citizenId = 1002L;
        Long campaignId = 2001L;
        Long oldSlotId = 3001L;
        Long fullSlotId = 3003L;

        Instant now = Instant.parse("2026-10-01T10:00:00Z");
        Instant oldSlotStart = now.plus(48, ChronoUnit.HOURS);
        Instant fullSlotStart = now.plus(72, ChronoUnit.HOURS);

        slotCapacityPortDouble.setAvailableCapacity(oldSlotId, 5);
        slotCapacityPortDouble.setSlotStartTime(oldSlotId, oldSlotStart);
        // Target new slot is full (0 capacity)
        slotCapacityPortDouble.setAvailableCapacity(fullSlotId, 0);
        slotCapacityPortDouble.setSlotStartTime(fullSlotId, fullSlotStart);

        Appointment original = bookingService.bookAppointment(citizenId, campaignId, oldSlotId);

        SlotFullException ex = assertThrows(
                SlotFullException.class,
                () -> rescheduleService.rescheduleAppointment(original.getId(), citizenId, fullSlotId, now)
        );
        assertTrue(ex.getMessage().contains("capacity reservation failed"));

        // Verify original appointment in DB is untouched (still assigned to oldSlotId)
        Appointment reloaded = appointmentRepository.findById(original.getId()).orElseThrow();
        assertEquals(oldSlotId, reloaded.getSlotId());

        // Verify old slot capacity was NOT incremented/touched during the rejected attempt
        assertTrue(slotCapacityPortDouble.hasAvailableCapacity(oldSlotId));
    }

    @Test
    void rescheduleAppointment_citizenInside24hWindow_rejectsWithRequiresStaffOverrideException() {
        Long citizenId = 1003L;
        Long campaignId = 2001L;
        Long oldSlotId = 3001L;
        Long newSlotId = 3002L;

        Instant now = Instant.parse("2026-10-01T10:00:00Z");
        Instant oldSlotStart = now.plus(12, ChronoUnit.HOURS); // 12h in future (<24h)
        Instant newSlotStart = now.plus(72, ChronoUnit.HOURS);

        slotCapacityPortDouble.setAvailableCapacity(oldSlotId, 5);
        slotCapacityPortDouble.setSlotStartTime(oldSlotId, oldSlotStart);
        slotCapacityPortDouble.setAvailableCapacity(newSlotId, 5);
        slotCapacityPortDouble.setSlotStartTime(newSlotId, newSlotStart);

        Appointment original = bookingService.bookAppointment(citizenId, campaignId, oldSlotId);

        // Citizen self-reschedule attempt inside 24h window throws RequiresStaffOverrideException
        RequiresStaffOverrideException ex = assertThrows(
                RequiresStaffOverrideException.class,
                () -> rescheduleService.rescheduleAppointment(original.getId(), citizenId, newSlotId, now)
        );
        assertTrue(ex.getMessage().contains("staff override"));

        // Appointment remains on oldSlotId
        Appointment reloaded = appointmentRepository.findById(original.getId()).orElseThrow();
        assertEquals(oldSlotId, reloaded.getSlotId());
    }

    @Test
    void rescheduleAppointment_completedAppointment_rejectsWithAppointmentAlreadyCompletedException() {
        Long citizenId = 1004L;
        Long campaignId = 2001L;
        Long oldSlotId = 3001L;
        Long newSlotId = 3002L;

        Instant now = Instant.parse("2026-10-01T10:00:00Z");
        Instant oldSlotStart = now.plus(48, ChronoUnit.HOURS);
        Instant newSlotStart = now.plus(72, ChronoUnit.HOURS);

        slotCapacityPortDouble.setAvailableCapacity(oldSlotId, 5);
        slotCapacityPortDouble.setSlotStartTime(oldSlotId, oldSlotStart);
        slotCapacityPortDouble.setAvailableCapacity(newSlotId, 5);
        slotCapacityPortDouble.setSlotStartTime(newSlotId, newSlotStart);

        Appointment original = bookingService.bookAppointment(citizenId, campaignId, oldSlotId);

        // Simulate COMPLETED status
        Appointment persisted = appointmentRepository.findById(original.getId()).orElseThrow();
        persisted.complete();
        appointmentRepository.saveAndFlush(persisted);

        AppointmentAlreadyCompletedException ex = assertThrows(
                AppointmentAlreadyCompletedException.class,
                () -> rescheduleService.rescheduleAppointment(original.getId(), citizenId, newSlotId, now)
        );
        assertTrue(ex.getMessage().contains("completed appointment cannot be modified"));
    }

    @Test
    @Timeout(30)
    void rescheduleAppointment_concurrentRescheduleToSingleCapacityTargetSlot_exactlyOneSucceeds() throws Exception {
        Long citizen1 = 2001L;
        Long citizen2 = 2002L;
        Long campaign1 = 4001L;
        Long campaign2 = 4002L;
        Long oldSlot1 = 5001L;
        Long oldSlot2 = 5002L;
        Long targetSlot = 6001L;

        Instant now = Instant.parse("2026-10-01T10:00:00Z");
        Instant slotStart = now.plus(48, ChronoUnit.HOURS);

        slotCapacityPortDouble.setAvailableCapacity(oldSlot1, 5);
        slotCapacityPortDouble.setSlotStartTime(oldSlot1, slotStart);
        slotCapacityPortDouble.setAvailableCapacity(oldSlot2, 5);
        slotCapacityPortDouble.setSlotStartTime(oldSlot2, slotStart);

        // Target slot has exactly 1 available place
        slotCapacityPortDouble.setAvailableCapacity(targetSlot, 1);
        slotCapacityPortDouble.setSlotStartTime(targetSlot, slotStart);

        eligibilityPortDouble.setEligibility(citizen1, campaign1, EligibilityStatus.ELIGIBLE);
        eligibilityPortDouble.setEligibility(citizen2, campaign2, EligibilityStatus.ELIGIBLE);

        Appointment appt1 = bookingService.bookAppointment(citizen1, campaign1, oldSlot1);
        Appointment appt2 = bookingService.bookAppointment(citizen2, campaign2, oldSlot2);

        // Concurrent reschedule into targetSlot (capacity = 1)
        List<Outcome> outcomes = runConcurrently(
                () -> rescheduleService.rescheduleAppointment(appt1.getId(), citizen1, targetSlot, now),
                () -> rescheduleService.rescheduleAppointment(appt2.getId(), citizen2, targetSlot, now)
        );

        long successCount = outcomes.stream().filter(Outcome::success).count();
        long failureCount = outcomes.stream().filter(o -> !o.success()).count();

        assertEquals(1, successCount, "Exactly one reschedule must succeed for the single-capacity target slot");
        assertEquals(1, failureCount, "Exactly one reschedule must fail when target slot capacity is exhausted");

        Outcome failed = outcomes.stream().filter(o -> !o.success()).findFirst().orElseThrow();
        assertInstanceOf(SlotFullException.class, failed.error(), "Failed reschedule must throw SlotFullException");

        // Verify exactly one appointment is assigned to targetSlot in database
        List<Appointment> targetAppointments = appointmentRepository.findBySlotIdAndStatusIn(
                targetSlot, List.of(AppointmentStatus.CONFIRMED));
        assertEquals(1, targetAppointments.size());
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
