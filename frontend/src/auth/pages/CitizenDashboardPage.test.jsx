import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

const mocks = vi.hoisted(() => ({
  auth: { user: null },
  dashboardData: null,
}));

vi.mock('../context/AuthContext', () => ({ useAuth: () => mocks.auth }));
vi.mock('../../features/citizen-dashboard/hooks/useCitizenDashboardData', () => ({
  default: () => mocks.dashboardData,
}));

import CitizenDashboardPage from './CitizenDashboardPage';

const emptyData = () => ({
  appointments: { loading: false, data: [], error: null },
  screening: { loading: false, submissions: [], assessments: [], campaigns: [], errors: {} },
});

function renderDashboard() {
  return render(<MemoryRouter><CitizenDashboardPage /></MemoryRouter>);
}

describe('CitizenDashboardPage', () => {
  beforeEach(() => {
    mocks.auth = {
      user: {
        id: 71,
        fullName: 'Citizen User',
        email: 'citizen@example.com',
        phoneNumber: '+94 77 123 4567',
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

  it('contains no fabricated metrics or unsupported citizen data', () => {
    renderDashboard();
    expect(screen.queryByText(/health score|notification count|vaccination record|digital certificate/i)).toBeNull();
  });
});
