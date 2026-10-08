import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  cleanup,
  fireEvent,
  render,
  screen,
  within,
} from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

const mocks = vi.hoisted(() => ({
  auth: { user: null },
  dashboardData: null,
}));

vi.mock('../../../auth/context/AuthContext', () => ({ useAuth: () => mocks.auth }));
vi.mock('../hooks/useCitizenDashboardData', () => ({
  default: () => mocks.dashboardData,
}));

import CitizenDashboardPage from './CitizenDashboardPage';

const emptyData = () => ({
  appointments: { loading: false, data: [], error: null },
  screening: { loading: false, submissions: [], assessments: [], campaigns: [], errors: {} },
});

function renderDashboard({ fromLogin = false } = {}) {
  return render(
    <MemoryRouter initialEntries={[{
      pathname: '/app/citizen',
      state: fromLogin ? { showCitizenWelcome: true } : null,
    }]}
    >
      <CitizenDashboardPage />
    </MemoryRouter>,
  );
}

describe('CitizenDashboardPage', () => {
  beforeEach(() => {
    mocks.auth = {
      loginWelcome: null,
      consumeLoginWelcome: vi.fn(),
      user: {
        id: 71,
        fullName: 'Citizen User',
        email: 'citizen@example.com',
        phoneNumber: '+94 77 123 4567',
        accountStatus: 'ACTIVE',
        roles: ['ROLE_CITIZEN'],
      },
    };
    mocks.dashboardData = emptyData();
  });

  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  it('renders one citizen heading and authenticated profile details', () => {
    renderDashboard();
    expect(screen.getAllByRole('heading', { level: 1 })).toHaveLength(1);
    expect(screen.getByRole('heading', { name: 'Welcome, Citizen User' })).toBeTruthy();
    expect(screen.getAllByText('citizen@example.com').length).toBeGreaterThan(0);
    expect(screen.getByText('+94 77 123 4567')).toBeTruthy();
    expect(screen.getByText('Active')).toBeTruthy();
    expect(screen.getByText('Citizen ID')).toBeTruthy();
    expect(screen.getByText('#71')).toBeTruthy();
    expect(screen.queryByLabelText('Citizen ID')).toBeNull();
    expect(screen.getByRole('link', { name: /view profile/i }).getAttribute('href')).toBe('/app/citizen/profile');
  });

  it('shows the named greeting only after login and clears it when dismissed', () => {
    const view = renderDashboard({ fromLogin: true });
    expect(screen.getByRole('dialog', { name: 'Welcome, Citizen User' })).toBeTruthy();

    fireEvent.click(screen.getByRole('button', { name: /skip intro/i }));
    expect(screen.queryByRole('dialog')).toBeNull();

    view.unmount();
    renderDashboard();
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('shows and consumes the authenticated login welcome when routing replaces login first', () => {
    mocks.auth.loginWelcome = { audience: 'citizen', userId: '71' };
    renderDashboard();

    expect(screen.getByRole('dialog', { name: 'Welcome, Citizen User' })).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: /skip intro/i }));

    expect(mocks.auth.consumeLoginWelcome).toHaveBeenCalledTimes(1);
  });

  it('reflects profile changes from shared authenticated user state', () => {
    const view = renderDashboard();
    mocks.auth = {
      user: {
        ...mocks.auth.user,
        fullName: 'Updated Citizen',
        phoneNumber: '+94 11 222 3333',
      },
    };
    view.rerender(<MemoryRouter><CitizenDashboardPage /></MemoryRouter>);

    expect(screen.getByRole('heading', { name: 'Welcome, Updated Citizen' })).toBeTruthy();
    expect(screen.getByText('+94 11 222 3333')).toBeTruthy();
  });

  it('guides a pending citizen to profile verification without showing the prompt to active citizens', () => {
    mocks.auth.user.accountStatus = 'PENDING_VERIFICATION';
    const view = renderDashboard();
    expect(screen.getByRole('heading', { name: 'Verify your email to activate your account' })).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Verify Email' }).getAttribute('href')).toBe('/app/citizen/profile');

    mocks.auth = { user: { ...mocks.auth.user, accountStatus: 'ACTIVE' } };
    view.rerender(<MemoryRouter><CitizenDashboardPage /></MemoryRouter>);
    expect(screen.queryByRole('heading', { name: 'Verify your email to activate your account' })).toBeNull();
  });

  it('links every supported quick action and exposes no dead or unsupported service', () => {
    renderDashboard();
    const shortcuts = screen.getByRole('navigation', { name: 'Citizen service shortcuts' });
    const expected = [
      ['Start or view screening', '/app/screening'],
      ['My Eligibility', '/app/screening/eligibility'],
      ['Book Appointment', '/app/appointments/book'],
      ['My Appointments', '/app/appointments/mine'],
      ['Screening History', '/app/screening/history'],
      ['Waitlist', '/app/appointments/waitlist'],
    ];
    expected.forEach(([name, path]) => {
      expect(within(shortcuts).getByRole('link', { name: new RegExp(name, 'i') }).getAttribute('href')).toBe(path);
    });
    expect(within(shortcuts).getAllByRole('link')).toHaveLength(6);
    expect(screen.queryByText(/coming soon/i)).toBeNull();
    expect(screen.queryByRole('link', { name: /vaccination history|certificate|notification|edit profile/i })).toBeNull();
  });

  it('shows independent loading states without hiding citizen services', () => {
    mocks.dashboardData = {
      appointments: { loading: true, data: [], error: null },
      screening: { loading: true, submissions: [], assessments: [], campaigns: [], errors: {} },
    };
    renderDashboard();
    expect(screen.getByText('Loading your appointments')).toBeTruthy();
    expect(screen.getByText('Loading screening records')).toBeTruthy();
    expect(screen.getByRole('navigation', { name: 'Citizen service shortcuts' })).toBeTruthy();
  });

  it('shows the appointments empty state with working appointment actions', () => {
    renderDashboard();
    expect(screen.getByText('No appointments recorded')).toBeTruthy();
    expect(screen.getAllByRole('link', { name: /view my appointments/i }).some(
      (link) => link.getAttribute('href') === '/app/appointments/mine',
    )).toBe(true);
    expect(screen.getAllByRole('link', { name: /book an? appointment/i }).some(
      (link) => link.getAttribute('href') === '/app/appointments/book',
    )).toBe(true);
  });

  it('shows the screening empty state with working choose campaign and view eligibility actions', () => {
    renderDashboard();
    expect(screen.getByText('No screening submitted')).toBeTruthy();
    expect(screen.getByText('Choose an available campaign to begin its questionnaire.')).toBeTruthy();
    expect(screen.getAllByRole('link', { name: /choose a campaign/i }).some(
      (link) => link.getAttribute('href') === '/app/screening',
    )).toBe(true);
    expect(screen.getAllByRole('link', { name: /view eligibility/i }).some(
      (link) => link.getAttribute('href') === '/app/screening/eligibility',
    )).toBe(true);
  });

  it('renders only supported appointment fields from a successful response', () => {
    mocks.dashboardData = emptyData();
    mocks.dashboardData.appointments.data = [{
      id: 84,
      status: 'CONFIRMED',
      campaignId: 22,
      slotId: 902,
      bookedAt: '2026-09-03T08:30:00Z',
      startAt: '2099-01-01T10:00:00Z',
      centerName: 'Unsupported Center Name',
      centerAddress: 'Unsupported Center Address',
    }];
    renderDashboard();
    expect(screen.getByRole('heading', { name: 'Appointment #84' })).toBeTruthy();
    expect(screen.getByText('Confirmed')).toBeTruthy();
    expect(screen.getByText('#22')).toBeTruthy();
    expect(screen.getByText('#902')).toBeTruthy();
    expect(screen.queryByText('Unsupported Center Name')).toBeNull();
    expect(screen.queryByText('Unsupported Center Address')).toBeNull();
    expect(screen.queryByText(/2099/)).toBeNull();
    expect(screen.queryByText(/upcoming appointment/i)).toBeNull();
  });

  it('localizes appointment failure while successful screening content remains visible', () => {
    mocks.dashboardData = {
      appointments: { loading: false, data: [], error: new Error('Appointments unavailable') },
      screening: {
        loading: false,
        submissions: [{ id: 12, campaignId: 22, status: 'PENDING_REVIEW', preliminaryResult: 'PRELIMINARY_PASS' }],
        assessments: [],
        campaigns: [{ id: 22, name: 'Community Campaign' }],
        errors: {},
      },
    };
    renderDashboard();
    expect(screen.getByText('Appointments could not be loaded')).toBeTruthy();
    expect(screen.getByRole('heading', { name: 'Community Campaign' })).toBeTruthy();
    expect(screen.getByText('Preliminary screening passed')).toBeTruthy();
    expect(screen.getByText('This is not a final eligibility decision.')).toBeTruthy();
  });

  it('localizes screening failure while successful appointment content remains visible', () => {
    mocks.dashboardData = {
      appointments: { loading: false, data: [{ id: 84, status: 'CONFIRMED', campaignId: 22, slotId: 902 }], error: null },
      screening: {
        loading: false,
        submissions: [],
        assessments: [],
        campaigns: [],
        errors: { submissions: new Error('Screening unavailable') },
      },
    };
    renderDashboard();
    expect(screen.getByText('Screening history could not be loaded')).toBeTruthy();
    expect(screen.getByRole('heading', { name: 'Appointment #84' })).toBeTruthy();
  });

  it('keeps screening visible through optional failures and hides internal clinical notes', () => {
    mocks.dashboardData = {
      appointments: { loading: false, data: [], error: null },
      screening: {
        loading: false,
        submissions: [{ id: 12, campaignId: 22, status: 'REVIEWED', createdAt: '2026-09-01' }],
        assessments: [{ status: 'ELIGIBLE', reviewedAt: '2026-09-02', clinicalNotes: 'INTERNAL SECRET' }],
        campaigns: [],
        errors: { campaigns: new Error('Campaign names unavailable') },
      },
    };
    renderDashboard();
    expect(screen.getByRole('heading', { name: 'Campaign #22' })).toBeTruthy();
    expect(screen.getByText('Eligible')).toBeTruthy();
    expect(screen.getByText(/campaign names are unavailable/i)).toBeTruthy();
    expect(screen.queryByText('INTERNAL SECRET')).toBeNull();
  });

  it('renders localized error instead of confirmed empty state when appointments payload is malformed', () => {
    mocks.dashboardData = {
      appointments: { loading: false, data: [], error: new Error('Malformed appointments data received.') },
      screening: {
        loading: false,
        submissions: [{ id: 12, campaignId: 22, status: 'PENDING_REVIEW', preliminaryResult: 'PRELIMINARY_PASS' }],
        assessments: [],
        campaigns: [{ id: 22, name: 'Community Campaign' }],
        errors: {},
      },
    };
    renderDashboard();
    expect(screen.getByText('Appointments could not be loaded')).toBeTruthy();
    expect(screen.queryByText('No appointments recorded')).toBeNull();
    expect(screen.getByRole('heading', { name: 'Community Campaign' })).toBeTruthy();
  });

  it('renders localized error instead of confirmed empty state when screening submissions payload is malformed', () => {
    mocks.dashboardData = {
      appointments: { loading: false, data: [{ id: 84, status: 'CONFIRMED', campaignId: 22, slotId: 902 }], error: null },
      screening: {
        loading: false,
        submissions: [],
        assessments: [],
        campaigns: [],
        errors: { submissions: new Error('Malformed screening submissions data received.') },
      },
    };
    renderDashboard();
    expect(screen.getByText('Screening history could not be loaded')).toBeTruthy();
    expect(screen.queryByText('No screening submitted')).toBeNull();
    expect(screen.getByRole('heading', { name: 'Appointment #84' })).toBeTruthy();
  });

  it('renders localized warning when assessment payload is malformed while keeping submissions visible', () => {
    mocks.dashboardData = {
      appointments: { loading: false, data: [], error: null },
      screening: {
        loading: false,
        submissions: [{ id: 12, campaignId: 22, status: 'REVIEWED', createdAt: '2026-09-01' }],
        assessments: [],
        campaigns: [{ id: 22, name: 'Community Campaign' }],
        errors: { assessments: new Error('Malformed screening assessments data received.') },
      },
    };
    renderDashboard();
    expect(screen.getByRole('heading', { name: 'Community Campaign' })).toBeTruthy();
    expect(screen.getByText(/recorded assessment information is temporarily unavailable/i)).toBeTruthy();
  });

  it('contains no fabricated metrics or unsupported citizen data', () => {
    renderDashboard();
    expect(screen.queryByText(/health score|notification count|vaccination record|digital certificate/i)).toBeNull();
  });
});
