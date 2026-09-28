package com.se2030.vaccinationportal.vaccination.integration;

import com.se2030.vaccinationportal.appointment.model.AppointmentStatus;
import com.se2030.vaccinationportal.appointment.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.service.CheckInService;
import com.se2030.vaccinationportal.campaign.repository.TimeSlotRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.vaccination.dependency.AppointmentValidationPort;
import com.se2030.vaccinationportal.vaccination.dependency.AppointmentValidationSnapshot;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Resolves the minimal MF-03 appointment state required by MF-05 validation and finalization.
 * The campaign vaccine type is resolved from the authoritative campaign record (never from
 * client-supplied data) so finalization can verify batch compatibility.
 */
@Component
public class Mf03AppointmentValidationAdapter implements AppointmentValidationPort {

    private final AppointmentRepository appointmentRepository;
    private final CheckInService checkInService;
    private final TimeSlotRepository timeSlotRepository;
    private final VaccinationCampaignRepository campaignRepository;

    @Autowired
    public Mf03AppointmentValidationAdapter(
            AppointmentRepository appointmentRepository,
            CheckInService checkInService,
            TimeSlotRepository timeSlotRepository,
            VaccinationCampaignRepository campaignRepository
    ) {
        this.appointmentRepository = appointmentRepository;
        this.checkInService = checkInService;
        this.timeSlotRepository = timeSlotRepository;
        this.campaignRepository = campaignRepository;
    }

    public Mf03AppointmentValidationAdapter(AppointmentRepository appointmentRepository) {
        this(appointmentRepository, null, null, null);
    }

    @Override
    public Optional<AppointmentValidationSnapshot> findByAppointmentId(Long appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .map(appointment -> {
                    Long centerId = null;
                    if (timeSlotRepository != null && appointment.getSlotId() != null) {
                        centerId = timeSlotRepository.findById(appointment.getSlotId())
                                .map(slot -> slot.getSession() != null && slot.getSession().getCenter() != null
                                        ? slot.getSession().getCenter().getId()
                                        : null)
                                .orElse(null);
                    }
                    Long campaignVaccineTypeId = null;
                    if (campaignRepository != null && appointment.getCampaignId() != null) {
                        campaignVaccineTypeId = campaignRepository.findById(appointment.getCampaignId())
                                .map(campaign -> campaign.getVaccineType() != null
                                        ? campaign.getVaccineType().getId()
                                        : null)
                                .orElse(null);
                    }
                    return new AppointmentValidationSnapshot(
                            appointment.getId(),
                            appointment.getStatus() == AppointmentStatus.CHECKED_IN,
                            appointment.getCitizenId(),
                            appointment.getCampaignId(),
                            appointment.getSlotId(),
                            centerId,
                            campaignVaccineTypeId
                    );
                });
    }

    @Override
    public void completeAppointment(Long appointmentId) {
        if (checkInService != null) {
            checkInService.completeAppointment(appointmentId);
        } else {
            appointmentRepository.findById(appointmentId).ifPresent(appointment -> {
                appointment.complete();
                appointmentRepository.save(appointment);
            });
        }
    }
}
