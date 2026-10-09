package com.se2030.vaccinationportal.screening.questionnaire.service;

import com.se2030.vaccinationportal.screening.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.screening.questionnaire.model.Questionnaire;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireVersionStatus;
import com.se2030.vaccinationportal.screening.questionnaire.repository.AnswerOptionRepository;
import com.se2030.vaccinationportal.screening.questionnaire.repository.QuestionnaireRepository;
import com.se2030.vaccinationportal.screening.questionnaire.repository.QuestionnaireVersionRepository;
import com.se2030.vaccinationportal.screening.questionnaire.repository.ScreeningQuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScreeningConfigurationServiceQuestionnaireDeletionTests {

    @Mock private ScreeningQuestionRepository questionRepository;
    @Mock private AnswerOptionRepository optionRepository;
    @Mock private QuestionnaireRepository questionnaireRepository;
    @Mock private QuestionnaireVersionRepository versionRepository;

    private ScreeningConfigurationService service;

    @BeforeEach
    void setUp() {
        service = new ScreeningConfigurationService(questionRepository, optionRepository,
                questionnaireRepository, versionRepository, new QuestionnaireMutabilityValidator(questionRepository));
    }

    @Test
    void deletesOnlyAnExistingEmptyQuestionnaire() {
        when(questionnaireRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(questionnaire()));
        when(questionnaireRepository.deleteIfWithoutVersions(7L)).thenReturn(1);

        service.deleteQuestionnaire(7L);

        verify(versionRepository).existsByQuestionnaireId(7L);
        verify(questionnaireRepository).deleteIfWithoutVersions(7L);
        verify(versionRepository, never()).deleteAll();
    }

    @ParameterizedTest
    @EnumSource(QuestionnaireVersionStatus.class)
    void rejectsEveryVersionStatusWithoutDeletingAnything(QuestionnaireVersionStatus status) {
        when(questionnaireRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(questionnaire()));
        when(versionRepository.existsByQuestionnaireId(7L)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteQuestionnaire(7L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("contains versions");

        verify(questionnaireRepository, never()).deleteIfWithoutVersions(7L);
    }

    @Test
    void missingQuestionnaireIsNotFound() {
        assertThatThrownBy(() -> service.deleteQuestionnaire(7L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(versionRepository, never()).existsByQuestionnaireId(7L);
    }

    @Test
    void concurrentVersionInsertCannotBeOverriddenByDelete() {
        when(questionnaireRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(questionnaire()));
        when(questionnaireRepository.deleteIfWithoutVersions(7L)).thenReturn(0);

        assertThatThrownBy(() -> service.deleteQuestionnaire(7L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("now referenced");
    }

    @Test
    void otherDatabaseReferenceBecomesSafeConflictWithoutSqlDetails() {
        when(questionnaireRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(questionnaire()));
        when(questionnaireRepository.deleteIfWithoutVersions(7L))
                .thenThrow(new DataIntegrityViolationException("database constraint details"));

        assertThatThrownBy(() -> service.deleteQuestionnaire(7L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining("database constraint details");
    }

    private Questionnaire questionnaire() {
        Questionnaire questionnaire = new Questionnaire("EMPTY", "Empty", null);
        questionnaire.setId(7L);
        return questionnaire;
    }
}
