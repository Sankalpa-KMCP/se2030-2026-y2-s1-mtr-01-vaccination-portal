import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { getEmailVerificationStatus, resendEmailOtp, verifyEmailOtp } from './emailVerificationApi';

function response(body, status = 200) {
  return {
    ok: status >= 200 && status < 300,
    status,
    statusText: status === 200 ? 'OK' : 'Error',
    headers: { get: () => 'application/json' },
    json: vi.fn().mockResolvedValue(body),
    text: vi.fn().mockResolvedValue(body ? JSON.stringify(body) : ''),
  };
}

describe('email verification API', () => {
  beforeEach(() => {
    window.sessionStorage.clear();
    vi.stubGlobal('fetch', vi.fn());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it('reads verification status from the principal-scoped endpoint', async () => {
    fetch.mockResolvedValueOnce(response({ emailVerified: false, emailVerifiedAt: null }));

    const status = await getEmailVerificationStatus();

    expect(fetch).toHaveBeenCalledWith('/api/v1/auth/email-verification/status', expect.any(Object));
    expect(status.emailVerified).toBe(false);
  });

  it('submits the OTP as a string so leading zeros survive', async () => {
    fetch.mockResolvedValueOnce(response({ emailVerified: true, emailVerifiedAt: '2026-10-05T09:00:00Z' }));

    await verifyEmailOtp('004281');

    expect(fetch).toHaveBeenCalledWith('/api/v1/auth/email-verification/verify', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({ otp: '004281' }),
    }));
  });

  it('resends without any user/email/account selector in the request', async () => {
    fetch.mockResolvedValueOnce(response({ emailVerified: false, emailVerifiedAt: null }));

    await resendEmailOtp();

    const [, options] = fetch.mock.calls[0];
    expect(options.method).toBe('POST');
    expect(options.body).toBeUndefined();
  });

  it('preserves the structured auth error code on rejection', async () => {
    fetch.mockResolvedValueOnce(response({
      timestamp: '2026-10-05T09:00:00Z',
      status: 400,
      error: 'Bad Request',
      code: 'EMAIL_VERIFICATION_INVALID_CODE',
      message: 'The verification code is incorrect',
      path: '/api/v1/auth/email-verification/verify',
    }, 400));

    const rejection = await verifyEmailOtp('000000').catch((error) => error);

    expect(rejection.status).toBe(400);
    expect(rejection.code).toBe('EMAIL_VERIFICATION_INVALID_CODE');
    expect(rejection.error).toBe('Bad Request');
    expect(rejection.data.code).toBe('EMAIL_VERIFICATION_INVALID_CODE');
  });

  it('carries retryAfterSeconds from a throttled resend', async () => {
    fetch.mockResolvedValueOnce(response({
      status: 429,
      error: 'Too Many Requests',
      code: 'EMAIL_VERIFICATION_RESEND_TOO_SOON',
      message: 'Please wait before requesting another verification code',
      retryAfterSeconds: 42,
    }, 429));

    const rejection = await resendEmailOtp().catch((error) => error);

    expect(rejection.status).toBe(429);
    expect(rejection.code).toBe('EMAIL_VERIFICATION_RESEND_TOO_SOON');
    expect(rejection.retryAfterSeconds).toBe(42);
  });
});
