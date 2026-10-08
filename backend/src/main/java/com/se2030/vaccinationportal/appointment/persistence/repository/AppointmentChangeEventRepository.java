package com.se2030.vaccinationportal.appointment.persistence.repository;

import com.se2030.vaccinationportal.appointment.persistence.model.AppointmentChangeEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentChangeEventRepository extends JpaRepository<AppointmentChangeEvent, Long> {
    List<AppointmentChangeEvent> findByAppointmentIdOrderByCreatedAtAscIdAsc(Long appointmentId);
}
