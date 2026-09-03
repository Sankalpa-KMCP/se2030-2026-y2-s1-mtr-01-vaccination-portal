import { apiRequest } from '../../../services/apiClient';

/**
 * API client methods for staff check-in, queue inspection, reversal, and no-show processing (US-04).
 * Interfaces with backend /api/v1/appointments endpoints protected by staff roles
 * (ADMIN, HEALTHCARE_OFFICER, CENTER_MANAGER).
 */

/**
 * Fetches the active vaccinator queue for a given time slot.
 *
 * @param {number|string} slotId - Time slot identifier
 * @returns {Promise<Array>} List of AppointmentResponse objects in queue order
 */
export async function getVaccinatorQueue(slotId) {
  const response = await apiRequest(`/v1/appointments/queue?slotId=${encodeURIComponent(slotId)}`);
  const data = await response.json();

  if (!response.ok) {
    const error = new Error(data.message || 'Failed to fetch vaccinator queue');
    error.status = response.status;
    error.error = data.error || 'QUEUE_FETCH_FAILED';
    throw error;
  }

  return data;
}

/**
 * Checks in a citizen for their appointment upon arrival at the vaccination center.
 *
 * @param {number|string} appointmentId - Appointment identifier
 * @param {boolean} [isStaffOverride=false] - Whether to bypass check-in window restrictions
 * @param {string|null} [staffReason=null] - Mandatory audit reason when staff override is applied
 * @returns {Promise<Object>} Updated AppointmentResponse object (status: CHECKED_IN)
 */
export async function checkIn(appointmentId, isStaffOverride = false, staffReason = null) {
  const payload = {
    isStaffOverride: Boolean(isStaffOverride),
    staffReason: staffReason || null,
  };

  const response = await apiRequest(`/v1/appointments/${appointmentId}/check-in`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });

  const data = await response.json();

  if (!response.ok) {
    const error = new Error(data.message || 'Check-in failed');
    error.status = response.status;
    error.error = data.error || 'CHECK_IN_FAILED';
    throw error;
  }

  return data;
}

/**
 * Reverses an erroneous check-in, returning the appointment to CONFIRMED status.
 *
 * @param {number|string} appointmentId - Appointment identifier
 * @returns {Promise<Object>} Updated AppointmentResponse object (status: CONFIRMED)
 */
export async function reverseCheckIn(appointmentId) {
  const response = await apiRequest(`/v1/appointments/${appointmentId}/reverse-check-in`, {
    method: 'POST',
  });

  const data = await response.json();

  if (!response.ok) {
    const error = new Error(data.message || 'Reverse check-in failed');
    error.status = response.status;
    error.error = data.error || 'REVERSE_CHECK_IN_FAILED';
    throw error;
  }

  return data;
}

/**
 * Marks an unattended appointment as NO_SHOW once the check-in window has elapsed.
 *
 * @param {number|string} appointmentId - Appointment identifier
 * @returns {Promise<Object>} Updated AppointmentResponse object (status: NO_SHOW)
 */
export async function markNoShow(appointmentId) {
  const response = await apiRequest(`/v1/appointments/${appointmentId}/no-show`, {
    method: 'POST',
  });

  const data = await response.json();

  if (!response.ok) {
    const error = new Error(data.message || 'Marking no-show failed');
    error.status = response.status;
    error.error = data.error || 'MARK_NO_SHOW_FAILED';
    throw error;
  }

  return data;
}
