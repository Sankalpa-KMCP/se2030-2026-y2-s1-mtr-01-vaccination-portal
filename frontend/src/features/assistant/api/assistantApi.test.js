import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  getAssistantStatus,
  sendAssistantMessage,
  isSafeInternalPath,
  sanitizeCurrentPath,
  MAX_MESSAGE_LENGTH,
} from './assistantApi';
import { setToken, clearToken } from '../../../shared/auth/authStorage';

function mockResponse(body, status = 200) {
  return {
    ok: status >= 200 && status < 300,
    status,
    statusText: status === 200 ? 'OK' : 'Error',
    headers: { get: () => 'application/json' },
    text: vi.fn().mockResolvedValue(body ? JSON.stringify(body) : ''),
  };
}

describe('assistantApi', () => {
  beforeEach(() => {
    window.sessionStorage.clear();
    vi.stubGlobal('fetch', vi.fn());
  });

  afterEach(() => {
    clearToken();
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  describe('isSafeInternalPath & sanitizeCurrentPath', () => {
    it('accepts safe internal paths', () => {
      expect(isSafeInternalPath('/')).toBe(true);
      expect(isSafeInternalPath('/how-it-works')).toBe(true);
      expect(isSafeInternalPath('/app/appointments/book')).toBe(true);
      expect(isSafeInternalPath('/register/citizen')).toBe(true);
    });

    it('rejects external URLs, protocol-relative, encoded, and dangerous paths', () => {
      expect(isSafeInternalPath('https://example.com')).toBe(false);
      expect(isSafeInternalPath('http://malicious.org')).toBe(false);
      expect(isSafeInternalPath('//google.com')).toBe(false);
      expect(isSafeInternalPath('javascript:alert(1)')).toBe(false);
      expect(isSafeInternalPath('/app/../etc/passwd')).toBe(false);
      expect(isSafeInternalPath('/app\\admin')).toBe(false);
      expect(isSafeInternalPath('/app/%2e%2e/admin')).toBe(false);
      expect(isSafeInternalPath('/app/%2F%2Fevil')).toBe(false);
      expect(isSafeInternalPath('/app/<script>')).toBe(false);
      expect(isSafeInternalPath('/app/"quoted"')).toBe(false);
      expect(isSafeInternalPath("/app/'quoted'")).toBe(false);
      expect(isSafeInternalPath('')).toBe(false);
      expect(isSafeInternalPath(null)).toBe(false);
      expect(isSafeInternalPath(123)).toBe(false);
    });

    it('sanitizes current path by stripping query params and fragments', () => {
      expect(sanitizeCurrentPath('/app/citizen?token=secret')).toBe('/app/citizen');
      expect(sanitizeCurrentPath('/about#section-1')).toBe('/about');
      expect(sanitizeCurrentPath('https://evil.com/phish')).toBe('/');
      expect(sanitizeCurrentPath('')).toBe('/');
      expect(sanitizeCurrentPath(null)).toBe('/');
    });
  });

  describe('getAssistantStatus', () => {
    it('calls GET /api/v1/assistant/status via apiClient', async () => {
      const mockStatus = {
        available: true,
        personalizedFeaturesAvailable: false,
        disclaimer: 'Informational only.',
      };
      fetch.mockResolvedValueOnce(mockResponse(mockStatus));

      const result = await getAssistantStatus();
      expect(result).toEqual(mockStatus);

      expect(fetch).toHaveBeenCalledWith(
        '/api/v1/assistant/status',
        expect.objectContaining({
          method: 'GET',
          headers: expect.objectContaining({
            Accept: 'application/json',
          }),
        })
      );
    });
  });

  describe('sendAssistantMessage', () => {
    it('rejects blank messages or whitespace-only messages', async () => {
      await expect(sendAssistantMessage({ message: '' })).rejects.toThrow('Message cannot be blank.');
      await expect(sendAssistantMessage({ message: '   \n  ' })).rejects.toThrow('Message cannot be blank.');
      await expect(sendAssistantMessage({ message: null })).rejects.toThrow('Message cannot be blank.');
    });

    it('rejects messages exceeding MAX_MESSAGE_LENGTH', async () => {
      const tooLong = 'a'.repeat(MAX_MESSAGE_LENGTH + 1);
      await expect(sendAssistantMessage({ message: tooLong })).rejects.toThrow(/exceeds maximum length/);
    });

    it('submits valid message with trimmed text, sanitized history, and sanitized currentPath', async () => {
      const mockResult = {
        answer: 'You can book an appointment at /app/appointments/book.',
        suggestedActions: [{ label: 'Book Appointment', path: '/app/appointments/book' }],
      };
      fetch.mockResolvedValueOnce(mockResponse(mockResult));

      const history = [
        { role: 'user', content: 'Hello' },
        { role: 'assistant', content: 'Hi, how can I help?' },
      ];

      const response = await sendAssistantMessage({
        message: '  How do I book?  ',
        history,
        currentPath: '/app/citizen/profile?ref=nav#top',
      });

      expect(response).toEqual(mockResult);
      expect(fetch).toHaveBeenCalledWith(
        '/api/v1/assistant/messages',
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify({
            message: 'How do I book?',
            history: [
              { role: 'user', content: 'Hello' },
              { role: 'assistant', content: 'Hi, how can I help?' },
            ],
            currentPath: '/app/citizen/profile',
          }),
        })
      );
    });

    it('caps history to the latest 10 valid turns', async () => {
      fetch.mockResolvedValueOnce(mockResponse({ answer: 'Response' }));

      // 15 history items
      const history = Array.from({ length: 15 }, (_, i) => ({
        role: i % 2 === 0 ? 'user' : 'assistant',
        content: `Message ${i}`,
      }));

      await sendAssistantMessage({
        message: 'Query',
        history,
        currentPath: '/home',
      });

      const callArgs = fetch.mock.calls[0];
      const parsedBody = JSON.parse(callArgs[1].body);

      expect(parsedBody.history.length).toBe(10);
      expect(parsedBody.history[0].content).toBe('Message 5');
      expect(parsedBody.history[9].content).toBe('Message 14');
    });

    it('delegates JWT attachment to apiClient when a token is held in memory', async () => {
      setToken('mock-jwt-token-xyz');
      fetch.mockResolvedValueOnce(mockResponse({ answer: 'Authenticated answer' }));

      await sendAssistantMessage({ message: 'Hello' });

      expect(fetch).toHaveBeenCalledWith(
        '/api/v1/assistant/messages',
        expect.objectContaining({
          headers: expect.objectContaining({
            Authorization: 'Bearer mock-jwt-token-xyz',
          }),
        })
      );
    });
  });
});
