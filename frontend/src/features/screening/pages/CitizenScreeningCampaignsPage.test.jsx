import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

const campaignApiMocks = vi.hoisted(() => ({ listCitizenCampaigns: vi.fn() }));
vi.mock('../../campaign-management/api/campaignApi', () => campaignApiMocks);

import CitizenScreeningCampaignsPage from './CitizenScreeningCampaignsPage';

function renderPage() {
  return render(<MemoryRouter><CitizenScreeningCampaignsPage /></MemoryRouter>);
}

describe('CitizenScreeningCampaignsPage', () => {
  beforeEach(() => vi.clearAllMocks());
  afterEach(cleanup);

  it('shows a campaign loading state', () => {
    campaignApiMocks.listCitizenCampaigns.mockReturnValue(new Promise(() => {}));
    renderPage();
    expect(screen.getByRole('status')).toBeTruthy();
    expect(screen.getByRole('heading', { level: 1, name: 'Choose a campaign' })).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Back to Dashboard' }).getAttribute('href')).toBe('/app/citizen');
  });

  it('shows a normal empty state when no intake campaigns are available', async () => {
    campaignApiMocks.listCitizenCampaigns.mockResolvedValue([]);
    renderPage();
    expect(await screen.findByText('No vaccination campaigns are currently available for screening.')).toBeTruthy();
    expect(screen.queryByText('Request Error')).toBeNull();
  });

  it('displays campaign API errors', async () => {
    campaignApiMocks.listCitizenCampaigns.mockRejectedValue(new Error('Campaign service unavailable'));
    renderPage();
    expect(await screen.findByText('Campaign service unavailable')).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Back to Dashboard' })).toBeTruthy();
  });

  it('calls citizen discovery and renders published and active campaigns with screening navigation', async () => {
    campaignApiMocks.listCitizenCampaigns.mockResolvedValue([
      {
        id: 3,
        code: 'PUB',
        name: 'Published Campaign',
        status: 'PUBLISHED',
        vaccineTypeCode: 'VAC-A',
        targetGroupCode: 'ADULT',
        currentlyAcceptingScreening: true,
      },
      {
        id: 4,
        code: 'ACT',
        name: 'Active Campaign',
        status: 'ACTIVE',
        vaccineTypeCode: 'VAC-B',
        targetGroupCode: 'SENIOR',
        currentlyAcceptingScreening: true,
      },
    ]);
    renderPage();

    await waitFor(() => expect(screen.getByText('Published Campaign')).toBeTruthy());
    expect(screen.getByText('Active Campaign')).toBeTruthy();
    expect(campaignApiMocks.listCitizenCampaigns).toHaveBeenCalledTimes(1);
    expect(screen.getByRole('heading', { level: 2, name: 'Campaign directory' })).toBeTruthy();
    expect(screen.getByText('2 available')).toBeTruthy();

    const links = screen.getAllByRole('link', { name: 'Start screening' });
    expect(links.map((link) => link.getAttribute('href'))).toEqual([
      '/app/screening/campaigns/3',
      '/app/screening/campaigns/4',
    ]);
    expect(links[0].className).toContain('citizen-action--primary');
  });

  it('disables start screening when currentlyAcceptingScreening is false', async () => {
    campaignApiMocks.listCitizenCampaigns.mockResolvedValue([
      {
        id: 5,
        code: 'PUB-CLOSED',
        name: 'Closed Window Campaign',
        status: 'PUBLISHED',
        vaccineTypeCode: 'VAC-C',
        targetGroupCode: 'YOUTH',
        currentlyAcceptingScreening: false,
      },
      {
        id: 6,
        code: 'ACT-OPEN',
        name: 'Open Window Campaign',
        status: 'ACTIVE',
        vaccineTypeCode: 'VAC-D',
        targetGroupCode: 'ALL',
        currentlyAcceptingScreening: true,
      },
    ]);
    renderPage();

    await waitFor(() => expect(screen.getByText('Closed Window Campaign')).toBeTruthy());

    // Disabled button for closed campaign
    const disabledButton = screen.getByRole('button', { name: 'Start screening' });
    expect(disabledButton.disabled).toBe(true);
    expect(disabledButton.className).toContain('citizen-action--secondary');

    // Active link only for open campaign
    const activeLinks = screen.getAllByRole('link', { name: 'Start screening' });
    expect(activeLinks).toHaveLength(1);
    expect(activeLinks[0].getAttribute('href')).toBe('/app/screening/campaigns/6');
    expect(activeLinks[0].className).toContain('citizen-action--primary');
  });
});
