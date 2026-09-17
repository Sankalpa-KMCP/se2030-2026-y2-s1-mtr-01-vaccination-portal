import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import ForgotPasswordPage from './ForgotPasswordPage';
import { forgotPassword } from '../api/authApi';

vi.mock('../api/authApi', () => ({
  forgotPassword: vi.fn(),
}));

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/forgot-password']}>
      <ForgotPasswordPage />
    </MemoryRouter>,
  );
}

describe('ForgotPasswordPage', () => {
  beforeEach(() => {
    forgotPassword.mockReset();
  });

  afterEach(cleanup);

  it('renders the forgot password form and fields', () => {
    renderPage();
    expect(screen.getByRole('heading', { name: 'Forgot your password?' })).toBeTruthy();
    expect(screen.getByLabelText('Email Address')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Send reset instructions' })).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Sign in' }).getAttribute('href')).toBe('/login');
  });

  it('validates client-side for empty email', async () => {
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: 'Send reset instructions' }));

    expect(screen.getByRole('alert').textContent).toBe('Email address is required.');
    expect(forgotPassword).not.toHaveBeenCalled();
  });

  it.each(['invalid', 'user@', '@example.com', 'user example.com'])(
    'validates client-side for malformed email: %s',
    async (email) => {
      renderPage();
      fireEvent.change(screen.getByLabelText('Email Address'), { target: { value: email } });
      fireEvent.click(screen.getByRole('button', { name: 'Send reset instructions' }));

      expect(screen.getByRole('alert').textContent).toBe('Please enter a valid email address.');
      expect(forgotPassword).not.toHaveBeenCalled();
    },
  );

  it('submits forgot password and renders generic success message without leaking account existence', async () => {
    forgotPassword.mockResolvedValueOnce({
      message: 'If an eligible account is associated with this email address, password reset instructions have been sent.',
    });

    renderPage();
    fireEvent.change(screen.getByLabelText('Email Address'), {
      target: { value: 'citizen@example.gov' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Send reset instructions' }));

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Check your email' })).toBeTruthy();
    });

    expect(forgotPassword).toHaveBeenCalledWith('citizen@example.gov');
    expect(screen.getByText(/If an eligible account is associated with this email address/i)).toBeTruthy();
    expect(screen.queryByText(/account exists/i)).toBeNull();
    expect(screen.queryByText(/account not found/i)).toBeNull();
    expect(screen.getByRole('link', { name: /return to sign in/i }).getAttribute('href')).toBe('/login');
  });

  it('disables submit button while submission is in flight', async () => {
    let resolvePromise;
    forgotPassword.mockReturnValue(new Promise((resolve) => { resolvePromise = resolve; }));

    renderPage();
    fireEvent.change(screen.getByLabelText('Email Address'), {
      target: { value: 'citizen@example.gov' },
    });
    const submitButton = screen.getByRole('button', { name: 'Send reset instructions' });
    fireEvent.click(submitButton);

    expect(submitButton.disabled).toBe(true);
    expect(screen.getByText('Sending instructions…')).toBeTruthy();

    resolvePromise({ message: 'Instructions sent.' });
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Check your email' })).toBeTruthy();
    });
  });

  it('handles backend API errors gracefully', async () => {
    forgotPassword.mockRejectedValueOnce(new Error('Network error. Please try again.'));

    renderPage();
    fireEvent.change(screen.getByLabelText('Email Address'), {
      target: { value: 'citizen@example.gov' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Send reset instructions' }));

    await waitFor(() => {
      expect(screen.getByRole('alert').textContent).toBe('Network error. Please try again.');
    });
  });
});
