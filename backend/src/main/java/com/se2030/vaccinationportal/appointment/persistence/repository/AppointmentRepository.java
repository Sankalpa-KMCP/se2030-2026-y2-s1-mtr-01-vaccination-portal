package com.se2030.vaccinationportal.appointment.persistence.repository;

import com.se2030.vaccinationportal.appointment.persistence.model.Appointment;
import com.se2030.vaccinationportal.appointment.persistence.model.AppointmentStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link Appointment} persistence operations.
 * Provides query methods for citizen active appointment validation, booking history, and slot queue queries.
 *
 * <p><b>User Stories:</b>
 * <ul>
 *   <li>US-03: Search, book, cancel, and reschedule appointments.</li>
 *   <li>US-26: View checked-in appointment queue for clinic arrival and vaccinator intake.</li>
 * </ul>
 *
 * <p><b>Business Rules:</b>
 * <ul>
 *   <li>MF03-BR-02: Active appointment duplicate verification per citizen and campaign.</li>
 *   <li>MF03-BR-10: Query confirmed appointments eligible for check-in.</li>
 *   <li>MF03-BR-11: Validate non-completed status for modification prevention.</li>
 * </ul>
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    /**
     * Find appointments for a given citizen ordered by booked timestamp descending (most recent first).
     *
     * @param citizenId Identifier of the citizen
     * @return List of citizen's appointments across all statuses
     */
    List<Appointment> findByCitizenIdOrderByBookedAtDesc(Long citizenId);

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

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.slotId IN " +
            "(SELECT ts.id FROM TimeSlot ts WHERE ts.session.id = :sessionId) " +
            "AND a.status IN :statuses")
    long countBySessionIdAndStatusIn(
            @Param("sessionId") Long sessionId,
            @Param("statuses") Collection<AppointmentStatus> statuses
    );

}
