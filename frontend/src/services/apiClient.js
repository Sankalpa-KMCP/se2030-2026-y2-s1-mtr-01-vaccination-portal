/**
 * API Client foundation for the Web-Based Vaccination Portal.
 * Configures the base URL for REST API communication, centralizes JWT
 * Bearer token injection from authStorage, and handles unauthorized session notifications.
 */

import { getToken, notifyUnauthorized } from '../auth/authStorage';

export function resolveApiBaseUrl(configuredBaseUrl) {
  return configuredBaseUrl || '/api';
}

const API_BASE_URL = resolveApiBaseUrl(import.meta.env.VITE_API_BASE_URL);
export const PERMISSION_DENIED_MESSAGE = 'You do not have permission to perform this action.';
export const NETWORK_ERROR_MESSAGE = 'Unable to reach the server. Please check your connection and try again.';

const DEFAULT_ERROR_MESSAGES = {
  400: 'The request could not be completed. Please check the submitted information.',
  401: 'Your session has expired. Please log in again.',
  403: PERMISSION_DENIED_MESSAGE,
  404: 'The requested resource could not be found.',
  409: 'The request conflicts with the current state of the resource.',
};

/**
 * Standard API error structure preserving HTTP status and response payload.
 */
export class ApiError extends Error {
  constructor(message, status, data = null, cause = null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.data = data;
    if (cause) {
      this.cause = cause;
    }
  }
}

function parseResponseText(response, text) {
  const trimmedText = text.trim();
  if (!trimmedText) {
    return null;
  }

  const contentType = response.headers?.get?.('content-type') || '';
  const looksLikeJson = contentType.toLowerCase().includes('json')
    || trimmedText.startsWith('{')
    || trimmedText.startsWith('[');

  if (looksLikeJson) {
    try {
      return JSON.parse(trimmedText);
    } catch {
      // Preserve malformed or mislabeled response text as a readable message.
    }
  }

  return trimmedText;
}

function getPayloadMessage(data) {
  if (typeof data === 'string') {
    return data;
  }

  return data?.message || data?.error || null;
}

function getErrorMessage(response, data, options) {
  if (response.status === 403) {
    return PERMISSION_DENIED_MESSAGE;
  }

  const configuredStatusMessage = options.statusMessages?.[response.status];
  if (configuredStatusMessage) {
    return configuredStatusMessage;
  }

  return getPayloadMessage(data)
    || options.defaultMessage
    || DEFAULT_ERROR_MESSAGES[response.status]
    || (response.statusText
      ? `Request failed with status ${response.status}: ${response.statusText}`
      : `Request failed with status ${response.status}`);
}

/**
 * Parses a fetch response into data or a consistent ApiError.
 * Empty successful responses always resolve to null.
 *
 * @param {Response} response - Native fetch response
 * @param {{ defaultMessage?: string, statusMessages?: Record<number, string> }} [options]
 * @returns {Promise<unknown>} Parsed JSON, response text, or null
 */
export async function parseApiResponse(response, options = {}) {
  let data = null;

  if (response.status !== 204 && response.status !== 205) {
    const text = await response.text();
    data = parseResponseText(response, text);
  }

  if (!response.ok) {
    throw new ApiError(
      getErrorMessage(response, data, options),
      response.status,
      data,
    );
  }

  return data;
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

  let response;
  try {
    response = await fetch(url, config);
  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }
    throw new ApiError(NETWORK_ERROR_MESSAGE, 0, null, error);
  }

  if (response.status === 401 && handleUnauthorized !== false) {
    notifyUnauthorized();
  }

  return response;
}

/**
 * Makes an API request and returns its centrally parsed response data.
 * This is a convenience wrapper around the existing apiRequest client.
 *
 * @param {string} endpoint - Relative API endpoint path
 * @param {RequestInit & { auth?: boolean, handleUnauthorized?: boolean }} [options]
 * @param {{ defaultMessage?: string, statusMessages?: Record<number, string> }} [parseOptions]
 * @returns {Promise<unknown>} Parsed JSON, response text, or null
 */
export async function apiRequestData(endpoint, options = {}, parseOptions = {}) {
  const response = await apiRequest(endpoint, options);
  return parseApiResponse(response, parseOptions);
}

export { API_BASE_URL };
