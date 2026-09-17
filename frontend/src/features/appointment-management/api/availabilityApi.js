import { apiRequest } from '../../../services/apiClient';

/**
 * API client methods for appointment session availability search.
 *
 * <p><b>User Story:</b>
 * <ul>
 *   <li>US-03: Search available vaccination appointment slots at nearby centers.</li>
 * </ul>
 *
 * <p><b>Business Rules Handled:</b>
 * <ul>
 *   <li>MF03-BR-01: Eligibility is not checked during discovery to allow browsing.</li>
 *   <li>MF03-BR-02: Backend excludes already-booked slots for the same campaign.</li>
 * </ul>
 */

/**
 * Searches for bookable vaccination sessions.
 *
 * @param {Object} params
 * @param {number|string} params.citizenId
 * @param {number|string} params.campaignId
 * @param {number|string} [params.centerId]
 * @param {string} params.fromDate - YYYY-MM-DD
 * @param {string} params.toDate - YYYY-MM-DD
 * @returns {Promise<Array>} List of AvailableSessionResponse objects
 */
export async function searchAvailableAppointments({ citizenId, campaignId, centerId, fromDate, toDate }) {
  const queryParams = new URLSearchParams();
  if (citizenId != null) queryParams.append('citizenId', citizenId);
  if (campaignId != null) queryParams.append('campaignId', campaignId);
  if (centerId != null && centerId !== '') queryParams.append('centerId', centerId);
  if (fromDate) queryParams.append('fromDate', fromDate);
  if (toDate) queryParams.append('toDate', toDate);

  const response = await apiRequest(`/v1/appointments/search?${queryParams.toString()}`);
  const data = await response.json();

  if (!response.ok) {
    const error = new Error(data.message || 'Failed to search available appointments');
    error.status = response.status;
    error.error = data.error || 'BAD_REQUEST';
    throw error;
  }

  return data;
}
