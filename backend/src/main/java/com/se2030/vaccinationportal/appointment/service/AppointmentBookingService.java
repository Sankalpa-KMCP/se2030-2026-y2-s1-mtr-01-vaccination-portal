package com.se2030.vaccinationportal.appointment.service;

import com.se2030.vaccinationportal.appointment.dependency.EligibilityLookupPort;
import com.se2030.vaccinationportal.appointment.dependency.EligibilityStatus;
import com.se2030.vaccinationportal.appointment.dependency.SlotCapacityPort;
import com.se2030.vaccinationportal.appointment.dependency.SlotReservationResult;
import com.se2030.vaccinationportal.appointment.exception.DuplicateActiveAppointmentException;
import com.se2030.vaccinationportal.appointment.exception.NotEligibleException;
import com.se2030.vaccinationportal.appointment.exception.SlotFullException;
import com.se2030.vaccinationportal.appointment.model.Appointment;
import com.se2030.vaccinationportal.appointment.model.AppointmentStatus;
import com.se2030.vaccinationportal.appointment.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.repository.CitizenCampaignBookingGuardRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service orchestrating atomic appointment booking workflows for citizens.
 * Enforces guard-row locking, cross-module eligibility lookup, active appointment duplicate checks,
 * and time-slot capacity reservations within one transactional boundary.
 */
@Service
@Transactional
public class AppointmentBookingService {

    private final AppointmentRepository appointmentRepository;
    private final CitizenCampaignBookingGuardRepository guardRepository;
    private final EligibilityLookupPort eligibilityLookupPort;
    private final SlotCapacityPort slotCapacityPort;

    @Autowired
    public AppointmentBookingService(
            AppointmentRepository appointmentRepository,
            CitizenCampaignBookingGuardRepository guardRepository,
            @Autowired(required = false) EligibilityLookupPort eligibilityLookupPort,
            @Autowired(required = false) SlotCapacityPort slotCapacityPort
    ) {
        this.appointmentRepository = appointmentRepository;
        this.guardRepository = guardRepository;
        this.eligibilityLookupPort = eligibilityLookupPort;
        this.slotCapacityPort = slotCapacityPort;
    }

    /**
     * Atomically books an appointment for a citizen in a target vaccination campaign time slot.
     * <p>
     * Order of execution per business rules:
     * <ol>
     *   <li>Acquire pessimistic write lock on citizen-campaign booking guard row.</li>
     *   <li>Check MF-01 eligibility status via {@link EligibilityLookupPort} (MF03-BR-01).</li>
     *   <li>Check existing active appointments via {@link AppointmentRepository} (MF03-BR-02).</li>
     *   <li>Reserve capacity on MF-02 time slot via {@link SlotCapacityPort} (MF03-BR-03).</li>
     *   <li>Persist new {@link Appointment} in CONFIRMED status.</li>
     * </ol>
     * </p>
     *
     * @param citizenId  Identifier of the booking citizen
     * @param campaignId Identifier of the target campaign
     * @param slotId     Identifier of the selected time slot
     * @return Saved {@link Appointment} entity
     * @throws NotEligibleException                if citizen is not ELIGIBLE in MF-01
     * @throws DuplicateActiveAppointmentException if citizen already has a CONFIRMED or CHECKED_IN appointment for the campaign
     * @throws SlotFullException                   if time slot capacity reservation fails in MF-02
     * @throws IllegalArgumentException            if any required parameter is null
     */
    public Appointment bookAppointment(Long citizenId, Long campaignId, Long slotId) {
        if (citizenId == null) {
            throw new IllegalArgumentException("Citizen ID is required");
        }
        if (campaignId == null) {
            throw new IllegalArgumentException("Campaign ID is required");
        }
        if (slotId == null) {
            throw new IllegalArgumentException("Slot ID is required");
        }

        // Step 1a: Acquire pessimistic write lock on citizen-campaign booking guard row
        guardRepository.insertGuardIfAbsent(citizenId, campaignId);
        guardRepository.findByCitizenIdAndCampaignIdForUpdate(citizenId, campaignId)
                .orElseThrow(() -> new IllegalStateException("Failed to acquire booking guard lock for citizen " + citizenId + " and campaign " + campaignId));

        // Step 1b: Check MF-01 eligibility
        if (eligibilityLookupPort == null) {
            throw new IllegalStateException("EligibilityLookupPort implementation (MF-01) is unavailable");
        }
        EligibilityStatus eligibilityStatus = eligibilityLookupPort.getCurrentEligibility(citizenId, campaignId);
        if (eligibilityStatus != EligibilityStatus.ELIGIBLE) {
            throw new NotEligibleException("Citizen " + citizenId + " is not eligible for campaign " + campaignId + ". Eligibility status: " + eligibilityStatus);
        }

        // Step 1c: Check active appointment duplicates (CONFIRMED or CHECKED_IN)
        List<Appointment> existingActiveAppointments = appointmentRepository.findByCitizenIdAndCampaignIdAndStatusIn(
                citizenId,
                campaignId,
                List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.CHECKED_IN)
        );
        if (!existingActiveAppointments.isEmpty()) {
            throw new DuplicateActiveAppointmentException("Citizen " + citizenId + " already has an active appointment for campaign " + campaignId);
        }

        // Step 1d: Reserve capacity on MF-02 time slot
        if (slotCapacityPort == null) {
            throw new IllegalStateException("SlotCapacityPort implementation (MF-02) is unavailable");
        }
        SlotReservationResult reservationResult = slotCapacityPort.reserveCapacity(slotId);
        if (reservationResult != SlotReservationResult.SUCCESS) {
            throw new SlotFullException("Time slot " + slotId + " capacity reservation failed with result: " + reservationResult);
        }

        // Step 1e: Persist new CONFIRMED appointment
        Appointment appointment = new Appointment(citizenId, campaignId, slotId, AppointmentStatus.CONFIRMED, Instant.now());
        return appointmentRepository.save(appointment);
    }
}
