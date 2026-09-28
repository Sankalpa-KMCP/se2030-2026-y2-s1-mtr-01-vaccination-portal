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
        slotCapacityPortDouble.clearSlotCampaignMap();
        slotCapacityPortDouble.clearSlotSessionMap();
        slotCapacityPortDouble.setDefaultSessionId(null);
        // Authoritative campaign/session relationships for R01 admission/promotion validation
        slotCapacityPortDouble.setSessionCampaign(3001L, 2001L);
        slotCapacityPortDouble.setSessionCampaign(3002L, 2001L);
    }

    @AfterEach
    void tearDown() {
        waitlistRepository.deleteAllInBatch();
        appointmentRepository.deleteAllInBatch();
        guardRepository.deleteAllInBatch();
        slotCapacityPortDouble.clearSlotCampaignMap();
        slotCapacityPortDouble.clearSlotSessionMap();
        slotCapacityPortDouble.setDefaultSessionId(null);
        slotCapacityPortDouble.clearSessionCampaignMap();
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
        slotCapacityPortDouble.setSlotSession(slotId, sessionId);

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
        slotCapacityPortDouble.setSlotSession(slotId, sessionId);

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
        slotCapacityPortDouble.setSlotSession(slotId, sessionId);

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
        slotCapacityPortDouble.setSlotSession(slotId, sessionId);
        slotCapacityPortDouble.setSlotCampaign(slotId, campaignId);

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

    @Test
    void promoteNextForSlot_promotesSameSessionCandidate() {
        Long citizenId = 1001L;
        Long campaignId = 2001L;
        Long sessionId = 3001L;
        Long slotId = 4001L;
        Instant now = Instant.parse("2026-10-01T10:00:00Z");

        eligibilityPortDouble.setEligibility(citizenId, campaignId, EligibilityStatus.ELIGIBLE);
        slotCapacityPortDouble.setAvailableCapacity(slotId, 1);
        slotCapacityPortDouble.setSessionFull(sessionId, true);
        slotCapacityPortDouble.setSlotSession(slotId, sessionId);
        slotCapacityPortDouble.setSlotCampaign(slotId, campaignId);

        WaitlistEntry entry = joinService.joinWaitlist(citizenId, campaignId, sessionId, now);
        assertNotNull(entry);

        Optional<Appointment> promoted = promotionService.promoteNextForSlot(campaignId, slotId, now);

        assertTrue(promoted.isPresent());
        assertEquals(citizenId, promoted.get().getCitizenId());
        assertEquals(campaignId, promoted.get().getCampaignId());
        assertEquals(slotId, promoted.get().getSlotId());
        assertEquals(AppointmentStatus.CONFIRMED, promoted.get().getStatus());

        WaitlistEntry updatedEntry = waitlistRepository.findById(entry.getId()).orElseThrow();
        assertEquals(WaitlistStatus.PROMOTED, updatedEntry.getStatus());
    }

    @Test
    void promoteNextForSlot_ignoresOlderCandidateFromAnotherSessionInSameCampaign() {
        // Finding F04: Candidate in Session 1 arrived at t0. Candidate in Session 2 arrived at t1 (t1 > t0).
        // Slot freed belongs to Session 2. Candidate from Session 2 must be promoted; candidate from Session 1 ignored.
        Long citizenSession1 = 1001L;
        Long citizenSession2 = 1002L;
        Long campaignId = 2001L;
        Long session1 = 3001L;
        Long session2 = 3002L;
        Long slotSession2 = 4002L;

        Instant t0 = Instant.parse("2026-10-01T09:00:00Z");
        Instant t1 = Instant.parse("2026-10-01T10:00:00Z");

        eligibilityPortDouble.setEligibility(citizenSession1, campaignId, EligibilityStatus.ELIGIBLE);
        eligibilityPortDouble.setEligibility(citizenSession2, campaignId, EligibilityStatus.ELIGIBLE);

        slotCapacityPortDouble.setSessionFull(session1, true);
        slotCapacityPortDouble.setSessionFull(session2, true);
        slotCapacityPortDouble.setAvailableCapacity(slotSession2, 1);
        slotCapacityPortDouble.setSlotSession(slotSession2, session2);
        slotCapacityPortDouble.setSlotCampaign(slotSession2, campaignId);

        // Citizen 1 joins session 1 earlier (t0)
        WaitlistEntry entrySession1 = joinService.joinWaitlist(citizenSession1, campaignId, session1, t0);
        // Citizen 2 joins session 2 later (t1)
        WaitlistEntry entrySession2 = joinService.joinWaitlist(citizenSession2, campaignId, session2, t1);

        // Promotion triggered for slotSession2 (which belongs to session2)
        Optional<Appointment> promoted = promotionService.promoteNextForSlot(campaignId, slotSession2, t1);

        assertTrue(promoted.isPresent(), "Candidate for session 2 must be promoted into session 2's slot");
        assertEquals(citizenSession2, promoted.get().getCitizenId(), "Must promote session 2 candidate, NOT older session 1 candidate");
        assertEquals(slotSession2, promoted.get().getSlotId());

        // Verify entry statuses
        assertEquals(WaitlistStatus.WAITING, waitlistRepository.findById(entrySession1.getId()).orElseThrow().getStatus(),
                "Candidate from session 1 must remain WAITING");
        assertEquals(WaitlistStatus.PROMOTED, waitlistRepository.findById(entrySession2.getId()).orElseThrow().getStatus(),
                "Candidate from session 2 must be PROMOTED");
    }

    @Test
    void promoteNextForSlot_fifoOrderingWithinSameSession() {
        Long citizen1 = 1001L;
        Long citizen2 = 1002L;
        Long campaignId = 2001L;
        Long sessionId = 3001L;
        Long slotId = 4001L;

        Instant t0 = Instant.parse("2026-10-01T10:00:00Z");
        Instant t1 = t0.plus(10, ChronoUnit.MINUTES);

        slotCapacityPortDouble.setSessionFull(sessionId, true);
        slotCapacityPortDouble.setAvailableCapacity(slotId, 1);
        slotCapacityPortDouble.setSlotSession(slotId, sessionId);
        slotCapacityPortDouble.setSlotCampaign(slotId, campaignId);

        WaitlistEntry entry1 = joinService.joinWaitlist(citizen1, campaignId, sessionId, t0);
        WaitlistEntry entry2 = joinService.joinWaitlist(citizen2, campaignId, sessionId, t1);

        Optional<Appointment> promoted = promotionService.promoteNextForSlot(campaignId, slotId, t1);

        assertTrue(promoted.isPresent());
        assertEquals(citizen1, promoted.get().getCitizenId(), "Earliest waiter within session must be promoted first (FIFO)");

        assertEquals(WaitlistStatus.PROMOTED, waitlistRepository.findById(entry1.getId()).orElseThrow().getStatus());
        assertEquals(WaitlistStatus.WAITING, waitlistRepository.findById(entry2.getId()).orElseThrow().getStatus());
    }

    @Test
    void promoteNextForSlot_slotCampaignMismatch_returnsEmptyAndPromotesNobody() {
        Long citizenId = 1001L;
        Long campaignA = 2001L;
        Long campaignB = 2002L;
        Long sessionId = 3001L;
        Long slotId = 4001L;
        Instant now = Instant.parse("2026-10-01T10:00:00Z");

        eligibilityPortDouble.setEligibility(citizenId, campaignA, EligibilityStatus.ELIGIBLE);
        slotCapacityPortDouble.setSessionFull(sessionId, true);
        slotCapacityPortDouble.setAvailableCapacity(slotId, 1);
        slotCapacityPortDouble.setSlotSession(slotId, sessionId);
        // Slot belongs to campaignA
        slotCapacityPortDouble.setSlotCampaign(slotId, campaignA);

        WaitlistEntry entry = joinService.joinWaitlist(citizenId, campaignA, sessionId, now);

        // Attempt promotion supplying mismatched campaignB
        Optional<Appointment> promoted = promotionService.promoteNextForSlot(campaignB, slotId, now);

        assertTrue(promoted.isEmpty(), "Mismatched campaign must reject promotion safely");
        assertEquals(WaitlistStatus.WAITING, waitlistRepository.findById(entry.getId()).orElseThrow().getStatus(),
                "Waitlist entry must remain untouched in WAITING status");
    }

    @Test
    void promoteNextForSlot_unresolvableSlot_returnsEmptyAndPromotesNobody() {
        Long citizenId = 1001L;
        Long campaignId = 2001L;
        Long sessionId = 3001L;
        Long unknownSlotId = 99999L;
        Instant now = Instant.parse("2026-10-01T10:00:00Z");

        eligibilityPortDouble.setEligibility(citizenId, campaignId, EligibilityStatus.ELIGIBLE);
        slotCapacityPortDouble.setSessionFull(sessionId, true);

        WaitlistEntry entry = joinService.joinWaitlist(citizenId, campaignId, sessionId, now);

        // Slot not mapped to any session in port double
        Optional<Appointment> promoted = promotionService.promoteNextForSlot(campaignId, unknownSlotId, now);

        assertTrue(promoted.isEmpty(), "Unresolvable slot must return empty without promoting anyone");
        assertEquals(WaitlistStatus.WAITING, waitlistRepository.findById(entry.getId()).orElseThrow().getStatus());
    }

    @Test
    void promoteNextForSlot_emptyWaitlistForSession_returnsEmpty() {
        Long campaignId = 2001L;
        Long sessionId = 3001L;
        Long slotId = 4001L;
        Instant now = Instant.parse("2026-10-01T10:00:00Z");

        slotCapacityPortDouble.setAvailableCapacity(slotId, 1);
        slotCapacityPortDouble.setSlotSession(slotId, sessionId);
        slotCapacityPortDouble.setSlotCampaign(slotId, campaignId);

        Optional<Appointment> promoted = promotionService.promoteNextForSlot(campaignId, slotId, now);

        assertTrue(promoted.isEmpty(), "Promotion on empty session waitlist must return empty Optional");
    }

    @Test
    void promoteNextForSlot_nullInputs_returnsEmptySafely() {
        Instant now = Instant.now();
        assertTrue(promotionService.promoteNextForSlot(null, 4001L, now).isEmpty());
        assertTrue(promotionService.promoteNextForSlot(2001L, null, now).isEmpty());
        assertTrue(promotionService.promoteNextForSlot(null, null, now).isEmpty());
    }
}
