import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { loginApi, registerCitizen, registerStaff } from './authApi';

function response(body, status = 200) {
  return {
    ok: status >= 200 && status < 300,
    status,
    statusText: status === 200 ? 'OK' : 'Error',
    headers: { get: () => 'application/json' },
    text: vi.fn().mockResolvedValue(body ? JSON.stringify(body) : ''),
  };
}

describe('auth API', () => {
  beforeEach(() => {
    window.sessionStorage.clear();
    vi.stubGlobal('fetch', vi.fn());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it('submits a citizen registration without confirmPassword', async () => {
    fetch.mockResolvedValueOnce(response({ id: 10 }, 201));

    await registerCitizen({
      fullName: '  Citizen User  ',
      email: ' citizen@example.com ',
      phoneNumber: ' 0771234567 ',
      password: 'Secret123!',
      confirmPassword: 'Secret123!',
    });

    expect(fetch).toHaveBeenCalledWith('/api/v1/auth/register/citizen', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({
        fullName: 'Citizen User',
        email: 'citizen@example.com',
        phoneNumber: '0771234567',
        password: 'Secret123!',
      }),
    }));
  });

  it('submits a staff registration with the requested role', async () => {
    fetch.mockResolvedValueOnce(response({ id: 11 }, 202));

    await registerStaff({
      fullName: 'Staff User',
      email: 'staff@example.com',
      phoneNumber: '0711002000',
      password: 'Secret123!',
      requestedRoleCode: 'ROLE_CENTER_MANAGER',
    });

    expect(fetch).toHaveBeenCalledWith('/api/v1/auth/register/staff', expect.objectContaining({
      method: 'POST',
      body: expect.stringContaining('ROLE_CENTER_MANAGER'),
    }));
  });

  it.each([
    ['ACCOUNT_PENDING_APPROVAL', 'Your staff account is waiting for administrator approval.'],
    ['ACCOUNT_REJECTED', 'Your staff account request was rejected. Contact an administrator if you need assistance.'],
  ])('maps %s to its stable login message', async (code, message) => {
    fetch.mockResolvedValueOnce(response({ code, message: 'Backend detail' }, 403));

    await expect(loginApi('staff@example.com', 'Secret123!')).rejects.toMatchObject({ message, status: 403 });
  });

  it('preserves generic invalid-credential messaging', async () => {
    fetch.mockResolvedValueOnce(response({ code: 'INVALID_CREDENTIALS' }, 401));

    await expect(loginApi('user@example.com', 'wrong')).rejects.toMatchObject({
      message: 'Invalid email or password',
      status: 401,
    });
  });
});
