package com.se2030.vaccinationportal.appointment.checkin.service;

import com.se2030.vaccinationportal.appointment.booking.service.AppointmentBookingService;
import com.se2030.vaccinationportal.appointment.booking.service.AppointmentCancellationService;
import com.se2030.vaccinationportal.appointment.booking.service.AppointmentRescheduleService;
import com.se2030.vaccinationportal.appointment.testsupport.TestEligibilityLookupPortDouble;
import com.se2030.vaccinationportal.appointment.testsupport.TestSlotCapacityPortDouble;


import com.se2030.vaccinationportal.appointment.dependency.EligibilityStatus;
import com.se2030.vaccinationportal.appointment.exception.InvalidAppointmentStateException;
import com.se2030.vaccinationportal.appointment.persistence.model.Appointment;
import com.se2030.vaccinationportal.appointment.persistence.model.AppointmentStatus;
import com.se2030.vaccinationportal.appointment.persistence.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.persistence.repository.CitizenCampaignBookingGuardRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Regression tests for NO_SHOW capacity ownership (R04).
 * A NO_SHOW appointment released its slot reservation when it was recorded, so it no longer owns
 * capacity: cancelling or rescheduling it must be rejected and must never release capacity a second
 * time. Reserved counters must never drop below the number of actual reservations.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(NoShowCapacityOwnershipTests.TestConfig.class)
class NoShowCapacityOwnershipTests {

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
    private CheckInService checkInService;

    @Autowired
    private AppointmentBookingService bookingService;

    @Autowired
    private AppointmentCancellationService cancellationService;

    @Autowired
    private AppointmentRescheduleService rescheduleService;

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
        slotCapacityPortDouble.setSimulateReleaseFailure(false);
    }

    @AfterEach
    void tearDown() {
        appointmentRepository.deleteAllInBatch();
        guardRepository.deleteAllInBatch();
    }

    private static final Instant SLOT_START = Instant.parse("2026-10-01T10:00:00Z");
    private static final Instant AFTER_WINDOW_CLOSE = SLOT_START.plus(45, ChronoUnit.MINUTES);

    @Test
    void markNoShow_releasesOwnedCapacityExactlyOnce() {
        Long campaignId = 2001L;
        Long slotId = 4101L;
        slotCapacityPortDouble.setAvailableCapacity(slotId, 1);
        slotCapacityPortDouble.setSlotStartTime(slotId, SLOT_START);

        Appointment appt = bookingService.bookAppointment(1001L, campaignId, slotId);
        assertEquals(0, slotCapacityPortDouble.getAvailableCapacity(slotId), "Reservation must consume the only place");

        checkInService.markNoShow(appt.getId(), AFTER_WINDOW_CLOSE);
        assertEquals(1, slotCapacityPortDouble.getAvailableCapacity(slotId), "NO_SHOW must release the reservation exactly once");

        // A second NO_SHOW marking is impossible: only CONFIRMED appointments can be marked NO_SHOW
        assertThrows(
                InvalidAppointmentStateException.class,
                () -> checkInService.markNoShow(appt.getId(), AFTER_WINDOW_CLOSE)
        );
        assertEquals(1, slotCapacityPortDouble.getAvailableCapacity(slotId), "Repeated NO_SHOW must not release again");
    }

    @Test
    void cancelAppointment_afterNoShow_isRejectedWithoutSecondRelease() {
        Long campaignId = 2001L;
        Long slotId = 4102L;
        slotCapacityPortDouble.setAvailableCapacity(slotId, 2);
        slotCapacityPortDouble.setSlotStartTime(slotId, SLOT_START);

        Appointment appt = bookingService.bookAppointment(1001L, campaignId, slotId);
        assertEquals(1, slotCapacityPortDouble.getAvailableCapacity(slotId));

        checkInService.markNoShow(appt.getId(), AFTER_WINDOW_CLOSE);
        assertEquals(2, slotCapacityPortDouble.getAvailableCapacity(slotId), "NO_SHOW released the reservation");

        InvalidAppointmentStateException ex = assertThrows(
                InvalidAppointmentStateException.class,
                () -> cancellationService.cancelAppointment(appt.getId(), 1001L, Instant.now())
        );
        assertNotNull(ex.getMessage());
        assertEquals(2, slotCapacityPortDouble.getAvailableCapacity(slotId), "Cancelled NO_SHOW must not release capacity a second time");
        assertEquals(AppointmentStatus.NO_SHOW,
                appointmentRepository.findById(appt.getId()).orElseThrow().getStatus());
    }

    @Test
    void rescheduleAppointment_afterNoShow_isRejectedWithoutCapacityChanges() {
        Long campaignId = 2001L;
        Long sourceSlotId = 4103L;
        Long targetSlotId = 4104L;
        slotCapacityPortDouble.setAvailableCapacity(sourceSlotId, 2);
        slotCapacityPortDouble.setSlotStartTime(sourceSlotId, SLOT_START);
        slotCapacityPortDouble.setAvailableCapacity(targetSlotId, 2);
        slotCapacityPortDouble.setSlotStartTime(targetSlotId, SLOT_START);

        Appointment appt = bookingService.bookAppointment(1001L, campaignId, sourceSlotId);
        assertEquals(1, slotCapacityPortDouble.getAvailableCapacity(sourceSlotId));

        checkInService.markNoShow(appt.getId(), AFTER_WINDOW_CLOSE);
        assertEquals(2, slotCapacityPortDouble.getAvailableCapacity(sourceSlotId));

        InvalidAppointmentStateException ex = assertThrows(
                InvalidAppointmentStateException.class,
                () -> rescheduleService.rescheduleAppointment(appt.getId(), 1001L, targetSlotId, Instant.now())
        );
        assertNotNull(ex.getMessage());
        assertEquals(2, slotCapacityPortDouble.getAvailableCapacity(sourceSlotId),
                "Rescheduled NO_SHOW must not release the source capacity a second time");
        assertEquals(2, slotCapacityPortDouble.getAvailableCapacity(targetSlotId),
                "Rescheduled NO_SHOW must not consume destination capacity");
        assertEquals(AppointmentStatus.NO_SHOW,
                appointmentRepository.findById(appt.getId()).orElseThrow().getStatus());
    }

    @Test
    void multipleReservations_retainExactCounterWhenNoShowModificationIsRejected() {
        Long campaignId = 2001L;
        Long slotId = 4105L;
        Long otherSlotId = 4108L;
        slotCapacityPortDouble.setAvailableCapacity(slotId, 3);
        slotCapacityPortDouble.setSlotStartTime(slotId, SLOT_START);
        slotCapacityPortDouble.setAvailableCapacity(otherSlotId, 3);
        slotCapacityPortDouble.setSlotStartTime(otherSlotId, SLOT_START);

        Appointment appt1 = bookingService.bookAppointment(1001L, campaignId, slotId);
        Appointment appt2 = bookingService.bookAppointment(1002L, campaignId, slotId);
        Appointment appt3 = bookingService.bookAppointment(1003L, campaignId, slotId);
        assertEquals(0, slotCapacityPortDouble.getAvailableCapacity(slotId), "Three reservations consume the whole slot");

        checkInService.markNoShow(appt1.getId(), AFTER_WINDOW_CLOSE);
        assertEquals(1, slotCapacityPortDouble.getAvailableCapacity(slotId), "One NO_SHOW releases exactly one place");

        assertThrows(
                InvalidAppointmentStateException.class,
                () -> cancellationService.cancelAppointment(appt1.getId(), 1001L, Instant.now())
        );
        assertThrows(
                InvalidAppointmentStateException.class,
                () -> rescheduleService.rescheduleAppointment(appt1.getId(), 1001L, otherSlotId, Instant.now())
        );

        // Two real reservations remain (citizens 2 and 3); the counter must match exactly
        assertEquals(1, slotCapacityPortDouble.getAvailableCapacity(slotId),
                "Reserved counter must stay exactly one above the two remaining reservations");
        assertEquals(3, slotCapacityPortDouble.getAvailableCapacity(otherSlotId),
                "Rejected NO_SHOW reschedule must not consume the destination slot");

        // Control: ordinary cancellation of a reservation-owning appointment releases exactly once
        cancellationService.cancelAppointment(appt2.getId(), 1002L, Instant.now());
        assertEquals(2, slotCapacityPortDouble.getAvailableCapacity(slotId));
        assertEquals(AppointmentStatus.CANCELLED,
                appointmentRepository.findById(appt2.getId()).orElseThrow().getStatus());
    }

    @Test
    void ordinaryReschedule_movesExactlyOneReservation() {
        Long campaignId = 2001L;
        Long sourceSlotId = 4106L;
        Long targetSlotId = 4107L;
        slotCapacityPortDouble.setAvailableCapacity(sourceSlotId, 2);
        slotCapacityPortDouble.setSlotStartTime(sourceSlotId, SLOT_START);
        slotCapacityPortDouble.setAvailableCapacity(targetSlotId, 2);
        slotCapacityPortDouble.setSlotStartTime(targetSlotId, SLOT_START);

        Appointment appt = bookingService.bookAppointment(1001L, campaignId, sourceSlotId);
        assertEquals(1, slotCapacityPortDouble.getAvailableCapacity(sourceSlotId));
        assertEquals(2, slotCapacityPortDouble.getAvailableCapacity(targetSlotId));

        Appointment rescheduled = rescheduleService.rescheduleAppointment(appt.getId(), 1001L, targetSlotId, Instant.now());
        assertEquals(targetSlotId, rescheduled.getSlotId());
        assertEquals(AppointmentStatus.CONFIRMED,
                appointmentRepository.findById(appt.getId()).orElseThrow().getStatus());

        assertEquals(2, slotCapacityPortDouble.getAvailableCapacity(sourceSlotId), "Source slot must regain exactly one place");
        assertEquals(1, slotCapacityPortDouble.getAvailableCapacity(targetSlotId), "Target slot must lose exactly one place");
    }

    @Test
    void markNoShow_whenCapacityReleaseFails_throwsIllegalStateExceptionAndDoesNotTransitionToNoShow() {
        Long campaignId = 2001L;
        Long slotId = 4109L;
        slotCapacityPortDouble.setAvailableCapacity(slotId, 1);
        slotCapacityPortDouble.setSlotStartTime(slotId, SLOT_START);

        Appointment appt = bookingService.bookAppointment(1001L, campaignId, slotId);
        assertEquals(0, slotCapacityPortDouble.getAvailableCapacity(slotId));

        slotCapacityPortDouble.setSimulateReleaseFailure(true);

        assertThrows(
                IllegalStateException.class,
                () -> checkInService.markNoShow(appt.getId(), AFTER_WINDOW_CLOSE)
        );

        Appointment reloaded = appointmentRepository.findById(appt.getId()).orElseThrow();
        assertEquals(AppointmentStatus.CONFIRMED, reloaded.getStatus(),
                "Appointment must remain CONFIRMED when capacity release fails");
    }
}
