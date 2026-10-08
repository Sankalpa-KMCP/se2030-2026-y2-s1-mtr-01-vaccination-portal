package com.se2030.vaccinationportal.screening.questionnaire.service;

import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireVersion;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireVersionStatus;
import com.se2030.vaccinationportal.screening.questionnaire.repository.ScreeningQuestionRepository;
import org.springframework.stereotype.Component;

/**
 * Central lifecycle guard for screening configuration shared across services.
 */
@Component
public class QuestionnaireMutabilityValidator {

    private final ScreeningQuestionRepository questionRepository;

    public QuestionnaireMutabilityValidator(ScreeningQuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public void assertVersionIsDraft(QuestionnaireVersion version) {
        if (version.getStatus() != QuestionnaireVersionStatus.DRAFT) {
            throw new IllegalStateException(
                    "Questionnaire version " + version.getId()
                            + " cannot be modified because it is not in DRAFT status (current status: "
                            + version.getStatus() + ")"
            );
        }
    }

    public void assertQuestionCanBeUpdated(Long questionId) {
        if (isReferencedByProtectedVersion(questionId)) {
            throw new IllegalStateException(
                    "Screening question cannot be updated because it is referenced by a published or retired questionnaire version"
            );
        }
        if (questionRepository.isReferencedInScreeningAnswers(questionId)) {
            throw new IllegalStateException(
                    "Screening question cannot be updated because it is referenced by historical screening answers"
            );
        }
    }

    public void assertQuestionCanBeDeleted(Long questionId) {
        if (isReferencedByProtectedVersion(questionId)) {
            throw new IllegalStateException(
                    "Screening question cannot be deleted because it is referenced by a published or retired questionnaire version"
            );
        }
        if (questionRepository.isReferencedInQuestionnaireVersion(questionId)) {
            throw new IllegalStateException(
                    "Screening question cannot be deleted because it is referenced by an existing questionnaire version. Deactivate it instead"
            );
        }
        if (questionRepository.isReferencedInScreeningAnswers(questionId)) {
            throw new IllegalStateException(
                    "Screening question cannot be deleted because it is referenced by historical screening answers. Deactivate it instead"
            );
        }
    }

    public void assertAnswerOptionsCanBeModified(Long questionId) {
        if (isReferencedByProtectedVersion(questionId)) {
            throw new IllegalStateException(
                    "Answer options cannot be modified because their question is referenced by a published or retired questionnaire version"
            );
        }
        if (questionRepository.isReferencedInScreeningAnswers(questionId)) {
            throw new IllegalStateException(
                    "Answer options cannot be modified because their question has historical screening answers"
            );
        }
    }

    private boolean isReferencedByProtectedVersion(Long questionId) {
        return questionRepository.isReferencedInProtectedQuestionnaireVersion(
                questionId,
                QuestionnaireVersionStatus.DRAFT
        );
    }
}
