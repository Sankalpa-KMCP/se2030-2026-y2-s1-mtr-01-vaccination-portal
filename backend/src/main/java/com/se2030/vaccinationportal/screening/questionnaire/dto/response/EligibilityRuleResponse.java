package com.se2030.vaccinationportal.screening.questionnaire.dto.response;

import com.se2030.vaccinationportal.screening.submission.model.PreliminaryScreeningResult;

import java.time.Instant;

public record EligibilityRuleResponse(
        Long id,
        Long questionnaireVersionId,
        String ruleCode,
        String ruleName,
        String conditionExpression,
        PreliminaryScreeningResult preliminaryResult,
        int priority,
        String message,
        Instant createdAt
) {}
