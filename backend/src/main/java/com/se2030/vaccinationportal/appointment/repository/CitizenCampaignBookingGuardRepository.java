package com.se2030.vaccinationportal.appointment.repository;

import com.se2030.vaccinationportal.appointment.model.CitizenCampaignBookingGuard;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link CitizenCampaignBookingGuard}.
 * Enforces pessimistic locking semantics per Canonical Section 12.4 to serialize
 * appointment and waitlist cross-table invariant checks.
 */
@Repository
public interface CitizenCampaignBookingGuardRepository extends JpaRepository<CitizenCampaignBookingGuard, Long> {

    /**
     * Fetch and lock one booking guard row for a given citizen and campaign using pessimistic write locking.
     * Evaluates as SELECT ... FOR UPDATE to serialize cross-table booking and waitlist operations.
     *
     * @param citizenId  Identifier of the citizen
     * @param campaignId Identifier of the campaign
     * @return Optional containing the locked guard row if present
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM CitizenCampaignBookingGuard g WHERE g.citizenId = :citizenId AND g.campaignId = :campaignId")
    Optional<CitizenCampaignBookingGuard> findByCitizenIdAndCampaignIdForUpdate(
            @Param("citizenId") Long citizenId,
            @Param("campaignId") Long campaignId
    );

    /**
     * Find a booking guard row without locking.
     *
     * @param citizenId  Identifier of the citizen
     * @param campaignId Identifier of the campaign
     * @return Optional containing the guard row if present
     */
    Optional<CitizenCampaignBookingGuard> findByCitizenIdAndCampaignId(Long citizenId, Long campaignId);

    /**
     * Insert a new guard row if one does not exist yet for the given citizen/campaign pair.
     * Used on initial booking attempt when no guard row exists yet.
     * Note: Wiring of this method belongs to the booking service in the next phase.
     *
     * @param citizenId  Identifier of the citizen
     * @param campaignId Identifier of the campaign
     * @return The existing or newly created guard row
     */
    default CitizenCampaignBookingGuard insertGuardIfAbsent(Long citizenId, Long campaignId) {
        return findByCitizenIdAndCampaignId(citizenId, campaignId)
                .orElseGet(() -> save(new CitizenCampaignBookingGuard(citizenId, campaignId)));
    }
}
