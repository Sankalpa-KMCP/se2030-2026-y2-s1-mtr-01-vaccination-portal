package com.se2030.vaccinationportal.screening.submission.repository;

import com.se2030.vaccinationportal.screening.submission.model.ScreeningSubmission;
import com.se2030.vaccinationportal.screening.submission.model.ScreeningSubmissionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScreeningSubmissionRepository extends JpaRepository<ScreeningSubmission, Long> {
    List<ScreeningSubmission> findByCitizenIdOrderByCreatedAtDesc(Long citizenId);
    List<ScreeningSubmission> findByCampaignIdAndStatus(Long campaignId, ScreeningSubmissionStatus status);
    List<ScreeningSubmission> findByStatus(ScreeningSubmissionStatus status);

    @Query("SELECT s FROM ScreeningSubmission s WHERE s.citizenId = :citizenId AND s.campaignId = :campaignId ORDER BY s.createdAt DESC")
    List<ScreeningSubmission> findByCitizenIdAndCampaignId(@Param("citizenId") Long citizenId, @Param("campaignId") Long campaignId);

    @Query("SELECT s FROM ScreeningSubmission s WHERE s.citizenId = :citizenId AND s.campaignId = :campaignId AND s.status IN :activeStatuses")
    Optional<ScreeningSubmission> findActiveSubmission(@Param("citizenId") Long citizenId,
                                                        @Param("campaignId") Long campaignId,
                                                        @Param("activeStatuses") List<ScreeningSubmissionStatus> activeStatuses);

    /**
     * Transactional serialization for the one-active-submission invariant (R05).
     * The pessimistic write lock (SELECT ... FOR UPDATE with next-key locking on MySQL) serializes
     * concurrent submissions for the same citizen/campaign pair so two requests cannot both observe
     * "no active submission" and insert two active rows. Combined with the database-level
     * {@code uk_screening_submission_active_citizen_campaign} unique constraint, at most one active
     * submission per citizen/campaign can exist.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ScreeningSubmission s WHERE s.citizenId = :citizenId AND s.campaignId = :campaignId AND s.status IN :activeStatuses")
    Optional<ScreeningSubmission> findActiveSubmissionForUpdate(@Param("citizenId") Long citizenId,
                                                                 @Param("campaignId") Long campaignId,
                                                                 @Param("activeStatuses") List<ScreeningSubmissionStatus> activeStatuses);

    boolean existsByCampaignId(Long campaignId);
}
