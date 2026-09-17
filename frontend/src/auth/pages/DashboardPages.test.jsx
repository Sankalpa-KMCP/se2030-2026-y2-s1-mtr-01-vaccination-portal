import React from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import CitizenDashboardPage from './CitizenDashboardPage';
import StaffDashboardPage from './StaffDashboardPage';
import AdminDashboardPage from './AdminDashboardPage';

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
    expect(screen.getAllByRole('link', { name: 'Open', exact: true }).length).toBeGreaterThan(0);
    expect(screen.queryByText('Screening Review')).toBeNull();
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

  it('shows the administrator profile and staff request entry', () => {
    authState.current = { user: {
      fullName: 'Portal Admin',
      email: 'admin@example.com',
      phoneNumber: '+94 70 100 2000',
      roles: ['ROLE_ADMIN'],
    } };
    renderPage(<AdminDashboardPage />);
    expect(screen.getByRole('heading', { name: 'Welcome, Portal Admin' })).toBeTruthy();
    expect(screen.getByRole('heading', { name: 'Staff Account Requests' })).toBeTruthy();
    expect(screen.getAllByRole('link', { name: 'Open' }).some(
      (link) => link.getAttribute('href') === '/app/admin/staff-requests',
    )).toBe(true);
    expect(screen.queryByRole('heading', { name: 'Screening Questions' })).toBeNull();
    expect(screen.queryByRole('heading', { name: 'Screening Questionnaires' })).toBeNull();
  });
});
