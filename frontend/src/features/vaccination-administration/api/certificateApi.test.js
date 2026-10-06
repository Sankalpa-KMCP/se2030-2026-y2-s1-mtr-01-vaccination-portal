import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import {
  verifyCertificatePublicly,
  listMyVaccinations,
  listMyCertificates,
  downloadCertificate,
  requestVaccinationCorrection,
  listMyCorrectionRequests,
  listAdminCorrectionRequests,
  approveCorrectionRequest,
  rejectCorrectionRequest,
  listVaccinatorCorrectionRequests,
  applyCorrectionRequest,
  getRecordCorrections,
} from './certificateApi';
import * as apiClient from '../../../shared/api/apiClient';

vi.mock('../../../shared/api/apiClient', () => ({
  apiRequest: vi.fn(),
  parseApiResponse: vi.fn(),
}));

describe('certificateApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('verifyCertificatePublicly', () => {
    it('throws error when token is empty or whitespace', async () => {
      await expect(verifyCertificatePublicly('')).rejects.toThrow('Verification token or reference code is required.');
      await expect(verifyCertificatePublicly('   ')).rejects.toThrow('Verification token or reference code is required.');
    });

    it('returns formatted verification result on 200 OK', async () => {
      const mockBackendResponse = {
        certificateReference: 'CERT-TEST-999',
        vaccinationRecordId: 101,
        issuedAt: '2026-09-01T10:00:00Z',
        status: 'VALID',
        doseNumber: 2,
        administrationTime: '2026-09-01T09:30:00Z',
        valid: true,
      };

      const mockResponse = { ok: true, status: 200 };
      apiClient.apiRequest.mockResolvedValue(mockResponse);
      apiClient.parseApiResponse.mockResolvedValue(mockBackendResponse);

      const result = await verifyCertificatePublicly('test-token-123');

      expect(apiClient.apiRequest).toHaveBeenCalledWith(
        '/v1/vaccination/certificates/verify/test-token-123',
        expect.objectContaining({ auth: false, method: 'GET' }),
      );
      expect(result.result).toBe('VALID');
      expect(result.certificateReference).toBe('CERT-TEST-999');
      expect(result.doseNumber).toBe(2);
      expect(result.citizenDisplayName).toBe('Verified Citizen');
    });

    it('returns INVALID result object on 404 response', async () => {
      const mockResponse = { ok: false, status: 404 };
      apiClient.apiRequest.mockResolvedValue(mockResponse);

      const result = await verifyCertificatePublicly('non-existent-token');

      expect(result.result).toBe('INVALID');
      expect(result.message).toMatch(/No active or archived certificate records match/i);
    });
  });

  describe('listMyVaccinations', () => {
    it('calls GET /v1/vaccination/records/mine', async () => {
      const mockRecords = [{ id: 1, doseNumber: 1 }];
      const mockResponse = { ok: true, status: 200 };
      apiClient.apiRequest.mockResolvedValue(mockResponse);
      apiClient.parseApiResponse.mockResolvedValue(mockRecords);

      const result = await listMyVaccinations();

      expect(apiClient.apiRequest).toHaveBeenCalledWith('/v1/vaccination/records/mine', { method: 'GET' });
      expect(result).toEqual(mockRecords);
    });
  });

  describe('listMyCertificates', () => {
    it('calls GET /v1/vaccination/certificates/mine', async () => {
      const mockCerts = [{ id: 1, certificateReference: 'CERT-001', status: 'VALID' }];
      const mockResponse = { ok: true, status: 200 };
      apiClient.apiRequest.mockResolvedValue(mockResponse);
      apiClient.parseApiResponse.mockResolvedValue(mockCerts);

      const result = await listMyCertificates();

      expect(apiClient.apiRequest).toHaveBeenCalledWith('/v1/vaccination/certificates/mine', { method: 'GET' });
      expect(result).toEqual(mockCerts);
    });
  });

  describe('downloadCertificate', () => {
    it('downloads PDF blob successfully', async () => {
      const mockBlob = new Blob(['%PDF-1.4 test'], { type: 'application/pdf' });
      const mockResponse = {
        ok: true,
        status: 200,
        blob: vi.fn().mockResolvedValue(mockBlob),
      };
      apiClient.apiRequest.mockResolvedValue(mockResponse);

      const createObjectURLMock = vi.fn().mockReturnValue('blob:http://localhost/123');
      const revokeObjectURLMock = vi.fn();
      window.URL.createObjectURL = createObjectURLMock;
      window.URL.revokeObjectURL = revokeObjectURLMock;

      const success = await downloadCertificate(42, 'CERT-DOWNLOAD-TEST');

      expect(apiClient.apiRequest).toHaveBeenCalledWith(
        '/v1/vaccination/certificates/42/download',
        expect.objectContaining({ headers: { Accept: 'application/pdf' } }),
      );
      expect(success).toBe(true);
      expect(createObjectURLMock).toHaveBeenCalledWith(mockBlob);
      expect(revokeObjectURLMock).toHaveBeenCalledWith('blob:http://localhost/123');
    });

    it('throws error when server returns error status', async () => {
      const mockResponse = { ok: false, status: 403 };
      apiClient.apiRequest.mockResolvedValue(mockResponse);

      await expect(downloadCertificate(42)).rejects.toThrow('Failed to download vaccination certificate PDF.');
    });
  });

  describe('Correction Requests API', () => {
    it('requestVaccinationCorrection sends POST to /v1/vaccination/records/:recordId/corrections', async () => {
      const mockCorrection = { id: 10, status: 'PENDING' };
      apiClient.apiRequest.mockResolvedValue({ ok: true, status: 201 });
      apiClient.parseApiResponse.mockResolvedValue(mockCorrection);

      const res = await requestVaccinationCorrection(5, 'Incorrect batch');

      expect(apiClient.apiRequest).toHaveBeenCalledWith(
        '/v1/vaccination/records/5/corrections',
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify({ message: 'Incorrect batch' }),
        }),
      );
      expect(res).toEqual(mockCorrection);
    });

    it('listMyCorrectionRequests calls GET /v1/vaccination/corrections/mine', async () => {
      const mockList = [{ id: 1 }];
      apiClient.apiRequest.mockResolvedValue({ ok: true });
      apiClient.parseApiResponse.mockResolvedValue(mockList);

      const res = await listMyCorrectionRequests();
      expect(apiClient.apiRequest).toHaveBeenCalledWith('/v1/vaccination/corrections/mine', { method: 'GET' });
      expect(res).toEqual(mockList);
    });

    it('listAdminCorrectionRequests calls GET /v1/vaccination/corrections', async () => {
      const mockList = [{ id: 1 }];
      apiClient.apiRequest.mockResolvedValue({ ok: true });
      apiClient.parseApiResponse.mockResolvedValue(mockList);

      const res = await listAdminCorrectionRequests();
      expect(apiClient.apiRequest).toHaveBeenCalledWith('/v1/vaccination/corrections', { method: 'GET' });
      expect(res).toEqual(mockList);
    });

    it('approveCorrectionRequest sends POST to /approve', async () => {
      apiClient.apiRequest.mockResolvedValue({ ok: true });
      apiClient.parseApiResponse.mockResolvedValue({ id: 1, status: 'APPROVED' });

      const res = await approveCorrectionRequest(100, 1, 'Looks valid');
      expect(apiClient.apiRequest).toHaveBeenCalledWith(
        '/v1/vaccination/records/100/corrections/1/approve',
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify({ reason: 'Looks valid' }),
        }),
      );
      expect(res.status).toBe('APPROVED');
    });

    it('rejectCorrectionRequest sends POST to /reject', async () => {
      apiClient.apiRequest.mockResolvedValue({ ok: true });
      apiClient.parseApiResponse.mockResolvedValue({ id: 1, status: 'REJECTED' });

      const res = await rejectCorrectionRequest(100, 1, 'Insufficient justification');
      expect(apiClient.apiRequest).toHaveBeenCalledWith(
        '/v1/vaccination/records/100/corrections/1/reject',
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify({ reason: 'Insufficient justification' }),
        }),
      );
      expect(res.status).toBe('REJECTED');
    });

    it('listVaccinatorCorrectionRequests calls GET /v1/vaccination/corrections/administered', async () => {
      const mockList = [{ id: 2 }];
      apiClient.apiRequest.mockResolvedValue({ ok: true });
      apiClient.parseApiResponse.mockResolvedValue(mockList);

      const res = await listVaccinatorCorrectionRequests();
      expect(apiClient.apiRequest).toHaveBeenCalledWith('/v1/vaccination/corrections/administered', { method: 'GET' });
      expect(res).toEqual(mockList);
    });

    it('applyCorrectionRequest sends POST to /apply with updates', async () => {
      apiClient.apiRequest.mockResolvedValue({ ok: true });
      apiClient.parseApiResponse.mockResolvedValue({ id: 2, status: 'APPLIED' });

      const updates = { fieldName: 'doseNumber', newValue: '2' };
      const res = await applyCorrectionRequest(100, 2, updates);
      expect(apiClient.apiRequest).toHaveBeenCalledWith(
        '/v1/vaccination/records/100/corrections/2/apply',
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify(updates),
        }),
      );
      expect(res.status).toBe('APPLIED');
    });

    it('getRecordCorrections calls GET /v1/vaccination/records/:recordId/corrections', async () => {
      const mockList = [{ id: 3 }];
      apiClient.apiRequest.mockResolvedValue({ ok: true });
      apiClient.parseApiResponse.mockResolvedValue(mockList);

      const res = await getRecordCorrections(5);
      expect(apiClient.apiRequest).toHaveBeenCalledWith('/v1/vaccination/records/5/corrections', { method: 'GET' });
      expect(res).toEqual(mockList);
    });
  });
});
