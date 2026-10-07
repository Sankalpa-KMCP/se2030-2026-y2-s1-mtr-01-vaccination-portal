import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  act,
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from '@testing-library/react';
import { apiRequest } from '../../shared/api/apiClient';
import { clearToken, getToken, getSessionPrincipalId } from '../../shared/auth/authStorage';
import { AuthProvider, useAuth } from './AuthContext';

function response({ body = '', status = 200, statusText = 'OK' } = {}) {
  return {
    ok: status >= 200 && status < 300,
    status,
    statusText,
    headers: {
      get: vi.fn(() => 'application/json'),
    },
    text: vi.fn().mockResolvedValue(body),
  };
}

function AuthStateProbe() {
  const { isAuthenticated, isInitializing, initializationError, login, logout, retryBootstrap, updateCurrentUserProfile, user } = useAuth();
  return (
    <div>
      <span data-testid="init-status">{isInitializing ? 'initializing' : 'initialized'}</span>
      <span data-testid="init-error">{initializationError || 'no-error'}</span>
      <span data-testid="auth-status">{isAuthenticated ? 'authenticated' : 'signed-out'}</span>
      <span>{user?.email || 'no-user'}</span>
      <span>{user?.fullName || 'no-name'}</span>
      <span>{user?.phoneNumber || 'no-phone'}</span>
      <span>{user?.roles?.join(',') || 'no-roles'}</span>
      <button type="button" onClick={() => login('citizen@example.com', 'Secret123!')}>Test Login</button>
      <button type="button" onClick={() => logout()}>Test Logout</button>
      <button type="button" onClick={() => retryBootstrap()}>Retry Bootstrap</button>
      <button type="button" onClick={() => updateCurrentUserProfile({ id: user?.id, fullName: 'Updated Name', phoneNumber: '+94 11 222 3333', roles: ['ROLE_ADMIN'] })}>Update Profile</button>
    </div>
  );
}

describe('AuthProvider unauthorized handling', () => {
  it('rejects a /me principal inconsistent with the bootstrap refresh identity', async () => {
    fetch.mockResolvedValueOnce(response({ body: JSON.stringify({ token: 'A1', user: { id: 1 } }) }))
      .mockResolvedValueOnce(response({ body: JSON.stringify({ id: 2, email: 'other@example.gov' }) }));
    render(<AuthProvider><AuthStateProbe /></AuthProvider>);
    await waitFor(() => {
      expect(screen.getByTestId('init-status').textContent).toBe('initialized');
      expect(screen.getByTestId('auth-status').textContent).toBe('signed-out');
    });
    expect(screen.queryByText('other@example.gov')).toBeNull();
    expect(getToken()).toBeNull();
    expect(getSessionPrincipalId()).toBeNull();
  });

  it('signs out the current consumer after a different-account refresh without mutation replay', async () => {
    fetch.mockResolvedValueOnce(response({ body: JSON.stringify({ token: 'A1', user: { id: 1 } }) }))
      .mockResolvedValueOnce(response({ body: JSON.stringify({ id: 1, email: 'a@example.gov' }) }));
    render(<AuthProvider><AuthStateProbe /></AuthProvider>);
    await screen.findByText('authenticated');
    fetch.mockResolvedValueOnce(response({ status: 401 }))
      .mockResolvedValueOnce(response({ body: JSON.stringify({ token: 'B1', user: { id: 2 } }) }));
    await act(async () => { await apiRequest('/v1/records', { method: 'PATCH' }); });
    expect(screen.getByTestId('auth-status').textContent).toBe('signed-out');
    expect(screen.getByText('no-user')).toBeTruthy();
    expect(getSessionPrincipalId()).toBeNull();
    expect(fetch.mock.calls.filter(([url]) => url === '/api/v1/records')).toHaveLength(1);
  });

  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn());
  });

  afterEach(() => {
    cleanup();
    clearToken();
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it('bootstraps through the refresh cookie and clears the session after an unrecoverable 401', async () => {
    fetch
      .mockResolvedValueOnce(response({
        body: JSON.stringify({ token: 'bootstrapped-token', user: { id: 4 } }),
      }))
      .mockResolvedValueOnce(response({
        body: JSON.stringify({
          id: 4,
          email: 'officer@example.gov',
          roles: ['ROLE_HEALTHCARE_OFFICER'],
        }),
      }));

    render(
      <AuthProvider>
        <AuthStateProbe />
      </AuthProvider>,
    );

    await screen.findByText('authenticated');
    expect(screen.getByText('officer@example.gov')).toBeTruthy();
    expect(getToken()).toBe('bootstrapped-token');
    expect(getSessionPrincipalId()).toBe('4');

    // The protected 401 triggers exactly one refresh attempt; when the refresh
    // itself is rejected the session is cleared and the user lands signed out.
    fetch
      .mockResolvedValueOnce(response({
        body: '{"message":"Authentication required"}',
        status: 401,
        statusText: 'Unauthorized',
      }))
      .mockResolvedValueOnce(response({
        body: '{"message":"Unauthorized"}',
        status: 401,
        statusText: 'Unauthorized',
      }));

    await act(async () => {
      await apiRequest('/v1/protected-resource');
    });

    await waitFor(() => {
      expect(screen.getByText('signed-out')).toBeTruthy();
      expect(screen.getByText('no-user')).toBeTruthy();
      expect(getToken()).toBeNull();
      expect(getSessionPrincipalId()).toBeNull();
    });
  });

  it('stores the richer login profile in memory and returns it to callers', async () => {
    fetch
      .mockResolvedValueOnce(response({
        body: JSON.stringify({ token: 'bootstrap-token', user: { id: 1 } }),
      }))
      .mockResolvedValueOnce(response({
        body: JSON.stringify({
          id: 1,
          email: 'bootstrap@example.gov',
          roles: ['ROLE_CITIZEN'],
        }),
      }))
      .mockResolvedValueOnce(response({
        body: JSON.stringify({
          token: 'citizen-token',
          user: {
            id: 9,
            fullName: 'Citizen User',
            email: 'citizen@example.com',
            phoneNumber: '+94 77 123 4567',
            accountStatus: 'ACTIVE',
            roles: ['ROLE_CITIZEN'],
          },
        }),
      }));

    render(<AuthProvider><AuthStateProbe /></AuthProvider>);
    await screen.findByText('authenticated');
    fireEvent.click(screen.getByRole('button', { name: 'Test Login' }));

    await screen.findByText('authenticated');
    expect(screen.getByText('Citizen User')).toBeTruthy();
    expect(getToken()).toBe('citizen-token');
    expect(getSessionPrincipalId()).toBe('9');
    expect(window.sessionStorage.getItem('vaccination_portal_token')).toBeNull();
    expect(window.localStorage.getItem('vaccination_portal_token')).toBeNull();

    fireEvent.click(screen.getByRole('button', { name: 'Update Profile' }));
    expect(screen.getByText('Updated Name')).toBeTruthy();
    expect(screen.getByText('+94 11 222 3333')).toBeTruthy();
    expect(screen.getByText('ROLE_CITIZEN')).toBeTruthy();
  });

  it('revokes the server session on logout and clears in-memory state', async () => {
    fetch
      .mockResolvedValueOnce(response({ body: JSON.stringify({ token: 'bootstrap-token', user: { id: 2 } }) }))
      .mockResolvedValueOnce(response({
        body: JSON.stringify({ id: 2, email: 'logout@example.gov', roles: ['ROLE_CITIZEN'] }),
      }))
      .mockResolvedValueOnce(response({ body: '' }, 204));

    render(<AuthProvider><AuthStateProbe /></AuthProvider>);
    await screen.findByText('authenticated');

    fireEvent.click(screen.getByRole('button', { name: 'Test Logout' }));

    await waitFor(() => {
      expect(screen.getByText('signed-out')).toBeTruthy();
      expect(getToken()).toBeNull();
      expect(getSessionPrincipalId()).toBeNull();
    });
    const logoutCall = fetch.mock.calls.find(([url]) => url === '/api/v1/auth/logout');
    expect(logoutCall).toBeTruthy();
    expect(logoutCall[1].credentials).toBe('include');
  });

  it('settles to signed-out and initialized on cold 401 with no cookie (PROMPT #021 fix)', async () => {
    fetch.mockResolvedValueOnce(response({
      body: '{"message":"Invalid refresh token"}',
      status: 401,
      statusText: 'Unauthorized',
    }));

    render(
      <AuthProvider>
        <AuthStateProbe />
      </AuthProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId('init-status').textContent).toBe('initialized');
      expect(screen.getByTestId('auth-status').textContent).toBe('signed-out');
      expect(screen.getByText('no-user')).toBeTruthy();
      expect(screen.getByTestId('init-error').textContent).toBe('no-error');
      expect(getToken()).toBeNull();
      expect(getSessionPrincipalId()).toBeNull();
    });
  });

  it('does not clobber newer login when an in-flight bootstrap settles late', async () => {
    let resolveBootstrapRefresh;
    fetch.mockImplementationOnce(() => new Promise((resolve) => { resolveBootstrapRefresh = resolve; }));
    fetch.mockResolvedValueOnce(response({
      body: JSON.stringify({
        token: 'user-b-token',
        user: { id: 2, email: 'user-b@example.gov', roles: ['ROLE_CITIZEN'] },
      }),
    }));

    render(
      <AuthProvider>
        <AuthStateProbe />
      </AuthProvider>,
    );

    expect(screen.getByTestId('init-status').textContent).toBe('initializing');

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: 'Test Login' }));
    });

    await waitFor(() => {
      expect(screen.getByTestId('auth-status').textContent).toBe('authenticated');
      expect(screen.getByText('user-b@example.gov')).toBeTruthy();
      expect(getToken()).toBe('user-b-token');
      expect(screen.getByTestId('init-status').textContent).toBe('initialized');
    });

    await act(async () => {
      resolveBootstrapRefresh(response({
        body: '{"message":"Invalid refresh token"}',
        status: 401,
        statusText: 'Unauthorized',
      }));
    });

    expect(screen.getByTestId('auth-status').textContent).toBe('authenticated');
    expect(screen.getByText('user-b@example.gov')).toBeTruthy();
    expect(getToken()).toBe('user-b-token');
    expect(screen.getByTestId('init-status').textContent).toBe('initialized');
  });

  it('does not resurrect session if user logs out while bootstrap refresh is in flight', async () => {
    let resolveBootstrapRefresh;
    fetch.mockImplementationOnce(() => new Promise((resolve) => { resolveBootstrapRefresh = resolve; }));
    fetch.mockResolvedValueOnce(response({ body: '' }, 204));

    render(
      <AuthProvider>
        <AuthStateProbe />
      </AuthProvider>,
    );

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: 'Test Logout' }));
    });

    expect(screen.getByTestId('auth-status').textContent).toBe('signed-out');
    expect(screen.getByTestId('init-status').textContent).toBe('initialized');

    await act(async () => {
      resolveBootstrapRefresh(response({ body: JSON.stringify({ token: 'old-stale-token', user: { id: 1 } }) }));
    });

    expect(screen.getByTestId('auth-status').textContent).toBe('signed-out');
    expect(screen.getByText('no-user')).toBeTruthy();
    expect(getToken()).toBeNull();
    expect(getSessionPrincipalId()).toBeNull();
  });

  it('preserves transient error state without hanging initialization on 503 refresh error', async () => {
    fetch.mockResolvedValueOnce(response({
      body: '{"message":"Service Unavailable"}',
      status: 503,
      statusText: 'Service Unavailable',
    }));

    render(
      <AuthProvider>
        <AuthStateProbe />
      </AuthProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId('init-status').textContent).toBe('initialized');
      expect(screen.getByTestId('auth-status').textContent).toBe('signed-out');
      expect(screen.getByTestId('init-error').textContent).not.toBe('no-error');
      expect(getToken()).toBeNull();
      expect(getSessionPrincipalId()).toBeNull();
    });
  });

  it('settles cleanly to signed-out after reload following a logout', async () => {
    fetch.mockResolvedValueOnce(response({
      body: '{"message":"Invalid refresh token"}',
      status: 401,
      statusText: 'Unauthorized',
    }));

    render(
      <AuthProvider>
        <AuthStateProbe />
      </AuthProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId('init-status').textContent).toBe('initialized');
      expect(screen.getByTestId('auth-status').textContent).toBe('signed-out');
      expect(screen.getByText('no-user')).toBeTruthy();
      expect(screen.getByTestId('init-error').textContent).toBe('no-error');
    });
  });

  it('F-024-2: superseded bootstrap A cannot prematurely settle initialization while bootstrap B is pending', async () => {
    let resolveMeA;
    let resolveRefreshB;
    fetch.mockImplementation((url) => {
      if (url.endsWith('/auth/refresh')) {
        if (!resolveMeA) {
          // Attempt A refresh succeeds with token-a
          return Promise.resolve(response({ body: JSON.stringify({ token: 'token-a', user: { id: 1 } }) }));
        }
        // Attempt B refresh is held pending
        return new Promise((resolve) => { resolveRefreshB = resolve; });
      }
      if (url.endsWith('/auth/me')) {
        // Attempt A /me is held pending
        return new Promise((resolve) => { resolveMeA = resolve; });
      }
      return Promise.reject(new Error(`Unexpected url: ${url}`));
    });

    render(
      <AuthProvider>
        <AuthStateProbe />
      </AuthProvider>,
    );

    // Wait for Attempt A to complete refresh and reach /me:
    await waitFor(() => {
      expect(resolveMeA).toBeDefined();
    });
    expect(screen.getByTestId('init-status').textContent).toBe('initializing');

    // Trigger second bootstrap attempt (B) via retry while Attempt A is in flight
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: 'Retry Bootstrap' }));
    });

    // Wait for Attempt B to enter /refresh:
    await waitFor(() => {
      expect(resolveRefreshB).toBeDefined();
    });
    expect(screen.getByTestId('init-status').textContent).toBe('initializing');

    // Attempt A finishes /me with 200
    await act(async () => {
      resolveMeA(response({
        body: JSON.stringify({ id: 1, email: 'attempt-a@example.gov', roles: ['ROLE_CITIZEN'] }),
      }));
    });

    // Attempt A MUST NOT settle initialization because Attempt B is still in flight!
    expect(screen.getByTestId('init-status').textContent).toBe('initializing');
    expect(screen.getByTestId('auth-status').textContent).toBe('signed-out');

    // Now Attempt B finishes with 401
    await act(async () => {
      resolveRefreshB(response({
        body: '{"message":"Invalid refresh token"}',
        status: 401,
        statusText: 'Unauthorized',
      }));
    });

    // Now initialization settles to initialized
    await waitFor(() => {
      expect(screen.getByTestId('init-status').textContent).toBe('initialized');
      expect(screen.getByTestId('auth-status').textContent).toBe('signed-out');
    });
  });

  it('F-024-2: React.StrictMode setup/cleanup/setup lifecycle does not allow first unmounted attempt to settle second attempt', async () => {
    let resolveMe;
    fetch.mockImplementation((url) => {
      if (url.endsWith('/auth/refresh')) {
        return Promise.resolve(response({ body: JSON.stringify({ token: 'strict-mode-token', user: { id: 99 } }) }));
      }
      if (url.endsWith('/auth/me')) {
        return new Promise((resolve) => { resolveMe = resolve; });
      }
      return Promise.reject(new Error(`Unexpected url: ${url}`));
    });

    render(
      <React.StrictMode>
        <AuthProvider>
          <AuthStateProbe />
        </AuthProvider>
      </React.StrictMode>,
    );

    // Wait for the mounted second attempt to reach /me:
    await waitFor(() => {
      expect(resolveMe).toBeDefined();
    });
    expect(screen.getByTestId('init-status').textContent).toBe('initializing');

    // Second attempt resolves /me:
    await act(async () => {
      resolveMe(response({
        body: JSON.stringify({ id: 99, email: 'strict@example.gov', roles: ['ROLE_CITIZEN'] }),
      }));
    });

    await waitFor(() => {
      expect(screen.getByTestId('init-status').textContent).toBe('initialized');
      expect(screen.getByTestId('auth-status').textContent).toBe('authenticated');
      expect(screen.getByText('strict@example.gov')).toBeTruthy();
    });
  });

  it('F-024-2: stale bootstrap error does not overwrite newer bootstrap error', async () => {
    let resolveMeA;
    let resolveRefreshB;
    fetch.mockImplementation((url) => {
      if (url.endsWith('/auth/refresh')) {
        if (!resolveMeA) {
          // Attempt A refresh succeeds with token-a
          return Promise.resolve(response({ body: JSON.stringify({ token: 'token-a', user: { id: 1 } }) }));
        }
        // Attempt B refresh is held
        return new Promise((resolve) => { resolveRefreshB = resolve; });
      }
      if (url.endsWith('/auth/me')) {
        // Attempt A /me is held
        return new Promise((resolve) => { resolveMeA = resolve; });
      }
      return Promise.reject(new Error(`Unexpected url: ${url}`));
    });

    render(
      <AuthProvider>
        <AuthStateProbe />
      </AuthProvider>,
    );

    // Wait for Attempt A to reach /me:
    await waitFor(() => {
      expect(resolveMeA).toBeDefined();
    });

    // Trigger attempt B while Attempt A is awaiting /me
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: 'Retry Bootstrap' }));
    });

    // Wait for Attempt B to reach /refresh:
    await waitFor(() => {
      expect(resolveRefreshB).toBeDefined();
    });

    // Attempt A fails with a 500 error on /me
    await act(async () => {
      resolveMeA(response({
        body: '{"message":"Attempt A Server Error"}',
        status: 500,
        statusText: 'Internal Server Error',
      }));
    });

    // Attempt A's error is ignored because Attempt B owns initialization
    expect(screen.getByTestId('init-status').textContent).toBe('initializing');
    expect(screen.getByTestId('init-error').textContent).toBe('no-error');

    // Attempt B fails with 503 on refresh
    await act(async () => {
      resolveRefreshB(response({
        body: '{"message":"Service Unavailable"}',
        status: 503,
        statusText: 'Service Unavailable',
      }));
    });

    await waitFor(() => {
      expect(screen.getByTestId('init-status').textContent).toBe('initialized');
      expect(screen.getByTestId('init-error').textContent).toBe('Authentication service is temporarily unavailable');
    });
  });
});
