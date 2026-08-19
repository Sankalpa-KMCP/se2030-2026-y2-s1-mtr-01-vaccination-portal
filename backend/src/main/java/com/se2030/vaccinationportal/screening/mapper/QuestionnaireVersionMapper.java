package com.se2030.vaccinationportal.screening.mapper;

import com.se2030.vaccinationportal.screening.dto.response.EligibilityRuleResponse;
import com.se2030.vaccinationportal.screening.dto.response.QuestionnaireItemResponse;
import com.se2030.vaccinationportal.screening.dto.response.QuestionnaireVersionResponse;
import com.se2030.vaccinationportal.screening.model.EligibilityRule;
import com.se2030.vaccinationportal.screening.model.QuestionnaireItem;
import com.se2030.vaccinationportal.screening.model.QuestionnaireVersion;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QuestionnaireVersionMapper {

    public QuestionnaireVersionResponse toResponse(QuestionnaireVersion version) {
        return new QuestionnaireVersionResponse(
                version.getId(),
                version.getQuestionnaire().getId(),
                version.getVersionNumber(),
                version.getStatus(),
                version.getPublishedAt(),
                version.getRetiredAt(),
                version.getItems().stream().map(this::toResponse).toList(),
                version.getRules().stream().map(this::toResponse).toList(),
                version.getCreatedAt(),
                version.getUpdatedAt()
        );
    }

    public List<QuestionnaireVersionResponse> toResponses(List<QuestionnaireVersion> versions) {
        return versions.stream().map(this::toResponse).toList();
    }

    public QuestionnaireItemResponse toResponse(QuestionnaireItem item) {
        return new QuestionnaireItemResponse(
                item.getId(),
                item.getQuestionnaireVersion().getId(),
                item.getQuestion().getId(),
                item.getDisplayOrder(),
                item.getRequiredOverride(),
                item.getCreatedAt()
        );
    }

    public EligibilityRuleResponse toResponse(EligibilityRule rule) {
        return new EligibilityRuleResponse(
                rule.getId(),
                rule.getQuestionnaireVersion().getId(),
                rule.getRuleCode(),
                rule.getRuleName(),
                rule.getConditionExpression(),
                rule.getPreliminaryResult(),
                rule.getPriority(),
                rule.getMessage(),
                rule.getCreatedAt()
        );
    }

    public List<EligibilityRuleResponse> toRuleResponses(List<EligibilityRule> rules) {
        return rules.stream().map(this::toResponse).toList();
    }
}
