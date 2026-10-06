import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import AppointmentBookingPage from './AppointmentBookingPage';
import { bookAppointment } from '../api/appointmentApi';
import { joinWaitlist } from '../api/waitlistApi';
import { getEmailVerificationStatus } from '../api/emailVerificationApi';

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

vi.mock('../api/emailVerificationApi', () => ({
  getEmailVerificationStatus: vi.fn(),
  verifyEmailOtp: vi.fn(),
  resendEmailOtp: vi.fn(),
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

function verificationRequiredError(message) {
  const error = new Error(message || 'Verify your email before booking an appointment.');
  error.status = 403;
  error.error = 'EMAIL_VERIFICATION_REQUIRED';
  error.code = 'EMAIL_VERIFICATION_REQUIRED';
  return error;
}

describe('AppointmentBookingPage slot-full error detection', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getEmailVerificationStatus.mockResolvedValue({ emailVerified: false, emailVerifiedAt: null });
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

    expect(bookAppointment).toHaveBeenCalledWith({ citizenId: 2, campaignId: 1, slotId: 1 });

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

describe('AppointmentBookingPage email verification handling', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getEmailVerificationStatus.mockResolvedValue({ emailVerified: false, emailVerifiedAt: null });
  });

  afterEach(cleanup);

  it('shows verification-specific feedback and opens the OTP dialog on booking rejection', async () => {
    bookAppointment.mockRejectedValue(verificationRequiredError());

    render(
      <MemoryRouter>
        <AppointmentBookingPage />
      </MemoryRouter>,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Book Test Slot' }));

    expect(await screen.findByText('Email verification required')).toBeTruthy();
    expect(screen.getByText('Verify your email before booking an appointment.')).toBeTruthy();
    expect(screen.queryByText(/Ineligible Citizen/i)).toBeNull();

    fireEvent.click(screen.getByRole('button', { name: 'Verify Email' }));

    expect(await screen.findByRole('dialog', { name: 'Verify your email' })).toBeTruthy();
    expect(getEmailVerificationStatus).toHaveBeenCalledTimes(1);
  });

  it('shows verification-specific feedback and opens the OTP dialog on waitlist rejection', async () => {
    const slotFullError = new Error('Time slot 1 capacity reservation failed with result: SLOT_FULL');
    slotFullError.status = 409;
    slotFullError.error = 'CONFLICT';
    bookAppointment.mockRejectedValue(slotFullError);
    joinWaitlist.mockRejectedValue(verificationRequiredError('Verify your email before joining the appointment waitlist.'));

    render(
      <MemoryRouter>
        <AppointmentBookingPage />
      </MemoryRouter>,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Book Test Slot' }));
    await screen.findByText(/Slot Exhausted/i);
    fireEvent.click(screen.getByRole('button', { name: 'Join Waitlist for This Session' }));

    expect(await screen.findByText('Verify your email before joining the appointment waitlist.')).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: 'Verify Email' }));

    expect(await screen.findByRole('dialog', { name: 'Verify your email' })).toBeTruthy();
  });

  it('keys the verification UX on the machine code, not the backend message text', async () => {
    bookAppointment.mockRejectedValue(verificationRequiredError('completely different wording'));

    render(
      <MemoryRouter>
        <AppointmentBookingPage />
      </MemoryRouter>,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Book Test Slot' }));

    expect(await screen.findByText('Email verification required')).toBeTruthy();
    expect(screen.queryByText('completely different wording')).toBeNull();
    expect(screen.getByRole('button', { name: 'Verify Email' })).toBeTruthy();
  });

  it('keeps the generic 403 ineligibility behavior for unrelated 403 errors', async () => {
    const backendError = new Error('Citizen 2 is not eligible for campaign 1');
    backendError.status = 403;
    backendError.error = 'FORBIDDEN';
    bookAppointment.mockRejectedValue(backendError);

    render(
      <MemoryRouter>
        <AppointmentBookingPage />
      </MemoryRouter>,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Book Test Slot' }));

    expect(await screen.findByText(/Ineligible Citizen/i)).toBeTruthy();
    expect(screen.queryByText('Email verification required')).toBeNull();
    expect(screen.queryByRole('button', { name: 'Verify Email' })).toBeNull();
  });

  it('preserves the availability search context across dialog open and close', async () => {
    bookAppointment.mockRejectedValue(verificationRequiredError());

    render(
      <MemoryRouter>
        <AppointmentBookingPage />
      </MemoryRouter>,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Book Test Slot' }));
    fireEvent.click(await screen.findByRole('button', { name: 'Verify Email' }));

    const dialog = await screen.findByRole('dialog', { name: 'Verify your email' });
    expect(dialog).toBeTruthy();

    fireEvent.click(screen.getByRole('button', { name: 'Close' }));

    expect(screen.queryByRole('dialog', { name: 'Verify your email' })).toBeNull();
    expect(screen.getByTestId('mock-availability-search')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Book Test Slot' })).toBeTruthy();
  });

  it('does not show any verification UI on a successful booking', async () => {
    bookAppointment.mockResolvedValue({ id: 77, status: 'CONFIRMED' });

    render(
      <MemoryRouter>
        <AppointmentBookingPage />
      </MemoryRouter>,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Book Test Slot' }));

    expect(await screen.findByText('Booking Confirmed!')).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Verify Email' })).toBeNull();
    expect(screen.queryByRole('dialog', { name: 'Verify your email' })).toBeNull();
  });
});
