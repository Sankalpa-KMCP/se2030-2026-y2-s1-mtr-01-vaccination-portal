import React from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import CitizenDashboardPage from '../../features/citizen-dashboard/pages/CitizenDashboardPage';
import StaffDashboardPage from './StaffDashboardPage';
import AdminDashboardPage from './AdminDashboardPage';

const draftApiState = vi.hoisted(() => ({
  current: {
    createVaccinationDraft: vi.fn(),
    discardVaccinationDraft: vi.fn(),
    listMyVaccinationDrafts: vi.fn(async () => []),
    updateVaccinationDraft: vi.fn(),
  },
}));

vi.mock('../../features/vaccination-administration/api/vaccinationDraftApi', () => ({
  createVaccinationDraft: (...args) => draftApiState.current.createVaccinationDraft(...args),
  discardVaccinationDraft: (...args) => draftApiState.current.discardVaccinationDraft(...args),
  listMyVaccinationDrafts: (...args) => draftApiState.current.listMyVaccinationDrafts(...args),
  updateVaccinationDraft: (...args) => draftApiState.current.updateVaccinationDraft(...args),
}));

vi.mock('../../features/appointment-management/api/checkInApi', () => ({
  getVaccinatorQueue: vi.fn(async () => []),
}));

vi.mock('../../features/center-operations/api/centerOperationsApi', () => ({
  listCenters: vi.fn(async () => []),
}));

vi.mock('../../features/analytics/api/analyticsApi', () => ({
  getOperationalAnalytics: vi.fn(async () => ({
    sessionCountByLifecycleStatus: { SCHEDULED: 0 },
    totalApprovedCapacity: 0,
    totalReservedSlots: 0,
  })),
}));

vi.mock('../api/adminStaffRequestsApi', () => ({
  listStaffRequests: vi.fn(async () => []),
}));

vi.mock('../../features/session-management/api/sessionApi', () => ({
  listTimeSlots: vi.fn(async () => []),
}));

const authState = vi.hoisted(() => ({ current: null }));
const dashboardState = vi.hoisted(() => ({
  current: {
    appointments: { loading: false, data: [], error: null },
    screening: { loading: false, submissions: [], assessments: [], campaigns: [], errors: {} },
  },
}));
vi.mock('../context/AuthContext', () => ({ useAuth: () => authState.current }));
vi.mock('../../features/citizen-dashboard/hooks/useCitizenDashboardData', () => ({
  default: () => dashboardState.current,
}));

function renderPage(page) {
  return render(<MemoryRouter>{page}</MemoryRouter>);
}

afterEach(cleanup);

describe('role dashboards', () => {
  it('exposes all implemented citizen services without future-feature placeholders', () => {
    authState.current = { user: {
      id: 71,
      fullName: 'Citizen User',
      email: 'citizen@example.com',
      phoneNumber: '+94 77 123 4567',
      roles: ['ROLE_CITIZEN'],
    } };
    renderPage(<CitizenDashboardPage />);
    expect(screen.getByRole('heading', { name: 'Welcome, Citizen User' })).toBeTruthy();
    expect(screen.getAllByText('citizen@example.com').length).toBeGreaterThan(0);
    const shortcuts = screen.getByRole('navigation', { name: 'Citizen service shortcuts' });
    expect(within(shortcuts).getByRole('link', { name: /my appointments/i }).getAttribute('href')).toBe('/app/appointments/mine');
    expect(within(shortcuts).getAllByRole('link')).toHaveLength(6);
    expect(screen.queryByText('Coming soon')).toBeNull();
    expect(screen.queryByRole('link', { name: /vaccination history/i })).toBeNull();
    expect(screen.queryByText('Screening Review')).toBeNull();
  });

  it('shows staff profile, role, and genuinely available routes', () => {
    authState.current = { user: {
      fullName: 'Center Manager',
      email: 'manager@example.com',
      phoneNumber: '+94 71 100 2000',
      roles: ['ROLE_CENTER_MANAGER'],
    } };
    renderPage(<StaffDashboardPage />);
    expect(screen.getByRole('heading', { name: 'Welcome, Center Manager' })).toBeTruthy();
    expect(screen.getByText('Center Manager', { selector: '.badge' })).toBeTruthy();
    const checkInCard = screen.getByRole('link', { name: 'Open Check-In & Queue Management' });
    expect(checkInCard.getAttribute('href')).toBe('/app/checkin/queue');
    expect(within(checkInCard).getByText(/process arrivals/i)).toBeTruthy();
    expect(screen.queryByRole('heading', { name: 'Check-In Queue' })).toBeNull();
    expect(screen.queryByText('Screening Review')).toBeNull();
  });

  it('shows the authenticated name in a one-time staff login greeting', () => {
    const consumeLoginWelcome = vi.fn();
    authState.current = {
      consumeLoginWelcome,
      loginWelcome: { audience: 'staff', userId: '44' },
      user: {
        id: 44,
        fullName: 'Vera Vaccinator',
        email: 'vera@example.gov',
        roles: ['ROLE_VACCINATOR'],
      },
    };

    renderPage(<StaffDashboardPage />);

    expect(screen.getByRole('dialog', { name: 'Welcome, Vera Vaccinator' })).toBeTruthy();
    expect(screen.getByText('Staff sign-in successful')).toBeTruthy();
    expect(screen.getByText('Your secure operational workspace is ready.')).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: /skip intro/i }));
    expect(consumeLoginWelcome).toHaveBeenCalledTimes(1);
  });

  it('shows only screening review and clinical configuration cards on the healthcare officer dashboard', () => {
    authState.current = { user: {
      fullName: 'Healthcare Officer',
      email: 'hco@example.com',
      roles: ['ROLE_HEALTHCARE_OFFICER'],
    } };
    renderPage(<StaffDashboardPage />);

    const screeningCard = screen.getByRole('heading', { name: 'Screening Review' }).closest('.card');
    expect(within(screeningCard).getByRole('link', { name: 'Open' }).getAttribute('href'))
      .toBe('/app/staff/screening/reviews');
    const questionsCard = screen.getByRole('heading', { name: 'Screening Questions' }).closest('.card');
    expect(within(questionsCard).getByRole('link', { name: 'Open' }).getAttribute('href'))
      .toBe('/app/screening/questions');
    const questionnairesCard = screen.getByRole('heading', { name: 'Screening Questionnaires' }).closest('.card');
    expect(within(questionnairesCard).getByRole('link', { name: 'Open' }).getAttribute('href'))
      .toBe('/app/screening/questionnaires');
    expect(screen.queryByRole('heading', { name: 'Campaigns' })).toBeNull();
    expect(screen.queryByRole('heading', { name: 'Center Operations' })).toBeNull();
    expect(screen.queryByRole('heading', { name: 'Sessions' })).toBeNull();
  });

  it('renders the vaccinator workspace and draft tools inside the shared staff shell', () => {
    authState.current = { user: {
      fullName: 'Vera Vaccinator',
      email: 'vera@example.gov',
      phoneNumber: '+94 76 100 2000',
      roles: ['ROLE_VACCINATOR'],
    } };
    renderPage(<StaffDashboardPage />);

    expect(screen.getByRole('heading', { name: 'Account Profile' })).toBeTruthy();
    expect(screen.getByText('Vaccinator', { selector: '.admin-role-pill' })).toBeTruthy();
    expect(screen.getByRole('heading', { name: 'Welcome, Vera Vaccinator' })).toBeTruthy();

    const workspace = screen.getByRole('region', { name: /vaccinator workspace/i });
    expect(within(workspace).getByRole('heading', { name: 'Checked-In Queue' })).toBeTruthy();
    expect(within(workspace).getByRole('heading', { name: 'Active Work' })).toBeTruthy();

    const queueCard = screen.getByRole('link', { name: 'Open Check-In Queue' });
    expect(queueCard.getAttribute('href')).toBe('/app/checkin/queue');
    expect(screen.queryByRole('heading', { name: 'Check-In & Queue Management' })).toBeNull();
  });

  it('hides the vaccinator workspace for a Center Manager', () => {
    authState.current = { user: {
      fullName: 'Center Manager',
      email: 'manager@example.com',
      phoneNumber: '+94 71 100 2000',
      roles: ['ROLE_CENTER_MANAGER'],
    } };
    renderPage(<StaffDashboardPage />);

    expect(screen.queryByRole('region', { name: /vaccinator workspace/i })).toBeNull();
    expect(screen.queryByRole('heading', { name: 'My Active Drafts' })).toBeNull();
    expect(screen.getByRole('heading', { name: 'Check-In & Queue Management' })).toBeTruthy();
  });

  it('renders the inventory card for an inventory officer and hides other operational modules', () => {
    authState.current = { user: {
      fullName: 'Ian Inventory',
      email: 'ian@example.gov',
      phoneNumber: '+94 77 900 1000',
      roles: ['ROLE_INVENTORY_OFFICER'],
    } };
    renderPage(<StaffDashboardPage />);

    expect(screen.getByRole('heading', { name: 'Welcome, Ian Inventory' })).toBeTruthy();
    const inventoryCard = screen.getByRole('link', { name: 'Open Inventory & Batches' });
    expect(inventoryCard.getAttribute('href')).toBe('/app/inventory');
    expect(screen.queryByRole('heading', { name: 'Check-In & Queue Management' })).toBeNull();
    expect(screen.queryByRole('heading', { name: 'Check-In Queue' })).toBeNull();
    expect(screen.queryByRole('heading', { name: 'Campaigns' })).toBeNull();
    expect(screen.queryByRole('heading', { name: 'Screening Review' })).toBeNull();
    expect(screen.queryByRole('region', { name: /vaccinator workspace/i })).toBeNull();
  });

  it('renders Check-In & Queue Management and avoids duplicate queue cards for dual Center Manager and Vaccinator roles', () => {
    authState.current = { user: {
      fullName: 'Dual Role Staff',
      email: 'dual@example.gov',
      phoneNumber: '+94 77 555 1234',
      roles: ['ROLE_CENTER_MANAGER', 'ROLE_VACCINATOR'],
    } };
    renderPage(<StaffDashboardPage />);

    expect(screen.getByRole('heading', { name: 'Welcome, Dual Role Staff' })).toBeTruthy();
    expect(screen.getByText('Center Manager · Vaccinator', { selector: '.admin-role-pill' })).toBeTruthy();
    expect(screen.getByRole('heading', { name: 'Check-In & Queue Management' })).toBeTruthy();
    expect(screen.queryByRole('heading', { name: 'Check-In Queue' })).toBeNull();
    const checkInLinks = screen.getAllByRole('link')
      .filter((link) => link.getAttribute('href') === '/app/checkin/queue');
    expect(checkInLinks).toHaveLength(1);
  });

  it('renders administrator profile, staff requests, and operational cards while excluding clinical screening', () => {
    authState.current = { user: {
      fullName: 'Portal Admin',
      email: 'admin@example.com',
      phoneNumber: '+94 70 100 2000',
      roles: ['ROLE_ADMIN'],
    } };
    renderPage(<AdminDashboardPage />);
    expect(screen.getByRole('heading', { name: /Welcome back,.*Portal Admin/ })).toBeTruthy();
    expect(screen.getByText('Administrator', { selector: '.admin-role-pill' })).toBeTruthy();
    expect(screen.getByRole('heading', { name: 'Account Profile' })).toBeTruthy();
    expect(screen.getByText('admin@example.com')).toBeTruthy();
    expect(screen.getByText('+94 70 100 2000')).toBeTruthy();

    const staffRequestsCard = screen.getByRole('heading', { name: 'Staff Account Requests' }).closest('.card');
    expect(staffRequestsCard.getAttribute('href')).toBe('/app/admin/staff-requests');

    const campaignCard = screen.getByRole('heading', { name: 'Campaign Management' }).closest('.card');
    expect(campaignCard.getAttribute('href')).toBe('/app/campaigns');

    const centerCard = screen.getByRole('heading', { name: 'Center Operations' }).closest('.card');
    expect(centerCard.getAttribute('href')).toBe('/app/center-operations');

    const sessionCard = screen.getByRole('heading', { name: 'Session Management' }).closest('.card');
    expect(sessionCard.getAttribute('href')).toBe('/app/sessions');

    const analyticsCard = screen.getByRole('heading', { name: 'Operational Analytics' }).closest('.card');
    expect(analyticsCard.getAttribute('href')).toBe('/app/analytics');

    const inventoryCard = screen.getByRole('heading', { name: 'Inventory & Batches' }).closest('.card');
    expect(inventoryCard.getAttribute('href')).toBe('/app/inventory');

    const checkInCard = screen.getByRole('heading', { name: 'Check-In & Queue Management' }).closest('.card');
    expect(checkInCard.getAttribute('href')).toBe('/app/checkin/queue');

    const openLinks = screen.getAllByRole('link', { name: /^Open / });
    expect(openLinks).toHaveLength(7);

    expect(screen.queryByRole('heading', { name: 'Screening Review Queue' })).toBeNull();
    expect(screen.queryByRole('heading', { name: 'Screening Questions' })).toBeNull();
    expect(screen.queryByRole('heading', { name: 'Screening Questionnaires' })).toBeNull();
  });

  it('renders super administrator profile, operational cards, and screening review queue', () => {
    authState.current = { user: {
      fullName: 'Super Admin User',
      email: 'superadmin@example.com',
      phoneNumber: '+94 70 999 8888',
      roles: ['ROLE_SUPER_ADMIN'],
    } };
    renderPage(<AdminDashboardPage />);
    expect(screen.getByRole('heading', { name: /Welcome back,.*Super Admin User/ })).toBeTruthy();
    expect(screen.getByText('Super Administrator', { selector: '.admin-role-pill' })).toBeTruthy();

    const staffRequestsCard = screen.getByRole('heading', { name: 'Staff Account Requests' }).closest('.card');
    expect(staffRequestsCard.getAttribute('href')).toBe('/app/admin/staff-requests');

    const campaignCard = screen.getByRole('heading', { name: 'Campaign Management' }).closest('.card');
    expect(campaignCard.getAttribute('href')).toBe('/app/campaigns');

    const centerCard = screen.getByRole('heading', { name: 'Center Operations' }).closest('.card');
    expect(centerCard.getAttribute('href')).toBe('/app/center-operations');

    const sessionCard = screen.getByRole('heading', { name: 'Session Management' }).closest('.card');
    expect(sessionCard.getAttribute('href')).toBe('/app/sessions');

    const analyticsCard = screen.getByRole('heading', { name: 'Operational Analytics' }).closest('.card');
    expect(analyticsCard.getAttribute('href')).toBe('/app/analytics');

    const inventoryCard = screen.getByRole('heading', { name: 'Inventory & Batches' }).closest('.card');
    expect(inventoryCard.getAttribute('href')).toBe('/app/inventory');

    const checkInCard = screen.getByRole('heading', { name: 'Check-In & Queue Management' }).closest('.card');
    expect(checkInCard.getAttribute('href')).toBe('/app/checkin/queue');

    const screeningCard = screen.getByRole('heading', { name: 'Screening Review Queue' }).closest('.card');
    expect(screeningCard.getAttribute('href')).toBe('/app/staff/screening/reviews');

    const openLinks = screen.getAllByRole('link', { name: /^Open / });
    expect(openLinks).toHaveLength(8);

    expect(screen.queryByRole('heading', { name: 'Screening Questions' })).toBeNull();
    expect(screen.queryByRole('heading', { name: 'Screening Questionnaires' })).toBeNull();
  });
});
