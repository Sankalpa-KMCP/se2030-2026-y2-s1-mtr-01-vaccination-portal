/**
 * API Client foundation for the Web-Based Vaccination Portal.
 * Configures the base URL for REST API communication.
 */

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

/**
 * Reusable request helper using native fetch API.
 *
 * @param {string} endpoint - Relative API endpoint path (e.g. '/health')
 * @param {RequestInit} [options] - Fetch request configuration options
 * @returns {Promise<Response>} Native fetch response promise
 */
export async function apiRequest(endpoint, options = {}) {
  const url = `${API_BASE_URL}${endpoint.startsWith('/') ? '' : '/'}${endpoint}`;

  const defaultHeaders = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  };

  const config = {
    ...options,
    headers: {
      ...defaultHeaders,
      ...options.headers,
    },
  };

  return fetch(url, config);
}

export { API_BASE_URL };
