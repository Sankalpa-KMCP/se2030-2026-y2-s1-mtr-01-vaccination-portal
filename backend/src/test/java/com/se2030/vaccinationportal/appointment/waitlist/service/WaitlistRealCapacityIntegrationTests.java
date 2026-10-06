package com.se2030.vaccinationportal.appointment.waitlist.service;

import com.se2030.vaccinationportal.appointment.testsupport.TestEligibilityLookupPortDouble;


import com.se2030.vaccinationportal.screening.api.eligibility.EligibilityStatus;
import com.se2030.vaccinationportal.appointment.persistence.model.WaitlistEntry;
import com.se2030.vaccinationportal.appointment.persistence.model.WaitlistStatus;
import com.se2030.vaccinationportal.appointment.persistence.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.persistence.repository.CitizenCampaignBookingGuardRepository;
import com.se2030.vaccinationportal.appointment.persistence.repository.WaitlistRepository;
import com.se2030.vaccinationportal.campaign.CampaignTestFixtures;
import com.se2030.vaccinationportal.campaign.core.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.center.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.reference.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.session.planning.model.TimeSlot;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.session.planning.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.reference.model.VaccineType;
import com.se2030.vaccinationportal.campaign.reference.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.TimeSlotRepository;
import com.se2030.vaccinationportal.campaign.core.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.center.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.reference.repository.VaccineTypeRepository;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end integration tests validating {@link WaitlistJoinService} with the REAL
 * {@link com.se2030.vaccinationportal.campaign.session.planning.service.TimeSlotCapacityService} production adapter.
 * Proves that invalid, nonexistent, DRAFT, CANCELLED, zero-slot, or available-capacity sessions
 * are rejected without creating waitlist rows, and only fully booked SCHEDULED sessions are admitted.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(WaitlistRealCapacityIntegrationTests.TestConfig.class)
class WaitlistRealCapacityIntegrationTests {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public TestEligibilityLookupPortDouble eligibilityLookupPort() {
            return new TestEligibilityLookupPortDouble();
        }
    }

    @Autowired
    private WaitlistJoinService waitlistJoinService;

    @Autowired
    private WaitlistRepository waitlistRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private CitizenCampaignBookingGuardRepository guardRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

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
    private TestEligibilityLookupPortDouble eligibilityPortDouble;

    private VaccinationCampaign campaign;
    private VaccinationCenter center;
    private VaccinationSession fullScheduledSession;
    private VaccinationSession availableScheduledSession;
    private VaccinationSession draftSession;
    private VaccinationSession cancelledSession;
    private VaccinationSession zeroSlotScheduledSession;
    private Instant baseStart;

    @BeforeEach
    void setUp() {
        cleanDb();
        eligibilityPortDouble.setDefaultStatus(EligibilityStatus.ELIGIBLE);

        VaccineType vax = vaccineTypeRepository.save(new VaccineType("VAX-RC-01", "Real Cap Vaccine"));
        TargetGroup tg = targetGroupRepository.save(new TargetGroup("TG-RC-01", "Real Cap Group"));

        campaign = campaignRepository.save(CampaignTestFixtures.campaign(
                "CMP-RC-01", "Real Cap Campaign", "Testing Real Capacity",
                vax, tg,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(30),
                Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(30, ChronoUnit.DAYS),
                CampaignStatus.ACTIVE
        ));

        center = centerRepository.save(new VaccinationCenter(
                "CTR-RC-01", "Real Cap Center", "100 Health Blvd", "+12345",
                CenterStatus.ACTIVE, 60, 100, "UTC"
        ));

        baseStart = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant baseEnd = baseStart.plus(8, ChronoUnit.HOURS);
        Instant bookingClose = baseStart.minus(2, ChronoUnit.HOURS);

        // 1. FULL SCHEDULED session (2 slots, both 5/5)
        fullScheduledSession = new VaccinationSession(campaign, center, baseStart, baseEnd, bookingClose, "UTC");
        fullScheduledSession.schedule();
        fullScheduledSession = sessionRepository.save(fullScheduledSession);

        TimeSlot slot1 = new TimeSlot(fullScheduledSession, baseStart, baseStart.plus(1, ChronoUnit.HOURS), 5);
        slot1.setReservedCount(5);
        timeSlotRepository.save(slot1);

        TimeSlot slot2 = new TimeSlot(fullScheduledSession, baseStart.plus(1, ChronoUnit.HOURS), baseStart.plus(2, ChronoUnit.HOURS), 5);
        slot2.setReservedCount(5);
        timeSlotRepository.save(slot2);

        // 2. AVAILABLE SCHEDULED session (1 slot: 3/5)
        availableScheduledSession = new VaccinationSession(campaign, center, baseStart, baseEnd, bookingClose, "UTC");
        availableScheduledSession.schedule();
        availableScheduledSession = sessionRepository.save(availableScheduledSession);

        TimeSlot slot3 = new TimeSlot(availableScheduledSession, baseStart, baseStart.plus(1, ChronoUnit.HOURS), 5);
        slot3.setReservedCount(3);
        timeSlotRepository.save(slot3);

        // 3. DRAFT session
        draftSession = new VaccinationSession(campaign, center, baseStart, baseEnd, bookingClose, "UTC");
        draftSession = sessionRepository.save(draftSession);

        // 4. CANCELLED session
        cancelledSession = new VaccinationSession(campaign, center, baseStart, baseEnd, bookingClose, "UTC");
        cancelledSession.cancel();
        cancelledSession = sessionRepository.save(cancelledSession);

        // 5. Zero-slot SCHEDULED session
        zeroSlotScheduledSession = new VaccinationSession(campaign, center, baseStart, baseEnd, bookingClose, "UTC");
        zeroSlotScheduledSession.schedule();
        zeroSlotScheduledSession = sessionRepository.save(zeroSlotScheduledSession);
    }

    @AfterEach
    void tearDown() {
        cleanDb();
    }

    private void cleanDb() {
        waitlistRepository.deleteAllInBatch();
        appointmentRepository.deleteAllInBatch();
        guardRepository.deleteAllInBatch();
        timeSlotRepository.deleteAllInBatch();
        sessionRepository.deleteAllInBatch();
        campaignRepository.deleteAllInBatch();
        centerRepository.deleteAllInBatch();
        vaccineTypeRepository.deleteAllInBatch();
        targetGroupRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("joinWaitlist succeeds for valid FULL SCHEDULED session using real TimeSlotCapacityService")
    void joinWaitlist_realScheduledFullSession_createsWaitingEntry() {
        Long citizenId = 5001L;
        Instant now = Instant.now();

        WaitlistEntry entry = waitlistJoinService.joinWaitlist(citizenId, campaign.getId(), fullScheduledSession.getId(), now);

        assertNotNull(entry);
        assertNotNull(entry.getId());
        assertEquals(citizenId, entry.getCitizenId());
        assertEquals(campaign.getId(), entry.getCampaignId());
        assertEquals(fullScheduledSession.getId(), entry.getSessionId());
        assertEquals(WaitlistStatus.WAITING, entry.getStatus());

        Optional<WaitlistEntry> persisted = waitlistRepository.findByCitizenIdAndCampaignIdAndStatus(
                citizenId, campaign.getId(), WaitlistStatus.WAITING);
        assertTrue(persisted.isPresent());
    }

    @Test
    @DisplayName("joinWaitlist rejects when SCHEDULED session has available capacity")
    void joinWaitlist_realScheduledSessionWithAvailableCapacity_rejected() {
        Long citizenId = 5002L;
        Instant now = Instant.now();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                waitlistJoinService.joinWaitlist(citizenId, campaign.getId(), availableScheduledSession.getId(), now)
        );
        assertTrue(ex.getMessage().contains("still has available booking capacity"));

        Optional<WaitlistEntry> persisted = waitlistRepository.findByCitizenIdAndCampaignIdAndStatus(
                citizenId, campaign.getId(), WaitlistStatus.WAITING);
        assertTrue(persisted.isEmpty());
    }

    @Test
    @DisplayName("joinWaitlist throws IllegalArgumentException for nonexistent session ID")
    void joinWaitlist_realNonexistentSession_throwsIllegalArgumentException() {
        Long citizenId = 5003L;
        Long nonexistentSessionId = 999999L;
        Instant now = Instant.now();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                waitlistJoinService.joinWaitlist(citizenId, campaign.getId(), nonexistentSessionId, now)
        );
        assertTrue(ex.getMessage().contains("Session not found"));

        Optional<WaitlistEntry> persisted = waitlistRepository.findByCitizenIdAndCampaignIdAndStatus(
                citizenId, campaign.getId(), WaitlistStatus.WAITING);
        assertTrue(persisted.isEmpty());
    }

    @Test
    @DisplayName("joinWaitlist throws IllegalStateException for DRAFT session")
    void joinWaitlist_realDraftSession_throwsIllegalStateException() {
        Long citizenId = 5004L;
        Instant now = Instant.now();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                waitlistJoinService.joinWaitlist(citizenId, campaign.getId(), draftSession.getId(), now)
        );
        assertTrue(ex.getMessage().contains("DRAFT"));

        Optional<WaitlistEntry> persisted = waitlistRepository.findByCitizenIdAndCampaignIdAndStatus(
                citizenId, campaign.getId(), WaitlistStatus.WAITING);
        assertTrue(persisted.isEmpty());
    }

    @Test
    @DisplayName("joinWaitlist throws IllegalStateException for CANCELLED session")
    void joinWaitlist_realCancelledSession_throwsIllegalStateException() {
        Long citizenId = 5005L;
        Instant now = Instant.now();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                waitlistJoinService.joinWaitlist(citizenId, campaign.getId(), cancelledSession.getId(), now)
        );
        assertTrue(ex.getMessage().contains("CANCELLED"));

        Optional<WaitlistEntry> persisted = waitlistRepository.findByCitizenIdAndCampaignIdAndStatus(
                citizenId, campaign.getId(), WaitlistStatus.WAITING);
        assertTrue(persisted.isEmpty());
    }

    @Test
    @DisplayName("joinWaitlist throws IllegalStateException for SCHEDULED session with zero configured slots")
    void joinWaitlist_realZeroSlotSession_throwsIllegalStateException() {
        Long citizenId = 5006L;
        Instant now = Instant.now();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                waitlistJoinService.joinWaitlist(citizenId, campaign.getId(), zeroSlotScheduledSession.getId(), now)
        );
        assertTrue(ex.getMessage().contains("has no configured time slots"));

        Optional<WaitlistEntry> persisted = waitlistRepository.findByCitizenIdAndCampaignIdAndStatus(
                citizenId, campaign.getId(), WaitlistStatus.WAITING);
        assertTrue(persisted.isEmpty());
    }
}
