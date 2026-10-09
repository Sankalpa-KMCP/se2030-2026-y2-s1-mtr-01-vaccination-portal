import React from 'react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import CitizenVaccinationsPage from './CitizenVaccinationsPage';
import * as certificateApi from '../api/certificateApi';

vi.mock('../api/certificateApi', () => ({
  listMyVaccinations: vi.fn(),
  listMyCertificates: vi.fn(),
  downloadCertificate: vi.fn(),
  listMyCorrectionRequests: vi.fn(),
  requestVaccinationCorrection: vi.fn(),
}));

describe('CitizenVaccinationsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    certificateApi.listMyCorrectionRequests.mockResolvedValue([]);
  });

  afterEach(() => {
    cleanup();
  });

  it('renders loading state initially and then empty state when no records exist', async () => {
    certificateApi.listMyVaccinations.mockResolvedValue([]);
    certificateApi.listMyCertificates.mockResolvedValue([]);
    certificateApi.listMyCorrectionRequests.mockResolvedValue([]);

    render(
      <MemoryRouter>
        <CitizenVaccinationsPage />
      </MemoryRouter>,
    );

    expect(screen.getByText(/Loading your vaccination records/i)).toBeTruthy();

    await waitFor(() => {
      expect(screen.getByText(/No Vaccination Records Yet/i)).toBeTruthy();
    });

    expect(screen.getByRole('link', { name: /Book an Appointment/i })).toBeTruthy();
  });

  it('renders vaccination history and digital certificates when present', async () => {
    const mockRecords = [
      {
        id: 1,
        doseNumber: 1,
        vaccineBatchId: 10,
        centerId: 5,
        administrationTime: '2026-09-01T09:00:00Z',
        nextDoseDate: '2026-10-01',
        certificateId: 101,
        certificateReference: 'CERT-001',
        certificateStatus: 'VALID',
      },
    ];
    const mockCerts = [
      {
        id: 101,
        certificateReference: 'CERT-001',
        status: 'VALID',
        doseNumber: 1,
        issuedAt: '2026-09-01T09:15:00Z',
        administrationTime: '2026-09-01T09:00:00Z',
        vaccineBatchId: 10,
        verificationToken: 'v_tok_12345',
      },
    ];

    certificateApi.listMyVaccinations.mockResolvedValue(mockRecords);
    certificateApi.listMyCertificates.mockResolvedValue(mockCerts);

    render(
      <MemoryRouter>
        <CitizenVaccinationsPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByText('CERT-001')).toBeTruthy();
      expect(screen.getAllByText('VALID').length).toBeGreaterThan(0);
      expect(screen.getByText('Batch #10')).toBeTruthy();
    });
  });

  it('opens detail modal when View button is clicked', async () => {
    const mockCerts = [
      {
        id: 101,
        certificateReference: 'CERT-DETAILS-VIEW',
        status: 'VALID',
        doseNumber: 2,
        issuedAt: '2026-09-01T09:15:00Z',
        administrationTime: '2026-09-01T09:00:00Z',
        vaccineBatchId: 20,
        verificationToken: 'v_token_secret_xyz',
      },
    ];

    certificateApi.listMyVaccinations.mockResolvedValue([{ id: 1, doseNumber: 2 }]);
    certificateApi.listMyCertificates.mockResolvedValue(mockCerts);

    render(
      <MemoryRouter>
        <CitizenVaccinationsPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByText('CERT-DETAILS-VIEW')).toBeTruthy();
    });

    const viewButton = screen.getByRole('button', { name: /View/i });
    fireEvent.click(viewButton);

    expect(screen.getByText('Official Valid Credential')).toBeTruthy();
    expect(screen.getByText('v_token_secret_xyz')).toBeTruthy();

    const closeButtons = screen.getAllByRole('button', { name: /Close/i });
    fireEvent.click(closeButtons[0]);

    expect(screen.queryByText('Official Valid Credential')).toBeNull();
  });

  it('triggers downloadCertificate when download button is clicked', async () => {
    const mockCerts = [
      {
        id: 101,
        certificateReference: 'CERT-DL',
        status: 'VALID',
        doseNumber: 1,
      },
    ];

    certificateApi.listMyVaccinations.mockResolvedValue([{ id: 1, doseNumber: 1 }]);
    certificateApi.listMyCertificates.mockResolvedValue(mockCerts);
    certificateApi.downloadCertificate.mockResolvedValue(true);

    render(
      <MemoryRouter>
        <CitizenVaccinationsPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByText('CERT-DL')).toBeTruthy();
    });

    const pdfButton = screen.getByRole('button', { name: /PDF/i });
    fireEvent.click(pdfButton);

    expect(certificateApi.downloadCertificate).toHaveBeenCalledWith(101, 'CERT-DL');
  });

  it('allows citizen to open correction modal and submit a correction request', async () => {
    const mockRecords = [
      {
        id: 1,
        doseNumber: 1,
        vaccineBatchId: 10,
        centerId: 5,
        administrationTime: '2026-09-01T09:00:00Z',
        certificateId: 101,
        certificateReference: 'CERT-001',
        certificateStatus: 'VALID',
      },
    ];
    const mockCerts = [
      {
        id: 101,
        vaccinationRecordId: 1,
        certificateReference: 'CERT-001',
        status: 'VALID',
        doseNumber: 1,
      },
    ];

    certificateApi.listMyVaccinations.mockResolvedValue(mockRecords);
    certificateApi.listMyCertificates.mockResolvedValue(mockCerts);
    certificateApi.listMyCorrectionRequests.mockResolvedValue([]);
    certificateApi.requestVaccinationCorrection.mockResolvedValue({ id: 1, status: 'PENDING' });

    render(
      <MemoryRouter>
        <CitizenVaccinationsPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByText('CERT-001')).toBeTruthy();
    });

    const requestCorrectionButtons = screen.getAllByRole('button', { name: /Request Correction/i });
    expect(requestCorrectionButtons.length).toBeGreaterThan(0);
    fireEvent.click(requestCorrectionButtons[0]);

    expect(screen.getAllByText(/Request Correction/i).length).toBeGreaterThan(0);
    expect(screen.getByText(/Citizen name\/profile correction requires a separate identity\/profile correction workflow/i)).toBeTruthy();

    const textarea = screen.getByLabelText(/Describe what you believe should be corrected/i);
    fireEvent.change(textarea, { target: { value: 'My dose number is actually dose 2' } });

    const submitBtn = screen.getByRole('button', { name: /Submit Request/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(certificateApi.requestVaccinationCorrection).toHaveBeenCalledWith(1, 'My dose number is actually dose 2');
    });
  });

  it('renders a single unified table without redundant separate vaccination history section', async () => {
    const mockRecords = [
      {
        id: 1,
        doseNumber: 1,
        vaccineBatchId: 10,
        centerId: 5,
        administrationTime: '2026-09-01T09:00:00Z',
        certificateId: 101,
        certificateReference: 'CERT-001',
        certificateStatus: 'VALID',
      },
    ];
    const mockCerts = [
      {
        id: 101,
        certificateReference: 'CERT-001',
        status: 'VALID',
        doseNumber: 1,
      },
    ];

    certificateApi.listMyVaccinations.mockResolvedValue(mockRecords);
    certificateApi.listMyCertificates.mockResolvedValue(mockCerts);

    render(
      <MemoryRouter>
        <CitizenVaccinationsPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByText(/Vaccination Records & Certificates/i)).toBeTruthy();
    });

    // Verify there is only one table
    const tables = screen.getAllByRole('table');
    expect(tables.length).toBe(1);

    // Verify redundant "Vaccination History" section heading is NOT present
    expect(screen.queryByRole('heading', { name: /^Vaccination History$/i })).toBeNull();

    // Verify no logged-in "Verify" button is present
    expect(screen.queryByRole('button', { name: /^Verify$/i })).toBeNull();
  });

  it('gracefully handles a record without a certificate by showing pending issuance', async () => {
    const mockRecords = [
      {
        id: 2,
        doseNumber: 1,
        vaccineBatchId: 15,
        centerId: 5,
        administrationTime: '2026-09-02T09:00:00Z',
        certificateId: null,
        certificateReference: null,
        certificateStatus: null,
      },
    ];

    certificateApi.listMyVaccinations.mockResolvedValue(mockRecords);
    certificateApi.listMyCertificates.mockResolvedValue([]);

    render(
      <MemoryRouter>
        <CitizenVaccinationsPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByText(/Pending issuance/i)).toBeTruthy();
      expect(screen.getByText('Batch #15')).toBeTruthy();
    });

    // View and PDF buttons should NOT be rendered when certificate is absent
    expect(screen.queryByRole('button', { name: /View/i })).toBeNull();
    expect(screen.queryByRole('button', { name: /PDF/i })).toBeNull();
  });
});
