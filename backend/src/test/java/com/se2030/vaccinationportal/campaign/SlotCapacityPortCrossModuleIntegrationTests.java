package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.appointment.dependency.EligibilityLookupPort;
import com.se2030.vaccinationportal.appointment.dependency.EligibilityStatus;
import com.se2030.vaccinationportal.campaign.api.capacity.SlotCapacityPort;
import com.se2030.vaccinationportal.appointment.persistence.model.Appointment;
import com.se2030.vaccinationportal.appointment.persistence.model.AppointmentStatus;
import com.se2030.vaccinationportal.appointment.persistence.model.WaitlistEntry;
import com.se2030.vaccinationportal.appointment.persistence.model.WaitlistStatus;
import com.se2030.vaccinationportal.appointment.persistence.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.persistence.repository.CitizenCampaignBookingGuardRepository;
import com.se2030.vaccinationportal.appointment.persistence.repository.WaitlistRepository;
import com.se2030.vaccinationportal.appointment.booking.service.AppointmentBookingService;
import com.se2030.vaccinationportal.appointment.booking.service.AppointmentCancellationService;
import com.se2030.vaccinationportal.appointment.testsupport.TestEligibilityLookupPortDouble;
import com.se2030.vaccinationportal.appointment.waitlist.service.WaitlistJoinService;
import com.se2030.vaccinationportal.appointment.waitlist.service.WaitlistPromotionService;
import java.util.Optional;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Import(SlotCapacityPortCrossModuleIntegrationTests.TestConfig.class)
class SlotCapacityPortCrossModuleIntegrationTests {

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
    private AppointmentCancellationService cancellationService;

    @Autowired
    private SlotCapacityPort slotCapacityPort;

    @Autowired
    private TestEligibilityLookupPortDouble eligibilityLookupPort;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

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
    private WaitlistRepository waitlistRepository;

    @Autowired
    private CitizenCampaignBookingGuardRepository guardRepository;

    @Autowired
    private WaitlistPromotionService promotionService;

    @Autowired
    private WaitlistJoinService waitlistJoinService;

    private VaccinationCampaign testCampaign;
    private VaccinationCenter testCenter;
    private VaccinationSession scheduledSession;
    private TimeSlot testSlot;
    private Instant baseStart;
    private Instant baseEnd;

    @BeforeEach
    void setUp() {
        cleanDb();
        VaccineType vax = vaccineTypeRepository.save(new VaccineType("VAX-XM-01", "Cross Vaccine"));
        TargetGroup tg = targetGroupRepository.save(new TargetGroup("TG-XM-01", "Cross Target Group"));

        testCampaign = campaignRepository.save(CampaignTestFixtures.campaign(
                "CMP-XM-01", "Cross Module Campaign", "Testing Cross Module",
                vax, tg,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(30),
                Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(30, ChronoUnit.DAYS),
                CampaignStatus.ACTIVE
        ));

        testCenter = centerRepository.save(new VaccinationCenter(
                "CTR-XM-01", "Cross Center", "100 Health Way", "+12345",
                CenterStatus.ACTIVE, 60, 100, "UTC"
        ));

        baseStart = Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        baseEnd = baseStart.plus(8, ChronoUnit.HOURS);

        scheduledSession = new VaccinationSession(
                testCampaign,
                testCenter,
                baseStart,
                baseEnd,
                baseStart.minus(2, ChronoUnit.HOURS),
                "UTC"
        );
        scheduledSession.schedule();
        scheduledSession = sessionRepository.save(scheduledSession);

        testSlot = new TimeSlot(scheduledSession, baseStart, baseStart.plus(1, ChronoUnit.HOURS), 2);
        testSlot = timeSlotRepository.save(testSlot);
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
        targetGroupRepository.deleteAllInBatch();
        vaccineTypeRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("Cross-module wiring: SlotCapacityPort is wired to TimeSlotCapacityService")
    void slotCapacityPort_isWiredToProductionAdapter() {
        assertNotNull(slotCapacityPort);
        assertTrue(slotCapacityPort instanceof TimeSlotCapacityService);
    }

    @Test
    @DisplayName("End-to-end appointment booking reserves capacity on MF-02 TimeSlot")
    void bookAppointment_reservesSlotCapacity() {
        Long citizenId = 501L;
        eligibilityLookupPort.setEligibility(citizenId, testCampaign.getId(), EligibilityStatus.ELIGIBLE);

        Appointment appointment = bookingService.bookAppointment(citizenId, testCampaign.getId(), testSlot.getId());
        assertNotNull(appointment.getId());
        assertEquals(AppointmentStatus.CONFIRMED, appointment.getStatus());

        TimeSlot updated = timeSlotRepository.findById(testSlot.getId()).orElseThrow();
        assertEquals(1, updated.getReservedCount());
        assertEquals(1, updated.getAvailableCapacity());
    }

    @Test
    @DisplayName("End-to-end appointment cancellation releases capacity on MF-02 TimeSlot")
    void cancelAppointment_releasesSlotCapacity() {
        Long citizenId = 502L;
        eligibilityLookupPort.setEligibility(citizenId, testCampaign.getId(), EligibilityStatus.ELIGIBLE);

        Appointment appointment = bookingService.bookAppointment(citizenId, testCampaign.getId(), testSlot.getId());
        assertEquals(1, timeSlotRepository.findById(testSlot.getId()).orElseThrow().getReservedCount());

        Appointment cancelled = cancellationService.cancelAppointment(appointment.getId(), citizenId, false, null, Instant.now());
        assertEquals(AppointmentStatus.CANCELLED, cancelled.getStatus());

        TimeSlot updated = timeSlotRepository.findById(testSlot.getId()).orElseThrow();
        assertEquals(0, updated.getReservedCount());
        assertEquals(2, updated.getAvailableCapacity());
    }

    @Test
    @DisplayName("Cross-module slot-campaign integrity: isSlotInCampaign validates correctly against database")
    void isSlotInCampaign_validatesAgainstDatabase() {
        assertTrue(slotCapacityPort.isSlotInCampaign(testSlot.getId(), testCampaign.getId()));
        assertFalse(slotCapacityPort.isSlotInCampaign(testSlot.getId(), 999999L));
        assertFalse(slotCapacityPort.isSlotInCampaign(999999L, testCampaign.getId()));
        assertFalse(slotCapacityPort.isSlotInCampaign(null, testCampaign.getId()));
        assertFalse(slotCapacityPort.isSlotInCampaign(testSlot.getId(), null));
    }

    @Test
    @DisplayName("Cross-module booking rejects mismatched campaign slot and leaves capacity untouched")
    void bookAppointment_withMismatchedCampaign_rejectsAndPreservesCapacity() {
        Long citizenId = 503L;
        Long mismatchedCampaignId = 999999L;
        eligibilityLookupPort.setEligibility(citizenId, mismatchedCampaignId, EligibilityStatus.ELIGIBLE);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.bookAppointment(citizenId, mismatchedCampaignId, testSlot.getId())
        );
        assertTrue(ex.getMessage().contains("does not belong to campaign " + mismatchedCampaignId));

        TimeSlot updated = timeSlotRepository.findById(testSlot.getId()).orElseThrow();
        assertEquals(0, updated.getReservedCount());
        assertEquals(2, updated.getAvailableCapacity());
    }

    @Test
    @DisplayName("Cross-module session resolution: getSessionIdForSlot resolves authoritative session from database")
    void getSessionIdForSlot_validatesAgainstDatabase() {
        Optional<Long> sessionIdOpt = slotCapacityPort.getSessionIdForSlot(testSlot.getId());
        assertTrue(sessionIdOpt.isPresent(), "Must resolve session for scheduled time slot");
        assertEquals(scheduledSession.getId(), sessionIdOpt.get());

        // Nonexistent slot
        assertTrue(slotCapacityPort.getSessionIdForSlot(999999L).isEmpty());
        // Null slot
        assertTrue(slotCapacityPort.getSessionIdForSlot(null).isEmpty());
    }

    @Test
    @DisplayName("Cross-module session resolution: getSessionIdForSlot returns empty for DRAFT session")
    void getSessionIdForSlot_draftSession_returnsEmpty() {
        VaccinationSession draftSession = new VaccinationSession(
                testCampaign,
                testCenter,
                baseStart,
                baseEnd,
                baseStart.minus(2, ChronoUnit.HOURS),
                "UTC"
        );
        draftSession = sessionRepository.save(draftSession);
        TimeSlot draftSlot = new TimeSlot(draftSession, baseStart, baseStart.plus(1, ChronoUnit.HOURS), 2);
        draftSlot = timeSlotRepository.save(draftSlot);

        assertTrue(slotCapacityPort.getSessionIdForSlot(draftSlot.getId()).isEmpty());
    }

    @Test
    @DisplayName("Cross-module end-to-end promotion scopes strictly to freed slot's session using real TimeSlotCapacityService")
    void promoteNextForSlot_withRealAdapter_scopesToFreedSlotSession() {
        Long citizenSession1 = 601L;
        Long citizenSession2 = 602L;
        eligibilityLookupPort.setEligibility(citizenSession1, testCampaign.getId(), EligibilityStatus.ELIGIBLE);
        eligibilityLookupPort.setEligibility(citizenSession2, testCampaign.getId(), EligibilityStatus.ELIGIBLE);

        // Session 1 is scheduledSession with testSlot (make it full: 2/2)
        testSlot.setReservedCount(2);
        timeSlotRepository.save(testSlot);

        // Session 2 is a second scheduled session with slot2 (full: 1/1)
        Instant s2Start = baseStart.plus(1, ChronoUnit.DAYS);
        Instant s2End = s2Start.plus(8, ChronoUnit.HOURS);
        VaccinationSession session2 = new VaccinationSession(
                testCampaign,
                testCenter,
                s2Start,
                s2End,
                s2Start.minus(2, ChronoUnit.HOURS),
                "UTC"
        );
        session2.schedule();
        session2 = sessionRepository.save(session2);

        TimeSlot slot2 = new TimeSlot(session2, s2Start, s2Start.plus(1, ChronoUnit.HOURS), 1);
        slot2.setReservedCount(1);
        slot2 = timeSlotRepository.save(slot2);

        Instant t0 = Instant.now().minus(2, ChronoUnit.HOURS);
        Instant t1 = Instant.now().minus(1, ChronoUnit.HOURS);

        // Citizen 1 joins session 1 at t0
        WaitlistEntry entry1 = waitlistJoinService.joinWaitlist(citizenSession1, testCampaign.getId(), scheduledSession.getId(), t0);
        // Citizen 2 joins session 2 at t1 (later)
        WaitlistEntry entry2 = waitlistJoinService.joinWaitlist(citizenSession2, testCampaign.getId(), session2.getId(), t1);

        // Slot 2 frees 1 place of capacity
        slot2.setReservedCount(0);
        timeSlotRepository.save(slot2);

        // Trigger promotion for slot2
        Optional<Appointment> promoted = promotionService.promoteNextForSlot(testCampaign.getId(), slot2.getId(), Instant.now());

        assertTrue(promoted.isPresent(), "Session 2 candidate must be promoted into slot2");
        assertEquals(citizenSession2, promoted.get().getCitizenId(), "Must promote session 2 candidate, NOT earlier session 1 candidate");
        assertEquals(slot2.getId(), promoted.get().getSlotId());

        // Verify entry1 remains WAITING and entry2 is PROMOTED
        assertEquals(WaitlistStatus.WAITING, waitlistRepository.findById(entry1.getId()).orElseThrow().getStatus());
        assertEquals(WaitlistStatus.PROMOTED, waitlistRepository.findById(entry2.getId()).orElseThrow().getStatus());
    }
}
