package com.se2030.vaccinationportal.campaign.service;

import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing VaccinationCampaign operational lifecycles and reverse configuration-impact rules.
 */
@Service
@Transactional
public class VaccinationCampaignService {

    private static final Set<SessionLifecycleStatus> BLOCKING_SESSION_STATUSES = EnumSet.of(
            SessionLifecycleStatus.DRAFT,
            SessionLifecycleStatus.SCHEDULED,
            SessionLifecycleStatus.IN_PROGRESS
    );

    private final VaccinationCampaignRepository campaignRepository;
    private final VaccinationSessionRepository sessionRepository;
    private final Clock clock;
    @PersistenceContext private EntityManager entityManager;

    public VaccinationCampaignService(
            VaccinationCampaignRepository campaignRepository,
            VaccinationSessionRepository sessionRepository,
            Clock clock
    ) {
        this.campaignRepository = campaignRepository;
        this.sessionRepository = sessionRepository;
        this.clock = clock;
    }


    public VaccinationCampaign createCampaign(
            String code,
            String name,
            String description,
            VaccineType vaccineType,
            TargetGroup targetGroup,
            LocalDate serviceStartDate,
            LocalDate serviceEndDate,
            Instant bookingOpenAt,
            Instant bookingCloseAt,
            CampaignStatus status
    ) {
        String normalizedCode = code != null ? code.trim() : null;
        if (campaignRepository.existsByCode(normalizedCode)) {
            throw new IllegalArgumentException("Campaign already exists with code: " + normalizedCode);
        }

        VaccinationCampaign campaign = new VaccinationCampaign(
                normalizedCode,
                name,
                description,
                vaccineType,
                targetGroup,
                serviceStartDate,
                serviceEndDate,
                bookingOpenAt,
                bookingCloseAt,
                status != null ? status : CampaignStatus.DRAFT
        );

        return campaignRepository.save(campaign);
    }

    public VaccinationCampaign updateServiceDates(
            Long campaignId,
            LocalDate serviceStartDate,
            LocalDate serviceEndDate
    ) {
        if (campaignId == null) {
            throw new IllegalArgumentException("Campaign ID is required");
        }
        if (serviceStartDate == null) {
            throw new IllegalArgumentException("Service start date is required");
        }
        if (serviceEndDate == null) {
            throw new IllegalArgumentException("Service end date is required");
        }
        if (serviceStartDate.isAfter(serviceEndDate)) {
            throw new IllegalArgumentException("Service start date (" + serviceStartDate +
                    ") cannot be after service end date (" + serviceEndDate + ")");
        }

        VaccinationCampaign campaign = campaignRepository.findByIdForUpdate(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found with ID: " + campaignId));
        entityManager.refresh(campaign);

        validateCampaignServiceDateReverseImpact(campaignId, serviceStartDate, serviceEndDate);

        campaign.setServiceStartDate(serviceStartDate);
        campaign.setServiceEndDate(serviceEndDate);

        return campaignRepository.save(campaign);
    }

    @Transactional(readOnly = true)
    public Optional<VaccinationCampaign> getCampaignById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return campaignRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<VaccinationCampaign> getCampaignByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return Optional.empty();
        }
        return campaignRepository.findByCode(code.trim());
    }

    private void validateCampaignServiceDateReverseImpact(
            Long campaignId,
            LocalDate proposedServiceStart,
            LocalDate proposedServiceEnd
    ) {
        List<VaccinationSession> blockingSessions = sessionRepository.findBlockingSessionsForCampaign(

                campaignId, BLOCKING_SESSION_STATUSES, clock.instant()
        );
        for (VaccinationSession session : blockingSessions) {
            ZoneId zoneId = ZoneId.of(session.getScheduleTimezone());
            ZonedDateTime startZdt = session.getStartAt().atZone(zoneId);
            LocalDate firstTouchedDate = startZdt.toLocalDate();
            Instant lastContainedInstant = session.getEndAt().minusNanos(1);
            LocalDate lastTouchedDate = lastContainedInstant.atZone(zoneId).toLocalDate();

            if (firstTouchedDate.isBefore(proposedServiceStart) || lastTouchedDate.isAfter(proposedServiceEnd)) {
                throw new IllegalArgumentException(
                        "Cannot shorten campaign service dates because active or scheduled session(s) fall outside proposed service dates [" +
                                proposedServiceStart + " to " + proposedServiceEnd + "]"
                );
            }
        }
    }
}
