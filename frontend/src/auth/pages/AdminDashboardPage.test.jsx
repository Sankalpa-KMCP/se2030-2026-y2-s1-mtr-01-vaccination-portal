import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import AdminDashboardPage from './AdminDashboardPage';

const api = vi.hoisted(() => ({
  listCenters: vi.fn(),
  getOperationalAnalytics: vi.fn(),
  listStaffRequests: vi.fn(),
}));

vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({
    user: {
      id: 12,
      fullName: 'Local E2E Administrator',
      email: 'admin@example.test',
      phoneNumber: '0771000001',
      roles: ['ROLE_ADMIN'],
    },
  }),
}));

vi.mock('../../features/center-operations/api/centerOperationsApi', () => ({
  listCenters: api.listCenters,
}));

vi.mock('../../features/analytics/api/analyticsApi', () => ({
  getOperationalAnalytics: api.getOperationalAnalytics,
}));

vi.mock('../api/adminStaffRequestsApi', () => ({
  listStaffRequests: api.listStaffRequests,
}));

function renderDashboard() {
  return render(<MemoryRouter><AdminDashboardPage /></MemoryRouter>);
}

beforeEach(() => {
  api.listCenters.mockReset().mockResolvedValue([]);
  api.getOperationalAnalytics.mockReset().mockResolvedValue({
    sessionCountByLifecycleStatus: { SCHEDULED: 0 },
    totalApprovedCapacity: 0,
    totalReservedSlots: 0,
  });
  api.listStaffRequests.mockReset().mockResolvedValue([]);
});

afterEach(cleanup);

describe('Administrator Dashboard redesign', () => {
  it('uses the authenticated account values and keeps the existing management destinations', () => {
    renderDashboard();

    expect(screen.getByRole('heading', { level: 1, name: /Welcome back,.*Local E2E Administrator/ })).toBeTruthy();
    const illustration = document.querySelector('.admin-hero__illustration');
    expect(illustration?.tagName).toBe('IMG');
    expect(illustration?.getAttribute('src')).toContain('admin-dashboard-hero-approved.png');
    const profile = screen.getByRole('region', { name: 'Account Profile' });
    expect(within(profile).getByText('Local E2E Administrator')).toBeTruthy();
    expect(within(profile).getByText('admin@example.test')).toBeTruthy();
    expect(within(profile).getByText('0771000001')).toBeTruthy();
    expect(within(profile).getByText('Administrator')).toBeTruthy();

    const destinations = {
      'Staff Account Requests': '/app/admin/staff-requests',
      'Campaign Management': '/app/campaigns',
      'Center Operations': '/app/center-operations',
      'Session Management': '/app/sessions',
      'Operational Analytics': '/app/analytics',
      'Inventory & Batches': '/app/inventory',
      'Check-In & Queue Management': '/app/checkin/queue',
    };
    Object.entries(destinations).forEach(([title, path]) => {
      expect(screen.getByRole('link', { name: `Open ${title}` }).getAttribute('href')).toBe(path);
    });
    expect(screen.queryByRole('link', { name: 'Open Screening Review Queue' })).toBeNull();
    expect(api.listCenters).toHaveBeenCalledWith('ACTIVE');
    expect(api.getOperationalAnalytics).toHaveBeenCalledWith();
    expect(api.listStaffRequests).toHaveBeenCalledWith();
  });

  it('renders actual API figures without reference-image placeholder statistics', async () => {
    api.listCenters.mockResolvedValue([{ id: 1 }, { id: 2 }]);
    api.getOperationalAnalytics.mockResolvedValue({
      sessionCountByLifecycleStatus: { SCHEDULED: 3 },
      totalApprovedCapacity: 240,
      totalReservedSlots: 86,
    });
    api.listStaffRequests.mockResolvedValue([{ id: 10 }]);

    renderDashboard();

    await waitFor(() => {
      expect(screen.getByRole('link', { name: 'Active Centers: 2' })).toBeTruthy();
      expect(screen.getByRole('link', { name: 'Scheduled Sessions: 3' })).toBeTruthy();
      expect(screen.getByRole('link', { name: 'Approved Capacity: 240' })).toBeTruthy();
      expect(screen.getByRole('link', { name: 'Reserved Slots: 86' })).toBeTruthy();
      expect(screen.getByRole('link', { name: 'Pending Staff Requests: 1' })).toBeTruthy();
    });
    expect(screen.getByRole('link', { name: 'Active Centers: 2' }).getAttribute('href')).toBe('/app/center-operations');
    expect(screen.getByRole('link', { name: 'Scheduled Sessions: 3' }).getAttribute('href')).toBe('/app/sessions');
    expect(screen.getByRole('link', { name: 'Approved Capacity: 240' }).getAttribute('href')).toBe('/app/analytics');
    expect(screen.getByRole('link', { name: 'Reserved Slots: 86' }).getAttribute('href')).toBe('/app/analytics');
    expect(screen.getByRole('link', { name: 'Pending Staff Requests: 1' }).getAttribute('href')).toBe('/app/admin/staff-requests');
    expect(screen.queryByText('+12%')).toBeNull();
    expect(screen.queryByText('Citizens Served Today')).toBeNull();
    expect(screen.queryByText('Vaccine Stock Items')).toBeNull();
  });

  it('shows loading, then truthful zero values for empty backend collections', async () => {
    let resolveCenters;
    api.listCenters.mockImplementation(() => new Promise((resolve) => { resolveCenters = resolve; }));

    renderDashboard();
    expect(screen.getByRole('link', { name: 'Active Centers: loading' })).toBeTruthy();
    resolveCenters([]);

    await waitFor(() => {
      expect(screen.getByRole('link', { name: 'Active Centers: 0' })).toBeTruthy();
      expect(screen.getByRole('link', { name: 'Scheduled Sessions: 0' })).toBeTruthy();
      expect(screen.getByRole('link', { name: 'Pending Staff Requests: 0' })).toBeTruthy();
    });
  });

  it('marks failed data sources unavailable without inventing zeros or hiding working metrics', async () => {
    api.listCenters.mockRejectedValue(new Error('Centers unavailable'));
    api.getOperationalAnalytics.mockRejectedValue(new Error('Analytics unavailable'));
    api.listStaffRequests.mockResolvedValue([{ id: 9 }]);

    renderDashboard();

    await waitFor(() => {
      expect(screen.getByRole('link', { name: 'Active Centers: unavailable' })).toBeTruthy();
      expect(screen.getByRole('link', { name: 'Scheduled Sessions: unavailable' })).toBeTruthy();
      expect(screen.getByRole('link', { name: 'Approved Capacity: unavailable' })).toBeTruthy();
      expect(screen.getByRole('link', { name: 'Reserved Slots: unavailable' })).toBeTruthy();
      expect(screen.getByRole('link', { name: 'Pending Staff Requests: 1' })).toBeTruthy();
    });
  });
});
