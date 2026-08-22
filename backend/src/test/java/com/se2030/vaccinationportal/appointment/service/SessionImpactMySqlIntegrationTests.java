package com.se2030.vaccinationportal.appointment.service;

import com.se2030.vaccinationportal.appointment.model.Appointment;
import com.se2030.vaccinationportal.appointment.model.AppointmentStatus;
import com.se2030.vaccinationportal.appointment.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.repository.CitizenCampaignBookingGuardRepository;
import com.se2030.vaccinationportal.campaign.model.CampaignCenterAssignment;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.OperatingHours;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.TimeSlot;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.CampaignCenterAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.CenterClosureRepository;
import com.se2030.vaccinationportal.campaign.repository.OperatingHoursRepository;
import com.se2030.vaccinationportal.campaign.repository.SessionChangeEventRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.TimeSlotRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.service.SessionImpactSummary;
import com.se2030.vaccinationportal.testcontainers.AbstractMySqlTestContainersIntegrationTest;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Production-equivalent MySQL Testcontainers integration tests verifying that the native cross-table
 * SessionImpact query executes correctly on real MySQL 8.4.3 InnoDB against the live versioned V1/V3 SQL schema.
 *
 * <p>Validates:
 * <ul>
 *   <li>Native subquery resolution between `appointment` and `time_slots` tables.</li>
 *   <li>Correct counting of active statuses (CONFIRMED, CHECKED_IN).</li>
 *   <li>Correct exclusion of inactive statuses (CANCELLED, COMPLETED, NO_SHOW).</li>
 *   <li>Strict session-level isolation (excluding appointments on time slots of other sessions).</li>
 *   <li>Authoritative SessionImpactService adapter execution and SessionImpactSummary assembly.</li>
 * </ul>
 */
class SessionImpactMySqlIntegrationTests extends AbstractMySqlTestContainersIntegrationTest {

    @Autowired
    private SessionImpactService sessionImpactService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private CitizenCampaignBookingGuardRepository bookingGuardRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private SessionChangeEventRepository sessionChangeEventRepository;

    @Autowired
    private VaccinationSessionRepository sessionRepository;

    @Autowired
    private CampaignCenterAssignmentRepository campaignCenterAssignmentRepository;

    @Autowired
    private CenterClosureRepository closureRepository;

    @Autowired
    private OperatingHoursRepository operatingHoursRepository;

    @Autowired
    private VaccinationCampaignRepository campaignRepository;

    @Autowired
    private VaccinationCenterRepository centerRepository;

    @Autowired
    private VaccineTypeRepository vaccineTypeRepository;

    @Autowired
    private TargetGroupRepository targetGroupRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;
    private VaccinationCampaign campaign;
    private VaccinationCenter center;
    private VaccinationSession sessionA;
    private VaccinationSession sessionB;
    private TimeSlot slotA1;
    private TimeSlot slotA2;
    private TimeSlot slotB1;

    @BeforeEach
    void setUp() throws Exception {
        assertConnectedToRealMySql();
        transactionTemplate = new TransactionTemplate(transactionManager);

        cleanupDatabase();

        VaccineType vax = vaccineTypeRepository.save(new VaccineType("VAX-MYSQL-IMP", "MySQL Impact Vaccine"));
        TargetGroup tg = targetGroupRepository.save(new TargetGroup("TG-MYSQL-IMP", "MySQL Impact Target Group"));

        LocalDate startDate = LocalDate.now().minusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(60);
        Instant regStart = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant regEnd = Instant.now().plus(60, ChronoUnit.DAYS);

        campaign = campaignRepository.save(new VaccinationCampaign(
                "CMP-MYSQL-IMP",
                "MySQL Session Impact Campaign",
                "Testing real MySQL subquery execution",
                vax,
                tg,
                startDate,
                endDate,
                regStart,
                regEnd
        ));

        center = centerRepository.save(new VaccinationCenter(
                "CTR-MYSQL-IMP",
                "MySQL Impact Center",
                "100 Database Highway",
                "+1234567890",
                CenterStatus.ACTIVE,
                100,
                50,
                "UTC"
        ));

        campaignCenterAssignmentRepository.save(new CampaignCenterAssignment(
                campaign,
                center,
                startDate,
                endDate.plusDays(1)
        ));

        for (DayOfWeek dow : DayOfWeek.values()) {
            operatingHoursRepository.save(new OperatingHours(
                    center,
                    dow,
                    LocalTime.of(8, 0),
                    LocalTime.of(18, 0),
                    startDate.minusMonths(1),
                    null
            ));
        }

        Instant baseStartA = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant baseEndA = baseStartA.plus(6, ChronoUnit.HOURS);
        sessionA = new VaccinationSession(
                campaign,
                center,
                baseStartA,
                baseEndA,
                baseStartA.minus(2, ChronoUnit.HOURS),
                "UTC"
        );
        sessionA.schedule();
        sessionA = sessionRepository.save(sessionA);

        Instant baseStartB = Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant baseEndB = baseStartB.plus(6, ChronoUnit.HOURS);
        sessionB = new VaccinationSession(
                campaign,
                center,
                baseStartB,
                baseEndB,
                baseStartB.minus(2, ChronoUnit.HOURS),
                "UTC"
        );
        sessionB.schedule();
        sessionB = sessionRepository.save(sessionB);

        slotA1 = timeSlotRepository.save(new TimeSlot(sessionA, baseStartA, baseStartA.plus(2, ChronoUnit.HOURS), 10));
        slotA2 = timeSlotRepository.save(new TimeSlot(sessionA, baseStartA.plus(2, ChronoUnit.HOURS), baseEndA, 10));
        slotB1 = timeSlotRepository.save(new TimeSlot(sessionB, baseStartB, baseEndB, 20));
    }

    @Test
    @Timeout(30)
    @DisplayName("Executes native SessionImpact count query on real MySQL with cross-slot and status filtering")
    void testCountActiveAppointmentsOnRealMySql() {
        // Appointments belonging to Session A
        appointmentRepository.save(new Appointment(1001L, campaign.getId(), slotA1.getId(), AppointmentStatus.CONFIRMED, Instant.now()));
        appointmentRepository.save(new Appointment(1002L, campaign.getId(), slotA1.getId(), AppointmentStatus.CHECKED_IN, Instant.now()));
        appointmentRepository.save(new Appointment(1003L, campaign.getId(), slotA2.getId(), AppointmentStatus.CANCELLED, Instant.now()));
        appointmentRepository.save(new Appointment(1004L, campaign.getId(), slotA2.getId(), AppointmentStatus.COMPLETED, Instant.now()));
        appointmentRepository.save(new Appointment(1005L, campaign.getId(), slotA2.getId(), AppointmentStatus.NO_SHOW, Instant.now()));

        // Appointment belonging to Session B (isolated)
        appointmentRepository.save(new Appointment(1006L, campaign.getId(), slotB1.getId(), AppointmentStatus.CONFIRMED, Instant.now()));

        // 1. Direct repository native query execution on real MySQL
        long sessionACount = appointmentRepository.countActiveAppointmentsBySessionId(sessionA.getId());
        assertEquals(2L, sessionACount, "Session A must count exactly 2 active appointments (CONFIRMED + CHECKED_IN)");

        long sessionBCount = appointmentRepository.countActiveAppointmentsBySessionId(sessionB.getId());
        assertEquals(1L, sessionBCount, "Session B must count exactly 1 active appointment");

        // 2. Production SessionImpactService adapter execution on real MySQL
        SessionImpactSummary impactA = sessionImpactService.getSessionImpact(sessionA.getId());
        assertNotNull(impactA);
        assertEquals(2, impactA.affectedAppointmentCount());
        assertEquals(0, impactA.affectedWaitlistCount());

        SessionImpactSummary impactB = sessionImpactService.getSessionImpact(sessionB.getId());
        assertNotNull(impactB);
        assertEquals(1, impactB.affectedAppointmentCount());
        assertEquals(0, impactB.affectedWaitlistCount());
    }

    @Test
    @Timeout(30)
    @DisplayName("Verifies unknown session ID returns zero impact on real MySQL")
    void testUnknownSessionIdOnRealMySql() {
        long count = appointmentRepository.countActiveAppointmentsBySessionId(999999L);
        assertEquals(0L, count);

        SessionImpactSummary impact = sessionImpactService.getSessionImpact(999999L);
        assertNotNull(impact);
        assertEquals(0, impact.affectedAppointmentCount());
        assertEquals(0, impact.affectedWaitlistCount());
    }

    @AfterEach
    void tearDown() {
        if (transactionTemplate != null) {
            cleanupDatabase();
        }
    }

    private void cleanupDatabase() {
        transactionTemplate.executeWithoutResult(status -> {
            appointmentRepository.deleteAllInBatch();
            bookingGuardRepository.deleteAllInBatch();
            timeSlotRepository.deleteAllInBatch();
            sessionChangeEventRepository.deleteAllInBatch();
            sessionRepository.deleteAllInBatch();
            campaignCenterAssignmentRepository.deleteAllInBatch();
            closureRepository.deleteAllInBatch();
            operatingHoursRepository.deleteAllInBatch();
            campaignRepository.deleteAllInBatch();
            centerRepository.deleteAllInBatch();
            targetGroupRepository.deleteAllInBatch();
            vaccineTypeRepository.deleteAllInBatch();
        });
    }
}
