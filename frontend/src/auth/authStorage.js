/**
 * Authentication Storage Foundation.
 * Manages sessionStorage persistence for the JWT access token and decoupled
 * unauthorized session notification events.
 */

const TOKEN_KEY = 'vaccination_portal_token';
const unauthorizedListeners = new Set();

/**
 * Safely retrieves the stored access token from sessionStorage.
 * @returns {string|null} The access token or null if absent/inaccessible.
 */
export function getToken() {
  try {
    if (typeof window !== 'undefined' && window.sessionStorage) {
      return window.sessionStorage.getItem(TOKEN_KEY);
    }
  } catch {
    // Storage access restricted or disabled
  }
  return null;
}

/**
 * Safely persists the access token to sessionStorage.
 * @param {string} token - JWT access token string.
 * @returns {boolean} True if successfully stored, false on failure.
 */
export function setToken(token) {
  try {
    if (typeof window !== 'undefined' && window.sessionStorage && token) {
      window.sessionStorage.setItem(TOKEN_KEY, token);
      return true;
    }
  } catch {
    // Storage access restricted or disabled
  }
  return false;
}

/**
 * Safely removes the access token from sessionStorage.
 */
export function clearToken() {
  try {
    if (typeof window !== 'undefined' && window.sessionStorage) {
      window.sessionStorage.removeItem(TOKEN_KEY);
    }
  } catch {
    // Storage access restricted or disabled
  }
}

/**
 * Registers a listener for unauthorized session events (e.g. 401 on protected requests).
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
