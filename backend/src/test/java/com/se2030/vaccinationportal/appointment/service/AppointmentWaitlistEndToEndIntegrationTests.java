package com.se2030.vaccinationportal.appointment.service;

import com.se2030.vaccinationportal.appointment.dependency.EligibilityStatus;
import com.se2030.vaccinationportal.appointment.exception.DuplicateWaitlistEntryException;
import com.se2030.vaccinationportal.appointment.exception.SlotFullException;
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
 * End-to-end integration test for the full MF-03 citizen journey.
 *
 * <p>Proves that all MF-03 services compose correctly using real Spring-wired beans and an
 * in-memory H2 test database (no mocks). Exercises the complete narrative from booking through
 * waitlist promotion to check-in (primary path) and no-show with capacity release (secondary path).
 *
 * <p>Business rules exercised per test:
 * MF03-BR-01, MF03-BR-02, MF03-BR-03, MF03-BR-04, MF03-BR-05, MF03-BR-06,
 * MF03-BR-07, MF03-BR-08, MF03-BR-09, MF03-BR-10, MF03-BR-14.
 *
 * <p>Follows the same {@code @SpringBootTest} + {@code @TestConfiguration} port-double injection
 * pattern used by all other service-level integration tests in this package.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(AppointmentWaitlistEndToEndIntegrationTests.TestConfig.class)
class AppointmentWaitlistEndToEndIntegrationTests {

    // -------------------------------------------------------------------------
    // Test fixture constants — unique IDs avoid cross-test interference
    // -------------------------------------------------------------------------
    private static final Long CITIZEN_A   = 7001L;
    private static final Long CITIZEN_B   = 7002L;
    private static final Long CAMPAIGN_ID = 8001L;
    private static final Long SESSION_ID  = 9001L;
    private static final Long SLOT_ID     = 5001L;

    /** Anchor slot start used for all time-window calculations. */
    private static final Instant SLOT_START = Instant.parse("2026-12-01T10:00:00Z");

    /** 48 h before slot start — safely outside the 24-hour citizen cancellation window. */
    private static final Instant BEFORE_24H_WINDOW = SLOT_START.minus(48, ChronoUnit.HOURS);

    /** Exactly at slot start — inside the +/-30-minute check-in window. */
    private static final Instant AT_SLOT_START = SLOT_START;

    /** 45 minutes after slot start — past the check-in window close, valid for no-show marking. */
    private static final Instant AFTER_CHECKIN_WINDOW = SLOT_START.plus(45, ChronoUnit.MINUTES);

    // -------------------------------------------------------------------------
    // Port doubles registered as Spring beans for this test context
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Autowired collaborators
    // -------------------------------------------------------------------------

    @Autowired private AppointmentBookingService      bookingService;
    @Autowired private AppointmentCancellationService cancellationService;
    @Autowired private CheckInService                 checkInService;
    @Autowired private WaitlistJoinService            joinService;
    @Autowired private WaitlistLeaveService           leaveService;

    @Autowired private AppointmentRepository                 appointmentRepository;
    @Autowired private WaitlistRepository                    waitlistRepository;
    @Autowired private CitizenCampaignBookingGuardRepository guardRepository;

    @Autowired private TestEligibilityLookupPortDouble eligibilityPortDouble;
    @Autowired private TestSlotCapacityPortDouble      slotCapacityPortDouble;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @BeforeEach
    void setUp() {
        // Matches the cleanup pattern of all other service integration tests
        waitlistRepository.deleteAllInBatch();
        appointmentRepository.deleteAllInBatch();
        guardRepository.deleteAllInBatch();

        // Both citizens are ELIGIBLE
        eligibilityPortDouble.setDefaultStatus(EligibilityStatus.ELIGIBLE);
        eligibilityPortDouble.setEligibility(CITIZEN_A, CAMPAIGN_ID, EligibilityStatus.ELIGIBLE);
        eligibilityPortDouble.setEligibility(CITIZEN_B, CAMPAIGN_ID, EligibilityStatus.ELIGIBLE);

        // Session/slot starts with capacity for exactly ONE appointment
        slotCapacityPortDouble.setAvailableCapacity(SLOT_ID, 1);
        slotCapacityPortDouble.setSlotStartTime(SLOT_ID, SLOT_START);
        // Session is not yet full — citizenA may book
        slotCapacityPortDouble.setSessionFull(SESSION_ID, false);
        slotCapacityPortDouble.setSimulateReleaseFailure(false);
        slotCapacityPortDouble.setSlotSession(SLOT_ID, SESSION_ID);
        slotCapacityPortDouble.setSlotCampaign(SLOT_ID, CAMPAIGN_ID);
        // Authoritative campaign/session relationship for R01 admission/promotion validation
        slotCapacityPortDouble.setSessionCampaign(SESSION_ID, CAMPAIGN_ID);
        slotCapacityPortDouble.setSessionCampaign(9002L, CAMPAIGN_ID);
    }

    @AfterEach
    void tearDown() {
        waitlistRepository.deleteAllInBatch();
        appointmentRepository.deleteAllInBatch();
        guardRepository.deleteAllInBatch();
    }

    // -------------------------------------------------------------------------
    // Test 1 — Primary path: book → waitlist → cancel → promote → check-in
    // -------------------------------------------------------------------------

    /**
     * Walks the complete happy-path MF-03 narrative in one connected method:
     * <ol>
     *   <li>(a) citizenA books the last slot &rarr; CONFIRMED, capacity 0           (MF03-BR-01, MF03-BR-02, MF03-BR-03)
     *   <li>(b) citizenB tries to book the full slot &rarr; SlotFullException        (MF03-BR-03)
     *   <li>(c) citizenB joins waitlist &rarr; WaitlistEntry WAITING                 (MF03-BR-01, MF03-BR-06, MF03-BR-14)
     *   <li>(d) citizenA cancels outside 24h window &rarr; Appointment CANCELLED     (MF03-BR-04, MF03-BR-05)
     *   <li>(e) Promotion fires automatically &rarr; citizenB PROMOTED + CONFIRMED   (MF03-BR-07, MF03-BR-08)
     *   <li>(f) citizenB checks in inside the 30-min window &rarr; CHECKED_IN        (MF03-BR-09, MF03-BR-10)
     *   <li>(g) Vaccinator queue contains citizenB's CHECKED_IN appointment
     * </ol>
     */
    @Test
    void fullCitizenJourney_bookWaitlistPromoteCheckIn() {

        // ---------------------------------------------------------------
        // (a) citizenA books the last available slot
        // ---------------------------------------------------------------
        Appointment apptA = bookingService.bookAppointment(CITIZEN_A, CAMPAIGN_ID, SLOT_ID);

        assertNotNull(apptA.getId(),
                "citizenA's appointment must be persisted with a DB-generated ID");
        assertEquals(CITIZEN_A, apptA.getCitizenId());
        assertEquals(CAMPAIGN_ID, apptA.getCampaignId());
        assertEquals(SLOT_ID, apptA.getSlotId());
        assertEquals(AppointmentStatus.CONFIRMED, apptA.getStatus(),
                "citizenA's appointment must be CONFIRMED after booking the last slot");

        // The double's internal counter is now 0; mark session full for citizenB's perspective
        slotCapacityPortDouble.setSessionFull(SESSION_ID, true);

        // ---------------------------------------------------------------
        // (b) citizenB attempts to book the same (now-full) slot → rejected
        // ---------------------------------------------------------------
        assertThrows(
                SlotFullException.class,
                () -> bookingService.bookAppointment(CITIZEN_B, CAMPAIGN_ID, SLOT_ID),
                "citizenB must receive SlotFullException when slot capacity is exhausted");

        List<Appointment> citizenBApptsAfterFailedBook =
                appointmentRepository.findByCitizenIdAndCampaignIdAndStatusIn(
                        CITIZEN_B, CAMPAIGN_ID,
                        List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.CHECKED_IN));
        assertTrue(citizenBApptsAfterFailedBook.isEmpty(),
                "citizenB must have no active appointment after the failed booking attempt");

        // ---------------------------------------------------------------
        // (c) citizenB joins the waitlist
        // ---------------------------------------------------------------
        WaitlistEntry entryB = joinService.joinWaitlist(
                CITIZEN_B, CAMPAIGN_ID, SESSION_ID, BEFORE_24H_WINDOW);

        assertNotNull(entryB.getId(), "citizenB's waitlist entry must be persisted");
        assertEquals(CITIZEN_B, entryB.getCitizenId());
        assertEquals(CAMPAIGN_ID, entryB.getCampaignId());
        assertEquals(SESSION_ID, entryB.getSessionId());
        assertEquals(WaitlistStatus.WAITING, entryB.getStatus(),
                "citizenB's waitlist entry must be in WAITING status immediately after joining");

        // ---------------------------------------------------------------
        // (d) citizenA cancels outside the 24-hour window.
        // Internally: releaseCapacity(slotId) brings counter to 1, then
        // promoteNextForSlot immediately re-reserves it (counter goes back to 0).
        // We do NOT assert intermediate capacity here — step (e) proves the full
        // release-then-reserve cycle was correct.
        // ---------------------------------------------------------------
        Appointment cancelledA = cancellationService.cancelAppointment(
                apptA.getId(), CITIZEN_A, BEFORE_24H_WINDOW);

        assertEquals(AppointmentStatus.CANCELLED, cancelledA.getStatus(),
                "citizenA's appointment must transition to CANCELLED");
        assertNotNull(cancelledA.getCancelledAt(),
                "Cancelled appointment must carry a non-null cancellation timestamp");

        // ---------------------------------------------------------------
        // (e) Promotion must have fired automatically inside cancelAppointment
        // ---------------------------------------------------------------
        WaitlistEntry refreshedEntryB = waitlistRepository.findById(entryB.getId())
                .orElseThrow(() -> new AssertionError(
                        "citizenB's waitlist entry must still exist in the DB after promotion"));

        assertEquals(WaitlistStatus.PROMOTED, refreshedEntryB.getStatus(),
                "citizenB's waitlist entry must be automatically PROMOTED when capacity is freed");

        List<Appointment> promotedAppts = appointmentRepository.findByCitizenIdAndCampaignIdAndStatusIn(
                CITIZEN_B, CAMPAIGN_ID, List.of(AppointmentStatus.CONFIRMED));
        assertEquals(1, promotedAppts.size(),
                "Exactly one CONFIRMED appointment must be created for citizenB upon promotion");

        Appointment promotedApptB = promotedAppts.get(0);
        assertEquals(CITIZEN_B, promotedApptB.getCitizenId());
        assertEquals(SLOT_ID, promotedApptB.getSlotId(),
                "citizenB's promoted appointment must target the slot freed by citizenA's cancellation");

        // ---------------------------------------------------------------
        // (f) citizenB checks in within the valid +/-30-minute window
        // ---------------------------------------------------------------
        Appointment checkedInB = checkInService.checkIn(promotedApptB.getId(), AT_SLOT_START);

        assertEquals(AppointmentStatus.CHECKED_IN, checkedInB.getStatus(),
                "citizenB's appointment must be CHECKED_IN after check-in within the valid window");

        // ---------------------------------------------------------------
        // (g) Vaccinator queue must return citizenB's CHECKED_IN appointment
        // ---------------------------------------------------------------
        List<Appointment> queue = checkInService.getVaccinatorQueueForSlot(SLOT_ID);

        assertEquals(1, queue.size(),
                "Vaccinator queue for the slot must contain exactly one entry");
        assertEquals(promotedApptB.getId(), queue.get(0).getId(),
                "The queued appointment must be citizenB's promoted appointment");
        assertEquals(AppointmentStatus.CHECKED_IN, queue.get(0).getStatus(),
                "The queued appointment must carry CHECKED_IN status");
    }

    // -------------------------------------------------------------------------
    // Test 2 — Secondary path: promote → no-show → capacity released, no further promotion
    // -------------------------------------------------------------------------

    /**
     * Covers the no-show alternative end of the journey:
     * <ol>
     *   <li>citizenA books, citizenB joins waitlist (same setup as test 1)
     *   <li>citizenA cancels &rarr; citizenB is PROMOTED
     *   <li>citizenB's check-in window expires without any check-in
     *   <li>{@link CheckInService#markNoShow} transitions the appointment to NO_SHOW  (MF03-BR-05)
     *   <li>Waitlist is now empty &rarr; no further promotion fires — the promotion service
     *       returns empty and no additional CONFIRMED appointments appear               (MF03-BR-07)
     * </ol>
     */
    @Test
    void noShowPath_promotedAppointmentMarkedNoShow_releasesCapacityAndNoFurtherPromotion() {

        // ------ repeat abbreviated setup: book → fill → join waitlist → cancel → promote ------
        Appointment apptA = bookingService.bookAppointment(CITIZEN_A, CAMPAIGN_ID, SLOT_ID);
        slotCapacityPortDouble.setSessionFull(SESSION_ID, true);

        WaitlistEntry entryB = joinService.joinWaitlist(
                CITIZEN_B, CAMPAIGN_ID, SESSION_ID, BEFORE_24H_WINDOW);
        assertEquals(WaitlistStatus.WAITING, entryB.getStatus(),
                "Pre-condition: citizenB must be WAITING before cancellation triggers promotion");

        // citizenA cancels → promotion fires → citizenB becomes PROMOTED + CONFIRMED
        cancellationService.cancelAppointment(apptA.getId(), CITIZEN_A, BEFORE_24H_WINDOW);

        WaitlistEntry refreshedEntryB = waitlistRepository.findById(entryB.getId()).orElseThrow();
        assertEquals(WaitlistStatus.PROMOTED, refreshedEntryB.getStatus(),
                "Pre-condition: citizenB must be PROMOTED before reaching the no-show step");

        Appointment promotedApptB = appointmentRepository
                .findByCitizenIdAndCampaignIdAndStatusIn(
                        CITIZEN_B, CAMPAIGN_ID, List.of(AppointmentStatus.CONFIRMED))
                .stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Pre-condition: citizenB must have a CONFIRMED promoted appointment"));

        // ------ No-show: citizenB never checks in; window has closed ------
        Appointment noShowAppt = checkInService.markNoShow(
                promotedApptB.getId(), AFTER_CHECKIN_WINDOW);

        assertEquals(AppointmentStatus.NO_SHOW, noShowAppt.getStatus(),
                "citizenB's promoted appointment must transition to NO_SHOW after the check-in window closes");

        // Capacity must be released back by markNoShow (same pattern as cancellation)
        assertTrue(slotCapacityPortDouble.hasAvailableCapacity(SLOT_ID),
                "Slot capacity must be released (> 0) after the NO_SHOW is recorded");

        // ------ No further promotion: waitlist is empty ------
        // entryB is already PROMOTED (terminal); no other WAITING entries exist.
        // markNoShow internally calls promoteNextForSlot which must return empty Optional.
        List<Appointment> confirmedAfterNoShow = appointmentRepository.findAll()
                .stream()
                .filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED)
                .toList();
        assertTrue(confirmedAfterNoShow.isEmpty(),
                "No additional CONFIRMED appointment must be created when the waitlist is empty "
                        + "after the no-show capacity release — promotion no-op must hold");

        List<WaitlistEntry> remainingWaiting = waitlistRepository.findAll()
                .stream()
                .filter(e -> e.getStatus() == WaitlistStatus.WAITING)
                .toList();
        assertTrue(remainingWaiting.isEmpty(),
                "No WAITING waitlist entries must remain after the full no-show scenario completes");
    }

    // -------------------------------------------------------------------------
    // Test 3 — Policy A invariant: active waitlist entry rejects direct booking until citizen leaves
    // -------------------------------------------------------------------------

    /**
     * Verifies Policy A cross-table invariant (F05):
     * <ol>
     *   <li>citizenA is on the waitlist with status WAITING for Campaign 1.</li>
     *   <li>A slot opens up or has capacity (slotWithCapacity).</li>
     *   <li>citizenA attempts direct booking for Campaign 1 &rarr; rejected with {@link DuplicateWaitlistEntryException}.</li>
     *   <li>Waitlist entry remains WAITING in database, no appointment created, slot capacity untouched.</li>
     *   <li>citizenA leaves the waitlist voluntarily via {@link WaitlistLeaveService#leaveWaitlist}.</li>
     *   <li>citizenA attempts direct booking again &rarr; booking succeeds with status CONFIRMED.</li>
     * </ol>
     */
    @Test
    void policyA_activeWaitlistEntryRejectsDirectBookingUntilWaitlistLeft() {
        Long slotWithCapacity = 5002L;
        Long otherSessionId = 9002L;
        slotCapacityPortDouble.setAvailableCapacity(slotWithCapacity, 2);
        slotCapacityPortDouble.setSlotStartTime(slotWithCapacity, SLOT_START);
        slotCapacityPortDouble.setSlotSession(slotWithCapacity, otherSessionId);
        slotCapacityPortDouble.setSlotCampaign(slotWithCapacity, CAMPAIGN_ID);

        // Session 9001 is full so citizenA can join its waitlist
        slotCapacityPortDouble.setSessionFull(SESSION_ID, true);

        // Step 1: Citizen A joins waitlist for Campaign 1 (Session 9001)
        WaitlistEntry entryA = joinService.joinWaitlist(
                CITIZEN_A, CAMPAIGN_ID, SESSION_ID, BEFORE_24H_WINDOW);
        assertNotNull(entryA.getId());
        assertEquals(WaitlistStatus.WAITING, entryA.getStatus());

        // Step 2: Citizen A attempts direct booking in slotWithCapacity for the same Campaign 1
        DuplicateWaitlistEntryException ex = assertThrows(
                DuplicateWaitlistEntryException.class,
                () -> bookingService.bookAppointment(CITIZEN_A, CAMPAIGN_ID, slotWithCapacity),
                "Citizen with active WAITING waitlist entry must be rejected from booking directly"
        );
        assertTrue(ex.getMessage().contains("already has an active waitlist entry for this campaign"));

        // Step 3: Invariant check - waitlist entry is still WAITING in DB, no appointment created, slot capacity untouched
        WaitlistEntry entryAfterFailedBook = waitlistRepository.findById(entryA.getId()).orElseThrow();
        assertEquals(WaitlistStatus.WAITING, entryAfterFailedBook.getStatus(),
                "Waitlist entry must remain WAITING after rejected direct booking attempt");

        List<Appointment> apptsAfterFailedBook = appointmentRepository.findByCitizenIdAndCampaignIdAndStatusIn(
                CITIZEN_A, CAMPAIGN_ID, List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.CHECKED_IN));
        assertTrue(apptsAfterFailedBook.isEmpty(),
                "No appointment must be created for citizenA after rejected booking attempt");

        assertEquals(2, slotCapacityPortDouble.getAvailableCapacity(slotWithCapacity),
                "Slot capacity must remain completely untouched after rejected booking attempt");

        // Step 4: Citizen A leaves waitlist voluntarily
        WaitlistEntry leftEntry = leaveService.leaveWaitlist(entryA.getId(), CITIZEN_A);
        assertEquals(WaitlistStatus.CANCELLED, leftEntry.getStatus(),
                "Waitlist entry must transition to CANCELLED upon leaving");

        // Step 5: Citizen A attempts direct booking again -> now succeeds
        Appointment apptA = bookingService.bookAppointment(CITIZEN_A, CAMPAIGN_ID, slotWithCapacity);
        assertNotNull(apptA.getId());
        assertEquals(CITIZEN_A, apptA.getCitizenId());
        assertEquals(CAMPAIGN_ID, apptA.getCampaignId());
        assertEquals(slotWithCapacity, apptA.getSlotId());
        assertEquals(AppointmentStatus.CONFIRMED, apptA.getStatus(),
                "Direct booking must succeed after citizen has left the waitlist");
        assertEquals(1, slotCapacityPortDouble.getAvailableCapacity(slotWithCapacity),
                "Slot capacity must be decremented by 1 after successful booking");
    }
}
