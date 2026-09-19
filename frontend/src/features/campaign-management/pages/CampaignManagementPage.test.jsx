import React from 'react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, cleanup, fireEvent, waitFor } from '@testing-library/react';
import CampaignManagementPage from './CampaignManagementPage';

const authMocks = vi.hoisted(() => ({
  currentUser: { id: 1, roles: ['ROLE_ADMIN'] },
}));

const campaignApiMocks = vi.hoisted(() => ({
  listCampaigns: vi.fn(),
  getCampaignById: vi.fn(),
  updateCampaign: vi.fn(),
  createCampaign: vi.fn(),
  getCampaignLifecycleEvents: vi.fn(),
  listVaccineTypes: vi.fn(),
  listTargetGroups: vi.fn(),
}));

vi.mock('../../../auth/context/AuthContext', () => ({
  useAuth: () => ({ user: authMocks.currentUser }),
}));

vi.mock('../api/campaignApi', () => campaignApiMocks);

vi.mock('../api/campaignConfigurationApi', () => ({
  getPublishedQuestionnaireVersions: vi.fn().mockResolvedValue([]),
}));

describe('CampaignManagementPage — Campaign Update Flow', () => {
  const sampleCampaignSummary = {
    id: 10,
    code: 'CMP-TEST-10',
    name: 'Test Flu Campaign',
    status: 'ACTIVE',
    vaccineTypeCode: 'FLU-01',
    targetGroupCode: 'ADULT',
    serviceStartDate: '2026-10-01',
    serviceEndDate: '2026-10-31',
  };

  const sampleCampaignDetails = {
    id: 10,
    code: 'CMP-TEST-10',
    name: 'Test Flu Campaign',
    description: 'Detailed description for test flu campaign',
    status: 'ACTIVE',
    vaccineType: { id: 1, code: 'FLU-01', name: 'Flu Vaccine' },
    targetGroup: { id: 2, code: 'ADULT', description: 'Adult Target Group' },
    serviceStartDate: '2026-10-01',
    serviceEndDate: '2026-10-31',
    bookingOpenAt: '2026-09-01T09:00:00.000Z',
    bookingCloseAt: '2026-09-30T17:00:00.000Z',
  };

  beforeEach(() => {
    vi.clearAllMocks();
    authMocks.currentUser = { id: 1, roles: ['ROLE_ADMIN'] };
    campaignApiMocks.listCampaigns.mockResolvedValue([sampleCampaignSummary]);
    campaignApiMocks.getCampaignById.mockResolvedValue(sampleCampaignDetails);
    campaignApiMocks.listVaccineTypes.mockResolvedValue([]);
    campaignApiMocks.listTargetGroups.mockResolvedValue([]);
    campaignApiMocks.getCampaignLifecycleEvents.mockResolvedValue([]);
  });

  afterEach(() => {
    cleanup();
  });

  it('initiating edit opens CampaignForm in edit mode and pre-populates details', async () => {
    render(<CampaignManagementPage />);

    expect(await screen.findByText('Test Flu Campaign')).toBeTruthy();

    const editBtn = screen.getByRole('button', { name: 'Edit' });
    fireEvent.click(editBtn);

    expect(await screen.findByRole('heading', { name: 'Edit Campaign' })).toBeTruthy();
    expect(campaignApiMocks.getCampaignById).toHaveBeenCalledWith(10);
    expect(screen.getByLabelText(/Campaign Name/i).value).toBe('Test Flu Campaign');
  });

  it('submitting edit form invokes updateCampaign, shows success alert, and refreshes list', async () => {
    const updatedCampaignResponse = {
      ...sampleCampaignDetails,
      name: 'Updated Flu Campaign 2026',
    };
    campaignApiMocks.updateCampaign.mockResolvedValue(updatedCampaignResponse);

    render(<CampaignManagementPage />);

    expect(await screen.findByText('Test Flu Campaign')).toBeTruthy();

    const editBtn = screen.getByRole('button', { name: 'Edit' });
    fireEvent.click(editBtn);

    expect(await screen.findByRole('heading', { name: 'Edit Campaign' })).toBeTruthy();

    const nameInput = screen.getByLabelText(/Campaign Name/i);
    expect(nameInput.value).toBe('Test Flu Campaign');
    fireEvent.change(nameInput, { target: { value: 'Updated Flu Campaign 2026' } });

    const saveChangesBtn = screen.getByRole('button', { name: /Save Changes/i });
    fireEvent.click(saveChangesBtn);

    await waitFor(() => {
      expect(campaignApiMocks.updateCampaign).toHaveBeenCalledTimes(1);
    });

    expect(campaignApiMocks.updateCampaign).toHaveBeenCalledWith(
      10,
      expect.objectContaining({
        name: 'Updated Flu Campaign 2026',
        serviceStartDate: '2026-10-01',
        serviceEndDate: '2026-10-31',
      })
    );

    // Form is dismissed and success banner is shown
    expect(await screen.findByText(/Campaign "Updated Flu Campaign 2026" updated successfully/i)).toBeTruthy();
    expect(screen.queryByRole('heading', { name: 'Edit Campaign' })).toBeNull();

    // Campaigns refreshed
    expect(campaignApiMocks.listCampaigns).toHaveBeenCalledTimes(2);
  });
});
