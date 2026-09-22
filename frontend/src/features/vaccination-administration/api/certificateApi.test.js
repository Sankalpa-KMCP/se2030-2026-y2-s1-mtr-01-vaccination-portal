import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import {
  verifyCertificatePublicly,
  listMyVaccinations,
  listMyCertificates,
  downloadCertificate,
} from './certificateApi';
import * as apiClient from '../../../services/apiClient';

vi.mock('../../../services/apiClient', () => ({
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
});
