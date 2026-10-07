/**
 * API Client foundation for the Web-Based Vaccination Portal.
 * Configures the base URL for REST API communication, centralizes JWT
 * Bearer token injection from authStorage, and handles unauthorized session notifications.
 */

import {
  endAuthSession,
  getSessionGeneration,
  getSessionPrincipalId,
  getToken,
  installRefreshedSession,
  normalizePrincipalId,
  notifyUnauthorized,
  onUnauthorized,
} from '../auth/authStorage';

export function resolveApiBaseUrl(configuredBaseUrl) {
  return configuredBaseUrl || '/api';
}

const API_BASE_URL = resolveApiBaseUrl(import.meta.env.VITE_API_BASE_URL);
export const PERMISSION_DENIED_MESSAGE = 'You do not have permission to perform this action.';
export const NETWORK_ERROR_MESSAGE = 'Unable to reach the server. Please check your connection and try again.';

const REFRESH_ENDPOINT = '/v1/auth/refresh';

/**
 * Classifies whether an HTTP status or error condition represents a transient
 * infrastructure failure rather than definitive session invalidation.
 *
 * @param {number} status
 * @returns {boolean}
 */
export function isTransientRefreshStatus(status) {
  return status === 0 || (status >= 500 && status <= 599);
}

let refreshInFlight = null;
let refreshInFlightGeneration = null;

let rotatedTokensGeneration = 0;
const rotatedTokens = new Map();

function ensureRotatedTokensForCurrentGeneration() {
  const currentGen = getSessionGeneration();
  if (rotatedTokensGeneration !== currentGen) {
    rotatedTokens.clear();
    rotatedTokensGeneration = currentGen;
  }
  return rotatedTokens;
}

onUnauthorized(() => {
  rotatedTokens.clear();
  rotatedTokensGeneration = 0;
});

export function __resetRotatedTokensForTests() {
  rotatedTokens.clear();
  rotatedTokensGeneration = 0;
  refreshInFlight = null;
  refreshInFlightGeneration = null;
}

function recordTokenRotation(oldToken, newToken) {
  if (oldToken && newToken && oldToken !== newToken) {
    const map = ensureRotatedTokensForCurrentGeneration();
    if (map.size >= 20) {
      const oldestKey = map.keys().next().value;
      map.delete(oldestKey);
    }
    map.set(oldToken, newToken);
  }
}

function wasTokenRotatedTo(oldToken, targetToken) {
  const map = ensureRotatedTokensForCurrentGeneration();
  let cur = oldToken;
  let depth = 0;
  while (cur && map.has(cur) && depth < 10) {
    cur = map.get(cur);
    if (cur === targetToken) {
      return true;
    }
    depth++;
  }
  return false;
}

/**
 * Single-flight refresh-token rotation (canonical §17.1). The opaque refresh
 * token lives in an HttpOnly cookie, so this request only carries credentials —
 * the browser JS never sees or stores the refresh token itself. Concurrent
 * callers share one in-flight refresh promise for the same auth session; a
 * definitive failure clears the in-memory session and notifies unauthorized
 * listeners. Stale refreshes whose generation has been superseded (by login or
 * logout) are silently discarded without mutating the newer session.
 *
 * @returns {Promise<string|null>} The new in-memory access token, or null when
 * the session could not be refreshed. Transient failures reject with ApiError
 * so callers can distinguish them from a definitive session end.
 */
export function refreshSession() {
  return rotateSession('preserve-principal');
}

/** Only cold bootstrap may establish an identity from the browser-shared cookie. */
export function bootstrapSession() {
  return rotateSession('bootstrap');
}

function rotateSession(mode) {
  const currentGen = getSessionGeneration();
  const principalAtStart = getSessionPrincipalId();
  const previousToken = getToken();
  const mayEstablishIdentity = mode === 'bootstrap' && principalAtStart === null && !previousToken;

  // Automatic request recovery must never turn an unbound token into another account.
  if (principalAtStart === null && !mayEstablishIdentity) {
    endAuthSession();
    notifyUnauthorized();
    return Promise.resolve(null);
  }

  if (refreshInFlight && refreshInFlightGeneration === currentGen) {
    return refreshInFlight;
  }

  const generationAtStart = currentGen;

  refreshInFlightGeneration = generationAtStart;
  refreshInFlight = (async () => {
    let response;
    try {
      response = await fetch(`${API_BASE_URL}${REFRESH_ENDPOINT}`, {
        method: 'POST',
        credentials: 'include',
        headers: { Accept: 'application/json' },
      });
    } catch (error) {
      // Transport failure: if this session generation is still current, preserve
      // the session and propagate the retryable error. If generation was superseded,
      // drop the stale failure silently.
      if (generationAtStart === getSessionGeneration()) {
        throw new ApiError(NETWORK_ERROR_MESSAGE, 0, null, error);
      }
      return null;
    }

    // Transient server outage (5xx): preserve current session without clearing state.
    if (response.status >= 500 && response.status <= 599) {
      if (generationAtStart === getSessionGeneration()) {
        throw new ApiError('Authentication service is temporarily unavailable', response.status);
      }
      return null;
    }

    // Definitive auth failure (400, 401, 403, 423, etc.):
    if (!response.ok) {
      if (generationAtStart === getSessionGeneration()) {
        endAuthSession();
        notifyUnauthorized();
      }
      return null;
    }

    const data = await parseApiResponse(response, { defaultMessage: 'Session refresh failed' });
    const token = data?.token || null;
    const returnedPrincipalId = normalizePrincipalId(data?.user?.id);
    // Check generation before invalidation: a late response must never clear a newer login.
    if (generationAtStart !== getSessionGeneration()) {
      return null;
    }
    if (!token || returnedPrincipalId === null
      || (!mayEstablishIdentity && returnedPrincipalId !== principalAtStart)) {
      if (generationAtStart === getSessionGeneration()) {
        endAuthSession();
        notifyUnauthorized();
      }
      return null;
    }

    // Session generation remained current during rotation: commit state.
    if (generationAtStart === getSessionGeneration()) {
      recordTokenRotation(previousToken, token);
      installRefreshedSession(token, returnedPrincipalId);
      return token;
    }

    // Session generation changed during in-flight refresh (e.g. login or logout):
    // drop the returned token without mutating the active session or genealogy.
    return null;
  })().finally(() => {
    if (refreshInFlightGeneration === generationAtStart) {
      refreshInFlight = null;
      refreshInFlightGeneration = null;
    }
  });

  return refreshInFlight;
}

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

function isRefreshOrLogoutEndpoint(endpoint) {
  return endpoint === REFRESH_ENDPOINT || endpoint === '/v1/auth/logout';
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
    __authRetried = false,
    ...fetchOptions
  } = options;

  const defaultHeaders = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  };

  const requestGeneration = getSessionGeneration();
  let requestToken = null;
  if (auth) {
    const token = getToken();
    requestToken = token;
    if (token) {
      defaultHeaders['Authorization'] = `Bearer ${token}`;
    }
  }

  const config = {
    ...fetchOptions,
    credentials: 'include',
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
    // Tie unauthorized handling to the authentication identity and session generation (R07 / NEW-F1 / F-024-1):
    // A. If already retried: never retry again. Notify only if session and token are still current.
    if (__authRetried) {
      if (requestGeneration === getSessionGeneration() && getToken() === requestToken) {
        notifyUnauthorized();
      }
      return response;
    }

    // B. If requestGeneration !== currentSessionGeneration:
    // The request belongs to an obsolete session. Do NOT refresh, do NOT replay,
    // do NOT clear or notify against the newer session; return the stale response immediately.
    if (requestGeneration !== getSessionGeneration()) {
      return response;
    }

    const canRefresh = auth && requestToken && !isRefreshOrLogoutEndpoint(endpoint);
    if (!canRefresh) {
      if (getToken() === requestToken) {
        notifyUnauthorized();
      }
      return response;
    }

    const currentToken = getToken();
    const tokenStillCurrent = currentToken === requestToken;

    if (tokenStillCurrent) {
      // The access token is short-lived: attempt exactly one single-flight refresh
      // and replay the original request with the renewed token (§17.1).
      let refreshedToken = null;
      let refreshHitTransientFailure = false;
      try {
        refreshedToken = await refreshSession();
      } catch (error) {
        refreshHitTransientFailure = error instanceof ApiError && isTransientRefreshStatus(error.status);
      }

      // D. POST-AWAIT RECHECK (F-024-1):
      // An authenticated request may only retry/replay inside the same logical
      // authentication session generation in which it was originally issued.
      // If the session generation changed during the await (e.g. login B or logout),
      // abort replay immediately without touching or replaying under the newer session.
      if (requestGeneration !== getSessionGeneration()) {
        return response;
      }

      if (refreshedToken) {
        return apiRequest(endpoint, { ...options, __authRetried: true });
      }
      if (!refreshHitTransientFailure) {
        // refreshSession already cleared memory state and notified unauthorized
        // listeners for a definitive session end; return the original 401 response.
        return response;
      }
      // Transient failure during refresh: the token/session remains valid in memory.
      // Do not clear the session; surface the original 401 to the caller.
      return response;
    }

    if (!tokenStillCurrent && currentToken && wasTokenRotatedTo(requestToken, currentToken)) {
      // Finding F-02: Concurrent in-flight request was dispatched with stale token T1
      // within the SAME session. Another concurrent request already encountered 401,
      // rotated the token to T2, and installed it into memory. Replay immediately ONCE
      // with the already-refreshed currentToken T2.
      if (refreshInFlight && refreshInFlightGeneration === requestGeneration) {
        try {
          await refreshInFlight;
        } catch {
          // ignore transient errors
        }
      }

      // D. POST-AWAIT RECHECK (F-024-1):
      // Recheck session generation after awaiting shared refreshInFlight.
      if (requestGeneration !== getSessionGeneration()) {
        return response;
      }

      const postWaitToken = getToken();
      if (postWaitToken && wasTokenRotatedTo(requestToken, postWaitToken)) {
        return apiRequest(endpoint, { ...options, __authRetried: true });
      }
      return response;
    }

    if (tokenStillCurrent && requestGeneration === getSessionGeneration()) {
      notifyUnauthorized();
    }
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
