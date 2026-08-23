/**
 * API Client foundation for the Web-Based Vaccination Portal.
 * Configures the base URL for REST API communication, centralizes JWT
 * Bearer token injection from authStorage, and handles unauthorized session notifications.
 */

import { getToken, notifyUnauthorized } from '../auth/authStorage';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

/**
 * Standard API error structure preserving HTTP status and response payload.
 */
export class ApiError extends Error {
  constructor(message, status, data = null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.data = data;
  }
}

/**
 * Reusable request helper using native fetch API.
 *
 * @param {string} endpoint - Relative API endpoint path (e.g. '/v1/campaigns')
 * @param {RequestInit & { auth?: boolean, handleUnauthorized?: boolean }} [options] - Fetch request configuration options
 * @returns {Promise<Response>} Native fetch response promise
 */
export async function apiRequest(endpoint, options = {}) {
  const url = `${API_BASE_URL}${endpoint.startsWith('/') ? '' : '/'}${endpoint}`;

  const {
    auth = true,
    handleUnauthorized = true,
    headers = {},
    ...fetchOptions
  } = options;

  const defaultHeaders = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  };

  if (auth) {
    const token = getToken();
    if (token) {
      defaultHeaders['Authorization'] = `Bearer ${token}`;
    }
  }

  const config = {
    ...fetchOptions,
    headers: {
      ...defaultHeaders,
      ...headers,
    },
  };

  const response = await fetch(url, config);

  if (response.status === 401 && handleUnauthorized !== false) {
    notifyUnauthorized();
  }

  return response;
}

export { API_BASE_URL };
