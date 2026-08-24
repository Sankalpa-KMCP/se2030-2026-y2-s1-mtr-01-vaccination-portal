/**
 * Authentication REST API Service.
 * Interfaces with backend /api/v1/auth endpoints.
 */

import { apiRequest, ApiError } from '../../services/apiClient';

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

  const response = await apiRequest('/v1/auth/login', {
    method: 'POST',
    auth: false,
    handleUnauthorized: false,
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => null);
    const message =
      response.status === 401
        ? 'Invalid email or password'
        : errorData?.message || 'Authentication request failed';
    throw new ApiError(message, response.status, errorData);
  }

  return response.json();
}

/**
 * Fetches the authenticated user profile from GET /api/v1/auth/me.
 *
 * @returns {Promise<{ id: number, email: string, roles: string[] }>}
 */
export async function getCurrentUserApi() {
  const response = await apiRequest('/v1/auth/me', {
    method: 'GET',
    auth: true,
    handleUnauthorized: false,
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => null);
    const message =
      response.status === 401
        ? 'Authentication session expired or invalid'
        : errorData?.message || 'Failed to fetch current user profile';
    throw new ApiError(message, response.status, errorData);
  }

  return response.json();
}
