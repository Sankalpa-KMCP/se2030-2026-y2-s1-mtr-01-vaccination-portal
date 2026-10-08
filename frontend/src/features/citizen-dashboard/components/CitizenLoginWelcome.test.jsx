import React from 'react';
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import CitizenLoginWelcome, {
  CITIZEN_WELCOME_DURATION_MS,
  CITIZEN_WELCOME_REDUCED_MOTION_DURATION_MS,
} from './CitizenLoginWelcome';

const authState = vi.hoisted(() => ({
  user: {
    id: 42,
    fullName: 'Nimali Perera',
    email: 'nimali@example.gov',
    roles: ['ROLE_CITIZEN'],
  },
}));

vi.mock('../../../auth/context/AuthContext', () => ({
  useAuth: () => ({ user: authState.user }),
}));

function renderWelcome({ fromLogin = true } = {}) {
  return render(
    <MemoryRouter initialEntries={[{
      pathname: '/app/citizen/welcome',
      state: fromLogin ? { fromCitizenLogin: true } : null,
    }]}
    >
      <Routes>
        <Route path="/app/citizen/welcome" element={<CitizenLoginWelcome />} />
        <Route path="/app/citizen" element={<h1>Citizen Dashboard</h1>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('CitizenLoginWelcome', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.stubGlobal('matchMedia', vi.fn(() => ({ matches: false })));
  });

  afterEach(() => {
    cleanup();
    vi.useRealTimers();
    vi.unstubAllGlobals();
  });

  it('welcomes the authenticated citizen by name and continues automatically', () => {
    renderWelcome();

    expect(screen.getByRole('heading', { name: 'Welcome, Nimali Perera' })).toBeTruthy();
    expect(screen.getByText('Secure sign-in successful')).toBeTruthy();
    expect(screen.getByRole('button', { name: /continue to dashboard/i })).toBeTruthy();

    act(() => vi.advanceTimersByTime(CITIZEN_WELCOME_DURATION_MS - 1));
    expect(screen.queryByRole('heading', { name: 'Citizen Dashboard' })).toBeNull();

    act(() => vi.advanceTimersByTime(1));
    expect(screen.getByRole('heading', { name: 'Citizen Dashboard' })).toBeTruthy();
  });

  it('allows the citizen to continue immediately', () => {
    renderWelcome();
    fireEvent.click(screen.getByRole('button', { name: /continue to dashboard/i }));
    expect(screen.getByRole('heading', { name: 'Citizen Dashboard' })).toBeTruthy();
  });

  it('does not replay when the welcome URL was not reached from login', async () => {
    vi.useRealTimers();
    renderWelcome({ fromLogin: false });
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Citizen Dashboard' })).toBeTruthy();
    });
  });

  it('uses a shorter display time when reduced motion is requested', () => {
    vi.stubGlobal('matchMedia', vi.fn(() => ({ matches: true })));
    renderWelcome();

    act(() => vi.advanceTimersByTime(CITIZEN_WELCOME_REDUCED_MOTION_DURATION_MS));
    expect(screen.getByRole('heading', { name: 'Citizen Dashboard' })).toBeTruthy();
  });
});
