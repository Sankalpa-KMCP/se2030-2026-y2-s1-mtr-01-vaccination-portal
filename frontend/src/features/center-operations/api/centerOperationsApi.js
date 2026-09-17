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

// =================================================================================================
// Vaccination Center Core API Operations
// =================================================================================================

/**
 * Fetch list of vaccination centers, optionally filtered by status.
 *
 * @param {string|null} [status] - Optional CenterStatus filter (ACTIVE, INACTIVE)
 * @returns {Promise<Array>} List of CenterSummaryResponse objects
 */
export async function listCenters(status = null) {
  const query = status ? `?status=${encodeURIComponent(status)}` : '';
  const response = await apiRequest(`/v1/centers${query}`, { method: 'GET' });
  return handleResponse(response);
}

/**
 * Fetch active vaccination centers for authenticated citizen discovery.
 *
 * @returns {Promise<Array>} List of CitizenCenterDiscoveryResponse objects ({ id, code, name, address, contact, timezone })
 */
export async function listCitizenCenters() {
  const response = await apiRequest('/v1/citizen/centers', { method: 'GET' });
  return handleResponse(response);
}

/**
 * Fetch detailed vaccination center record by identifier.
 *
 * @param {number|string} id - Center identifier
 * @returns {Promise<Object>} CenterResponse object
 */
export async function getCenterById(id) {
  const response = await apiRequest(`/v1/centers/${id}`, { method: 'GET' });
  return handleResponse(response);
}

/**
 * Create a new physical vaccination center record.
 *
 * @param {Object} payload - CreateCenterRequest payload ({ code, name, address, contact, safeConcurrentOccupancy, sessionServiceCeiling, timezone })
 * @returns {Promise<Object>} Created CenterResponse object
 */
export async function createCenter(payload) {
  const response = await apiRequest('/v1/centers', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return handleResponse(response);
}

/**
 * Update mutable details of an existing vaccination center.
 *
 * @param {number|string} id - Center identifier
 * @param {Object} payload - UpdateCenterRequest payload ({ name, address, contact, safeConcurrentOccupancy, sessionServiceCeiling })
 * @returns {Promise<Object>} Updated CenterResponse object
 */
export async function updateCenter(id, payload) {
  const response = await apiRequest(`/v1/centers/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
  return handleResponse(response);
}

/**
 * Activate an inactive vaccination center.
 *
 * @param {number|string} id - Center identifier
 * @returns {Promise<Object>} Updated CenterResponse object
 */
export async function activateCenter(id) {
  const response = await apiRequest(`/v1/centers/${id}/activate`, {
    method: 'POST',
  });
  return handleResponse(response);
}

/**
 * Deactivate an active vaccination center.
 *
 * @param {number|string} id - Center identifier
 * @returns {Promise<Object>} Updated CenterResponse object
 */
export async function deactivateCenter(id) {
  const response = await apiRequest(`/v1/centers/${id}/deactivate`, {
    method: 'POST',
  });
  return handleResponse(response);
}

// =================================================================================================
// Center Operating Hours API Operations
// =================================================================================================

/**
 * Fetch all operating hours schedule shifts for a center.
 *
 * @param {number|string} centerId - Vaccination center identifier
 * @returns {Promise<Array>} List of OperatingHoursResponse objects
 */
export async function getOperatingHours(centerId) {
  const response = await apiRequest(`/v1/centers/${centerId}/operating-hours`, {
    method: 'GET',
  });
  return handleResponse(response);
}

/**
 * Create a new operating hours schedule shift for a center.
 *
 * @param {number|string} centerId - Vaccination center identifier
 * @param {Object} payload - CreateOperatingHoursRequest ({ dayOfWeek, opening, closing, effectiveFrom, effectiveTo })
 * @returns {Promise<Object>} Created OperatingHoursResponse object
 */
export async function createOperatingHours(centerId, payload) {
  const response = await apiRequest(`/v1/centers/${centerId}/operating-hours`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return handleResponse(response);
}

/**
 * Update an existing operating hours schedule shift.
 *
 * @param {number|string} centerId - Vaccination center identifier
 * @param {number|string} hoursId - Operating hours record identifier
 * @param {Object} payload - UpdateOperatingHoursRequest ({ dayOfWeek, opening, closing, effectiveFrom, effectiveTo })
 * @returns {Promise<Object>} Updated OperatingHoursResponse object
 */
export async function updateOperatingHours(centerId, hoursId, payload) {
  const response = await apiRequest(`/v1/centers/${centerId}/operating-hours/${hoursId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
  return handleResponse(response);
}

/**
 * End or shorten the effective validity of an operating hours schedule shift.
 *
 * @param {number|string} centerId - Vaccination center identifier
 * @param {number|string} hoursId - Operating hours record identifier
 * @param {Object} payload - EndDateOperatingHoursRequest ({ endDate })
 * @returns {Promise<Object>} Updated OperatingHoursResponse object
 */
export async function endDateOperatingHours(centerId, hoursId, payload) {
  const response = await apiRequest(`/v1/centers/${centerId}/operating-hours/${hoursId}/end-date`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return handleResponse(response);
}

// =================================================================================================
// Center Temporary Closures API Operations
// =================================================================================================

/**
 * Fetch all temporary closure records for a center.
 *
 * @param {number|string} centerId - Vaccination center identifier
 * @returns {Promise<Array>} List of CenterClosureResponse objects
 */
export async function getClosures(centerId) {
  const response = await apiRequest(`/v1/centers/${centerId}/closures`, {
    method: 'GET',
  });
  return handleResponse(response);
}

/**
 * Register a new temporary closure interval for a center.
 *
 * @param {number|string} centerId - Vaccination center identifier
 * @param {Object} payload - CreateCenterClosureRequest ({ startAt, endAt, reason })
 * @returns {Promise<Object>} Created CenterClosureResponse object
 */
export async function createClosure(centerId, payload) {
  const response = await apiRequest(`/v1/centers/${centerId}/closures`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return handleResponse(response);
}

/**
 * Update an active future temporary closure interval.
 *
 * @param {number|string} centerId - Vaccination center identifier
 * @param {number|string} closureId - Center closure identifier
 * @param {Object} payload - UpdateCenterClosureRequest ({ startAt, endAt, reason })
 * @returns {Promise<Object>} Updated CenterClosureResponse object
 */
export async function updateClosure(centerId, closureId, payload) {
  const response = await apiRequest(`/v1/centers/${centerId}/closures/${closureId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
  return handleResponse(response);
}

/**
 * Cancel a future temporary closure.
 *
 * @param {number|string} centerId - Vaccination center identifier
 * @param {number|string} closureId - Center closure identifier
 * @returns {Promise<Object>} Cancelled CenterClosureResponse object
 */
export async function cancelClosure(centerId, closureId) {
  const response = await apiRequest(`/v1/centers/${centerId}/closures/${closureId}/cancel`, {
    method: 'POST',
  });
  return handleResponse(response);
}

// =================================================================================================
// Operational Physical Resources API Operations
// =================================================================================================

/**
 * Fetch operational physical resources for a center, optionally filtered by resource type.
 *
 * @param {number|string} centerId - Vaccination center identifier
 * @param {string|null} [resourceType] - Optional ResourceType filter (ROOM, VACCINATION_STATION, EQUIPMENT)
 * @returns {Promise<Array>} List of OperationalResourceResponse objects
 */
export async function listOperationalResources(centerId, resourceType = null) {
  const query = resourceType ? `?resourceType=${encodeURIComponent(resourceType)}` : '';
  const response = await apiRequest(`/v1/centers/${centerId}/resources${query}`, { method: 'GET' });
  return handleResponse(response);
}

/**
 * Fetch a single operational resource by center and resource identifier.
 *
 * @param {number|string} centerId - Vaccination center identifier
 * @param {number|string} resourceId - Resource identifier
 * @returns {Promise<Object>} OperationalResourceResponse object
 */
export async function getOperationalResource(centerId, resourceId) {
  const response = await apiRequest(`/v1/centers/${centerId}/resources/${resourceId}`, { method: 'GET' });
  return handleResponse(response);
}

/**
 * Create a new physical operational resource under a vaccination center.
 *
 * @param {number|string} centerId - Vaccination center identifier
 * @param {Object} payload - CreateOperationalResourceRequest payload ({ code, resourceType, name, status })
 * @returns {Promise<Object>} Created OperationalResourceResponse object
 */
export async function createOperationalResource(centerId, payload) {
  const response = await apiRequest(`/v1/centers/${centerId}/resources`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return handleResponse(response);
}

/**
 * Update the mutable attributes (name, status) of an operational resource.
 *
 * @param {number|string} centerId - Vaccination center identifier
 * @param {number|string} resourceId - Resource identifier
 * @param {Object} payload - UpdateOperationalResourceRequest payload ({ name, status })
 * @returns {Promise<Object>} Updated OperationalResourceResponse object
 */
export async function updateOperationalResource(centerId, resourceId, payload) {
  const response = await apiRequest(`/v1/centers/${centerId}/resources/${resourceId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
  return handleResponse(response);
}

// =================================================================================================
// Operational Staff Roster API Operations
// =================================================================================================

/**
 * Fetch operational staff roster for a center, optionally filtered by role and status.
 *
 * @param {number|string} centerId - Vaccination center identifier
 * @param {string|null} [role] - Optional StaffRole filter (VACCINATOR, CHECK_IN_STAFF, SUPERVISOR)
 * @param {string|null} [status] - Optional OperationalStaffStatus filter (ACTIVE, INACTIVE)
 * @returns {Promise<Array>} List of OperationalStaffRosterResponse objects
 */
export async function listStaffRoster(centerId, role = null, status = null) {
  const params = new URLSearchParams();
  if (role) params.append('role', role);
  if (status) params.append('status', status);
  const queryString = params.toString() ? `?${params.toString()}` : '';

  const response = await apiRequest(`/v1/centers/${centerId}/staff-roster${queryString}`, { method: 'GET' });
  return handleResponse(response);
}

/**
 * Register a staff member on the center's operational roster.
 *
 * @param {number|string} centerId - Vaccination center identifier
 * @param {Object} payload - RegisterStaffRosterRequest payload ({ userId, role, status })
 * @returns {Promise<Object>} Created OperationalStaffRosterResponse object
 */
export async function registerStaffMember(centerId, payload) {
  const response = await apiRequest(`/v1/centers/${centerId}/staff-roster`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return handleResponse(response);
}

/**
 * Update the mutable attributes (role, status) of an operational staff roster record.
 *
 * @param {number|string} centerId - Vaccination center identifier
 * @param {number|string} rosterId - Staff roster entry identifier
 * @param {Object} payload - UpdateStaffRosterRequest payload ({ role, status })
 * @returns {Promise<Object>} Updated OperationalStaffRosterResponse object
 */
export async function updateStaffRoster(centerId, rosterId, payload) {
  const response = await apiRequest(`/v1/centers/${centerId}/staff-roster/${rosterId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
  return handleResponse(response);
}
