import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { setToken } from '../../../shared/auth/authStorage';
import {
  buildCitizenProfileUpdate,
  getCitizenProfile,
  updateCitizenProfile,
} from './citizenProfileApi';

function response(body, status = 200) {
  return {
    ok: status >= 200 && status < 300,
    status,
    statusText: status === 200 ? 'OK' : 'Error',
    headers: { get: () => 'application/json' },
    text: vi.fn().mockResolvedValue(body ? JSON.stringify(body) : ''),
  };
}

describe('citizen profile API', () => {
  beforeEach(() => {
    window.sessionStorage.clear();
    setToken('profile-token');
    vi.stubGlobal('fetch', vi.fn());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
    window.sessionStorage.clear();
  });

  it('loads the authenticated citizen profile', async () => {
    fetch.mockResolvedValueOnce(response({ id: 17, email: 'citizen@example.com' }));

    await expect(getCitizenProfile()).resolves.toMatchObject({ id: 17 });
    expect(fetch).toHaveBeenCalledWith('/api/v1/profile/me', expect.objectContaining({
      method: 'GET',
      headers: expect.objectContaining({ Authorization: 'Bearer profile-token' }),
    }));
  });

  it('sends only the five editable fields and normalizes whitespace', async () => {
    fetch.mockResolvedValueOnce(response({ id: 17, fullName: 'Updated Citizen' }));

    await updateCitizenProfile({
      id: 999,
      userId: 999,
      email: 'attempted-change@example.com',
      roles: ['ROLE_ADMIN'],
      role: 'ROLE_ADMIN',
      accountStatus: 'DISABLED',
      password: 'must-not-leak',
      createdAt: '2020-01-01T00:00:00Z',
      updatedAt: '2020-01-01T00:00:00Z',
      fullName: '  Updated Citizen  ',
      phoneNumber: '  0770001111  ',
      dateOfBirth: '',
      gender: '',
      address: '   ',
    });

    expect(fetch).toHaveBeenCalledWith('/api/v1/profile/me', expect.objectContaining({
      method: 'PUT',
      body: JSON.stringify({
        fullName: 'Updated Citizen',
        phoneNumber: '0770001111',
        dateOfBirth: null,
        gender: null,
        address: null,
      }),
    }));
  });

  it('preserves supported optional values in the update contract', () => {
    expect(buildCitizenProfileUpdate({
      fullName: 'Citizen',
      phoneNumber: '0770001111',
      dateOfBirth: '1994-06-15',
      gender: 'PREFER_NOT_TO_SAY',
      address: '  10 Green Road  ',
    })).toEqual({
      fullName: 'Citizen',
      phoneNumber: '0770001111',
      dateOfBirth: '1994-06-15',
      gender: 'PREFER_NOT_TO_SAY',
      address: '10 Green Road',
    });
  });
});
