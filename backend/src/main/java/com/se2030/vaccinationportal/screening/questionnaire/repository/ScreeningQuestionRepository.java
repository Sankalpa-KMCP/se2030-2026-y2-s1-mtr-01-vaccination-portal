package com.se2030.vaccinationportal.screening.questionnaire.repository;

import com.se2030.vaccinationportal.screening.questionnaire.model.ScreeningQuestion;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireVersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScreeningQuestionRepository extends JpaRepository<ScreeningQuestion, Long> {
    Optional<ScreeningQuestion> findByCode(String code);
    boolean existsByCode(String code);
    List<ScreeningQuestion> findByActiveTrue();

    @Query("SELECT COUNT(qi) > 0 FROM QuestionnaireItem qi WHERE qi.question.id = :questionId")
    boolean isReferencedInQuestionnaireVersion(@Param("questionId") Long questionId);

    @Query("SELECT COUNT(qi) > 0 FROM QuestionnaireItem qi " +
           "WHERE qi.question.id = :questionId AND qi.questionnaireVersion.status <> :draftStatus")
    boolean isReferencedInProtectedQuestionnaireVersion(
            @Param("questionId") Long questionId,
            @Param("draftStatus") QuestionnaireVersionStatus draftStatus
    );

    @Query("SELECT COUNT(sa) > 0 FROM ScreeningAnswer sa WHERE sa.question.id = :questionId")
    boolean isReferencedInScreeningAnswers(@Param("questionId") Long questionId);
}
