import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

const authState = vi.hoisted(() => ({
  current: null,
}));

vi.mock('../auth/context/AuthContext', () => ({
  useAuth: () => authState.current,
}));

import Header from '../components/common/Header';
import ProtectedRoute from './ProtectedRoute';
import PortalRoutes from './routes';

function setAuth({ isAuthenticated = true, roles = [] } = {}) {
  const logout = vi.fn();
  authState.current = {
    user: isAuthenticated
      ? { id: 1, email: 'user@example.gov', roles }
      : null,
    isAuthenticated,
    isInitializing: false,
    initializationError: null,
    retryBootstrap: vi.fn(),
    logout,
  };
  return authState.current;
}

function renderGuard(allowedRoles, initialPath = '/protected') {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <Routes>
        <Route path="/login" element={<h1>Login Route</h1>} />
        <Route path="/app/not-authorized" element={<h1>Not Authorized Route</h1>} />
        <Route
          path="/protected"
          element={(
            <ProtectedRoute allowedRoles={allowedRoles}>
              <h1>Protected Content</h1>
            </ProtectedRoute>
          )}
        />
      </Routes>
    </MemoryRouter>,
  );
}

describe('shared routing and role guards', () => {
  beforeEach(() => {
    window.sessionStorage.clear();
    setAuth({ isAuthenticated: false });
  });

  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  it('redirects an unauthenticated protected route to login', () => {
    renderGuard();

    expect(screen.getByRole('heading', { name: 'Login Route' })).toBeTruthy();
  });

  it('opens a protected route for an authenticated permitted role', () => {
    setAuth({ roles: ['ROLE_ADMIN'] });
    renderGuard(['ROLE_ADMIN', 'ROLE_HEALTHCARE_OFFICER']);

    expect(screen.getByRole('heading', { name: 'Protected Content' })).toBeTruthy();
  });

  it('shows Not Authorized for an authenticated wrong role', () => {
    setAuth({ roles: ['ROLE_CITIZEN'] });
    renderGuard(['ROLE_ADMIN']);

    expect(screen.getByRole('heading', { name: 'Not Authorized Route' })).toBeTruthy();
  });

  it('protects an ADMIN-only declaration', () => {
    setAuth({ roles: ['ROLE_HEALTHCARE_OFFICER'] });
    renderGuard(['ADMIN']);

    expect(screen.getByRole('heading', { name: 'Not Authorized Route' })).toBeTruthy();
  });

  it('allows a CITIZEN route declaration', () => {
    setAuth({ roles: ['ROLE_CITIZEN'] });
    renderGuard(['CITIZEN']);

    expect(screen.getByRole('heading', { name: 'Protected Content' })).toBeTruthy();
  });

  it('allows a HEALTHCARE_OFFICER route declaration', () => {
    setAuth({ roles: ['ROLE_HEALTHCARE_OFFICER'] });
    renderGuard(['HEALTHCARE_OFFICER']);

    expect(screen.getByRole('heading', { name: 'Protected Content' })).toBeTruthy();
  });

  it('shows Not Found for an unknown application path', () => {
    setAuth({ roles: ['ROLE_ADMIN'] });

    render(
      <MemoryRouter initialEntries={['/unknown-route']}>
        <PortalRoutes />
      </MemoryRouter>,
    );

    expect(screen.getByRole('heading', { name: 'Page Not Found' })).toBeTruthy();
  });

  it('preserves the Header logout transition', () => {
    const auth = setAuth({ roles: ['ROLE_ADMIN'] });

    render(
      <MemoryRouter initialEntries={['/app/overview']}>
        <Header />
      </MemoryRouter>,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Logout' }));
    expect(auth.logout).toHaveBeenCalledTimes(1);
  });

  it('returns an expired authenticated session to login', async () => {
    setAuth({ roles: ['ROLE_ADMIN'] });
    const view = render(
      <MemoryRouter initialEntries={['/app/overview']}>
        <PortalRoutes />
      </MemoryRouter>,
    );

    expect(screen.getByRole('heading', { name: 'Web-Based Vaccination Portal' })).toBeTruthy();

    setAuth({ isAuthenticated: false });
    view.rerender(
      <MemoryRouter initialEntries={['/app/overview']}>
        <PortalRoutes />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Vaccination Portal' })).toBeTruthy();
    });
  });

  it('ignores spoofed role-header values and trusts AuthContext roles', () => {
    window.sessionStorage.setItem('X-User-Role', 'ROLE_ADMIN');
    window.sessionStorage.setItem('X-Role', 'ROLE_ADMIN');
    setAuth({ roles: ['ROLE_CITIZEN'] });
    renderGuard(['ROLE_ADMIN']);

    expect(screen.getByRole('heading', { name: 'Not Authorized Route' })).toBeTruthy();
  });
});
