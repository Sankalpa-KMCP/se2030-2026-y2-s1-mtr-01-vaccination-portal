/**
 * Safely derives 'YYYY-MM-DD' from the local calendar day of the provided Date object.
 * Does NOT rely on toISOString() or UTC conversion to avoid shifting calendar days across timezones.
 *
 * @param {Date} [date=new Date()]
 * @returns {string} Local date string in 'YYYY-MM-DD' format
 */
export function getLocalCalendarToday(date = new Date()) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

/**
 * Extracts 'YYYY-MM-DD' from an ISO string, date string, or null/undefined.
 *
 * @param {string|null|undefined} value
 * @returns {string} 'YYYY-MM-DD' or empty string
 */
export function formatLocalDateOnly(value) {
  if (!value) return '';
  return String(value).slice(0, 10);
}

/**
 * Converts a local 'YYYY-MM-DD' date into the backend API ISO midnight format: `${dateStr}T00:00:00Z`.
 *
 * @param {string} dateStr - Date string in 'YYYY-MM-DD' format
 * @returns {string} ISO-8601 string at UTC midnight
 */
export function toApiIsoDate(dateStr) {
  if (!dateStr) return '';
  return `${dateStr}T00:00:00Z`;
}
