/**
 * Authentication REST API Service.
 * Interfaces with backend /api/v1/auth endpoints.
 */

import { apiRequestData } from '../../services/apiClient';

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

  return apiRequestData(
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
