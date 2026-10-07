import { apiRequestData } from '../../shared/api/apiClient';

const STAFF_REQUESTS_PATH = '/v1/admin/staff-requests';

export function listStaffRequests() {
  return apiRequestData(STAFF_REQUESTS_PATH, { method: 'GET' }, {
    defaultMessage: 'Failed to load pending staff account requests',
  });
}

export function getStaffRequest(requestId) {
  return apiRequestData(`${STAFF_REQUESTS_PATH}/${requestId}`, { method: 'GET' }, {
    defaultMessage: 'Failed to load the staff account request',
  });
}

export function approveStaffRequest(requestId, roleCode) {
  return apiRequestData(`${STAFF_REQUESTS_PATH}/${requestId}/approve`, {
    method: 'POST',
    body: JSON.stringify({ roleCode }),
  }, {
    defaultMessage: 'Failed to approve the staff account request',
  });
}

export function rejectStaffRequest(requestId, reason) {
  return apiRequestData(`${STAFF_REQUESTS_PATH}/${requestId}/reject`, {
    method: 'POST',
    body: JSON.stringify({ reason }),
  }, {
    defaultMessage: 'Failed to reject the staff account request',
  });
}
