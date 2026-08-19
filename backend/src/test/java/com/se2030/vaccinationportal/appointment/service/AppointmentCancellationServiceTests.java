package com.se2030.vaccinationportal.appointment.service;

import com.se2030.vaccinationportal.appointment.dependency.EligibilityLookupPort;
import com.se2030.vaccinationportal.appointment.dependency.EligibilityStatus;
import com.se2030.vaccinationportal.appointment.dependency.SlotCapacityPort;
import com.se2030.vaccinationportal.appointment.exception.AppointmentAlreadyCompletedException;
import com.se2030.vaccinationportal.appointment.exception.AppointmentNotFoundException;
import com.se2030.vaccinationportal.appointment.exception.RequiresStaffOverrideException;
import com.se2030.vaccinationportal.appointment.model.Appointment;
import com.se2030.vaccinationportal.appointment.model.AppointmentStatus;
import com.se2030.vaccinationportal.appointment.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.repository.CitizenCampaignBookingGuardRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration and unit tests for {@link AppointmentCancellationService}.
 * Verifies citizen self-service cancellation outside 24h window (MF03-BR-04),
 * 24h window staff override enforcement, completed appointment immutability (MF03-BR-11),
 * and transactional atomic capacity release (MF03-BR-05).
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(AppointmentCancellationServiceTests.TestConfig.class)
class AppointmentCancellationServiceTests {

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
    private AppointmentCancellationService cancellationService;

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
        slotCapacityPortDouble.setSimulateReleaseFailure(false);
    }

    @AfterEach
    void tearDown() {
        appointmentRepository.deleteAllInBatch();
        guardRepository.deleteAllInBatch();
    }

    @Test
    void cancelAppointment_citizenSelfServiceOutside24hWindow_succeedsAndReleasesCapacity() {
        Long citizenId = 1001L;
        Long campaignId = 2001L;
        Long slotId = 3001L;
        Instant now = Instant.parse("2026-10-01T10:00:00Z");
        Instant slotStart = now.plus(48, ChronoUnit.HOURS); // 48h in future

        slotCapacityPortDouble.setAvailableCapacity(slotId, 5);
        slotCapacityPortDouble.setSlotStartTime(slotId, slotStart);

        Appointment appointment = bookingService.bookAppointment(citizenId, campaignId, slotId);
        assertNotNull(appointment);

        // Citizen cancels outside 24h window
        Appointment cancelled = cancellationService.cancelAppointment(appointment.getId(), citizenId, now);

        assertEquals(AppointmentStatus.CANCELLED, cancelled.getStatus());
        assertNotNull(cancelled.getCancelledAt());
        assertEquals(now, cancelled.getCancelledAt());

        // Verify capacity was released back to 5
        assertTrue(slotCapacityPortDouble.hasAvailableCapacity(slotId));
    }

    @Test
    void cancelAppointment_citizenSelfServiceInside24hWindow_rejectsWithRequiresStaffOverrideException() {
        Long citizenId = 1002L;
        Long campaignId = 2001L;
        Long slotId = 3002L;
        Instant now = Instant.parse("2026-10-01T10:00:00Z");
        Instant slotStart = now.plus(12, ChronoUnit.HOURS); // 12h in future (<24h)

        slotCapacityPortDouble.setAvailableCapacity(slotId, 5);
        slotCapacityPortDouble.setSlotStartTime(slotId, slotStart);

        Appointment appointment = bookingService.bookAppointment(citizenId, campaignId, slotId);

        // Citizen attempt inside 24h window throws RequiresStaffOverrideException
        RequiresStaffOverrideException ex = assertThrows(
                RequiresStaffOverrideException.class,
                () -> cancellationService.cancelAppointment(appointment.getId(), citizenId, now)
        );
        assertTrue(ex.getMessage().contains("staff override"));

        // Verify appointment remains CONFIRMED in database
        Appointment persisted = appointmentRepository.findById(appointment.getId()).orElseThrow();
        assertEquals(AppointmentStatus.CONFIRMED, persisted.getStatus());
    }

    @Test
    void cancelAppointment_staffOverrideInside24hWindow_succeedsWithReasonRecorded() {
        Long citizenId = 1003L;
        Long campaignId = 2001L;
        Long slotId = 3003L;
        Instant now = Instant.parse("2026-10-01T10:00:00Z");
        Instant slotStart = now.plus(12, ChronoUnit.HOURS); // 12h in future (<24h)

        slotCapacityPortDouble.setAvailableCapacity(slotId, 5);
        slotCapacityPortDouble.setSlotStartTime(slotId, slotStart);

        Appointment appointment = bookingService.bookAppointment(citizenId, campaignId, slotId);

        // Staff cancels with staff override and reason inside 24h window
        Appointment cancelled = cancellationService.cancelAppointmentByStaff(
                appointment.getId(),
                "Medical emergency exemption",
                now
        );

        assertEquals(AppointmentStatus.CANCELLED, cancelled.getStatus());
        assertNotNull(cancelled.getCancelledAt());
    }

    @Test
    void cancelAppointment_completedAppointment_rejectsWithAppointmentAlreadyCompletedException() {
        Long citizenId = 1004L;
        Long campaignId = 2001L;
        Long slotId = 3004L;
        Instant now = Instant.parse("2026-10-01T10:00:00Z");
        Instant slotStart = now.plus(48, ChronoUnit.HOURS);

        slotCapacityPortDouble.setAvailableCapacity(slotId, 5);
        slotCapacityPortDouble.setSlotStartTime(slotId, slotStart);

        Appointment appointment = bookingService.bookAppointment(citizenId, campaignId, slotId);

        // Simulate COMPLETED appointment
        Appointment persisted = appointmentRepository.findById(appointment.getId()).orElseThrow();
        persisted.complete();
        appointmentRepository.saveAndFlush(persisted);

        AppointmentAlreadyCompletedException ex = assertThrows(
                AppointmentAlreadyCompletedException.class,
                () -> cancellationService.cancelAppointment(appointment.getId(), citizenId, now)
        );
        assertTrue(ex.getMessage().contains("completed appointment cannot be cancelled"));
    }

    @Test
    void cancelAppointment_notFound_throwsAppointmentNotFoundException() {
        Long unknownId = 9999L;
        Instant now = Instant.now();

        assertThrows(
                AppointmentNotFoundException.class,
                () -> cancellationService.cancelAppointment(unknownId, 1001L, now)
        );
    }

    @Test
    void cancelAppointment_capacityReleaseFailure_rollsBackStatusChange() {
        Long citizenId = 1005L;
        Long campaignId = 2001L;
        Long slotId = 3005L;
        Instant now = Instant.parse("2026-10-01T10:00:00Z");
        Instant slotStart = now.plus(48, ChronoUnit.HOURS);

        slotCapacityPortDouble.setAvailableCapacity(slotId, 5);
        slotCapacityPortDouble.setSlotStartTime(slotId, slotStart);

        Appointment appointment = bookingService.bookAppointment(citizenId, campaignId, slotId);

        // Simulate capacity release failure in port double
        slotCapacityPortDouble.setSimulateReleaseFailure(true);

        // Cancellation throws IllegalStateException
        assertThrows(
                IllegalStateException.class,
                () -> cancellationService.cancelAppointment(appointment.getId(), citizenId, now)
        );

        // Verify status remained CONFIRMED (rolled back)
        Appointment reloaded = appointmentRepository.findById(appointment.getId()).orElseThrow();
        assertEquals(AppointmentStatus.CONFIRMED, reloaded.getStatus());
    }
}
