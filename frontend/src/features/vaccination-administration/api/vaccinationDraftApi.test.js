import { beforeEach, describe, expect, it, vi } from 'vitest';
import { apiRequestData } from '../../../services/apiClient';
import { vaccinationDraftApi } from './vaccinationDraftApi';

vi.mock('../../../services/apiClient', () => ({ apiRequestData: vi.fn() }));

describe('draft API client', () => {
  beforeEach(() => vi.clearAllMocks());

  it('uses the existing authenticated API client for reads and deletion', async () => {
    await vaccinationDraftApi.list();
    await vaccinationDraftApi.get(1);
    await vaccinationDraftApi.remove(1);
    expect(apiRequestData.mock.calls).toEqual([
      ['/v1/vaccination/drafts'], ['/v1/vaccination/drafts/1'], ['/v1/vaccination/drafts/1', { method: 'DELETE' }],
    ]);
  });

  it('POST sends only create DTO fields, never identity or physical confirmation', async () => {
    await vaccinationDraftApi.create({ appointmentId: 101, vaccineBatchId: 301, doseNumber: 1,
      administrationTime: '2026-09-16T10:00:00Z', nextDoseDate: '2030-01-01', authorizedNotes: 'Notes',
      vaccinatorId: 999, physicalAdministrationConfirmed: true, status: 'READY_FOR_FINALIZATION' });
    const [url, options] = apiRequestData.mock.calls[0];
    expect(url).toBe('/v1/vaccination/drafts');
    expect(options.method).toBe('POST');
    expect(JSON.parse(options.body)).toEqual({ appointmentId: 101, vaccineBatchId: 301, doseNumber: 1,
      administrationTime: '2026-09-16T10:00:00Z', nextDoseDate: '2030-01-01', authorizedNotes: 'Notes' });
  });

  it('PATCH sends only editable fields, excluding appointment and server-controlled fields', async () => {
    await vaccinationDraftApi.update(1, { appointmentId: 999, vaccinatorId: 999,
      physicalAdministrationConfirmed: true, doseNumber: 2, authorizedNotes: 'Updated' });
    expect(apiRequestData).toHaveBeenCalledWith('/v1/vaccination/drafts/1', {
      method: 'PATCH', body: JSON.stringify({ doseNumber: 2, authorizedNotes: 'Updated' }),
    });
  });
});
