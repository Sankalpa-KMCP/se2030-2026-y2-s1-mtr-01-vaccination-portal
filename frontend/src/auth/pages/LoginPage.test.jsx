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

const authState = vi.hoisted(() => ({ current: null }));
vi.mock('../context/AuthContext', () => ({ useAuth: () => authState.current }));

function renderPage(path = '/login') {
  return render(<MemoryRouter initialEntries={[path]}><LoginPage /></MemoryRouter>);
}

describe('LoginPage entry experiences', () => {
  beforeEach(() => {
    authState.current = {
      login: vi.fn(),
      loginError: null,
      clearLoginError: vi.fn(),
    };
  });

  afterEach(cleanup);

  it('renders the citizen login entry', () => {
    renderPage('/login/citizen');
    expect(screen.getByRole('heading', { name: 'Citizen Login' })).toBeTruthy();
    expect(screen.getByText('Log in with your citizen account.')).toBeTruthy();
    expect(screen.getByText('Need a citizen account?')).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Create Citizen Account' }).getAttribute('href')).toBe('/register/citizen');
  });

  it('renders the staff login entry', () => {
    renderPage('/login/staff');
    expect(screen.getByRole('heading', { name: 'Staff Login' })).toBeTruthy();
    expect(screen.getByText('Log in with your approved staff or administrator account.')).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Request a Staff Account' })).toBeTruthy();
  });

  it('preserves a generic login entry', () => {
    renderPage();
    expect(screen.getByRole('heading', { name: 'Login to Vaccination Portal' })).toBeTruthy();
  });

  it.each([
    'Your staff account is waiting for administrator approval.',
    'Your staff account request was rejected. Contact an administrator if you need assistance.',
    'Invalid email or password',
  ])('shows the authentication message: %s', (message) => {
    authState.current = { ...authState.current, loginError: message };
    renderPage('/login/staff');
    expect(screen.getByRole('alert').textContent).toBe(message);
  });

  it.each(['/login', '/login/citizen', '/login/staff'])(
    'uses the shared login behavior on %s',
    async (path) => {
      authState.current.login.mockResolvedValue({ success: false });
      renderPage(path);

      fireEvent.change(screen.getByLabelText('Email Address'), {
        target: { value: 'person@example.gov' },
      });
      fireEvent.change(screen.getByLabelText('Password'), {
        target: { value: 'Password123!' },
      });
      fireEvent.click(screen.getByRole('button', { name: 'Login' }));

      await waitFor(() => {
        expect(authState.current.login).toHaveBeenCalledWith('person@example.gov', 'Password123!');
      });
    },
  );

  it('exposes guided access cards on universal login', () => {
    renderPage('/login');
    expect(screen.getByRole('heading', { name: 'Citizen Portal' })).toBeTruthy();
    expect(screen.getByRole('heading', { name: 'Staff Portal' })).toBeTruthy();
    expect(screen.getByRole('link', { name: /Continue as Citizen/i }).getAttribute('href')).toBe('/login/citizen');
    expect(screen.getByRole('link', { name: /Continue as Staff/i }).getAttribute('href')).toBe('/login/staff');
  });

  it('exposes registration actions on universal login', () => {
    renderPage('/login');
    expect(screen.getByRole('link', { name: /Register as Citizen/i }).getAttribute('href')).toBe('/register/citizen');
    expect(screen.getByRole('link', { name: /Register as Staff/i }).getAttribute('href')).toBe('/register/staff');
    expect(screen.getByText(/Staff registration may require administrator approval/i)).toBeTruthy();
  });

  it('toggles password visibility', () => {
    renderPage('/login');
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
    renderPage('/login');
    fireEvent.click(screen.getByRole('button', { name: 'Login' }));

    expect(screen.getByRole('alert').textContent).toBe('Email address is required.');
  });
});
