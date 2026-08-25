package com.se2030.vaccinationportal.screening.service;

import com.se2030.vaccinationportal.screening.model.EligibilityRule;
import com.se2030.vaccinationportal.screening.model.PreliminaryScreeningResult;
import com.se2030.vaccinationportal.screening.model.QuestionType;
import com.se2030.vaccinationportal.screening.model.ScreeningAnswer;
import com.se2030.vaccinationportal.screening.model.ScreeningQuestion;
import com.se2030.vaccinationportal.screening.repository.EligibilityRuleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreliminaryScreeningServiceTests {

    private static final Long VERSION_ID = 20L;

    @Mock
    private EligibilityRuleRepository ruleRepository;

    @InjectMocks
    private PreliminaryScreeningService service;

    @Test
    void reviewRequiredRuleDominatesTriggeredPassRuleAndPreservesEvidenceOrder() {
        ScreeningAnswer allergyAnswer = answer("ALLERGY", "yes");
        EligibilityRule passRule = rule(
                "PASS-ALLERGY",
                "ALLERGY == yes",
                PreliminaryScreeningResult.PRELIMINARY_PASS,
                10,
                "Allergy answer recorded"
        );
        EligibilityRule reviewRule = rule(
                "REVIEW-ALLERGY",
                "ALLERGY CONTAINS yes",
                PreliminaryScreeningResult.PRELIMINARY_REVIEW_REQUIRED,
                20,
                "Allergy requires clinical review"
        );
        when(ruleRepository.findByQuestionnaireVersionIdOrderByPriorityAsc(VERSION_ID))
                .thenReturn(List.of(passRule, reviewRule));

        PreliminaryScreeningService.RuleEvaluationResult result =
                service.evaluateAnswers(VERSION_ID, List.of(allergyAnswer));

        assertThat(result.overallResult())
                .isEqualTo(PreliminaryScreeningResult.PRELIMINARY_REVIEW_REQUIRED);
        assertThat(result.triggeredRuleCodes())
                .containsExactly("PASS-ALLERGY", "REVIEW-ALLERGY");
        assertThat(result.guidanceMessages())
                .containsExactly("Allergy answer recorded", "Allergy requires clinical review");
    }

    @Test
    void nonMatchingRulesReturnPreliminaryPassWithoutEvidence() {
        ScreeningAnswer allergyAnswer = answer("ALLERGY", "no");
        EligibilityRule rule = rule(
                "REVIEW-ALLERGY",
                "ALLERGY == yes",
                PreliminaryScreeningResult.PRELIMINARY_REVIEW_REQUIRED,
                10,
                "Allergy requires clinical review"
        );
        when(ruleRepository.findByQuestionnaireVersionIdOrderByPriorityAsc(VERSION_ID))
                .thenReturn(List.of(rule));

        PreliminaryScreeningService.RuleEvaluationResult result =
                service.evaluateAnswers(VERSION_ID, List.of(allergyAnswer));

        assertThat(result.overallResult()).isEqualTo(PreliminaryScreeningResult.PRELIMINARY_PASS);
        assertThat(result.triggeredRuleCodes()).isEmpty();
        assertThat(result.guidanceMessages()).isEmpty();
    }

    @Test
    void supportedRuleOperatorsAreEvaluatedCaseInsensitively() {
        List<ScreeningAnswer> answers = List.of(
                answer("FEVER", "YES"),
                answer("AGE_GROUP", "adult"),
                answer("SYMPTOMS", "Mild Rash Present")
        );
        List<EligibilityRule> rules = List.of(
                rule("EQUALS", "fever == yes", PreliminaryScreeningResult.PRELIMINARY_PASS, 10,
                        "Equality matched"),
                rule("NOT-EQUALS", "age_group != CHILD", PreliminaryScreeningResult.PRELIMINARY_PASS, 20,
                        "Inequality matched"),
                rule("CONTAINS", "symptoms contains rash", PreliminaryScreeningResult.PRELIMINARY_PASS, 30,
                        "Contains matched")
        );
        when(ruleRepository.findByQuestionnaireVersionIdOrderByPriorityAsc(VERSION_ID)).thenReturn(rules);

        PreliminaryScreeningService.RuleEvaluationResult result =
                service.evaluateAnswers(VERSION_ID, answers);

        assertThat(result.overallResult()).isEqualTo(PreliminaryScreeningResult.PRELIMINARY_PASS);
        assertThat(result.triggeredRuleCodes()).containsExactly("EQUALS", "NOT-EQUALS", "CONTAINS");
        assertThat(result.guidanceMessages())
                .containsExactly("Equality matched", "Inequality matched", "Contains matched");
    }

    private ScreeningAnswer answer(String questionCode, String typedAnswer) {
        ScreeningQuestion question = new ScreeningQuestion(
                questionCode,
                "Question " + questionCode,
                QuestionType.TEXT,
                true,
                true
        );
        return new ScreeningAnswer(null, question, typedAnswer);
    }

    private EligibilityRule rule(
            String code,
            String condition,
            PreliminaryScreeningResult result,
            int priority,
            String message
    ) {
        return new EligibilityRule(code, null, code, condition, result, priority, message);
    }
}
