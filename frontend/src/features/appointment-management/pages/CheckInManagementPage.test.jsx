import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
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

function deferred() {
  let resolve;
  let reject;
  const promise = new Promise((resolvePromise, rejectPromise) => {
    resolve = resolvePromise;
    reject = rejectPromise;
  });
  return { promise, resolve, reject };
}

function renderPage() {
  return render(
    <MemoryRouter>
      <CheckInManagementPage />
    </MemoryRouter>,
  );
}

const sampleAppointments = [
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

    // Checked-in queue response should be visible
    await waitFor(() => {
      expect(screen.getByText('Appointment #102')).toBeTruthy();
    });

    // Mutation controls MUST NOT be rendered
    expect(screen.queryByRole('button', { name: 'Check In' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Reverse Check-In' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Mark No-Show' })).toBeNull();
  });

  it.each(['ROLE_ADMIN', 'ROLE_CENTER_MANAGER'])(
    'preserves read/write controls and session lookup for %s',
    async (role) => {
    authMock.user = {
      fullName: 'Authorized Operator',
      roles: [role],
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

    // The production queue contains CHECKED_IN rows, so Reverse is the applicable mutation.
    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Reverse Check-In' })).toBeTruthy();
      expect(screen.queryByRole('button', { name: 'Check In' })).toBeNull();
      expect(screen.queryByRole('button', { name: 'Mark No-Show' })).toBeNull();
    });
    },
  );

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
      expect(screen.queryByText('Queue is Empty')).toBeNull();
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
      expect(screen.getByText(/No checked-in appointments found for Slot #15\./i)).toBeTruthy();
    });
  });

  it('keeps the newest queue when an older slot success resolves afterward', async () => {
    const slotFour = deferred();
    const slotFortyTwo = deferred();
    checkInApiMocks.getVaccinatorQueue
      .mockReturnValueOnce(slotFour.promise)
      .mockReturnValueOnce(slotFortyTwo.promise);

    renderPage();
    const slotInput = screen.getByLabelText(/Active Time Slot ID/i);
    fireEvent.change(slotInput, { target: { value: '4' } });
    fireEvent.change(slotInput, { target: { value: '42' } });

    await waitFor(() => {
      expect(checkInApiMocks.getVaccinatorQueue).toHaveBeenCalledTimes(2);
    });
    slotFortyTwo.resolve(sampleAppointments);
    await waitFor(() => {
      expect(screen.getByText('Vaccinator Queue for Slot #42')).toBeTruthy();
      expect(screen.getByText('Appointment #102')).toBeTruthy();
    });

    await act(async () => {
      slotFour.resolve([{ ...sampleAppointments[0], id: 4, slotId: 4 }]);
      await slotFour.promise;
    });
    await waitFor(() => {
      expect(screen.getByText('Appointment #102')).toBeTruthy();
      expect(screen.queryByText('Appointment #4')).toBeNull();
    });
  });

  it('ignores an older queue failure after a newer slot succeeds', async () => {
    const slotFour = deferred();
    checkInApiMocks.getVaccinatorQueue
      .mockReturnValueOnce(slotFour.promise)
      .mockResolvedValueOnce(sampleAppointments);

    renderPage();
    const slotInput = screen.getByLabelText(/Active Time Slot ID/i);
    fireEvent.change(slotInput, { target: { value: '4' } });
    fireEvent.change(slotInput, { target: { value: '42' } });

    await waitFor(() => {
      expect(screen.getByText('Appointment #102')).toBeTruthy();
    });
    await act(async () => {
      slotFour.reject(new Error('Old slot failed'));
      try {
        await slotFour.promise;
      } catch {
        // The component must ignore the obsolete rejection.
      }
    });

    await waitFor(() => {
      expect(screen.getByText('Appointment #102')).toBeTruthy();
      expect(screen.queryByText('Old slot failed')).toBeNull();
      expect(screen.queryByText('Error Loading Queue')).toBeNull();
    });
  });
});
