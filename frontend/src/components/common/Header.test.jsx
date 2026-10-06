import React from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Header from './Header';

vi.mock('../../auth/context/AuthContext', () => ({
  useAuth: vi.fn(),
}));

import { useAuth } from '../../auth/context/AuthContext';

const CITIZEN_USER = {
  id: 1,
  fullName: 'Test Citizen',
  email: 'citizen@test.local',
  roles: ['ROLE_CITIZEN'],
};

const ADMIN_USER = {
  id: 2,
  fullName: 'Test Admin',
  email: 'admin@test.local',
  roles: ['ROLE_ADMIN'],
};

const SYSTEM_ADMIN_USER = {
  id: 3,
  fullName: 'System Administrator',
  email: 'system-admin@test.local',
  roles: ['ROLE_SYSTEM_ADMINISTRATOR'],
};

function renderHeader(path = '/app/citizen', onLogout) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Header onLogout={onLogout} />
    </MemoryRouter>,
  );
}

function mockUser(user) {
  useAuth.mockReturnValue({ user, logout: vi.fn() });
}

describe('Header', () => {
  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  it('shows citizen destinations and hides staff-only destinations for a citizen', () => {
    mockUser(CITIZEN_USER);
    renderHeader('/app/citizen');

    const dashboard = screen.getByText('Dashboard');
    expect(dashboard.getAttribute('href')).toBe('/app/citizen');
    expect(screen.getByText('Book Appointment').getAttribute('href')).toBe('/app/appointments/book');
    expect(screen.getByText('My Appointments').getAttribute('href')).toBe('/app/appointments/mine');
    expect(screen.getByText('Waitlist').getAttribute('href')).toBe('/app/appointments/waitlist');
    expect(screen.queryByText('Campaigns')).toBeNull();
    expect(screen.queryByText('Staff Requests')).toBeNull();
    expect(screen.queryByText('Screening Questions')).toBeNull();
    expect(screen.queryByText('Screening Review')).toBeNull();
    expect(screen.queryByText('Inventory')).toBeNull();
  });

  it('marks the active destination and keeps authorization visibility for an admin', () => {
    mockUser(ADMIN_USER);
    renderHeader('/app/admin');

    expect(screen.getByText('Dashboard').getAttribute('href')).toBe('/app/admin');
    expect(screen.getByText('Dashboard').className).toContain('active');
    expect(screen.getByText('Campaigns').getAttribute('href')).toBe('/app/campaigns');
    expect(screen.getByText('Center Operations')).toBeTruthy();
    expect(screen.getByText('Sessions')).toBeTruthy();
    expect(screen.queryByText('Check-In Queue')).toBeNull();
    expect(screen.getByText('Analytics')).toBeTruthy();
    expect(screen.getByText('Inventory').getAttribute('href')).toBe('/app/inventory');
    expect(screen.getByText('Staff Requests').getAttribute('href')).toBe('/app/admin/staff-requests');
    expect(screen.getByText('Correction Requests').getAttribute('href')).toBe('/app/admin/correction-requests');
    expect(screen.queryByText('Screening Questions')).toBeNull();
    expect(screen.queryByText('Questionnaires')).toBeNull();
    expect(screen.queryByText('Book Appointment')).toBeNull();
    expect(screen.queryByText('Waitlist')).toBeNull();
    expect(screen.queryByText('Screening Review')).toBeNull();
  });

  it('shows vaccination links and hides check-in queue for a vaccinator', () => {
    mockUser({
      id: 7,
      fullName: 'Vera Vaccinator',
      email: 'vaccinator@test.local',
      roles: ['ROLE_VACCINATOR'],
    });
    renderHeader('/app/staff');

    expect(screen.getByText('Dashboard').getAttribute('href')).toBe('/app/staff');
    expect(screen.getByText('Vaccination Drafts').getAttribute('href')).toBe('/app/staff/vaccination-drafts');
    expect(screen.getByText('Vaccination Records').getAttribute('href')).toBe('/app/staff/vaccination-records');
    expect(screen.getByText('Correction Requests').getAttribute('href')).toBe('/app/staff/correction-requests');
    expect(screen.queryByText('Check-In Queue')).toBeNull();
  });

  it('does not show screening configuration links to a system administrator', () => {
    mockUser(SYSTEM_ADMIN_USER);
    renderHeader('/app/screening/questions');

    expect(screen.queryByText('Screening Questions')).toBeNull();
    expect(screen.queryByText('Questionnaires')).toBeNull();
    expect(screen.queryByText('Staff Requests')).toBeNull();
    expect(screen.queryByText('Campaigns')).toBeNull();
  });

  it('shows only screening links in navbar for a healthcare officer and hides operational links', () => {
    mockUser({
      id: 3,
      fullName: 'Healthcare Officer',
      email: 'hco@test.local',
      roles: ['ROLE_HEALTHCARE_OFFICER'],
    });
    renderHeader('/app/staff/screening/reviews');

    expect(screen.getByText('Dashboard').getAttribute('href')).toBe('/app/staff');
    const reviewLink = screen.getByText('Screening Review');
    expect(reviewLink.getAttribute('href')).toBe('/app/staff/screening/reviews');
    expect(reviewLink.className).toContain('active');
    expect(screen.getByText('Screening Questions').getAttribute('href')).toBe('/app/screening/questions');
    expect(screen.getByText('Questionnaires').getAttribute('href')).toBe('/app/screening/questionnaires');
    expect(screen.queryByText('Campaigns')).toBeNull();
    expect(screen.queryByText('Center Operations')).toBeNull();
    expect(screen.queryByText('Sessions')).toBeNull();
    expect(screen.queryByText('Check-In Queue')).toBeNull();
    expect(screen.queryByText('Inventory')).toBeNull();
  });

  it('shows inventory navigation and hides screening review for an inventory officer', () => {
    mockUser({
      id: 4,
      fullName: 'Inventory Officer',
      email: 'inventory@test.local',
      roles: ['ROLE_INVENTORY_OFFICER'],
    });
    renderHeader('/app/staff');

    expect(screen.getByText('Inventory').getAttribute('href')).toBe('/app/inventory');
    expect(screen.queryByText('Screening Review')).toBeNull();
    expect(screen.queryByText('Campaigns')).toBeNull();
    expect(screen.queryByText('Check-In Queue')).toBeNull();
  });

  it('shows inventory navigation to a super administrator via implied roles', () => {
    mockUser({
      id: 5,
      fullName: 'Super Administrator',
      email: 'superadmin@test.local',
      roles: ['ROLE_SUPER_ADMIN'],
    });
    renderHeader('/app/admin');

    expect(screen.getByText('Inventory').getAttribute('href')).toBe('/app/inventory');
  });

  it('hides check-in queue and inventory navigation in navbar for a center manager', () => {
    mockUser({
      id: 6,
      fullName: 'Center Manager',
      email: 'manager@test.local',
      roles: ['ROLE_CENTER_MANAGER'],
    });
    renderHeader('/app/staff');

    expect(screen.queryByText('Check-In Queue')).toBeNull();
    expect(screen.queryByText('Inventory')).toBeNull();
  });

  it('opens the mobile menu from the collapsed state', () => {
    mockUser(CITIZEN_USER);
    renderHeader('/app/citizen');

    const toggler = screen.getByRole('button', { name: 'Toggle navigation' });
    expect(toggler.getAttribute('aria-expanded')).toBe('false');

    fireEvent.click(toggler);

    expect(toggler.getAttribute('aria-expanded')).toBe('true');
    const menu = document.getElementById('appPrimaryNav');
    expect(menu.className).toContain('show');
  });

  it('closes the mobile menu when the toggler is activated again', () => {
    mockUser(CITIZEN_USER);
    renderHeader('/app/citizen');

    const toggler = screen.getByRole('button', { name: 'Toggle navigation' });
    fireEvent.click(toggler);
    expect(toggler.getAttribute('aria-expanded')).toBe('true');

    fireEvent.click(toggler);

    expect(toggler.getAttribute('aria-expanded')).toBe('false');
    const menu = document.getElementById('appPrimaryNav');
    expect(menu.className).not.toContain('show');
  });

  it('closes the open mobile menu after selecting a destination', () => {
    mockUser(CITIZEN_USER);
    renderHeader('/app/citizen');

    const toggler = screen.getByRole('button', { name: 'Toggle navigation' });
    fireEvent.click(toggler);

    fireEvent.click(screen.getByText('Book Appointment'));

    expect(toggler.getAttribute('aria-expanded')).toBe('false');
    expect(document.getElementById('appPrimaryNav').className).not.toContain('show');
  });

  it('invokes the voluntary logout handler from the header button', () => {
    mockUser(CITIZEN_USER);
    const onLogout = vi.fn();
    renderHeader('/app/citizen', onLogout);

    fireEvent.click(screen.getByRole('button', { name: 'Logout' }));

    expect(onLogout).toHaveBeenCalledTimes(1);
  });
});
