import { apiRequest, parseApiResponse } from '../../../shared/api/apiClient';

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
