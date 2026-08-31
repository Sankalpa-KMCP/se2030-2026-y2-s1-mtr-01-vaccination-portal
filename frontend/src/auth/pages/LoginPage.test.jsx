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
});
