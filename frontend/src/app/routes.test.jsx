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

vi.mock('../features/screening/pages/ScreeningQuestionsPage', () => ({
  default: () => 'Screening Questions Test Page',
}));

vi.mock('../features/screening/pages/ScreeningQuestionnairesPage', () => ({
  default: () => 'Screening Questionnaires Test Page',
}));

vi.mock('../features/screening/pages/CitizenScreeningCampaignsPage', () => ({
  default: () => 'Citizen Screening Campaigns Test Page',
}));

vi.mock('../features/screening/pages/CitizenScreeningPage', () => ({
  default: () => 'Citizen Screening Form Test Page',
}));

vi.mock('../auth/pages/PendingStaffRequestsPage', () => ({
  default: () => 'Pending Staff Requests Test Page',
}));

import Header from '../components/common/Header';
import ProtectedRoute from './ProtectedRoute';
import PortalRoutes from './routes';

vi.mock('../features/vaccination-administration/pages/VaccinationDraftsPage', () => ({
  default: () => 'Vaccination Drafts Test Page',
}));

vi.mock('../features/screening/pages/CitizenEligibilityPage', () => ({ default: () => 'Citizen Eligibility Test Page' }));
vi.mock('../features/screening/pages/CitizenScreeningHistoryPage', () => ({ default: () => 'Citizen History Test Page' }));
vi.mock('../features/screening/pages/CitizenScreeningHistoryDetailsPage', () => ({ default: () => 'Citizen History Details Test Page' }));
vi.mock('../features/screening/pages/CitizenScreeningCorrectionPage', () => ({ default: () => 'Citizen Correction Test Page' }));
vi.mock('../features/citizen-profile/pages/CitizenProfilePage', () => ({ default: () => 'Citizen Profile Test Page' }));
vi.mock('../features/screening/pages/HealthcareOfficerScreeningReviewQueuePage', () => ({ default: () => 'HCO Screening Review Queue Test Page' }));
vi.mock('../features/screening/pages/HealthcareOfficerScreeningReviewDetailsPage', () => ({ default: () => 'HCO Screening Review Details Test Page' }));

function setAuth({ isAuthenticated = true, roles = [] } = {}) {
  const logout = vi.fn();
  authState.current = {
    user: isAuthenticated
      ? { id: 1, email: 'user@example.gov', roles }
      : null,
    isAuthenticated,
    isInitializing: false,
    initializationError: null,
    loginError: null,
    login: vi.fn(),
    clearLoginError: vi.fn(),
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

  it('routes a Vaccinator to staff and shows draft navigation', () => {
    setAuth({ roles: ['ROLE_VACCINATOR'] });
    render(<MemoryRouter initialEntries={['/app']}><PortalRoutes /></MemoryRouter>);
    expect(screen.getByText('Staff Dashboard')).toBeTruthy();
    expect(screen.getAllByRole('link', { name: /Vaccination Drafts/ }).length).toBeGreaterThan(0);
    expect(screen.queryByRole('link', { name: 'Staff Requests' })).toBeNull();
  });

  it('allows a Vaccinator to open draft CRUD', () => {
    setAuth({ roles: ['ROLE_VACCINATOR'] });
    render(<MemoryRouter initialEntries={['/app/staff/vaccination-drafts']}><PortalRoutes /></MemoryRouter>);
    expect(screen.getByText('Vaccination Drafts Test Page')).toBeTruthy();
  });

  it.each(['ROLE_CITIZEN', 'ROLE_ADMIN', 'ROLE_HEALTHCARE_OFFICER'])('denies draft CRUD to %s', (role) => {
    setAuth({ roles: [role] });
    render(<MemoryRouter initialEntries={['/app/staff/vaccination-drafts']}><PortalRoutes /></MemoryRouter>);
    expect(screen.getByRole('heading', { name: 'Not Authorized' })).toBeTruthy();
    expect(screen.queryByText('Vaccination Drafts Test Page')).toBeNull();
  });

  it('requires login for draft CRUD', () => {
    render(<MemoryRouter initialEntries={['/app/staff/vaccination-drafts']}><PortalRoutes /></MemoryRouter>);
    expect(screen.getByRole('heading', { name: 'Welcome back.' })).toBeTruthy();
    expect(screen.queryByText('Vaccination Drafts Test Page')).toBeNull();
  });

  it('redirects an unauthenticated protected route to login', () => {
    renderGuard();

    expect(screen.getByRole('heading', { name: 'Login Route' })).toBeTruthy();
  });

  describe.each([
    ['/app/screening/eligibility', 'Citizen Eligibility Test Page'],
    ['/app/screening/history', 'Citizen History Test Page'],
    ['/app/screening/history/42', 'Citizen History Details Test Page'],
    ['/app/screening/history/42/correct', 'Citizen Correction Test Page'],
  ])('citizen screening route %s', (path, page) => {
    it('allows a citizen', () => {
      setAuth({ roles: ['ROLE_CITIZEN'] });
      render(<MemoryRouter initialEntries={[path]}><PortalRoutes /></MemoryRouter>);
      expect(screen.getByText(page)).toBeTruthy();
    });
    it('redirects unauthenticated visitors to login', () => {
      render(<MemoryRouter initialEntries={[path]}><PortalRoutes /></MemoryRouter>);
      expect(screen.getByRole('heading', { name: 'Welcome back.' })).toBeTruthy();
      expect(screen.queryByText(page)).toBeNull();
    });
    it.each(['ROLE_ADMIN', 'ROLE_HEALTHCARE_OFFICER', 'ROLE_CENTER_MANAGER', 'ROLE_INVENTORY_OFFICER'])('denies %s', (role) => {
      setAuth({ roles: [role] });
      render(<MemoryRouter initialEntries={[path]}><PortalRoutes /></MemoryRouter>);
      expect(screen.getByRole('heading', { name: 'Not Authorized' })).toBeTruthy();
      expect(screen.queryByText(page)).toBeNull();
    });
  });

  describe('citizen profile route', () => {
    it('allows a citizen', () => {
      setAuth({ roles: ['ROLE_CITIZEN'] });
      render(<MemoryRouter initialEntries={['/app/citizen/profile']}><PortalRoutes /></MemoryRouter>);
      expect(screen.getByText('Citizen Profile Test Page')).toBeTruthy();
    });

    it('redirects an unauthenticated visitor to login', () => {
      render(<MemoryRouter initialEntries={['/app/citizen/profile']}><PortalRoutes /></MemoryRouter>);
      expect(screen.getByRole('heading', { name: 'Welcome back.' })).toBeTruthy();
      expect(screen.queryByText('Citizen Profile Test Page')).toBeNull();
    });

    it.each([
      'ROLE_ADMIN',
      'ROLE_HEALTHCARE_OFFICER',
      'ROLE_SYSTEM_ADMINISTRATOR',
      'ROLE_CAMPAIGN_OFFICER',
      'ROLE_CENTER_MANAGER',
      'ROLE_MANAGEMENT_USER',
    ])('denies %s', (role) => {
      setAuth({ roles: [role] });
      render(<MemoryRouter initialEntries={['/app/citizen/profile']}><PortalRoutes /></MemoryRouter>);
      expect(screen.getByRole('heading', { name: 'Not Authorized' })).toBeTruthy();
      expect(screen.queryByText('Citizen Profile Test Page')).toBeNull();
    });
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

    expect(screen.getByRole('heading', { name: 'Welcome, user@example.gov' })).toBeTruthy();

    setAuth({ isAuthenticated: false });
    view.rerender(
      <MemoryRouter initialEntries={['/app/overview']}>
        <PortalRoutes />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Welcome back.' })).toBeTruthy();
    });
  });

  it('ignores spoofed role-header values and trusts AuthContext roles', () => {
    window.sessionStorage.setItem('X-User-Role', 'ROLE_ADMIN');
    window.sessionStorage.setItem('X-Role', 'ROLE_ADMIN');
    setAuth({ roles: ['ROLE_CITIZEN'] });
    renderGuard(['ROLE_ADMIN']);

    expect(screen.getByRole('heading', { name: 'Not Authorized Route' })).toBeTruthy();
  });

  describe('screening configuration access for ROLE_HEALTHCARE_OFFICER', () => {
    it('opens the screening questions route', () => {
      setAuth({ roles: ['ROLE_HEALTHCARE_OFFICER'] });

      render(
        <MemoryRouter initialEntries={['/app/screening/questions']}>
          <PortalRoutes />
        </MemoryRouter>,
      );

      expect(screen.getByText('Screening Questions Test Page')).toBeTruthy();
    });

    it('opens the screening questionnaires route', () => {
      setAuth({ roles: ['ROLE_HEALTHCARE_OFFICER'] });

      render(
        <MemoryRouter initialEntries={['/app/screening/questionnaires']}>
          <PortalRoutes />
        </MemoryRouter>,
      );

      expect(screen.getByText('Screening Questionnaires Test Page')).toBeTruthy();
    });
  });

  describe.each([
    'ROLE_ADMIN',
    'ROLE_SYSTEM_ADMINISTRATOR',
    'ROLE_CITIZEN',
    'ROLE_CAMPAIGN_OFFICER',
    'ROLE_CENTER_MANAGER',
    'ROLE_MANAGEMENT_USER',
    'ROLE_INVENTORY_OFFICER',
  ])('screening configuration denial for %s', (role) => {
    it.each([
      '/app/screening/questions',
      '/app/screening/questionnaires',
    ])('denies %s', (path) => {
      setAuth({ roles: [role] });

      render(
        <MemoryRouter initialEntries={[path]}>
          <PortalRoutes />
        </MemoryRouter>,
      );

      expect(screen.getByRole('heading', { name: 'Not Authorized' })).toBeTruthy();
    });
  });

  it('allows citizens to open both citizen screening routes', () => {
    setAuth({ roles: ['ROLE_CITIZEN'] });
    const view = render(
      <MemoryRouter key="campaigns" initialEntries={['/app/screening']}>
        <PortalRoutes />
      </MemoryRouter>,
    );
    expect(screen.getByText('Citizen Screening Campaigns Test Page')).toBeTruthy();

    view.rerender(
      <MemoryRouter key="form" initialEntries={['/app/screening/campaigns/22']}>
        <PortalRoutes />
      </MemoryRouter>,
    );
    expect(screen.getByText('Citizen Screening Form Test Page')).toBeTruthy();
  });

  it('denies citizen screening routes to the wrong role', () => {
    setAuth({ roles: ['ROLE_HEALTHCARE_OFFICER'] });
    render(
      <MemoryRouter initialEntries={['/app/screening']}>
        <PortalRoutes />
      </MemoryRouter>,
    );
    expect(screen.getByRole('heading', { name: 'Not Authorized' })).toBeTruthy();
  });

  it('shows screening configuration navigation to HEALTHCARE_OFFICER only', () => {
    setAuth({ roles: ['ROLE_HEALTHCARE_OFFICER'] });
    const view = render(
      <MemoryRouter>
        <Header />
      </MemoryRouter>,
    );

    expect(screen.getByRole('link', { name: 'Screening Questions' })).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Questionnaires' })).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Screening Review' })).toBeTruthy();
    expect(screen.queryByRole('link', { name: 'Staff Requests' })).toBeNull();

    setAuth({ roles: ['ROLE_ADMIN'] });
    view.rerender(
      <MemoryRouter>
        <Header />
      </MemoryRouter>,
    );

    expect(screen.queryByRole('link', { name: 'Screening Questions' })).toBeNull();
    expect(screen.queryByRole('link', { name: 'Questionnaires' })).toBeNull();
    expect(screen.getByRole('link', { name: 'Staff Requests' })).toBeTruthy();

    setAuth({ roles: ['ROLE_SYSTEM_ADMINISTRATOR'] });
    view.rerender(
      <MemoryRouter>
        <Header />
      </MemoryRouter>,
    );

    expect(screen.queryByRole('link', { name: 'Screening Questions' })).toBeNull();
    expect(screen.queryByRole('link', { name: 'Questionnaires' })).toBeNull();
    expect(screen.queryByRole('link', { name: 'Staff Requests' })).toBeNull();
  });

  it('keeps citizen and staff account entry points public', () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <PortalRoutes />
      </MemoryRouter>,
    );

    expect(screen.getByRole('link', { name: /create citizen account/i })).toBeTruthy();
    expect(screen.getByRole('link', { name: /sign in as a citizen/i })).toBeTruthy();
    expect(screen.getByRole('link', { name: /request staff account/i })).toBeTruthy();
    expect(screen.getByRole('link', { name: /enter staff portal/i })).toBeTruthy();
  });

  it('keeps the generic, citizen, and staff login routes registered', () => {
    const view = render(<MemoryRouter key="generic" initialEntries={['/login']}><PortalRoutes /></MemoryRouter>);
    expect(screen.getByRole('heading', { name: 'Welcome back.' })).toBeTruthy();

    view.rerender(<MemoryRouter key="citizen" initialEntries={['/login/citizen']}><PortalRoutes /></MemoryRouter>);
    expect(screen.getByRole('heading', { name: 'Citizen sign in' })).toBeTruthy();
    expect(screen.getByText('Access appointments, eligibility, and your available vaccination services.')).toBeTruthy();
    expect(screen.getByText('New citizen?')).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Create Citizen Account' }).getAttribute('href')).toBe('/register/citizen');

    view.rerender(<MemoryRouter key="staff" initialEntries={['/login/staff']}><PortalRoutes /></MemoryRouter>);
    expect(screen.getByRole('heading', { name: 'Staff sign in' })).toBeTruthy();
    expect(screen.getByText('For approved clinical, campaign, center, inventory, administrative, and management accounts.')).toBeTruthy();
  });

  it('uses /app/overview as a compatibility redirect to the role dashboard', () => {
    setAuth({ roles: ['ROLE_CITIZEN'] });
    render(<MemoryRouter initialEntries={['/app/overview']}><PortalRoutes /></MemoryRouter>);
    expect(screen.getByRole('heading', { name: 'Welcome, user@example.gov' })).toBeTruthy();
    expect(screen.getByText('Citizen Dashboard')).toBeTruthy();
  });

  it('allows only citizens to open the citizen dashboard', () => {
    setAuth({ roles: ['ROLE_CITIZEN'] });
    const view = render(<MemoryRouter initialEntries={['/app/citizen']}><PortalRoutes /></MemoryRouter>);
    expect(screen.getByText('Citizen Dashboard')).toBeTruthy();

    setAuth({ roles: ['ROLE_CENTER_MANAGER'] });
    view.rerender(<MemoryRouter initialEntries={['/app/citizen']}><PortalRoutes /></MemoryRouter>);
    expect(screen.getByRole('heading', { name: 'Not Authorized' })).toBeTruthy();
  });

  it('allows canonical staff and denies citizens on the staff dashboard', () => {
    setAuth({ roles: ['ROLE_INVENTORY_OFFICER'] });
    const view = render(<MemoryRouter initialEntries={['/app/staff']}><PortalRoutes /></MemoryRouter>);
    expect(screen.getByText('Staff Dashboard')).toBeTruthy();

    setAuth({ roles: ['ROLE_CITIZEN'] });
    view.rerender(<MemoryRouter initialEntries={['/app/staff']}><PortalRoutes /></MemoryRouter>);
    expect(screen.getByRole('heading', { name: 'Not Authorized' })).toBeTruthy();
  });

  describe.each([
    ['/app/staff/screening/reviews', 'HCO Screening Review Queue Test Page'],
    ['/app/staff/screening/reviews/42', 'HCO Screening Review Details Test Page'],
  ])('healthcare-officer review route %s', (path, page) => {
    it('allows a healthcare officer', () => {
      setAuth({ roles: ['ROLE_HEALTHCARE_OFFICER'] });
      render(<MemoryRouter initialEntries={[path]}><PortalRoutes /></MemoryRouter>);
      expect(screen.getByText(page)).toBeTruthy();
    });

    it('redirects an unauthenticated visitor to login', () => {
      setAuth({ isAuthenticated: false });
      render(<MemoryRouter initialEntries={[path]}><PortalRoutes /></MemoryRouter>);
      expect(screen.getByRole('heading', { name: 'Welcome back.' })).toBeTruthy();
    });

    it.each([
      'ROLE_CITIZEN',
      'ROLE_INVENTORY_OFFICER',
      'ROLE_ADMIN',
      'ROLE_SYSTEM_ADMINISTRATOR',
    ])('denies %s', (role) => {
      setAuth({ roles: [role] });
      render(<MemoryRouter initialEntries={[path]}><PortalRoutes /></MemoryRouter>);
      expect(screen.getByRole('heading', { name: 'Not Authorized' })).toBeTruthy();
      expect(screen.queryByText(page)).toBeNull();
    });
  });

  it('allows only ADMIN to open admin dashboard and staff requests', () => {
    setAuth({ roles: ['ROLE_ADMIN'] });
    const view = render(<MemoryRouter key="admin" initialEntries={['/app/admin']}><PortalRoutes /></MemoryRouter>);
    expect(screen.getByRole('heading', { name: 'Welcome, user@example.gov' })).toBeTruthy();

    view.rerender(<MemoryRouter key="requests" initialEntries={['/app/admin/staff-requests']}><PortalRoutes /></MemoryRouter>);
    expect(screen.getByText('Pending Staff Requests Test Page')).toBeTruthy();

    setAuth({ roles: ['ROLE_HEALTHCARE_OFFICER'] });
    view.rerender(<MemoryRouter key="requests-denied" initialEntries={['/app/admin/staff-requests']}><PortalRoutes /></MemoryRouter>);
    expect(screen.getByRole('heading', { name: 'Not Authorized' })).toBeTruthy();
  });

  it('keeps all three public pages accessible without authentication', () => {
    setAuth({ isAuthenticated: false });

    render(<MemoryRouter key="home" initialEntries={['/']}><PortalRoutes /></MemoryRouter>);
    expect(screen.getByRole('heading', { level: 1, name: /a clear path from campaign to clinic appointment/i })).toBeTruthy();
    cleanup();

    render(<MemoryRouter key="how" initialEntries={['/how-it-works']}><PortalRoutes /></MemoryRouter>);
    expect(screen.getByRole('heading', { level: 1, name: /one system, two journeys/i })).toBeTruthy();
    cleanup();

    render(<MemoryRouter key="about" initialEntries={['/about']}><PortalRoutes /></MemoryRouter>);
    expect(screen.getByRole('heading', { level: 1, name: /a system built on responsibility/i })).toBeTruthy();
  });

  it('redirects authenticated users away from public pages to their role home', () => {
    setAuth({ roles: ['ROLE_CITIZEN'] });
    render(<MemoryRouter initialEntries={['/how-it-works']}><PortalRoutes /></MemoryRouter>);
    expect(screen.getByText('Citizen Dashboard')).toBeTruthy();
  });
});
