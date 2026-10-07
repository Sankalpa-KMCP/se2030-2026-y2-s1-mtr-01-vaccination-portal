import React from 'react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import VaccinatorVaccinationRecordsPage from './VaccinatorVaccinationRecordsPage';
import * as certificateApi from '../api/certificateApi';

vi.mock('../api/certificateApi', () => ({
  listVaccinatorRecords: vi.fn(),
  downloadCertificate: vi.fn(),
}));

describe('VaccinatorVaccinationRecordsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  it('renders loading state initially and then empty state when no records exist', async () => {
    certificateApi.listVaccinatorRecords.mockResolvedValue([]);

    render(
      <MemoryRouter>
        <VaccinatorVaccinationRecordsPage />
      </MemoryRouter>,
    );

    expect(screen.getByText(/Loading your administered vaccination records/i)).toBeTruthy();

    await waitFor(() => {
      expect(screen.getByText(/No Administered Records Yet/i)).toBeTruthy();
    });

    expect(screen.getByRole('link', { name: /Open Vaccination Drafts Workspace/i })).toBeTruthy();
  });

  it('renders error state and allows retrying when API fails', async () => {
    certificateApi.listVaccinatorRecords.mockRejectedValueOnce(new Error('Network error'));

    render(
      <MemoryRouter>
        <VaccinatorVaccinationRecordsPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByText(/Unable to Load Records/i)).toBeTruthy();
      expect(screen.getByText(/Network error/i)).toBeTruthy();
    });

    certificateApi.listVaccinatorRecords.mockResolvedValueOnce([]);
    const retryBtn = screen.getByRole('button', { name: /Retry/i });
    fireEvent.click(retryBtn);

    await waitFor(() => {
      expect(screen.getByText(/No Administered Records Yet/i)).toBeTruthy();
    });
  });

  it('renders administered records and filters by search query', async () => {
    const mockRecords = [
      {
        id: 101,
        appointmentId: 1001,
        citizenId: 501,
        citizenName: 'Alice Citizen',
        campaignId: 201,
        campaignName: 'National Campaign',
        centerId: 301,
        centerName: 'Metro Health Center',
        vaccineBatchId: 401,
        batchNumber: 'BATCH-ALPHA',
        vaccineName: 'Pfizer BioNTech',
        doseNumber: 1,
        administrationTime: '2026-09-01T09:00:00Z',
        nextDoseDate: '2026-10-01',
        authorizedNotes: 'Administered in left deltoid.',
        finalizedAt: '2026-09-01T09:05:00Z',
        certificateId: 201,
        certificateReference: 'CERT-ALICE-01',
        certificateStatus: 'VALID',
      },
      {
        id: 102,
        appointmentId: 1002,
        citizenId: 502,
        citizenName: 'Bob Citizen',
        campaignId: 201,
        campaignName: 'National Campaign',
        centerId: 301,
        centerName: 'Metro Health Center',
        vaccineBatchId: 402,
        batchNumber: 'BATCH-BETA',
        vaccineName: 'Moderna Spikevax',
        doseNumber: 2,
        administrationTime: '2026-09-02T10:00:00Z',
        nextDoseDate: null,
        authorizedNotes: 'Second dose completed.',
        finalizedAt: '2026-09-02T10:05:00Z',
        certificateId: 202,
        certificateReference: 'CERT-BOB-02',
        certificateStatus: 'VALID',
      },
    ];

    certificateApi.listVaccinatorRecords.mockResolvedValue(mockRecords);

    render(
      <MemoryRouter>
        <VaccinatorVaccinationRecordsPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getAllByText('Alice Citizen').length).toBeGreaterThan(0);
      expect(screen.getAllByText('Bob Citizen').length).toBeGreaterThan(0);
      expect(screen.getAllByText('CERT-ALICE-01').length).toBeGreaterThan(0);
      expect(screen.getAllByText('CERT-BOB-02').length).toBeGreaterThan(0);
    });

    // Test search filter
    const searchInput = screen.getByLabelText(/Filter vaccination records/i);
    fireEvent.change(searchInput, { target: { value: 'Alice' } });

    expect(screen.getAllByText('Alice Citizen').length).toBeGreaterThan(0);
    expect(screen.queryByText('Bob Citizen')).toBeNull();

    // Clear search
    const clearBtn = screen.getByRole('button', { name: /Clear search/i });
    fireEvent.click(clearBtn);

    expect(screen.getAllByText('Alice Citizen').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Bob Citizen').length).toBeGreaterThan(0);
  });

  it('opens record details modal on view and does NOT show revoke/reissue controls', async () => {
    const mockRecord = {
      id: 101,
      appointmentId: 1001,
      citizenId: 501,
      citizenName: 'Alice Citizen',
      campaignId: 201,
      centerId: 301,
      centerName: 'Metro Health Center',
      vaccineBatchId: 401,
      batchNumber: 'BATCH-ALPHA',
      vaccineName: 'Pfizer BioNTech',
      doseNumber: 1,
      administrationTime: '2026-09-01T09:00:00Z',
      nextDoseDate: '2026-10-01',
      authorizedNotes: 'Administered in left deltoid.',
      finalizedAt: '2026-09-01T09:05:00Z',
      certificateId: 201,
      certificateReference: 'CERT-ALICE-01',
      certificateStatus: 'VALID',
    };

    certificateApi.listVaccinatorRecords.mockResolvedValue([mockRecord]);

    render(
      <MemoryRouter>
        <VaccinatorVaccinationRecordsPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getAllByText('Alice Citizen').length).toBeGreaterThan(0);
    });

    const viewBtn = screen.getByRole('button', { name: /View details for record 101/i });
    fireEvent.click(viewBtn);

    expect(screen.getByRole('heading', { name: /Vaccination Record #101/i })).toBeTruthy();
    expect(screen.getByText('Administered in left deltoid.')).toBeTruthy();
    expect(screen.getByText('This is a permanent immutable clinical record. It cannot be altered or deleted.')).toBeTruthy();

    // Verify absence of revoke and reissue buttons
    expect(screen.queryByRole('button', { name: /revoke/i })).toBeNull();
    expect(screen.queryByRole('button', { name: /reissue/i })).toBeNull();
    expect(screen.queryByRole('button', { name: /edit/i })).toBeNull();

    // Close modal
    const closeBtns = screen.getAllByRole('button', { name: /Close/i });
    fireEvent.click(closeBtns[0]);

    expect(screen.queryByRole('heading', { name: /Vaccination Record #101/i })).toBeNull();
  });

  it('triggers downloadCertificate when PDF download button is clicked', async () => {
    const mockRecord = {
      id: 101,
      appointmentId: 1001,
      citizenId: 501,
      citizenName: 'Alice Citizen',
      certificateId: 201,
      certificateReference: 'CERT-ALICE-01',
      certificateStatus: 'VALID',
    };

    certificateApi.listVaccinatorRecords.mockResolvedValue([mockRecord]);
    certificateApi.downloadCertificate.mockResolvedValue(true);

    render(
      <MemoryRouter>
        <VaccinatorVaccinationRecordsPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getAllByText('Alice Citizen').length).toBeGreaterThan(0);
    });

    const pdfBtn = screen.getByRole('button', { name: /Download certificate PDF for record 101/i });
    fireEvent.click(pdfBtn);

    expect(certificateApi.downloadCertificate).toHaveBeenCalledWith(201, 'CERT-ALICE-01');
  });
});
