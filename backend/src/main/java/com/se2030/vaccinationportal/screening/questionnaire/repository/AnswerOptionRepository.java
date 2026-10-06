package com.se2030.vaccinationportal.screening.questionnaire.repository;

import com.se2030.vaccinationportal.screening.questionnaire.model.AnswerOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnswerOptionRepository extends JpaRepository<AnswerOption, Long> {
    List<AnswerOption> findByQuestionIdOrderByDisplayOrderAsc(Long questionId);
    Optional<AnswerOption> findByQuestionIdAndCode(Long questionId, String code);
    boolean existsByQuestionIdAndCode(Long questionId, String code);
}
