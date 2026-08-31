import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { registerCitizen } from '../api/authApi';
import CitizenRegistrationPage from './CitizenRegistrationPage';

const auth = vi.hoisted(() => ({
  login: vi.fn(),
  clearLoginError: vi.fn(),
}));

vi.mock('../api/authApi', () => ({ registerCitizen: vi.fn() }));
vi.mock('../context/AuthContext', () => ({ useAuth: () => auth }));

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/register/citizen']}>
      <Routes>
        <Route path="/register/citizen" element={<CitizenRegistrationPage />} />
        <Route path="/app/citizen" element={<h1>Citizen Home</h1>} />
      </Routes>
    </MemoryRouter>,
  );
}

function fillCitizenForm({ confirmPassword = 'Secret123!' } = {}) {
  fireEvent.change(screen.getByLabelText('Full Name'), { target: { value: 'Citizen User' } });
  fireEvent.change(screen.getByLabelText('Email Address'), { target: { value: 'citizen@example.com' } });
  fireEvent.change(screen.getByLabelText('Phone Number'), { target: { value: '+94 77 123 4567' } });
  fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'Secret123!' } });
  fireEvent.change(screen.getByLabelText('Confirm Password'), { target: { value: confirmPassword } });
}

describe('CitizenRegistrationPage', () => {
  beforeEach(() => {
    registerCitizen.mockReset();
    auth.login.mockReset();
    auth.clearLoginError.mockReset();
  });

  afterEach(cleanup);

  it('renders all citizen registration fields', () => {
    renderPage();
    expect(screen.getByRole('heading', { name: 'Create Citizen Account' })).toBeTruthy();
    expect(screen.getByLabelText('Full Name')).toBeTruthy();
    expect(screen.getByLabelText('Email Address')).toBeTruthy();
    expect(screen.getByLabelText('Phone Number')).toBeTruthy();
    expect(screen.getByLabelText('Password')).toBeTruthy();
    expect(screen.getByLabelText('Confirm Password')).toBeTruthy();
  });

  it('blocks mismatched passwords before calling the API', () => {
    renderPage();
    fillCitizenForm({ confirmPassword: 'Different123!' });
    fireEvent.click(screen.getByRole('button', { name: 'Create Citizen Account' }));
    expect(screen.getByRole('alert').textContent).toContain('Passwords do not match');
    expect(registerCitizen).not.toHaveBeenCalled();
  });

  it('registers, automatically signs in, and redirects a citizen', async () => {
    registerCitizen.mockResolvedValue({ id: 1 });
    auth.login.mockResolvedValue({ success: true, user: { roles: ['ROLE_CITIZEN'] } });
    renderPage();
    fillCitizenForm();
    fireEvent.click(screen.getByRole('button', { name: 'Create Citizen Account' }));

    await screen.findByRole('heading', { name: 'Citizen Home' });
    expect(registerCitizen).toHaveBeenCalledTimes(1);
    expect(auth.login).toHaveBeenCalledWith('citizen@example.com', 'Secret123!');
    expect(registerCitizen.mock.calls[0][0]).not.toHaveProperty('confirmPassword');
  });

  it('preserves account-created success when automatic login fails', async () => {
    registerCitizen.mockResolvedValue({ id: 1 });
    auth.login.mockResolvedValue({ success: false, error: 'Login unavailable' });
    renderPage();
    fillCitizenForm();
    fireEvent.click(screen.getByRole('button', { name: 'Create Citizen Account' }));

    expect(await screen.findByText('Account created successfully')).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Go to Citizen Login' })).toBeTruthy();
  });

  it('shows a backend registration error', async () => {
    registerCitizen.mockRejectedValue(new Error('Email is already registered'));
    renderPage();
    fillCitizenForm();
    fireEvent.click(screen.getByRole('button', { name: 'Create Citizen Account' }));

    await waitFor(() => expect(screen.getByRole('alert').textContent).toContain('Email is already registered'));
    expect(auth.login).not.toHaveBeenCalled();
  });
});
