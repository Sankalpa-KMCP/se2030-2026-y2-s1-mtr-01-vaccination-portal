import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import AppointmentBookingPage from './AppointmentBookingPage';
import { bookAppointment } from '../api/appointmentApi';
import { joinWaitlist } from '../api/waitlistApi';

const authUser = { id: 2, fullName: 'Test Citizen', email: 'citizen@example.com' };

vi.mock('../../../auth/context/AuthContext', () => ({
  useAuth: () => ({ user: authUser }),
}));

vi.mock('../api/appointmentApi', () => ({
  bookAppointment: vi.fn(),
}));

vi.mock('../api/waitlistApi', () => ({
  joinWaitlist: vi.fn(),
}));

vi.mock('../components/AvailabilitySearch', () => ({
  default: ({ onBookSession }) => (
    <div data-testid="mock-availability-search">
      <button
        type="button"
        onClick={() => onBookSession({ slotId: 1, campaignId: 1, sessionId: 101, centerName: 'Center 1' })}
      >
        Book Test Slot
      </button>
    </div>
  ),
}));

describe('AppointmentBookingPage slot-full error detection', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(cleanup);

  it('renders "Join Waitlist for This Session" button when backend returns real SLOT_FULL error', async () => {
    const backendError = new Error('Time slot 1 capacity reservation failed with result: SLOT_FULL');
    backendError.status = 409;
    backendError.error = 'CONFLICT';
    bookAppointment.mockRejectedValue(backendError);

    render(
      <MemoryRouter>
        <AppointmentBookingPage />
      </MemoryRouter>,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Book Test Slot' }));

    await waitFor(() => {
      expect(screen.getByText(/Slot Exhausted: This time slot has no remaining capacity/i)).toBeTruthy();
    });

    const waitlistButton = screen.getByRole('button', { name: 'Join Waitlist for This Session' });
    expect(waitlistButton).toBeTruthy();

    fireEvent.click(waitlistButton);

    await waitFor(() => {
      expect(joinWaitlist).toHaveBeenCalledWith({
        citizenId: 2,
        campaignId: 1,
        sessionId: 101,
      });
    });
  });

  it('handles active appointment duplicate error without showing waitlist button', async () => {
    const backendError = new Error('Citizen 2 already has an active appointment for campaign 1');
    backendError.status = 409;
    backendError.error = 'CONFLICT';
    bookAppointment.mockRejectedValue(backendError);

    render(
      <MemoryRouter>
        <AppointmentBookingPage />
      </MemoryRouter>,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Book Test Slot' }));

    await waitFor(() => {
      expect(screen.getByText(/Duplicate Booking: You already hold an active appointment/i)).toBeTruthy();
    });

    expect(screen.queryByRole('button', { name: 'Join Waitlist for This Session' })).toBeNull();
  });
});
