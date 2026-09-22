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
}));

describe('CitizenVaccinationsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  it('renders loading state initially and then empty state when no records exist', async () => {
    certificateApi.listMyVaccinations.mockResolvedValue([]);
    certificateApi.listMyCertificates.mockResolvedValue([]);

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
});
