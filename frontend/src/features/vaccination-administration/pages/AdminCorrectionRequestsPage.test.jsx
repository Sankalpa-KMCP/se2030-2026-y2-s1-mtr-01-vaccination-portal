import React from 'react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import AdminCorrectionRequestsPage from './AdminCorrectionRequestsPage';
import * as certificateApi from '../api/certificateApi';

vi.mock('../api/certificateApi', () => ({
  listAdminCorrectionRequests: vi.fn(),
  approveCorrectionRequest: vi.fn(),
  rejectCorrectionRequest: vi.fn(),
}));

describe('AdminCorrectionRequestsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  const mockRequests = [
    {
      id: 1,
      vaccinationRecordId: 100,
      citizenId: 50,
      citizenName: 'Jane Doe',
      citizenEmail: 'jane@example.com',
      certificateReference: 'CERT-100-AAA',
      certificateStatus: 'VALID',
      fieldName: 'authorizedNotes',
      oldValue: 'None',
      newValue: null,
      reason: 'Please correct notes',
      status: 'PENDING',
      correctedAt: '2026-10-01T10:00:00Z',
      reviewedAt: null,
      rejectionReason: null,
      reviewNotes: null,
      vaccinatorName: 'Dr. Smith',
    },
    {
      id: 2,
      vaccinationRecordId: 101,
      citizenId: 51,
      citizenName: 'John Smith',
      citizenEmail: 'john@example.com',
      certificateReference: 'CERT-101-BBB',
      certificateStatus: 'VALID',
      fieldName: 'doseNumber',
      oldValue: '1',
      newValue: '2',
      reason: 'Dose number was recorded wrong',
      status: 'APPROVED',
      correctedAt: '2026-10-01T11:00:00Z',
      reviewedAt: '2026-10-01T12:00:00Z',
      rejectionReason: null,
      reviewNotes: 'Approved for vaccinator correction',
      vaccinatorName: 'Dr. Smith',
    },
  ];

  it('renders loading state then requests table and summary cards', async () => {
    certificateApi.listAdminCorrectionRequests.mockResolvedValue(mockRequests);

    render(
      <MemoryRouter>
        <AdminCorrectionRequestsPage />
      </MemoryRouter>,
    );

    expect(screen.getByText(/Loading correction requests/i)).toBeTruthy();

    await waitFor(() => {
      expect(screen.getByText('Jane Doe')).toBeTruthy();
      expect(screen.getByText('John Smith')).toBeTruthy();
      expect(screen.getByText('CERT-100-AAA')).toBeTruthy();
    });

    expect(screen.getByText('Citizen Correction Requests')).toBeTruthy();
    expect(screen.getByText(/Administrative Governance:/i)).toBeTruthy();
  });

  it('allows admin to approve a pending request with review notes', async () => {
    certificateApi.listAdminCorrectionRequests.mockResolvedValue(mockRequests);
    certificateApi.approveCorrectionRequest.mockResolvedValue({
      ...mockRequests[0],
      status: 'APPROVED',
      reviewNotes: 'Verified with paper records',
    });

    render(
      <MemoryRouter>
        <AdminCorrectionRequestsPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByText('Jane Doe')).toBeTruthy();
    });

    const approveBtn = screen.getByRole('button', { name: /^Approve$/i });
    fireEvent.click(approveBtn);

    expect(screen.getByText(/Approve Correction Request #1/i)).toBeTruthy();

    const noteInput = screen.getByLabelText(/Approval Notes/i);
    fireEvent.change(noteInput, { target: { value: 'Verified with paper records' } });

    const confirmApproveBtn = screen.getByRole('button', { name: /Confirm Approval/i });
    fireEvent.click(confirmApproveBtn);

    await waitFor(() => {
      expect(certificateApi.approveCorrectionRequest).toHaveBeenCalledWith(100, 1, 'Verified with paper records');
    });
  });

  it('allows admin to reject a pending request with rejection reason', async () => {
    certificateApi.listAdminCorrectionRequests.mockResolvedValue(mockRequests);
    certificateApi.rejectCorrectionRequest.mockResolvedValue({
      ...mockRequests[0],
      status: 'REJECTED',
      rejectionReason: 'Invalid documentation provided',
    });

    render(
      <MemoryRouter>
        <AdminCorrectionRequestsPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByText('Jane Doe')).toBeTruthy();
    });

    const rejectBtn = screen.getByRole('button', { name: /^Reject$/i });
    fireEvent.click(rejectBtn);

    expect(screen.getByText(/Reject Correction Request #1/i)).toBeTruthy();

    const reasonInput = screen.getByLabelText(/Rejection Reason/i);
    fireEvent.change(reasonInput, { target: { value: 'Invalid documentation provided' } });

    const confirmRejectBtn = screen.getByRole('button', { name: /Confirm Rejection/i });
    fireEvent.click(confirmRejectBtn);

    await waitFor(() => {
      expect(certificateApi.rejectCorrectionRequest).toHaveBeenCalledWith(100, 1, 'Invalid documentation provided');
    });
  });

  it('does NOT provide apply or direct clinical edit controls to admin', async () => {
    certificateApi.listAdminCorrectionRequests.mockResolvedValue(mockRequests);

    render(
      <MemoryRouter>
        <AdminCorrectionRequestsPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByText('Jane Doe')).toBeTruthy();
    });

    // Ensure no "Apply Correction" button exists on the Admin page
    expect(screen.queryByRole('button', { name: /Apply Correction/i })).toBeNull();
    expect(screen.queryByText(/Edit Dose/i)).toBeNull();
    expect(screen.queryByText(/Change Batch/i)).toBeNull();
  });
});
