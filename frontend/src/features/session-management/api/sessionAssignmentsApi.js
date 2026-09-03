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
 * List all operational resource assignments for a vaccination session.
 *
 * @param {number|string} sessionId - Vaccination session identifier
 * @returns {Promise<Array>} List of SessionResourceAssignmentResponse objects
 */
export async function listResourceAssignments(sessionId) {
  const response = await apiRequest(`/v1/sessions/${sessionId}/resource-assignments`, {
    method: 'GET',
  });
  return handleResponse(response);
}

/**
 * Assign an operational physical resource to a DRAFT vaccination session.
 *
 * @param {number|string} sessionId - Vaccination session identifier
 * @param {Object} payload - CreateSessionResourceAssignmentRequest ({ resourceId, startAt, endAt })
 * @returns {Promise<Object>} Created SessionResourceAssignmentResponse object
 */
export async function createResourceAssignment(sessionId, payload) {
  const response = await apiRequest(`/v1/sessions/${sessionId}/resource-assignments`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return handleResponse(response);
}

/**
 * Delete an operational resource assignment from a DRAFT vaccination session.
 *
 * @param {number|string} sessionId - Vaccination session identifier
 * @param {number|string} assignmentId - Resource assignment identifier
 * @returns {Promise<null>} 204 No Content
 */
export async function deleteResourceAssignment(sessionId, assignmentId) {
  const response = await apiRequest(`/v1/sessions/${sessionId}/resource-assignments/${assignmentId}`, {
    method: 'DELETE',
  });
  return handleResponse(response);
}

/**
 * List all operational staff assignments for a vaccination session.
 *
 * @param {number|string} sessionId - Vaccination session identifier
 * @returns {Promise<Array>} List of StaffAssignmentResponse objects
 */
export async function listStaffAssignments(sessionId) {
  const response = await apiRequest(`/v1/sessions/${sessionId}/staff-assignments`, {
    method: 'GET',
  });
  return handleResponse(response);
}

/**
 * Assign a rostered staff member to a DRAFT vaccination session.
 *
 * @param {number|string} sessionId - Vaccination session identifier
 * @param {Object} payload - CreateStaffAssignmentRequest ({ staffRosterId, role, startAt, endAt })
 * @returns {Promise<Object>} Created StaffAssignmentResponse object
 */
export async function createStaffAssignment(sessionId, payload) {
  const response = await apiRequest(`/v1/sessions/${sessionId}/staff-assignments`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return handleResponse(response);
}

/**
 * Delete an operational staff assignment from a DRAFT vaccination session.
 *
 * @param {number|string} sessionId - Vaccination session identifier
 * @param {number|string} assignmentId - Staff assignment identifier
 * @returns {Promise<null>} 204 No Content
 */
export async function deleteStaffAssignment(sessionId, assignmentId) {
  const response = await apiRequest(`/v1/sessions/${sessionId}/staff-assignments/${assignmentId}`, {
    method: 'DELETE',
  });
  return handleResponse(response);
}
