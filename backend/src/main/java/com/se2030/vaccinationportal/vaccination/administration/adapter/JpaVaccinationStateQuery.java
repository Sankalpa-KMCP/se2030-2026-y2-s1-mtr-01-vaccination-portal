package com.se2030.vaccinationportal.vaccination.administration.adapter;

import com.se2030.vaccinationportal.appointment.api.vaccination.VaccinationStateQuery;
import com.se2030.vaccinationportal.vaccination.administration.repository.VaccinationDraftRepository;
import com.se2030.vaccinationportal.vaccination.administration.repository.VaccinationRecordRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Supplies MF-05 draft/finalization facts through MF-03's read-only integration port. */
@Component
@Transactional(readOnly = true)
public class JpaVaccinationStateQuery implements VaccinationStateQuery {

    private final VaccinationDraftRepository drafts;
    private final VaccinationRecordRepository records;

    public JpaVaccinationStateQuery(
            VaccinationDraftRepository drafts,
            VaccinationRecordRepository records
    ) {
        this.drafts = drafts;
        this.records = records;
    }

    @Override
    public boolean hasDraftForAppointment(Long appointmentId) {
        return appointmentId != null && drafts.existsByAppointmentId(appointmentId);
    }

    @Override
    public boolean hasFinalizedRecordForAppointment(Long appointmentId) {
        return appointmentId != null && records.existsByAppointmentId(appointmentId);
    }
}
