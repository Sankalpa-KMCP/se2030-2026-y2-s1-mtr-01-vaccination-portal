import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

const mocks = vi.hoisted(() => ({
  user: { id: 71 },
  bookAppointment: vi.fn(),
  getMyAppointments: vi.fn(),
  cancelAppointment: vi.fn(),
  joinWaitlist: vi.fn(),
  getWaitlistStatus: vi.fn(),
  leaveWaitlist: vi.fn(),
  searchAvailableAppointments: vi.fn(),
}));

vi.mock('../../../auth/context/AuthContext', () => ({ useAuth: () => ({ user: mocks.user }) }));
vi.mock('../api/appointmentApi', () => mocks);
vi.mock('../api/waitlistApi', () => mocks);
vi.mock('../api/availabilityApi', () => mocks);

import AppointmentBookingPage from './AppointmentBookingPage';
import MyAppointmentsPage from './MyAppointmentsPage';
import WaitlistPage from './WaitlistPage';

function show(component) {
  return render(<MemoryRouter>{component}</MemoryRouter>);
}

beforeEach(() => {
  vi.resetAllMocks();
  mocks.user = { id: 71 };
  mocks.getMyAppointments.mockResolvedValue([]);
  mocks.getWaitlistStatus.mockResolvedValue(null);
  mocks.searchAvailableAppointments.mockResolvedValue([]);
});

afterEach(cleanup);

describe('citizen appointment pages', () => {
  it('keeps the booking flow accessible and truthful', () => {
    const { container } = show(<AppointmentBookingPage />);
    expect(screen.getByRole('heading', { level: 1, name: 'Book a Vaccination Appointment' })).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Back to Dashboard' }).getAttribute('href')).toBe('/app/citizen');
    expect(screen.getByRole('link', { name: 'View My Appointments' }).getAttribute('href')).toBe('/app/appointments/mine');
    const campaignInput = screen.getByRole('spinbutton', { name: /Campaign ID/i });
    const centerInput = screen.getByRole('spinbutton', { name: /Center ID/i });
    expect(campaignInput).toBeTruthy();
    expect(campaignInput.getAttribute('min')).toBe('1');
    expect(centerInput).toBeTruthy();
    expect(centerInput.getAttribute('min')).toBe('1');
    expect(screen.getByRole('button', { name: 'Search Sessions' })).toBeTruthy();
    expect(screen.getByText('How it works')).toBeTruthy();
    expect(container.querySelector('[aria-current], [data-completed]')).toBeNull();
    expect(screen.queryByText(/nearby|closest|distance/i)).toBeNull();
  });

  it('enforces positive numbers on Center ID and Campaign ID in availability search', () => {
    show(<AppointmentBookingPage />);
    const centerInput = screen.getByRole('spinbutton', { name: /Center ID/i });
    const campaignInput = screen.getByRole('spinbutton', { name: /Campaign ID/i });

    fireEvent.change(centerInput, { target: { value: '-9' } });
    expect(centerInput.value).not.toBe('-9');

    fireEvent.change(centerInput, { target: { value: '0' } });
    expect(centerInput.value).not.toBe('0');

    fireEvent.change(centerInput, { target: { value: '12' } });
    expect(centerInput.value).toBe('12');

    fireEvent.change(campaignInput, { target: { value: '-5' } });
    expect(campaignInput.value).not.toBe('-5');
  });

  it('provides a supported action from the appointments empty state', async () => {
    show(<MyAppointmentsPage />);
    expect(screen.getByRole('heading', { level: 1, name: 'My Appointments' })).toBeTruthy();
    expect(await screen.findByText('No Appointments Found')).toBeTruthy();
    const bookingLinks = screen.getAllByRole('link', { name: 'Book Appointment' });
    expect(bookingLinks).toHaveLength(1);
    expect(bookingLinks[0].getAttribute('href')).toBe('/app/appointments/book');
    expect(screen.queryByText(/upcoming appointment/i)).toBeNull();
  });

  it('retains appointment management actions for confirmed records', async () => {
    mocks.getMyAppointments.mockResolvedValue([{
      id: 84,
      citizenId: 71,
      campaignId: 22,
      slotId: 902,
      bookedAt: '2026-09-03T08:30:00Z',
      status: 'CONFIRMED',
    }]);
    show(<MyAppointmentsPage />);
    expect(await screen.findByRole('button', { name: 'Reschedule' })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Cancel Appointment' })).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Book Appointment' }).getAttribute('href')).toBe('/app/appointments/book');
    expect(screen.queryByText(/center|address/i)).toBeNull();
  });

  it('does not offer a contradictory booking action while appointments load or fail', async () => {
    mocks.getMyAppointments.mockReturnValue(new Promise(() => {}));
    const view = show(<MyAppointmentsPage />);
    expect(screen.queryByRole('link', { name: 'Book Appointment' })).toBeNull();

    view.unmount();
    mocks.getMyAppointments.mockRejectedValue(new Error('Appointments are temporarily unavailable'));
    show(<MyAppointmentsPage />);
    expect(await screen.findByText('Appointments are temporarily unavailable')).toBeTruthy();
    expect(screen.getByRole('alert').className).toContain('citizen-state--error');
    expect(screen.queryByRole('link', { name: 'Book Appointment' })).toBeNull();
  });

  it('keeps waitlist lookup and empty-state actions available', async () => {
    const { container } = show(<WaitlistPage />);
    expect(screen.getByRole('heading', { level: 1, name: 'My Waitlist Status' })).toBeTruthy();
    expect(screen.getByRole('spinbutton', { name: /Campaign ID/i })).toBeTruthy();
    expect(await screen.findByRole('button', { name: 'Check Status' })).toBeTruthy();
    expect(screen.getByText('How it works')).toBeTruthy();
    expect(container.querySelector('[aria-current], [data-completed]')).toBeNull();
    expect(await screen.findByText("You're not on any waitlist")).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Browse Available Sessions' }).getAttribute('href')).toBe('/app/appointments/book');
    expect(screen.queryByText(/estimated time|automatic promotion|notification/i)).toBeNull();

    fireEvent.change(screen.getByRole('spinbutton', { name: /Campaign ID/i }), { target: { value: '9001' } });
    fireEvent.click(await screen.findByRole('button', { name: 'Check Status' }));
    await waitFor(() => expect(mocks.getWaitlistStatus).toHaveBeenCalledWith({ citizenId: 71, campaignId: '9001' }));
  });

  it('retains the leave action for an actual waiting entry', async () => {
    mocks.getWaitlistStatus.mockResolvedValue({
      id: 5,
      citizenId: 71,
      campaignId: 8001,
      sessionId: 44,
      status: 'WAITING',
      position: 2,
      joinedAt: '2026-09-03T08:30:00Z',
    });
    show(<WaitlistPage />);
    expect(await screen.findByRole('button', { name: 'Leave Waitlist' })).toBeTruthy();
    expect(screen.getByText('Waiting in Queue')).toBeTruthy();
  });
});
