import { apiRequest, parseApiResponse } from '../../../services/apiClient';

/**
 * Publicly verifies a digital vaccination certificate token without disclosing sensitive PII.
 * Compliant with NFR-PRV-01 Privacy Filter.
 *
 * @param {string} token
 * @returns {Promise<Object>} Formatted verification result object
 */
export async function verifyCertificatePublicly(token) {
  if (!token || !token.trim()) {
    throw new Error('Verification token or reference code is required.');
  }

  const cleanToken = token.trim();
  const endpoint = `/v1/vaccination/certificates/verify/${encodeURIComponent(cleanToken)}`;

  try {
    const response = await apiRequest(endpoint, {
      method: 'GET',
      auth: false,
      handleUnauthorized: false,
    });

    if (response.status === 404 || response.status === 400) {
      return {
        result: 'INVALID',
        message: 'No active or archived certificate records match the verification token provided. This credential may be forged or entered incorrectly.',
      };
    }

    const data = await parseApiResponse(response);

    return {
      result: data.status || (data.valid ? 'VALID' : 'REVOKED'),
      versionNumber: 1,
      certificateReference: data.certificateReference,
      doseNumber: data.doseNumber,
      administrationDate: data.administrationTime,
      citizenDisplayName: 'Verified Citizen',
      vaccineName: 'Approved COVID-19 / Infectious Disease Formulation',
      issuingOrganization: 'National Vaccination Authority',
    };
  } catch (err) {
    if (err.status === 404 || err.status === 400) {
      return {
        result: 'INVALID',
        message: 'No active or archived certificate records match the verification token provided. This credential may be forged or entered incorrectly.',
      };
    }
    throw err;
  }
}

/**
 * Lists the authenticated citizen's permanent vaccination history.
 *
 * @returns {Promise<Array>} List of CitizenVaccinationRecordResponse objects
 */
export async function listMyVaccinations() {
  const response = await apiRequest('/v1/vaccination/records/mine', {
    method: 'GET',
  });
  return parseApiResponse(response);
}

/**
 * Lists the authenticated citizen's issued digital certificates.
 *
 * @returns {Promise<Array>} List of CitizenCertificateResponse objects
 */
export async function listMyCertificates() {
  const response = await apiRequest('/v1/vaccination/certificates/mine', {
    method: 'GET',
  });
  return parseApiResponse(response);
}

/**
 * Downloads the official PDF certificate file generated from immutable vaccination records.
 *
 * @param {number|string} certificateId
 * @param {string} [certificateReference]
 * @returns {Promise<boolean>}
 */
export async function downloadCertificate(certificateId, certificateReference = 'certificate') {
  const response = await apiRequest(`/v1/vaccination/certificates/${certificateId}/download`, {
    method: 'GET',
    headers: {
      Accept: 'application/pdf',
    },
  });

  if (!response.ok) {
    throw new Error('Failed to download vaccination certificate PDF.');
  }

  const blob = await response.blob();
  const downloadUrl = window.URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = downloadUrl;
  anchor.download = `certificate-${certificateReference}.pdf`;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  window.URL.revokeObjectURL(downloadUrl);

  return true;
}

/**
 * Lists the permanent vaccination records administered by the authenticated vaccinator.
 *
 * @returns {Promise<Array>} List of VaccinatorVaccinationRecordResponse objects
 */
export async function listVaccinatorRecords() {
  const response = await apiRequest('/v1/vaccination/records/vaccinator/mine', {
    method: 'GET',
  });
  return parseApiResponse(response);
}

/**
 * Gets detailed record info for a single vaccination record.
 *
 * @param {number|string} recordId
 * @returns {Promise<Object>}
 */
export async function getRecordDetails(recordId) {
  const response = await apiRequest(`/v1/vaccination/records/${recordId}`, {
    method: 'GET',
  });
  return parseApiResponse(response);
}

/**
 * Retrieves available compatible vaccine batches for a specific appointment.
 *
 * @param {number|string} appointmentId
 * @returns {Promise<Array>}
 */
export async function getAvailableBatches(appointmentId) {
  const response = await apiRequest(`/v1/vaccination/batches/available?appointmentId=${encodeURIComponent(appointmentId)}`, {
    method: 'GET',
  });
  return parseApiResponse(response);
}

/**
 * Submits a correction request from a citizen for their own vaccination record.
 *
 * @param {number|string} recordId
 * @param {string} message
 * @returns {Promise<Object>}
 */
export async function requestVaccinationCorrection(recordId, message) {
  const response = await apiRequest(`/v1/vaccination/records/${recordId}/corrections`, {
    method: 'POST',
    body: JSON.stringify({ message }),
  });
  return parseApiResponse(response);
}

/**
 * Lists the authenticated citizen's correction requests.
 *
 * @returns {Promise<Array>}
 */
export async function listMyCorrectionRequests() {
  const response = await apiRequest('/v1/vaccination/corrections/mine', {
    method: 'GET',
  });
  return parseApiResponse(response);
}

/**
 * Lists all correction requests for review by administrators.
 *
 * @returns {Promise<Array>}
 */
export async function listAdminCorrectionRequests() {
  const response = await apiRequest('/v1/vaccination/corrections', {
    method: 'GET',
  });
  return parseApiResponse(response);
}

/**
 * Approves a pending correction request (Admin only).
 *
 * @param {number|string} recordId
 * @param {number|string} correctionId
 * @param {string} [reason]
 * @returns {Promise<Object>}
 */
export async function approveCorrectionRequest(recordId, correctionId, reason = '') {
  const response = await apiRequest(`/v1/vaccination/records/${recordId}/corrections/${correctionId}/approve`, {
    method: 'POST',
    body: JSON.stringify({ reason }),
  });
  return parseApiResponse(response);
}

/**
 * Rejects a pending correction request (Admin only).
 *
 * @param {number|string} recordId
 * @param {number|string} correctionId
 * @param {string} reason
 * @returns {Promise<Object>}
 */
export async function rejectCorrectionRequest(recordId, correctionId, reason) {
  const response = await apiRequest(`/v1/vaccination/records/${recordId}/corrections/${correctionId}/reject`, {
    method: 'POST',
    body: JSON.stringify({ reason }),
  });
  return parseApiResponse(response);
}

/**
 * Lists correction requests for vaccinations administered by the authenticated vaccinator.
 *
 * @returns {Promise<Array>}
 */
export async function listVaccinatorCorrectionRequests() {
  const response = await apiRequest('/v1/vaccination/corrections/administered', {
    method: 'GET',
  });
  return parseApiResponse(response);
}

/**
 * Applies an approved correction and triggers certificate supersession/reissue (Vaccinator only).
 *
 * @param {number|string} recordId
 * @param {number|string} correctionId
 * @param {{ fieldName?: string, newValue?: string }} [payload]
 * @returns {Promise<Object>}
 */
export async function applyCorrectionRequest(recordId, correctionId, payload = {}) {
  const response = await apiRequest(`/v1/vaccination/records/${recordId}/corrections/${correctionId}/apply`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return parseApiResponse(response);
}

/**
 * Retrieves the correction history for a specific vaccination record.
 *
 * @param {number|string} recordId
 * @returns {Promise<Array>}
 */
export async function getRecordCorrections(recordId) {
  const response = await apiRequest(`/v1/vaccination/records/${recordId}/corrections`, {
    method: 'GET',
  });
  return parseApiResponse(response);
}
