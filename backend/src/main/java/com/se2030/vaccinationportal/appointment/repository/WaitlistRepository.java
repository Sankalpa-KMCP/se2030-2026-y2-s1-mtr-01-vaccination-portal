package com.se2030.vaccinationportal.appointment.repository;

import com.se2030.vaccinationportal.appointment.model.WaitlistEntry;
import com.se2030.vaccinationportal.appointment.model.WaitlistStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA Repository for {@link WaitlistEntry} persistence.
 * Provides query methods for active waitlist entry checks and deterministic FIFO promotion ordering.
 *
 * <p><b>User Story:</b>
 * <ul>
 *   <li>US-05: Citizen waitlist join, queue position tracking, voluntary leave, and automated promotion.</li>
 * </ul>
 *
 * <p><b>Business Rules:</b>
 * <ul>
 *   <li>MF03-BR-06: Strict FIFO queue evaluation ordering (joinedAt ascending); single WAITING entry per citizen/campaign.</li>
 *   <li>MF03-BR-07: Candidate retrieval for promotion eligibility and duplicate screening.</li>
 *   <li>MF03-BR-14: Duplicate active waitlist entry validation.</li>
 * </ul>
 */
@Repository
public interface WaitlistRepository extends JpaRepository<WaitlistEntry, Long> {

    /**
     * Finds a waitlist entry for a citizen, campaign, and specific status (e.g. WAITING).
     * Used for duplicate active waitlist entry validation.
     *
     * @param citizenId  Identifier of the citizen
     * @param campaignId Identifier of the campaign
     * @param status     Target {@link WaitlistStatus}
     * @return {@link Optional} containing matching {@link WaitlistEntry} if present
     */
    Optional<WaitlistEntry> findByCitizenIdAndCampaignIdAndStatus(Long citizenId, Long campaignId, WaitlistStatus status);

    /**
     * Finds the most recent waitlist entry for a citizen, campaign, and specific status, ordered by joined timestamp descending.
     *
     * @param citizenId  Identifier of the citizen
     * @param campaignId Identifier of the campaign
     * @param status     Target {@link WaitlistStatus}
     * @return {@link Optional} containing matching {@link WaitlistEntry} if present
     */
    Optional<WaitlistEntry> findFirstByCitizenIdAndCampaignIdAndStatusOrderByJoinedAtDesc(
            Long citizenId, Long campaignId, WaitlistStatus status);

    /**
     * Finds all waitlist entries for a session with a specific status, ordered by joined timestamp ascending (FIFO).
     * Used by promotion logic to process waitlist entries in deterministic arrival order (MF03-BR-06).
     *
     * @param sessionId Identifier of the session
     * @param status    Target {@link WaitlistStatus}
     * @return {@link List} of matching {@link WaitlistEntry} entities in FIFO order
     */
    List<WaitlistEntry> findBySessionIdAndStatusOrderByJoinedAtAsc(Long sessionId, WaitlistStatus status);

    /**
     * Finds all waitlist entries for a campaign with a specific status, ordered by joined timestamp ascending (FIFO).
     *
     * @param campaignId Identifier of the campaign
     * @param status     Target {@link WaitlistStatus}
     * @return {@link List} of matching {@link WaitlistEntry} entities in FIFO order
     */
    List<WaitlistEntry> findByCampaignIdAndStatusOrderByJoinedAtAsc(Long campaignId, WaitlistStatus status);
}
