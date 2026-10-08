package com.se2030.vaccinationportal.screening.eligibility.repository;

import com.se2030.vaccinationportal.screening.eligibility.model.ScreeningTriggeredRuleEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScreeningTriggeredRuleEvidenceRepository
        extends JpaRepository<ScreeningTriggeredRuleEvidence, Long> {

    List<ScreeningTriggeredRuleEvidence> findBySubmissionIdOrderByEvaluationOrderAsc(Long submissionId);

    void deleteBySubmissionId(Long submissionId);
}
