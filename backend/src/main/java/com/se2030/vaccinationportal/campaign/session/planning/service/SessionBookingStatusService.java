package com.se2030.vaccinationportal.campaign.session.planning.service;

import com.se2030.vaccinationportal.campaign.core.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionBookingStatus;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.session.planning.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.session.planning.repository.SessionSlotCapacitySummary;
import com.se2030.vaccinationportal.campaign.session.planning.repository.TimeSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Authoritative domain service deriving five-state {@link SessionBookingStatus}
 * based on campaign publication, session lifecycle, schedule intervals, and slot capacity.
 * Enforces single-query batch resolution to strictly eliminate N+1 database queries.
 */
@Service
@Transactional(readOnly = true)
public class SessionBookingStatusService {

    private final TimeSlotRepository timeSlotRepository;
    private final Clock clock;

    @Autowired
    public SessionBookingStatusService(
            TimeSlotRepository timeSlotRepository,
            Clock clock
    ) {
        this.timeSlotRepository = Objects.requireNonNull(timeSlotRepository, "TimeSlotRepository must not be null");
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    public SessionBookingStatusService(TimeSlotRepository timeSlotRepository) {
        this(timeSlotRepository, Clock.systemUTC());
    }

    /**
     * Derives the authoritative booking status for a single vaccination session at current system clock time.
     */
    public SessionBookingStatus deriveBookingStatus(VaccinationSession session) {
        return deriveBookingStatus(session, clock.instant());
    }

    /**
     * Derives the authoritative booking status for a single vaccination session at a given evaluation instant.
     */
    public SessionBookingStatus deriveBookingStatus(VaccinationSession session, Instant now) {
        if (session == null) {
            return SessionBookingStatus.CLOSED;
        }

        // Fast-path evaluation before querying slots:
        if (isPermanentlyClosed(session, now)) {
            return SessionBookingStatus.CLOSED;
        }

        Optional<SessionSlotCapacitySummary> summaryOpt = timeSlotRepository.summarizeCapacityBySessionId(session.getId());
        long slotCount = summaryOpt.map(SessionSlotCapacitySummary::slotCount).orElse(0L);
        long totalCapacity = summaryOpt.map(SessionSlotCapacitySummary::totalCapacity).orElse(0L);
        long totalReserved = summaryOpt.map(SessionSlotCapacitySummary::totalReservedCount).orElse(0L);

        return evaluateStatus(session, slotCount, totalCapacity, totalReserved, now);
    }

    /**
     * Derives authoritative booking statuses for a collection of vaccination sessions in a single batch query,
     * avoiding N+1 database operations.
     */
    public Map<Long, SessionBookingStatus> deriveBookingStatuses(Collection<VaccinationSession> sessions) {
        return deriveBookingStatuses(sessions, clock.instant());
    }

    /**
     * Derives authoritative booking statuses for a collection of vaccination sessions at a given instant.
     */
    public Map<Long, SessionBookingStatus> deriveBookingStatuses(Collection<VaccinationSession> sessions, Instant now) {
        if (sessions == null || sessions.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, SessionBookingStatus> resultMap = new HashMap<>();
        Set<Long> candidateSessionIds = sessions.stream()
                .filter(session -> session != null && session.getId() != null)
                .filter(session -> {
                    if (isPermanentlyClosed(session, now)) {
                        resultMap.put(session.getId(), SessionBookingStatus.CLOSED);
                        return false;
                    }
                    return true;
                })
                .map(VaccinationSession::getId)
                .collect(Collectors.toSet());

        if (!candidateSessionIds.isEmpty()) {
            List<SessionSlotCapacitySummary> summaries = timeSlotRepository.summarizeCapacityBySessionIds(candidateSessionIds);
            Map<Long, SessionSlotCapacitySummary> summaryMap = summaries.stream()
                    .collect(Collectors.toMap(SessionSlotCapacitySummary::sessionId, s -> s));

            for (VaccinationSession session : sessions) {
                if (session == null || session.getId() == null || resultMap.containsKey(session.getId())) {
                    continue;
                }
                SessionSlotCapacitySummary summary = summaryMap.get(session.getId());
                long slotCount = summary != null ? summary.slotCount() : 0L;
                long totalCapacity = summary != null ? summary.totalCapacity() : 0L;
                long totalReserved = summary != null ? summary.totalReservedCount() : 0L;

                resultMap.put(session.getId(), evaluateStatus(session, slotCount, totalCapacity, totalReserved, now));
            }
        }

        return resultMap;
    }

    /**
     * Checks if the session or campaign lifecycle/temporal bounds dictate permanent closure prior to slot inspection.
     */
    private boolean isPermanentlyClosed(VaccinationSession session, Instant now) {
        if (session.getLifecycleStatus() != SessionLifecycleStatus.SCHEDULED) {
            return true;
        }
        VaccinationCampaign campaign = session.getCampaign();
        if (campaign == null || campaign.getStatus() == null) {
            return true;
        }
        CampaignStatus campaignStatus = campaign.getStatus();
        if (campaignStatus.isOperationallyClosed()
                || campaignStatus == CampaignStatus.DRAFT
                || campaignStatus == CampaignStatus.SCHEDULED) {
            return true;
        }
        if (session.getBookingCloseAt() != null && now.isAfter(session.getBookingCloseAt())) {
            return true;
        }
        if (session.getStartAt() != null && now.isAfter(session.getStartAt())) {
            return true;
        }
        if (campaign.getBookingCloseAt() != null && now.isAfter(campaign.getBookingCloseAt())) {
            return true;
        }
        return false;
    }

    /**
     * Pure, authoritative evaluation of the five-state {@link SessionBookingStatus} machine.
     * Precedence:
     * 1. CLOSED (lifecycle / temporal window / missing slots)
     * 2. SUSPENDED (manual session suspension or campaign suspension)
     * 3. NOT_OPEN (future intake window)
     * 4. FULL (zero remaining capacity)
     * 5. OPEN (positive remaining capacity)
     */
    public static SessionBookingStatus evaluateStatus(
            VaccinationSession session,
            long slotCount,
            long totalCapacity,
            long totalReservedCount,
            Instant now
    ) {
        if (session == null) {
            return SessionBookingStatus.CLOSED;
        }
        VaccinationCampaign campaign = session.getCampaign();

        // 1. CLOSED checks (highest precedence)
        if (session.getLifecycleStatus() != SessionLifecycleStatus.SCHEDULED) {
            return SessionBookingStatus.CLOSED;
        }
        if (campaign == null || campaign.getStatus() == null) {
            return SessionBookingStatus.CLOSED;
        }
        CampaignStatus campaignStatus = campaign.getStatus();
        if (campaignStatus.isOperationallyClosed()
                || campaignStatus == CampaignStatus.DRAFT
                || campaignStatus == CampaignStatus.SCHEDULED) {
            return SessionBookingStatus.CLOSED;
        }
        if (session.getBookingCloseAt() != null && now.isAfter(session.getBookingCloseAt())) {
            return SessionBookingStatus.CLOSED;
        }
        if (session.getStartAt() != null && now.isAfter(session.getStartAt())) {
            return SessionBookingStatus.CLOSED;
        }
        if (campaign.getBookingCloseAt() != null && now.isAfter(campaign.getBookingCloseAt())) {
            return SessionBookingStatus.CLOSED;
        }
        if (slotCount <= 0) {
            return SessionBookingStatus.CLOSED;
        }

        // 2. SUSPENDED checks (evaluated only after CLOSED checks pass)
        if (session.isBookingSuspended() || campaignStatus == CampaignStatus.SUSPENDED) {
            return SessionBookingStatus.SUSPENDED;
        }

        // 3. NOT_OPEN checks
        if (campaign.getBookingOpenAt() != null && now.isBefore(campaign.getBookingOpenAt())) {
            return SessionBookingStatus.NOT_OPEN;
        }

        // 4. FULL vs 5. OPEN checks
        long availableCapacity = Math.max(0L, totalCapacity - totalReservedCount);
        if (availableCapacity <= 0L) {
            return SessionBookingStatus.FULL;
        }

        return SessionBookingStatus.OPEN;
    }
}
