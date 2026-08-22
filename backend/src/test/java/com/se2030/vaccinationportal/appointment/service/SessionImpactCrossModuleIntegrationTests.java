package com.se2030.vaccinationportal.appointment.service;

import com.se2030.vaccinationportal.appointment.model.Appointment;
import com.se2030.vaccinationportal.appointment.model.AppointmentStatus;
import com.se2030.vaccinationportal.appointment.repository.AppointmentRepository;
import com.se2030.vaccinationportal.campaign.model.CampaignCenterAssignment;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.OperatingHours;
import com.se2030.vaccinationportal.campaign.model.SessionChangeType;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.TimeSlot;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.CampaignCenterAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.OperatingHoursRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.TimeSlotRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.service.SessionChangePreview;
import com.se2030.vaccinationportal.campaign.service.SessionImpactReader;
import com.se2030.vaccinationportal.campaign.service.VaccinationSessionService;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Cross-module integration tests proving that the REAL production {@link SessionImpactService} bean
 * is discovered by Spring Boot component scanning and automatically wired into MF-02's {@link VaccinationSessionService}.
 * Verifies that SCHEDULED session cancellation preview, cancellation confirmation, and rescheduling preview
 * successfully compute and propagate live appointment impact without throwing "Session impact reader is not available".
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SessionImpactCrossModuleIntegrationTests {

    @Autowired
    private VaccinationSessionService vaccinationSessionService;

    @Autowired
    private SessionImpactReader sessionImpactReader;

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

    @Autowired
    private CampaignCenterAssignmentRepository assignmentRepository;

    @Autowired
    private OperatingHoursRepository operatingHoursRepository;

    private VaccinationCampaign testCampaign;
    private VaccinationCenter testCenter;
    private VaccinationSession scheduledSession;
    private TimeSlot slot1;
    private TimeSlot slot2;

    private static final LocalDate CAMPAIGN_START = LocalDate.of(2026, 10, 1);
    private static final LocalDate CAMPAIGN_END = LocalDate.of(2026, 10, 31);

    @BeforeEach
    void setUp() {
        VaccineType vt = vaccineTypeRepository.saveAndFlush(
                new VaccineType("VAX-CROSS-" + System.nanoTime(), "Cross Vaccine"));
        TargetGroup tg = targetGroupRepository.saveAndFlush(
                new TargetGroup("TG-CROSS-" + System.nanoTime(), "Cross Target Group"));

        testCampaign = new VaccinationCampaign(
                "CAMP-CROSS-" + System.nanoTime(),
                "Cross Module Campaign",
                "Description",
                vt,
                tg,
                CAMPAIGN_START,
                CAMPAIGN_END,
                Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-10-30T00:00:00Z")
        );
        testCampaign = campaignRepository.saveAndFlush(testCampaign);

        testCenter = new VaccinationCenter(
                "CTR-CROSS-" + System.nanoTime(),
                "Cross Module Center",
                "456 Health Avenue",
                "+94 11 222 3333",
                CenterStatus.ACTIVE,
                200,
                100,
                "UTC"
        );
        testCenter = centerRepository.saveAndFlush(testCenter);

        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                testCampaign, testCenter, CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));

        for (DayOfWeek dow : DayOfWeek.values()) {
            operatingHoursRepository.saveAndFlush(new OperatingHours(
                    testCenter, dow, LocalTime.of(8, 0), LocalTime.of(18, 0),
                    CAMPAIGN_START.minusMonths(1), null));
        }

        Instant start = Instant.parse("2026-10-15T09:00:00Z");
        Instant end = Instant.parse("2026-10-15T12:00:00Z");
        Instant close = Instant.parse("2026-10-15T08:00:00Z");

        scheduledSession = new VaccinationSession(testCampaign, testCenter, start, end, close, "UTC");
        scheduledSession.schedule();
        scheduledSession = sessionRepository.saveAndFlush(scheduledSession);

        slot1 = new TimeSlot(scheduledSession, start, start.plusSeconds(3600), 10);
        slot1 = timeSlotRepository.saveAndFlush(slot1);

        slot2 = new TimeSlot(scheduledSession, start.plusSeconds(3600), end, 10);
        slot2 = timeSlotRepository.saveAndFlush(slot2);
    }

    @Test
    @DisplayName("Proves production SessionImpactService is wired and discovered as SessionImpactReader")
    void testProductionBeanWiring() {
        assertNotNull(sessionImpactReader);
        assertInstanceOf(SessionImpactService.class, sessionImpactReader);
    }

    @Test
    @DisplayName("SCHEDULED session cancellation preview succeeds with zero impact when no appointments exist")
    void testPreviewCancellationZeroImpact() {
        SessionChangePreview preview = vaccinationSessionService.previewCancellation(scheduledSession.getId());

        assertNotNull(preview);
        assertEquals(SessionChangeType.CANCELLED, preview.changeType());
        assertEquals(0, preview.affectedAppointmentCount());
        assertEquals(0, preview.affectedWaitlistCount());
        assertNotNull(preview.previewToken());
    }

    @Test
    @DisplayName("SCHEDULED session cancellation preview queries real appointments via production adapter")
    void testPreviewCancellationWithActiveAppointments() {
        appointmentRepository.saveAndFlush(new Appointment(201L, testCampaign.getId(), slot1.getId(), AppointmentStatus.CONFIRMED, Instant.now()));
        appointmentRepository.saveAndFlush(new Appointment(202L, testCampaign.getId(), slot2.getId(), AppointmentStatus.CHECKED_IN, Instant.now()));
        appointmentRepository.saveAndFlush(new Appointment(203L, testCampaign.getId(), slot2.getId(), AppointmentStatus.CANCELLED, Instant.now()));

        SessionChangePreview preview = vaccinationSessionService.previewCancellation(scheduledSession.getId());

        assertNotNull(preview);
        assertEquals(SessionChangeType.CANCELLED, preview.changeType());
        assertEquals(2, preview.affectedAppointmentCount(), "Should count 2 active appointments (1 CONFIRMED + 1 CHECKED_IN)");
        assertEquals(0, preview.affectedWaitlistCount());
        assertNotNull(preview.previewToken());
    }

    @Test
    @DisplayName("SCHEDULED session cancellation confirmation succeeds with real production adapter")
    void testConfirmCancellationWithActiveAppointments() {
        appointmentRepository.saveAndFlush(new Appointment(201L, testCampaign.getId(), slot1.getId(), AppointmentStatus.CONFIRMED, Instant.now()));
        appointmentRepository.saveAndFlush(new Appointment(202L, testCampaign.getId(), slot2.getId(), AppointmentStatus.CONFIRMED, Instant.now()));

        SessionChangePreview preview = vaccinationSessionService.previewCancellation(scheduledSession.getId());
        assertEquals(2, preview.affectedAppointmentCount());

        VaccinationSession cancelled = vaccinationSessionService.confirmCancellation(
                scheduledSession.getId(),
                preview.previewToken(),
                "Emergency maintenance closure",
                "staff-actor-001"
        );

        assertNotNull(cancelled);
        assertEquals(SessionLifecycleStatus.CANCELLED, cancelled.getLifecycleStatus());
    }

    @Test
    @DisplayName("SCHEDULED session reschedule preview evaluates real active appointments via production adapter")
    void testPreviewRescheduleWithActiveAppointments() {
        appointmentRepository.saveAndFlush(new Appointment(201L, testCampaign.getId(), slot1.getId(), AppointmentStatus.CONFIRMED, Instant.now()));

        Instant newStart = Instant.parse("2026-10-15T13:00:00Z");
        Instant newEnd = Instant.parse("2026-10-15T16:00:00Z");
        Instant newClose = Instant.parse("2026-10-15T12:00:00Z");

        SessionChangePreview preview = vaccinationSessionService.previewReschedule(
                scheduledSession.getId(),
                newStart,
                newEnd,
                newClose
        );

        assertNotNull(preview);
        assertEquals(SessionChangeType.RESCHEDULED, preview.changeType());
        assertEquals(1, preview.affectedAppointmentCount());
        assertEquals(0, preview.affectedWaitlistCount());
    }
}
