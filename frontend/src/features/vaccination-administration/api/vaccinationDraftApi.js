import { apiRequestData } from '../../../services/apiClient';

const resource = '/v1/vaccination/drafts';
const editableFields = ['vaccineBatchId', 'doseNumber', 'administrationTime', 'nextDoseDate', 'authorizedNotes'];

function requestBody(fields, payload) {
  return JSON.stringify(Object.fromEntries(fields
    .filter((field) => payload[field] !== undefined)
    .map((field) => [field, payload[field]])));
}

export const vaccinationDraftApi = {
  list: () => apiRequestData(resource),
  get: (id) => apiRequestData(`${resource}/${id}`),
  create: (payload) => apiRequestData(resource, {
    method: 'POST', body: requestBody(['appointmentId', ...editableFields], payload),
  }),
  update: (id, payload) => apiRequestData(`${resource}/${id}`, {
    method: 'PATCH', body: requestBody(editableFields, payload),
  }),
  remove: (id) => apiRequestData(`${resource}/${id}`, { method: 'DELETE' }),
};
