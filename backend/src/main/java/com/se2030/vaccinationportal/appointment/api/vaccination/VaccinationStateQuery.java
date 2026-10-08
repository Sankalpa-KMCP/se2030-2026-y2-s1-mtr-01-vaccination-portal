package com.se2030.vaccinationportal.appointment.api.vaccination;

/**
 * MF-03-owned port for checking whether MF-05 state prevents reversing a check-in.
 */
public interface VaccinationStateQuery {

    boolean hasDraftForAppointment(Long appointmentId);

    boolean hasFinalizedRecordForAppointment(Long appointmentId);
}
