import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import VerifyEmailPage from './VerifyEmailPage';
import { verifyEmail } from '../api/authApi';

vi.mock('../api/authApi', () => ({
  verifyEmail: vi.fn(),
  resendVerification: vi.fn(),
}));

function renderPage(search = '?token=valid-verify-token') {
  return render(
    <MemoryRouter initialEntries={[`/verify-email${search}`]}>
      <VerifyEmailPage />
    </MemoryRouter>,
  );
}

describe('VerifyEmailPage', () => {
  beforeEach(() => {
    verifyEmail.mockReset();
  });

  afterEach(cleanup);

  it('renders missing token message when no token query parameter is provided', () => {
    renderPage('');
    expect(screen.getByRole('alert').textContent).toContain('No verification token provided');
    expect(verifyEmail).not.toHaveBeenCalled();
    expect(screen.getByRole('link', { name: /return to sign in/i }).getAttribute('href')).toBe('/login');
  });

  it('verifies email successfully and displays success message with link to login', async () => {
    verifyEmail.mockResolvedValueOnce({ message: 'Email verified successfully.' });

    renderPage('?token=my-action-token');

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Email verified successfully' })).toBeTruthy();
    });

    expect(verifyEmail).toHaveBeenCalledWith('my-action-token');
    expect(screen.getByRole('link', { name: 'Continue to Sign in' }).getAttribute('href')).toBe('/login');
  });

  it.each([
    ['INVALID_TOKEN', 'This verification link is invalid or unrecognized. Please request a new verification email.'],
    ['TOKEN_EXPIRED', 'This verification link has expired. Please request a new verification email.'],
    ['TOKEN_ALREADY_USED', 'This verification link has already been used. If your account is already active, please sign in.'],
  ])('displays user-friendly error message for %s', async (code, expectedMessage) => {
    const error = new Error('Verification failed');
    error.data = { code };
    verifyEmail.mockRejectedValueOnce(error);

    renderPage('?token=problematic-token');

    await waitFor(() => {
      expect(screen.getByRole('alert').textContent).toContain(expectedMessage);
    });

    expect(screen.getByRole('button', { name: 'Resend verification email' })).toBeTruthy();
    expect(screen.getByRole('link', { name: /return to sign in/i }).getAttribute('href')).toBe('/login');
  });

  it('prevents duplicate API call on mount/render for the same token', async () => {
    verifyEmail.mockResolvedValue({ message: 'Email verified successfully.' });

    const view = renderPage('?token=single-call-token');
    view.rerender(
      <MemoryRouter initialEntries={['/verify-email?token=single-call-token']}>
        <VerifyEmailPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Email verified successfully' })).toBeTruthy();
    });

    expect(verifyEmail).toHaveBeenCalledTimes(1);
  });
});
