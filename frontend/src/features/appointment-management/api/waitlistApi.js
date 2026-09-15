import { apiRequest } from '../../../services/apiClient';

/**
 * API client methods for waitlist management (US-05).
 * Interfaces with backend /api/v1/waitlist endpoints.
 */

/**
 * Joins the waitlist for a full vaccination session.
 *
 * @param {Object} payload
 * @param {number|string} payload.citizenId
 * @param {number|string} payload.campaignId
 * @param {number|string} payload.sessionId
 * @returns {Promise<Object>} WaitlistEntryResponse object
 */
export async function joinWaitlist({ citizenId, campaignId, sessionId }) {
  const response = await apiRequest('/v1/waitlist/join', {
    method: 'POST',
    body: JSON.stringify({
      citizenId: Number(citizenId),
      campaignId: Number(campaignId),
      sessionId: Number(sessionId),
    }),
  });

  const data = await response.json();

  if (!response.ok) {
    const error = new Error(data.message || 'Failed to join waitlist');
    error.status = response.status;
    error.error = data.error || 'WAITLIST_JOIN_FAILED';
    throw error;
  }

  return data;
}

/**
 * Leaves an active waitlist entry for a citizen.
 *
 * @param {number|string} waitlistEntryId
 * @param {number|string|{ citizenId: number|string }} citizenIdOrOptions
 * @returns {Promise<Object>} WaitlistEntryResponse object
 */
export async function leaveWaitlist(waitlistEntryId, citizenIdOrOptions) {
  const citizenId = typeof citizenIdOrOptions === 'object'
    ? citizenIdOrOptions?.citizenId
    : citizenIdOrOptions;

  const response = await apiRequest(
    `/v1/waitlist/${waitlistEntryId}/leave?citizenId=${encodeURIComponent(citizenId)}`,
    {
      method: 'POST',
    }
  );

  const data = await response.json();

  if (!response.ok) {
    const error = new Error(data.message || 'Failed to leave waitlist');
    error.status = response.status;
    error.error = data.error || 'WAITLIST_LEAVE_FAILED';
    throw error;
  }

  return data;
}

/**
 * Retrieves the current waitlist status for a citizen in a given campaign.
 *
 * @param {Object} params
 * @param {number|string} params.citizenId
 * @param {number|string} params.campaignId
 * @returns {Promise<Object|null>} WaitlistEntryResponse object, or null if no entry exists (404)
 */
export async function getWaitlistStatus({ citizenId, campaignId }) {
  const response = await apiRequest(
    `/v1/waitlist/status?citizenId=${encodeURIComponent(citizenId)}&campaignId=${encodeURIComponent(campaignId)}`
  );

  if (response.status === 404) {
    return null;
  }

  const data = await response.json();

  if (!response.ok) {
    const error = new Error(data.message || 'Failed to fetch waitlist status');
    error.status = response.status;
    error.error = data.error || 'WAITLIST_STATUS_FAILED';
    throw error;
  }

  return data;
}
