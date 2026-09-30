import { apiRequest } from '../../../shared/api/apiClient';

/**
 * API client methods for appointment booking, listing, cancellation, and rescheduling.
 *
 * <p><b>User Story:</b>
 * <ul>
 *   <li>US-03: Citizen appointment booking, cancellation, and rescheduling.</li>
 * </ul>
 *
 * <p><b>Business Rules Handled:</b>
 * <ul>
 *   <li>MF03-BR-01: Screening eligibility required for booking.</li>
 *   <li>MF03-BR-02: Single active appointment per citizen per campaign.</li>
 *   <li>MF03-BR-03: Atomic slot capacity allocation.</li>
 *   <li>MF03-BR-04: 24-hour modification window; late modifications require staff override and reason.</li>
 *   <li>MF03-BR-05: Atomic release and reserve during rescheduling.</li>
 * </ul>
 */

/**
 * Fetches all appointments belonging to a specific citizen across all statuses (US-03).
 *
 * @param {number|string} citizenId
 * @returns {Promise<Array>} List of AppointmentResponse objects
 */
export async function getMyAppointments(citizenId) {
  const response = await apiRequest(`/v1/appointments/mine?citizenId=${encodeURIComponent(citizenId)}`);
  const data = await response.json();

  if (!response.ok) {
    const error = new Error(data.message || 'Failed to fetch citizen appointments');
    error.status = response.status;
    error.error = data.error || 'FETCH_FAILED';
    throw error;
  }

  return data;
}

/**
 * Books an appointment for an eligible citizen.
 *
 * @param {Object} payload
 * @param {number|string} payload.citizenId
 * @param {number|string} payload.campaignId
 * @param {number|string} payload.slotId
 * @returns {Promise<Object>} AppointmentResponse object
 */
export async function bookAppointment({ citizenId, campaignId, slotId }) {
  const response = await apiRequest('/v1/appointments/book', {
    method: 'POST',
    body: JSON.stringify({
      citizenId: Number(citizenId),
      campaignId: Number(campaignId),
      slotId: Number(slotId),
    }),
  });

  const data = await response.json();

  if (!response.ok) {
    const error = new Error(data.message || 'Failed to book appointment');
    error.status = response.status;
    error.error = data.error || 'BOOKING_FAILED';
    throw error;
  }

  return data;
}

/**
 * Cancels an existing appointment (US-03, MF03-BR-04, MF03-BR-05).
 * Sends cancellation request with optional staff override flags if within 24 hours of session start.
 *
 * @param {number|string} appointmentId
 * @param {Object} [options]
 * @param {number|string} [options.requestingCitizenId]
 * @param {boolean} [options.isStaffOverride=false]
 * @param {string|null} [options.staffReason=null]
 * @returns {Promise<Object>} Updated AppointmentResponse object (status: CANCELLED)
 */
export async function cancelAppointment(appointmentId, { requestingCitizenId, isStaffOverride = false, staffReason = null } = {}) {
  const response = await apiRequest(`/v1/appointments/${appointmentId}/cancel`, {
    method: 'POST',
    body: JSON.stringify({
      requestingCitizenId: requestingCitizenId ? Number(requestingCitizenId) : null,
      isStaffOverride: Boolean(isStaffOverride),
      staffReason,
    }),
  });

  const data = await response.json();

  if (!response.ok) {
    const error = new Error(data.message || 'Failed to cancel appointment');
    error.status = response.status;
    error.error = data.error || 'CANCELLATION_FAILED';
    throw error;
  }

  return data;
}

/**
 * Reschedules an existing appointment to a new time slot (US-03, MF03-BR-04, MF03-BR-05).
 * Communicates the target slot ID and optional staff override parameters to the atomic backend endpoint.
 *
 * @param {number|string} appointmentId
 * @param {Object} options
 * @param {number|string} options.requestingCitizenId
 * @param {number|string} options.newSlotId
 * @param {boolean} [options.isStaffOverride=false]
 * @param {string|null} [options.staffReason=null]
 * @returns {Promise<Object>} Updated AppointmentResponse object with new slotId
 */
export async function rescheduleAppointment(appointmentId, { requestingCitizenId, newSlotId, isStaffOverride = false, staffReason = null }) {
  const response = await apiRequest(`/v1/appointments/${appointmentId}/reschedule`, {
    method: 'POST',
    body: JSON.stringify({
      requestingCitizenId: Number(requestingCitizenId),
      newSlotId: Number(newSlotId),
      isStaffOverride: Boolean(isStaffOverride),
      staffReason,
    }),
  });

  const data = await response.json();

  if (!response.ok) {
    const error = new Error(data.message || 'Failed to reschedule appointment');
    error.status = response.status;
    error.error = data.error || 'RESCHEDULE_FAILED';
    throw error;
  }

  return data;
}

export {
  getVaccinatorQueue,
  checkIn,
  reverseCheckIn,
  markNoShow,
} from './checkInApi';
