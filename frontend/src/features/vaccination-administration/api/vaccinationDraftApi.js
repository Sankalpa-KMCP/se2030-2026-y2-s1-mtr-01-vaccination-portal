import { apiRequest, parseApiResponse } from '../../../services/apiClient';

const DRAFTS_BASE_PATH = '/v1/vaccination/drafts';
const EDITABLE_FIELDS = ['vaccineBatchId', 'doseNumber', 'administrationTime', 'nextDoseDate', 'authorizedNotes'];

/**
 * API client for the temporary vaccination draft CRUD (MF-05).
 * Interfaces with the backend VaccinationDraftController at
 * /api/v1/vaccination/drafts, which is restricted to VACCINATOR and SUPER_ADMIN
 * and enforces draft ownership isolation on the server.
 */

async function draftRequest(endpoint, options = {}) {
  const response = await apiRequest(`${DRAFTS_BASE_PATH}${endpoint}`, options);
  return parseApiResponse(response);
}

/**
 * Creates a new temporary vaccination draft for a checked-in appointment.
 *
 * @param {Object} payload - CreateVaccinationDraftRequest body
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
 * @param {Object} payload - UpdateVaccinationDraftRequest body
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

/**
 * Confirms the physical administration of a vaccination draft.
 * Transitions draft status to READY_FOR_FINALIZATION.
 *
 * @param {number|string} draftId
 * @returns {Promise<Object>} VaccinationDraftResponse
 */
export function confirmPhysicalAdministration(draftId) {
  return draftRequest(`/${draftId}/confirm-administration`, {
    method: 'POST',
  });
}

/**
 * Finalizes a vaccination draft: creates immutable record, deducts inventory dose,
 * completes appointment, issues digital certificate, and marks draft FINALIZED.
 *
 * @param {number|string} draftId
 * @returns {Promise<Object>} FinalizeVaccinationResponse
 */
export function finalizeVaccinationDraft(draftId) {
  return draftRequest(`/${draftId}/finalize`, {
    method: 'POST',
  });
}

/**
 * Confirms physical administration and transactionally finalizes the draft in one step.
 * Creates immutable record, deducts stock, completes appointment, issues certificate, and finalizes draft.
 *
 * @param {number|string} draftId
 * @returns {Promise<Object>} FinalizeVaccinationResponse
 */
export function confirmAndFinalizeVaccinationDraft(draftId) {
  return draftRequest(`/${draftId}/confirm-and-finalize`, {
    method: 'POST',
  });
}

function filterFields(fields, payload) {
  if (!payload || typeof payload !== 'object') return payload;
  return Object.fromEntries(fields
    .filter((field) => payload[field] !== undefined)
    .map((field) => [field, payload[field]]));
}

/**
 * Object-style export compatible with VaccinationDraftsPage.
 */
export const vaccinationDraftApi = {
  list: () => listMyVaccinationDrafts(),
  get: (id) => getVaccinationDraft(id),
  create: (payload) => createVaccinationDraft(filterFields(['appointmentId', ...EDITABLE_FIELDS], payload)),
  update: (id, payload) => updateVaccinationDraft(id, filterFields(EDITABLE_FIELDS, payload)),
  remove: (id, reason = 'Discarded by vaccinator') => discardVaccinationDraft(id, reason),
  confirmAdministration: (id) => confirmPhysicalAdministration(id),
  finalize: (id) => finalizeVaccinationDraft(id),
  confirmAndFinalize: (id) => confirmAndFinalizeVaccinationDraft(id),
};

