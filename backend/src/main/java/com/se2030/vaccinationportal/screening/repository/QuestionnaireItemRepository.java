package com.se2030.vaccinationportal.screening.repository;

import com.se2030.vaccinationportal.screening.model.QuestionnaireItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionnaireItemRepository extends JpaRepository<QuestionnaireItem, Long> {
    List<QuestionnaireItem> findByQuestionnaireVersionIdOrderByDisplayOrderAsc(Long versionId);
    Optional<QuestionnaireItem> findByQuestionnaireVersionIdAndQuestionId(Long versionId, Long questionId);
    boolean existsByQuestionnaireVersionIdAndDisplayOrder(Long versionId, int displayOrder);
    boolean existsByQuestionnaireVersionIdAndQuestionId(Long versionId, Long questionId);
}
