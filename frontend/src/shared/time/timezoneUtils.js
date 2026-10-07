/**
 * Shared timezone and temporal utility functions for the Vaccination Portal frontend.
 *
 * Provides native Intl.DateTimeFormat-based timezone conversions between local wall-clock
 * date/time inputs and ISO-8601 UTC Instant representations without external dependencies.
 */

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
