package com.se2030.vaccinationportal.screening.questionnaire.service;

import com.se2030.vaccinationportal.screening.questionnaire.model.EligibilityRule;
import com.se2030.vaccinationportal.screening.submission.model.PreliminaryScreeningResult;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionType;
import com.se2030.vaccinationportal.screening.questionnaire.model.Questionnaire;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireItem;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireVersion;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireVersionStatus;
import com.se2030.vaccinationportal.screening.questionnaire.model.ScreeningQuestion;
import com.se2030.vaccinationportal.screening.questionnaire.repository.EligibilityRuleRepository;
import com.se2030.vaccinationportal.screening.questionnaire.repository.QuestionnaireItemRepository;
import com.se2030.vaccinationportal.screening.questionnaire.repository.QuestionnaireRepository;
import com.se2030.vaccinationportal.screening.questionnaire.repository.QuestionnaireVersionRepository;
import com.se2030.vaccinationportal.screening.questionnaire.repository.ScreeningQuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionnaireVersionServiceMutabilityTests {

    @Mock
    private QuestionnaireRepository questionnaireRepository;

    @Mock
    private QuestionnaireVersionRepository versionRepository;

    @Mock
    private QuestionnaireItemRepository itemRepository;

    @Mock
    private EligibilityRuleRepository ruleRepository;

    @Mock
    private ScreeningQuestionRepository questionRepository;

    private QuestionnaireVersionService service;
    private QuestionnaireVersion version;
    private ScreeningQuestion question;
    private EligibilityRule rule;

    @BeforeEach
    void setUp() {
        service = new QuestionnaireVersionService(
                questionnaireRepository,
                versionRepository,
                itemRepository,
                ruleRepository,
                questionRepository,
                new QuestionnaireMutabilityValidator(questionRepository)
        );
        Questionnaire questionnaire = new Questionnaire("PRE_VAX", "Pre-vaccination", null);
        questionnaire.setId(10L);
        version = new QuestionnaireVersion(questionnaire, 1, QuestionnaireVersionStatus.DRAFT);
        version.setId(20L);
        question = new ScreeningQuestion("ALLERGY", "Do you have allergies?", QuestionType.SINGLE_CHOICE, true, true);
        question.setId(30L);
        rule = new EligibilityRule(
                "ALLERGY",
                version,
                "Allergy review",
                "ALLERGY == true",
                PreliminaryScreeningResult.PRELIMINARY_REVIEW_REQUIRED,
                10,
                "Review required"
        );
        rule.setId(40L);
    }

    @Test
    void questionItemCanBeAddedToDraftVersion() {
        when(versionRepository.findById(20L)).thenReturn(Optional.of(version));
        when(questionRepository.findById(30L)).thenReturn(Optional.of(question));
        when(itemRepository.save(any(QuestionnaireItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QuestionnaireItem item = service.addQuestionItem(20L, 30L, 1, true);

        assertThat(item.getQuestionnaireVersion()).isSameAs(version);
        assertThat(item.getQuestion()).isSameAs(question);
        assertThat(item.getDisplayOrder()).isEqualTo(1);
    }

    @Test
    void questionItemWithPositiveDisplayOrderGreaterThanOneCanBeAdded() {
        when(versionRepository.findById(20L)).thenReturn(Optional.of(version));
        when(questionRepository.findById(30L)).thenReturn(Optional.of(question));
        when(itemRepository.save(any(QuestionnaireItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QuestionnaireItem item = service.addQuestionItem(20L, 30L, 5, null);

        assertThat(item.getDisplayOrder()).isEqualTo(5);
    }

    @Test
    void questionItemWithZeroDisplayOrderIsRejected() {
        when(versionRepository.findById(20L)).thenReturn(Optional.of(version));

        assertThatThrownBy(() -> service.addQuestionItem(20L, 30L, 0, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Display order must be at least 1");

        verify(itemRepository, never()).save(any());
    }

    @Test
    void questionItemWithNegativeDisplayOrderIsRejected() {
        when(versionRepository.findById(20L)).thenReturn(Optional.of(version));

        assertThatThrownBy(() -> service.addQuestionItem(20L, 30L, -1, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Display order must be at least 1");

        verify(itemRepository, never()).save(any());
    }

    @Test
    void questionItemCannotBeAddedToPublishedVersion() {
        version.setStatus(QuestionnaireVersionStatus.PUBLISHED);
        when(versionRepository.findById(20L)).thenReturn(Optional.of(version));

        assertThatThrownBy(() -> service.addQuestionItem(20L, 30L, 1, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not in DRAFT status")
                .hasMessageContaining("PUBLISHED");

        verify(itemRepository, never()).save(any());
    }

    @Test
    void questionItemCannotBeAddedToRetiredVersion() {
        version.setStatus(QuestionnaireVersionStatus.RETIRED);
        when(versionRepository.findById(20L)).thenReturn(Optional.of(version));

        assertThatThrownBy(() -> service.addQuestionItem(20L, 30L, 1, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RETIRED");

        verify(itemRepository, never()).save(any());
    }

    @Test
    void ruleInPublishedVersionCannotBeUpdated() {
        version.setStatus(QuestionnaireVersionStatus.PUBLISHED);
        when(ruleRepository.findById(40L)).thenReturn(Optional.of(rule));

        assertThatThrownBy(() -> updateRule())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not in DRAFT status")
                .hasMessageContaining("PUBLISHED");

        verify(ruleRepository, never()).save(any());
    }

    @Test
    void ruleInPublishedVersionCannotBeDeleted() {
        version.setStatus(QuestionnaireVersionStatus.PUBLISHED);
        when(ruleRepository.findById(40L)).thenReturn(Optional.of(rule));

        assertThatThrownBy(() -> service.deleteEligibilityRule(40L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PUBLISHED");

        verify(ruleRepository, never()).delete(any());
    }

    @Test
    void ruleInRetiredVersionCannotBeUpdatedOrDeleted() {
        version.setStatus(QuestionnaireVersionStatus.RETIRED);
        when(ruleRepository.findById(40L)).thenReturn(Optional.of(rule));

        assertThatThrownBy(() -> updateRule())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RETIRED");
        assertThatThrownBy(() -> service.deleteEligibilityRule(40L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RETIRED");

        verify(ruleRepository, never()).save(any());
        verify(ruleRepository, never()).delete(any());
    }

    @Test
    void ruleCannotBeAddedToPublishedVersion() {
        version.setStatus(QuestionnaireVersionStatus.PUBLISHED);
        when(versionRepository.findById(20L)).thenReturn(Optional.of(version));

        assertThatThrownBy(() -> service.addEligibilityRule(
                20L,
                "FEVER",
                "Fever review",
                "FEVER == true",
                PreliminaryScreeningResult.PRELIMINARY_REVIEW_REQUIRED,
                20,
                "Review required"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PUBLISHED");

        verify(ruleRepository, never()).save(any());
    }

    @Test
    void publishedVersionCannotBePermanentlyDeleted() {
        version.setStatus(QuestionnaireVersionStatus.PUBLISHED);
        when(versionRepository.findById(20L)).thenReturn(Optional.of(version));

        assertThatThrownBy(() -> service.deleteDraftVersion(20L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PUBLISHED");

        verify(versionRepository, never()).delete(any());
    }

    @Test
    void retiredVersionCannotBePermanentlyDeleted() {
        version.setStatus(QuestionnaireVersionStatus.RETIRED);
        when(versionRepository.findById(20L)).thenReturn(Optional.of(version));

        assertThatThrownBy(() -> service.deleteDraftVersion(20L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RETIRED");

        verify(versionRepository, never()).delete(any());
    }

    @Test
    void versionWithHistoricalSubmissionCannotBePermanentlyDeleted() {
        when(versionRepository.findById(20L)).thenReturn(Optional.of(version));
        when(versionRepository.hasSubmissions(20L)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteDraftVersion(20L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("associated screening submissions");

        verify(versionRepository, never()).delete(any());
    }

    @Test
    void unusedDraftVersionCanBePermanentlyDeleted() {
        when(versionRepository.findById(20L)).thenReturn(Optional.of(version));

        service.deleteDraftVersion(20L);

        verify(versionRepository).delete(version);
    }

    private EligibilityRule updateRule() {
        return service.updateEligibilityRule(
                40L,
                "ALLERGY",
                "Changed allergy rule",
                "ALLERGY == severe",
                PreliminaryScreeningResult.PRELIMINARY_REVIEW_REQUIRED,
                5,
                "Changed historical meaning"
        );
    }
}
