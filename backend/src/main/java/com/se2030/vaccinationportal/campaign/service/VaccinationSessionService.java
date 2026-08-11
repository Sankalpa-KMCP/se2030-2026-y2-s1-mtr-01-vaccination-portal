package com.se2030.vaccinationportal.campaign.service;

import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.validation.VaccinationSessionValidator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing concrete vaccination sessions.
 * Enforces deterministic parent locking (campaign -> center) and authoritative cross-component
 * validation prior to session persistence.
 */
@Service
@Transactional
public class VaccinationSessionService {

    private final VaccinationSessionRepository sessionRepository;
    private final VaccinationCampaignRepository campaignRepository;
    private final VaccinationCenterRepository centerRepository;
    private final VaccinationSessionValidator validator;
    @PersistenceContext
    private EntityManager entityManager;

    public VaccinationSessionService(
            VaccinationSessionRepository sessionRepository,
            VaccinationCampaignRepository campaignRepository,
            VaccinationCenterRepository centerRepository,
            VaccinationSessionValidator validator
    ) {
        this.sessionRepository = sessionRepository;
        this.campaignRepository = campaignRepository;
        this.centerRepository = centerRepository;
        this.validator = validator;
    }

    /**
     * Atomically creates a validated one-off vaccination session in initial DRAFT lifecycle status.
     * Uses deterministic pessimistic parent locking (campaign -> center) and authoritative entity refresh.
     *
     * @param campaignId     ID of the target VaccinationCampaign
     * @param centerId       ID of the hosting VaccinationCenter
     * @param startAt        Session start UTC instant
     * @param endAt          Session end UTC instant
     * @param bookingCloseAt Booking intake close UTC instant
     * @return Saved VaccinationSession entity
     * @throws IllegalArgumentException if parameters or business invariants are violated
     */
    public VaccinationSession createOneOffDraftSession(
            Long campaignId,
            Long centerId,
            Instant startAt,
            Instant endAt,
            Instant bookingCloseAt
    ) {
        requireId(campaignId, "Campaign ID is required");
        requireId(centerId, "Center ID is required");

        VaccinationCampaign campaign = campaignRepository.findByIdForUpdate(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found with ID: " + campaignId));
        if (entityManager != null) {
            entityManager.refresh(campaign);
        }

        VaccinationCenter center = centerRepository.findByIdForUpdate(centerId)
                .orElseThrow(() -> new IllegalArgumentException("Vaccination center not found with ID: " + centerId));
        if (entityManager != null) {
            entityManager.refresh(center);
        }

        validator.validateSessionSchedule(campaign, center, startAt, endAt, bookingCloseAt);

        VaccinationSession session = new VaccinationSession(
                campaign,
                center,
                startAt,
                endAt,
                bookingCloseAt,
                center.getTimezone()
        );

        return sessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public Optional<VaccinationSession> getSessionById(Long id) {
        requireId(id, "Session ID is required");
        return sessionRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<VaccinationSession> getSessionsForCampaign(Long campaignId) {
        requireId(campaignId, "Campaign ID is required");
        return sessionRepository.findByCampaignId(campaignId);
    }

    @Transactional(readOnly = true)
    public List<VaccinationSession> getSessionsForCenter(Long centerId) {
        requireId(centerId, "Center ID is required");
        return sessionRepository.findByCenterId(centerId);
    }

    private static void requireId(Long id, String message) {
        if (id == null) {
            throw new IllegalArgumentException(message);
        }
    }
}
