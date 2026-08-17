package com.se2030.vaccinationportal.campaign.service;

import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.SessionChangeEvent;
import com.se2030.vaccinationportal.campaign.model.SessionChangeType;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.SessionScheduleSnapshot;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.repository.SessionChangeEventRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.validation.VaccinationSessionValidator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing concrete vaccination sessions and individual session change workflows.
 * Enforces deterministic hierarchical parent locking (campaign -&gt; center(s) -&gt; session),
 * authoritative cross-component validation, stale-preview protection, and append-only
 * {@link SessionChangeEvent} audit logging.
 */
@Service
@Transactional
public class VaccinationSessionService {

    private final VaccinationSessionRepository sessionRepository;
    private final VaccinationCampaignRepository campaignRepository;
    private final VaccinationCenterRepository centerRepository;
    private final SessionChangeEventRepository sessionChangeEventRepository;
    private final VaccinationSessionValidator validator;
    private final SessionImpactReader sessionImpactReader;
    private final Clock clock;
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public VaccinationSessionService(
            VaccinationSessionRepository sessionRepository,
            VaccinationCampaignRepository campaignRepository,
            VaccinationCenterRepository centerRepository,
            SessionChangeEventRepository sessionChangeEventRepository,
            VaccinationSessionValidator validator,
            @Autowired(required = false) SessionImpactReader sessionImpactReader,
            Clock clock
    ) {
        this.sessionRepository = sessionRepository;
        this.campaignRepository = campaignRepository;
        this.centerRepository = centerRepository;
        this.sessionChangeEventRepository = sessionChangeEventRepository;
        this.validator = validator;
        this.sessionImpactReader = sessionImpactReader;
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    public VaccinationSessionService(
            VaccinationSessionRepository sessionRepository,
            VaccinationCampaignRepository campaignRepository,
            VaccinationCenterRepository centerRepository,
            SessionChangeEventRepository sessionChangeEventRepository,
            VaccinationSessionValidator validator,
            SessionImpactReader sessionImpactReader
    ) {
        this(sessionRepository, campaignRepository, centerRepository, sessionChangeEventRepository, validator, sessionImpactReader, Clock.systemUTC());
    }

    public VaccinationSessionService(
            VaccinationSessionRepository sessionRepository,
            VaccinationCampaignRepository campaignRepository,
            VaccinationCenterRepository centerRepository,
            SessionChangeEventRepository sessionChangeEventRepository,
            VaccinationSessionValidator validator
    ) {
        this(sessionRepository, campaignRepository, centerRepository, sessionChangeEventRepository, validator, null, Clock.systemUTC());
    }

    /**
     * Atomically creates a validated one-off vaccination session in initial DRAFT lifecycle status.
     * Uses deterministic pessimistic parent locking (campaign -&gt; center) and authoritative entity refresh.
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
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + campaignId));
        if (entityManager != null) {
            entityManager.refresh(campaign);
        }
        if (campaign.getStatus() == CampaignStatus.SUSPENDED) {
            throw new IllegalArgumentException(
                    "Cannot create one-off session for suspended campaign with ID: " + campaignId);
        }
        if (campaign.getStatus().isOperationallyClosed()) {
            throw new IllegalArgumentException(
                    "Cannot create one-off session for operationally closed campaign with ID: " + campaignId);
        }

        VaccinationCenter center = centerRepository.findByIdForUpdate(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination center not found with ID: " + centerId));
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
    public VaccinationSession getRequiredSessionById(Long id) {
        if (id == null) {
            throw new ResourceNotFoundException("Session ID is required");
        }
        return sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination session not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<VaccinationSession> getSessions(Long campaignId, Long centerId, SessionLifecycleStatus status) {
        return sessionRepository.findWithFilters(campaignId, centerId, status);
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

    @Transactional(readOnly = true)
    public List<SessionChangeEvent> getSessionChangeEvents(Long sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID is required");
        }
        if (!sessionRepository.existsById(sessionId)) {
            throw new ResourceNotFoundException("Vaccination session not found with ID: " + sessionId);
        }
        return sessionChangeEventRepository.findBySessionIdOrderByCreatedAtAscIdAsc(sessionId);
    }

    /**
     * Schedules a DRAFT vaccination session (transitions to SCHEDULED).
     * Lifecycle-only transition: does NOT create a SessionChangeEvent.
     *
     * @param sessionId ID of the target VaccinationSession
     * @return Saved VaccinationSession in SCHEDULED lifecycle status
     */
    public VaccinationSession scheduleSession(Long sessionId) {
        requireId(sessionId, "Session ID is required");
        VaccinationSession unlockedSession = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination session not found with ID: " + sessionId));
        Long campaignId = unlockedSession.getCampaign().getId();

        VaccinationCampaign campaign = campaignRepository.findByIdForUpdate(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + campaignId));
        if (entityManager != null) {
            entityManager.refresh(campaign);
        }

        VaccinationSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination session not found with ID: " + sessionId));
        if (entityManager != null) {
            entityManager.refresh(session);
        }

        if (!session.getCampaign().getId().equals(campaignId)) {
            throw new IllegalStateException("Authoritative session parent campaign changed concurrently");
        }

        if (campaign.getStatus() == CampaignStatus.SUSPENDED) {
            throw new IllegalArgumentException(
                    "Cannot schedule session for suspended campaign with ID: " + campaignId);
        }
        if (campaign.getStatus().isOperationallyClosed()) {
            throw new IllegalArgumentException(
                    "Cannot schedule session for operationally closed campaign with ID: " + campaignId);
        }

        session.schedule();
        return sessionRepository.save(session);
    }

    /**
     * Starts a SCHEDULED vaccination session (transitions to IN_PROGRESS).
     * Lifecycle-only transition: does NOT create a SessionChangeEvent.
     *
     * @param sessionId ID of the target VaccinationSession
     * @return Saved VaccinationSession in IN_PROGRESS lifecycle status
     */
    public VaccinationSession startSession(Long sessionId) {
        requireId(sessionId, "Session ID is required");
        VaccinationSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination session not found with ID: " + sessionId));
        if (entityManager != null) {
            entityManager.refresh(session);
        }
        Instant now = clock.instant();
        if (now.isBefore(session.getStartAt())) {
            throw new IllegalStateException(
                    "Cannot start vaccination session before scheduled start time: " + session.getStartAt());
        }
        session.start();
        return sessionRepository.save(session);
    }

    /**
     * Completes an IN_PROGRESS vaccination session (transitions to COMPLETED).
     * Lifecycle-only transition: does NOT create a SessionChangeEvent.
     *
     * @param sessionId ID of the target VaccinationSession
     * @return Saved VaccinationSession in COMPLETED lifecycle status
     */
    public VaccinationSession completeSession(Long sessionId) {
        requireId(sessionId, "Session ID is required");
        VaccinationSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination session not found with ID: " + sessionId));
        if (entityManager != null) {
            entityManager.refresh(session);
        }
        session.complete();
        return sessionRepository.save(session);
    }

    /**
     * Previews a proposed same-center reschedule of a vaccination session without mutating state.
     *
     * @param sessionId         ID of the target VaccinationSession
     * @param newStartAt        Proposed new session start UTC instant
     * @param newEndAt          Proposed new session end UTC instant
     * @param newBookingCloseAt Optional proposed new booking close instant (computed via policy if null)
     * @return {@link SessionChangePreview} containing old/new snapshots, impact counts, and preview token
     */
    @Transactional(readOnly = true)
    public SessionChangePreview previewReschedule(
            Long sessionId,
            Instant newStartAt,
            Instant newEndAt,
            Instant newBookingCloseAt
    ) {
        return previewRescheduleOrTransfer(sessionId, null, newStartAt, newEndAt, newBookingCloseAt);
    }

    /**
     * Previews a proposed center transfer (and optional schedule change) of a vaccination session without mutating state.
     *
     * @param sessionId         ID of the target VaccinationSession
     * @param newCenterId       ID of the new hosting VaccinationCenter
     * @param newStartAt        Proposed new session start UTC instant
     * @param newEndAt          Proposed new session end UTC instant
     * @param newBookingCloseAt Optional proposed new booking close instant (computed via policy if null)
     * @return {@link SessionChangePreview} containing old/new snapshots, impact counts, and preview token
     */
    @Transactional(readOnly = true)
    public SessionChangePreview previewCenterTransfer(
            Long sessionId,
            Long newCenterId,
            Instant newStartAt,
            Instant newEndAt,
            Instant newBookingCloseAt
    ) {
        requireId(newCenterId, "New center ID is required for center transfer");
        return previewRescheduleOrTransfer(sessionId, newCenterId, newStartAt, newEndAt, newBookingCloseAt);
    }

    /**
     * Previews a proposed schedule change and/or center transfer of a vaccination session without mutating state.
     *
     * @param sessionId         ID of the target VaccinationSession
     * @param newCenterId       Optional new center ID (defaults to current session center if null)
     * @param newStartAt        Proposed new session start UTC instant
     * @param newEndAt          Proposed new session end UTC instant
     * @param newBookingCloseAt Optional proposed new booking close instant (computed via policy if null)
     * @return {@link SessionChangePreview} containing old/new snapshots, impact counts, and preview token
     */
    @Transactional(readOnly = true)
    public SessionChangePreview previewRescheduleOrTransfer(
            Long sessionId,
            Long newCenterId,
            Instant newStartAt,
            Instant newEndAt,
            Instant newBookingCloseAt
    ) {
        requireId(sessionId, "Session ID is required");
        if (newStartAt == null) {
            throw new IllegalArgumentException("Session start is required");
        }
        if (newEndAt == null) {
            throw new IllegalArgumentException("Session end is required");
        }
        if (!newStartAt.isBefore(newEndAt)) {
            throw new IllegalArgumentException("Session start must be before session end");
        }

        VaccinationSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination session not found with ID: " + sessionId));

        if (session.getLifecycleStatus() == SessionLifecycleStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot reschedule cancelled session");
        }
        if (session.getLifecycleStatus() != SessionLifecycleStatus.DRAFT && session.getLifecycleStatus() != SessionLifecycleStatus.SCHEDULED) {
            throw new IllegalArgumentException("Cannot reschedule session in " + session.getLifecycleStatus() + " lifecycle status");
        }

        Long targetCenterId = (newCenterId != null) ? newCenterId : session.getCenter().getId();
        VaccinationCenter targetCenter = centerRepository.findById(targetCenterId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination center not found with ID: " + targetCenterId));

        Instant effectiveBookingClose = (newBookingCloseAt != null)
                ? newBookingCloseAt
                : (session.getBookingCloseAt().isAfter(newStartAt) ? newStartAt : session.getBookingCloseAt());

        boolean isNoOp = session.getCenter().getId().equals(targetCenter.getId())
                && session.getScheduleTimezone().equals(targetCenter.getTimezone())
                && session.getStartAt().equals(newStartAt)
                && session.getEndAt().equals(newEndAt)
                && session.getBookingCloseAt().equals(effectiveBookingClose);

        if (isNoOp) {
            throw new IllegalArgumentException("Proposed session change makes no material change to center or schedule");
        }

        validator.validateSessionSchedule(session.getCampaign(), targetCenter, newStartAt, newEndAt, effectiveBookingClose);

        int affectedAppointments = 0;
        int affectedWaitlist = 0;
        if (session.getLifecycleStatus() == SessionLifecycleStatus.SCHEDULED) {
            if (sessionImpactReader == null) {
                throw new IllegalStateException("Session impact reader is not available to assess SCHEDULED session changes");
            }
            SessionImpactSummary impact = sessionImpactReader.getSessionImpact(sessionId);
            if (impact == null) {
                throw new IllegalStateException("Session impact reader returned null summary for session ID: " + sessionId);
            }
            affectedAppointments = impact.affectedAppointmentCount();
            affectedWaitlist = impact.affectedWaitlistCount();
        }

        SessionScheduleSnapshot oldSnapshot = SessionScheduleSnapshot.from(session);
        SessionChangeType changeType = session.getCenter().getId().equals(targetCenterId)
                ? SessionChangeType.RESCHEDULED
                : SessionChangeType.CENTER_TRANSFERRED;

        SessionScheduleSnapshot newSnapshot = new SessionScheduleSnapshot(
                targetCenter.getId(),
                targetCenter.getTimezone(),
                newStartAt,
                newEndAt,
                effectiveBookingClose,
                session.getLifecycleStatus()
        );

        String previewToken = generatePreviewToken(
                sessionId,
                oldSnapshot,
                newSnapshot,
                changeType,
                affectedAppointments,
                affectedWaitlist
        );

        return new SessionChangePreview(
                sessionId,
                changeType,
                oldSnapshot,
                newSnapshot,
                affectedAppointments,
                affectedWaitlist,
                previewToken
        );
    }

    /**
     * Confirms and applies a same-center reschedule of a vaccination session.
     */
    public VaccinationSession confirmReschedule(
            Long sessionId,
            Instant newStartAt,
            Instant newEndAt,
            Instant newBookingCloseAt,
            String previewToken,
            String reason,
            String actorId
    ) {
        return confirmRescheduleOrTransfer(sessionId, null, newStartAt, newEndAt, newBookingCloseAt, previewToken, reason, actorId);
    }

    /**
     * Confirms and applies a center transfer (and optional reschedule) of a vaccination session.
     */
    public VaccinationSession confirmCenterTransfer(
            Long sessionId,
            Long newCenterId,
            Instant newStartAt,
            Instant newEndAt,
            Instant newBookingCloseAt,
            String previewToken,
            String reason,
            String actorId
    ) {
        requireId(newCenterId, "New center ID is required for center transfer");
        return confirmRescheduleOrTransfer(sessionId, newCenterId, newStartAt, newEndAt, newBookingCloseAt, previewToken, reason, actorId);
    }

    /**
     * Confirms and applies a schedule change and/or center transfer of a vaccination session.
     * Enforces deterministic parent lock ordering (campaign -&gt; center(s) -&gt; session),
     * stale-preview verification, revalidation, and atomic {@link SessionChangeEvent} creation.
     */
    public VaccinationSession confirmRescheduleOrTransfer(
            Long sessionId,
            Long newCenterId,
            Instant newStartAt,
            Instant newEndAt,
            Instant newBookingCloseAt,
            String previewToken,
            String reason,
            String actorId
    ) {
        requireId(sessionId, "Session ID is required");
        String validatedReason = SessionChangeEvent.validateReason(reason);
        String validatedActorId = SessionChangeEvent.validateActorId(actorId);
        if (previewToken == null || previewToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Preview token is required");
        }
        if (newStartAt == null) {
            throw new IllegalArgumentException("Session start is required");
        }
        if (newEndAt == null) {
            throw new IllegalArgumentException("Session end is required");
        }
        if (!newStartAt.isBefore(newEndAt)) {
            throw new IllegalArgumentException("Session start must be before session end");
        }

        // Step 1: Pre-read to discover routing IDs
        VaccinationSession preRead = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination session not found with ID: " + sessionId));
        Long campaignId = preRead.getCampaign().getId();
        Long currentCenterId = preRead.getCenter().getId();
        Long targetCenterId = (newCenterId != null) ? newCenterId : currentCenterId;

        // Step 2: Acquire deterministic write locks
        // Lock campaign first
        VaccinationCampaign campaign = campaignRepository.findByIdForUpdate(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + campaignId));
        if (entityManager != null) {
            entityManager.refresh(campaign);
        }

        // Lock centers in ascending ID order
        VaccinationCenter currentCenter;
        VaccinationCenter targetCenter;
        if (currentCenterId.equals(targetCenterId)) {
            currentCenter = centerRepository.findByIdForUpdate(currentCenterId)
                    .orElseThrow(() -> new ResourceNotFoundException("Vaccination center not found with ID: " + currentCenterId));
            if (entityManager != null) {
                entityManager.refresh(currentCenter);
            }
            targetCenter = currentCenter;
        } else {
            Long firstCenterId = Math.min(currentCenterId, targetCenterId);
            Long secondCenterId = Math.max(currentCenterId, targetCenterId);

            VaccinationCenter firstCenter = centerRepository.findByIdForUpdate(firstCenterId)
                    .orElseThrow(() -> new ResourceNotFoundException("Vaccination center not found with ID: " + firstCenterId));
            if (entityManager != null) {
                entityManager.refresh(firstCenter);
            }

            VaccinationCenter secondCenter = centerRepository.findByIdForUpdate(secondCenterId)
                    .orElseThrow(() -> new ResourceNotFoundException("Vaccination center not found with ID: " + secondCenterId));
            if (entityManager != null) {
                entityManager.refresh(secondCenter);
            }

            currentCenter = currentCenterId.equals(firstCenterId) ? firstCenter : secondCenter;
            targetCenter = targetCenterId.equals(firstCenterId) ? firstCenter : secondCenter;
        }

        // Lock session
        VaccinationSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination session not found with ID: " + sessionId));
        if (entityManager != null) {
            entityManager.refresh(session);
        }

        // Step 3: Defensively verify authoritative relationships
        if (!session.getCampaign().getId().equals(campaignId) || !session.getCenter().getId().equals(currentCenterId)) {
            throw new IllegalStateException("Authoritative session parent relationship changed concurrently");
        }

        // Step 4: Check lifecycle status
        if (session.getLifecycleStatus() == SessionLifecycleStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot reschedule cancelled session");
        }
        if (session.getLifecycleStatus() != SessionLifecycleStatus.DRAFT && session.getLifecycleStatus() != SessionLifecycleStatus.SCHEDULED) {
            throw new IllegalArgumentException("Cannot reschedule session in " + session.getLifecycleStatus() + " lifecycle status");
        }

        // Step 5: Compute effective booking close instant
        Instant effectiveBookingClose = (newBookingCloseAt != null)
                ? newBookingCloseAt
                : (session.getBookingCloseAt().isAfter(newStartAt) ? newStartAt : session.getBookingCloseAt());

        boolean isNoOp = session.getCenter().getId().equals(targetCenter.getId())
                && session.getScheduleTimezone().equals(targetCenter.getTimezone())
                && session.getStartAt().equals(newStartAt)
                && session.getEndAt().equals(newEndAt)
                && session.getBookingCloseAt().equals(effectiveBookingClose);

        if (isNoOp) {
            throw new IllegalArgumentException("Proposed session change makes no material change to center or schedule");
        }

        // Step 6: Revalidate schedule against authoritatively locked entities
        validator.validateSessionSchedule(campaign, targetCenter, newStartAt, newEndAt, effectiveBookingClose);

        // Step 7: Compute impact counts
        int affectedAppointments = 0;
        int affectedWaitlist = 0;
        if (session.getLifecycleStatus() == SessionLifecycleStatus.SCHEDULED) {
            if (sessionImpactReader == null) {
                throw new IllegalStateException("Session impact reader is not available to assess SCHEDULED session changes");
            }
            SessionImpactSummary impact = sessionImpactReader.getSessionImpact(sessionId);
            if (impact == null) {
                throw new IllegalStateException("Session impact reader returned null summary for session ID: " + sessionId);
            }
            affectedAppointments = impact.affectedAppointmentCount();
            affectedWaitlist = impact.affectedWaitlistCount();
        }

        // Step 8: Build snapshots & verify preview token
        SessionScheduleSnapshot oldSnapshot = SessionScheduleSnapshot.from(session);
        SessionChangeType changeType = currentCenterId.equals(targetCenterId)
                ? SessionChangeType.RESCHEDULED
                : SessionChangeType.CENTER_TRANSFERRED;

        SessionScheduleSnapshot newSnapshot = new SessionScheduleSnapshot(
                targetCenter.getId(),
                targetCenter.getTimezone(),
                newStartAt,
                newEndAt,
                effectiveBookingClose,
                session.getLifecycleStatus()
        );

        String currentToken = generatePreviewToken(
                sessionId,
                oldSnapshot,
                newSnapshot,
                changeType,
                affectedAppointments,
                affectedWaitlist
        );

        if (!currentToken.equals(previewToken.trim())) {
            throw new IllegalStateException("Preview token is stale or invalid. Re-preview the session change before confirming.");
        }

        // Step 9: Mutate session
        session.reschedule(targetCenter, newStartAt, newEndAt, effectiveBookingClose);

        // Step 10: Create and persist SessionChangeEvent
        SessionChangeEvent event = new SessionChangeEvent(
                session,
                changeType,
                oldSnapshot,
                newSnapshot,
                validatedReason,
                affectedAppointments,
                affectedWaitlist,
                validatedActorId
        );
        sessionChangeEventRepository.save(event);

        return session;
    }

    /**
     * Previews the cancellation of a vaccination session without mutating state.
     *
     * @param sessionId ID of the target VaccinationSession
     * @return {@link SessionChangePreview} containing old/new snapshots, impact counts, and preview token
     */
    @Transactional(readOnly = true)
    public SessionChangePreview previewCancellation(Long sessionId) {
        requireId(sessionId, "Session ID is required");

        VaccinationSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination session not found with ID: " + sessionId));

        if (session.getLifecycleStatus() == SessionLifecycleStatus.CANCELLED) {
            throw new IllegalArgumentException("Session is already cancelled");
        }
        if (session.getLifecycleStatus() != SessionLifecycleStatus.DRAFT && session.getLifecycleStatus() != SessionLifecycleStatus.SCHEDULED) {
            throw new IllegalArgumentException("Cannot cancel session in " + session.getLifecycleStatus() + " lifecycle status");
        }

        int affectedAppointments = 0;
        int affectedWaitlist = 0;
        if (session.getLifecycleStatus() == SessionLifecycleStatus.SCHEDULED) {
            if (sessionImpactReader == null) {
                throw new IllegalStateException("Session impact reader is not available to assess SCHEDULED session changes");
            }
            SessionImpactSummary impact = sessionImpactReader.getSessionImpact(sessionId);
            if (impact == null) {
                throw new IllegalStateException("Session impact reader returned null summary for session ID: " + sessionId);
            }
            affectedAppointments = impact.affectedAppointmentCount();
            affectedWaitlist = impact.affectedWaitlistCount();
        }

        SessionScheduleSnapshot oldSnapshot = SessionScheduleSnapshot.from(session);
        SessionScheduleSnapshot newSnapshot = new SessionScheduleSnapshot(
                session.getCenter().getId(),
                session.getScheduleTimezone(),
                session.getStartAt(),
                session.getEndAt(),
                session.getBookingCloseAt(),
                SessionLifecycleStatus.CANCELLED
        );
        SessionChangeType changeType = SessionChangeType.CANCELLED;

        String previewToken = generatePreviewToken(
                sessionId,
                oldSnapshot,
                newSnapshot,
                changeType,
                affectedAppointments,
                affectedWaitlist
        );

        return new SessionChangePreview(
                sessionId,
                changeType,
                oldSnapshot,
                newSnapshot,
                affectedAppointments,
                affectedWaitlist,
                previewToken
        );
    }

    /**
     * Confirms and applies cancellation to a vaccination session.
     * Enforces deterministic parent lock ordering (campaign -&gt; center -&gt; session),
     * stale-preview verification, and atomic {@link SessionChangeEvent} creation.
     */
    public VaccinationSession confirmCancellation(
            Long sessionId,
            String previewToken,
            String reason,
            String actorId
    ) {
        requireId(sessionId, "Session ID is required");
        String validatedReason = SessionChangeEvent.validateReason(reason);
        String validatedActorId = SessionChangeEvent.validateActorId(actorId);
        if (previewToken == null || previewToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Preview token is required");
        }

        // Step 1: Pre-read to discover routing IDs
        VaccinationSession preRead = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination session not found with ID: " + sessionId));
        Long campaignId = preRead.getCampaign().getId();
        Long centerId = preRead.getCenter().getId();

        // Step 2: Acquire deterministic write locks
        VaccinationCampaign campaign = campaignRepository.findByIdForUpdate(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + campaignId));
        if (entityManager != null) {
            entityManager.refresh(campaign);
        }

        VaccinationCenter center = centerRepository.findByIdForUpdate(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination center not found with ID: " + centerId));
        if (entityManager != null) {
            entityManager.refresh(center);
        }

        VaccinationSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination session not found with ID: " + sessionId));
        if (entityManager != null) {
            entityManager.refresh(session);
        }

        // Step 3: Check lifecycle status
        if (session.getLifecycleStatus() == SessionLifecycleStatus.CANCELLED) {
            throw new IllegalArgumentException("Session is already cancelled");
        }
        if (session.getLifecycleStatus() != SessionLifecycleStatus.DRAFT && session.getLifecycleStatus() != SessionLifecycleStatus.SCHEDULED) {
            throw new IllegalArgumentException("Cannot cancel session in " + session.getLifecycleStatus() + " lifecycle status");
        }

        // Step 4: Compute impact counts
        int affectedAppointments = 0;
        int affectedWaitlist = 0;
        if (session.getLifecycleStatus() == SessionLifecycleStatus.SCHEDULED) {
            if (sessionImpactReader == null) {
                throw new IllegalStateException("Session impact reader is not available to assess SCHEDULED session changes");
            }
            SessionImpactSummary impact = sessionImpactReader.getSessionImpact(sessionId);
            if (impact == null) {
                throw new IllegalStateException("Session impact reader returned null summary for session ID: " + sessionId);
            }
            affectedAppointments = impact.affectedAppointmentCount();
            affectedWaitlist = impact.affectedWaitlistCount();
        }

        // Step 5: Build snapshots & verify preview token
        SessionScheduleSnapshot oldSnapshot = SessionScheduleSnapshot.from(session);
        SessionScheduleSnapshot newSnapshot = new SessionScheduleSnapshot(
                session.getCenter().getId(),
                session.getScheduleTimezone(),
                session.getStartAt(),
                session.getEndAt(),
                session.getBookingCloseAt(),
                SessionLifecycleStatus.CANCELLED
        );
        SessionChangeType changeType = SessionChangeType.CANCELLED;

        String currentToken = generatePreviewToken(
                sessionId,
                oldSnapshot,
                newSnapshot,
                changeType,
                affectedAppointments,
                affectedWaitlist
        );

        if (!currentToken.equals(previewToken.trim())) {
            throw new IllegalStateException("Preview token is stale or invalid. Re-preview the session change before confirming.");
        }

        // Step 6: Mutate session
        session.cancel();

        // Step 7: Create and persist SessionChangeEvent
        SessionChangeEvent event = new SessionChangeEvent(
                session,
                changeType,
                oldSnapshot,
                newSnapshot,
                validatedReason,
                affectedAppointments,
                affectedWaitlist,
                validatedActorId
        );
        sessionChangeEventRepository.save(event);

        return session;
    }

    /**
     * Generates a deterministic, cryptographically secure SHA-256 fingerprint token
     * representing the previewed session change state and impact counts.
     */
    public static String generatePreviewToken(
            Long sessionId,
            SessionScheduleSnapshot currentSnapshot,
            SessionScheduleSnapshot proposedSnapshot,
            SessionChangeType changeType,
            int affectedAppointmentCount,
            int affectedWaitlistCount
    ) {
        if (sessionId == null || currentSnapshot == null || proposedSnapshot == null || changeType == null) {
            throw new IllegalArgumentException("Parameters for preview token generation cannot be null");
        }
        String raw = String.format(
                "%d|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%d|%d",
                sessionId,
                currentSnapshot.getCenterId(),
                currentSnapshot.getScheduleTimezone(),
                currentSnapshot.getStartAt(),
                currentSnapshot.getEndAt(),
                currentSnapshot.getBookingCloseAt(),
                currentSnapshot.getLifecycleStatus(),
                proposedSnapshot.getCenterId(),
                proposedSnapshot.getScheduleTimezone(),
                proposedSnapshot.getStartAt(),
                proposedSnapshot.getEndAt(),
                proposedSnapshot.getBookingCloseAt(),
                proposedSnapshot.getLifecycleStatus(),
                changeType.name(),
                affectedAppointmentCount,
                affectedWaitlistCount
        );
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private static void requireId(Long id, String message) {
        if (id == null) {
            throw new IllegalArgumentException(message);
        }
    }
}
