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
// Timezone and Date/Time Conversion Utilities (Platform Native Intl / Date)
// -------------------------------------------------------------------------------------------------

/**
 * Converts a wall-clock date string (YYYY-MM-DD) and time string (HH:mm) in a specified
 * IANA timezone (e.g. "Asia/Colombo", "UTC", "America/New_York") to an ISO-8601 UTC Instant string.
 *
 * Performs deterministic round-trip verification to detect and reject nonexistent DST gap times
 * and resolve timezone offsets without third-party dependencies.
 *
 * @param {string} dateStr - Date formatted as YYYY-MM-DD
 * @param {string} timeStr - Time formatted as HH:mm (or HH:mm:ss)
 * @param {string} [timeZone='UTC'] - Authoritative IANA time zone identifier
 * @returns {string} ISO-8601 UTC Instant string (e.g. "2026-08-25T04:00:00.000Z")
 * @throws {Error} if local time is nonexistent or invalid in the target time zone
 */
export function localToUtcIso(dateStr, timeStr, timeZone = 'UTC') {
  if (!dateStr || !timeStr) {
    throw new Error('Date and time must be provided');
  }

  const [yearStr, monthStr, dayStr] = dateStr.split('-');
  const [hourStr, minStr] = timeStr.split(':');
  const year = parseInt(yearStr, 10);
  const month = parseInt(monthStr, 10);
  const day = parseInt(dayStr, 10);
  const hour = parseInt(hourStr, 10);
  const minute = parseInt(minStr, 10);
  const second = 0;

  if (isNaN(year) || isNaN(month) || isNaN(day) || isNaN(hour) || isNaN(minute)) {
    throw new Error('Invalid date or time component format');
  }

  const localUtcMs = Date.UTC(year, month - 1, day, hour, minute, second);

  const getOffsetMs = (instantMs) => {
    const formatter = new Intl.DateTimeFormat('en-US', {
      timeZone: timeZone || 'UTC',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hourCycle: 'h23',
    });
    const parts = formatter.formatToParts(new Date(instantMs));
    const getPart = (type) => parseInt(parts.find((p) => p.type === type)?.value || '0', 10);
    const y = getPart('year');
    const m = getPart('month');
    const d = getPart('day');
    let h = getPart('hour');
    if (h === 24) h = 0;
    const min = getPart('minute');
    const s = getPart('second');
    const asUtc = Date.UTC(y, m - 1, d, h, min, s);
    return asUtc - instantMs;
  };

  // Initial estimate and second-pass refinement
  let targetInstantMs = localUtcMs - getOffsetMs(localUtcMs);
  const offset2 = getOffsetMs(targetInstantMs);
  targetInstantMs = localUtcMs - offset2;

  // Round-trip verification
  const verifier = new Intl.DateTimeFormat('en-US', {
    timeZone: timeZone || 'UTC',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hourCycle: 'h23',
  });
  const verifyParts = verifier.formatToParts(new Date(targetInstantMs));
  const getVerifyPart = (type) => parseInt(verifyParts.find((p) => p.type === type)?.value || '0', 10);
  let verifyHour = getVerifyPart('hour');
  if (verifyHour === 24) verifyHour = 0;

  const match = (
    getVerifyPart('year') === year &&
    getVerifyPart('month') === month &&
    getVerifyPart('day') === day &&
    verifyHour === hour &&
    getVerifyPart('minute') === minute
  );

  if (!match) {
    throw new Error(
      `Selected time ${dateStr} ${timeStr} is invalid or nonexistent in timezone ${timeZone} (e.g. daylight saving transition gap)`
    );
  }

  return new Date(targetInstantMs).toISOString();
}

/**
 * Validates whether a provided string represents a valid, usable IANA timezone identifier.
 *
 * @param {string|null|undefined} timeZone - Timezone string to validate
 * @returns {boolean} True if valid IANA timezone; false otherwise
 */
export function isValidIanaTimezone(timeZone) {
  if (!timeZone || typeof timeZone !== 'string' || !timeZone.trim()) {
    return false;
  }
  try {
    new Intl.DateTimeFormat('en-US', { timeZone: timeZone.trim() });
    return true;
  } catch {
    return false;
  }
}

/**
 * Normalizes a local time string for backend Jackson LocalTime serialization.
 * Preserves seconds if present (HH:mm:ss), or appends :00 to HH:mm without truncating precision.
 *
 * @param {string|null|undefined} timeStr - Time string
 * @returns {string|null} Normalized LocalTime string (e.g. "09:00:30" or "09:00:00")
 */
export function formatLocalTimeForDto(timeStr) {
  if (!timeStr || typeof timeStr !== 'string') return null;
  const trimmed = timeStr.trim();
  const parts = trimmed.split(':');
  if (parts.length === 2) {
    return `${trimmed}:00`;
  }
  return trimmed;
}

/**
 * Format an ISO UTC instant string in a specified IANA schedule timezone.
 *
 * @param {string} isoString - ISO UTC string
 * @param {string} [timeZone='UTC'] - IANA time zone
 * @param {Object} [options={}] - Custom Intl.DateTimeFormat options
 * @returns {string} Formatted human-readable string
 */
export function formatInTimezone(isoString, timeZone = 'UTC', options = {}) {
  if (!isoString) return '—';
  try {
    const date = new Date(isoString);
    if (isNaN(date.getTime())) return isoString;
    return new Intl.DateTimeFormat('en-US', {
      timeZone: timeZone || 'UTC',
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      hour12: true,
      ...options,
    }).format(date);
  } catch {
    return isoString;
  }
}

/**
 * Format only the date portion in a specified schedule timezone.
 *
 * @param {string} isoString - ISO UTC string
 * @param {string} [timeZone='UTC'] - IANA time zone
 * @returns {string} Formatted date (e.g. "Aug 25, 2026")
 */
export function formatDateInTimezone(isoString, timeZone = 'UTC') {
  return formatInTimezone(isoString, timeZone, {
    hour: undefined,
    minute: undefined,
    hour12: undefined,
  });
}

/**
 * Format only the time portion in a specified schedule timezone.
 *
 * @param {string} isoString - ISO UTC string
 * @param {string} [timeZone='UTC'] - IANA time zone
 * @returns {string} Formatted time (e.g. "09:30 AM")
 */
export function formatTimeInTimezone(isoString, timeZone = 'UTC') {
  return formatInTimezone(isoString, timeZone, {
    year: undefined,
    month: undefined,
    day: undefined,
  });
}

/**
 * Splits an ISO UTC instant string into local date (YYYY-MM-DD) and local time (HH:mm) strings
 * in a specified schedule timezone for prefilling HTML date and time input elements.
 *
 * @param {string} isoString - ISO UTC string
 * @param {string} [timeZone='UTC'] - IANA time zone
 * @returns {{ date: string, time: string }} Object containing formatted date and time strings
 */
export function utcToLocalDateAndTime(isoString, timeZone = 'UTC') {
  if (!isoString) return { date: '', time: '' };
  try {
    const date = new Date(isoString);
    if (isNaN(date.getTime())) return { date: '', time: '' };
    const formatter = new Intl.DateTimeFormat('en-US', {
      timeZone: timeZone || 'UTC',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hourCycle: 'h23',
    });
    const parts = formatter.formatToParts(date);
    const getPart = (type) => parts.find((p) => p.type === type)?.value || '';
    const y = getPart('year');
    const m = getPart('month');
    const d = getPart('day');
    let h = getPart('hour');
    if (h === '24') h = '00';
    const min = getPart('minute');
    return {
      date: `${y}-${m}-${d}`,
      time: `${h.padStart(2, '0')}:${min.padStart(2, '0')}`,
    };
  } catch {
    return { date: '', time: '' };
  }
}


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
