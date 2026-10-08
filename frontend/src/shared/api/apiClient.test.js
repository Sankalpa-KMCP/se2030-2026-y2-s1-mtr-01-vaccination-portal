import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  ApiError,
  API_BASE_URL,
  NETWORK_ERROR_MESSAGE,
  PERMISSION_DENIED_MESSAGE,
  apiRequest,
  apiRequestData,
  parseApiResponse,
  refreshSession,
  bootstrapSession,
  resolveApiBaseUrl,
  __resetRotatedTokensForTests,
} from './apiClient';
import {
  clearToken,
  endAuthSession,
  getToken,
  getSessionPrincipalId,
  onUnauthorized,
  setToken,
  startAuthSession,
  __resetAuthStorageForTests,
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
  it.each(['PUT', 'PATCH'])('rejects a different-account shared cookie without replaying %s', async (method) => {
    startAuthSession('A1', 'A');
    const listener = vi.fn();
    onUnauthorized(listener);
    fetch.mockResolvedValueOnce(response({ status: 401 }))
      .mockResolvedValueOnce(response({ body: JSON.stringify({ token: 'B1', user: { id: 'B' } }) }));

    const result = await apiRequest('/v1/records', { method, body: '{"value":1}' });

    expect(result.status).toBe(401);
    expect(fetch.mock.calls.filter(([url]) => url === '/api/v1/records')).toHaveLength(1);
    expect(fetch).toHaveBeenCalledTimes(2);
    expect(fetch.mock.calls.some(([, config]) => config.headers.Authorization === 'Bearer B1')).toBe(false);
    expect(getToken()).toBeNull();
    expect(getSessionPrincipalId()).toBeNull();
    expect(listener).toHaveBeenCalledTimes(1);
  });

  it('accepts a same-account shared cookie and replays exactly once', async () => {
    startAuthSession('A1', 7);
    fetch.mockResolvedValueOnce(response({ status: 401 }))
      .mockResolvedValueOnce(response({ body: JSON.stringify({ token: 'A2', user: { id: 7 } }) }))
      .mockResolvedValueOnce(response({ status: 204 }));
    expect((await apiRequest('/v1/records', { method: 'PATCH' })).status).toBe(204);
    expect(fetch).toHaveBeenCalledTimes(3);
    expect(fetch.mock.calls[2][1].headers.Authorization).toBe('Bearer A2');
    expect(getSessionPrincipalId()).toBe('7');
  });

  it('only explicit cold bootstrap may establish a principal', async () => {
    fetch.mockResolvedValueOnce(response({ body: JSON.stringify({ token: 'A1', user: { id: 7 } }) }));
    expect(await refreshSession()).toBeNull();
    expect(fetch).not.toHaveBeenCalled();
    expect(await bootstrapSession()).toBe('A1');
    expect(getSessionPrincipalId()).toBe('7');
  });

  it('bootstrap preserves an already-bound identity', async () => {
    startAuthSession('A1', 'A');
    fetch.mockResolvedValueOnce(response({ body: JSON.stringify({ token: 'B1', user: { id: 'B' } }) }));
    expect(await bootstrapSession()).toBeNull();
    expect(getToken()).toBeNull();
    expect(getSessionPrincipalId()).toBeNull();
  });

  it.each([500, 503, 0])('preserves the principal and token on transient refresh status %s', async (status) => {
    startAuthSession('A1', 'A');
    if (status === 0) fetch.mockRejectedValueOnce(new TypeError('Failed to fetch'));
    else fetch.mockResolvedValueOnce(response({ status }));
    await expect(refreshSession()).rejects.toMatchObject({ status });
    expect(getToken()).toBe('A1');
    expect(getSessionPrincipalId()).toBe('A');
  });

  it('does not clear a newer login when an old mismatching refresh finishes', async () => {
    startAuthSession('A1', 'A');
    const listener = vi.fn();
    onUnauthorized(listener);
    let finishRefresh;
    fetch.mockImplementationOnce(() => new Promise((resolve) => { finishRefresh = resolve; }));
    const pending = refreshSession();
    startAuthSession('C1', 'C');
    finishRefresh(response({ body: JSON.stringify({ token: 'B1', user: { id: 'B' } }) }));
    expect(await pending).toBeNull();
    expect(getToken()).toBe('C1');
    expect(getSessionPrincipalId()).toBe('C');
    expect(listener).not.toHaveBeenCalled();
  });

  it('keeps principal binding memory-only and clears it on logout', () => {
    const write = vi.spyOn(Storage.prototype, 'setItem');
    startAuthSession('A1', 'A');
    expect(getSessionPrincipalId()).toBe('A');
    endAuthSession();
    expect(getToken()).toBeNull();
    expect(getSessionPrincipalId()).toBeNull();
    expect(write).not.toHaveBeenCalled();
  });

  beforeEach(() => {
    window.sessionStorage.clear();
    __resetAuthStorageForTests();
    __resetRotatedTokensForTests();
    vi.stubGlobal('fetch', vi.fn());
  });

  afterEach(() => {
    __resetAuthStorageForTests();
    __resetRotatedTokensForTests();
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

  it('recovers a current-session 401 through one refresh and one retry', async () => {
    startAuthSession('expired-token', 1);
    fetch
      .mockResolvedValueOnce(response({ body: '{"message":"Unauthorized"}', status: 401, statusText: 'Unauthorized' }))
      .mockResolvedValueOnce(response({ body: '{"token":"fresh-token","user":{"id":1}}' }))
      .mockResolvedValueOnce(response({ body: '{"ok":true}' }));

    const result = await apiRequestData('/v1/protected');

    expect(result).toEqual({ ok: true });
    expect(getToken()).toBe('fresh-token');
    expect(fetch).toHaveBeenCalledTimes(3);
    const refreshCall = fetch.mock.calls.find(([url]) => url === '/api/v1/auth/refresh');
    expect(refreshCall[1].credentials).toBe('include');
    expect(refreshCall[1].headers.Accept).toBe('application/json');
    const retryCall = fetch.mock.calls[2];
    expect(retryCall[1].headers.Authorization).toBe('Bearer fresh-token');
  });

  it('clears the session and notifies once when refresh definitively fails', async () => {
    startAuthSession('expired-token', 1);
    const listener = vi.fn(() => clearToken());
    const unsubscribe = onUnauthorized(listener);
    fetch
      .mockResolvedValueOnce(response({
        body: '{"message":"Unauthorized"}',
        status: 401,
        statusText: 'Unauthorized',
      }))
      .mockResolvedValueOnce(response({
        body: '{"message":"Unauthorized"}',
        status: 401,
        statusText: 'Unauthorized',
      }));

    await expect(apiRequestData('/v1/protected')).rejects.toBeInstanceOf(ApiError);

    expect(listener).toHaveBeenCalledTimes(1);
    expect(getToken()).toBeNull();
    unsubscribe();
  });

  it('keeps the session alive when refresh hits a transport failure (R07)', async () => {
    startAuthSession('token-b', 1);
    const listener = vi.fn(() => clearToken());
    const unsubscribe = onUnauthorized(listener);
    fetch
      .mockResolvedValueOnce(response({
        body: '{"message":"Unauthorized"}',
        status: 401,
        statusText: 'Unauthorized',
      }))
      .mockRejectedValueOnce(new TypeError('Failed to fetch'));

    await expect(apiRequestData('/v1/protected')).rejects.toBeInstanceOf(ApiError);

    expect(getToken()).toBe('token-b');
    expect(listener).not.toHaveBeenCalled();
    unsubscribe();
  });

  it('never retries more than once (no refresh loop)', async () => {
    startAuthSession('expired-token', 1);
    fetch
      .mockResolvedValueOnce(response({ body: '{"message":"Unauthorized"}', status: 401, statusText: 'Unauthorized' }))
      .mockResolvedValueOnce(response({ body: '{"token":"fresh-token","user":{"id":1}}' }))
      .mockResolvedValueOnce(response({ body: '{"message":"Unauthorized"}', status: 401, statusText: 'Unauthorized' }))
      .mockResolvedValueOnce(response({ body: '{"token":"fresh-token-2","user":{"id":1}}' }));

    await expect(apiRequestData('/v1/protected')).rejects.toBeInstanceOf(ApiError);

    expect(fetch).toHaveBeenCalledTimes(3);
  });

  it('shares one refresh across concurrent 401s (single flight, no storms)', async () => {
    startAuthSession('expired-token', 1);
    const unauthorized = response({
      body: '{"message":"Unauthorized"}',
      status: 401,
      statusText: 'Unauthorized',
    });
    fetch
      .mockResolvedValueOnce(unauthorized)
      .mockResolvedValueOnce(unauthorized)
      .mockResolvedValueOnce(response({ body: '{"token":"fresh-token","user":{"id":1}}' }))
      .mockResolvedValueOnce(response({ body: '{"r1":1}' }))
      .mockResolvedValueOnce(response({ body: '{"r2":2}' }));

    const [first, second] = await Promise.all([
      apiRequestData('/v1/protected'),
      apiRequestData('/v1/profile'),
    ]);

    expect(first).toEqual({ r1: 1 });
    expect(second).toEqual({ r2: 2 });
    const refreshCalls = fetch.mock.calls.filter(([url]) => url === '/api/v1/auth/refresh');
    expect(refreshCalls).toHaveLength(1);
  });

  it('replays a concurrent request once with the already-refreshed token without extra refresh (F-02)', async () => {
    startAuthSession('token-t1', 1);
    let resolveA;
    let resolveB;
    fetch
      // 1. Initial Request A call
      .mockImplementationOnce(() => new Promise((resolve) => { resolveA = resolve; }))
      // 2. Initial Request B call (both dispatched concurrently while token is token-t1)
      .mockImplementationOnce(() => new Promise((resolve) => { resolveB = resolve; }))
      // 3. Refresh endpoint call (triggered by Request A 401)
      .mockResolvedValueOnce(response({ body: '{"token":"token-t2","user":{"id":1}}' }))
      // 4. Request A retry call
      .mockResolvedValueOnce(response({ body: '{"endpoint":"a"}' }))
      // 5. Request B retry call with token-t2
      .mockResolvedValueOnce(response({ body: '{"endpoint":"b"}' }));

    const promiseA = apiRequestData('/v1/endpoint-a');
    const promiseB = apiRequestData('/v1/endpoint-b');

    // Request A receives 401 first:
    resolveA(response({ body: '{"message":"Unauthorized"}', status: 401, statusText: 'Unauthorized' }));

    // Request A refreshes token to token-t2 and completes its retry:
    const resA = await promiseA;
    expect(resA).toEqual({ endpoint: 'a' });
    expect(getToken()).toBe('token-t2');

    // Now Request B receives its delayed 401 for token-t1:
    resolveB(response({ body: '{"message":"Unauthorized"}', status: 401, statusText: 'Unauthorized' }));

    // Request B sees token-t1 was rotated to token-t2, replays once without extra refresh, and succeeds:
    const resB = await promiseB;
    expect(resB).toEqual({ endpoint: 'b' });

    // Verify only ONE refresh request was made across both concurrent requests:
    const refreshCalls = fetch.mock.calls.filter(([url]) => url === '/api/v1/auth/refresh');
    expect(refreshCalls).toHaveLength(1);

    // Verify Request B retried with the refreshed token-t2:
    const lastCall = fetch.mock.calls[fetch.mock.calls.length - 1];
    expect(lastCall[1].headers.Authorization).toBe('Bearer token-t2');
  });

  it('discards a pending refresh if a new login completes before refresh finishes (NEW-F1)', async () => {
    startAuthSession('user-a-initial', 1);
    let resolveRefresh;
    fetch.mockImplementationOnce(() => new Promise((resolve) => { resolveRefresh = resolve; }));

    // User A triggers refresh
    const refreshPromise = refreshSession();

    // While refresh is pending, User B logs in
    startAuthSession('user-b-token', 1);

    // Late User A refresh returns 200 with User A's new token
    resolveRefresh(response({ body: '{"token":"user-a-refreshed","user":{"id":1}}' }));
    const result = await refreshPromise;

    // Must return null (discarded) and NOT overwrite User B's token
    expect(result).toBeNull();
    expect(getToken()).toBe('user-b-token');
  });

  it('discards a pending refresh if the user logs out before refresh finishes (NEW-F1)', async () => {
    startAuthSession('user-a-token', 1);
    let resolveRefresh;
    fetch.mockImplementationOnce(() => new Promise((resolve) => { resolveRefresh = resolve; }));

    const refreshPromise = refreshSession();

    // User logs out before refresh resolves
    endAuthSession();

    // Late refresh returns 200 with refreshed token
    resolveRefresh(response({ body: '{"token":"user-a-refreshed","user":{"id":1}}' }));
    const result = await refreshPromise;

    expect(result).toBeNull();
    expect(getToken()).toBeNull();
  });

  it('never replays a prior-session request under a new session (NEW-F1 / R07)', async () => {
    startAuthSession('user-a-token', 1);
    let resolveReqA;
    fetch.mockImplementationOnce(() => new Promise((resolve) => { resolveReqA = resolve; }));

    // Request dispatched under User A
    const reqA = apiRequestData('/v1/protected');

    // User B logs in while Request A is in flight
    startAuthSession('user-b-token', 1);

    // Request A finishes with 401
    resolveReqA(response({
      body: '{"message":"Unauthorized"}',
      status: 401,
      statusText: 'Unauthorized',
    }));

    await expect(reqA).rejects.toBeInstanceOf(ApiError);
    // User B's token remains intact and Request A was not replayed
    expect(getToken()).toBe('user-b-token');
    // Fetch was called only once (no replay or refresh under B)
    expect(fetch).toHaveBeenCalledTimes(1);
  });

  it('F-024-1: request A awaiting refresh aborts replay when user B logs in during the await', async () => {
    // 1. Session A starts with token A1
    startAuthSession('user-a-t1', 1);
    const listener = vi.fn();
    const unsubscribe = onUnauthorized(listener);

    let resolveRefresh;
    fetch
      // 1st fetch: Request A (dispatched with A1) returns 401
      .mockResolvedValueOnce(response({
        body: '{"message":"Token expired"}',
        status: 401,
        statusText: 'Unauthorized',
      }))
      // 2nd fetch: refreshSession() is called and held unresolved
      .mockImplementationOnce(() => new Promise((resolve) => { resolveRefresh = resolve; }));

    const reqA = apiRequestData('/v1/citizen-records', { method: 'PATCH', body: '{"field":"val"}' });

    // Ensure Request A has entered refresh wait
    await vi.waitFor(() => {
      expect(fetch).toHaveBeenCalledTimes(2);
    });

    // 2. While refresh is in-flight, User B logs in with token B1
    startAuthSession('user-b-t1', 1);

    // 3. User A's refresh resolves
    resolveRefresh(response({ body: '{"token":"user-a-t2","user":{"id":1}}' }));

    // 4. Request A must reject with 401 and MUST NOT replay under User B
    await expect(reqA).rejects.toBeInstanceOf(ApiError);

    // Fetch was called only twice: initial PATCH + 1 refresh attempt. Zero replay under B!
    expect(fetch).toHaveBeenCalledTimes(2);
    // User B's token must be untouched
    expect(getToken()).toBe('user-b-t1');
    // Unauthorized listener must not have been triggered against User B
    expect(listener).not.toHaveBeenCalled();
    unsubscribe();
  });

  it('F-024-1: request A awaiting refresh aborts replay when user logs out during the await', async () => {
    startAuthSession('user-a-t1', 1);
    const listener = vi.fn();
    const unsubscribe = onUnauthorized(listener);

    let resolveRefresh;
    fetch
      .mockResolvedValueOnce(response({
        body: '{"message":"Token expired"}',
        status: 401,
        statusText: 'Unauthorized',
      }))
      .mockImplementationOnce(() => new Promise((resolve) => { resolveRefresh = resolve; }));

    const reqA = apiRequestData('/v1/citizen-records', { method: 'PATCH', body: '{"field":"val"}' });

    await vi.waitFor(() => {
      expect(fetch).toHaveBeenCalledTimes(2);
    });

    // User logs out while refresh is in-flight
    endAuthSession();

    // Refresh resolves
    resolveRefresh(response({ body: '{"token":"user-a-t2","user":{"id":1}}' }));

    await expect(reqA).rejects.toBeInstanceOf(ApiError);

    // Only initial PATCH + refresh fetch; no replay after logout
    expect(fetch).toHaveBeenCalledTimes(2);
    expect(getToken()).toBeNull();
    expect(listener).not.toHaveBeenCalled();
    unsubscribe();
  });

  it('F-024-1: concurrent request awaiting rotated token aborts replay when user B logs in during await', async () => {
    startAuthSession('user-a-t1', 1);
    const listener = vi.fn();
    const unsubscribe = onUnauthorized(listener);

    let resolveRefresh;
    fetch
      // 1st fetch: Concurrent Request 1 gets 401 and starts refresh
      .mockResolvedValueOnce(response({ body: '{"message":"Token expired"}', status: 401, statusText: 'Unauthorized' }))
      // 2nd fetch: Request 2 gets 401 (dispatched with t1)
      .mockResolvedValueOnce(response({ body: '{"message":"Token expired"}', status: 401, statusText: 'Unauthorized' }))
      // 3rd fetch: The single-flight refresh call, held in flight
      .mockImplementationOnce(() => new Promise((resolve) => { resolveRefresh = resolve; }));

    const req1 = apiRequestData('/v1/res-1');
    const req2 = apiRequestData('/v1/res-2');

    await vi.waitFor(() => {
      expect(fetch).toHaveBeenCalledTimes(3);
    });

    // User B logs in during the await
    startAuthSession('user-b-t1', 1);

    // Refresh finishes
    resolveRefresh(response({ body: '{"token":"user-a-t2","user":{"id":1}}' }));

    await expect(req1).rejects.toBeInstanceOf(ApiError);
    await expect(req2).rejects.toBeInstanceOf(ApiError);

    // No replay calls happened under B
    expect(fetch).toHaveBeenCalledTimes(3);
    expect(getToken()).toBe('user-b-t1');
    expect(listener).not.toHaveBeenCalled();
    unsubscribe();
  });

  it('clears token rotation genealogy when a new session starts (NEW-F1)', async () => {
    // Session 1 rotates t1 -> t2
    startAuthSession('session-1-t1', 1);
    fetch.mockResolvedValueOnce(response({ body: '{"token":"session-1-t2","user":{"id":1}}' }));
    await refreshSession();
    expect(getToken()).toBe('session-1-t2');

    // Start a request in session 1 that remains in flight
    let resolveStaleReq;
    fetch.mockImplementationOnce(() => new Promise((resolve) => { resolveStaleReq = resolve; }));
    const staleReq = apiRequestData('/v1/protected');

    // Now Session 2 starts (new login)
    startAuthSession('session-2-t1', 1);

    // Stale request finishes with 401
    resolveStaleReq(response({
      body: '{"message":"Unauthorized"}',
      status: 401,
      statusText: 'Unauthorized',
    }));

    await expect(staleReq).rejects.toBeInstanceOf(ApiError);

    // It was not replayed and did not trigger a refresh under session 2:
    // Total fetch calls: 1 (refresh in session 1) + 1 (staleReq initial call) = 2
    expect(fetch).toHaveBeenCalledTimes(2);
    expect(getToken()).toBe('session-2-t1');
  });

  it('does not clear session or notify unauthorized on transient 500 refresh error (NEW-F3)', async () => {
    startAuthSession('active-token', 1);
    const listener = vi.fn();
    const unsubscribe = onUnauthorized(listener);

    fetch
      .mockResolvedValueOnce(response({ body: '{"message":"Unauthorized"}', status: 401, statusText: 'Unauthorized' }))
      .mockResolvedValueOnce(response({ body: '{"error":"Internal Server Error"}', status: 500, statusText: 'Internal Server Error' }));

    const res = await apiRequest('/v1/data');
    expect(res.status).toBe(401);
    expect(getToken()).toBe('active-token');
    expect(listener).not.toHaveBeenCalled();
    unsubscribe();
  });

  it('does not clear session or notify unauthorized on transient 503 refresh error (NEW-F3)', async () => {
    startAuthSession('active-token', 1);
    const listener = vi.fn();
    const unsubscribe = onUnauthorized(listener);

    fetch.mockResolvedValueOnce(response({
      body: '{"error":"Service Unavailable"}',
      status: 503,
      statusText: 'Service Unavailable',
    }));

    await expect(refreshSession()).rejects.toMatchObject({
      name: 'ApiError',
      status: 503,
    });

    expect(getToken()).toBe('active-token');
    expect(listener).not.toHaveBeenCalled();
    unsubscribe();
  });

  it('clears session and notifies listeners on definitive 403 or 423 refresh error (NEW-F3)', async () => {
    startAuthSession('active-token', 1);
    const listener = vi.fn();
    const unsubscribe = onUnauthorized(listener);

    fetch.mockResolvedValueOnce(response({
      body: '{"error":"Account Locked"}',
      status: 423,
      statusText: 'Locked',
    }));

    const result = await refreshSession();
    expect(result).toBeNull();
    expect(getToken()).toBeNull();
    expect(listener).toHaveBeenCalledTimes(1);
    unsubscribe();
  });

  it('stale-generation transient failure has no effect on newer session (NEW-F3)', async () => {
    startAuthSession('user-a-token', 1);
    let resolveRefresh;
    fetch.mockImplementationOnce(() => new Promise((resolve) => { resolveRefresh = resolve; }));

    const refreshPromise = refreshSession();

    // User B logs in while refresh is in-flight
    startAuthSession('user-b-token', 1);

    // Refresh fails with 500
    resolveRefresh(response({ body: '{"error":"Server Error"}', status: 500, statusText: 'Server Error' }));
    const result = await refreshPromise;

    expect(result).toBeNull();
    expect(getToken()).toBe('user-b-token');
  });

  it('never persists the access token to sessionStorage or localStorage', async () => {
    setToken('memory-only-token');

    expect(getToken()).toBe('memory-only-token');
    expect(window.sessionStorage.getItem('vaccination_portal_token')).toBeNull();
    expect(window.localStorage.getItem('vaccination_portal_token')).toBeNull();
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
    fetch
      .mockResolvedValueOnce(response({
        body: '{"message":"Unauthorized"}',
        status: 401,
        statusText: 'Unauthorized',
      }))
      .mockResolvedValueOnce(response({
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
