import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  ApiError,
  API_BASE_URL,
  NETWORK_ERROR_MESSAGE,
  PERMISSION_DENIED_MESSAGE,
  apiRequest,
  apiRequestData,
  parseApiResponse,
  resolveApiBaseUrl,
} from './apiClient';
import {
  clearToken,
  getToken,
  onUnauthorized,
  setToken,
} from '../auth/authStorage';

function response({
  body = '',
  status = 200,
  statusText = 'OK',
  contentType = 'application/json',
} = {}) {
  const text = vi.fn().mockResolvedValue(body);
  return {
    ok: status >= 200 && status < 300,
    status,
    statusText,
    headers: {
      get: vi.fn((name) => name.toLowerCase() === 'content-type' ? contentType : null),
    },
    text,
  };
}

describe('shared API response and error handling', () => {
  beforeEach(() => {
    window.sessionStorage.clear();
    vi.stubGlobal('fetch', vi.fn());
  });

  afterEach(() => {
    clearToken();
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it('uses the same-origin /api path by default', () => {
    expect(resolveApiBaseUrl()).toBe('/api');
    expect(API_BASE_URL).toBe('/api');
  });

  it('preserves a configured VITE_API_BASE_URL override', () => {
    expect(resolveApiBaseUrl('https://api.example.gov/api')).toBe('https://api.example.gov/api');
  });

  it('parses a successful JSON response', async () => {
    const result = await parseApiResponse(response({ body: '{"id":7,"name":"Clinic"}' }));

    expect(result).toEqual({ id: 7, name: 'Clinic' });
  });

  it('returns null for 204 No Content without reading the body', async () => {
    const noContentResponse = response({ status: 204, statusText: 'No Content' });

    await expect(parseApiResponse(noContentResponse)).resolves.toBeNull();
    expect(noContentResponse.text).not.toHaveBeenCalled();
  });

  it('returns null for a successful response with an empty body', async () => {
    await expect(parseApiResponse(response({ body: '  ' }))).resolves.toBeNull();
  });

  it('preserves a structured backend JSON error', async () => {
    const errorData = {
      status: 400,
      error: 'VALIDATION_FAILED',
      message: 'The request is invalid',
      fieldErrors: [{ field: 'name', message: 'Name is required' }],
    };

    await expect(parseApiResponse(response({
      body: JSON.stringify(errorData),
      status: 400,
      statusText: 'Bad Request',
    }))).rejects.toMatchObject({
      name: 'ApiError',
      status: 400,
      message: 'The request is invalid',
      data: errorData,
    });
  });

  it('preserves a readable plain-text error', async () => {
    await expect(parseApiResponse(response({
      body: 'Service temporarily unavailable',
      status: 503,
      statusText: 'Service Unavailable',
      contentType: 'text/plain',
    }))).rejects.toMatchObject({
      status: 503,
      message: 'Service temporarily unavailable',
      data: 'Service temporarily unavailable',
    });
  });

  it('wraps a network failure in a consistent ApiError', async () => {
    const networkFailure = new TypeError('Failed to fetch');
    fetch.mockRejectedValueOnce(networkFailure);

    await expect(apiRequestData('/v1/health')).rejects.toMatchObject({
      name: 'ApiError',
      status: 0,
      message: NETWORK_ERROR_MESSAGE,
      cause: networkFailure,
    });
  });

  it('triggers the existing unauthorized session path on 401', async () => {
    setToken('expired-token');
    const listener = vi.fn(() => clearToken());
    const unsubscribe = onUnauthorized(listener);
    fetch.mockResolvedValueOnce(response({
      body: '{"message":"Unauthorized"}',
      status: 401,
      statusText: 'Unauthorized',
    }));

    await expect(apiRequestData('/v1/protected')).rejects.toBeInstanceOf(ApiError);

    expect(listener).toHaveBeenCalledTimes(1);
    expect(getToken()).toBeNull();
    unsubscribe();
  });

  it('does not clear a newer session when a stale request finishes with 401 (R07)', async () => {
    // Request starts under session A...
    setToken('token-a');
    const listener = vi.fn(() => clearToken());
    const unsubscribe = onUnauthorized(listener);

    let resolveStale;
    fetch.mockImplementationOnce(() => new Promise((resolve) => { resolveStale = resolve; }));
    const staleRequest = apiRequestData('/v1/protected');

    // ...and the user logs in as B while the request is in flight
    setToken('token-b');

    resolveStale(response({
      body: '{"message":"Unauthorized"}',
      status: 401,
      statusText: 'Unauthorized',
    }));
    await expect(staleRequest).rejects.toBeInstanceOf(ApiError);

    expect(listener).not.toHaveBeenCalled();
    expect(getToken()).toBe('token-b');
    unsubscribe();
  });

  it('still invokes normal unauthorized handling for a current-session 401 (R07)', async () => {
    setToken('token-b');
    const listener = vi.fn(() => clearToken());
    const unsubscribe = onUnauthorized(listener);
    fetch.mockResolvedValueOnce(response({
      body: '{"message":"Unauthorized"}',
      status: 401,
      statusText: 'Unauthorized',
    }));

    await expect(apiRequestData('/v1/protected')).rejects.toBeInstanceOf(ApiError);

    expect(listener).toHaveBeenCalledTimes(1);
    expect(getToken()).toBeNull();
    unsubscribe();
  });

  it('does not notify again when a request finishes with 401 after the user already logged out (R07)', async () => {
    setToken('token-a');
    const listener = vi.fn(() => clearToken());
    const unsubscribe = onUnauthorized(listener);

    let resolveStale;
    fetch.mockImplementationOnce(() => new Promise((resolve) => { resolveStale = resolve; }));
    const staleRequest = apiRequestData('/v1/protected');

    // Explicit logout happens while the request is in flight
    clearToken();

    resolveStale(response({
      body: '{"message":"Unauthorized"}',
      status: 401,
      statusText: 'Unauthorized',
    }));
    await expect(staleRequest).rejects.toBeInstanceOf(ApiError);

    expect(listener).not.toHaveBeenCalled();
    expect(getToken()).toBeNull();
    unsubscribe();
  });

  it('keeps anonymous 401 behavior intact when no token was ever stored (R07)', async () => {
    const listener = vi.fn(() => clearToken());
    const unsubscribe = onUnauthorized(listener);
    fetch.mockResolvedValueOnce(response({
      body: '{"message":"Unauthorized"}',
      status: 401,
      statusText: 'Unauthorized',
    }));

    await expect(apiRequestData('/v1/protected')).rejects.toBeInstanceOf(ApiError);

    expect(listener).toHaveBeenCalledTimes(1);
    unsubscribe();
  });

  it('concurrent stale 401s never disturb a newer login (R07)', async () => {
    setToken('token-a');
    const listener = vi.fn(() => clearToken());
    const unsubscribe = onUnauthorized(listener);

    let resolveFirst;
    let resolveSecond;
    fetch.mockImplementationOnce(() => new Promise((resolve) => { resolveFirst = resolve; }));
    fetch.mockImplementationOnce(() => new Promise((resolve) => { resolveSecond = resolve; }));

    const first = apiRequestData('/v1/protected');
    const second = apiRequestData('/v1/profile');

    // Login as B while both A-session requests are in flight
    setToken('token-b');

    const unauthorized = response({
      body: '{"message":"Unauthorized"}',
      status: 401,
      statusText: 'Unauthorized',
    });
    resolveFirst(unauthorized);
    resolveSecond(unauthorized);

    await expect(first).rejects.toBeInstanceOf(ApiError);
    await expect(second).rejects.toBeInstanceOf(ApiError);

    expect(listener).not.toHaveBeenCalled();
    expect(getToken()).toBe('token-b');
    unsubscribe();
  });

  it('does not clear or report the authenticated session as unauthorized on 403', async () => {
    setToken('valid-token');
    const listener = vi.fn();
    const unsubscribe = onUnauthorized(listener);
    fetch.mockResolvedValueOnce(response({
      body: '{"message":"Forbidden"}',
      status: 403,
      statusText: 'Forbidden',
    }));

    await expect(apiRequestData('/v1/admin-only')).rejects.toBeInstanceOf(ApiError);

    expect(listener).not.toHaveBeenCalled();
    expect(getToken()).toBe('valid-token');
    unsubscribe();
  });

  it('uses clear permission-denied information for 403 responses', async () => {
    await expect(parseApiResponse(response({
      body: '{"message":"Forbidden"}',
      status: 403,
      statusText: 'Forbidden',
    }))).rejects.toMatchObject({
      status: 403,
      message: PERMISSION_DENIED_MESSAGE,
      data: { message: 'Forbidden' },
    });
  });

  it('continues attaching a Bearer token to authenticated requests', async () => {
    setToken('signed-jwt');
    fetch.mockResolvedValueOnce(response({ body: '{"ok":true}' }));

    await apiRequestData('/v1/items?status=ACTIVE', { method: 'GET' });

    expect(fetch).toHaveBeenCalledWith(
      '/api/v1/items?status=ACTIVE',
      expect.objectContaining({
        method: 'GET',
        headers: expect.objectContaining({ Authorization: 'Bearer signed-jwt' }),
      }),
    );
  });

  it('supports public requests without attaching an Authorization header', async () => {
    setToken('signed-jwt');
    fetch.mockResolvedValueOnce(response({ body: '{"public":true}' }));

    await apiRequestData('/v1/public', { method: 'GET', auth: false });

    const [, config] = fetch.mock.calls[0];
    expect(config.headers.Authorization).toBeUndefined();
  });

  it.each([
    ['POST', '/v1/items', '{"name":"New item"}'],
    ['PUT', '/v1/items/7', '{"name":"Updated item"}'],
  ])('supports %s requests with JSON bodies', async (method, endpoint, body) => {
    fetch.mockResolvedValueOnce(response({ body: '{"id":7}' }));

    await apiRequestData(endpoint, { method, body });

    expect(fetch).toHaveBeenCalledWith(
      `/api${endpoint}`,
      expect.objectContaining({ method, body }),
    );
  });

  it('supports DELETE requests and centrally parses a 204 response', async () => {
    fetch.mockResolvedValueOnce(response({ status: 204, statusText: 'No Content' }));

    await expect(apiRequestData('/v1/items/7', { method: 'DELETE' })).resolves.toBeNull();
    expect(fetch).toHaveBeenCalledWith(
      '/api/v1/items/7',
      expect.objectContaining({ method: 'DELETE' }),
    );
  });

  it('preserves the raw-response apiRequest contract for existing feature modules', async () => {
    const rawResponse = response({ body: '{"id":7}' });
    fetch.mockResolvedValueOnce(rawResponse);

    await expect(apiRequest('/v1/items/7')).resolves.toBe(rawResponse);
  });
});
