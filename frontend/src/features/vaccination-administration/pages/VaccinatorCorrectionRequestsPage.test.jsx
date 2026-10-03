import React from 'react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import VaccinatorCorrectionRequestsPage from './VaccinatorCorrectionRequestsPage';
import * as certificateApi from '../api/certificateApi';

vi.mock('../api/certificateApi', () => ({
  listVaccinatorCorrectionRequests: vi.fn(),
  applyCorrectionRequest: vi.fn(),
  getRecordDetails: vi.fn(),
}));

describe('VaccinatorCorrectionRequestsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  const mockRequests = [
    {
      id: 10,
      vaccinationRecordId: 200,
      citizenId: 50,
      citizenName: 'Alice Green',
      citizenEmail: 'alice@example.com',
      certificateReference: 'CERT-200-XYZ',
      certificateStatus: 'VALID',
      fieldName: 'authorizedNotes',
      oldValue: 'Initial notes',
      newValue: null,
      reason: 'Please update notes with booster guidance',
      status: 'PENDING',
      correctedAt: '2026-10-01T09:00:00Z',
      reviewedAt: null,
      rejectionReason: null,
      reviewNotes: null,
      vaccinatorName: 'Dr. Vaccinator',
    },
    {
      id: 11,
      vaccinationRecordId: 201,
      citizenId: 51,
      citizenName: 'Bob Brown',
      citizenEmail: 'bob@example.com',
      certificateReference: 'CERT-201-ABC',
      certificateStatus: 'VALID',
      fieldName: 'doseNumber',
      oldValue: '1',
      newValue: null,
      reason: 'Should be dose 2',
      status: 'APPROVED',
      correctedAt: '2026-10-01T10:00:00Z',
      reviewedAt: '2026-10-01T11:00:00Z',
      rejectionReason: null,
      reviewNotes: 'Admin verified dose record',
      vaccinatorName: 'Dr. Vaccinator',
    },
  ];

  it('renders loading state then requests list for administered vaccinations', async () => {
    certificateApi.listVaccinatorCorrectionRequests.mockResolvedValue(mockRequests);

    render(
      <MemoryRouter>
        <VaccinatorCorrectionRequestsPage />
      </MemoryRouter>,
    );

    expect(screen.getByText(/Loading correction requests/i)).toBeTruthy();

    await waitFor(() => {
      expect(screen.getByText('Alice Green')).toBeTruthy();
      expect(screen.getByText('Bob Brown')).toBeTruthy();
      expect(screen.getByText('CERT-200-XYZ')).toBeTruthy();
    });

    expect(screen.getByText('Administered Correction Requests')).toBeTruthy();
    expect(screen.getByText('Awaiting Admin Approval')).toBeTruthy();
  });

  it('does NOT permit applying correction when request is still PENDING', async () => {
    certificateApi.listVaccinatorCorrectionRequests.mockResolvedValue([mockRequests[0]]);

    render(
      <MemoryRouter>
        <VaccinatorCorrectionRequestsPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByText('Alice Green')).toBeTruthy();
    });

    // Check that there is no active Apply Correction button for PENDING
    expect(screen.queryByRole('button', { name: /Apply Correction/i })).toBeNull();
    // But View button exists
    expect(screen.getByRole('button', { name: /View/i })).toBeTruthy();
  });

  it('allows vaccinator to apply an APPROVED correction to clinical fields', async () => {
    certificateApi.listVaccinatorCorrectionRequests.mockResolvedValue(mockRequests);
    certificateApi.getRecordDetails.mockResolvedValue({
      id: 201,
      doseNumber: 1,
      vaccineBatchId: 10,
    });
    certificateApi.applyCorrectionRequest.mockResolvedValue({
      id: 11,
      status: 'APPLIED',
      fieldName: 'doseNumber',
      newValue: '2',
    });

    render(
      <MemoryRouter>
        <VaccinatorCorrectionRequestsPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByText('Bob Brown')).toBeTruthy();
    });

    const applyBtn = screen.getByRole('button', { name: /Apply Correction/i });
    fireEvent.click(applyBtn);

    expect(screen.getByText(/Apply Approved Correction #11/i)).toBeTruthy();
    expect(screen.getByText(/Profile Isolation:/i)).toBeTruthy();
    expect(screen.getByText(/Inventory Protection:/i)).toBeTruthy();

    await waitFor(() => {
      expect(screen.getByText('Bob Brown')).toBeTruthy();
    });

    // Verify vaccineBatchId is excluded from selectable fields
    const fieldSelect = screen.getByLabelText(/Select Field to Correct/i);
    const options = Array.from(fieldSelect.querySelectorAll('option')).map((o) => o.value);
    expect(options).not.toContain('vaccineBatchId');
    expect(options).toEqual(['doseNumber', 'administrationTime', 'nextDoseDate', 'authorizedNotes']);

    // Select field and value
    fireEvent.change(fieldSelect, { target: { value: 'doseNumber' } });

    const newValueInput = screen.getByLabelText(/Corrected Replacement Value/i);
    fireEvent.change(newValueInput, { target: { value: '2' } });

    const confirmApplyBtn = screen.getByRole('button', { name: /Apply Correction & Reissue Certificate/i });
    fireEvent.click(confirmApplyBtn);

    await waitFor(() => {
      expect(certificateApi.applyCorrectionRequest).toHaveBeenCalledWith(201, 11, {
        fieldName: 'doseNumber',
        newValue: '2',
      });
    });
  });
});
