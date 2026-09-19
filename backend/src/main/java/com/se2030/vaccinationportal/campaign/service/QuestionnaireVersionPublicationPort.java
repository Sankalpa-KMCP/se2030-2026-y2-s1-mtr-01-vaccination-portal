package com.se2030.vaccinationportal.campaign.service;

/**
 * Cross-module integration port used by MF-02 to verify questionnaire version publication status.
 *
 * <p>Canonical Architecture Boundaries (WBVP-SE2030 Section 2, 5, 6):</p>
 * <ul>
 *   <li>MF-01 owns {@code Questionnaire}, {@code QuestionnaireVersion}, questions, answer options, and eligibility rules.</li>
 *   <li>MF-02 owns {@code VaccinationCampaign} and {@code CampaignQuestionnaireAssignment}.</li>
 *   <li>MF-02 must never create its own duplicate {@code QuestionnaireVersion} entity or tables.</li>
 *   <li>The scalar {@code questionnaireVersionId} is a temporary pre-integration boundary because MF-01 is not yet implemented in code.</li>
 *   <li>MF-01 must later provide the production adapter implementation backed by its real {@code QuestionnaireVersion} repository/service.</li>
 *   <li>No production adapter may simply return {@code true} without verifying actual {@code PUBLISHED} status in MF-01.</li>
 *   <li>MF-01 must NOT create a second {@code CampaignQuestionnaireAssignment} table.</li>
 * </ul>
 *
 * <p>This port interface is permanent; only temporary test doubles are test-scoped.</p>
 */
public interface QuestionnaireVersionPublicationPort {

    /**
     * Checks whether the specified questionnaire version exists in MF-01 and is in {@code PUBLISHED} status.
     *
     * @param questionnaireVersionId Unique identifier of the QuestionnaireVersion in MF-01
     * @return {@code true} if the questionnaire version exists and is in {@code PUBLISHED} status; {@code false} otherwise
     */
    boolean isPublished(Long questionnaireVersionId);

    /**
     * Checks whether the specified campaign already has citizen screening submissions recorded in MF-01.
     *
     * @param campaignId Unique identifier of the VaccinationCampaign
     * @return {@code true} if citizen screening submissions exist for this campaign; {@code false} otherwise
     */
    boolean hasCampaignSubmissions(Long campaignId);
}
