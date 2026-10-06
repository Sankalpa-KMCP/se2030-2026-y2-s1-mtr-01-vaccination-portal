import { apiRequest } from '../../../shared/api/apiClient';

/**
 * API client for the authenticated citizen email OTP verification endpoints.
 * Identity always derives from the JWT bearer principal: no request carries an
 * email/user/account selector, and the OTP travels strictly as a string so
 * leading zeros survive.
 *
 * The Auth backend structures errors differently from Appointment: the machine
 * code lives in the `code` field (e.g. EMAIL_VERIFICATION_INVALID_CODE) and
 * throttled resends add `retryAfterSeconds`. Both are normalized onto the
 * thrown error so the dialog never has to parse messages.
 */

function toVerificationError(data, response, fallbackCode) {
  const error = new Error(data?.message || 'Email verification request failed');
  error.status = response.status;
  error.error = data?.error || fallbackCode;
  error.code = data?.code || data?.error || fallbackCode;
  error.retryAfterSeconds = typeof data?.retryAfterSeconds === 'number' ? data.retryAfterSeconds : null;
  error.data = data;
  return error;
}

/**
 * Reads the signed-in citizen's email verification state.
 *
 * @returns {Promise<{emailVerified: boolean, emailVerifiedAt: string|null}>}
 */
export async function getEmailVerificationStatus() {
  const response = await apiRequest('/v1/auth/email-verification/status');
  const data = await response.json();
  if (!response.ok) {
    throw toVerificationError(data, response, 'EMAIL_VERIFICATION_STATUS_FAILED');
  }
  return data;
}

/**
 * Verifies the signed-in citizen's email with the six-digit OTP string.
 *
 * @param {string} otp - exactly six digits; leading zeros are significant
 * @returns {Promise<{emailVerified: boolean, emailVerifiedAt: string|null}>}
 */
export async function verifyEmailOtp(otp) {
  const response = await apiRequest('/v1/auth/email-verification/verify', {
    method: 'POST',
    body: JSON.stringify({ otp }),
  });
  const data = await response.json();
  if (!response.ok) {
    throw toVerificationError(data, response, 'EMAIL_VERIFICATION_FAILED');
  }
  return data;
}

/**
 * Requests a fresh verification code for the signed-in citizen.
 * Sends no body: the backend derives the target account from the principal
 * and enforces the resend cooldown itself.
 *
 * @returns {Promise<{emailVerified: boolean, emailVerifiedAt: string|null}>}
 */
export async function resendEmailOtp() {
  const response = await apiRequest('/v1/auth/email-verification/resend', {
    method: 'POST',
  });
  const data = await response.json();
  if (!response.ok) {
    throw toVerificationError(data, response, 'EMAIL_VERIFICATION_RESEND_FAILED');
  }
  return data;
}
