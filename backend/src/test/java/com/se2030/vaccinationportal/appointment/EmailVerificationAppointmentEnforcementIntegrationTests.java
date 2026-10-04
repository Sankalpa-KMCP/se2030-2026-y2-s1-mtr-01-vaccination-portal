package com.se2030.vaccinationportal.appointment;

import com.se2030.vaccinationportal.appointment.booking.service.AppointmentBookingService;
import com.se2030.vaccinationportal.appointment.exception.EmailVerificationRequiredException;
import com.se2030.vaccinationportal.appointment.persistence.model.Appointment;
import com.se2030.vaccinationportal.appointment.persistence.model.AppointmentStatus;
import com.se2030.vaccinationportal.appointment.persistence.model.WaitlistEntry;
import com.se2030.vaccinationportal.appointment.persistence.model.WaitlistStatus;
import com.se2030.vaccinationportal.appointment.persistence.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.persistence.repository.CitizenCampaignBookingGuardRepository;
import com.se2030.vaccinationportal.appointment.persistence.repository.WaitlistRepository;
import com.se2030.vaccinationportal.appointment.testsupport.TestEligibilityLookupPortDouble;
import com.se2030.vaccinationportal.appointment.testsupport.TestSlotCapacityPortDouble;
import com.se2030.vaccinationportal.appointment.waitlist.service.WaitlistJoinService;
import com.se2030.vaccinationportal.appointment.waitlist.service.WaitlistPromotionService;
import com.se2030.vaccinationportal.screening.api.eligibility.EligibilityStatus;
import com.se2030.vaccinationportal.shared.auth.model.AccountStatus;
import com.se2030.vaccinationportal.shared.auth.model.Role;
import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import com.se2030.vaccinationportal.shared.auth.repository.RoleRepository;
import com.se2030.vaccinationportal.shared.auth.repository.UserAccountRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Cross-feature enforcement tests for the verified-email appointment rule, exercised through the
 * REAL Auth-owned {@code EmailVerificationStatusReader} (no test double for verification): real
 * {@code UserAccount} rows with and without {@code emailVerifiedAt} drive the gate.
 * <p>
 * Deliberately unregistered here: any test double for {@code EmailVerificationStatusReader}. The
 * only doubles are the Appointment-owned port doubles for eligibility and slot capacity, which
 * exist to control booking conditions, not to bypass the verification contract.
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(EmailVerificationAppointmentEnforcementIntegrationTests.TestConfig.class)
@Transactional
class EmailVerificationAppointmentEnforcementIntegrationTests {

    private static final Long CAMPAIGN_ID = 2001L;
    private static final Long SESSION_ID = 3001L;
    private static final Long SLOT_ID = 4001L;

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
    private WaitlistJoinService joinService;

    @Autowired
    private WaitlistPromotionService promotionService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private WaitlistRepository waitlistRepository;

    @Autowired
    private CitizenCampaignBookingGuardRepository guardRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TestEligibilityLookupPortDouble eligibilityPortDouble;

    @Autowired
    private TestSlotCapacityPortDouble slotCapacityPortDouble;

    private UserAccount verifiedCitizen;
    private UserAccount verifiedQueueMemberB;
    private UserAccount verifiedQueueMemberC;
    private UserAccount unverifiedCitizen;
    private UserAccount unverifiedQueueMember;

    @BeforeEach
    void setUp() {
        waitlistRepository.deleteAll();
        appointmentRepository.deleteAll();
        guardRepository.deleteAll();

        if (roleRepository.findByCode("ROLE_CITIZEN").isEmpty()) {
            roleRepository.save(new Role("ROLE_CITIZEN", "Citizen"));
        }
        Role citizenRole = roleRepository.findByCode("ROLE_CITIZEN").orElseThrow();

        // An established citizen whose email is verified (the state of every pre-existing
        // ACTIVE account after migration V22's backfill).
        verifiedCitizen = new UserAccount(
                "verified.booker@example.com", passwordEncoder.encode("Password123!"), AccountStatus.ACTIVE);
        verifiedCitizen.setFullName("Verified Booker");
        verifiedCitizen.setEmailVerifiedAt(Instant.now());
        verifiedCitizen.addRole(citizenRole);
        verifiedCitizen = userAccountRepository.save(verifiedCitizen);

        verifiedQueueMemberB = new UserAccount(
                "verified.queue.b@example.com", passwordEncoder.encode("Password123!"), AccountStatus.ACTIVE);
        verifiedQueueMemberB.setEmailVerifiedAt(Instant.now());
        verifiedQueueMemberB.addRole(citizenRole);
        verifiedQueueMemberB = userAccountRepository.save(verifiedQueueMemberB);

        verifiedQueueMemberC = new UserAccount(
                "verified.queue.c@example.com", passwordEncoder.encode("Password123!"), AccountStatus.ACTIVE);
        verifiedQueueMemberC.setEmailVerifiedAt(Instant.now());
        verifiedQueueMemberC.addRole(citizenRole);
        verifiedQueueMemberC = userAccountRepository.save(verifiedQueueMemberC);

        // A freshly self-registered citizen: account exists but the email is not verified yet.
        unverifiedCitizen = new UserAccount(
                "unverified.booker@example.com", passwordEncoder.encode("Password123!"), AccountStatus.PENDING_VERIFICATION);
        unverifiedCitizen.setFullName("Unverified Booker");
        unverifiedCitizen.addRole(citizenRole);
        unverifiedCitizen = userAccountRepository.save(unverifiedCitizen);

        unverifiedQueueMember = new UserAccount(
                "unverified.queue@example.com", passwordEncoder.encode("Password123!"), AccountStatus.PENDING_VERIFICATION);
        unverifiedQueueMember.addRole(citizenRole);
        unverifiedQueueMember = userAccountRepository.save(unverifiedQueueMember);

        eligibilityPortDouble.setDefaultStatus(EligibilityStatus.ELIGIBLE);
        slotCapacityPortDouble.clearSlotCampaignMap();
        slotCapacityPortDouble.clearSlotSessionMap();
        slotCapacityPortDouble.setSessionCampaign(SESSION_ID, CAMPAIGN_ID);
        slotCapacityPortDouble.setSessionFull(SESSION_ID, true);
        slotCapacityPortDouble.setSlotSession(SLOT_ID, SESSION_ID);
    }

    @Test
    @DisplayName("Unverified citizen cannot book: no appointment, no capacity consumed, even though the request would otherwise succeed")
    void unverifiedCitizen_directBooking_rejected() {
        eligibilityPortDouble.setEligibility(unverifiedCitizen.getId(), CAMPAIGN_ID, EligibilityStatus.ELIGIBLE);
        slotCapacityPortDouble.setAvailableCapacity(SLOT_ID, 5);
        int capacityBefore = slotCapacityPortDouble.getAvailableCapacity(SLOT_ID);

        assertThrows(EmailVerificationRequiredException.class,
                () -> bookingService.bookAppointment(unverifiedCitizen.getId(), CAMPAIGN_ID, SLOT_ID));

        assertTrue(appointmentRepository.findByCitizenIdAndCampaignIdAndStatusIn(
                        unverifiedCitizen.getId(), CAMPAIGN_ID, List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.CHECKED_IN)).isEmpty(),
                "No appointment may be created for an unverified citizen");
        assertEquals(capacityBefore, slotCapacityPortDouble.getAvailableCapacity(SLOT_ID),
                "Rejected booking must not touch capacity");
    }

    @Test
    @DisplayName("Missing Auth account is treated as unverified: booking fails closed")
    void missingAuthAccount_bookingFailsClosed() {
        Long citizenWithoutAccount = 999_999L;
        eligibilityPortDouble.setEligibility(citizenWithoutAccount, CAMPAIGN_ID, EligibilityStatus.ELIGIBLE);
        slotCapacityPortDouble.setAvailableCapacity(SLOT_ID, 5);

        assertThrows(EmailVerificationRequiredException.class,
                () -> bookingService.bookAppointment(citizenWithoutAccount, CAMPAIGN_ID, SLOT_ID));
        assertTrue(appointmentRepository.findByCitizenIdAndCampaignIdAndStatusIn(
                        citizenWithoutAccount, CAMPAIGN_ID, List.of(AppointmentStatus.CONFIRMED)).isEmpty());
    }

    @Test
    @DisplayName("Verified citizen books exactly as before the enforcement existed")
    void verifiedCitizen_directBooking_succeeds() {
        eligibilityPortDouble.setEligibility(verifiedCitizen.getId(), CAMPAIGN_ID, EligibilityStatus.ELIGIBLE);
        slotCapacityPortDouble.setAvailableCapacity(SLOT_ID, 5);

        Appointment booked = bookingService.bookAppointment(verifiedCitizen.getId(), CAMPAIGN_ID, SLOT_ID);

        assertNotNull(booked.getId());
        assertEquals(AppointmentStatus.CONFIRMED, booked.getStatus());
        assertEquals(4, slotCapacityPortDouble.getAvailableCapacity(SLOT_ID));
    }

    @Test
    @DisplayName("Unverified citizen cannot join a waitlist; verified citizen can")
    void waitlistJoin_enforcesVerification() {
        eligibilityPortDouble.setEligibility(unverifiedCitizen.getId(), CAMPAIGN_ID, EligibilityStatus.ELIGIBLE);
        Instant now = Instant.parse("2026-10-01T10:00:00Z");

        assertThrows(EmailVerificationRequiredException.class,
                () -> joinService.joinWaitlist(unverifiedCitizen.getId(), CAMPAIGN_ID, SESSION_ID, now));
        assertTrue(waitlistRepository.findByCitizenIdAndCampaignIdAndStatus(
                        unverifiedCitizen.getId(), CAMPAIGN_ID, WaitlistStatus.WAITING).isEmpty(),
                "No queue position may be created for an unverified citizen");

        WaitlistEntry entry = joinService.joinWaitlist(verifiedCitizen.getId(), CAMPAIGN_ID, SESSION_ID, now);
        assertNotNull(entry.getId());
        assertEquals(WaitlistStatus.WAITING, entry.getStatus());
    }

    @Test
    @DisplayName("Promotion skips the unverified head of the queue, promotes the next verified candidate, and leaves the skipped entry untouched")
    void promotion_skipsUnverifiedCandidate_promotesNextVerified() {
        Instant t0 = Instant.parse("2026-10-01T10:00:00Z");
        Instant t1 = t0.plus(10, ChronoUnit.MINUTES);

        slotCapacityPortDouble.setAvailableCapacity(SLOT_ID, 1);

        // The oldest candidate is unverified (e.g. a legacy queue entry); the later candidate is verified.
        WaitlistEntry unverifiedEntry = waitlistRepository.save(
                new WaitlistEntry(unverifiedCitizen.getId(), CAMPAIGN_ID, SESSION_ID, WaitlistStatus.WAITING, t0));
        WaitlistEntry verifiedEntry = waitlistRepository.save(
                new WaitlistEntry(verifiedCitizen.getId(), CAMPAIGN_ID, SESSION_ID, WaitlistStatus.WAITING, t1));

        Optional<Appointment> promoted = promotionService.promoteNext(SESSION_ID, SLOT_ID, t1);

        assertTrue(promoted.isPresent());
        assertEquals(verifiedCitizen.getId(), promoted.get().getCitizenId());
        assertEquals(AppointmentStatus.CONFIRMED, promoted.get().getStatus());

        WaitlistEntry skipped = waitlistRepository.findById(unverifiedEntry.getId()).orElseThrow();
        assertEquals(WaitlistStatus.WAITING, skipped.getStatus());
        assertEquals(t0, skipped.getJoinedAt(), "Skipped candidate must retain its original queue timestamp");
        assertEquals(WaitlistStatus.PROMOTED, waitlistRepository.findById(verifiedEntry.getId()).orElseThrow().getStatus());
        assertEquals(0, slotCapacityPortDouble.getAvailableCapacity(SLOT_ID), "Capacity consumed exactly once");
    }

    @Test
    @DisplayName("All-unverified queue: nothing is promoted, no capacity is used, no entry is altered")
    void promotion_allUnverified_noOps() {
        Instant t0 = Instant.parse("2026-10-01T10:00:00Z");
        Instant t1 = t0.plus(10, ChronoUnit.MINUTES);

        slotCapacityPortDouble.setAvailableCapacity(SLOT_ID, 2);
        int capacityBefore = slotCapacityPortDouble.getAvailableCapacity(SLOT_ID);

        waitlistRepository.save(new WaitlistEntry(unverifiedCitizen.getId(), CAMPAIGN_ID, SESSION_ID, WaitlistStatus.WAITING, t0));
        waitlistRepository.save(new WaitlistEntry(unverifiedQueueMember.getId(), CAMPAIGN_ID, SESSION_ID, WaitlistStatus.WAITING, t1));

        Optional<Appointment> promoted = promotionService.promoteNext(SESSION_ID, SLOT_ID, t1);

        assertTrue(promoted.isEmpty());
        assertEquals(capacityBefore, slotCapacityPortDouble.getAvailableCapacity(SLOT_ID));
        assertEquals(2, waitlistRepository.findBySessionIdAndStatusOrderByJoinedAtAsc(SESSION_ID, WaitlistStatus.WAITING).size());
    }

    @Test
    @DisplayName("A candidate skipped while unverified is promoted at its original FIFO position after verifying")
    void promotion_afterLaterVerification_honorsOriginalFifoPosition() {
        Instant t0 = Instant.parse("2026-10-01T10:00:00Z");
        Instant t1 = t0.plus(10, ChronoUnit.MINUTES);
        Instant t2 = t0.plus(20, ChronoUnit.MINUTES);

        slotCapacityPortDouble.setSlotSession(4002L, SESSION_ID);

        WaitlistEntry aEntry = waitlistRepository.save(
                new WaitlistEntry(unverifiedCitizen.getId(), CAMPAIGN_ID, SESSION_ID, WaitlistStatus.WAITING, t0));
        waitlistRepository.save(new WaitlistEntry(verifiedQueueMemberB.getId(), CAMPAIGN_ID, SESSION_ID, WaitlistStatus.WAITING, t1));
        waitlistRepository.save(new WaitlistEntry(verifiedQueueMemberC.getId(), CAMPAIGN_ID, SESSION_ID, WaitlistStatus.WAITING, t2));

        // First freed slot: unverified A is skipped, the first verified candidate is promoted.
        slotCapacityPortDouble.setAvailableCapacity(SLOT_ID, 1);
        Optional<Appointment> first = promotionService.promoteNext(SESSION_ID, SLOT_ID, t1);
        assertTrue(first.isPresent());
        assertEquals(verifiedQueueMemberB.getId(), first.get().getCitizenId());
        assertEquals(WaitlistStatus.WAITING, waitlistRepository.findById(aEntry.getId()).orElseThrow().getStatus());

        // The citizen then verifies their email; the next freed slot must be theirs by original FIFO order.
        unverifiedCitizen.setEmailVerifiedAt(Instant.now());
        userAccountRepository.save(unverifiedCitizen);

        slotCapacityPortDouble.setAvailableCapacity(4002L, 1);
        Optional<Appointment> second = promotionService.promoteNext(SESSION_ID, 4002L, t2);

        assertTrue(second.isPresent());
        assertEquals(unverifiedCitizen.getId(), second.get().getCitizenId(),
                "Once verified, the earlier-joined citizen outranks later queue members");
    }
}
