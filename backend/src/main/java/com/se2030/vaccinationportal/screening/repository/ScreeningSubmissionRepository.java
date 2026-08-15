package com.se2030.vaccinationportal.screening.repository;

import com.se2030.vaccinationportal.screening.model.ScreeningSubmission;
import com.se2030.vaccinationportal.screening.model.ScreeningSubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
