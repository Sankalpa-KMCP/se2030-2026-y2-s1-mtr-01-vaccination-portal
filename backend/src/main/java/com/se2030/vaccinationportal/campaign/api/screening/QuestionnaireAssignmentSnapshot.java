package com.se2030.vaccinationportal.campaign.api.screening;

/** Immutable identity of the effective campaign questionnaire assignment. */
public record QuestionnaireAssignmentSnapshot(Long id, Long campaignId, Long questionnaireVersionId) {
}
