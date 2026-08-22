package com.se2030.vaccinationportal.appointment.service;

import com.se2030.vaccinationportal.appointment.model.Appointment;
import com.se2030.vaccinationportal.appointment.model.AppointmentStatus;
import com.se2030.vaccinationportal.appointment.repository.AppointmentRepository;
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
import com.se2030.vaccinationportal.campaign.service.SessionImpactSummary;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration tests for {@link SessionImpactService} verifying accurate calculation of active citizen appointments
 * (CONFIRMED, CHECKED_IN) for a session's time slots, correct exclusion of inactive statuses (CANCELLED, COMPLETED, NO_SHOW),
 * cross-session isolation, and waitlist baseline behavior.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SessionImpactServiceIntegrationTests {

    @Autowired
    private SessionImpactService sessionImpactService;

    @Autowired
    private AppointmentRepository appointmentRepository;

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

    private VaccinationCampaign testCampaign;
    private VaccinationCenter testCenter;
    private VaccinationSession sessionA;
    private VaccinationSession sessionB;
    private TimeSlot slotA1;
    private TimeSlot slotA2;
    private TimeSlot slotB1;

    @BeforeEach
    void setUp() {
        VaccineType vt = vaccineTypeRepository.saveAndFlush(
                new VaccineType("VAX-IMP-" + System.nanoTime(), "Impact Vaccine"));
        TargetGroup tg = targetGroupRepository.saveAndFlush(
                new TargetGroup("TG-IMP-" + System.nanoTime(), "Impact Target Group"));

        testCampaign = new VaccinationCampaign(
                "CAMP-IMP-" + System.nanoTime(),
                "Impact Test Campaign",
                "Description",
                vt,
                tg,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31),
                Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-10-30T00:00:00Z")
        );
        testCampaign = campaignRepository.saveAndFlush(testCampaign);

        testCenter = new VaccinationCenter(
                "CTR-IMP-" + System.nanoTime(),
                "Impact Center",
                "123 Hospital Road",
                "+94 11 111 1111",
                CenterStatus.ACTIVE,
                100,
                50,
                "UTC"
        );
        testCenter = centerRepository.saveAndFlush(testCenter);

        Instant startA = Instant.parse("2026-10-15T09:00:00Z");
        Instant endA = Instant.parse("2026-10-15T12:00:00Z");
        Instant closeA = Instant.parse("2026-10-15T08:00:00Z");
        sessionA = new VaccinationSession(testCampaign, testCenter, startA, endA, closeA, "UTC");
        sessionA.schedule();
        sessionA = sessionRepository.saveAndFlush(sessionA);

        Instant startB = Instant.parse("2026-10-16T09:00:00Z");
        Instant endB = Instant.parse("2026-10-16T12:00:00Z");
        Instant closeB = Instant.parse("2026-10-16T08:00:00Z");
        sessionB = new VaccinationSession(testCampaign, testCenter, startB, endB, closeB, "UTC");
        sessionB.schedule();
        sessionB = sessionRepository.saveAndFlush(sessionB);

        slotA1 = new TimeSlot(sessionA, startA, startA.plusSeconds(3600), 10);
        slotA1 = timeSlotRepository.saveAndFlush(slotA1);

        slotA2 = new TimeSlot(sessionA, startA.plusSeconds(3600), endA, 10);
        slotA2 = timeSlotRepository.saveAndFlush(slotA2);

        slotB1 = new TimeSlot(sessionB, startB, endB, 20);
        slotB1 = timeSlotRepository.saveAndFlush(slotB1);
    }

    @Test
    @DisplayName("Returns zero active impact when session has no appointments")
    void testZeroImpactWhenNoAppointments() {
        SessionImpactSummary impact = sessionImpactService.getSessionImpact(sessionA.getId());
        assertNotNull(impact);
        assertEquals(0, impact.affectedAppointmentCount());
        assertEquals(0, impact.affectedWaitlistCount());
    }

    @Test
    @DisplayName("Counts CONFIRMED appointment for session time slot")
    void testCountConfirmedAppointment() {
        Appointment apt = new Appointment(101L, testCampaign.getId(), slotA1.getId(), AppointmentStatus.CONFIRMED, Instant.now());
        appointmentRepository.saveAndFlush(apt);

        SessionImpactSummary impact = sessionImpactService.getSessionImpact(sessionA.getId());
        assertEquals(1, impact.affectedAppointmentCount());
        assertEquals(0, impact.affectedWaitlistCount());
    }

    @Test
    @DisplayName("Counts both CONFIRMED and CHECKED_IN appointments across multiple slots of session")
    void testCountConfirmedAndCheckedInAcrossSlots() {
        appointmentRepository.saveAndFlush(new Appointment(101L, testCampaign.getId(), slotA1.getId(), AppointmentStatus.CONFIRMED, Instant.now()));
        appointmentRepository.saveAndFlush(new Appointment(102L, testCampaign.getId(), slotA1.getId(), AppointmentStatus.CHECKED_IN, Instant.now()));
        appointmentRepository.saveAndFlush(new Appointment(103L, testCampaign.getId(), slotA2.getId(), AppointmentStatus.CONFIRMED, Instant.now()));

        SessionImpactSummary impact = sessionImpactService.getSessionImpact(sessionA.getId());
        assertEquals(3, impact.affectedAppointmentCount());
        assertEquals(0, impact.affectedWaitlistCount());
    }

    @Test
    @DisplayName("Excludes CANCELLED, COMPLETED, and NO_SHOW appointments from active impact count")
    void testExcludeInactiveStatuses() {
        appointmentRepository.saveAndFlush(new Appointment(101L, testCampaign.getId(), slotA1.getId(), AppointmentStatus.CONFIRMED, Instant.now()));
        appointmentRepository.saveAndFlush(new Appointment(102L, testCampaign.getId(), slotA1.getId(), AppointmentStatus.CANCELLED, Instant.now()));
        appointmentRepository.saveAndFlush(new Appointment(103L, testCampaign.getId(), slotA2.getId(), AppointmentStatus.COMPLETED, Instant.now()));
        appointmentRepository.saveAndFlush(new Appointment(104L, testCampaign.getId(), slotA2.getId(), AppointmentStatus.NO_SHOW, Instant.now()));

        SessionImpactSummary impact = sessionImpactService.getSessionImpact(sessionA.getId());
        assertEquals(1, impact.affectedAppointmentCount());
        assertEquals(0, impact.affectedWaitlistCount());
    }

    @Test
    @DisplayName("Excludes appointments belonging to another session")
    void testCrossSessionExclusion() {
        appointmentRepository.saveAndFlush(new Appointment(101L, testCampaign.getId(), slotA1.getId(), AppointmentStatus.CONFIRMED, Instant.now()));
        appointmentRepository.saveAndFlush(new Appointment(102L, testCampaign.getId(), slotB1.getId(), AppointmentStatus.CONFIRMED, Instant.now()));
        appointmentRepository.saveAndFlush(new Appointment(103L, testCampaign.getId(), slotB1.getId(), AppointmentStatus.CONFIRMED, Instant.now()));

        SessionImpactSummary impactA = sessionImpactService.getSessionImpact(sessionA.getId());
        assertEquals(1, impactA.affectedAppointmentCount());

        SessionImpactSummary impactB = sessionImpactService.getSessionImpact(sessionB.getId());
        assertEquals(2, impactB.affectedAppointmentCount());
    }

    @Test
    @DisplayName("Unknown session ID returns zero impact")
    void testUnknownSessionIdReturnsZeroImpact() {
        SessionImpactSummary impact = sessionImpactService.getSessionImpact(999999L);
        assertNotNull(impact);
        assertEquals(0, impact.affectedAppointmentCount());
        assertEquals(0, impact.affectedWaitlistCount());
    }

    @Test
    @DisplayName("Null session ID throws IllegalArgumentException")
    void testNullSessionIdThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> sessionImpactService.getSessionImpact(null));
    }
}
