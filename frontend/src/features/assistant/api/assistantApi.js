/**
 * Assistant API Service.
 * Interfaces with the backend AI Assistant endpoints:
 * - GET /api/v1/assistant/status
 * - POST /api/v1/assistant/messages
 *
 * Employs apiClient for automatic base URL resolution and JWT attachment.
 * Does not expose or require any third-party AI keys or models in the frontend.
 */

import { apiRequestData } from '../../../services/apiClient';

export const MAX_MESSAGE_LENGTH = 1500;
export const MAX_HISTORY_LENGTH = 10;

/**
 * Checks whether a suggested action path is a safe, relative internal route.
 * Rejects external URLs, protocol-relative paths, backslashes, colons, traversal, or control characters.
 *
 * @param {string} path - Target route path
 * @returns {boolean} True if the path is safe for internal client-side navigation
 */
export function isSafeInternalPath(path) {
  if (typeof path !== 'string' || !path.trim()) {
    return false;
  }
  const trimmed = path.trim();

  // Must begin with a single slash and not double slash (protocol-relative)
  if (!trimmed.startsWith('/') || trimmed.startsWith('//')) {
    return false;
  }

  // Reject URLs with schemes/protocols, backslashes, path traversal, control chars
  if (
    trimmed.includes(':') ||
    trimmed.includes('\\') ||
    trimmed.includes('..') ||
    Array.from(trimmed).some((ch) => {
      const code = ch.charCodeAt(0);
      return code < 32 || code === 127;
    })
  ) {
    return false;
  }

  return true;
}

/**
 * Cleans and sanitizes the current pathname to guarantee it contains no query parameters,
 * hash fragments, or traversal sequences.
 *
 * @param {string} pathname - Raw location pathname
 * @returns {string} Sanitized path
 */
export function sanitizeCurrentPath(pathname) {
  if (typeof pathname !== 'string' || !pathname.trim()) {
    return '/';
  }
  const clean = pathname.split('?')[0].split('#')[0].trim();
  return isSafeInternalPath(clean) ? clean : '/';
}

/**
 * Fetches the assistant operational status and system disclaimer.
 *
 * @returns {Promise<{ available: boolean, disclaimer: string, model?: string }>}
 */
export async function getAssistantStatus() {
  return apiRequestData('/v1/assistant/status', {
    method: 'GET',
  });
}

/**
 * Sends a message turn to the assistant.
 *
 * @param {Object} params
 * @param {string} params.message - Current user query (max 1500 characters)
 * @param {Array<{ role: string, content: string }>} [params.history=[]] - Prior conversation turns (max 10)
 * @param {string} [params.currentPath='/'] - Current client route for contextual awareness
 * @returns {Promise<{ answer: string, suggestedActions: Array<{ label: string, path: string }> }>}
 */
export async function sendAssistantMessage({ message, history = [], currentPath = '/' }) {
  if (typeof message !== 'string' || !message.trim()) {
    throw new Error('Message cannot be blank.');
  }

  const trimmedMessage = message.trim();
  if (trimmedMessage.length > MAX_MESSAGE_LENGTH) {
    throw new Error(`Message exceeds maximum length of ${MAX_MESSAGE_LENGTH} characters.`);
  }

  // Sanitize prior history turns: cap at latest 10, ensure correct role and string content
  const sanitizedHistory = (Array.isArray(history) ? history : [])
    .slice(-MAX_HISTORY_LENGTH)
    .filter(
      (entry) =>
        entry &&
        (entry.role === 'user' || entry.role === 'assistant') &&
        typeof entry.content === 'string' &&
        entry.content.trim()
    )
    .map((entry) => ({
      role: entry.role,
      content: entry.content.trim(),
    }));

  const payload = {
    message: trimmedMessage,
    history: sanitizedHistory,
    currentPath: sanitizeCurrentPath(currentPath),
  };

  return apiRequestData('/v1/assistant/messages', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}
