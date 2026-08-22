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
