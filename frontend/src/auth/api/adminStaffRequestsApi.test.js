import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { setToken } from '../../shared/auth/authStorage';
import {
  approveStaffRequest,
  getStaffRequest,
  listStaffRequests,
  rejectStaffRequest,
} from './adminStaffRequestsApi';

function response(body, status = 200) {
  return {
    ok: status >= 200 && status < 300,
    status,
    statusText: 'OK',
    headers: { get: () => 'application/json' },
    text: vi.fn().mockResolvedValue(JSON.stringify(body)),
  };
}

describe('admin staff request API', () => {
  beforeEach(() => {
    window.sessionStorage.clear();
    setToken('admin-token');
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(response({ id: 7 })));
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it('lists pending staff requests with Bearer authentication', async () => {
    fetch.mockResolvedValueOnce(response([{ id: 7 }]));
    await listStaffRequests();
    expect(fetch).toHaveBeenCalledWith('/api/v1/admin/staff-requests', expect.objectContaining({
      method: 'GET',
      headers: expect.objectContaining({ Authorization: 'Bearer admin-token' }),
    }));
  });

  it('loads one staff request', async () => {
    await getStaffRequest(7);
    expect(fetch).toHaveBeenCalledWith('/api/v1/admin/staff-requests/7', expect.objectContaining({ method: 'GET' }));
  });

  it('approves with only the authoritative role choice', async () => {
    await approveStaffRequest(7, 'ROLE_INVENTORY_OFFICER');
    expect(fetch).toHaveBeenCalledWith('/api/v1/admin/staff-requests/7/approve', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({ roleCode: 'ROLE_INVENTORY_OFFICER' }),
    }));
  });

  it('rejects with only the reason and no reviewer ID', async () => {
    await rejectStaffRequest(7, 'Unable to verify employment');
    expect(fetch).toHaveBeenCalledWith('/api/v1/admin/staff-requests/7/reject', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({ reason: 'Unable to verify employment' }),
    }));
  });
});
