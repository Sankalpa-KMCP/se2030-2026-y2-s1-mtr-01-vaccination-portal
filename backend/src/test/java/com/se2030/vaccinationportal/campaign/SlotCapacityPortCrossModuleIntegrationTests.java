package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.appointment.dependency.EligibilityLookupPort;
import com.se2030.vaccinationportal.appointment.dependency.EligibilityStatus;
import com.se2030.vaccinationportal.appointment.dependency.SlotCapacityPort;
import com.se2030.vaccinationportal.appointment.model.Appointment;
import com.se2030.vaccinationportal.appointment.model.AppointmentStatus;
import com.se2030.vaccinationportal.appointment.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.service.AppointmentBookingService;
import com.se2030.vaccinationportal.appointment.service.AppointmentCancellationService;
import com.se2030.vaccinationportal.appointment.service.TestEligibilityLookupPortDouble;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        appointmentRepository.deleteAllInBatch();
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
}
