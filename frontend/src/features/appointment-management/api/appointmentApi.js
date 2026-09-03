import { apiRequest } from '../../../services/apiClient';

/**
 * API client methods for appointment booking, listing, cancellation, and rescheduling.
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
 * Cancels an existing appointment.
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
 * Reschedules an existing appointment.
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
