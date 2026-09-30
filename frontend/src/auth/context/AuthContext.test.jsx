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
import { getToken, setToken } from '../../shared/auth/authStorage';
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
  const { isAuthenticated, login, updateCurrentUserProfile, user } = useAuth();
  return (
    <div>
      <span>{isAuthenticated ? 'authenticated' : 'signed-out'}</span>
      <span>{user?.email || 'no-user'}</span>
      <span>{user?.fullName || 'no-name'}</span>
      <span>{user?.phoneNumber || 'no-phone'}</span>
      <span>{user?.roles?.join(',') || 'no-roles'}</span>
      <button type="button" onClick={() => login('citizen@example.com', 'Secret123!')}>Test Login</button>
      <button type="button" onClick={() => updateCurrentUserProfile({ id: user?.id, fullName: 'Updated Name', phoneNumber: '+94 11 222 3333', roles: ['ROLE_ADMIN'] })}>Update Profile</button>
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

  it('stores the richer login profile and returns it to callers', async () => {
    fetch.mockResolvedValueOnce(response({
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
    await screen.findByText('signed-out');
    fireEvent.click(screen.getByRole('button', { name: 'Test Login' }));

    await screen.findByText('authenticated');
    expect(screen.getByText('Citizen User')).toBeTruthy();
    expect(getToken()).toBe('citizen-token');

    fireEvent.click(screen.getByRole('button', { name: 'Update Profile' }));
    expect(screen.getByText('Updated Name')).toBeTruthy();
    expect(screen.getByText('+94 11 222 3333')).toBeTruthy();
    expect(screen.getByText('ROLE_CITIZEN')).toBeTruthy();
  });
});
