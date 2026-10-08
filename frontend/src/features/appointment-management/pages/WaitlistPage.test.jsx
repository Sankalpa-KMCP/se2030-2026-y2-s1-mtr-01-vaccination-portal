import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import WaitlistPage from './WaitlistPage';
import { getWaitlistStatus } from '../api/waitlistApi';

const authUser = { id: 2, fullName: 'Test Citizen', email: 'citizen@example.com' };

vi.mock('../../../auth/context/AuthContext', () => ({
  useAuth: () => ({ user: authUser }),
}));

vi.mock('../api/waitlistApi', () => ({
  getWaitlistStatus: vi.fn(),
  leaveWaitlist: vi.fn(),
}));

describe('WaitlistPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(cleanup);

  it('renders promoted success banner with link to My Appointments when status is PROMOTED', async () => {
    getWaitlistStatus.mockResolvedValue({
      id: 10,
      citizenId: 2,
      campaignId: 8001,
      sessionId: 4001,
      status: 'PROMOTED',
      joinedAt: '2026-09-11T12:00:00Z',
      position: null,
    });

    render(
      <MemoryRouter>
        <WaitlistPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByText(/You've been promoted!/i)).toBeTruthy();
      expect(screen.getByText(/Check My Appointments for your confirmed slot/i)).toBeTruthy();
    });

    const appointmentsLink = screen.getByRole('link', { name: 'View My Appointments' });
    expect(appointmentsLink.getAttribute('href')).toBe('/app/appointments/mine');
    expect(screen.getByText('Promoted to Appointment')).toBeTruthy();
  });

  it('renders clean empty state without error alert when status is 404', async () => {
    const notFoundError = new Error('The requested resource could not be found.');
    notFoundError.status = 404;
    getWaitlistStatus.mockRejectedValue(notFoundError);

    render(
      <MemoryRouter>
        <WaitlistPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByText("You're not on any waitlist")).toBeTruthy();
    });

    expect(screen.queryByText('Error Loading Waitlist Status')).toBeNull();
    expect(screen.getByRole('link', { name: 'Browse Available Sessions' })).toBeTruthy();
  });
});
