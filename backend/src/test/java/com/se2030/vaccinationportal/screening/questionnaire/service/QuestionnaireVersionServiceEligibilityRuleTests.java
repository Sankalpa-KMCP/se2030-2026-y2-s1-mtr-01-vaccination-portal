package com.se2030.vaccinationportal.screening.questionnaire.service;

import com.se2030.vaccinationportal.screening.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.screening.questionnaire.model.EligibilityRule;
import com.se2030.vaccinationportal.screening.submission.model.PreliminaryScreeningResult;
import com.se2030.vaccinationportal.screening.questionnaire.model.Questionnaire;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireVersion;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireVersionStatus;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionnaireVersionServiceEligibilityRuleTests {

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

        Questionnaire questionnaire = new Questionnaire("PRE_VAX", "Pre-vaccination", "Screening questionnaire");
        questionnaire.setId(10L);
        version = new QuestionnaireVersion(questionnaire, 1, QuestionnaireVersionStatus.DRAFT);
        version.setId(20L);
        rule = new EligibilityRule(
                "FEVER",
                version,
                "Fever review",
                "FEVER == true",
                PreliminaryScreeningResult.PRELIMINARY_REVIEW_REQUIRED,
                10,
                "Clinical review required"
        );
        rule.setId(30L);
    }

    @Test
    void createsEligibilityRuleForDraftVersion() {
        when(versionRepository.findById(20L)).thenReturn(Optional.of(version));
        when(ruleRepository.existsByQuestionnaireVersionIdAndRuleCode(20L, "ALLERGY")).thenReturn(false);
        when(ruleRepository.save(any(EligibilityRule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EligibilityRule created = service.addEligibilityRule(
                20L,
                " ALLERGY ",
                " Allergy review ",
                " ALLERGY == severe ",
                PreliminaryScreeningResult.PRELIMINARY_REVIEW_REQUIRED,
                20,
                " Review allergy history "
        );

        assertThat(created.getQuestionnaireVersion()).isSameAs(version);
        assertThat(created.getRuleCode()).isEqualTo("ALLERGY");
        assertThat(created.getRuleName()).isEqualTo("Allergy review");
        assertThat(created.getConditionExpression()).isEqualTo("ALLERGY == severe");
        assertThat(created.getMessage()).isEqualTo("Review allergy history");
    }

    @Test
    void listsEligibilityRulesForExistingVersion() {
        when(versionRepository.existsById(20L)).thenReturn(true);
        when(ruleRepository.findByQuestionnaireVersionIdOrderByPriorityAsc(20L)).thenReturn(List.of(rule));

        assertThat(service.getEligibilityRulesForVersion(20L)).containsExactly(rule);
    }

    @Test
    void readsEligibilityRuleById() {
        when(ruleRepository.findById(30L)).thenReturn(Optional.of(rule));

        assertThat(service.getEligibilityRuleById(30L)).isSameAs(rule);
    }

    @Test
    void updatesBusinessFieldsWithoutChangingOwningVersion() {
        when(ruleRepository.findById(30L)).thenReturn(Optional.of(rule));
        when(ruleRepository.existsByQuestionnaireVersionIdAndRuleCode(20L, "HIGH_FEVER")).thenReturn(false);
        when(ruleRepository.save(rule)).thenReturn(rule);

        EligibilityRule updated = service.updateEligibilityRule(
                30L,
                " HIGH_FEVER ",
                " High fever review ",
                " TEMPERATURE >= 39 ",
                PreliminaryScreeningResult.PRELIMINARY_REVIEW_REQUIRED,
                5,
                " Defer pending clinical review "
        );

        assertThat(updated.getQuestionnaireVersion()).isSameAs(version);
        assertThat(updated.getRuleCode()).isEqualTo("HIGH_FEVER");
        assertThat(updated.getRuleName()).isEqualTo("High fever review");
        assertThat(updated.getConditionExpression()).isEqualTo("TEMPERATURE >= 39");
        assertThat(updated.getPriority()).isEqualTo(5);
        assertThat(updated.getMessage()).isEqualTo("Defer pending clinical review");
    }

    @Test
    void rejectsDuplicateEligibilityRuleCodeWithinVersion() {
        when(ruleRepository.findById(30L)).thenReturn(Optional.of(rule));
        when(ruleRepository.existsByQuestionnaireVersionIdAndRuleCode(20L, "ALLERGY")).thenReturn(true);

        assertThatThrownBy(() -> service.updateEligibilityRule(
                30L,
                "ALLERGY",
                "Allergy review",
                "ALLERGY == true",
                PreliminaryScreeningResult.PRELIMINARY_REVIEW_REQUIRED,
                10,
                "Review required"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already exists in this version");

        verify(ruleRepository, never()).save(any());
    }

    @Test
    void deletesEligibilityRule() {
        when(ruleRepository.findById(30L)).thenReturn(Optional.of(rule));

        service.deleteEligibilityRule(30L);

        verify(ruleRepository).delete(rule);
    }

    @Test
    void rejectsNonexistentEligibilityRuleId() {
        when(ruleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getEligibilityRuleById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Eligibility rule not found with ID: 999");
    }

    @Test
    void rejectsNonexistentQuestionnaireVersionWhenListingRules() {
        when(versionRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.getEligibilityRulesForVersion(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Questionnaire version not found with ID: 999");
    }
}
