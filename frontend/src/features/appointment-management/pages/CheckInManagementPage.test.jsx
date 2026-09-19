import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import CheckInManagementPage from './CheckInManagementPage';

const authMock = vi.hoisted(() => ({
  user: null,
}));

const checkInApiMocks = vi.hoisted(() => ({
  getVaccinatorQueue: vi.fn(),
  checkIn: vi.fn(),
  reverseCheckIn: vi.fn(),
  markNoShow: vi.fn(),
}));

const sessionApiMocks = vi.hoisted(() => ({
  listTimeSlots: vi.fn(),
}));

vi.mock('../../../auth/context/AuthContext', () => ({
  useAuth: () => ({ user: authMock.user }),
}));

vi.mock('../api/checkInApi', () => checkInApiMocks);
vi.mock('../../session-management/api/sessionApi', () => sessionApiMocks);

function renderPage() {
  return render(
    <MemoryRouter>
      <CheckInManagementPage />
    </MemoryRouter>,
  );
}

const sampleAppointments = [
  {
    id: 101,
    citizenId: 501,
    campaignId: 301,
    slotId: 42,
    status: 'CONFIRMED',
    bookedAt: '2026-09-19T09:00:00Z',
  },
  {
    id: 102,
    citizenId: 502,
    campaignId: 301,
    slotId: 42,
    status: 'CHECKED_IN',
    bookedAt: '2026-09-19T09:15:00Z',
  },
];

describe('CheckInManagementPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    authMock.user = {
      fullName: 'Vera Vaccinator',
      email: 'vera@example.gov',
      roles: ['ROLE_VACCINATOR'],
    };
    checkInApiMocks.getVaccinatorQueue.mockResolvedValue([]);
    sessionApiMocks.listTimeSlots.mockResolvedValue([]);
  });

  afterEach(cleanup);

  it('renders read-only queue for ROLE_VACCINATOR without mutation buttons or session lookup', async () => {
    authMock.user = {
      fullName: 'Vera Vaccinator',
      roles: ['ROLE_VACCINATOR'],
    };
    checkInApiMocks.getVaccinatorQueue.mockResolvedValue(sampleAppointments);

    renderPage();

    // Check vaccinator-specific subtitle
    expect(
      screen.getByText('Monitor the on-site clinic queue for your active session slot.'),
    ).toBeTruthy();

    // Session lookup helper must NOT be rendered for vaccinator
    expect(
      screen.queryByText(/Don't know the Time Slot ID\? Find slots by Session ID/i),
    ).toBeNull();

    // Enter Slot ID (which triggers fetchQueue via useEffect)
    const slotInput = screen.getByLabelText(/Active Time Slot ID/i);
    fireEvent.change(slotInput, { target: { value: '42' } });

    await waitFor(() => {
      expect(checkInApiMocks.getVaccinatorQueue).toHaveBeenCalledWith('42');
    });

    // Both appointments should be visible
    await waitFor(() => {
      expect(screen.getByText('Appointment #101')).toBeTruthy();
      expect(screen.getByText('Appointment #102')).toBeTruthy();
    });

    // Mutation controls MUST NOT be rendered
    expect(screen.queryByRole('button', { name: 'Check In' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Reverse Check-In' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Mark No-Show' })).toBeNull();
  });

  it('renders operational mutation controls and session lookup for ROLE_ADMIN and ROLE_CENTER_MANAGER', async () => {
    authMock.user = {
      fullName: 'Manager Mike',
      roles: ['ROLE_CENTER_MANAGER'],
    };
    checkInApiMocks.getVaccinatorQueue.mockResolvedValue(sampleAppointments);

    renderPage();

    // Check manager/admin subtitle
    expect(
      screen.getByText(
        'Monitor the on-site clinic queue, process arrival check-ins, apply authorized overrides, or record no-shows.',
      ),
    ).toBeTruthy();

    // Session lookup helper MUST be rendered
    expect(
      screen.getByText(/Don't know the Time Slot ID\? Find slots by Session ID/i),
    ).toBeTruthy();

    // Enter Slot ID
    const slotInput = screen.getByLabelText(/Active Time Slot ID/i);
    fireEvent.change(slotInput, { target: { value: '42' } });

    await waitFor(() => {
      expect(checkInApiMocks.getVaccinatorQueue).toHaveBeenCalledWith('42');
    });

    // Both mutation buttons must be present once loaded
    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Check In' })).toBeTruthy();
      expect(screen.getByRole('button', { name: 'Mark No-Show' })).toBeTruthy();
      expect(screen.getByRole('button', { name: 'Reverse Check-In' })).toBeTruthy();
    });
  });

  it('displays error feedback when getVaccinatorQueue fails', async () => {
    authMock.user = {
      fullName: 'Vera Vaccinator',
      roles: ['ROLE_VACCINATOR'],
    };
    checkInApiMocks.getVaccinatorQueue.mockRejectedValue(new Error('Network error loading queue'));

    renderPage();

    const slotInput = screen.getByLabelText(/Active Time Slot ID/i);
    fireEvent.change(slotInput, { target: { value: '99' } });

    await waitFor(() => {
      expect(screen.getByText('Error Loading Queue')).toBeTruthy();
      expect(screen.getByText('Network error loading queue')).toBeTruthy();
    });
  });

  it('displays empty state when queue is empty', async () => {
    authMock.user = {
      fullName: 'Vera Vaccinator',
      roles: ['ROLE_VACCINATOR'],
    };
    checkInApiMocks.getVaccinatorQueue.mockResolvedValue([]);

    renderPage();

    const slotInput = screen.getByLabelText(/Active Time Slot ID/i);
    fireEvent.change(slotInput, { target: { value: '15' } });

    await waitFor(() => {
      expect(screen.getByText('Queue is Empty')).toBeTruthy();
      expect(screen.getByText(/No active confirmed or checked-in appointments found for Slot #15\./i)).toBeTruthy();
    });
  });
});
