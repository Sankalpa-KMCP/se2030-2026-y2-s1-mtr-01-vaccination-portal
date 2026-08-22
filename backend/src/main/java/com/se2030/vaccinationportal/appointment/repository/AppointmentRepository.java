package com.se2030.vaccinationportal.appointment.repository;

import com.se2030.vaccinationportal.appointment.model.Appointment;
import com.se2030.vaccinationportal.appointment.model.AppointmentStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link Appointment} persistence operations.
 * Provides query methods for citizen active appointment validation, slot capacity checking, and session impact evaluation.
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
     * Counts active appointments (CONFIRMED or CHECKED_IN) belonging to all time slots of a given session.
     * Used by MF-03 SessionImpactService to satisfy the cross-module SessionImpactReader port.
     *
     * @param sessionId Identifier of the vaccination session
     * @return Total count of active appointments for the session
     */
    @Query(
            value = "SELECT COUNT(a.id) FROM appointment a " +
                    "WHERE a.slot_id IN (SELECT ts.id FROM time_slots ts WHERE ts.session_id = :sessionId) " +
                    "AND a.status IN ('CONFIRMED', 'CHECKED_IN')",
            nativeQuery = true
    )
    long countActiveAppointmentsBySessionId(@Param("sessionId") Long sessionId);
}
