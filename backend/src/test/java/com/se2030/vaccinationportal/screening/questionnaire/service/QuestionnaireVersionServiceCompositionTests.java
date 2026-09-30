package com.se2030.vaccinationportal.screening.questionnaire.service;

import com.se2030.vaccinationportal.screening.questionnaire.model.EligibilityRule;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionType;
import com.se2030.vaccinationportal.screening.questionnaire.model.Questionnaire;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireItem;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireVersion;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireVersionStatus;
import com.se2030.vaccinationportal.screening.questionnaire.model.ScreeningQuestion;
import com.se2030.vaccinationportal.screening.submission.model.PreliminaryScreeningResult;
import com.se2030.vaccinationportal.screening.submission.service.PreliminaryScreeningService;


import com.se2030.vaccinationportal.screening.questionnaire.repository.EligibilityRuleRepository;
import com.se2030.vaccinationportal.screening.questionnaire.repository.QuestionnaireItemRepository;
import com.se2030.vaccinationportal.screening.questionnaire.repository.QuestionnaireRepository;
import com.se2030.vaccinationportal.screening.questionnaire.repository.QuestionnaireVersionRepository;
import com.se2030.vaccinationportal.screening.questionnaire.repository.ScreeningQuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionnaireVersionServiceCompositionTests {

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
    private ScreeningQuestion question1;
    private ScreeningQuestion question2;
    private ScreeningQuestion question3;
    private QuestionnaireItem item1;
    private QuestionnaireItem item2;
    private QuestionnaireItem item3;

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

        Questionnaire questionnaire = new Questionnaire("SCREENING", "Screening Container", null);
        questionnaire.setId(10L);

        version = new QuestionnaireVersion(questionnaire, 1, QuestionnaireVersionStatus.DRAFT);
        version.setId(20L);

        question1 = new ScreeningQuestion("FEVER", "Do you have a fever?", QuestionType.BOOLEAN, true, true);
        question1.setId(101L);

        question2 = new ScreeningQuestion("COUGH", "Do you have a cough?", QuestionType.BOOLEAN, false, true);
        question2.setId(102L);

        question3 = new ScreeningQuestion("AGE", "What is your age?", QuestionType.NUMERIC, true, true);
        question3.setId(103L);

        item1 = new QuestionnaireItem(version, question1, 1, true);
        item1.setId(201L);

        item2 = new QuestionnaireItem(version, question2, 2, false);
        item2.setId(202L);

        item3 = new QuestionnaireItem(version, question3, 3, null);
        item3.setId(203L);

        version.addItem(item1);
        version.addItem(item2);
        version.addItem(item3);
    }

    @Test
    void removeItemFromDraftVersionNormalizesContiguousOrderAndPreservesQuestion() {
        when(versionRepository.findById(20L)).thenReturn(Optional.of(version));
        when(itemRepository.findById(202L)).thenReturn(Optional.of(item2));

        // After deleting item2, remaining items are item1 (order 1) and item3 (order 3)
        List<QuestionnaireItem> remaining = new ArrayList<>(List.of(item1, item3));
        when(itemRepository.findByQuestionnaireVersionIdOrderByDisplayOrderAsc(20L)).thenReturn(remaining);

        service.removeQuestionItem(20L, 202L);

        // Verify item was deleted from repository
        verify(itemRepository).delete(item2);
        verify(itemRepository, atLeastOnce()).flush();

        // Verify questionRepository.delete was NEVER called (screening_question preserved)
        verify(questionRepository, never()).delete(any());
        verify(questionRepository, never()).deleteById(anyLong());

        // Verify display order was normalized: item1 -> 1, item3 -> 2
        assertThat(item1.getDisplayOrder()).isEqualTo(1);
        assertThat(item3.getDisplayOrder()).isEqualTo(2);
    }

    @Test
    void removeItemBelongingToAnotherVersionIsRejected() {
        QuestionnaireVersion otherVersion = new QuestionnaireVersion();
        otherVersion.setId(999L);
        otherVersion.setStatus(QuestionnaireVersionStatus.DRAFT);
        item2.setQuestionnaireVersion(otherVersion);

        when(versionRepository.findById(20L)).thenReturn(Optional.of(version));
        when(itemRepository.findById(202L)).thenReturn(Optional.of(item2));

        assertThatThrownBy(() -> service.removeQuestionItem(20L, 202L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to version 20");

        verify(itemRepository, never()).delete(any());
    }

    @Test
    void removeItemReferencedByEligibilityRuleIsBlocked() {
        EligibilityRule rule = new EligibilityRule(
                "FEVER_RULE",
                version,
                "Fever review",
                "FEVER == true",
                PreliminaryScreeningResult.PRELIMINARY_REVIEW_REQUIRED,
                1,
                "Review fever"
        );
        version.addRule(rule);

        when(versionRepository.findById(20L)).thenReturn(Optional.of(version));
        when(itemRepository.findById(201L)).thenReturn(Optional.of(item1));

        assertThatThrownBy(() -> service.removeQuestionItem(20L, 201L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Question 'FEVER' is used by an eligibility rule ('FEVER_RULE')");

        verify(itemRepository, never()).delete(any());
    }

    @Test
    void removeItemReferencedByDemoFeverRuleIsBlocked() {
        ScreeningQuestion demoQuestion = new ScreeningQuestion("DEMO_2026_FEVER", "Fever test", QuestionType.BOOLEAN, false, true);
        demoQuestion.setId(109L);
        QuestionnaireItem demoItem = new QuestionnaireItem(version, demoQuestion, 1, false);
        demoItem.setId(209L);

        EligibilityRule rule = new EligibilityRule(
                "DEMO_RULE",
                version,
                "Demo review",
                "DEMO_2026_FEVER == true",
                PreliminaryScreeningResult.PRELIMINARY_REVIEW_REQUIRED,
                1,
                "Review demo fever"
        );
        version.addRule(rule);

        when(versionRepository.findById(20L)).thenReturn(Optional.of(version));
        when(itemRepository.findById(209L)).thenReturn(Optional.of(demoItem));

        assertThatThrownBy(() -> service.removeQuestionItem(20L, 209L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Question 'DEMO_2026_FEVER' is used by an eligibility rule ('DEMO_RULE')");

        verify(itemRepository, never()).delete(any());
    }

    @Test
    void removeItemReferencedByInequalityRuleIsBlocked() {
        EligibilityRule rule = new EligibilityRule(
                "AGE_RULE",
                version,
                "Age review",
                "AGE != 18",
                PreliminaryScreeningResult.PRELIMINARY_REVIEW_REQUIRED,
                1,
                "Review age"
        );
        version.addRule(rule);

        when(versionRepository.findById(20L)).thenReturn(Optional.of(version));
        when(itemRepository.findById(203L)).thenReturn(Optional.of(item3)); // item3 is AGE

        assertThatThrownBy(() -> service.removeQuestionItem(20L, 203L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Question 'AGE' is used by an eligibility rule ('AGE_RULE')");

        verify(itemRepository, never()).delete(any());
    }

    @Test
    void replaceItemReferencedByContainsRuleIsBlocked() {
        ScreeningQuestion symptomsQuestion = new ScreeningQuestion("SYMPTOMS", "Symptoms check", QuestionType.TEXT, false, true);
        symptomsQuestion.setId(110L);
        QuestionnaireItem symptomsItem = new QuestionnaireItem(version, symptomsQuestion, 1, false);
        symptomsItem.setId(210L);

        EligibilityRule rule = new EligibilityRule(
                "SYMPTOMS_RULE",
                version,
                "Symptoms review",
                "SYMPTOMS CONTAINS \"Fever\"",
                PreliminaryScreeningResult.PRELIMINARY_REVIEW_REQUIRED,
                1,
                "Review symptoms"
        );
        version.addRule(rule);

        ScreeningQuestion replacement = new ScreeningQuestion("HEADACHE", "Headache check", QuestionType.BOOLEAN, false, true);
        replacement.setId(111L);

        when(versionRepository.findById(20L)).thenReturn(Optional.of(version));
        when(itemRepository.findById(210L)).thenReturn(Optional.of(symptomsItem));
        when(questionRepository.findById(111L)).thenReturn(Optional.of(replacement));
        when(itemRepository.existsByQuestionnaireVersionIdAndQuestionId(20L, 111L)).thenReturn(false);

        assertThatThrownBy(() -> service.replaceQuestionItem(20L, 210L, 111L, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Question 'SYMPTOMS' is used by an eligibility rule ('SYMPTOMS_RULE')");

        verify(itemRepository, never()).save(any());
    }

    @Test
    void ruleDependencyCheckDoesNotFalsePositiveOnSimilarIdentifiers() {
        // Rule uses AGE_YEARS, but item uses AGE
        EligibilityRule rule = new EligibilityRule(
                "AGE_YEARS_RULE",
                version,
                "Age in years review",
                "AGE_YEARS == 18",
                PreliminaryScreeningResult.PRELIMINARY_REVIEW_REQUIRED,
                1,
                "Review age in years"
        );
        version.addRule(rule);

        when(versionRepository.findById(20L)).thenReturn(Optional.of(version));
        when(itemRepository.findById(203L)).thenReturn(Optional.of(item3)); // item3 is AGE

        List<QuestionnaireItem> remaining = new ArrayList<>(List.of(item1, item2));
        when(itemRepository.findByQuestionnaireVersionIdOrderByDisplayOrderAsc(20L)).thenReturn(remaining);

        // Should NOT throw because AGE != AGE_YEARS
        service.removeQuestionItem(20L, 203L);

        verify(itemRepository).delete(item3);
    }

    @Test
    void ruleDependencyCheckBlocksRemovalWhenItemMatchesExactly() {
        // Rule uses AGE == 18, and item uses AGE
        EligibilityRule rule = new EligibilityRule(
                "EXACT_AGE_RULE",
                version,
                "Exact age review",
                "AGE == 18",
                PreliminaryScreeningResult.PRELIMINARY_REVIEW_REQUIRED,
                1,
                "Review exact age"
        );
        version.addRule(rule);

        when(versionRepository.findById(20L)).thenReturn(Optional.of(version));
        when(itemRepository.findById(203L)).thenReturn(Optional.of(item3)); // item3 is AGE

        assertThatThrownBy(() -> service.removeQuestionItem(20L, 203L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Question 'AGE' is used by an eligibility rule ('EXACT_AGE_RULE')");

        verify(itemRepository, never()).delete(any());
    }

    @Test
    void removeItemFromPublishedOrRetiredVersionIsRejected() {
        version.setStatus(QuestionnaireVersionStatus.PUBLISHED);
        when(versionRepository.findById(20L)).thenReturn(Optional.of(version));

        assertThatThrownBy(() -> service.removeQuestionItem(20L, 201L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not in DRAFT status");

        version.setStatus(QuestionnaireVersionStatus.RETIRED);
        assertThatThrownBy(() -> service.removeQuestionItem(20L, 201L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not in DRAFT status");

        verify(itemRepository, never()).delete(any());
    }

    @Test
    void replaceQuestionInDraftVersionPreservesDisplayOrderAndOverrides() {
        ScreeningQuestion replacement = new ScreeningQuestion("HEADACHE", "Do you have headache?", QuestionType.BOOLEAN, false, true);
        replacement.setId(104L);

        when(versionRepository.findById(20L)).thenReturn(Optional.of(version));
        when(itemRepository.findById(202L)).thenReturn(Optional.of(item2));
        when(questionRepository.findById(104L)).thenReturn(Optional.of(replacement));
        when(itemRepository.existsByQuestionnaireVersionIdAndQuestionId(20L, 104L)).thenReturn(false);
        when(itemRepository.save(any(QuestionnaireItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QuestionnaireItem updated = service.replaceQuestionItem(20L, 202L, 104L, null);

        assertThat(updated.getQuestion()).isSameAs(replacement);
        assertThat(updated.getDisplayOrder()).isEqualTo(2); // Preserved displayOrder
        assertThat(updated.getRequiredOverride()).isFalse(); // Preserved existing override
    }

    @Test
    void replaceQuestionWithExplicitRequiredOverrideUpdatesBoth() {
        ScreeningQuestion replacement = new ScreeningQuestion("HEADACHE", "Do you have headache?", QuestionType.BOOLEAN, false, true);
        replacement.setId(104L);

        when(versionRepository.findById(20L)).thenReturn(Optional.of(version));
        when(itemRepository.findById(202L)).thenReturn(Optional.of(item2));
        when(questionRepository.findById(104L)).thenReturn(Optional.of(replacement));
        when(itemRepository.existsByQuestionnaireVersionIdAndQuestionId(20L, 104L)).thenReturn(false);
        when(itemRepository.save(any(QuestionnaireItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QuestionnaireItem updated = service.replaceQuestionItem(20L, 202L, 104L, true);

        assertThat(updated.getQuestion()).isSameAs(replacement);
        assertThat(updated.getRequiredOverride()).isTrue();
    }

    @Test
    void replaceQuestionWithSameQuestionIdAllowsOverrideUpdateWithoutDuplicateError() {
        when(versionRepository.findById(20L)).thenReturn(Optional.of(version));
        when(itemRepository.findById(201L)).thenReturn(Optional.of(item1));
        when(questionRepository.findById(101L)).thenReturn(Optional.of(question1));
        when(itemRepository.save(any(QuestionnaireItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QuestionnaireItem updated = service.replaceQuestionItem(20L, 201L, 101L, false);

        assertThat(updated.getQuestion()).isSameAs(question1);
        assertThat(updated.getRequiredOverride()).isFalse();
        verify(itemRepository, never()).existsByQuestionnaireVersionIdAndQuestionId(anyLong(), anyLong());
    }

    @Test
    void replaceQuestionWithInactiveQuestionIsRejected() {
        ScreeningQuestion inactive = new ScreeningQuestion("INACTIVE_Q", "Inactive question", QuestionType.BOOLEAN, false, false);
        inactive.setId(105L);

        when(versionRepository.findById(20L)).thenReturn(Optional.of(version));
        when(itemRepository.findById(202L)).thenReturn(Optional.of(item2));
        when(questionRepository.findById(105L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> service.replaceQuestionItem(20L, 202L, 105L, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot add inactive question");

        verify(itemRepository, never()).save(any());
    }

    @Test
    void replaceQuestionWithAlreadyAttachedQuestionIsRejected() {
        when(versionRepository.findById(20L)).thenReturn(Optional.of(version));
        when(itemRepository.findById(202L)).thenReturn(Optional.of(item2));
        when(questionRepository.findById(103L)).thenReturn(Optional.of(question3));
        when(itemRepository.existsByQuestionnaireVersionIdAndQuestionId(20L, 103L)).thenReturn(true);

        assertThatThrownBy(() -> service.replaceQuestionItem(20L, 202L, 103L, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already included in this questionnaire version");

        verify(itemRepository, never()).save(any());
    }

    @Test
    void replaceQuestionReferencedByRuleIsBlocked() {
        EligibilityRule rule = new EligibilityRule(
                "COUGH_RULE",
                version,
                "Cough review",
                "COUGH == true",
                PreliminaryScreeningResult.PRELIMINARY_REVIEW_REQUIRED,
                1,
                "Review cough"
        );
        version.addRule(rule);

        ScreeningQuestion replacement = new ScreeningQuestion("HEADACHE", "Do you have headache?", QuestionType.BOOLEAN, false, true);
        replacement.setId(104L);

        when(versionRepository.findById(20L)).thenReturn(Optional.of(version));
        when(itemRepository.findById(202L)).thenReturn(Optional.of(item2));
        when(questionRepository.findById(104L)).thenReturn(Optional.of(replacement));
        when(itemRepository.existsByQuestionnaireVersionIdAndQuestionId(20L, 104L)).thenReturn(false);

        assertThatThrownBy(() -> service.replaceQuestionItem(20L, 202L, 104L, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Question 'COUGH' is used by an eligibility rule ('COUGH_RULE')");

        verify(itemRepository, never()).save(any());
    }

    @Test
    void replaceQuestionInPublishedOrRetiredVersionIsRejected() {
        version.setStatus(QuestionnaireVersionStatus.PUBLISHED);
        when(versionRepository.findById(20L)).thenReturn(Optional.of(version));

        assertThatThrownBy(() -> service.replaceQuestionItem(20L, 201L, 102L, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not in DRAFT status");

        version.setStatus(QuestionnaireVersionStatus.RETIRED);
        assertThatThrownBy(() -> service.replaceQuestionItem(20L, 201L, 102L, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not in DRAFT status");
    }

    @Test
    void reorderQuestionItemsSucceedsForCompleteSetOfItems() {
        when(versionRepository.findById(20L)).thenReturn(Optional.of(version));
        when(itemRepository.findByQuestionnaireVersionIdOrderByDisplayOrderAsc(20L))
                .thenReturn(List.of(item1, item2, item3));

        // Reorder: 203 (was 3), 201 (was 1), 202 (was 2)
        service.reorderQuestionItems(20L, List.of(203L, 201L, 202L));

        assertThat(item3.getDisplayOrder()).isEqualTo(1);
        assertThat(item1.getDisplayOrder()).isEqualTo(2);
        assertThat(item2.getDisplayOrder()).isEqualTo(3);

        verify(itemRepository, atLeast(2)).saveAllAndFlush(any());
    }

    @Test
    void reorderQuestionItemsRejectsMissingItems() {
        when(versionRepository.findById(20L)).thenReturn(Optional.of(version));
        when(itemRepository.findByQuestionnaireVersionIdOrderByDisplayOrderAsc(20L))
                .thenReturn(List.of(item1, item2, item3));

        assertThatThrownBy(() -> service.reorderQuestionItems(20L, List.of(203L, 201L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must include all questionnaire items for this version exactly once");

        verify(itemRepository, never()).saveAllAndFlush(any());
    }

    @Test
    void reorderQuestionItemsRejectsDuplicateIds() {
        when(versionRepository.findById(20L)).thenReturn(Optional.of(version));
        when(itemRepository.findByQuestionnaireVersionIdOrderByDisplayOrderAsc(20L))
                .thenReturn(List.of(item1, item2, item3));

        assertThatThrownBy(() -> service.reorderQuestionItems(20L, List.of(201L, 201L, 202L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate item IDs");

        verify(itemRepository, never()).saveAllAndFlush(any());
    }

    @Test
    void reorderQuestionItemsRejectsItemFromAnotherVersion() {
        when(versionRepository.findById(20L)).thenReturn(Optional.of(version));
        when(itemRepository.findByQuestionnaireVersionIdOrderByDisplayOrderAsc(20L))
                .thenReturn(List.of(item1, item2, item3));

        assertThatThrownBy(() -> service.reorderQuestionItems(20L, List.of(201L, 202L, 999L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to version 20");

        verify(itemRepository, never()).saveAllAndFlush(any());
    }

    @Test
    void reorderQuestionItemsInPublishedOrRetiredVersionIsRejected() {
        version.setStatus(QuestionnaireVersionStatus.PUBLISHED);
        when(versionRepository.findById(20L)).thenReturn(Optional.of(version));

        assertThatThrownBy(() -> service.reorderQuestionItems(20L, List.of(201L, 202L, 203L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not in DRAFT status");

        version.setStatus(QuestionnaireVersionStatus.RETIRED);
        assertThatThrownBy(() -> service.reorderQuestionItems(20L, List.of(201L, 202L, 203L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not in DRAFT status");
    }

    @Test
    void extractReferencedQuestionCodeWithRealSupportedExpressions() {
        // DEMO_2026_FEVER == true -> DEMO_2026_FEVER
        assertThat(QuestionnaireVersionService.extractReferencedQuestionCode("DEMO_2026_FEVER == true"))
                .isEqualTo("DEMO_2026_FEVER");

        // AGE != 18 -> AGE
        assertThat(QuestionnaireVersionService.extractReferencedQuestionCode("AGE != 18"))
                .isEqualTo("AGE");

        // SYMPTOMS CONTAINS "Fever" -> SYMPTOMS
        assertThat(QuestionnaireVersionService.extractReferencedQuestionCode("SYMPTOMS CONTAINS \"Fever\""))
                .isEqualTo("SYMPTOMS");

        // symptoms contains rash (case-insensitivity of operator)
        assertThat(QuestionnaireVersionService.extractReferencedQuestionCode("symptoms contains rash"))
                .isEqualTo("symptoms");

        // AGE_YEARS == 18 -> AGE_YEARS (must NOT match AGE)
        String ageYearsCode = QuestionnaireVersionService.extractReferencedQuestionCode("AGE_YEARS == 18");
        assertThat(ageYearsCode).isEqualTo("AGE_YEARS");
        assertThat(ageYearsCode.equalsIgnoreCase("AGE")).isFalse();

        // AGE == 18 -> AGE (must match AGE)
        String ageCode = QuestionnaireVersionService.extractReferencedQuestionCode("AGE == 18");
        assertThat(ageCode).isEqualTo("AGE");
        assertThat(ageCode.equalsIgnoreCase("AGE")).isTrue();

        // expressions without spaces around operators
        assertThat(QuestionnaireVersionService.extractReferencedQuestionCode("AGE==18"))
                .isEqualTo("AGE");
        assertThat(QuestionnaireVersionService.extractReferencedQuestionCode("AGE!=18"))
                .isEqualTo("AGE");

        // Whitespace variations
        assertThat(QuestionnaireVersionService.extractReferencedQuestionCode("  ALLERGY == severe  "))
                .isEqualTo("ALLERGY");

        // Non-executable comparison operators are not supported by PreliminaryScreeningService
        // and therefore must not be treated as valid rule expressions
        assertThat(QuestionnaireVersionService.extractReferencedQuestionCode("TEMPERATURE >= 39"))
                .isNull();
        assertThat(QuestionnaireVersionService.extractReferencedQuestionCode("DOSE_COUNT <= 2"))
                .isNull();
        assertThat(QuestionnaireVersionService.extractReferencedQuestionCode("SCORE > 5"))
                .isNull();
        assertThat(QuestionnaireVersionService.extractReferencedQuestionCode("WEIGHT < 40"))
                .isNull();
    }
}
