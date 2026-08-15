package com.se2030.vaccinationportal.screening.repository;

import com.se2030.vaccinationportal.screening.model.EligibilityRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EligibilityRuleRepository extends JpaRepository<EligibilityRule, Long> {
    List<EligibilityRule> findByQuestionnaireVersionIdOrderByPriorityAsc(Long versionId);
    Optional<EligibilityRule> findByQuestionnaireVersionIdAndRuleCode(Long versionId, String ruleCode);
    boolean existsByQuestionnaireVersionIdAndRuleCode(Long versionId, String ruleCode);
}
