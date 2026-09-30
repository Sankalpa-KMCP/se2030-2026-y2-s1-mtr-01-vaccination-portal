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
      // Response body was not JSON
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
 * Fetches reconciled descriptive operational analytics.
 *
 * @param {Object} [filters={}] - Optional filters
 * @param {number|string|null} [filters.centerId] - Optional center identifier
 * @param {number|string|null} [filters.campaignId] - Optional campaign identifier
 * @returns {Promise<Object>} OperationalAnalyticsResponse object
 */
export async function getOperationalAnalytics(filters = {}) {
  const params = new URLSearchParams();
  if (filters.centerId) {
    params.set('centerId', filters.centerId);
  }
  if (filters.campaignId) {
    params.set('campaignId', filters.campaignId);
  }

  const queryString = params.toString() ? `?${params.toString()}` : '';
  const response = await apiRequest(`/v1/analytics/operations${queryString}`, { method: 'GET' });
  return handleResponse(response);
}
