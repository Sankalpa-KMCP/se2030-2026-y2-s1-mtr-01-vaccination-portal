/**
 * Authentication Storage Foundation.
 * Holds the short-lived access token in module memory only (canonical §17.1):
 * the refresh token lives in an HttpOnly cookie that browser JS can never read,
 * so nothing authentication-related is persisted to sessionStorage/localStorage.
 * Decoupled unauthorized session notification events are also managed here.
 */

let memoryToken = null;
let sessionPrincipalId = null;
let currentSessionGeneration = 1;
const unauthorizedListeners = new Set();

/**
 * Returns the monotonically increasing in-memory session generation epoch.
 * Used to ensure that asynchronous operations (such as in-flight refreshes)
 * only mutate state if their originating session is still current (canonical §17.1).
 *
 * @returns {number}
 */
export function getSessionGeneration() {
  return currentSessionGeneration;
}

/**
 * Advances the session generation epoch and returns the new value.
 * Invalidates any in-flight refresh or auth mutations from prior epochs.
 *
 * @returns {number}
 */
export function nextSessionGeneration() {
  currentSessionGeneration += 1;
  return currentSessionGeneration;
}

/** Canonical representation of the stable account ID supplied by the server. */
export function normalizePrincipalId(principalId) {
  return principalId == null || String(principalId).trim() === '' ? null : String(principalId);
}

export function getSessionPrincipalId() {
  return sessionPrincipalId;
}

/** Installs a verified refresh identity without advancing its originating generation. */
export function installRefreshedSession(token, principalId) {
  sessionPrincipalId = normalizePrincipalId(principalId);
  setToken(token);
}

/** Starts a new login generation bound to the server-provided account ID. */
export function startAuthSession(token, principalId) {
  if (!token || normalizePrincipalId(principalId) === null) {
    throw new Error('Authentication response was incomplete');
  }
  nextSessionGeneration();
  installRefreshedSession(token, principalId);
  return currentSessionGeneration;
}

/**
 * Explicitly terminates the current authentication session (e.g. logout / definitive invalidation).
 * Advances the generation epoch and drops the in-memory access token.
 *
 * @returns {number} The new session generation epoch
 */
export function endAuthSession() {
  nextSessionGeneration();
  clearToken();
  return currentSessionGeneration;
}

/**
 * Retrieves the in-memory access token.
 * @returns {string|null} The access token or null when absent.
 */
export function getToken() {
  return memoryToken;
}

/**
 * Keeps the access token in memory for the lifetime of the page.
 * @param {string} token - JWT access token string.
 * @returns {boolean} True (memory persistence cannot fail).
 */
export function setToken(token) {
  memoryToken = token || null;
  return Boolean(memoryToken);
}

/**
 * Drops the in-memory access token.
 */
export function clearToken() {
  memoryToken = null;
  sessionPrincipalId = null;
}

/**
 * Test helper to reset module state between test cases.
 */
export function __resetAuthStorageForTests() {
  memoryToken = null;
  sessionPrincipalId = null;
  currentSessionGeneration = 1;
  unauthorizedListeners.clear();
}

/**
 * Registers a listener for unauthorized session events (e.g. a 401 that a
 * refresh attempt could not recover from).
 * @param {Function} listener - Callback function invoked on unauthorized session.
 * @returns {Function} Unsubscribe function to remove the listener.
 */
export function onUnauthorized(listener) {
  if (typeof listener === 'function') {
    unauthorizedListeners.add(listener);
    return () => {
      unauthorizedListeners.delete(listener);
    };
  }
  return () => {};
}

/**
 * Notifies all registered listeners that the current session is unauthorized.
 */
export function notifyUnauthorized() {
  unauthorizedListeners.forEach((listener) => {
    try {
      listener();
    } catch {
      // Ignore listener error
    }
  });
}
