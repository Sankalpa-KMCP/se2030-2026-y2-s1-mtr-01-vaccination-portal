package com.se2030.vaccinationportal.campaign.service;

import com.se2030.vaccinationportal.campaign.model.OperatingHours;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.repository.OperatingHoursRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Service managing OperatingHours schedules, versioning, conflict enforcement,
 * and parent center write serialization for physical vaccination centers.
 * Defined by canonical specification WBVP-SE2030 / MF-02.
 */
@Service
@Transactional
public class OperatingHoursService {

    private final OperatingHoursRepository operatingHoursRepository;
    private final VaccinationCenterRepository centerRepository;

    public OperatingHoursService(OperatingHoursRepository operatingHoursRepository,
                                 VaccinationCenterRepository centerRepository) {
        this.operatingHoursRepository = operatingHoursRepository;
        this.centerRepository = centerRepository;
    }

    /**
     * Creates and persists a new OperatingHours schedule record for a vaccination center.
     * Serializes concurrent modifications using a pessimistic write lock on the parent center.
     * Validates that the proposed shift does not conflict with existing schedules on the same weekday.
     *
     * @param centerId      ID of the parent VaccinationCenter
     * @param dayOfWeek     Day of the week
     * @param opening       Shift opening time
     * @param closing       Shift closing time
     * @param effectiveFrom Effective start date (inclusive)
     * @param effectiveTo   Effective end date (inclusive, nullable for open-ended)
     * @return Persisted OperatingHours entity
     */
    public OperatingHours createOperatingHours(Long centerId,
                                               DayOfWeek dayOfWeek,
                                               LocalTime opening,
                                               LocalTime closing,
                                               LocalDate effectiveFrom,
                                               LocalDate effectiveTo) {
        if (centerId == null) {
            throw new IllegalArgumentException("Center ID is required");
        }

        VaccinationCenter center = centerRepository.findByIdForUpdate(centerId)
                .orElseThrow(() -> new IllegalArgumentException("Vaccination center not found with ID: " + centerId));

        OperatingHours.validateParameters(center, dayOfWeek, opening, closing, effectiveFrom, effectiveTo);

        List<OperatingHours> siblings = operatingHoursRepository.findByCenterIdAndDayOfWeek(centerId, dayOfWeek);
        validateNoConflict(siblings, null, opening, closing, effectiveFrom, effectiveTo);

        OperatingHours opHours = new OperatingHours(
                center,
                dayOfWeek,
                opening,
                closing,
                effectiveFrom,
                effectiveTo
        );

        return operatingHoursRepository.save(opHours);
    }

    /**
     * Updates an existing OperatingHours schedule record.
     * Center ownership is immutable; schedule attributes (dayOfWeek, times, effective dates) are editable.
     * Locks the parent center prior to loading authoritative target state to eliminate stale mutation windows.
     *
     * @param id            OperatingHours ID
     * @param dayOfWeek     Updated day of week
     * @param opening       Updated opening time
     * @param closing       Updated closing time
     * @param effectiveFrom Updated effective from date
     * @param effectiveTo   Updated effective to date (nullable)
     * @return Updated OperatingHours entity
     */
    public OperatingHours updateOperatingHours(Long id,
                                               DayOfWeek dayOfWeek,
                                               LocalTime opening,
                                               LocalTime closing,
                                               LocalDate effectiveFrom,
                                               LocalDate effectiveTo) {
        if (id == null) {
            throw new IllegalArgumentException("Operating hours ID is required");
        }

        Long centerId = operatingHoursRepository.findCenterIdById(id)
                .orElseThrow(() -> new IllegalArgumentException("Operating hours not found with ID: " + id));

        centerRepository.findByIdForUpdate(centerId)
                .orElseThrow(() -> new IllegalArgumentException("Vaccination center not found with ID: " + centerId));

        OperatingHours target = operatingHoursRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Operating hours not found with ID: " + id));

        OperatingHours.validateParameters(target.getCenter(), dayOfWeek, opening, closing, effectiveFrom, effectiveTo);

        List<OperatingHours> siblings = operatingHoursRepository.findByCenterIdAndDayOfWeek(centerId, dayOfWeek);
        validateNoConflict(siblings, id, opening, closing, effectiveFrom, effectiveTo);

        target.updateSchedule(dayOfWeek, opening, closing, effectiveFrom, effectiveTo);

        return operatingHoursRepository.save(target);
    }

    /**
     * Ends or shortens the effective period of an existing OperatingHours schedule record.
     * This is strictly a deactivation/shortening operation, never an extension.
     * Locks the parent center prior to mutating the target entity.
     *
     * @param id      OperatingHours ID
     * @param endDate New effective end date (inclusive)
     * @return Updated OperatingHours entity
     */
    public OperatingHours endDateOperatingHours(Long id, LocalDate endDate) {
        if (id == null) {
            throw new IllegalArgumentException("Operating hours ID is required");
        }
        if (endDate == null) {
            throw new IllegalArgumentException("End date is required");
        }

        Long centerId = operatingHoursRepository.findCenterIdById(id)
                .orElseThrow(() -> new IllegalArgumentException("Operating hours not found with ID: " + id));

        centerRepository.findByIdForUpdate(centerId)
                .orElseThrow(() -> new IllegalArgumentException("Vaccination center not found with ID: " + centerId));

        OperatingHours target = operatingHoursRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Operating hours not found with ID: " + id));

        if (endDate.isBefore(target.getEffectiveFrom())) {
            throw new IllegalArgumentException("End date (" + endDate + ") cannot be before effective from date (" + target.getEffectiveFrom() + ")");
        }

        if (target.getEffectiveTo() != null && endDate.isAfter(target.getEffectiveTo())) {
            throw new IllegalArgumentException("End date (" + endDate + ") cannot extend beyond current effective to date (" + target.getEffectiveTo() + "). Use updateSchedule for schedule extensions.");
        }

        target.endOn(endDate);

        return operatingHoursRepository.save(target);
    }

    /**
     * Retrieves an OperatingHours record by its primary key ID.
     *
     * @param id OperatingHours ID
     * @return Optional containing the OperatingHours if found
     */
    @Transactional(readOnly = true)
    public Optional<OperatingHours> getOperatingHoursById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Operating hours ID is required");
        }
        return operatingHoursRepository.findById(id);
    }

    /**
     * Lists all operating hours for a vaccination center sorted deterministically in Java:
     * 1. Day of week (Monday=1 through Sunday=7)
     * 2. Opening time ascending
     * 3. Effective from date ascending
     * 4. Primary key ID tie-breaker
     *
     * @param centerId ID of the VaccinationCenter
     * @return Deterministically ordered list of operating hours
     */
    @Transactional(readOnly = true)
    public List<OperatingHours> getOperatingHoursByCenter(Long centerId) {
        if (centerId == null) {
            throw new IllegalArgumentException("Center ID is required");
        }
        List<OperatingHours> list = operatingHoursRepository.findByCenterId(centerId);
        list.sort(Comparator.comparingInt((OperatingHours oh) -> oh.getDayOfWeek().getValue())
                .thenComparing(OperatingHours::getOpening)
                .thenComparing(OperatingHours::getEffectiveFrom)
                .thenComparing(OperatingHours::getId, Comparator.nullsLast(Comparator.naturalOrder())));
        return list;
    }

    /**
     * Retrieves all operating hours effective for a center on a specific calendar date.
     * Derives the day of the week from the date and queries effective shifts ordered by opening time ascending.
     *
     * @param centerId ID of the VaccinationCenter
     * @param date     Evaluation calendar date
     * @return List of effective operating hours ordered by opening time ascending
     */
    @Transactional(readOnly = true)
    public List<OperatingHours> getEffectiveOperatingHours(Long centerId, LocalDate date) {
        if (centerId == null) {
            throw new IllegalArgumentException("Center ID is required");
        }
        if (date == null) {
            throw new IllegalArgumentException("Evaluation date is required");
        }
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return operatingHoursRepository.findEffectiveOperatingHours(centerId, dayOfWeek, date);
    }

    /**
     * Validates that proposed schedule parameters do not conflict with any existing sibling shifts.
     * A multidimensional conflict exists if and only if:
     * 1. Same center (guaranteed by query)
     * 2. Same day of week (guaranteed by query)
     * 3. Effective date intervals overlap: (A.effectiveTo == null || A.effectiveTo >= B.effectiveFrom)
     *    AND (B.effectiveTo == null || B.effectiveTo >= A.effectiveFrom)
     * 4. Half-open time intervals [opening, closing) overlap: A.opening < B.closing AND B.opening < A.closing
     *
     * @param siblings      List of existing sibling OperatingHours for the same center and weekday
     * @param excludeId     OperatingHours ID to exclude from conflict checks (nullable, used during updates)
     * @param opening       Proposed shift opening time
     * @param closing       Proposed shift closing time
     * @param effectiveFrom Proposed shift effective from date
     * @param effectiveTo   Proposed shift effective to date (nullable)
     */
    private void validateNoConflict(List<OperatingHours> siblings,
                                    Long excludeId,
                                    LocalTime opening,
                                    LocalTime closing,
                                    LocalDate effectiveFrom,
                                    LocalDate effectiveTo) {
        for (OperatingHours sibling : siblings) {
            if (excludeId != null && excludeId.equals(sibling.getId())) {
                continue;
            }

            boolean dateOverlap = (effectiveTo == null || !effectiveTo.isBefore(sibling.getEffectiveFrom()))
                    && (sibling.getEffectiveTo() == null || !sibling.getEffectiveTo().isBefore(effectiveFrom));

            boolean timeOverlap = opening.isBefore(sibling.getClosing())
                    && sibling.getOpening().isBefore(closing);

            if (dateOverlap && timeOverlap) {
                throw new IllegalArgumentException(String.format(
                        "Operating hours conflict with existing schedule on %s [%s - %s] effective [%s to %s]",
                        sibling.getDayOfWeek(),
                        sibling.getOpening(),
                        sibling.getClosing(),
                        sibling.getEffectiveFrom(),
                        sibling.getEffectiveTo() != null ? sibling.getEffectiveTo() : "open-ended"
                ));
            }
        }
    }
}
