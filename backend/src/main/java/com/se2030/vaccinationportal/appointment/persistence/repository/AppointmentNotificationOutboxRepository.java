package com.se2030.vaccinationportal.appointment.persistence.repository;

import com.se2030.vaccinationportal.appointment.persistence.model.AppointmentNotificationOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentNotificationOutboxRepository extends JpaRepository<AppointmentNotificationOutbox, Long> {
}
