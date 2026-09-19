import { apiRequest, parseApiResponse } from '../../../services/apiClient';

const DRAFTS_BASE_PATH = '/v1/vaccination/drafts';

/**
 * API client for the temporary vaccination draft CRUD (MF-05).
 * Interfaces with the backend VaccinationDraftController at
 * /api/v1/vaccination/drafts, which is restricted to VACCINATOR and SUPER_ADMIN
 * and enforces draft ownership isolation on the server.
 *
 * <p><b>Business Rules Handled:</b>
 * <ul>
 *   <li>Draft ownership isolation: the backend only returns and exposes the
 *       authenticated vaccinator's own drafts (SUPER_ADMIN may read all).</li>
 *   <li>Audited deletion: discarding a draft always requires a non-blank reason,
 *       which is recorded in the backend audit trail.</li>
 *   <li>Partial updates: PATCH bodies should only contain the mutable fields the
 *       vaccinator actually provided (the backend rejects unsupported fields).</li>
 * </ul>
 */

async function draftRequest(endpoint, options = {}) {
  const response = await apiRequest(`${DRAFTS_BASE_PATH}${endpoint}`, options);
  return parseApiResponse(response);
}

/**
 * Creates a new temporary vaccination draft for a checked-in appointment.
 *
 * @param {Object} payload - CreateVaccinationDraftRequest body
 * @param {number|string} payload.appointmentId - Required appointment identifier
 * @param {number|string} [payload.vaccineBatchId]
 * @param {number|string} [payload.doseNumber]
 * @param {string} [payload.administrationTime] - ISO-8601 instant
 * @param {string} [payload.nextDoseDate] - ISO local date (YYYY-MM-DD)
 * @param {string} [payload.authorizedNotes] - Max 2000 characters
 * @returns {Promise<Object>} VaccinationDraftResponse
 */
export function createVaccinationDraft(payload) {
  return draftRequest('', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

/**
 * Lists the authenticated vaccinator's own vaccination drafts.
 *
 * @returns {Promise<Array>} List of VaccinationDraftResponse objects
 */
export function listMyVaccinationDrafts() {
  return draftRequest('', { method: 'GET' });
}

/**
 * Reads a single vaccination draft by ID (ownership enforced server-side).
 *
 * @param {number|string} draftId
 * @returns {Promise<Object>} VaccinationDraftResponse
 */
export function getVaccinationDraft(draftId) {
  return draftRequest(`/${draftId}`, { method: 'GET' });
}

/**
 * Updates the provided mutable fields of a vaccination draft (partial update).
 *
 * @param {number|string} draftId
 * @param {Object} payload - UpdateVaccinationDraftRequest body; only include
 *   fields that should change (vaccineBatchId, doseNumber, administrationTime,
 *   nextDoseDate, authorizedNotes).
 * @returns {Promise<Object>} Updated VaccinationDraftResponse
 */
export function updateVaccinationDraft(draftId, payload) {
  return draftRequest(`/${draftId}`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
}

/**
 * Discards (deletes) a vaccination draft with a mandatory audited reason.
 * The reason is sent as the backend-supported `reason` query parameter and is
 * recorded in the audit trail; deletion without a reason is rejected.
 *
 * @param {number|string} draftId
 * @param {string} reason - Mandatory, non-blank audit reason (max 1000 chars)
 * @returns {Promise<null>} Resolves to null on the 204 No Content response
 */
export async function discardVaccinationDraft(draftId, reason) {
  const trimmedReason = typeof reason === 'string' ? reason.trim() : '';
  if (!trimmedReason) {
    throw new Error('A deletion reason is required to discard a vaccination draft.');
  }

  return draftRequest(`/${draftId}?reason=${encodeURIComponent(trimmedReason)}`, {
    method: 'DELETE',
  });
}
