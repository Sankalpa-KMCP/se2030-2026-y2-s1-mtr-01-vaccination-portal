/**
 * Authentication REST API Service.
 * Interfaces with backend /api/v1/auth endpoints.
 */

import { ApiError, apiRequestData } from '../../shared/api/apiClient';

const LOGIN_ERROR_MESSAGES = {
  ACCOUNT_PENDING_APPROVAL: 'Your staff account is waiting for administrator approval.',
  ACCOUNT_REJECTED: 'Your staff account request was rejected. Contact an administrator if you need assistance.',
  ACCOUNT_PENDING_VERIFICATION: 'Your email address still needs to be verified before you can sign in.',
};

function mapLoginError(error) {
  const message = LOGIN_ERROR_MESSAGES[error?.data?.code];
  if (!message) return error;
  return new ApiError(message, error.status, error.data, error);
}

/**
 * Authenticates user credentials against POST /api/v1/auth/login.
 *
 * @param {string} email - User email address.
 * @param {string} password - User password.
 * @returns {Promise<{ token: string, user: { id: number, email: string, roles: string[] } }>}
 */
export async function loginApi(email, password) {
  const payload = {
    email: email ? email.trim() : '',
    password: password || '',
  };

  try {
    return await apiRequestData(
      '/v1/auth/login',
      {
        method: 'POST',
        auth: false,
        handleUnauthorized: false,
        body: JSON.stringify(payload),
      },
      {
        defaultMessage: 'Authentication request failed',
        statusMessages: { 401: 'Invalid email or password' },
      },
    );
  } catch (error) {
    throw mapLoginError(error);
  }
}

export function registerCitizen({ fullName, email, phoneNumber, password }) {
  return apiRequestData('/v1/auth/register/citizen', {
    method: 'POST',
    auth: false,
    handleUnauthorized: false,
    body: JSON.stringify({
      fullName: fullName.trim(),
      email: email.trim(),
      phoneNumber: phoneNumber.trim(),
      password,
    }),
  }, {
    defaultMessage: 'Failed to create the citizen account',
  });
}

export function registerStaff({ fullName, email, phoneNumber, password, requestedRoleCode }) {
  return apiRequestData('/v1/auth/register/staff', {
    method: 'POST',
    auth: false,
    handleUnauthorized: false,
    body: JSON.stringify({
      fullName: fullName.trim(),
      email: email.trim(),
      phoneNumber: phoneNumber.trim(),
      password,
      requestedRoleCode,
    }),
  }, {
    defaultMessage: 'Failed to submit the staff account request',
  });
}

/**
 * Fetches the authenticated user profile from GET /api/v1/auth/me.
 *
 * @returns {Promise<{ id: number, email: string, roles: string[] }>}
 */
export async function getCurrentUserApi() {
  return apiRequestData(
    '/v1/auth/me',
    {
      method: 'GET',
      auth: true,
      handleUnauthorized: false,
    },
    {
      defaultMessage: 'Failed to fetch current user profile',
      statusMessages: { 401: 'Authentication session expired or invalid' },
    },
  );
}

/**
 * Verifies email address using an action token via POST /api/v1/auth/verify-email.
 *
 * @param {string} token - The raw verification action token.
 * @returns {Promise<{ message: string }>}
 */
export function verifyEmail(token) {
  return apiRequestData(
    '/v1/auth/verify-email',
    {
      method: 'POST',
      auth: false,
      handleUnauthorized: false,
      body: JSON.stringify({ token: token ? token.trim() : '' }),
    },
    {
      defaultMessage: 'Failed to verify email address',
    },
  );
}

/**
 * Requests resending an email verification link via POST /api/v1/auth/resend-verification.
 *
 * @param {string} email - The user's registered email address.
 * @returns {Promise<{ message: string }>}
 */
export function resendVerification(email) {
  return apiRequestData(
    '/v1/auth/resend-verification',
    {
      method: 'POST',
      auth: false,
      handleUnauthorized: false,
      body: JSON.stringify({ email: email ? email.trim() : '' }),
    },
    {
      defaultMessage: 'Failed to resend verification email',
    },
  );
}

/**
 * Initiates password recovery via POST /api/v1/auth/forgot-password.
 *
 * @param {string} email - The user's account email address.
 * @returns {Promise<{ message: string }>}
 */
export function forgotPassword(email) {
  return apiRequestData(
    '/v1/auth/forgot-password',
    {
      method: 'POST',
      auth: false,
      handleUnauthorized: false,
      body: JSON.stringify({ email: email ? email.trim() : '' }),
    },
    {
      defaultMessage: 'Failed to submit forgot password request',
    },
  );
}

/**
 * Resets a password using an action token via POST /api/v1/auth/reset-password.
 *
 * @param {string} token - The raw reset action token.
 * @param {string} newPassword - The replacement password.
 * @returns {Promise<{ message: string }>}
 */
export function resetPassword(token, newPassword) {
  return apiRequestData(
    '/v1/auth/reset-password',
    {
      method: 'POST',
      auth: false,
      handleUnauthorized: false,
      body: JSON.stringify({
        token: token ? token.trim() : '',
        newPassword: newPassword || '',
      }),
    },
    {
      defaultMessage: 'Failed to reset password',
    },
  );
}
