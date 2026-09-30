package com.se2030.vaccinationportal.screening.eligibility.repository;

import com.se2030.vaccinationportal.screening.eligibility.model.EligibilityAssessment;
import com.se2030.vaccinationportal.screening.eligibility.model.EligibilityAssessmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EligibilityAssessmentRepository extends JpaRepository<EligibilityAssessment, Long> {
    Optional<EligibilityAssessment> findBySubmissionId(Long submissionId);
    List<EligibilityAssessment> findByCitizenIdOrderByReviewedAtDesc(Long citizenId);

    @Query("SELECT ea FROM EligibilityAssessment ea WHERE ea.citizenId = :citizenId AND ea.campaignId = :campaignId ORDER BY ea.reviewedAt DESC")
    List<EligibilityAssessment> findByCitizenIdAndCampaignId(@Param("citizenId") Long citizenId, @Param("campaignId") Long campaignId);

    @Query("SELECT ea FROM EligibilityAssessment ea WHERE ea.citizenId = :citizenId AND ea.campaignId = :campaignId AND ea.status = :status")
    Optional<EligibilityAssessment> findByCitizenIdAndCampaignIdAndStatus(@Param("citizenId") Long citizenId,
                                                                         @Param("campaignId") Long campaignId,
                                                                         @Param("status") EligibilityAssessmentStatus status);
}
