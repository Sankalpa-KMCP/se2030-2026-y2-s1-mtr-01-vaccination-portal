import { beforeEach, describe, expect, it, vi } from 'vitest';

const clientMocks = vi.hoisted(() => ({
  apiRequest: vi.fn(),
  parseApiResponse: vi.fn(),
}));

vi.mock('../../../shared/api/apiClient', () => clientMocks);

import {
  confirmPhysicalAdministration,
  createVaccinationDraft,
  discardVaccinationDraft,
  finalizeVaccinationDraft,
  getVaccinationDraft,
  listMyVaccinationDrafts,
  updateVaccinationDraft,
  vaccinationDraftApi,
} from './vaccinationDraftApi';

describe('vaccinationDraftApi', () => {
  const rawResponse = { status: 200 };

  beforeEach(() => {
    vi.clearAllMocks();
    clientMocks.apiRequest.mockResolvedValue(rawResponse);
    clientMocks.parseApiResponse.mockResolvedValue({ result: 'parsed' });
  });

  it('creates a draft through the exact backend route with the DTO body', async () => {
    const payload = { appointmentId: 42, vaccineBatchId: 7, doseNumber: 1 };

    await expect(createVaccinationDraft(payload)).resolves.toEqual({ result: 'parsed' });

    expect(clientMocks.apiRequest).toHaveBeenCalledExactlyOnceWith('/v1/vaccination/drafts', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
    expect(clientMocks.parseApiResponse).toHaveBeenCalledExactlyOnceWith(rawResponse);
  });

  it('lists the authenticated vaccinator drafts without an unnecessary empty query string', async () => {
    await expect(listMyVaccinationDrafts()).resolves.toEqual({ result: 'parsed' });

    expect(clientMocks.apiRequest).toHaveBeenCalledExactlyOnceWith('/v1/vaccination/drafts', { method: 'GET' });
    expect(clientMocks.parseApiResponse).toHaveBeenCalledExactlyOnceWith(rawResponse);
  });

  it('reads a single draft through the ownership-guarded route', async () => {
    await getVaccinationDraft(12);

    expect(clientMocks.apiRequest).toHaveBeenCalledExactlyOnceWith('/v1/vaccination/drafts/12', { method: 'GET' });
  });

  it('patches a draft with only the provided mutable fields', async () => {
    const payload = { authorizedNotes: 'Administered in left arm' };

    await updateVaccinationDraft(12, payload);

    expect(clientMocks.apiRequest).toHaveBeenCalledExactlyOnceWith('/v1/vaccination/drafts/12', {
      method: 'PATCH',
      body: JSON.stringify(payload),
    });
  });

  it('sends the audited deletion reason as the backend query parameter', async () => {
    await discardVaccinationDraft(12, 'Created for the wrong appointment ');

    expect(clientMocks.apiRequest).toHaveBeenCalledExactlyOnceWith(
      '/v1/vaccination/drafts/12?reason=Created%20for%20the%20wrong%20appointment',
      { method: 'DELETE' },
    );
  });

  it('refuses to discard a draft without a mandatory non-blank reason', async () => {
    await expect(discardVaccinationDraft(12, '   ')).rejects.toThrow(
      'A deletion reason is required to discard a vaccination draft.',
    );
    expect(clientMocks.apiRequest).not.toHaveBeenCalled();
  });

  it('confirms physical administration via POST to /confirm-administration', async () => {
    await confirmPhysicalAdministration(15);

    expect(clientMocks.apiRequest).toHaveBeenCalledExactlyOnceWith('/v1/vaccination/drafts/15/confirm-administration', {
      method: 'POST',
    });
  });

  it('finalizes draft via POST to /finalize', async () => {
    await finalizeVaccinationDraft(15);

    expect(clientMocks.apiRequest).toHaveBeenCalledExactlyOnceWith('/v1/vaccination/drafts/15/finalize', {
      method: 'POST',
    });
  });

  describe('object-style vaccinationDraftApi helper', () => {
    it('supports list, get, and default reason deletion', async () => {
      await vaccinationDraftApi.list();
      await vaccinationDraftApi.get(5);
      await vaccinationDraftApi.remove(5);

      expect(clientMocks.apiRequest).toHaveBeenNthCalledWith(1, '/v1/vaccination/drafts', { method: 'GET' });
      expect(clientMocks.apiRequest).toHaveBeenNthCalledWith(2, '/v1/vaccination/drafts/5', { method: 'GET' });
      expect(clientMocks.apiRequest).toHaveBeenNthCalledWith(
        3,
        '/v1/vaccination/drafts/5?reason=Discarded%20by%20vaccinator',
        { method: 'DELETE' },
      );
    });

    it('filters create payload to allowed fields', async () => {
      await vaccinationDraftApi.create({
        appointmentId: 101,
        vaccineBatchId: 301,
        doseNumber: 1,
        vaccinatorId: 999,
        physicalAdministrationConfirmed: true,
      });

      expect(clientMocks.apiRequest).toHaveBeenCalledWith('/v1/vaccination/drafts', {
        method: 'POST',
        body: JSON.stringify({ appointmentId: 101, vaccineBatchId: 301, doseNumber: 1 }),
      });
    });

    it('filters update payload to editable fields', async () => {
      await vaccinationDraftApi.update(10, {
        appointmentId: 999,
        doseNumber: 2,
        authorizedNotes: 'Updated note',
      });

      expect(clientMocks.apiRequest).toHaveBeenCalledWith('/v1/vaccination/drafts/10', {
        method: 'PATCH',
        body: JSON.stringify({ doseNumber: 2, authorizedNotes: 'Updated note' }),
      });
    });

    it('delegates confirmAdministration and finalize to correct endpoints', async () => {
      await vaccinationDraftApi.confirmAdministration(10);
      expect(clientMocks.apiRequest).toHaveBeenCalledWith('/v1/vaccination/drafts/10/confirm-administration', {
        method: 'POST',
      });

      await vaccinationDraftApi.finalize(10);
      expect(clientMocks.apiRequest).toHaveBeenCalledWith('/v1/vaccination/drafts/10/finalize', {
        method: 'POST',
      });
    });
  });
});
