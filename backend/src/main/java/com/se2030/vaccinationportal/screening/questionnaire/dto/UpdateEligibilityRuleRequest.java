package com.se2030.vaccinationportal.screening.questionnaire.dto;

import com.se2030.vaccinationportal.screening.submission.model.PreliminaryScreeningResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateEligibilityRuleRequest(
        @NotBlank(message = "Rule code is required")
        @Size(max = 64, message = "Rule code must not exceed 64 characters")
        String ruleCode,

        @NotBlank(message = "Rule name is required")
        @Size(max = 255, message = "Rule name must not exceed 255 characters")
        String ruleName,

        @NotBlank(message = "Condition expression is required")
        String conditionExpression,

        @NotNull(message = "Preliminary result is required")
        PreliminaryScreeningResult preliminaryResult,

        int priority,

        @NotBlank(message = "Message is required")
        @Size(max = 500, message = "Message must not exceed 500 characters")
        String message
) {}
