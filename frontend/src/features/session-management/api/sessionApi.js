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
 * Fetch list of vaccination session summaries, optionally filtered by campaign, center, and lifecycle status.
 *
 * @param {Object} [filters={}] - Optional query filters
 * @param {number|string|null} [filters.campaignId] - Optional campaign identifier
 * @param {number|string|null} [filters.centerId] - Optional center identifier
 * @param {string|null} [filters.status] - Optional SessionLifecycleStatus
 * @returns {Promise<Array>} List of SessionSummaryResponse objects
 */
export async function listSessions(filters = {}) {
  const params = new URLSearchParams();
  if (filters.campaignId) {
    params.append('campaignId', filters.campaignId);
  }
  if (filters.centerId) {
    params.append('centerId', filters.centerId);
  }
  if (filters.status) {
    params.append('status', filters.status);
  }

  const queryString = params.toString() ? `?${params.toString()}` : '';
  const response = await apiRequest(`/v1/sessions${queryString}`, {
    method: 'GET',
  });
  return handleResponse(response);
}

/**
 * Fetch detailed vaccination session record by identifier.
 *
 * @param {number|string} sessionId - Vaccination session identifier
 * @returns {Promise<Object>} SessionResponse object
 */
export async function getSessionById(sessionId) {
  const response = await apiRequest(`/v1/sessions/${sessionId}`, {
    method: 'GET',
  });
  return handleResponse(response);
}

/**
 * Retrieves the authoritative, side-effect-free operational readiness diagnostics for a vaccination session.
 *
 * @param {number|string} sessionId - Vaccination session identifier
 * @returns {Promise<Object>} SessionOperationalReadinessResponse object
 */
export async function getSessionReadiness(sessionId) {
  const response = await apiRequest(`/v1/sessions/${sessionId}/readiness`, {
    method: 'GET',
  });
  return handleResponse(response);
}

/**
 * Create a new one-off vaccination session in initial DRAFT lifecycle status.
 *
 * @param {Object} payload - CreateSessionRequest { campaignId, centerId, startAt, endAt, bookingCloseAt }
 * @returns {Promise<Object>} Created SessionResponse object
 */
export async function createSession(payload) {
  const response = await apiRequest('/v1/sessions', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return handleResponse(response);
}

// -------------------------------------------------------------------------------------------------
// Timezone and Date/Time Conversion Utilities (Re-exported from shared timezoneUtils)
// -------------------------------------------------------------------------------------------------

export {
  localToUtcIso,
  isValidIanaTimezone,
  formatLocalTimeForDto,
  formatInTimezone,
  formatDateInTimezone,
  formatTimeInTimezone,
  utcToLocalDateAndTime,
} from '../../../services/timezoneUtils';



// -------------------------------------------------------------------------------------------------
// Capacity Planning API (F5B)
// -------------------------------------------------------------------------------------------------

/**
 * Previews throughput and occupancy parameters for a session from operational state without persisting.
 *
 * @param {number|string} sessionId - Vaccination session identifier
 * @param {Object} payload - PreviewCapacityPlanRequest
 * @returns {Promise<Object>} CapacityPlanPreviewResponse
 */
export async function previewCapacityPlan(sessionId, payload) {
  const response = await apiRequest(`/v1/sessions/${sessionId}/capacity-plans/preview`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return handleResponse(response);
}

/**
 * Create and approve a new immutable session capacity plan revision.
 *
 * @param {number|string} sessionId - Vaccination session identifier
 * @param {Object} payload - CreateSessionCapacityPlanRequest
 * @returns {Promise<Object>} Created SessionCapacityPlanResponse
 */
export async function createCapacityPlan(sessionId, payload) {
  const response = await apiRequest(`/v1/sessions/${sessionId}/capacity-plans`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return handleResponse(response);
}

/**
 * List all historical capacity plan revisions for a vaccination session, ordered by revision ascending.
 *
 * @param {number|string} sessionId - Vaccination session identifier
 * @returns {Promise<Array>} List of SessionCapacityPlanResponse objects
 */
export async function listCapacityPlans(sessionId) {
  const response = await apiRequest(`/v1/sessions/${sessionId}/capacity-plans`, {
    method: 'GET',
  });
  return handleResponse(response);
}

// -------------------------------------------------------------------------------------------------
// Simple Session Lifecycle API (F4B)
// -------------------------------------------------------------------------------------------------

/**
 * Transitions a DRAFT session to SCHEDULED status after backend operational readiness validation.
 *
 * @param {number|string} sessionId - Vaccination session identifier
 * @returns {Promise<Object>} Updated SessionResponse object
 */
export async function scheduleSession(sessionId) {
  const response = await apiRequest(`/v1/sessions/${sessionId}/schedule`, {
    method: 'POST',
  });
  return handleResponse(response);
}

/**
 * Transitions a SCHEDULED session to IN_PROGRESS status when scheduled start time has arrived.
 *
 * @param {number|string} sessionId - Vaccination session identifier
 * @returns {Promise<Object>} Updated SessionResponse object
 */
export async function startSession(sessionId) {
  const response = await apiRequest(`/v1/sessions/${sessionId}/start`, {
    method: 'POST',
  });
  return handleResponse(response);
}

/**
 * Transitions an IN_PROGRESS session to COMPLETED status.
 *
 * @param {number|string} sessionId - Vaccination session identifier
 * @returns {Promise<Object>} Updated SessionResponse object
 */
export async function completeSession(sessionId) {
  const response = await apiRequest(`/v1/sessions/${sessionId}/complete`, {
    method: 'POST',
  });
  return handleResponse(response);
}

// -------------------------------------------------------------------------------------------------
// Session Change Control API (F4C)
// -------------------------------------------------------------------------------------------------

/**
 * Previews the cancellation of a vaccination session without mutating state.
 *
 * @param {number|string} sessionId - Vaccination session identifier
 * @returns {Promise<Object>} SessionChangePreviewResponse object
 */
export async function previewSessionCancellation(sessionId) {
  const response = await apiRequest(`/v1/sessions/${sessionId}/cancellation-preview`, {
    method: 'POST',
  });
  return handleResponse(response);
}

/**
 * Confirms the cancellation of a vaccination session.
 * Authenticated principal identity is derived server-side.
 *
 * @param {number|string} sessionId - Vaccination session identifier
 * @param {Object} payload - { previewToken, reason }
 * @returns {Promise<Object>} Updated SessionResponse object
 */
export async function cancelSession(sessionId, payload) {
  const response = await apiRequest(`/v1/sessions/${sessionId}/cancel`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return handleResponse(response);
}

/**
 * Previews a proposed same-center reschedule of a vaccination session without mutating state.
 *
 * @param {number|string} sessionId - Vaccination session identifier
 * @param {Object} payload - { newStartAt, newEndAt, newBookingCloseAt }
 * @returns {Promise<Object>} SessionChangePreviewResponse object
 */
export async function previewSessionReschedule(sessionId, payload) {
  const response = await apiRequest(`/v1/sessions/${sessionId}/reschedule-preview`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return handleResponse(response);
}

/**
 * Confirms a same-center reschedule of a vaccination session.
 * Authenticated principal identity is derived server-side.
 *
 * @param {number|string} sessionId - Vaccination session identifier
 * @param {Object} payload - { newStartAt, newEndAt, newBookingCloseAt, previewToken, reason }
 * @returns {Promise<Object>} Updated SessionResponse object
 */
export async function rescheduleSession(sessionId, payload) {
  const response = await apiRequest(`/v1/sessions/${sessionId}/reschedule`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return handleResponse(response);
}

/**
 * Previews a proposed center transfer of a vaccination session without mutating state.
 *
 * @param {number|string} sessionId - Vaccination session identifier
 * @param {Object} payload - { newCenterId, newStartAt, newEndAt, newBookingCloseAt }
 * @returns {Promise<Object>} SessionChangePreviewResponse object
 */
export async function previewSessionTransfer(sessionId, payload) {
  const response = await apiRequest(`/v1/sessions/${sessionId}/transfer-preview`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return handleResponse(response);
}

/**
 * Confirms a center transfer of a vaccination session.
 * Authenticated principal identity is derived server-side.
 *
 * @param {number|string} sessionId - Vaccination session identifier
 * @param {Object} payload - { newCenterId, newStartAt, newEndAt, newBookingCloseAt, previewToken, reason }
 * @returns {Promise<Object>} Updated SessionResponse object
 */
export async function transferSession(sessionId, payload) {
  const response = await apiRequest(`/v1/sessions/${sessionId}/transfer`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return handleResponse(response);
}

/**
 * Lists all historical change events for a vaccination session.
 *
 * @param {number|string} sessionId - Vaccination session identifier
 * @returns {Promise<Array>} List of SessionChangeEventResponse objects
 */
export async function listSessionChangeEvents(sessionId) {
  const response = await apiRequest(`/v1/sessions/${sessionId}/change-events`, {
    method: 'GET',
  });
  return handleResponse(response);
}

// -------------------------------------------------------------------------------------------------
// Session Recurrence Rule and Series Management (F4D)
// -------------------------------------------------------------------------------------------------

/**
 * Creates a new bounded session recurrence rule.
 *
 * @param {Object} payload - CreateSessionRecurrenceRuleRequest
 * @returns {Promise<Object>} SessionRecurrenceRuleResponse
 */
export async function createRecurrenceRule(payload) {
  const response = await apiRequest('/v1/recurrence-rules', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return handleResponse(response);
}

/**
 * Retrieves a specific session recurrence rule by identifier.
 *
 * @param {number|string} ruleId - Recurrence rule identifier
 * @returns {Promise<Object>} SessionRecurrenceRuleResponse
 */
export async function getRecurrenceRuleById(ruleId) {
  const response = await apiRequest(`/v1/recurrence-rules/${ruleId}`, {
    method: 'GET',
  });
  return handleResponse(response);
}

/**
 * Lists session recurrence rules filtered by campaign, center, and status.
 *
 * @param {Object} [filters={}] - Optional query filters { campaignId, centerId, status }
 * @returns {Promise<Array>} List of SessionRecurrenceRuleResponse
 */
export async function listRecurrenceRules(filters = {}) {
  const params = new URLSearchParams();
  if (filters.campaignId) {
    params.append('campaignId', filters.campaignId);
  }
  if (filters.centerId) {
    params.append('centerId', filters.centerId);
  }
  if (filters.status) {
    params.append('status', filters.status);
  }

  const queryString = params.toString() ? `?${params.toString()}` : '';
  const response = await apiRequest(`/v1/recurrence-rules${queryString}`, {
    method: 'GET',
  });
  return handleResponse(response);
}

/**
 * Updates the future schedule definition of an un-materialized recurrence rule.
 *
 * @param {number|string} ruleId - Recurrence rule identifier
 * @param {Object} payload - UpdateSessionRecurrenceRuleRequest
 * @returns {Promise<Object>} Updated SessionRecurrenceRuleResponse
 */
export async function updateRecurrenceRule(ruleId, payload) {
  const response = await apiRequest(`/v1/recurrence-rules/${ruleId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
  return handleResponse(response);
}

/**
 * Materializes concrete session occurrences for a recurrence rule within an optional date window.
 *
 * @param {number|string} ruleId - Recurrence rule identifier
 * @param {Object} [payload=null] - Optional GenerateOccurrencesRequest { requestedStartDate, requestedEndDate }
 * @returns {Promise<Object>} RecurrenceGenerationResponse
 */
export async function generateRecurrenceOccurrences(ruleId, payload = null) {
  const options = {
    method: 'POST',
  };
  if (payload && (payload.requestedStartDate || payload.requestedEndDate)) {
    options.body = JSON.stringify({
      requestedStartDate: payload.requestedStartDate || null,
      requestedEndDate: payload.requestedEndDate || null,
    });
  }
  const response = await apiRequest(`/v1/recurrence-rules/${ruleId}/generate`, options);
  return handleResponse(response);
}

/**
 * Previews a series-wide schedule change without applying mutations.
 *
 * @param {number|string} ruleId - Recurrence rule identifier
 * @param {Object} payload - PreviewSeriesChangeRequest
 * @returns {Promise<Object>} SeriesChangePreviewResponse
 */
export async function previewSeriesChange(ruleId, payload) {
  const response = await apiRequest(`/v1/recurrence-rules/${ruleId}/change-preview`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return handleResponse(response);
}

/**
 * Confirms and applies a series-wide schedule change.
 * Authenticated server-side principal supplies audit identity; actorId is strictly omitted.
 *
 * @param {number|string} ruleId - Recurrence rule identifier
 * @param {Object} payload - { weekdays, startDate, endDate, startTime, endTime, timezone, previewToken, reason }
 * @returns {Promise<Object>} SeriesChangeResultResponse
 */
export async function confirmSeriesChange(ruleId, payload) {
  const cleanPayload = {
    weekdays: payload.weekdays,
    startDate: payload.startDate,
    endDate: payload.endDate,
    startTime: payload.startTime,
    endTime: payload.endTime,
    timezone: payload.timezone,
    previewToken: payload.previewToken,
    reason: payload.reason,
  };
  const response = await apiRequest(`/v1/recurrence-rules/${ruleId}/change`, {
    method: 'POST',
    body: JSON.stringify(cleanPayload),
  });
  return handleResponse(response);
}

/**
 * Previews a full series cancellation without applying mutations.
 *
 * @param {number|string} ruleId - Recurrence rule identifier
 * @returns {Promise<Object>} SeriesChangePreviewResponse
 */
export async function previewSeriesCancellation(ruleId) {
  const response = await apiRequest(`/v1/recurrence-rules/${ruleId}/cancellation-preview`, {
    method: 'POST',
  });
  return handleResponse(response);
}

/**
 * Confirms and applies full series cancellation.
 * Authenticated server-side principal supplies audit identity; actorId is strictly omitted.
 *
 * @param {number|string} ruleId - Recurrence rule identifier
 * @param {Object} payload - { previewToken, reason }
 * @returns {Promise<Object>} SeriesChangeResultResponse
 */
export async function confirmSeriesCancellation(ruleId, payload) {
  const cleanPayload = {
    previewToken: payload.previewToken,
    reason: payload.reason,
  };
  const response = await apiRequest(`/v1/recurrence-rules/${ruleId}/cancel`, {
    method: 'POST',
    body: JSON.stringify(cleanPayload),
  });
  return handleResponse(response);
}

// -------------------------------------------------------------------------------------------------
// Session Time-Slot Management API (H04-S2)
// -------------------------------------------------------------------------------------------------

/**
 * List all operational time slots configured for a vaccination session.
 *
 * @param {number|string} sessionId - Vaccination session identifier
 * @returns {Promise<Array>} List of TimeSlotResponse objects
 */
export async function listTimeSlots(sessionId) {
  const response = await apiRequest(`/v1/sessions/${sessionId}/time-slots`, {
    method: 'GET',
  });
  return handleResponse(response);
}

/**
 * Create a new operational time slot in a vaccination session.
 *
 * @param {number|string} sessionId - Vaccination session identifier
 * @param {Object} payload - CreateTimeSlotRequest { startTime: string, endTime: string, capacity: number }
 * @returns {Promise<Object>} Created TimeSlotResponse object
 */
export async function createTimeSlot(sessionId, payload) {
  const response = await apiRequest(`/v1/sessions/${sessionId}/time-slots`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return handleResponse(response);
}

/**
 * Update the capacity of an existing time slot in a vaccination session.
 *
 * @param {number|string} sessionId - Vaccination session identifier
 * @param {number|string} slotId - TimeSlot identifier
 * @param {Object} payload - UpdateTimeSlotCapacityRequest { capacity: number }
 * @returns {Promise<Object>} Updated TimeSlotResponse object
 */
export async function updateTimeSlotCapacity(sessionId, slotId, payload) {
  const response = await apiRequest(`/v1/sessions/${sessionId}/time-slots/${slotId}/capacity`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
  return handleResponse(response);
}

/**
 * Authoritatively suspend booking intake on a scheduled vaccination session.
 *
 * @param {number|string} sessionId - Vaccination session identifier
 * @param {Object} payload - SuspendSessionBookingRequest { reason: string }
 * @returns {Promise<Object>} Updated SessionResponse object
 */
export async function suspendSessionBooking(sessionId, payload) {
  const response = await apiRequest(`/v1/sessions/${sessionId}/booking-suspend`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return handleResponse(response);
}

/**
 * Authoritatively resume booking intake on a previously suspended scheduled vaccination session.
 *
 * @param {number|string} sessionId - Vaccination session identifier
 * @param {Object} payload - ResumeSessionBookingRequest { reason: string }
 * @returns {Promise<Object>} Updated SessionResponse object
 */
export async function resumeSessionBooking(sessionId, payload) {
  const response = await apiRequest(`/v1/sessions/${sessionId}/booking-resume`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return handleResponse(response);
}
