import { beforeEach, describe, expect, it, vi } from 'vitest';
import { apiRequest } from '../../../shared/api/apiClient';
import { deleteVaccineType, deleteTargetGroup } from './campaignApi';

vi.mock('../../../shared/api/apiClient', () => ({ apiRequest: vi.fn() }));
beforeEach(() => vi.resetAllMocks());

describe.each([
  ['vaccine-types', deleteVaccineType], ['target-groups', deleteTargetGroup],
])('delete %s', (route, remove) => {
  it('handles an empty 204 response', async () => {
    const json = vi.fn();
    apiRequest.mockResolvedValue({ ok: true, status: 204, json });
    expect(await remove(7)).toBeNull();
    expect(apiRequest).toHaveBeenCalledWith(`/v1/${route}/7`, { method: 'DELETE' });
    expect(json).not.toHaveBeenCalled();
  });
  it('preserves a structured conflict error', async () => {
    apiRequest.mockResolvedValue({ ok: false, status: 409, json: async () => ({ message: 'Deactivate it instead.' }) });
    await expect(remove(7)).rejects.toMatchObject({ status: 409, message: 'Deactivate it instead.' });
  });
});
