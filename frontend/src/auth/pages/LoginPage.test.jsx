import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import LoginPage from './LoginPage';
import { resendVerification } from '../api/authApi';

const authState = vi.hoisted(() => ({ current: null }));
vi.mock('../context/AuthContext', () => ({ useAuth: () => authState.current }));
vi.mock('../api/authApi', () => ({ resendVerification: vi.fn() }));

function renderPage(path = '/login') {
  return render(<MemoryRouter initialEntries={[path]}><LoginPage /></MemoryRouter>);
}

describe('LoginPage universal experience', () => {
  beforeEach(() => {
    authState.current = {
      login: vi.fn(),
      loginError: null,
      clearLoginError: vi.fn(),
    };
    resendVerification.mockReset();
  });

  afterEach(cleanup);

  it('renders the single universal login experience', () => {
    renderPage();
    expect(screen.getByRole('heading', { name: 'Welcome back' })).toBeTruthy();
    expect(screen.getByText('Sign in with your registered account to continue.')).toBeTruthy();
    expect(screen.getByText('Vaccination Portal')).toBeTruthy();
  });

  it('does not display citizen or staff portal selection cards', () => {
    renderPage();
    expect(screen.queryByRole('heading', { name: /Choose your portal/i })).toBeNull();
    expect(screen.queryByRole('link', { name: /Continue as Citizen/i })).toBeNull();
    expect(screen.queryByRole('link', { name: /Continue as Staff/i })).toBeNull();
  });

  it.each([
    'Your staff account is waiting for administrator approval.',
    'Your staff account request was rejected. Contact an administrator if you need assistance.',
    'Your email address still needs to be verified before you can sign in.',
    'Invalid email or password',
  ])('shows the authentication message: %s', (message) => {
    authState.current = { ...authState.current, loginError: message };
    renderPage();
    expect(screen.getByRole('alert').textContent).toContain(message);
  });

  it('displays resend verification option when account is pending verification and triggers resend', async () => {
    authState.current = {
      ...authState.current,
      loginError: 'Your email address still needs to be verified before you can sign in.',
    };
    resendVerification.mockResolvedValueOnce({
      message: 'If the account exists and is pending verification, a verification email has been sent.',
    });

    renderPage();

    const emailInput = screen.getByLabelText('Email Address');
    fireEvent.change(emailInput, { target: { value: 'citizen@example.gov' } });

    const resendBtn = screen.getByRole('button', { name: /resend verification email/i });
    expect(resendBtn).toBeTruthy();

    fireEvent.click(resendBtn);

    await waitFor(() => {
      expect(resendVerification).toHaveBeenCalledWith('citizen@example.gov');
      expect(screen.getByText(/If the account exists and is pending verification/i)).toBeTruthy();
    });
  });

  it('renders a forgot-password link leading to /forgot-password', () => {
    renderPage();
    const forgotLink = screen.getByRole('link', { name: 'Forgot password?' });
    expect(forgotLink).toBeTruthy();
    expect(forgotLink.getAttribute('href')).toBe('/forgot-password');
  });

  it('submits credentials through the universal login form', async () => {
    authState.current.login.mockResolvedValue({ success: false });
    renderPage();

    fireEvent.change(screen.getByLabelText('Email Address'), {
      target: { value: 'person@example.gov' },
    });
    fireEvent.change(screen.getByLabelText('Password'), {
      target: { value: 'Password123!' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Sign in' }));

    await waitFor(() => {
      expect(authState.current.login).toHaveBeenCalledWith('person@example.gov', 'Password123!');
    });
  });

  it('exposes registration actions for citizen and staff on universal login', () => {
    renderPage();
    expect(screen.getByRole('link', { name: /Register as Citizen/i }).getAttribute('href')).toBe('/register/citizen');
    expect(screen.getByRole('link', { name: /Request Staff Access/i }).getAttribute('href')).toBe('/register/staff');
    expect(screen.getByText(/Staff accounts require administrator approval/i)).toBeTruthy();
  });

  it('toggles password visibility', () => {
    renderPage();
    const passwordInput = screen.getByLabelText('Password');
    const toggleButton = screen.getByRole('button', { name: 'Show password' });

    expect(passwordInput.getAttribute('type')).toBe('password');
    fireEvent.click(toggleButton);
    expect(passwordInput.getAttribute('type')).toBe('text');
    expect(screen.getByRole('button', { name: 'Hide password' })).toBeTruthy();

    fireEvent.click(screen.getByRole('button', { name: 'Hide password' }));
    expect(passwordInput.getAttribute('type')).toBe('password');
  });

  it('navigates back to portal home when back button is clicked', () => {
    const handleBack = vi.fn();
    render(
      <MemoryRouter initialEntries={['/login']}>
        <LoginPage onBackToLanding={handleBack} />
      </MemoryRouter>,
    );

    const backBtn = screen.getByRole('button', { name: /Back to portal home/i });
    fireEvent.click(backBtn);
    expect(handleBack).toHaveBeenCalledTimes(1);
  });

  it('displays client validation error for empty fields', () => {
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(screen.getByRole('alert').textContent).toBe('Email address is required.');
  });
});
