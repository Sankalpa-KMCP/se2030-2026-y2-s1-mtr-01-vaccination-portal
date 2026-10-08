package com.se2030.vaccinationportal.screening.questionnaire.dto;

import com.se2030.vaccinationportal.screening.submission.model.PreliminaryScreeningResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddEligibilityRuleRequest(
        @NotBlank(message = "Rule code is required")
        @Size(max = 64, message = "Rule code must not exceed 64 characters")
        String ruleCode,

        String ruleName,

        @NotBlank(message = "Condition expression is required")
        String conditionExpression,

        PreliminaryScreeningResult preliminaryResult,
        int priority,

        @Size(max = 500, message = "Message must not exceed 500 characters")
        String message
) {}
