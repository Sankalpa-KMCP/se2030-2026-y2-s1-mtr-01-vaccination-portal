import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

const campaignApiMocks = vi.hoisted(() => ({ listCampaigns: vi.fn() }));
vi.mock('../../campaign-management/api/campaignApi', () => campaignApiMocks);

import CitizenScreeningCampaignsPage from './CitizenScreeningCampaignsPage';

function renderPage() {
  return render(<MemoryRouter><CitizenScreeningCampaignsPage /></MemoryRouter>);
}

describe('CitizenScreeningCampaignsPage', () => {
  beforeEach(() => vi.clearAllMocks());
  afterEach(cleanup);

  it('shows a campaign loading state', () => {
    campaignApiMocks.listCampaigns.mockReturnValue(new Promise(() => {}));
    renderPage();
    expect(screen.getByRole('status')).toBeTruthy();
    expect(screen.getByRole('heading', { level: 1, name: 'Choose a campaign' })).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Back to Dashboard' }).getAttribute('href')).toBe('/app/citizen');
  });

  it('shows a normal empty state when no intake campaigns are available', async () => {
    campaignApiMocks.listCampaigns.mockResolvedValue([
      { id: 1, name: 'Draft campaign', status: 'DRAFT' },
      { id: 2, name: 'Suspended campaign', status: 'SUSPENDED' },
    ]);
    renderPage();
    expect(await screen.findByText('No vaccination campaigns are currently available for screening.')).toBeTruthy();
    expect(screen.queryByText('Request Error')).toBeNull();
  });

  it('displays campaign API errors', async () => {
    campaignApiMocks.listCampaigns.mockRejectedValue(new Error('Campaign service unavailable'));
    renderPage();
    expect(await screen.findByText('Campaign service unavailable')).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Back to Dashboard' })).toBeTruthy();
  });

  it('shows only published and active campaigns with screening navigation', async () => {
    campaignApiMocks.listCampaigns.mockResolvedValue([
      { id: 3, code: 'PUB', name: 'Published Campaign', status: 'PUBLISHED', vaccineTypeCode: 'VAC-A', targetGroupCode: 'ADULT' },
      { id: 4, code: 'ACT', name: 'Active Campaign', status: 'ACTIVE', vaccineTypeCode: 'VAC-B', targetGroupCode: 'SENIOR' },
      { id: 5, code: 'DONE', name: 'Completed Campaign', status: 'COMPLETED' },
    ]);
    renderPage();

    await waitFor(() => expect(screen.getByText('Published Campaign')).toBeTruthy());
    expect(screen.getByText('Active Campaign')).toBeTruthy();
    expect(screen.queryByText('Completed Campaign')).toBeNull();
    const links = screen.getAllByRole('link', { name: 'Start screening' });
    expect(links.map((link) => link.getAttribute('href'))).toEqual([
      '/app/screening/campaigns/3',
      '/app/screening/campaigns/4',
    ]);
  });
});
