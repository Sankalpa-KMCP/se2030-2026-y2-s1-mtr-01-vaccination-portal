import React from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import CitizenDashboardPage from './CitizenDashboardPage';
import StaffDashboardPage from './StaffDashboardPage';
import AdminDashboardPage from './AdminDashboardPage';

const authState = vi.hoisted(() => ({ current: null }));
vi.mock('../context/AuthContext', () => ({ useAuth: () => authState.current }));

function renderPage(page) {
  return render(<MemoryRouter>{page}</MemoryRouter>);
}

afterEach(cleanup);

describe('role dashboards', () => {
  it('enables screening, eligibility and screening history while preserving future cards', () => {
    authState.current = { user: {
      fullName: 'Citizen User',
      email: 'citizen@example.com',
      phoneNumber: '+94 77 123 4567',
      roles: ['ROLE_CITIZEN'],
    } };
    renderPage(<CitizenDashboardPage />);
    expect(screen.getByRole('heading', { name: 'Welcome, Citizen User' })).toBeTruthy();
    expect(screen.getByText('citizen@example.com')).toBeTruthy();
    expect(screen.getAllByRole('link', { name: 'Open' })).toHaveLength(3);
    for (const [title, path] of [['Pre-Vaccination Screening', '/app/screening'], ['My Eligibility', '/app/screening/eligibility'], ['Screening History', '/app/screening/history']]) {
      const card = screen.getByRole('heading', { name: title }).closest('.card');
      expect(within(card).getByRole('link', { name: 'Open' }).getAttribute('href')).toBe(path);
    }
    expect(screen.getAllByText('Coming soon')).toHaveLength(2);
    for (const title of ['My Appointments', 'Vaccination History']) {
      const card = screen.getByRole('heading', { name: title }).closest('.card');
      expect(within(card).getByText('Coming soon')).toBeTruthy();
      expect(within(card).queryByRole('link')).toBeNull();
    }
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
  });
});
