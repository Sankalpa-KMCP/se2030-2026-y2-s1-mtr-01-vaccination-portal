import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  act,
  cleanup,
  render,
  screen,
  waitFor,
} from '@testing-library/react';
import { apiRequest } from '../../services/apiClient';
import { getToken, setToken } from '../authStorage';
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
  const { isAuthenticated, user } = useAuth();
  return (
    <div>
      <span>{isAuthenticated ? 'authenticated' : 'signed-out'}</span>
      <span>{user?.email || 'no-user'}</span>
    </div>
  );
}

describe('AuthProvider unauthorized handling', () => {
  beforeEach(() => {
    window.sessionStorage.clear();
    vi.stubGlobal('fetch', vi.fn());
  });

  afterEach(() => {
    cleanup();
    window.sessionStorage.clear();
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it('clears the stored session and authenticated state after a protected 401', async () => {
    setToken('expired-after-bootstrap');
    fetch.mockResolvedValueOnce(response({
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

    fetch.mockResolvedValueOnce(response({
      body: '{"message":"Authentication required"}',
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
    });
  });
});
