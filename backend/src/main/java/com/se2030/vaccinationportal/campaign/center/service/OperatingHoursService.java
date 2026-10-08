package com.se2030.vaccinationportal.campaign.center.service;

import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.center.model.OperatingHours;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.session.planning.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.center.repository.OperatingHoursRepository;
import com.se2030.vaccinationportal.campaign.center.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.VaccinationSessionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manages dated opening hours and checks that existing sessions still fit. */
@Service
@Transactional
public class OperatingHoursService {

    private static final List<SessionLifecycleStatus> BLOCKING_STATUSES = List.of(
            SessionLifecycleStatus.DRAFT,
            SessionLifecycleStatus.SCHEDULED,
            SessionLifecycleStatus.IN_PROGRESS
    );

    private final OperatingHoursRepository operatingHoursRepository;
    private final VaccinationCenterRepository centerRepository;
    private final VaccinationSessionRepository sessionRepository;
    private final Clock clock;
    @PersistenceContext private EntityManager entityManager;

    public OperatingHoursService(OperatingHoursRepository operatingHoursRepository,
                                 VaccinationCenterRepository centerRepository,
                                 VaccinationSessionRepository sessionRepository,
                                 Clock clock) {
        this.operatingHoursRepository = operatingHoursRepository;
        this.centerRepository = centerRepository;
        this.sessionRepository = sessionRepository;
        this.clock = clock;
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
            throw new ResourceNotFoundException("Center ID is required");
        }

        VaccinationCenter center = centerRepository.findByIdForUpdate(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination center not found with ID: " + centerId));
        if (entityManager != null) {
            entityManager.refresh(center);
        }

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
     * Updates an existing OperatingHours schedule record with parent ownership validation.
     *
     * @param centerId      Parent VaccinationCenter ID
     * @param id            OperatingHours ID
     * @param dayOfWeek     Updated day of week
     * @param opening       Updated opening time
     * @param closing       Updated closing time
     * @param effectiveFrom Updated effective from date
     * @param effectiveTo   Updated effective to date (nullable)
     * @return Updated OperatingHours entity
     */
    public OperatingHours updateOperatingHours(Long centerId,
                                               Long id,
                                               DayOfWeek dayOfWeek,
                                               LocalTime opening,
                                               LocalTime closing,
                                               LocalDate effectiveFrom,
                                               LocalDate effectiveTo) {
        if (centerId == null) {
            throw new ResourceNotFoundException("Center ID is required");
        }
        if (id == null) {
            throw new ResourceNotFoundException("Operating hours ID is required");
        }

        Long actualCenterId = operatingHoursRepository.findCenterIdById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Operating hours not found with ID: " + id));

        if (!centerId.equals(actualCenterId)) {
            throw new ResourceNotFoundException("Operating hours not found with ID: " + id + " for center ID: " + centerId);
        }

        return updateOperatingHours(id, dayOfWeek, opening, closing, effectiveFrom, effectiveTo);
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
            throw new ResourceNotFoundException("Operating hours ID is required");
        }

        Long centerId = operatingHoursRepository.findCenterIdById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Operating hours not found with ID: " + id));

        VaccinationCenter center = centerRepository.findByIdForUpdate(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination center not found with ID: " + centerId));
        if (entityManager != null) {
            entityManager.refresh(center);
        }

        OperatingHours target = operatingHoursRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Operating hours not found with ID: " + id));

        OperatingHours.validateParameters(target.getCenter(), dayOfWeek, opening, closing, effectiveFrom, effectiveTo);

        List<OperatingHours> siblings = operatingHoursRepository.findByCenterIdAndDayOfWeek(centerId, dayOfWeek);
        validateNoConflict(siblings, id, opening, closing, effectiveFrom, effectiveTo);

        List<OperatingHours> allHours = operatingHoursRepository.findByCenterId(centerId);
        List<OperatingHours> simulatedHours = allHours.stream()
                .map(oh -> id.equals(oh.getId())
                        ? new OperatingHours(center, dayOfWeek, opening, closing, effectiveFrom, effectiveTo)
                        : oh)
                .toList();

        validateOperatingHoursReverseImpact(centerId, center, simulatedHours);

        target.updateSchedule(dayOfWeek, opening, closing, effectiveFrom, effectiveTo);

        return operatingHoursRepository.save(target);
    }

    /**
     * Ends or shortens the effective period of an existing OperatingHours schedule record with parent ownership validation.
     *
     * @param centerId Parent VaccinationCenter ID
     * @param id       OperatingHours ID
     * @param endDate  New effective end date (inclusive)
     * @return Updated OperatingHours entity
     */
    public OperatingHours endDateOperatingHours(Long centerId, Long id, LocalDate endDate) {
        if (centerId == null) {
            throw new ResourceNotFoundException("Center ID is required");
        }
        if (id == null) {
            throw new ResourceNotFoundException("Operating hours ID is required");
        }

        Long actualCenterId = operatingHoursRepository.findCenterIdById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Operating hours not found with ID: " + id));

        if (!centerId.equals(actualCenterId)) {
            throw new ResourceNotFoundException("Operating hours not found with ID: " + id + " for center ID: " + centerId);
        }

        return endDateOperatingHours(id, endDate);
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
            throw new ResourceNotFoundException("Operating hours ID is required");
        }
        if (endDate == null) {
            throw new IllegalArgumentException("End date is required");
        }

        Long centerId = operatingHoursRepository.findCenterIdById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Operating hours not found with ID: " + id));

        VaccinationCenter center = centerRepository.findByIdForUpdate(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination center not found with ID: " + centerId));
        if (entityManager != null) {
            entityManager.refresh(center);
        }

        OperatingHours target = operatingHoursRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Operating hours not found with ID: " + id));

        if (endDate.isBefore(target.getEffectiveFrom())) {
            throw new IllegalArgumentException("End date (" + endDate + ") cannot be before effective from date (" + target.getEffectiveFrom() + ")");
        }

        if (target.getEffectiveTo() != null && endDate.isAfter(target.getEffectiveTo())) {
            throw new IllegalArgumentException("End date (" + endDate + ") cannot extend beyond current effective to date (" + target.getEffectiveTo() + "). Use updateSchedule for schedule extensions.");
        }

        List<OperatingHours> allHours = operatingHoursRepository.findByCenterId(centerId);
        List<OperatingHours> simulatedHours = allHours.stream()
                .map(oh -> id.equals(oh.getId())
                        ? new OperatingHours(center, oh.getDayOfWeek(), oh.getOpening(), oh.getClosing(), oh.getEffectiveFrom(), endDate)
                        : oh)
                .toList();

        validateOperatingHoursReverseImpact(centerId, center, simulatedHours);

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
            throw new ResourceNotFoundException("Operating hours ID is required");
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
     * @throws ResourceNotFoundException if parent center does not exist
     */
    @Transactional(readOnly = true)
    public List<OperatingHours> getOperatingHoursByCenter(Long centerId) {
        if (centerId == null) {
            throw new ResourceNotFoundException("Center ID is required");
        }
        if (!centerRepository.existsById(centerId)) {
            throw new ResourceNotFoundException("Vaccination center not found with ID: " + centerId);
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

    private void validateOperatingHoursReverseImpact(
            Long centerId,
            VaccinationCenter center,
            List<OperatingHours> simulatedHours
    ) {
        List<VaccinationSession> blockingSessions = sessionRepository.findBlockingSessionsForCenter(
                centerId,
                BLOCKING_STATUSES,
                clock.instant()
        );
        if (blockingSessions.isEmpty()) {
            return;
        }

        ZoneId zoneId = ZoneId.of(center.getTimezone().trim());

        for (VaccinationSession session : blockingSessions) {
            Instant startAt = session.getStartAt();
            Instant endAt = session.getEndAt();

            LocalDate firstTouchedDate = startAt.atZone(zoneId).toLocalDate();
            LocalDate lastTouchedDate = endAt.minusNanos(1).atZone(zoneId).toLocalDate();

            for (LocalDate date = firstTouchedDate; !date.isAfter(lastTouchedDate); date = date.plusDays(1)) {
                LocalDate evalDate = date;
                ZonedDateTime dayStartZdt = evalDate.atStartOfDay(zoneId);
                ZonedDateTime nextDayStartZdt = evalDate.plusDays(1).atStartOfDay(zoneId);
                Instant dayStart = dayStartZdt.toInstant();
                Instant nextDayStart = nextDayStartZdt.toInstant();

                if (!endAt.isBefore(nextDayStart)) {
                    throw new IllegalArgumentException(
                            "Operating hours change leaves session (" + session.getOccurrenceKey() +
                                    ") extending across midnight on " + evalDate);
                }

                LocalTime requiredStart = startAt.isAfter(dayStart)
                        ? startAt.atZone(zoneId).toLocalTime()
                        : LocalTime.MIN;
                LocalTime requiredEnd = endAt.atZone(zoneId).toLocalTime();

                List<OperatingHours> hoursList = simulatedHours.stream()
                        .filter(oh -> oh.getDayOfWeek() == evalDate.getDayOfWeek())
                        .filter(oh -> !evalDate.isBefore(oh.getEffectiveFrom()))
                        .filter(oh -> oh.getEffectiveTo() == null || !evalDate.isAfter(oh.getEffectiveTo()))
                        .sorted(Comparator.comparing(OperatingHours::getOpening))
                        .toList();

                if (hoursList.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Operating hours change leaves session (" + session.getOccurrenceKey() +
                                    ") without operating hours on " + evalDate + " (" + evalDate.getDayOfWeek() + ")");
                }

                LocalTime coveredUntil = null;
                for (OperatingHours shift : hoursList) {
                    LocalTime opening = shift.getOpening();
                    LocalTime closing = shift.getClosing();

                    if (!closing.isAfter(requiredStart)) {
                        continue;
                    }
                    if (!opening.isBefore(requiredEnd)) {
                        break;
                    }

                    if (coveredUntil == null) {
                        if (opening.isAfter(requiredStart)) {
                            throw new IllegalArgumentException(
                                    "Operating hours change leaves session start time (" + requiredStart +
                                            ") un-covered by center opening time (" + opening + ") on " + date);
                        }
                        coveredUntil = closing;
                    } else {
                        if (opening.isAfter(coveredUntil)) {
                            if (coveredUntil.isBefore(requiredEnd)) {
                                throw new IllegalArgumentException(
                                        "Operating hours change introduces a gap in coverage between " +
                                                coveredUntil + " and " + opening + " on " + date);
                            }
                        }
                        if (closing.isAfter(coveredUntil)) {
                            coveredUntil = closing;
                        }
                    }

                    if (!coveredUntil.isBefore(requiredEnd)) {
                        break;
                    }
                }

                if (coveredUntil == null || coveredUntil.isBefore(requiredEnd)) {
                    throw new IllegalArgumentException(
                            "Operating hours change leaves session end time (" + requiredEnd +
                                    ") extending beyond center operating hours on " + date);
                }
            }
        }
    }
}
