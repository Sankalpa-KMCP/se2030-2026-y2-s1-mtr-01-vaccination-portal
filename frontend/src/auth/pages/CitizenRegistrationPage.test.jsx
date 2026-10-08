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

function fillCitizenForm({
  confirmPassword = 'Secret123!',
  email = 'citizen@example.com',
  phoneNumber = '0771234567',
} = {}) {
  fireEvent.change(screen.getByLabelText('Full Name'), { target: { value: 'Citizen User' } });
  fireEvent.change(screen.getByLabelText('Email Address'), { target: { value: email } });
  fireEvent.change(screen.getByLabelText('Phone Number'), { target: { value: phoneNumber } });
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
    expect(screen.getByRole('heading', { name: 'Create your account' })).toBeTruthy();
    expect(screen.getByLabelText('Full Name')).toBeTruthy();
    expect(screen.getByLabelText('Email Address')).toBeTruthy();
    expect(screen.getByLabelText('Phone Number')).toBeTruthy();
    expect(screen.getByLabelText('Phone Number').maxLength).toBe(10);
    expect(screen.getByLabelText('Phone Number').inputMode).toBe('numeric');
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

  it('blocks passwords shorter than 8 characters before calling the API', () => {
    renderPage();
    fireEvent.change(screen.getByLabelText('Full Name'), { target: { value: 'Citizen User' } });
    fireEvent.change(screen.getByLabelText('Email Address'), { target: { value: 'citizen@example.com' } });
    fireEvent.change(screen.getByLabelText('Phone Number'), { target: { value: '0771234567' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'short' } });
    fireEvent.change(screen.getByLabelText('Confirm Password'), { target: { value: 'short' } });
    fireEvent.click(screen.getByRole('button', { name: 'Create Citizen Account' }));
    expect(screen.getByRole('alert').textContent).toBe('Password must be between 8 and 128 characters.');
    expect(registerCitizen).not.toHaveBeenCalled();
  });

  it.each(['student', 'student@', '@gmail.com', 'student gmail.com', 'student@domain'])(
    'blocks malformed email %s and preserves the entered value',
    (email) => {
      renderPage();
      fillCitizenForm({ email });
      fireEvent.click(screen.getByRole('button', { name: 'Create Citizen Account' }));

      expect(screen.getByRole('alert').textContent).toContain('Enter a valid email address.');
      expect(screen.getByLabelText('Email Address').value).toBe(email);
      expect(registerCitizen).not.toHaveBeenCalled();
    },
  );

  it.each(['771234567', '07712345678', '07712ABC67', '077-123-4567', '+94771234567', '077 1234567'])(
    'blocks invalid phone number %s and preserves the entered value',
    (phoneNumber) => {
      renderPage();
      fillCitizenForm({ phoneNumber });
      fireEvent.click(screen.getByRole('button', { name: 'Create Citizen Account' }));

      expect(screen.getByRole('alert').textContent).toContain('Phone number must contain exactly 10 digits.');
      expect(screen.getByLabelText('Phone Number').value).toBe(phoneNumber);
      expect(registerCitizen).not.toHaveBeenCalled();
    },
  );

  it('registers a citizen and displays OTP verification guidance for the login step', async () => {
    registerCitizen.mockResolvedValue({ id: 1 });
    renderPage();
    fillCitizenForm();
    fireEvent.click(screen.getByRole('button', { name: 'Create Citizen Account' }));

    await screen.findByRole('heading', { name: 'Verify your email to activate your account.' });
    expect(registerCitizen).toHaveBeenCalledTimes(1);
    expect(registerCitizen).toHaveBeenCalledWith({
      fullName: 'Citizen User',
      email: 'citizen@example.com',
      phoneNumber: '0771234567',
      password: 'Secret123!',
    });
    expect(registerCitizen.mock.calls[0][0]).not.toHaveProperty('confirmPassword');
    expect(screen.getByText(/Sign in, then open your profile to enter a verification code/i)).toBeTruthy();
    expect(screen.queryByText(/click the verification link/i)).toBeNull();
    expect(screen.getByRole('link', { name: 'Go to Citizen Login' }).getAttribute('href')).toBe('/login');
  });

  it('shows a backend registration error', async () => {
    registerCitizen.mockRejectedValue(new Error('Email is already registered'));
    renderPage();
    fillCitizenForm();
    fireEvent.click(screen.getByRole('button', { name: 'Create Citizen Account' }));

    await waitFor(() => expect(screen.getByRole('alert').textContent).toContain('Email is already registered'));
  });
});
