package com.se2030.vaccinationportal.campaign.session.planning.validation;

import com.se2030.vaccinationportal.campaign.center.model.CampaignCenterAssignmentStatus;
import com.se2030.vaccinationportal.campaign.center.model.CenterClosureStatus;
import com.se2030.vaccinationportal.campaign.center.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.center.model.OperatingHours;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.center.repository.CampaignCenterAssignmentRepository;
import com.se2030.vaccinationportal.campaign.center.repository.CenterClosureRepository;
import com.se2030.vaccinationportal.campaign.center.repository.OperatingHoursRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Authoritative write-time validator for concrete vaccination sessions.
 * Evaluates candidate session parameters against locked campaign and center configuration state
 * to ensure compliance with MF02-BR-05 and MF02-BR-06 before persistence.
 */
@Component
public class VaccinationSessionValidator {

    private final CampaignCenterAssignmentRepository assignmentRepository;
    private final OperatingHoursRepository operatingHoursRepository;
    private final CenterClosureRepository closureRepository;
    @PersistenceContext
    private EntityManager entityManager;

    public VaccinationSessionValidator(
            CampaignCenterAssignmentRepository assignmentRepository,
            OperatingHoursRepository operatingHoursRepository,
            CenterClosureRepository closureRepository
    ) {
        this.assignmentRepository = assignmentRepository;
        this.operatingHoursRepository = operatingHoursRepository;
        this.closureRepository = closureRepository;
    }

    /**
     * Validates a candidate session schedule against locked and authoritatively refreshed parent entities
     * and current configuration records.
     *
     * @param campaign       Locked and refreshed VaccinationCampaign
     * @param center         Locked and refreshed VaccinationCenter
     * @param startAt        Candidate session start UTC instant
     * @param endAt          Candidate session end UTC instant
     * @param bookingCloseAt Candidate booking close UTC instant
     * @throws IllegalArgumentException if any validation rule is violated
     */
    public void validateSessionSchedule(
            VaccinationCampaign campaign,
            VaccinationCenter center,
            Instant startAt,
            Instant endAt,
            Instant bookingCloseAt
    ) {
        validateBasicParameters(campaign, center, startAt, endAt, bookingCloseAt);
        validateCenterActive(center);

        ZoneId zoneId = parseZoneId(center.getTimezone());
        ZonedDateTime startZdt = startAt.atZone(zoneId);
        LocalDate firstTouchedDate = startZdt.toLocalDate();
        Instant lastContainedInstant = endAt.minusNanos(1);
        LocalDate lastTouchedDate = lastContainedInstant.atZone(zoneId).toLocalDate();

        validateCampaignServiceDates(campaign, firstTouchedDate, lastTouchedDate);
        validateAssignmentCoverage(campaign, center, firstTouchedDate, lastTouchedDate);
        validateOperatingHoursCoverage(center, zoneId, startAt, endAt, firstTouchedDate, lastTouchedDate);
        validateCenterClosureExclusion(center, startAt, endAt);
    }

    private void validateBasicParameters(
            VaccinationCampaign campaign,
            VaccinationCenter center,
            Instant startAt,
            Instant endAt,
            Instant bookingCloseAt
    ) {
        if (campaign == null) {
            throw new IllegalArgumentException("Campaign is required");
        }
        if (center == null) {
            throw new IllegalArgumentException("Vaccination center is required");
        }
        if (startAt == null) {
            throw new IllegalArgumentException("Session start is required");
        }
        if (endAt == null) {
            throw new IllegalArgumentException("Session end is required");
        }
        if (!startAt.isBefore(endAt)) {
            throw new IllegalArgumentException("Session start must be before session end");
        }
        if (bookingCloseAt == null) {
            throw new IllegalArgumentException("Booking close time is required");
        }
        if (bookingCloseAt.isAfter(startAt)) {
            throw new IllegalArgumentException("Booking close time cannot be after session start");
        }
    }

    private void validateCenterActive(VaccinationCenter center) {
        if (center.getCenterStatus() != CenterStatus.ACTIVE) {
            throw new IllegalArgumentException("Vaccination center must be ACTIVE to host sessions");
        }
    }

    private ZoneId parseZoneId(String timezone) {
        if (timezone == null || timezone.trim().isEmpty()) {
            throw new IllegalArgumentException("Vaccination center timezone is required");
        }
        try {
            return ZoneId.of(timezone.trim());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Vaccination center timezone is invalid: " + timezone, ex);
        }
    }

    private void validateCampaignServiceDates(
            VaccinationCampaign campaign,
            LocalDate firstTouchedDate,
            LocalDate lastTouchedDate
    ) {
        if (firstTouchedDate.isBefore(campaign.getServiceStartDate())) {
            throw new IllegalArgumentException(
                    "Session start date (" + firstTouchedDate + ") is before campaign service start date (" +
                            campaign.getServiceStartDate() + ")");
        }
        if (lastTouchedDate.isAfter(campaign.getServiceEndDate())) {
            throw new IllegalArgumentException(
                    "Session end date (" + lastTouchedDate + ") is after campaign service end date (" +
                            campaign.getServiceEndDate() + ")");
        }
    }

    private void validateAssignmentCoverage(
            VaccinationCampaign campaign,
            VaccinationCenter center,
            LocalDate firstTouchedDate,
            LocalDate lastTouchedDate
    ) {
        LocalDate requiredToExclusive = lastTouchedDate.plusDays(1);
        boolean isCovered = assignmentRepository.existsActiveAssignmentCovering(
                campaign.getId(),
                center.getId(),
                CampaignCenterAssignmentStatus.ACTIVE,
                firstTouchedDate,
                requiredToExclusive
        );
        if (!isCovered) {
            throw new IllegalArgumentException(
                    "Vaccination center is not authorized for campaign on session dates (" +
                            firstTouchedDate + " to " + lastTouchedDate + ")");
        }
    }

    private void validateOperatingHoursCoverage(
            VaccinationCenter center,
            ZoneId zoneId,
            Instant startAt,
            Instant endAt,
            LocalDate firstTouchedDate,
            LocalDate lastTouchedDate
    ) {
        for (LocalDate date = firstTouchedDate; !date.isAfter(lastTouchedDate); date = date.plusDays(1)) {
            ZonedDateTime dayStartZdt = date.atStartOfDay(zoneId);
            ZonedDateTime nextDayStartZdt = date.plusDays(1).atStartOfDay(zoneId);
            Instant dayStart = dayStartZdt.toInstant();
            Instant nextDayStart = nextDayStartZdt.toInstant();

            if (!endAt.isBefore(nextDayStart)) {
                throw new IllegalArgumentException(
                        "Session interval extends across midnight on " + date +
                                " which is not covered by same-day operating hours");
            }

            LocalTime requiredStart = startAt.isAfter(dayStart)
                    ? startAt.atZone(zoneId).toLocalTime()
                    : LocalTime.MIN;
            LocalTime requiredEnd = endAt.atZone(zoneId).toLocalTime();

            List<OperatingHours> hoursList = operatingHoursRepository.findEffectiveOperatingHours(
                    center.getId(),
                    date.getDayOfWeek(),
                    date
            );

            if (entityManager != null) {
                for (OperatingHours oh : hoursList) {
                    if (entityManager.contains(oh)) {
                        entityManager.refresh(oh);
                    }
                }
            }

            if (hoursList.isEmpty()) {
                throw new IllegalArgumentException(
                        "No operating hours configured for center on " + date + " (" + date.getDayOfWeek() + ")");
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
                                "Session start time (" + requiredStart + ") is before center opening time (" +
                                        opening + ") on " + date);
                    }
                    coveredUntil = closing;
                } else {
                    if (opening.isAfter(coveredUntil)) {
                        if (coveredUntil.isBefore(requiredEnd)) {
                            throw new IllegalArgumentException(
                                    "Session interval has a gap in operating hours between " +
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
                        "Session end time (" + requiredEnd + ") extends beyond center operating hours on " + date);
            }
        }
    }

    private void validateCenterClosureExclusion(
            VaccinationCenter center,
            Instant startAt,
            Instant endAt
    ) {
        boolean hasOverlap = closureRepository.existsOverlappingClosure(
                center.getId(),
                CenterClosureStatus.ACTIVE,
                null,
                startAt,
                endAt
        );
        if (hasOverlap) {
            throw new IllegalArgumentException("Session interval overlaps an active center closure");
        }
    }
}
