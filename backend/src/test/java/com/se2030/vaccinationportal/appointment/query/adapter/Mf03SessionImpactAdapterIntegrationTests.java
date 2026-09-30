package com.se2030.vaccinationportal.appointment.query.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.se2030.vaccinationportal.appointment.persistence.model.Appointment;
import com.se2030.vaccinationportal.appointment.persistence.model.AppointmentStatus;
import com.se2030.vaccinationportal.appointment.persistence.model.WaitlistEntry;
import com.se2030.vaccinationportal.appointment.persistence.model.WaitlistStatus;
import com.se2030.vaccinationportal.appointment.persistence.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.persistence.repository.WaitlistRepository;
import com.se2030.vaccinationportal.campaign.CampaignTestFixtures;
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
import com.se2030.vaccinationportal.campaign.service.SessionImpactReader;
import com.se2030.vaccinationportal.campaign.service.SessionImpactSummary;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Mf03SessionImpactAdapterIntegrationTests {

    @Autowired private ApplicationContext applicationContext;
    @Autowired private SessionImpactReader impactReader;
    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private WaitlistRepository waitlistRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private VaccinationSessionRepository sessionRepository;
    @Autowired private TimeSlotRepository timeSlotRepository;

    @Test
    void productionContextProvidesExactlyOneReader() {
        var readers = applicationContext.getBeansOfType(SessionImpactReader.class);
        assertEquals(1, readers.size());
        assertInstanceOf(Mf03SessionImpactAdapter.class, impactReader);
    }

    @Test
    void countsOnlyActiveAppointmentsAndWaitingEntriesForTheRequestedSession() {
        VaccineType vaccine = vaccineTypeRepository.saveAndFlush(new VaccineType("IMPACT-VAX", "Impact Vaccine"));
        TargetGroup group = targetGroupRepository.saveAndFlush(new TargetGroup("IMPACT-GROUP", "Impact Group"));
        VaccinationCampaign campaign = campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                "IMPACT-CAMPAIGN", "Impact Campaign", "Session impact test", vaccine, group,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
                Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-10-30T00:00:00Z"),
                CampaignStatus.DRAFT));
        VaccinationCenter center = centerRepository.saveAndFlush(new VaccinationCenter(
                "IMPACT-CENTER", "Impact Center", "100 Main Street", "+94110000000",
                CenterStatus.ACTIVE, 100, 100, "UTC"));

        Instant start = Instant.parse("2026-10-15T09:00:00Z");
        VaccinationSession target = sessionRepository.saveAndFlush(new VaccinationSession(
                campaign, center, start, start.plusSeconds(10800), start.minusSeconds(3600), "UTC"));
        VaccinationSession other = sessionRepository.saveAndFlush(new VaccinationSession(
                campaign, center, start.plusSeconds(86400), start.plusSeconds(97200),
                start.plusSeconds(82800), "UTC"));
        assertEquals(new SessionImpactSummary(0, 0), impactReader.getSessionImpact(target.getId()));

        TimeSlot first = timeSlotRepository.saveAndFlush(new TimeSlot(target, start, start.plusSeconds(3600), 20));
        TimeSlot second = timeSlotRepository.saveAndFlush(new TimeSlot(
                target, start.plusSeconds(3600), start.plusSeconds(7200), 20));
        TimeSlot elsewhere = timeSlotRepository.saveAndFlush(new TimeSlot(
                other, start.plusSeconds(86400), start.plusSeconds(90000), 20));
        assertEquals(0, impactReader.getSessionImpact(target.getId()).affectedAppointmentCount());

        appointmentRepository.saveAndFlush(new Appointment(101L, campaign.getId(), first.getId(), AppointmentStatus.CONFIRMED, start));
        assertEquals(1, impactReader.getSessionImpact(target.getId()).affectedAppointmentCount());
        appointmentRepository.saveAndFlush(new Appointment(102L, campaign.getId(), second.getId(), AppointmentStatus.CHECKED_IN, start));
        assertEquals(2, impactReader.getSessionImpact(target.getId()).affectedAppointmentCount());
        appointmentRepository.saveAndFlush(new Appointment(103L, campaign.getId(), first.getId(), AppointmentStatus.COMPLETED, start));
        assertEquals(2, impactReader.getSessionImpact(target.getId()).affectedAppointmentCount());
        appointmentRepository.saveAndFlush(new Appointment(104L, campaign.getId(), first.getId(), AppointmentStatus.CANCELLED, start));
        assertEquals(2, impactReader.getSessionImpact(target.getId()).affectedAppointmentCount());
        appointmentRepository.saveAndFlush(new Appointment(105L, campaign.getId(), second.getId(), AppointmentStatus.NO_SHOW, start));
        assertEquals(2, impactReader.getSessionImpact(target.getId()).affectedAppointmentCount());
        appointmentRepository.saveAndFlush(new Appointment(106L, campaign.getId(), elsewhere.getId(), AppointmentStatus.CONFIRMED, start));
        assertEquals(2, impactReader.getSessionImpact(target.getId()).affectedAppointmentCount());
        assertEquals(1, impactReader.getSessionImpact(other.getId()).affectedAppointmentCount());

        long citizenId = 201L;
        for (WaitlistStatus status : WaitlistStatus.values()) {
            waitlistRepository.saveAndFlush(new WaitlistEntry(citizenId++, campaign.getId(), target.getId(), status, start));
        }
        waitlistRepository.saveAndFlush(new WaitlistEntry(citizenId, campaign.getId(), other.getId(), WaitlistStatus.WAITING, start));

        assertEquals(new SessionImpactSummary(2, 1), impactReader.getSessionImpact(target.getId()));
        assertEquals(new SessionImpactSummary(1, 1), impactReader.getSessionImpact(other.getId()));
        assertEquals(new SessionImpactSummary(0, 0), impactReader.getSessionImpact(Long.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> impactReader.getSessionImpact(null));
    }
}
