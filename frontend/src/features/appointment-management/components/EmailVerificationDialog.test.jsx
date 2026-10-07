import React from 'react';
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import EmailVerificationDialog from './EmailVerificationDialog';
import { getEmailVerificationStatus, resendEmailOtp, verifyEmailOtp } from '../api/emailVerificationApi';

vi.mock('../api/emailVerificationApi', () => ({
  getEmailVerificationStatus: vi.fn(),
  verifyEmailOtp: vi.fn(),
  resendEmailOtp: vi.fn(),
}));

function authError(code, status = 400, extra = {}) {
  const error = new Error('backend rejection');
  error.status = status;
  error.code = code;
  error.error = 'Bad Request';
  error.data = { code, status };
  Object.assign(error, extra);
  return error;
}

function enterOtp(value) {
  fireEvent.change(screen.getByLabelText('Verification code'), { target: { value } });
}

async function renderDialog() {
  render(<EmailVerificationDialog onClose={vi.fn()} />);
  await waitFor(() => expect(getEmailVerificationStatus).toHaveBeenCalledTimes(1));
  await screen.findByLabelText('Verification code');
}

describe('EmailVerificationDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getEmailVerificationStatus.mockResolvedValue({ emailVerified: false, emailVerifiedAt: null });
  });

  afterEach(cleanup);

  it('renders the OTP entry dialog and requests verification status once', async () => {
    await renderDialog();

    expect(screen.getByRole('dialog', { name: 'Verify your email' })).toBeTruthy();
    expect(screen.getByLabelText('Verification code')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Verify' })).toBeTruthy();
    expect(getEmailVerificationStatus).toHaveBeenCalledTimes(1);
  });

  it('shows the verified state without OTP entry when status reports already verified', async () => {
    getEmailVerificationStatus.mockResolvedValue({ emailVerified: true, emailVerifiedAt: '2026-10-05T09:00:00Z' });

    render(<EmailVerificationDialog onClose={vi.fn()} />);

    expect(await screen.findByText('Email verified successfully.')).toBeTruthy();
    expect(screen.queryByLabelText('Verification code')).toBeNull();
  });

  it('submits a six-digit OTP as a string with leading zeros preserved', async () => {
    verifyEmailOtp.mockResolvedValue({ emailVerified: true, emailVerifiedAt: '2026-10-05T09:00:00Z' });
    await renderDialog();

    enterOtp('004281');
    fireEvent.click(screen.getByRole('button', { name: 'Verify' }));

    await screen.findByText('Email verified successfully.');
    expect(verifyEmailOtp).toHaveBeenCalledTimes(1);
    expect(verifyEmailOtp).toHaveBeenCalledWith('004281');
  });

  it('rejects incomplete codes without calling the backend', async () => {
    await renderDialog();

    const verifyButton = screen.getByRole('button', { name: 'Verify' });
    expect(verifyButton.disabled).toBe(true);

    enterOtp('0042');
    expect(verifyButton.disabled).toBe(true);
    fireEvent.click(verifyButton);
    expect(verifyEmailOtp).not.toHaveBeenCalled();
  });

  it('maps an invalid code to inline feedback and stays in entry state', async () => {
    verifyEmailOtp.mockRejectedValueOnce(authError('EMAIL_VERIFICATION_INVALID_CODE'));
    await renderDialog();

    enterOtp('999999');
    fireEvent.click(screen.getByRole('button', { name: 'Verify' }));

    expect(await screen.findByText('That verification code is not valid.')).toBeTruthy();
    expect(screen.queryByText('Email verified successfully.')).toBeNull();
  });

  it('maps an expired code to the request-a-new-code message', async () => {
    verifyEmailOtp.mockRejectedValueOnce(authError('EMAIL_VERIFICATION_CODE_EXPIRED'));
    await renderDialog();

    enterOtp('123456');
    fireEvent.click(screen.getByRole('button', { name: 'Verify' }));

    expect(await screen.findByText('That verification code has expired. Request a new code.')).toBeTruthy();
  });

  it('maps exhausted attempts to an actionable message', async () => {
    verifyEmailOtp.mockRejectedValueOnce(authError('EMAIL_VERIFICATION_ATTEMPTS_EXCEEDED'));
    await renderDialog();

    enterOtp('123456');
    fireEvent.click(screen.getByRole('button', { name: 'Verify' }));

    expect(await screen.findByText('Too many incorrect attempts. Request a new verification code.')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Resend code' })).toBeTruthy();
  });

  it('treats an already-verified rejection as the verified state', async () => {
    verifyEmailOtp.mockRejectedValueOnce(authError('EMAIL_ALREADY_VERIFIED', 409));
    await renderDialog();

    enterOtp('123456');
    fireEvent.click(screen.getByRole('button', { name: 'Verify' }));

    expect(await screen.findByText('Email verified successfully.')).toBeTruthy();
  });

  it('shows retryable generic feedback when verification fails unexpectedly', async () => {
    verifyEmailOtp.mockRejectedValueOnce(new Error('network down'));
    await renderDialog();

    enterOtp('123456');
    fireEvent.click(screen.getByRole('button', { name: 'Verify' }));

    expect(await screen.findByText('We could not verify that code right now. Please try again.')).toBeTruthy();
    expect(screen.getByLabelText('Verification code')).toBeTruthy();
  });

  it('confirms a successful resend and clears the stale code', async () => {
    resendEmailOtp.mockResolvedValueOnce({ emailVerified: false, emailVerifiedAt: null });
    await renderDialog();

    enterOtp('111111');
    fireEvent.click(screen.getByRole('button', { name: 'Resend code' }));

    expect(await screen.findByText('A new code has been sent to your email.')).toBeTruthy();
    expect(screen.getByLabelText('Verification code').value).toBe('');
  });

  it('blocks resends inside the server-provided cooldown window', async () => {
    resendEmailOtp.mockRejectedValueOnce(authError('EMAIL_VERIFICATION_RESEND_TOO_SOON', 429, { retryAfterSeconds: 45 }));
    await renderDialog();

    fireEvent.click(screen.getByRole('button', { name: 'Resend code' }));

    const coolingButton = await screen.findByRole('button', { name: 'Resend code in 45s' });
    expect(coolingButton.disabled).toBe(true);
  });

  it('re-enables resend when the server-driven countdown reaches zero', async () => {
    vi.useFakeTimers();
    try {
      resendEmailOtp.mockRejectedValueOnce(authError('EMAIL_VERIFICATION_RESEND_TOO_SOON', 429, { retryAfterSeconds: 3 }));
      render(<EmailVerificationDialog onClose={vi.fn()} />);
      await act(async () => {});

      fireEvent.click(screen.getByRole('button', { name: 'Resend code' }));
      await act(async () => {});
      expect(screen.getByRole('button', { name: 'Resend code in 3s' })).toBeTruthy();

      act(() => { vi.advanceTimersByTime(1000); });
      expect(screen.getByRole('button', { name: 'Resend code in 2s' })).toBeTruthy();
      act(() => { vi.advanceTimersByTime(2000); });

      const reenabled = screen.getByRole('button', { name: 'Resend code' });
      expect(reenabled.disabled).toBe(false);
    } finally {
      vi.useRealTimers();
    }
  });

  it('shows retryable generic feedback when resend fails without a cooldown', async () => {
    resendEmailOtp.mockRejectedValueOnce(new Error('smtp down'));
    await renderDialog();

    fireEvent.click(screen.getByRole('button', { name: 'Resend code' }));

    expect(await screen.findByText('We could not send a new code right now. Please try again shortly.')).toBeTruthy();
    expect(screen.getByRole('dialog', { name: 'Verify your email' })).toBeTruthy();
    expect(screen.queryByText('Email verified successfully.')).toBeNull();
  });

  it('closes through the close control', async () => {
    const onClose = vi.fn();
    render(<EmailVerificationDialog onClose={onClose} />);
    await screen.findByLabelText('Verification code');

    fireEvent.click(screen.getByRole('button', { name: 'Close' }));

    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('closes through Done after successful verification', async () => {
    const onClose = vi.fn();
    verifyEmailOtp.mockResolvedValue({ emailVerified: true, emailVerifiedAt: '2026-10-05T09:00:00Z' });
    render(<EmailVerificationDialog onClose={onClose} />);
    await screen.findByLabelText('Verification code');

    enterOtp('123456');
    fireEvent.click(screen.getByRole('button', { name: 'Verify' }));
    fireEvent.click(await screen.findByRole('button', { name: 'Done' }));

    expect(onClose).toHaveBeenCalledTimes(1);
  });
});
