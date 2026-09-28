package com.se2030.vaccinationportal.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.se2030.vaccinationportal.appointment.dependency.SlotBookability;
import com.se2030.vaccinationportal.appointment.dependency.SlotCapacityPort;
import com.se2030.vaccinationportal.appointment.dependency.SlotReservationResult;
import com.se2030.vaccinationportal.appointment.dependency.EligibilityStatus;
import com.se2030.vaccinationportal.appointment.model.Appointment;
import com.se2030.vaccinationportal.appointment.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.repository.CitizenCampaignBookingGuardRepository;
import com.se2030.vaccinationportal.appointment.service.AppointmentBookingService;
import com.se2030.vaccinationportal.appointment.service.AppointmentRescheduleService;
import com.se2030.vaccinationportal.appointment.service.TestEligibilityLookupPortDouble;
import com.se2030.vaccinationportal.appointment.service.WaitlistPromotionService;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.TimeSlot;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.TimeSlotRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.service.TimeSlotCapacityService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Regression tests for the authoritative bookability policy at the reservation mutation boundary (R03).
 * Direct booking and rescheduling destinations must enforce the same restrictions the availability
 * search advertises (session lifecycle, booking suspension, booking cutoff, campaign booking window,
 * future slot time), computed by the authoritative MF-02 policy implementation. Waitlist promotion
 * follows its documented narrower policy (no suspension/cutoff/future-slot enforcement — freed
 * capacity from cancellations and no-shows must stay reachable) but still rejects non-intake campaigns.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(AuthoritativeBookabilityMutationTests.TestConfig.class)
class AuthoritativeBookabilityMutationTests {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public TestEligibilityLookupPortDouble eligibilityLookupPort() {
            return new TestEligibilityLookupPortDouble();
        }
    }

    @Autowired
    private AppointmentBookingService bookingService;

    @Autowired
    private AppointmentRescheduleService rescheduleService;

    @Autowired
    private WaitlistPromotionService promotionService;

    @Autowired
    private SlotCapacityPort slotCapacityPort;

    @Autowired
    private TestEligibilityLookupPortDouble eligibilityLookupPort;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private CitizenCampaignBookingGuardRepository guardRepository;

    @Autowired
    private VaccinationSessionRepository sessionRepository;

    @Autowired
    private VaccinationCampaignRepository campaignRepository;

    @Autowired
    private VaccinationCenterRepository centerRepository;

    @Autowired
    private VaccineTypeRepository vaccineTypeRepository;

    @Autowired
    private TargetGroupRepository targetGroupRepository;

    @Autowired
    private com.se2030.vaccinationportal.appointment.repository.WaitlistRepository waitlistRepository;

    @Autowired
    private com.se2030.vaccinationportal.appointment.service.AppointmentCancellationService cancellationService;

    private VaccinationCampaign openCampaign;
    private VaccinationCenter center;
    private VaccinationSession bookableSession;
    private TimeSlot futureSlot;

    @BeforeEach
    void setUp() {
        cleanDb();
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        VaccineType vax = vaccineTypeRepository.save(new VaccineType("VAX-R03-" + suffix, "R03 Vaccine"));
        TargetGroup tg = targetGroupRepository.save(new TargetGroup("TG-R03-" + suffix, "R03 Target Group"));

        openCampaign = campaignRepository.save(CampaignTestFixtures.campaign(
                "CMP-R03-" + suffix, "R03 Campaign", "R03 open-window campaign",
                vax, tg,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(30),
                Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(30, ChronoUnit.DAYS),
                CampaignStatus.ACTIVE
        ));

        center = centerRepository.save(new VaccinationCenter(
                "CTR-R03-" + suffix, "R03 Center", "1 Policy Way", "+12345",
                CenterStatus.ACTIVE, 60, 100, "UTC"
        ));

        Instant sessionStart = Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        bookableSession = new VaccinationSession(
                openCampaign, center, sessionStart, sessionStart.plus(8, ChronoUnit.HOURS),
                sessionStart.minus(2, ChronoUnit.HOURS), "UTC");
        bookableSession.schedule();
        bookableSession = sessionRepository.save(bookableSession);

        futureSlot = timeSlotRepository.save(new TimeSlot(
                bookableSession, sessionStart, sessionStart.plus(1, ChronoUnit.HOURS), 3));
    }

    @AfterEach
    void tearDown() {
        cleanDb();
    }

    private void cleanDb() {
        if (waitlistRepository != null) {
            waitlistRepository.deleteAllInBatch();
        }
        appointmentRepository.deleteAllInBatch();
        guardRepository.deleteAllInBatch();
        timeSlotRepository.deleteAllInBatch();
        sessionRepository.deleteAllInBatch();
        campaignRepository.deleteAllInBatch();
        centerRepository.deleteAllInBatch();
        targetGroupRepository.deleteAllInBatch();
        vaccineTypeRepository.deleteAllInBatch();
    }

    private VaccinationSession suspendedSession() {
        Instant start = Instant.now().plus(5, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        VaccinationSession session = new VaccinationSession(
                openCampaign, center, start, start.plus(4, ChronoUnit.HOURS),
                start.minus(2, ChronoUnit.HOURS), "UTC");
        session.schedule();
        session = sessionRepository.save(session);
        session.suspendBooking();
        session = sessionRepository.save(session);
        return session;
    }

    private VaccinationSession closedCutoffSession() {
        // Booking cutoff already passed while the slot itself is still in the future
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        VaccinationSession session = new VaccinationSession(
                openCampaign, center, start, start.plus(4, ChronoUnit.HOURS),
                Instant.now().minus(1, ChronoUnit.HOURS), "UTC");
        session.schedule();
        return sessionRepository.save(session);
    }

    @Test
    @DisplayName("R03 wiring: SlotCapacityPort remains the authoritative TimeSlotCapacityService policy")
    void slotCapacityPort_isProductionPolicyOwner() {
        assertTrue(slotCapacityPort instanceof TimeSlotCapacityService);
    }

    @Test
    @DisplayName("Direct booking into a booking-suspended session is rejected")
    void directBooking_suspendedSession_rejected() {
        VaccinationSession session = suspendedSession();
        TimeSlot slot = timeSlotRepository.save(new TimeSlot(
                session, session.getStartAt(), session.getStartAt().plus(1, ChronoUnit.HOURS), 3));
        Long citizenId = 6101L;
        eligibilityLookupPort.setEligibility(citizenId, openCampaign.getId(), EligibilityStatus.ELIGIBLE);

        assertEquals(SlotBookability.SESSION_BOOKING_SUSPENDED,
                slotCapacityPort.evaluateBookabilityForNewBooking(slot.getId(), Instant.now()));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.bookAppointment(citizenId, openCampaign.getId(), slot.getId())
        );
        assertTrue(ex.getMessage().contains("not bookable"));
        assertEquals(3, slot.getAvailableCapacity(),
                "Rejected booking must not consume capacity");
        assertTrue(appointmentRepository.findByCitizenIdAndCampaignIdAndStatusIn(
                citizenId, openCampaign.getId(),
                java.util.List.of(com.se2030.vaccinationportal.appointment.model.AppointmentStatus.CONFIRMED)).isEmpty());
    }

    @Test
    @DisplayName("Direct booking after the session booking cutoff is rejected")
    void directBooking_afterCutoff_rejected() {
        VaccinationSession session = closedCutoffSession();
        TimeSlot slot = timeSlotRepository.save(new TimeSlot(
                session, session.getStartAt(), session.getStartAt().plus(1, ChronoUnit.HOURS), 3));
        Long citizenId = 6102L;
        eligibilityLookupPort.setEligibility(citizenId, openCampaign.getId(), EligibilityStatus.ELIGIBLE);

        assertEquals(SlotBookability.SESSION_BOOKING_CLOSED,
                slotCapacityPort.evaluateBookabilityForNewBooking(slot.getId(), Instant.now()));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.bookAppointment(citizenId, openCampaign.getId(), slot.getId())
        );
        assertTrue(ex.getMessage().contains("not bookable"));
        assertEquals(3, slot.getAvailableCapacity(),
                "Rejected booking must not consume capacity");
    }

    @Test
    @DisplayName("Direct booking into an elapsed slot is rejected")
    void directBooking_elapsedSlot_rejected() {
        // Session starts in the future with a cutoff that is still open, but this particular
        // slot's start time already elapsed — the slot itself must be rejected (SLOT_IN_PAST)
        Instant sessionStart = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        VaccinationSession session = new VaccinationSession(
                openCampaign, center, sessionStart, sessionStart.plus(4, ChronoUnit.HOURS),
                sessionStart.minus(1, ChronoUnit.HOURS), "UTC");
        session.schedule();
        session = sessionRepository.save(session);
        TimeSlot pastSlot = timeSlotRepository.save(new TimeSlot(
                session, Instant.now().minus(1, ChronoUnit.HOURS), Instant.now().plus(1, ChronoUnit.HOURS), 3));

        assertEquals(SlotBookability.SLOT_IN_PAST,
                slotCapacityPort.evaluateBookabilityForNewBooking(pastSlot.getId(), Instant.now()));

        Long citizenId = 6103L;
        eligibilityLookupPort.setEligibility(citizenId, openCampaign.getId(), EligibilityStatus.ELIGIBLE);
        assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.bookAppointment(citizenId, openCampaign.getId(), pastSlot.getId())
        );
    }

    @Test
    @DisplayName("Direct booking into a closed campaign/session destination is rejected")
    void directBooking_closedCampaign_rejected() {
        openCampaign.suspend();
        openCampaign = campaignRepository.save(openCampaign);

        assertEquals(SlotBookability.CAMPAIGN_BOOKING_NOT_OPEN,
                slotCapacityPort.evaluateBookabilityForNewBooking(futureSlot.getId(), Instant.now()));

        Long citizenId = 6104L;
        eligibilityLookupPort.setEligibility(citizenId, openCampaign.getId(), EligibilityStatus.ELIGIBLE);
        assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.bookAppointment(citizenId, openCampaign.getId(), futureSlot.getId())
        );
        assertEquals(3, futureSlot.getAvailableCapacity(),
                "Closed campaign must not consume capacity via booking");
    }

    @Test
    @DisplayName("Valid direct booking still succeeds when the full policy is satisfied")
    void directBooking_valid_succeeds() {
        Long citizenId = 6105L;
        eligibilityLookupPort.setEligibility(citizenId, openCampaign.getId(), EligibilityStatus.ELIGIBLE);

        assertEquals(SlotBookability.BOOKABLE,
                slotCapacityPort.evaluateBookabilityForNewBooking(futureSlot.getId(), Instant.now()));

        Appointment appointment = bookingService.bookAppointment(citizenId, openCampaign.getId(), futureSlot.getId());
        assertNotNull(appointment);
        assertEquals(com.se2030.vaccinationportal.appointment.model.AppointmentStatus.CONFIRMED, appointment.getStatus());
        assertEquals(2, timeSlotRepository.findById(futureSlot.getId()).orElseThrow().getAvailableCapacity());
    }

    @Test
    @DisplayName("Rescheduling into an unbookable destination is rejected")
    void reschedule_suspendedDestination_rejected() {
        Long citizenId = 6106L;
        eligibilityLookupPort.setEligibility(citizenId, openCampaign.getId(), EligibilityStatus.ELIGIBLE);
        Appointment appointment = bookingService.bookAppointment(citizenId, openCampaign.getId(), futureSlot.getId());

        VaccinationSession suspended = suspendedSession();
        TimeSlot suspendedSlot = timeSlotRepository.save(new TimeSlot(
                suspended, suspended.getStartAt(), suspended.getStartAt().plus(1, ChronoUnit.HOURS), 3));

        // Request now: the source slot is >24h away (source-side rule passes) so the
        // destination bookability policy is what rejects the mutation
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> rescheduleService.rescheduleAppointment(appointment.getId(), citizenId, suspendedSlot.getId(),
                        Instant.now())
        );
        assertTrue(ex.getMessage().contains("not bookable"));

        // Source and destination counters untouched: no reservation moved
        Appointment reloaded = appointmentRepository.findById(appointment.getId()).orElseThrow();
        assertEquals(futureSlot.getId(), reloaded.getSlotId());
        assertEquals(com.se2030.vaccinationportal.appointment.model.AppointmentStatus.CONFIRMED, reloaded.getStatus());
        assertEquals(2, timeSlotRepository.findById(futureSlot.getId()).orElseThrow().getAvailableCapacity(),
                "Source reservation must be untouched");
        assertEquals(3, timeSlotRepository.findById(suspendedSlot.getId()).orElseThrow().getAvailableCapacity(),
                "Destination must not be consumed");
    }

    @Test
    @DisplayName("Promotion into a non-intake campaign is rejected, but cutoff/past-slot stays reachable")
    void promotion_bookability_distinctionIsPreserved() {
        // Campaign operationally closed: promotion rejected (documented distinction)
        openCampaign.suspend();
        openCampaign = campaignRepository.save(openCampaign);
        assertEquals(SlotBookability.CAMPAIGN_BOOKING_NOT_OPEN,
                slotCapacityPort.evaluateBookabilityForWaitlistPromotion(futureSlot.getId(), Instant.now()));
        openCampaign.resumeToActive();
        openCampaign = campaignRepository.save(openCampaign);

        // Promotion deliberately does NOT enforce booking cutoff or future-slot time:
        // freed capacity from cancellations and no-shows occurs at/after slot start, past
        // the cutoff, and must remain reachable by the FIFO queue (no-show flow).
        Instant pastStart = Instant.now().minus(1, ChronoUnit.HOURS);
        VaccinationSession startedSession = new VaccinationSession(
                openCampaign, center, pastStart.minus(1, ChronoUnit.HOURS), pastStart.plus(4, ChronoUnit.HOURS),
                pastStart.minus(2, ChronoUnit.HOURS), "UTC");
        startedSession.schedule();
        startedSession = sessionRepository.save(startedSession);
        TimeSlot startedSlot = timeSlotRepository.save(new TimeSlot(
                startedSession, pastStart, pastStart.plus(1, ChronoUnit.HOURS), 3));

        assertEquals(SlotBookability.BOOKABLE,
                slotCapacityPort.evaluateBookabilityForWaitlistPromotion(startedSlot.getId(), Instant.now()),
                "Promotion must stay reachable for past-slot capacity freed by a no-show");
        assertEquals(SlotBookability.SESSION_BOOKING_CLOSED,
                slotCapacityPort.evaluateBookabilityForNewBooking(startedSlot.getId(), Instant.now()),
                "Direct booking must stay blocked after cutoff");
    }

    @Test
    @DisplayName("Promotion into a non-scheduled session is rejected")
    void promotion_nonScheduledSession_rejected() {
        bookableSession.cancel();
        bookableSession = sessionRepository.save(bookableSession);

        assertEquals(SessionLifecycleStatus.CANCELLED, bookableSession.getLifecycleStatus());
        assertEquals(SlotBookability.SESSION_NOT_SCHEDULED,
                slotCapacityPort.evaluateBookabilityForWaitlistPromotion(futureSlot.getId(), Instant.now()));
    }

    @Test
    @DisplayName("TOCTOU prevention: concurrent suspension while reservation is in flight is detected under lock and rejected")
    void toctou_concurrentSessionSuspension_rejectedUnderLock() {
        Long citizenId = 6199L;
        eligibilityLookupPort.setEligibility(citizenId, openCampaign.getId(), EligibilityStatus.ELIGIBLE);

        // Pre-check evaluates to BOOKABLE
        assertEquals(SlotBookability.BOOKABLE,
                slotCapacityPort.evaluateBookabilityForNewBooking(futureSlot.getId(), Instant.now()));

        // Concurrently, another transaction suspends the session before reserveCapacityForNewBooking executes
        bookableSession.suspendBooking();
        sessionRepository.saveAndFlush(bookableSession);

        // Atomic reservation method must re-evaluate under session lock and return NOT_BOOKABLE
        SlotReservationResult result = slotCapacityPort.reserveCapacityForNewBooking(futureSlot.getId(), Instant.now());
        assertEquals(SlotReservationResult.NOT_BOOKABLE, result,
                "Atomic reservation must re-evaluate bookability under lock and reject suspended session");

        // Attempting to book via service must fail with not bookable exception
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.bookAppointment(citizenId, openCampaign.getId(), futureSlot.getId())
        );
        assertTrue(ex.getMessage().contains("not bookable"));

        // Capacity must remain completely untouched
        assertEquals(3, timeSlotRepository.findById(futureSlot.getId()).orElseThrow().getAvailableCapacity());
    }

    @Test
    @DisplayName("TOCTOU prevention: concurrent campaign suspension while reservation is in flight is rejected under lock")
    void toctou_concurrentCampaignSuspension_rejectedUnderLock() {
        Long citizenId = 6198L;
        eligibilityLookupPort.setEligibility(citizenId, openCampaign.getId(), EligibilityStatus.ELIGIBLE);

        // Pre-check evaluates to BOOKABLE
        assertEquals(SlotBookability.BOOKABLE,
                slotCapacityPort.evaluateBookabilityForNewBooking(futureSlot.getId(), Instant.now()));

        // Concurrently, another transaction suspends the campaign
        openCampaign.suspend();
        campaignRepository.saveAndFlush(openCampaign);

        // Atomic reservation must re-evaluate under lock and reject
        SlotReservationResult result = slotCapacityPort.reserveCapacityForNewBooking(futureSlot.getId(), Instant.now());
        assertEquals(SlotReservationResult.NOT_BOOKABLE, result);

        assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.bookAppointment(citizenId, openCampaign.getId(), futureSlot.getId())
        );

        assertEquals(3, timeSlotRepository.findById(futureSlot.getId()).orElseThrow().getAvailableCapacity());
    }

    @Test
    @DisplayName("TOCTOU interleaving: session suspended concurrently across threads is rejected by atomic reservation")
    void toctou_interleavedSessionSuspension_withCountDownLatch_rejectedSafely() throws Exception {
        Long citizenId = 6197L;
        eligibilityLookupPort.setEligibility(citizenId, openCampaign.getId(), EligibilityStatus.ELIGIBLE);

        java.util.concurrent.CountDownLatch preCheckDone = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch suspensionDone = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> bookingError = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<SlotReservationResult> reservationResult = new java.util.concurrent.atomic.AtomicReference<>();

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);

        // Thread 1: performs pre-check (passes), waits for Thread 2 to suspend session, then calls reserveCapacityForNewBooking
        java.util.concurrent.Future<?> f1 = executor.submit(() -> {
            try {
                SlotBookability preCheck = slotCapacityPort.evaluateBookabilityForNewBooking(futureSlot.getId(), Instant.now());
                assertEquals(SlotBookability.BOOKABLE, preCheck);
                preCheckDone.countDown();
                suspensionDone.await(5, java.util.concurrent.TimeUnit.SECONDS);

                reservationResult.set(slotCapacityPort.reserveCapacityForNewBooking(futureSlot.getId(), Instant.now()));
            } catch (Throwable t) {
                bookingError.set(t);
            }
        });

        // Thread 2: waits for pre-check to complete, then suspends session and commits
        java.util.concurrent.Future<?> f2 = executor.submit(() -> {
            try {
                preCheckDone.await(5, java.util.concurrent.TimeUnit.SECONDS);
                VaccinationSession s = sessionRepository.findById(bookableSession.getId()).orElseThrow();
                s.suspendBooking();
                sessionRepository.saveAndFlush(s);
                suspensionDone.countDown();
            } catch (Throwable t) {
                bookingError.set(t);
            }
        });

        f1.get(10, java.util.concurrent.TimeUnit.SECONDS);
        f2.get(10, java.util.concurrent.TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(SlotReservationResult.NOT_BOOKABLE, reservationResult.get(),
                "Atomic reservation must detect concurrent session suspension under lock");
        assertEquals(3, timeSlotRepository.findById(futureSlot.getId()).orElseThrow().getAvailableCapacity(),
                "No capacity must be consumed when concurrent suspension occurs");
    }

    @Test
    @DisplayName("F1-A: Direct booking begins while campaign open; concurrent campaign closure commits; booking fails safely under lock")
    void f1a_directBooking_interleavedCampaignClosure_rejectedSafely() throws Exception {
        Long citizenId = 6201L;
        eligibilityLookupPort.setEligibility(citizenId, openCampaign.getId(), EligibilityStatus.ELIGIBLE);

        java.util.concurrent.CountDownLatch preCheckDone = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch campaignClosed = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> threadError = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<SlotReservationResult> reservationResult = new java.util.concurrent.atomic.AtomicReference<>();

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);

        // Thread 1: pre-checks slot bookability (BOOKABLE), waits for campaign closure, then attempts authoritative reservation
        java.util.concurrent.Future<?> f1 = executor.submit(() -> {
            try {
                SlotBookability pre = slotCapacityPort.evaluateBookabilityForNewBooking(futureSlot.getId(), Instant.now());
                assertEquals(SlotBookability.BOOKABLE, pre);
                preCheckDone.countDown();
                assertTrue(campaignClosed.await(5, java.util.concurrent.TimeUnit.SECONDS));

                // Re-evaluation under Campaign -> Session -> Slot lock must catch the closed campaign!
                reservationResult.set(slotCapacityPort.reserveCapacityForNewBooking(futureSlot.getId(), Instant.now()));
            } catch (Throwable t) {
                threadError.set(t);
            }
        });

        // Thread 2: awaits preCheckDone, closes/suspends the campaign and commits
        java.util.concurrent.Future<?> f2 = executor.submit(() -> {
            try {
                assertTrue(preCheckDone.await(5, java.util.concurrent.TimeUnit.SECONDS));
                VaccinationCampaign camp = campaignRepository.findById(openCampaign.getId()).orElseThrow();
                camp.suspend();
                campaignRepository.saveAndFlush(camp);
                campaignClosed.countDown();
            } catch (Throwable t) {
                threadError.set(t);
            }
        });

        f1.get(10, java.util.concurrent.TimeUnit.SECONDS);
        f2.get(10, java.util.concurrent.TimeUnit.SECONDS);
        executor.shutdown();

        if (threadError.get() != null) {
            throw new AssertionError("Thread error in F1-A test", threadError.get());
        }

        assertEquals(SlotReservationResult.NOT_BOOKABLE, reservationResult.get(),
                "Atomic reservation must re-read campaign state under lock and reject closed campaign");
        assertEquals(3, timeSlotRepository.findById(futureSlot.getId()).orElseThrow().getAvailableCapacity(),
                "No slot capacity may be consumed when campaign is closed");
    }

    @Test
    @DisplayName("F1-A: Reschedule begins while destination campaign open; concurrent destination campaign closure commits; reschedule rejected")
    void f1a_reschedule_interleavedDestinationCampaignClosure_rejectedSafely() throws Exception {
        Long citizenId = 6202L;
        eligibilityLookupPort.setEligibility(citizenId, openCampaign.getId(), EligibilityStatus.ELIGIBLE);
        Appointment appointment = bookingService.bookAppointment(citizenId, openCampaign.getId(), futureSlot.getId());
        assertEquals(2, timeSlotRepository.findById(futureSlot.getId()).orElseThrow().getAvailableCapacity());

        Instant destStart = Instant.now().plus(4, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        VaccinationSession destSession = new VaccinationSession(
                openCampaign, center, destStart, destStart.plus(4, ChronoUnit.HOURS),
                destStart.minus(2, ChronoUnit.HOURS), "UTC");
        destSession.schedule();
        destSession = sessionRepository.save(destSession);
        TimeSlot destSlot = timeSlotRepository.save(new TimeSlot(destSession, destStart, destStart.plus(1, ChronoUnit.HOURS), 5));

        java.util.concurrent.CountDownLatch preCheckDone = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch closureDone = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> threadError = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<SlotReservationResult> destResult = new java.util.concurrent.atomic.AtomicReference<>();

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);

        // Thread 1: evaluates destination bookability (BOOKABLE), waits for campaign closure, then attempts reservation
        java.util.concurrent.Future<?> f1 = executor.submit(() -> {
            try {
                SlotBookability pre = slotCapacityPort.evaluateBookabilityForNewBooking(destSlot.getId(), Instant.now());
                assertEquals(SlotBookability.BOOKABLE, pre);
                preCheckDone.countDown();
                assertTrue(closureDone.await(5, java.util.concurrent.TimeUnit.SECONDS));

                destResult.set(slotCapacityPort.reserveCapacityForNewBooking(destSlot.getId(), Instant.now()));
            } catch (Throwable t) {
                threadError.set(t);
            }
        });

        // Thread 2: suspends campaign
        java.util.concurrent.Future<?> f2 = executor.submit(() -> {
            try {
                assertTrue(preCheckDone.await(5, java.util.concurrent.TimeUnit.SECONDS));
                VaccinationCampaign camp = campaignRepository.findById(openCampaign.getId()).orElseThrow();
                camp.suspend();
                campaignRepository.saveAndFlush(camp);
                closureDone.countDown();
            } catch (Throwable t) {
                threadError.set(t);
            }
        });

        f1.get(10, java.util.concurrent.TimeUnit.SECONDS);
        f2.get(10, java.util.concurrent.TimeUnit.SECONDS);
        executor.shutdown();

        if (threadError.get() != null) {
            throw new AssertionError("Thread error in F1-A reschedule test", threadError.get());
        }

        assertEquals(SlotReservationResult.NOT_BOOKABLE, destResult.get());
        assertEquals(5, timeSlotRepository.findById(destSlot.getId()).orElseThrow().getAvailableCapacity(),
                "Destination slot must not have capacity decremented");
        assertEquals(com.se2030.vaccinationportal.appointment.model.AppointmentStatus.CONFIRMED,
                appointmentRepository.findById(appointment.getId()).orElseThrow().getStatus());
        assertEquals(2, timeSlotRepository.findById(futureSlot.getId()).orElseThrow().getAvailableCapacity(),
                "Source slot capacity remains unchanged");
    }

    @Test
    @DisplayName("F1-A: Waitlist candidate queued; campaign closes concurrently; promotion rejects reservation under lock")
    void f1a_promotion_interleavedCampaignClosure_rejectedSafely() throws Exception {
        Long citizenId = 6203L;
        com.se2030.vaccinationportal.appointment.model.WaitlistEntry entry =
                new com.se2030.vaccinationportal.appointment.model.WaitlistEntry(
                        citizenId, openCampaign.getId(), bookableSession.getId()
                );
        waitlistRepository.saveAndFlush(entry);

        java.util.concurrent.CountDownLatch preCheckDone = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch closureDone = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> threadError = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<SlotReservationResult> promoResult = new java.util.concurrent.atomic.AtomicReference<>();

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);

        // Thread 1: evaluates promotion bookability (BOOKABLE), waits for campaign closure, then attempts reservation
        java.util.concurrent.Future<?> f1 = executor.submit(() -> {
            try {
                SlotBookability pre = slotCapacityPort.evaluateBookabilityForWaitlistPromotion(futureSlot.getId(), Instant.now());
                assertEquals(SlotBookability.BOOKABLE, pre);
                preCheckDone.countDown();
                assertTrue(closureDone.await(5, java.util.concurrent.TimeUnit.SECONDS));

                promoResult.set(slotCapacityPort.reserveCapacityForWaitlistPromotion(futureSlot.getId(), Instant.now()));
            } catch (Throwable t) {
                threadError.set(t);
            }
        });

        // Thread 2: suspends campaign
        java.util.concurrent.Future<?> f2 = executor.submit(() -> {
            try {
                assertTrue(preCheckDone.await(5, java.util.concurrent.TimeUnit.SECONDS));
                VaccinationCampaign camp = campaignRepository.findById(openCampaign.getId()).orElseThrow();
                camp.suspend();
                campaignRepository.saveAndFlush(camp);
                closureDone.countDown();
            } catch (Throwable t) {
                threadError.set(t);
            }
        });

        f1.get(10, java.util.concurrent.TimeUnit.SECONDS);
        f2.get(10, java.util.concurrent.TimeUnit.SECONDS);
        executor.shutdown();

        if (threadError.get() != null) {
            throw new AssertionError("Thread error in F1-A promotion test", threadError.get());
        }

        assertEquals(SlotReservationResult.NOT_BOOKABLE, promoResult.get(),
                "Promotion reservation must observe closed campaign under lock and return NOT_BOOKABLE");
        assertEquals(3, timeSlotRepository.findById(futureSlot.getId()).orElseThrow().getAvailableCapacity(),
                "Capacity must remain untouched");
    }

    @Test
    @DisplayName("F1-B: Concurrent booking vs cancellation + promotion on same session/slot executes without lock inversion or deadlock")
    void f1b_concurrentBookingVsCancellationAndPromotion_noDeadlock() throws Exception {
        Long bookerCitizenId = 6204L;
        Long cancellingCitizenId = 6205L;
        Long waiterCitizenId = 6206L;

        eligibilityLookupPort.setEligibility(bookerCitizenId, openCampaign.getId(), EligibilityStatus.ELIGIBLE);
        eligibilityLookupPort.setEligibility(cancellingCitizenId, openCampaign.getId(), EligibilityStatus.ELIGIBLE);
        eligibilityLookupPort.setEligibility(waiterCitizenId, openCampaign.getId(), EligibilityStatus.ELIGIBLE);

        // Pre-create appointment for cancelling citizen
        Appointment cancellingAppt = bookingService.bookAppointment(cancellingCitizenId, openCampaign.getId(), futureSlot.getId());
        assertEquals(2, timeSlotRepository.findById(futureSlot.getId()).orElseThrow().getAvailableCapacity());

        // Queue waitlist candidate
        com.se2030.vaccinationportal.appointment.model.WaitlistEntry waiterEntry =
                waitlistRepository.saveAndFlush(new com.se2030.vaccinationportal.appointment.model.WaitlistEntry(
                        waiterCitizenId, openCampaign.getId(), bookableSession.getId()
                ));

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.CountDownLatch ready = new java.util.concurrent.CountDownLatch(2);
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> threadError = new java.util.concurrent.atomic.AtomicReference<>();

        // Thread 1: booking path (acquires Campaign -> Session -> Slot)
        java.util.concurrent.Future<?> f1 = executor.submit(() -> {
            ready.countDown();
            try {
                start.await();
                bookingService.bookAppointment(bookerCitizenId, openCampaign.getId(), futureSlot.getId());
            } catch (Throwable t) {
                threadError.set(t);
            }
        });

        // Thread 2: cancellation path (releases capacity Campaign -> Session -> Slot, then synchronously promotes waiter Campaign -> Session -> Slot)
        java.util.concurrent.Future<?> f2 = executor.submit(() -> {
            ready.countDown();
            try {
                start.await();
                cancellationService.cancelAppointment(cancellingAppt.getId(), cancellingCitizenId, Instant.now());
            } catch (Throwable t) {
                threadError.set(t);
            }
        });

        ready.await();
        start.countDown();

        // Must complete without deadlock (timeout triggers failure if deadlocked)
        f1.get(10, java.util.concurrent.TimeUnit.SECONDS);
        f2.get(10, java.util.concurrent.TimeUnit.SECONDS);
        executor.shutdown();

        if (threadError.get() != null) {
            throw new AssertionError("Thread failed during concurrent booking vs cancellation", threadError.get());
        }

        // Verify state is consistent:
        Appointment cancelled = appointmentRepository.findById(cancellingAppt.getId()).orElseThrow();
        assertEquals(com.se2030.vaccinationportal.appointment.model.AppointmentStatus.CANCELLED, cancelled.getStatus());

        assertFalse(appointmentRepository.findByCitizenIdAndCampaignIdAndStatusIn(
                bookerCitizenId, openCampaign.getId(),
                java.util.List.of(com.se2030.vaccinationportal.appointment.model.AppointmentStatus.CONFIRMED)).isEmpty());
        assertNotNull(waitlistRepository.findById(waiterEntry.getId()).orElseThrow());
    }
}
