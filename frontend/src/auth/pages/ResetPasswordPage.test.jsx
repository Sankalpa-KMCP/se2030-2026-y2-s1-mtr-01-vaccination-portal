import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import ResetPasswordPage from './ResetPasswordPage';
import { resetPassword } from '../api/authApi';

vi.mock('../api/authApi', () => ({
  resetPassword: vi.fn(),
}));

function renderPage(search = '?token=test-reset-token-123') {
  return render(
    <MemoryRouter initialEntries={[`/reset-password${search}`]}>
      <ResetPasswordPage />
    </MemoryRouter>,
  );
}

describe('ResetPasswordPage', () => {
  beforeEach(() => {
    resetPassword.mockReset();
  });

  afterEach(cleanup);

  it('renders missing-token state when token is missing from URL query string', () => {
    renderPage('');
    expect(screen.getByRole('alert').textContent).toContain('A valid password reset token is required');
    expect(screen.getByRole('link', { name: 'Request New Reset Link' }).getAttribute('href')).toBe('/forgot-password');
    expect(screen.queryByLabelText('New Password')).toBeNull();
  });

  it('renders form fields when token is present', () => {
    renderPage('?token=valid-token');
    expect(screen.getByRole('heading', { name: 'Set new password' })).toBeTruthy();
    expect(screen.getByLabelText('New Password')).toBeTruthy();
    expect(screen.getByLabelText('Confirm New Password')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Reset password' })).toBeTruthy();
  });

  it('validates password minimum length (< 8 chars)', async () => {
    renderPage('?token=valid-token');
    fireEvent.change(screen.getByLabelText('New Password'), { target: { value: 'short' } });
    fireEvent.change(screen.getByLabelText('Confirm New Password'), { target: { value: 'short' } });
    fireEvent.click(screen.getByRole('button', { name: 'Reset password' }));

    expect(screen.getByRole('alert').textContent).toBe('Password must be between 8 and 128 characters.');
    expect(resetPassword).not.toHaveBeenCalled();
  });

  it('validates password mismatch', async () => {
    renderPage('?token=valid-token');
    fireEvent.change(screen.getByLabelText('New Password'), { target: { value: 'ValidPassword123!' } });
    fireEvent.change(screen.getByLabelText('Confirm New Password'), { target: { value: 'DifferentPassword123!' } });
    fireEvent.click(screen.getByRole('button', { name: 'Reset password' }));

    expect(screen.getByRole('alert').textContent).toBe('Passwords do not match.');
    expect(resetPassword).not.toHaveBeenCalled();
  });

  it('successfully resets password and shows success state with login link', async () => {
    resetPassword.mockResolvedValueOnce({
      message: 'Password has been reset successfully',
    });

    renderPage('?token=my-secret-token');
    fireEvent.change(screen.getByLabelText('New Password'), { target: { value: 'NewPassword123!' } });
    fireEvent.change(screen.getByLabelText('Confirm New Password'), { target: { value: 'NewPassword123!' } });
    fireEvent.click(screen.getByRole('button', { name: 'Reset password' }));

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Password reset successfully' })).toBeTruthy();
    });

    expect(resetPassword).toHaveBeenCalledWith('my-secret-token', 'NewPassword123!');
    expect(screen.getByRole('link', { name: 'Continue to Sign in' }).getAttribute('href')).toBe('/login');
  });

  it.each([
    ['INVALID_TOKEN', 'This password reset token is invalid or unrecognized. Please request a new link.'],
    ['TOKEN_EXPIRED', 'This password reset token has expired. Please request a new reset link.'],
    ['TOKEN_ALREADY_USED', 'This password reset token has already been used. Please request a new reset link if needed.'],
    ['VALIDATION_FAILED', 'Please ensure your new password meets all requirements (8 to 128 characters).'],
  ])('maps backend error code %s to clear user message', async (code, expectedMessage) => {
    const error = new Error('Backend failed');
    error.data = { code };
    resetPassword.mockRejectedValueOnce(error);

    renderPage('?token=invalid-token');
    fireEvent.change(screen.getByLabelText('New Password'), { target: { value: 'NewPassword123!' } });
    fireEvent.change(screen.getByLabelText('Confirm New Password'), { target: { value: 'NewPassword123!' } });
    fireEvent.click(screen.getByRole('button', { name: 'Reset password' }));

    await waitFor(() => {
      expect(screen.getByRole('alert').textContent).toBe(expectedMessage);
    });
  });
});
