import React from 'react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, cleanup, fireEvent, waitFor } from '@testing-library/react';
import CampaignForm from './CampaignForm';

const apiMocks = vi.hoisted(() => ({
  listVaccineTypes: vi.fn(),
  listTargetGroups: vi.fn(),
}));

vi.mock('../api/campaignApi', () => ({
  listVaccineTypes: apiMocks.listVaccineTypes,
  listTargetGroups: apiMocks.listTargetGroups,
}));

describe('CampaignForm — Create and Edit Modes', () => {
  const sampleCampaign = {
    id: 42,
    code: 'CMP-IMMUTABLE-CODE',
    name: 'Initial Campaign Name',
    description: 'Initial Description',
    vaccineTypeId: 1,
    vaccineType: { id: 1, code: 'VAX-01', name: 'Initial Vaccine' },
    targetGroupId: 2,
    targetGroup: { id: 2, code: 'TG-01', description: 'Initial Target' },
    serviceStartDate: '2026-10-01',
    serviceEndDate: '2026-10-31',
    bookingOpenAt: '2026-09-01T09:00:00.000Z',
    bookingCloseAt: '2026-09-30T17:00:00.000Z',
    status: 'ACTIVE',
  };

  beforeEach(() => {
    vi.clearAllMocks();
    apiMocks.listVaccineTypes.mockResolvedValue([
      { id: 1, code: 'VAX-01', name: 'Vaccine One' },
    ]);
    apiMocks.listTargetGroups.mockResolvedValue([
      { id: 2, code: 'TG-01', description: 'Target One' },
    ]);
  });

  afterEach(() => {
    cleanup();
  });

  it('edit mode prefills approved values and displays Edit Campaign header', () => {
    render(
      <CampaignForm
        initialCampaign={sampleCampaign}
        onSubmit={vi.fn()}
        onCancel={vi.fn()}
      />
    );

    expect(screen.getByRole('heading', { level: 5 }).textContent).toContain('Edit Campaign');
    expect(screen.getByLabelText(/Campaign Name/i).value).toBe('Initial Campaign Name');
    expect(screen.getByLabelText(/Description/i).value).toBe('Initial Description');
    expect(screen.getByLabelText(/Service Start Date/i).value).toBe('2026-10-01');
    expect(screen.getByLabelText(/Service End Date/i).value).toBe('2026-10-31');

    // Campaign code is disabled / read-only
    const codeInput = screen.getByLabelText(/Campaign Code/i);
    expect(codeInput.value).toBe('CMP-IMMUTABLE-CODE');
    expect(codeInput.disabled).toBe(true);
  });

  it('edit mode payload contains exactly approved editable fields and omits immutable fields', () => {
    const onSubmit = vi.fn();
    render(
      <CampaignForm
        initialCampaign={sampleCampaign}
        onSubmit={onSubmit}
        onCancel={vi.fn()}
      />
    );

    const nameInput = screen.getByLabelText(/Campaign Name/i);
    fireEvent.change(nameInput, { target: { value: 'Updated Campaign Name' } });

    const submitBtn = screen.getByRole('button', { name: /Save Changes/i });
    fireEvent.click(submitBtn);

    expect(onSubmit).toHaveBeenCalledTimes(1);
    const payload = onSubmit.mock.calls[0][0];

    // Approved editable fields are present
    expect(payload.name).toBe('Updated Campaign Name');
    expect(payload.description).toBe('Initial Description');
    expect(payload.serviceStartDate).toBe('2026-10-01');
    expect(payload.serviceEndDate).toBe('2026-10-31');
    expect(payload.bookingOpenAt).toBeDefined();
    expect(payload.bookingCloseAt).toBeDefined();

    // Immutable fields are strictly omitted
    expect(payload.code).toBeUndefined();
    expect(payload.vaccineTypeId).toBeUndefined();
    expect(payload.vaccineType).toBeUndefined();
    expect(payload.targetGroupId).toBeUndefined();
    expect(payload.targetGroup).toBeUndefined();
    expect(payload.status).toBeUndefined();
    expect(payload.id).toBeUndefined();
  });

  it('edit mode enforces date validation without submitting', () => {
    const onSubmit = vi.fn();
    render(
      <CampaignForm
        initialCampaign={sampleCampaign}
        onSubmit={onSubmit}
        onCancel={vi.fn()}
      />
    );

    // Set end date before start date
    const endDateInput = screen.getByLabelText(/Service End Date/i);
    fireEvent.change(endDateInput, { target: { value: '2026-09-01' } });

    const submitBtn = screen.getByRole('button', { name: /Save Changes/i });
    fireEvent.click(submitBtn);

    expect(onSubmit).not.toHaveBeenCalled();
    expect(screen.getByText(/Service start date must not be after service end date/i)).toBeTruthy();
  });

  it('create mode remains functional and submits complete payload with master data', async () => {
    const onSubmit = vi.fn();
    render(
      <CampaignForm
        onSubmit={onSubmit}
        onCancel={vi.fn()}
      />
    );

    expect(screen.getByRole('heading', { level: 5 }).textContent).toContain('Create New Vaccination Campaign');

    // Wait for master data to load
    await waitFor(() => {
      expect(screen.getByLabelText(/Vaccine Type/i).disabled).toBe(false);
    });

    fireEvent.change(screen.getByLabelText(/Campaign Code/i), { target: { value: 'NEW-CMP-001' } });
    fireEvent.change(screen.getByLabelText(/Campaign Name/i), { target: { value: 'New Campaign' } });
    fireEvent.change(screen.getByLabelText(/Service Start Date/i), { target: { value: '2026-11-01' } });
    fireEvent.change(screen.getByLabelText(/Service End Date/i), { target: { value: '2026-11-30' } });
    fireEvent.change(screen.getByLabelText(/Citizen Booking Open Timestamp/i), { target: { value: '2026-10-01T08:00' } });
    fireEvent.change(screen.getByLabelText(/Citizen Booking Close Timestamp/i), { target: { value: '2026-10-31T18:00' } });

    const submitBtn = screen.getByRole('button', { name: /Save Campaign/i });
    fireEvent.click(submitBtn);

    expect(onSubmit).toHaveBeenCalledTimes(1);
    const payload = onSubmit.mock.calls[0][0];

    expect(payload.code).toBe('NEW-CMP-001');
    expect(payload.name).toBe('New Campaign');
    expect(payload.vaccineTypeId).toBe(1);
    expect(payload.targetGroupId).toBe(2);
    expect(payload.serviceStartDate).toBe('2026-11-01');
    expect(payload.serviceEndDate).toBe('2026-11-30');
  });
});
