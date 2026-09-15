package com.se2030.vaccinationportal.appointment.service;

import com.se2030.vaccinationportal.appointment.dependency.EligibilityStatus;
import com.se2030.vaccinationportal.appointment.model.Appointment;
import com.se2030.vaccinationportal.appointment.model.AppointmentStatus;
import com.se2030.vaccinationportal.appointment.model.WaitlistEntry;
import com.se2030.vaccinationportal.appointment.model.WaitlistStatus;
import com.se2030.vaccinationportal.appointment.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.repository.CitizenCampaignBookingGuardRepository;
import com.se2030.vaccinationportal.appointment.repository.WaitlistRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit and integration tests for {@link WaitlistPromotionService}.
 * Verifies automatic promotion when capacity opens (MF03-BR-07 / MF03-BR-08), FIFO order preservation,
 * skipping of ineligible/duplicate waiters, no-op on empty waitlist, and end-to-end promotion triggered by appointment cancellation.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(WaitlistPromotionServiceTests.TestConfig.class)
class WaitlistPromotionServiceTests {

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
    private WaitlistPromotionService promotionService;

    @Autowired
    private WaitlistJoinService joinService;

    @Autowired
    private AppointmentCancellationService cancellationService;

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
        slotCapacityPortDouble.setSimulateReleaseFailure(false);
    }

    @AfterEach
    void tearDown() {
        waitlistRepository.deleteAllInBatch();
        appointmentRepository.deleteAllInBatch();
        guardRepository.deleteAllInBatch();
    }

    @Test
    void promoteNext_singleWaitingEntry_promotesToConfirmedAppointment() {
        Long citizenId = 1001L;
        Long campaignId = 2001L;
        Long sessionId = 3001L;
        Long slotId = 4001L;
        Instant now = Instant.parse("2026-10-01T10:00:00Z");

        eligibilityPortDouble.setEligibility(citizenId, campaignId, EligibilityStatus.ELIGIBLE);
        slotCapacityPortDouble.setAvailableCapacity(slotId, 1);
        slotCapacityPortDouble.setSessionFull(sessionId, true);

        WaitlistEntry entry = joinService.joinWaitlist(citizenId, campaignId, sessionId, now);
        assertNotNull(entry);

        // Promote waiter into slotId
        Optional<Appointment> promoted = promotionService.promoteNext(sessionId, slotId, now);

        assertTrue(promoted.isPresent());
        assertEquals(citizenId, promoted.get().getCitizenId());
        assertEquals(campaignId, promoted.get().getCampaignId());
        assertEquals(slotId, promoted.get().getSlotId());
        assertEquals(AppointmentStatus.CONFIRMED, promoted.get().getStatus());

        // Verify waitlist entry status updated to PROMOTED
        WaitlistEntry updatedEntry = waitlistRepository.findById(entry.getId()).orElseThrow();
        assertEquals(WaitlistStatus.PROMOTED, updatedEntry.getStatus());
    }

    @Test
    void promoteNext_multipleWaiters_respectsFifoOrder() {
        Long citizen1 = 1001L;
        Long citizen2 = 1002L;
        Long citizen3 = 1003L;
        Long campaignId = 2001L;
        Long sessionId = 3001L;
        Long slotId = 4001L;

        Instant t0 = Instant.parse("2026-10-01T10:00:00Z");
        Instant t1 = t0.plus(10, ChronoUnit.MINUTES);
        Instant t2 = t0.plus(20, ChronoUnit.MINUTES);

        slotCapacityPortDouble.setSessionFull(sessionId, true);
        slotCapacityPortDouble.setAvailableCapacity(slotId, 1);

        WaitlistEntry entry1 = joinService.joinWaitlist(citizen1, campaignId, sessionId, t0);
        WaitlistEntry entry2 = joinService.joinWaitlist(citizen2, campaignId, sessionId, t1);
        WaitlistEntry entry3 = joinService.joinWaitlist(citizen3, campaignId, sessionId, t2);

        // Promote single available capacity
        Optional<Appointment> promoted = promotionService.promoteNext(sessionId, slotId, t2);

        assertTrue(promoted.isPresent());
        assertEquals(citizen1, promoted.get().getCitizenId(), "Earliest waiter (FIFO) must be promoted first");

        // Verify statuses
        assertEquals(WaitlistStatus.PROMOTED, waitlistRepository.findById(entry1.getId()).orElseThrow().getStatus());
        assertEquals(WaitlistStatus.WAITING, waitlistRepository.findById(entry2.getId()).orElseThrow().getStatus());
        assertEquals(WaitlistStatus.WAITING, waitlistRepository.findById(entry3.getId()).orElseThrow().getStatus());
    }

    @Test
    void promoteNext_ineligibleWaiter_skipsAndPromotesNextEligibleWaiter() {
        Long citizenIneligible = 1001L;
        Long citizenEligible = 1002L;
        Long campaignId = 2001L;
        Long sessionId = 3001L;
        Long slotId = 4001L;

        Instant t0 = Instant.parse("2026-10-01T10:00:00Z");
        Instant t1 = t0.plus(10, ChronoUnit.MINUTES);

        slotCapacityPortDouble.setSessionFull(sessionId, true);
        slotCapacityPortDouble.setAvailableCapacity(slotId, 1);

        // Both join while ELIGIBLE
        WaitlistEntry entry1 = joinService.joinWaitlist(citizenIneligible, campaignId, sessionId, t0);
        WaitlistEntry entry2 = joinService.joinWaitlist(citizenEligible, campaignId, sessionId, t1);

        // Citizen 1 eligibility later expires/changes to NOT_ELIGIBLE
        eligibilityPortDouble.setEligibility(citizenIneligible, campaignId, EligibilityStatus.NOT_ELIGIBLE);

        Optional<Appointment> promoted = promotionService.promoteNext(sessionId, slotId, t1);

        assertTrue(promoted.isPresent());
        assertEquals(citizenEligible, promoted.get().getCitizenId(), "Must skip ineligible candidate and promote next eligible candidate");

        // Verify entry1 marked SKIPPED_INELIGIBLE and entry2 marked PROMOTED
        assertEquals(WaitlistStatus.SKIPPED_INELIGIBLE, waitlistRepository.findById(entry1.getId()).orElseThrow().getStatus());
        assertEquals(WaitlistStatus.PROMOTED, waitlistRepository.findById(entry2.getId()).orElseThrow().getStatus());
    }

    @Test
    void promoteNext_emptyWaitlist_returnsEmptyOptional() {
        Long sessionId = 3001L;
        Long slotId = 4001L;
        Instant now = Instant.now();

        Optional<Appointment> result = promotionService.promoteNext(sessionId, slotId, now);

        assertTrue(result.isEmpty());
    }

    @Test
    void cancellationTriggersPromotion_endToEndIntegration() {
        Long citizenAppt = 1001L;
        Long citizenWait = 1002L;
        Long campaignId = 2001L;
        Long sessionId = 3001L;
        Long slotId = 4001L;

        Instant now = Instant.parse("2026-10-01T10:00:00Z");
        Instant slotStart = now.plus(48, ChronoUnit.HOURS);

        slotCapacityPortDouble.setAvailableCapacity(slotId, 1);
        slotCapacityPortDouble.setSlotStartTime(slotId, slotStart);

        // Citizen 1 books the only available slot
        Appointment appt = bookingService.bookAppointment(citizenAppt, campaignId, slotId);
        assertNotNull(appt);

        // Session is now full; Citizen 2 joins waitlist
        slotCapacityPortDouble.setSessionFull(sessionId, true);
        WaitlistEntry entry = joinService.joinWaitlist(citizenWait, campaignId, sessionId, now);
        assertEquals(WaitlistStatus.WAITING, entry.getStatus());

        // Citizen 1 cancels appointment outside 24h window
        Appointment cancelled = cancellationService.cancelAppointment(appt.getId(), citizenAppt, now);
        assertEquals(AppointmentStatus.CANCELLED, cancelled.getStatus());

        // Verify end-to-end: Citizen 2's waitlist entry was automatically PROMOTED
        WaitlistEntry updatedEntry = waitlistRepository.findById(entry.getId()).orElseThrow();
        assertEquals(WaitlistStatus.PROMOTED, updatedEntry.getStatus());

        // Verify new CONFIRMED appointment exists for Citizen 2
        List<Appointment> citizen2Appointments = appointmentRepository.findByCitizenIdAndCampaignIdAndStatusIn(
                citizenWait, campaignId, List.of(AppointmentStatus.CONFIRMED));
        assertEquals(1, citizen2Appointments.size());
        assertEquals(slotId, citizen2Appointments.get(0).getSlotId());
    }
}
