package com.se2030.vaccinationportal.appointment.waitlist.service;

import com.se2030.vaccinationportal.appointment.testsupport.TestEligibilityLookupPortDouble;
import com.se2030.vaccinationportal.appointment.testsupport.TestSlotCapacityPortDouble;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.se2030.vaccinationportal.screening.api.eligibility.EligibilityStatus;
import com.se2030.vaccinationportal.appointment.persistence.model.Appointment;
import com.se2030.vaccinationportal.appointment.persistence.model.AppointmentStatus;
import com.se2030.vaccinationportal.appointment.persistence.model.WaitlistEntry;
import com.se2030.vaccinationportal.appointment.persistence.model.WaitlistStatus;
import com.se2030.vaccinationportal.appointment.persistence.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.persistence.repository.CitizenCampaignBookingGuardRepository;
import com.se2030.vaccinationportal.appointment.persistence.repository.WaitlistRepository;
import java.time.Instant;
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

/**
 * Regression tests for waitlist campaign/session integrity (R01).
 * A waitlist entry must never represent a campaign/session pairing that is not authoritative:
 * admission validates the session's authoritative campaign, and promotion revalidates
 * campaign/session/slot consistency so malformed historical entries can never create an
 * appointment or reserve capacity.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(WaitlistCampaignSessionIntegrityTests.TestConfig.class)
class WaitlistCampaignSessionIntegrityTests {

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
    private WaitlistJoinService joinService;

    @Autowired
    private WaitlistPromotionService promotionService;

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

    private static final Long CITIZEN_A = 1101L;
    private static final Long CITIZEN_B = 1102L;
    private static final Long CAMPAIGN_A = 2101L;
    private static final Long CAMPAIGN_B = 2102L;
    private static final Long SESSION_A = 3101L;
    private static final Long SESSION_B = 3102L;
    private static final Long SLOT_A = 4101L;
    private static final Long SLOT_B = 4102L;

    @BeforeEach
    void setUp() {
        waitlistRepository.deleteAllInBatch();
        appointmentRepository.deleteAllInBatch();
        guardRepository.deleteAllInBatch();
        eligibilityPortDouble.setDefaultStatus(EligibilityStatus.ELIGIBLE);
        slotCapacityPortDouble.setSimulateReleaseFailure(false);
        // Authoritative topology: session A belongs to campaign A (served by slot A),
        // session B belongs to campaign B (served by slot B)
        slotCapacityPortDouble.setSessionCampaign(SESSION_A, CAMPAIGN_A);
        slotCapacityPortDouble.setSessionCampaign(SESSION_B, CAMPAIGN_B);
        slotCapacityPortDouble.setSlotSession(SLOT_A, SESSION_A);
        slotCapacityPortDouble.setSlotCampaign(SLOT_A, CAMPAIGN_A);
        slotCapacityPortDouble.setSlotSession(SLOT_B, SESSION_B);
        slotCapacityPortDouble.setSlotCampaign(SLOT_B, CAMPAIGN_B);
    }

    @AfterEach
    void tearDown() {
        waitlistRepository.deleteAllInBatch();
        appointmentRepository.deleteAllInBatch();
        guardRepository.deleteAllInBatch();
        slotCapacityPortDouble.clearSessionCampaignMap();
        slotCapacityPortDouble.clearSlotSessionMap();
        slotCapacityPortDouble.clearSlotCampaignMap();
        slotCapacityPortDouble.clearBookabilityOverrides();
    }

    @Test
    void joinWaitlist_validCampaignAndMatchingSession_succeeds() {
        slotCapacityPortDouble.setSessionFull(SESSION_A, true);

        WaitlistEntry entry = joinService.joinWaitlist(CITIZEN_A, CAMPAIGN_A, SESSION_A, Instant.now());

        assertNotNull(entry);
        assertEquals(WaitlistStatus.WAITING, entry.getStatus());
        assertEquals(CAMPAIGN_A, entry.getCampaignId());
        assertEquals(SESSION_A, entry.getSessionId());
    }

    @Test
    void joinWaitlist_campaignSessionMismatch_isRejected() {
        slotCapacityPortDouble.setSessionFull(SESSION_B, true);
        eligibilityPortDouble.setEligibility(CITIZEN_A, CAMPAIGN_A, EligibilityStatus.ELIGIBLE);

        // Campaign A + session belonging to campaign B must be rejected
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> joinService.joinWaitlist(CITIZEN_A, CAMPAIGN_A, SESSION_B, Instant.now())
        );
        assertTrue(ex.getMessage().contains("does not belong to campaign"));
    }

    @Test
    void joinWaitlist_mismatchedPair_createsNoQueueRecord() {
        slotCapacityPortDouble.setSessionFull(SESSION_B, true);
        eligibilityPortDouble.setEligibility(CITIZEN_A, CAMPAIGN_A, EligibilityStatus.ELIGIBLE);

        assertThrows(
                IllegalArgumentException.class,
                () -> joinService.joinWaitlist(CITIZEN_A, CAMPAIGN_A, SESSION_B, Instant.now())
        );

        assertEquals(0, waitlistRepository.count(),
                "No waitlist entry may be created for a mismatched campaign/session pair");
        assertTrue(waitlistRepository.findByCitizenIdAndCampaignIdAndStatus(
                        CITIZEN_A, CAMPAIGN_A, WaitlistStatus.WAITING).isEmpty());
    }

    @Test
    void promotion_withSlotFromAnotherSession_isRejectedSafely() {
        slotCapacityPortDouble.setSessionFull(SESSION_A, true);
        WaitlistEntry entry = joinService.joinWaitlist(CITIZEN_A, CAMPAIGN_A, SESSION_A, Instant.now());
        slotCapacityPortDouble.setAvailableCapacity(SLOT_B, 1);

        // Slot B belongs to session B; promotion scoped to session A must not use it
        Optional<Appointment> promoted = promotionService.promoteNext(SESSION_A, SLOT_B, Instant.now());

        assertTrue(promoted.isEmpty(),
                "Promotion must never place a session-A waiter into a session-B slot");
        assertEquals(WaitlistStatus.WAITING, waitlistRepository.findById(entry.getId()).orElseThrow().getStatus());
        assertEquals(1, slotCapacityPortDouble.getAvailableCapacity(SLOT_B),
                "Rejected promotion must not reserve the foreign slot's capacity");
    }

    @Test
    void promotion_malformedEntry_isSkippedWithoutAppointmentOrReservation() {
        // Simulate malformed historical data: entry claims campaign B but sits on campaign A's session
        WaitlistEntry malformed = new WaitlistEntry(CITIZEN_A, CAMPAIGN_B, SESSION_A,
                WaitlistStatus.WAITING, Instant.now());
        malformed = waitlistRepository.save(malformed);
        slotCapacityPortDouble.setAvailableCapacity(SLOT_A, 1);

        Optional<Appointment> promoted = promotionService.promoteNext(SESSION_A, SLOT_A, Instant.now());

        assertTrue(promoted.isEmpty(),
                "Malformed queue entry must not be promoted into an appointment");
        assertEquals(0, appointmentRepository.count(),
                "Malformed promotion must produce no appointment");
        assertEquals(1, slotCapacityPortDouble.getAvailableCapacity(SLOT_A),
                "Malformed promotion must not reserve capacity");
        assertEquals(WaitlistStatus.SKIPPED_MALFORMED, waitlistRepository.findById(malformed.getId()).orElseThrow().getStatus(),
                "Malformed entry is transitioned to SKIPPED_MALFORMED to release active uniqueness and queue position");
    }

    @Test
    void promotion_malformedEntryDoesNotBlockValidWaiters_fifoPreserved() {
        // Malformed entry joined first (FIFO head), valid entry second
        WaitlistEntry malformed = waitlistRepository.save(new WaitlistEntry(
                CITIZEN_A, CAMPAIGN_B, SESSION_A, WaitlistStatus.WAITING, Instant.now().minusSeconds(60)));
        eligibilityPortDouble.setEligibility(CITIZEN_B, CAMPAIGN_A, EligibilityStatus.ELIGIBLE);
        WaitlistEntry valid = joinService.joinWaitlist(CITIZEN_B, CAMPAIGN_A, SESSION_A, Instant.now());
        slotCapacityPortDouble.setAvailableCapacity(SLOT_A, 1);

        Optional<Appointment> promoted = promotionService.promoteNext(SESSION_A, SLOT_A, Instant.now());

        assertTrue(promoted.isPresent(), "Valid waiter behind a malformed entry must still be promoted");
        assertEquals(CITIZEN_B, promoted.get().getCitizenId());
        assertEquals(CAMPAIGN_A, promoted.get().getCampaignId());
        assertEquals(SLOT_A, promoted.get().getSlotId());
        assertEquals(AppointmentStatus.CONFIRMED, promoted.get().getStatus());
        assertEquals(WaitlistStatus.PROMOTED, waitlistRepository.findById(valid.getId()).orElseThrow().getStatus());
        assertEquals(WaitlistStatus.SKIPPED_MALFORMED, waitlistRepository.findById(malformed.getId()).orElseThrow().getStatus(),
                "Malformed entry must be marked SKIPPED_MALFORMED so it releases active uniqueness");
        assertEquals(0, slotCapacityPortDouble.getAvailableCapacity(SLOT_A));
    }

    @Test
    void promotion_malformedEntryTransition_releasesActiveCampaignUniquenessAndQueuePosition() {
        // Malformed entry for CITIZEN_A on CAMPAIGN_B
        WaitlistEntry malformed = waitlistRepository.save(new WaitlistEntry(
                CITIZEN_A, CAMPAIGN_B, SESSION_A, WaitlistStatus.WAITING, Instant.now().minusSeconds(60)));

        slotCapacityPortDouble.setAvailableCapacity(SLOT_A, 1);
        promotionService.promoteNext(SESSION_A, SLOT_A, Instant.now());

        // Malformed entry transitioned to SKIPPED_MALFORMED
        assertEquals(WaitlistStatus.SKIPPED_MALFORMED, waitlistRepository.findById(malformed.getId()).orElseThrow().getStatus());

        // Active lookup returns empty, meaning citizen A is no longer blocked from joining campaign B
        assertTrue(waitlistRepository.findByCitizenIdAndCampaignIdAndStatus(CITIZEN_A, CAMPAIGN_B, WaitlistStatus.WAITING).isEmpty());

        // Queue position query does not count SKIPPED_MALFORMED entries
        long queuePos = waitlistRepository.countQueuePosition(SESSION_A, WaitlistStatus.WAITING, Instant.now(), 9999L);
        assertEquals(0, queuePos, "SKIPPED_MALFORMED entry must not inflate queue position");
    }

    @Test
    void promotion_validEntry_stillSucceedsAndSessionScopedPromotionRemainsIntact() {
        eligibilityPortDouble.setEligibility(CITIZEN_A, CAMPAIGN_A, EligibilityStatus.ELIGIBLE);
        slotCapacityPortDouble.setSessionFull(SESSION_A, true);
        WaitlistEntry entry = joinService.joinWaitlist(CITIZEN_A, CAMPAIGN_A, SESSION_A, Instant.now());
        slotCapacityPortDouble.setAvailableCapacity(SLOT_A, 1);

        Optional<Appointment> promoted = promotionService.promoteNextForSlot(CAMPAIGN_A, SLOT_A, Instant.now());

        assertTrue(promoted.isPresent(), "Valid promotion must still succeed");
        assertEquals(AppointmentStatus.CONFIRMED, promoted.get().getStatus());
        assertEquals(WaitlistStatus.PROMOTED, waitlistRepository.findById(entry.getId()).orElseThrow().getStatus());
    }

    @Test
    void promotion_slotCampaignMismatchRequest_returnsEmptyAndPromotesNobody() {
        eligibilityPortDouble.setEligibility(CITIZEN_A, CAMPAIGN_A, EligibilityStatus.ELIGIBLE);
        slotCapacityPortDouble.setSessionFull(SESSION_A, true);
        WaitlistEntry entry = joinService.joinWaitlist(CITIZEN_A, CAMPAIGN_A, SESSION_A, Instant.now());
        slotCapacityPortDouble.setAvailableCapacity(SLOT_A, 1);

        // Caller supplies a campaign that does not own the slot (F03 integrity preserved)
        Optional<Appointment> promoted = promotionService.promoteNextForSlot(CAMPAIGN_B, SLOT_A, Instant.now());

        assertTrue(promoted.isEmpty());
        assertEquals(WaitlistStatus.WAITING, waitlistRepository.findById(entry.getId()).orElseThrow().getStatus());
        assertEquals(1, slotCapacityPortDouble.getAvailableCapacity(SLOT_A));
    }

    @Test
    void promotion_malformedEntries_neverCreateCrossCampaignAppointments() {
        // Both malformed rows target campaign B while sitting on campaign A's session queue
        waitlistRepository.save(new WaitlistEntry(
                CITIZEN_A, CAMPAIGN_B, SESSION_A, WaitlistStatus.WAITING, Instant.now().minusSeconds(60)));
        waitlistRepository.save(new WaitlistEntry(
                CITIZEN_B, CAMPAIGN_B, SESSION_A, WaitlistStatus.WAITING, Instant.now()));
        slotCapacityPortDouble.setAvailableCapacity(SLOT_A, 2);

        Optional<Appointment> promoted = promotionService.promoteNext(SESSION_A, SLOT_A, Instant.now());

        assertTrue(promoted.isEmpty());
        List<Appointment> all = appointmentRepository.findAll();
        assertTrue(all.isEmpty(), "No appointment may exist after malformed promotion attempts");
        assertEquals(2, slotCapacityPortDouble.getAvailableCapacity(SLOT_A),
                "Capacity must be exactly untouched after malformed promotion attempts");
    }
}
