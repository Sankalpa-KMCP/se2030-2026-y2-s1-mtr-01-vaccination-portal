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

    fireEvent.click(screen.getByRole('button', { name: /Actions for/ }));
    const editBtn = screen.getByRole('button', { name: 'Edit' });
    fireEvent.click(editBtn);

    expect(await screen.findByRole('heading', { name: 'Edit Campaign' })).toBeTruthy();
    expect(campaignApiMocks.getCampaignById).toHaveBeenCalledWith(10);
    expect(screen.getByLabelText(/Campaign Name/i).value).toBe('Test Flu Campaign');
    expect(document.activeElement).toBe(screen.getByLabelText(/Campaign Name/i));
  });

  it('submitting edit form invokes updateCampaign, shows success alert, and refreshes list', async () => {
    const updatedCampaignResponse = {
      ...sampleCampaignDetails,
      name: 'Updated Flu Campaign 2026',
    };
    campaignApiMocks.updateCampaign.mockResolvedValue(updatedCampaignResponse);

    render(<CampaignManagementPage />);

    expect(await screen.findByText('Test Flu Campaign')).toBeTruthy();

    fireEvent.change(screen.getByLabelText('Search campaigns'), { target: { value: 'Flu' } });
    fireEvent.change(screen.getByLabelText('Status'), { target: { value: 'ACTIVE' } });
    await waitFor(() => expect(campaignApiMocks.listCampaigns).toHaveBeenCalledWith('ACTIVE'));
    await screen.findByRole('button', { name: /Actions for/ });

    fireEvent.click(screen.getByRole('button', { name: /Actions for/ }));
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
    expect(campaignApiMocks.listCampaigns).toHaveBeenCalledTimes(3);
    expect(screen.getByLabelText('Search campaigns').value).toBe('Flu');
    expect(screen.getByLabelText('Status').value).toBe('ACTIVE');
  });

  it('keeps the toolbar available while the status API loads and resets both filters', async () => {
    render(<CampaignManagementPage />);
    await screen.findByText('Test Flu Campaign');
    let resolveList;
    campaignApiMocks.listCampaigns.mockImplementationOnce(() => new Promise((resolve) => { resolveList = resolve; }));
    fireEvent.change(screen.getByLabelText('Search campaigns'), { target: { value: 'flu' } });
    fireEvent.change(screen.getByLabelText('Status'), { target: { value: 'ACTIVE' } });
    await waitFor(() => expect(screen.getByText('Loading campaigns…')).toBeTruthy());
    expect(screen.getByRole('heading', { name: 'Vaccination Campaigns' })).toBeTruthy();
    expect(screen.getByLabelText('Search campaigns').value).toBe('flu');
    resolveList([sampleCampaignSummary]);
    await screen.findByText('Test Flu Campaign');
    fireEvent.click(screen.getByRole('button', { name: 'Clear filters' }));
    await waitFor(() => expect(campaignApiMocks.listCampaigns).toHaveBeenLastCalledWith(null));
    expect(screen.getByLabelText('Search campaigns').value).toBe('');
    expect(screen.getByLabelText('Status').value).toBe('');
  });

  it('shows API failures without suggesting that the campaign database is empty', async () => {
    campaignApiMocks.listCampaigns.mockRejectedValueOnce(new Error('Campaign service unavailable'));
    render(<CampaignManagementPage />);
    expect(await screen.findByText('Campaign service unavailable')).toBeTruthy();
    expect(screen.getByText('Results unavailable')).toBeTruthy();
    expect(screen.queryByText('No campaigns yet')).toBeNull();
    expect(screen.getByLabelText('Status')).toBeTruthy();
  });
});
