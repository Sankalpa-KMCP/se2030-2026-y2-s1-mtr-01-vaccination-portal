import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import {
  approveStaffRequest,
  getStaffRequest,
  listStaffRequests,
  rejectStaffRequest,
} from '../api/adminStaffRequestsApi';
import PendingStaffRequestsPage from './PendingStaffRequestsPage';

vi.mock('../api/adminStaffRequestsApi', () => ({
  approveStaffRequest: vi.fn(),
  getStaffRequest: vi.fn(),
  listStaffRequests: vi.fn(),
  rejectStaffRequest: vi.fn(),
}));

const request = {
  id: 41,
  fullName: 'Pending Officer',
  email: 'pending@example.com',
  phoneNumber: '+94 77 100 2000',
  requestedRoleCode: 'ROLE_HEALTHCARE_OFFICER',
  accountStatus: 'PENDING_APPROVAL',
  createdAt: '2026-08-31T10:00:00Z',
};

function renderPage() {
  return render(<MemoryRouter><PendingStaffRequestsPage /></MemoryRouter>);
}

describe('PendingStaffRequestsPage', () => {
  beforeEach(() => {
    approveStaffRequest.mockReset();
    getStaffRequest.mockReset();
    listStaffRequests.mockReset();
    rejectStaffRequest.mockReset();
  });

  afterEach(cleanup);

  it('renders pending request details and review actions', async () => {
    listStaffRequests.mockResolvedValue([request]);
    renderPage();
    expect(await screen.findByText('Pending Officer')).toBeTruthy();
    expect(screen.getByText('Healthcare Officer')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'View' })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Approve' })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Reject' })).toBeTruthy();
  });

  it('shows the empty pending-list state', async () => {
    listStaffRequests.mockResolvedValue([]);
    renderPage();
    expect(await screen.findByRole('heading', { name: 'No pending requests' })).toBeTruthy();
  });

  it('loads request detail for View', async () => {
    listStaffRequests.mockResolvedValue([request]);
    getStaffRequest.mockResolvedValue(request);
    renderPage();
    fireEvent.click(await screen.findByRole('button', { name: 'View' }));
    expect(await screen.findByRole('dialog')).toBeTruthy();
    expect(getStaffRequest).toHaveBeenCalledWith(41);
  });

  it('allows an approved role to differ and refreshes the list', async () => {
    listStaffRequests.mockResolvedValueOnce([request]).mockResolvedValueOnce([]);
    approveStaffRequest.mockResolvedValue({ ...request, accountStatus: 'ACTIVE' });
    renderPage();
    fireEvent.click(await screen.findByRole('button', { name: 'Approve' }));
    fireEvent.change(screen.getByLabelText('Approved Role'), { target: { value: 'ROLE_CENTER_MANAGER' } });
    fireEvent.click(screen.getByRole('button', { name: 'Approve Request' }));

    await waitFor(() => expect(approveStaffRequest).toHaveBeenCalledWith(41, 'ROLE_CENTER_MANAGER'));
    expect(await screen.findByText(/staff account was approved/i)).toBeTruthy();
    expect(await screen.findByRole('heading', { name: 'No pending requests' })).toBeTruthy();
    expect(listStaffRequests).toHaveBeenCalledTimes(2);
  });

  it('renders each approvable role option exactly once without duplicates', async () => {
    listStaffRequests.mockResolvedValue([request]);
    renderPage();
    fireEvent.click(await screen.findByRole('button', { name: 'Approve' }));

    const roleSelect = screen.getByLabelText('Approved Role');
    const options = Array.from(roleSelect.querySelectorAll('option'));
    const optionValues = options.map((opt) => opt.value);
    const optionLabels = options.map((opt) => opt.textContent);

    expect(new Set(optionValues).size).toBe(optionValues.length);
    expect(optionValues.filter((v) => v === 'ROLE_VACCINATOR')).toHaveLength(1);
    expect(optionLabels.filter((l) => l === 'Vaccinator')).toHaveLength(1);
    expect(optionValues).toEqual([
      'ROLE_HEALTHCARE_OFFICER',
      'ROLE_CENTER_MANAGER',
      'ROLE_CAMPAIGN_OFFICER',
      'ROLE_INVENTORY_OFFICER',
      'ROLE_VACCINATOR',
    ]);
  });

  it('requires a nonblank rejection reason', async () => {
    listStaffRequests.mockResolvedValue([request]);
    renderPage();
    fireEvent.click(await screen.findByRole('button', { name: 'Reject' }));
    const submit = screen.getByRole('button', { name: 'Reject Request' });
    fireEvent.submit(submit.closest('form'));
    expect(screen.getByRole('alert').textContent).toContain('rejection reason is required');
    expect(rejectStaffRequest).not.toHaveBeenCalled();
  });

  it('rejects with a reason and refreshes the list', async () => {
    listStaffRequests.mockResolvedValueOnce([request]).mockResolvedValueOnce([]);
    rejectStaffRequest.mockResolvedValue({ ...request, accountStatus: 'REJECTED' });
    renderPage();
    fireEvent.click(await screen.findByRole('button', { name: 'Reject' }));
    fireEvent.change(screen.getByLabelText('Rejection Reason'), { target: { value: 'Unable to verify employment' } });
    fireEvent.click(screen.getByRole('button', { name: 'Reject Request' }));

    await waitFor(() => expect(rejectStaffRequest).toHaveBeenCalledWith(41, 'Unable to verify employment'));
    expect(await screen.findByText(/account request was rejected/i)).toBeTruthy();
    expect(await screen.findByRole('heading', { name: 'No pending requests' })).toBeTruthy();
  });
});
