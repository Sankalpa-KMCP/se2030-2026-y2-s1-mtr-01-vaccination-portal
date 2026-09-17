package com.se2030.vaccinationportal.appointment.service;

import com.se2030.vaccinationportal.appointment.exception.AppointmentNotFoundException;
import com.se2030.vaccinationportal.appointment.model.WaitlistEntry;
import com.se2030.vaccinationportal.appointment.model.WaitlistStatus;
import com.se2030.vaccinationportal.appointment.repository.WaitlistRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service orchestrating voluntary waitlist entry cancellation (leaving waitlist) and waitlist status/position inspection.
 *
 * <p><b>User Story:</b>
 * <ul>
 *   <li>US-05: Citizen waitlist status checking, queue position inspection, and voluntary withdrawal.</li>
 * </ul>
 *
 * <p><b>Business Rules Enforced:</b>
 * <ul>
 *   <li>MF03-BR-06: Evaluates FIFO queue order to compute dynamic 1-based queue position;
 *       voluntary cancellation transitions status to CANCELLED and frees queue position for subsequent waiters.</li>
 * </ul>
 */
@Service
@Transactional
public class WaitlistLeaveService {

    private final WaitlistRepository waitlistRepository;

    @Autowired
    public WaitlistLeaveService(WaitlistRepository waitlistRepository) {
        this.waitlistRepository = waitlistRepository;
    }

    /**
     * Voluntarily leaves (cancels) a WAITING waitlist entry for a citizen (US-05).
     *
     * @param waitlistEntryId     Identifier of the waitlist entry
     * @param requestingCitizenId Identifier of the citizen making the request
     * @return Updated {@link WaitlistEntry} in CANCELLED status
     */
    public WaitlistEntry leaveWaitlist(Long waitlistEntryId, Long requestingCitizenId) {
        if (waitlistEntryId == null) {
            throw new IllegalArgumentException("Waitlist entry ID is required");
        }
        if (requestingCitizenId == null) {
            throw new IllegalArgumentException("Requesting citizen ID is required");
        }

        WaitlistEntry entry = waitlistRepository.findById(waitlistEntryId)
                .orElseThrow(() -> new AppointmentNotFoundException("Waitlist entry not found with ID: " + waitlistEntryId));

        if (!requestingCitizenId.equals(entry.getCitizenId())) {
            throw new IllegalArgumentException("Waitlist entry " + waitlistEntryId + " does not belong to citizen " + requestingCitizenId);
        }

        if (entry.getStatus() != WaitlistStatus.WAITING) {
            throw new IllegalStateException("Cannot leave waitlist entry in status " + entry.getStatus()
                    + ". Only WAITING entries can be cancelled voluntarily.");
        }

        entry.cancel();
        return waitlistRepository.save(entry);
    }

    /**
     * Retrieves the active WAITING or most recent PROMOTED waitlist entry for a citizen and campaign (US-05).
     *
     * @param citizenId  Identifier of the citizen
     * @param campaignId Identifier of the campaign
     * @return {@link Optional} containing matching {@link WaitlistEntry} if present
     */
    @Transactional(readOnly = true)
    public Optional<WaitlistEntry> getWaitlistStatus(Long citizenId, Long campaignId) {
        if (citizenId == null || campaignId == null) {
            return Optional.empty();
        }
        Optional<WaitlistEntry> waiting = waitlistRepository.findByCitizenIdAndCampaignIdAndStatus(
                citizenId, campaignId, WaitlistStatus.WAITING);
        if (waiting.isPresent()) {
            return waiting;
        }
        return waitlistRepository.findFirstByCitizenIdAndCampaignIdAndStatusOrderByJoinedAtDesc(
                citizenId, campaignId, WaitlistStatus.PROMOTED);
    }

    /**
     * Calculates the 1-based FIFO position of a WAITING entry in its session queue.
     *
     * @param entry Target {@link WaitlistEntry}
     * @return 1-based position in queue, or null if entry is null or not in WAITING status
     */
    @Transactional(readOnly = true)
    public Integer getQueuePosition(WaitlistEntry entry) {
        if (entry == null || entry.getStatus() != WaitlistStatus.WAITING) {
            return null;
        }
        List<WaitlistEntry> waitingEntries = waitlistRepository.findBySessionIdAndStatusOrderByJoinedAtAsc(
                entry.getSessionId(), WaitlistStatus.WAITING);

        for (int i = 0; i < waitingEntries.size(); i++) {
            if (waitingEntries.get(i).getId().equals(entry.getId())) {
                return i + 1;
            }
        }
        return 1;
    }
}
