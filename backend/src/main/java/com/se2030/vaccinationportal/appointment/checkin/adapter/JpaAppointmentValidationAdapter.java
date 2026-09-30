package com.se2030.vaccinationportal.appointment.checkin.adapter;

import com.se2030.vaccinationportal.appointment.api.vaccination.AppointmentValidationPort;
import com.se2030.vaccinationportal.appointment.api.vaccination.AppointmentValidationSnapshot;
import com.se2030.vaccinationportal.appointment.persistence.model.AppointmentStatus;
import com.se2030.vaccinationportal.appointment.persistence.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.checkin.service.CheckInService;
import com.se2030.vaccinationportal.campaign.api.vaccination.CampaignVaccinationReader;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Appointment-owned validation and completion within the caller's transaction. */
@Component
public class JpaAppointmentValidationAdapter implements AppointmentValidationPort {
    private final AppointmentRepository appointments;
    private final CheckInService checkIn;
    private final CampaignVaccinationReader campaigns;

    public JpaAppointmentValidationAdapter(AppointmentRepository appointments, CheckInService checkIn,
            CampaignVaccinationReader campaigns) {
        this.appointments = appointments;
        this.checkIn = checkIn;
        this.campaigns = campaigns;
    }

    @Override
    public Optional<AppointmentValidationSnapshot> findByAppointmentId(Long appointmentId) {
        return appointments.findById(appointmentId).map(appointment -> {
            Long centerId = appointment.getSlotId() == null ? null
                    : campaigns.findCenterIdBySlotId(appointment.getSlotId()).orElse(null);
            Long vaccineTypeId = appointment.getCampaignId() == null ? null
                    : campaigns.findVaccineTypeIdByCampaignId(appointment.getCampaignId()).orElse(null);
            return new AppointmentValidationSnapshot(appointment.getId(),
                    appointment.getStatus() == AppointmentStatus.CHECKED_IN, appointment.getCitizenId(),
                    appointment.getCampaignId(), appointment.getSlotId(), centerId, vaccineTypeId);
        });
    }

    @Override
    public void completeAppointment(Long appointmentId) {
        checkIn.completeAppointment(appointmentId);
    }
}
