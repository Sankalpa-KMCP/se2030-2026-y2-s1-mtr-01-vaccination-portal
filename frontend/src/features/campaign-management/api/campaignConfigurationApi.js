import { apiRequest } from '../../../services/apiClient';

/**
 * Helper to process Fetch API responses and unpack backend JSON or structured error payloads.
 */
async function handleResponse(response) {
  if (!response.ok) {
    let errorData = null;
    try {
      errorData = await response.json();
    } catch {
      // Body was not JSON
    }

    const message = errorData?.message || errorData?.error || `HTTP ${response.status}: ${response.statusText}`;
    const error = new Error(message);
    error.status = response.status;
    error.data = errorData;
    throw error;
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
}

/**
 * Fetch all dose programme version history records for a vaccination campaign.
 *
 * @param {number|string} campaignId - Campaign identifier
 * @returns {Promise<Array>} List of CampaignDoseProgrammeResponse objects
 */
export async function getDoseProgrammes(campaignId) {
  const response = await apiRequest(`/v1/campaigns/${campaignId}/dose-programmes`, {
    method: 'GET',
  });
  return handleResponse(response);
}

/**
 * Create initial dose programme version (v1) for a campaign.
 *
 * @param {number|string} campaignId - Campaign identifier
 * @param {Object} payload - CreateDoseProgrammeRequest payload
 * @returns {Promise<Object>} Created CampaignDoseProgrammeResponse
 */
export async function createDoseProgramme(campaignId, payload) {
  const response = await apiRequest(`/v1/campaigns/${campaignId}/dose-programmes`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return handleResponse(response);
}

/**
 * Create a new dose programme version (vN) superseding the active version.
 *
 * @param {number|string} campaignId - Campaign identifier
 * @param {Object} payload - CreateDoseProgrammeVersionRequest payload
 * @returns {Promise<Object>} Created CampaignDoseProgrammeResponse
 */
export async function createDoseProgrammeVersion(campaignId, payload) {
  const response = await apiRequest(`/v1/campaigns/${campaignId}/dose-programmes/versions`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return handleResponse(response);
}

/**
 * Fetch all questionnaire assignment history records for a campaign.
 *
 * @param {number|string} campaignId - Campaign identifier
 * @returns {Promise<Array>} List of CampaignQuestionnaireAssignmentResponse objects
 */
export async function getQuestionnaireAssignments(campaignId) {
  const response = await apiRequest(`/v1/campaigns/${campaignId}/questionnaire-assignments`, {
    method: 'GET',
  });
  return handleResponse(response);
}

/**
 * Assign a published questionnaire version to a campaign.
 *
 * @param {number|string} campaignId - Campaign identifier
 * @param {Object} payload - AssignQuestionnaireRequest payload ({ questionnaireVersionId, effectiveStartDate })
 * @returns {Promise<Object>} Created CampaignQuestionnaireAssignmentResponse
 */
export async function assignQuestionnaire(campaignId, payload) {
  const response = await apiRequest(`/v1/campaigns/${campaignId}/questionnaire-assignments`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return handleResponse(response);
}

/**
 * Fetch all root questionnaire definitions from MF-01 Screening Configuration catalog.
 *
 * @returns {Promise<Array>} List of QuestionnaireResponse objects
 */
export async function getScreeningQuestionnaires() {
  const response = await apiRequest('/v1/screening/questionnaires', {
    method: 'GET',
  });
  return handleResponse(response);
}

/**
 * Fetch all versions for a given questionnaire container from MF-01.
 *
 * @param {number|string} questionnaireId - Questionnaire identifier
 * @returns {Promise<Array>} List of QuestionnaireVersionResponse objects
 */
export async function getQuestionnaireVersions(questionnaireId) {
  const response = await apiRequest(`/v1/screening/questionnaires/${questionnaireId}/versions`, {
    method: 'GET',
  });
  return handleResponse(response);
}

/**
 * Fetch specific questionnaire version details from MF-01.
 *
 * @param {number|string} versionId - QuestionnaireVersion identifier
 * @returns {Promise<Object>} QuestionnaireVersionResponse object
 */
export async function getQuestionnaireVersion(versionId) {
  const response = await apiRequest(`/v1/screening/questionnaire-versions/${versionId}`, {
    method: 'GET',
  });
  return handleResponse(response);
}
