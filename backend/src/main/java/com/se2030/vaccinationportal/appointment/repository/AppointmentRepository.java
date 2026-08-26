package com.se2030.vaccinationportal.appointment.repository;

import com.se2030.vaccinationportal.appointment.model.Appointment;
import com.se2030.vaccinationportal.appointment.model.AppointmentStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link Appointment} persistence operations.
 * Provides query methods for citizen active appointment validation and slot capacity checking.
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    /**
     * Find appointments for a given citizen and campaign filtered by status collection.
     * Used to detect existing active appointments (e.g. CONFIRMED, CHECKED_IN).
     *
     * @param citizenId  Identifier of the citizen
     * @param campaignId Identifier of the campaign
     * @param statuses   Collection of statuses to include
     * @return List of matching appointments
     */
    List<Appointment> findByCitizenIdAndCampaignIdAndStatusIn(
            Long citizenId,
            Long campaignId,
            Collection<AppointmentStatus> statuses
    );

    /**
     * Find appointments for a given slot filtered by status collection.
     * Used for time slot capacity, queue, and availability queries.
     *
     * @param slotId   Identifier of the time slot
     * @param statuses Collection of statuses to include
     * @return List of matching appointments
     */
    List<Appointment> findBySlotIdAndStatusIn(
            Long slotId,
            Collection<AppointmentStatus> statuses
    );

    /**
     * Find appointments for a collection of slot IDs filtered by status collection.
     * Supports vaccinator queue queries across all slots of a session (US-26 / US-27).
     *
     * @param slotIds  Collection of time slot IDs
     * @param statuses Collection of statuses to include
     * @return List of matching appointments
     */
    List<Appointment> findBySlotIdInAndStatusIn(
            Collection<Long> slotIds,
            Collection<AppointmentStatus> statuses
    );
}
