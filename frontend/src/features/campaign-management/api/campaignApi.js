import { apiRequest } from '../../../shared/api/apiClient';

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
 * Fetch list of campaigns, optionally filtered by status.
 *
 * @param {string|null} [status] - Optional CampaignStatus filter
 * @returns {Promise<Array>} List of CampaignSummaryResponse objects
 */
export async function listCampaigns(status = null) {
  const query = status ? `?status=${encodeURIComponent(status)}` : '';
  const response = await apiRequest(`/v1/campaigns${query}`, { method: 'GET' });
  return handleResponse(response);
}

/**
 * Fetch list of visible campaigns for authenticated citizens (MF02-R-001).
 *
 * @returns {Promise<Array>} List of CitizenCampaignDiscoveryResponse objects
 */
export async function listCitizenCampaigns() {
  const response = await apiRequest('/v1/citizen/campaigns', { method: 'GET' });
  return handleResponse(response);
}

/**
 * Fetch detailed campaign record by identifier.
 *
 * @param {number|string} id - Campaign identifier
 * @returns {Promise<Object>} CampaignResponse object
 */
export async function getCampaignById(id) {
  const response = await apiRequest(`/v1/campaigns/${id}`, { method: 'GET' });
  return handleResponse(response);
}

/**
 * Create a new campaign.
 *
 * @param {Object} payload - CreateCampaignRequest payload
 * @returns {Promise<Object>} Created CampaignResponse object
 */
export async function createCampaign(payload) {
  const response = await apiRequest('/v1/campaigns', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return handleResponse(response);
}

/**
 * Update an existing campaign.
 *
 * @param {number|string} id - Campaign identifier
 * @param {Object} payload - UpdateCampaignRequest payload
 * @returns {Promise<Object>} Updated CampaignResponse object
 */
export async function updateCampaign(id, payload) {
  const response = await apiRequest(`/v1/campaigns/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
  return handleResponse(response);
}

/**
 * Fetch lifecycle audit event history for a campaign.
 *
 * @param {number|string} id - Campaign identifier
 * @returns {Promise<Array>} List of CampaignLifecycleEventResponse objects
 */
export async function getCampaignLifecycleEvents(id) {
  const response = await apiRequest(`/v1/campaigns/${id}/lifecycle-events`, { method: 'GET' });
  return handleResponse(response);
}

/**
 * Transition campaign from DRAFT to SCHEDULED.
 *
 * @param {number|string} id - Campaign identifier
 * @returns {Promise<Object>} Updated CampaignResponse
 */
export async function scheduleCampaign(id) {
  const response = await apiRequest(`/v1/campaigns/${id}/schedule`, { method: 'POST' });
  return handleResponse(response);
}

/**
 * Transition campaign from SCHEDULED to PUBLISHED.
 *
 * @param {number|string} id - Campaign identifier
 * @returns {Promise<Object>} Updated CampaignResponse
 */
export async function publishCampaign(id) {
  const response = await apiRequest(`/v1/campaigns/${id}/publish`, { method: 'POST' });
  return handleResponse(response);
}

/**
 * Transition campaign from PUBLISHED to ACTIVE.
 *
 * @param {number|string} id - Campaign identifier
 * @returns {Promise<Object>} Updated CampaignResponse
 */
export async function activateCampaign(id) {
  const response = await apiRequest(`/v1/campaigns/${id}/activate`, { method: 'POST' });
  return handleResponse(response);
}

/**
 * Transition campaign from ACTIVE to SUSPENDED.
 *
 * @param {number|string} id - Campaign identifier
 * @param {Object} payload - { reason: string, actorId: number }
 * @returns {Promise<Object>} Updated CampaignResponse
 */
export async function suspendCampaign(id, payload) {
  const response = await apiRequest(`/v1/campaigns/${id}/suspend`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return handleResponse(response);
}

/**
 * Transition campaign from SUSPENDED to ACTIVE.
 *
 * @param {number|string} id - Campaign identifier
 * @param {Object} payload - { reason: string, actorId: number }
 * @returns {Promise<Object>} Updated CampaignResponse
 */
export async function resumeCampaign(id, payload) {
  const response = await apiRequest(`/v1/campaigns/${id}/resume`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return handleResponse(response);
}

/**
 * Transition campaign to COMPLETED upon conclusion of service work.
 *
 * @param {number|string} id - Campaign identifier
 * @param {Object} payload - { reason: string }
 * @returns {Promise<Object>} Updated CampaignResponse
 */
export async function completeCampaign(id, payload) {
  const response = await apiRequest(`/v1/campaigns/${id}/complete`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return handleResponse(response);
}

/**
 * Transition campaign to CANCELLED.
 *
 * @param {number|string} id - Campaign identifier
 * @param {Object} payload - { reason: string }
 * @returns {Promise<Object>} Updated CampaignResponse
 */
export async function cancelCampaign(id, payload) {
  const response = await apiRequest(`/v1/campaigns/${id}/cancel`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return handleResponse(response);
}

/**
 * Transition completed/cancelled campaign to ARCHIVED for immutable historical retention.
 *
 * @param {number|string} id - Campaign identifier
 * @param {Object} payload - { reason: string }
 * @returns {Promise<Object>} Updated CampaignResponse
 */
export async function archiveCampaign(id, payload) {
  const response = await apiRequest(`/v1/campaigns/${id}/archive`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return handleResponse(response);
}

/**
 * Fetch list of active vaccine types for campaign configuration.
 *
 * @returns {Promise<Array>} List of VaccineTypeSummaryResponse objects
 */
export async function listVaccineTypes() {
  const response = await apiRequest('/v1/vaccine-types', { method: 'GET' });
  return handleResponse(response);
}

/**
 * Fetch all vaccine types including inactive records for master data management.
 *
 * @returns {Promise<Array>} List of VaccineTypeSummaryResponse objects
 */
export async function listAllVaccineTypes() {
  const response = await apiRequest('/v1/vaccine-types?includeInactive=true', { method: 'GET' });
  return handleResponse(response);
}

/**
 * Create a new vaccine type.
 *
 * @param {Object} payload { code, name, doseUnit }
 * @returns {Promise<Object>} Created VaccineTypeSummaryResponse
 */
export async function createVaccineType(payload) {
  const response = await apiRequest('/v1/vaccine-types', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return handleResponse(response);
}

/**
 * Update an existing vaccine type.
 *
 * @param {number|string} id Vaccine type ID
 * @param {Object} payload { name, doseUnit }
 * @returns {Promise<Object>} Updated VaccineTypeSummaryResponse
 */
export async function updateVaccineType(id, payload) {
  const response = await apiRequest(`/v1/vaccine-types/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
  return handleResponse(response);
}

/**
 * Activate a vaccine type.
 *
 * @param {number|string} id Vaccine type ID
 * @returns {Promise<Object>} Updated VaccineTypeSummaryResponse
 */
export async function activateVaccineType(id) {
  const response = await apiRequest(`/v1/vaccine-types/${id}/activate`, {
    method: 'POST',
  });
  return handleResponse(response);
}

/**
 * Deactivate a vaccine type.
 *
 * @param {number|string} id Vaccine type ID
 * @returns {Promise<Object>} Updated VaccineTypeSummaryResponse
 */
export async function deactivateVaccineType(id) {
  const response = await apiRequest(`/v1/vaccine-types/${id}/deactivate`, {
    method: 'POST',
  });
  return handleResponse(response);
}

/**
 * Fetch list of active target demographic groups for campaign configuration.
 *
 * @returns {Promise<Array>} List of TargetGroupSummaryResponse objects
 */
export async function listTargetGroups() {
  const response = await apiRequest('/v1/target-groups', { method: 'GET' });
  return handleResponse(response);
}

/**
 * Fetch all target demographic groups including inactive records for master data management.
 *
 * @returns {Promise<Array>} List of TargetGroupSummaryResponse objects
 */
export async function listAllTargetGroups() {
  const response = await apiRequest('/v1/target-groups?includeInactive=true', { method: 'GET' });
  return handleResponse(response);
}

/**
 * Create a new target demographic group.
 *
 * @param {Object} payload { code, description }
 * @returns {Promise<Object>} Created TargetGroupSummaryResponse
 */
export async function createTargetGroup(payload) {
  const response = await apiRequest('/v1/target-groups', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return handleResponse(response);
}

/**
 * Update an existing target demographic group.
 *
 * @param {number|string} id Target group ID
 * @param {Object} payload { description }
 * @returns {Promise<Object>} Updated TargetGroupSummaryResponse
 */
export async function updateTargetGroup(id, payload) {
  const response = await apiRequest(`/v1/target-groups/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
  return handleResponse(response);
}

/**
 * Activate a target demographic group.
 *
 * @param {number|string} id Target group ID
 * @returns {Promise<Object>} Updated TargetGroupSummaryResponse
 */
export async function activateTargetGroup(id) {
  const response = await apiRequest(`/v1/target-groups/${id}/activate`, {
    method: 'POST',
  });
  return handleResponse(response);
}

/**
 * Deactivate a target demographic group.
 *
 * @param {number|string} id Target group ID
 * @returns {Promise<Object>} Updated TargetGroupSummaryResponse
 */
export async function deactivateTargetGroup(id) {
  const response = await apiRequest(`/v1/target-groups/${id}/deactivate`, {
    method: 'POST',
  });
  return handleResponse(response);
}

export async function deleteVaccineType(id) {
  const response = await apiRequest(`/v1/vaccine-types/${id}`, { method: 'DELETE' });
  return handleResponse(response);
}

export async function deleteTargetGroup(id) {
  const response = await apiRequest(`/v1/target-groups/${id}`, { method: 'DELETE' });
  return handleResponse(response);
}
