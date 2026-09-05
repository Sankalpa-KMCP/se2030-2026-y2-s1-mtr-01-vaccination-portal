import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
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
    show(<AppointmentBookingPage />);
    expect(screen.getByRole('heading', { level: 1, name: 'Book a Vaccination Appointment' })).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Back to Dashboard' }).getAttribute('href')).toBe('/app/citizen');
    expect(screen.getByRole('link', { name: 'View My Appointments' }).getAttribute('href')).toBe('/app/appointments/mine');
    expect(screen.getByRole('spinbutton', { name: /Campaign ID/i })).toBeTruthy();
    expect(screen.getByRole('spinbutton', { name: /Center ID/i })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Search Sessions' })).toBeTruthy();
    expect(screen.queryByText(/nearby|closest|distance/i)).toBeNull();
  });

  it('provides a supported action from the appointments empty state', async () => {
    show(<MyAppointmentsPage />);
    expect(screen.getByRole('heading', { level: 1, name: 'My Appointments' })).toBeTruthy();
    expect(await screen.findByText('No Appointments Found')).toBeTruthy();
    expect(screen.getAllByRole('link', { name: 'Book Appointment' }).every(
      (link) => link.getAttribute('href') === '/app/appointments/book',
    )).toBe(true);
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
    expect(screen.queryByText(/center|address/i)).toBeNull();
  });

  it('keeps waitlist lookup and empty-state actions available', async () => {
    show(<WaitlistPage />);
    expect(screen.getByRole('heading', { level: 1, name: 'My Waitlist Status' })).toBeTruthy();
    expect(screen.getByRole('spinbutton', { name: /Campaign ID/i })).toBeTruthy();
    expect(await screen.findByRole('button', { name: 'Check Status' })).toBeTruthy();
    expect(await screen.findByText("You're not on any waitlist")).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Browse Available Sessions' }).getAttribute('href')).toBe('/app/appointments/book');
    expect(screen.queryByText(/estimated time|automatic promotion|notification/i)).toBeNull();
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
