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
