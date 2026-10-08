package com.se2030.vaccinationportal.appointment.checkin.service;

import com.se2030.vaccinationportal.appointment.booking.service.AppointmentBookingService;
import com.se2030.vaccinationportal.appointment.testsupport.TestEligibilityLookupPortDouble;
import com.se2030.vaccinationportal.appointment.testsupport.TestSlotCapacityPortDouble;
import com.se2030.vaccinationportal.appointment.testsupport.TestEmailVerificationStatusReaderDouble;
import com.se2030.vaccinationportal.appointment.waitlist.service.WaitlistJoinService;


import com.se2030.vaccinationportal.screening.api.eligibility.EligibilityStatus;
import com.se2030.vaccinationportal.appointment.exception.AppointmentAlreadyCompletedException;
import com.se2030.vaccinationportal.appointment.exception.InvalidAppointmentStateException;
import com.se2030.vaccinationportal.appointment.exception.InvalidCheckInWindowException;
import com.se2030.vaccinationportal.appointment.persistence.model.Appointment;
import com.se2030.vaccinationportal.appointment.persistence.model.AppointmentStatus;
import com.se2030.vaccinationportal.appointment.persistence.model.WaitlistEntry;
import com.se2030.vaccinationportal.appointment.persistence.model.WaitlistStatus;
import com.se2030.vaccinationportal.appointment.persistence.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.persistence.repository.CitizenCampaignBookingGuardRepository;
import com.se2030.vaccinationportal.appointment.persistence.repository.WaitlistRepository;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationDraft;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationRecord;
import com.se2030.vaccinationportal.vaccination.administration.repository.VaccinationDraftRepository;
import com.se2030.vaccinationportal.vaccination.administration.repository.VaccinationRecordRepository;
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
 * Unit and integration tests for {@link CheckInService}.
 * Verifies successful check-in inside window, window rejection, status guard rejection, check-in reversal,
 * no-show capacity release and waitlist promotion, and vaccinator queue queries (US-26 / MF03-BR-09 / MF03-BR-10 / MF03-BR-15).
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(CheckInServiceTests.TestConfig.class)
class CheckInServiceTests {

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
    private CheckInService checkInService;

    @Autowired
    private AppointmentBookingService bookingService;

    @Autowired
    private WaitlistJoinService joinService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private WaitlistRepository waitlistRepository;

    @Autowired
    private CitizenCampaignBookingGuardRepository guardRepository;

    @Autowired
    private VaccinationDraftRepository vaccinationDraftRepository;

    @Autowired
    private VaccinationRecordRepository vaccinationRecordRepository;

    @Autowired
    private TestEligibilityLookupPortDouble eligibilityPortDouble;

    @Autowired
    private TestSlotCapacityPortDouble slotCapacityPortDouble;

    @BeforeEach
    void setUp() {
        emailVerificationStatusReaderDouble.clear();
        emailVerificationStatusReaderDouble.markVerified(1001L, 1002L, 1003L, 1004L);
        vaccinationRecordRepository.deleteAllInBatch();
        vaccinationDraftRepository.deleteAllInBatch();
        waitlistRepository.deleteAllInBatch();
        appointmentRepository.deleteAllInBatch();
        guardRepository.deleteAllInBatch();
        eligibilityPortDouble.setDefaultStatus(EligibilityStatus.ELIGIBLE);
        slotCapacityPortDouble.setSimulateReleaseFailure(false);
        // Authoritative campaign/session relationship for R01 admission validation
        slotCapacityPortDouble.setSessionCampaign(3001L, 2001L);
    }

    @AfterEach
    void tearDown() {
        vaccinationRecordRepository.deleteAllInBatch();
        vaccinationDraftRepository.deleteAllInBatch();
        waitlistRepository.deleteAllInBatch();
        appointmentRepository.deleteAllInBatch();
        guardRepository.deleteAllInBatch();
        slotCapacityPortDouble.clearSessionCampaignMap();
    }

    @Test
    void checkIn_successfulInsideWindow_updatesStatusToCheckedIn() {
        Long citizenId = 1001L;
        Long campaignId = 2001L;
        Long slotId = 4001L;
        Instant slotStart = Instant.parse("2026-10-01T10:00:00Z");
        Instant now = slotStart.plus(15, ChronoUnit.MINUTES);

        slotCapacityPortDouble.setAvailableCapacity(slotId, 5);
        slotCapacityPortDouble.setSlotStartTime(slotId, slotStart);

        Appointment appt = bookingService.bookAppointment(citizenId, campaignId, slotId);
        assertNotNull(appt);
        assertEquals(AppointmentStatus.CONFIRMED, appt.getStatus());

        Appointment checkedIn = checkInService.checkIn(appt.getId(), now);
        assertEquals(AppointmentStatus.CHECKED_IN, checkedIn.getStatus());
    }

    @Test
    void checkIn_outsideWindow_rejectsWithInvalidCheckInWindowException() {
        Long citizenId = 1002L;
        Long campaignId = 2001L;
        Long slotId = 4002L;
        Instant slotStart = Instant.parse("2026-10-01T10:00:00Z");
        Instant lateTime = slotStart.plus(45, ChronoUnit.MINUTES);

        slotCapacityPortDouble.setAvailableCapacity(slotId, 5);
        slotCapacityPortDouble.setSlotStartTime(slotId, slotStart);

        Appointment appt = bookingService.bookAppointment(citizenId, campaignId, slotId);

        InvalidCheckInWindowException ex = assertThrows(
                InvalidCheckInWindowException.class,
                () -> checkInService.checkIn(appt.getId(), lateTime)
        );
        assertTrue(ex.getMessage().contains("outside the allowable 30-minute window"));
    }

    @Test
    void checkIn_nonConfirmedStatus_rejectsWithInvalidAppointmentStateException() {
        Long citizenId = 1003L;
        Long campaignId = 2001L;
        Long slotId = 4003L;
        Instant slotStart = Instant.parse("2026-10-01T10:00:00Z");
        Instant now = slotStart.plus(10, ChronoUnit.MINUTES);

        slotCapacityPortDouble.setAvailableCapacity(slotId, 5);
        slotCapacityPortDouble.setSlotStartTime(slotId, slotStart);

        Appointment appt = bookingService.bookAppointment(citizenId, campaignId, slotId);
        checkInService.checkIn(appt.getId(), now);

        // Attempt second check-in on already CHECKED_IN appointment
        assertThrows(
                InvalidAppointmentStateException.class,
                () -> checkInService.checkIn(appt.getId(), now)
        );

        // Complete appointment and test completed check-in rejection
        Appointment latestAppt = appointmentRepository.findById(appt.getId()).orElseThrow();
        latestAppt.complete();
        appointmentRepository.save(latestAppt);

        assertThrows(
                AppointmentAlreadyCompletedException.class,
                () -> checkInService.checkIn(appt.getId(), now)
        );
    }

    @Test
    void reverseCheckIn_checkedInAppointment_revertsToConfirmed() {
        Long citizenId = 1004L;
        Long campaignId = 2001L;
        Long slotId = 4004L;
        Instant slotStart = Instant.parse("2026-10-01T10:00:00Z");
        Instant now = slotStart.plus(10, ChronoUnit.MINUTES);

        slotCapacityPortDouble.setAvailableCapacity(slotId, 5);
        slotCapacityPortDouble.setSlotStartTime(slotId, slotStart);

        Appointment appt = bookingService.bookAppointment(citizenId, campaignId, slotId);
        checkInService.checkIn(appt.getId(), now);

        Appointment reversed = checkInService.reverseCheckIn(appt.getId(), "Incorrect arrival selected");
        assertEquals(AppointmentStatus.CONFIRMED, reversed.getStatus());
    }

    @Test
    void reverseCheckIn_requiresReasonAndRejectsActiveVaccinationDraft() {
        Long slotId = 4011L;
        Instant slotStart = Instant.parse("2026-10-01T10:00:00Z");
        slotCapacityPortDouble.setAvailableCapacity(slotId, 5);
        slotCapacityPortDouble.setSlotStartTime(slotId, slotStart);

        Appointment appointment = bookingService.bookAppointment(1001L, 2001L, slotId);
        checkInService.checkIn(appointment.getId(), slotStart);

        assertThrows(IllegalArgumentException.class,
                () -> checkInService.reverseCheckIn(appointment.getId(), " "));

        vaccinationDraftRepository.saveAndFlush(new VaccinationDraft(appointment.getId()));

        InvalidAppointmentStateException error = assertThrows(
                InvalidAppointmentStateException.class,
                () -> checkInService.reverseCheckIn(appointment.getId(), "Citizen selected incorrectly")
        );
        assertTrue(error.getMessage().contains("vaccination draft"));
    }

    @Test
    void reverseCheckIn_rejectsFinalizedVaccinationRecord() {
        Long slotId = 4012L;
        Instant slotStart = Instant.parse("2026-10-01T10:00:00Z");
        slotCapacityPortDouble.setAvailableCapacity(slotId, 5);
        slotCapacityPortDouble.setSlotStartTime(slotId, slotStart);

        Appointment appointment = bookingService.bookAppointment(1002L, 2001L, slotId);
        checkInService.checkIn(appointment.getId(), slotStart);
        vaccinationRecordRepository.saveAndFlush(new VaccinationRecord(
                appointment.getId(),
                appointment.getCitizenId(),
                appointment.getCampaignId(),
                5001L,
                6001L,
                7001L,
                1,
                slotStart,
                null,
                "Finalized vaccination",
                slotStart.plusSeconds(60)
        ));

        InvalidAppointmentStateException error = assertThrows(
                InvalidAppointmentStateException.class,
                () -> checkInService.reverseCheckIn(appointment.getId(), "Incorrect check-in")
        );
        assertTrue(error.getMessage().contains("finalization"));
    }

    @Test
    void markNoShow_afterWindowClose_releasesCapacityAndTriggersWaitlistPromotion() {
        Long citizen1 = 1001L;
        Long citizen2 = 1002L;
        Long campaignId = 2001L;
        Long sessionId = 3001L;
        Long slotId = 4005L;

        Instant slotStart = Instant.parse("2026-10-01T10:00:00Z");
        Instant afterWindowClose = slotStart.plus(45, ChronoUnit.MINUTES);

        slotCapacityPortDouble.setAvailableCapacity(slotId, 1);
        slotCapacityPortDouble.setSlotStartTime(slotId, slotStart);
        slotCapacityPortDouble.setSlotSession(slotId, sessionId);
        slotCapacityPortDouble.setSlotCampaign(slotId, campaignId);

        // Citizen 1 books the only available slot
        Appointment appt1 = bookingService.bookAppointment(citizen1, campaignId, slotId);
        assertNotNull(appt1);

        // Session is full; Citizen 2 joins waitlist
        slotCapacityPortDouble.setSessionFull(sessionId, true);
        WaitlistEntry entry2 = joinService.joinWaitlist(citizen2, campaignId, sessionId, slotStart);
        assertEquals(WaitlistStatus.WAITING, entry2.getStatus());

        // Citizen 1 fails to check in; marked as NO_SHOW after window close
        Appointment noShowAppt = checkInService.markNoShow(appt1.getId(), afterWindowClose);
        assertEquals(AppointmentStatus.NO_SHOW, noShowAppt.getStatus());

        // Verify end-to-end: Citizen 2 was automatically PROMOTED
        WaitlistEntry updatedEntry2 = waitlistRepository.findById(entry2.getId()).orElseThrow();
        assertEquals(WaitlistStatus.PROMOTED, updatedEntry2.getStatus());

        List<Appointment> citizen2Appointments = appointmentRepository.findByCitizenIdAndCampaignIdAndStatusIn(
                citizen2, campaignId, List.of(AppointmentStatus.CONFIRMED));
        assertEquals(1, citizen2Appointments.size());
        assertEquals(slotId, citizen2Appointments.get(0).getSlotId());
    }

    @Test
    void getVaccinatorQueueForSlot_returnsCheckedInAppointments() {
        Long citizen1 = 1001L;
        Long citizen2 = 1002L;
        Long campaignId = 2001L;
        Long slotId = 4006L;
        Instant slotStart = Instant.parse("2026-10-01T10:00:00Z");
        Instant now = slotStart.plus(10, ChronoUnit.MINUTES);

        slotCapacityPortDouble.setAvailableCapacity(slotId, 5);
        slotCapacityPortDouble.setSlotStartTime(slotId, slotStart);

        Appointment appt1 = bookingService.bookAppointment(citizen1, campaignId, slotId);
        Appointment appt2 = bookingService.bookAppointment(citizen2, campaignId, slotId);

        checkInService.checkIn(appt1.getId(), now);

        List<Appointment> queue = checkInService.getVaccinatorQueueForSlot(slotId);
        assertEquals(1, queue.size());
        assertEquals(appt1.getId(), queue.get(0).getId());
        assertEquals(AppointmentStatus.CHECKED_IN, queue.get(0).getStatus());
    }

    @Test
    void getArrivalQueueForSlot_returnsConfirmedAndCheckedInAppointments() {
        Long campaignId = 2001L;
        Long slotId = 4010L;
        Instant slotStart = Instant.parse("2026-10-01T10:00:00Z");

        slotCapacityPortDouble.setAvailableCapacity(slotId, 5);
        slotCapacityPortDouble.setSlotStartTime(slotId, slotStart);

        Appointment confirmed = bookingService.bookAppointment(1003L, campaignId, slotId);
        Appointment checkedIn = bookingService.bookAppointment(1004L, campaignId, slotId);
        checkInService.checkIn(checkedIn.getId(), slotStart);

        List<Appointment> queue = checkInService.getArrivalQueueForSlot(slotId);

        assertEquals(2, queue.size());
        assertEquals(
                java.util.Set.of(confirmed.getId(), checkedIn.getId()),
                queue.stream().map(Appointment::getId).collect(java.util.stream.Collectors.toSet())
        );
    }
}
