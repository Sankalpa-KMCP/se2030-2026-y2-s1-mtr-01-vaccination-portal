import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

const mocks = vi.hoisted(() => ({
  listScreeningReviewQueue: vi.fn(),
  listCampaigns: vi.fn(),
}));

vi.mock('../api/screeningApi', () => ({
  listScreeningReviewQueue: mocks.listScreeningReviewQueue,
}));
vi.mock('../../campaign-management/api/campaignApi', () => ({
  listCampaigns: mocks.listCampaigns,
}));

import HealthcareOfficerScreeningReviewQueuePage from './HealthcareOfficerScreeningReviewQueuePage';

const submissions = [
  {
    id: 12,
    citizenId: 72,
    campaignId: 99,
    status: 'PENDING_REVIEW',
    preliminaryResult: 'PRELIMINARY_REVIEW_REQUIRED',
    submittedAt: '2026-09-06T10:00:00Z',
    createdAt: '2026-09-06T09:00:00Z',
  },
  {
    id: 11,
    citizenId: 71,
    campaignId: 22,
    status: 'PENDING_REVIEW',
    preliminaryResult: 'PRELIMINARY_PASS',
    submittedAt: '2026-09-05T10:00:00Z',
    createdAt: '2026-09-05T09:00:00Z',
  },
];

function renderPage() {
  return render(<MemoryRouter><HealthcareOfficerScreeningReviewQueuePage /></MemoryRouter>);
}

describe('HealthcareOfficerScreeningReviewQueuePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.listScreeningReviewQueue.mockResolvedValue(submissions);
    mocks.listCampaigns.mockResolvedValue([{ id: 22, name: 'Adult Immunisation', code: 'ADULT-26' }]);
  });

  afterEach(cleanup);

  it('shows the initial loading state', () => {
    mocks.listScreeningReviewQueue.mockReturnValue(new Promise(() => {}));
    renderPage();
    expect(screen.getByRole('status').textContent).toContain('Loading screening review queue...');
  });

  it('shows a queue API error and supports a manual retry', async () => {
    mocks.listScreeningReviewQueue
      .mockRejectedValueOnce(Object.assign(new Error('Queue unavailable'), { status: 500 }))
      .mockResolvedValueOnce([]);
    renderPage();

    expect(await screen.findByText('Queue unavailable')).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: 'Retry loading queue' }));
    await screen.findByText('No screenings are currently awaiting review.');
    expect(mocks.listScreeningReviewQueue).toHaveBeenCalledTimes(2);
  });

  it('renders an empty queue as a neutral state', async () => {
    mocks.listScreeningReviewQueue.mockResolvedValue([]);
    renderPage();
    expect(await screen.findByText('No screenings are currently awaiting review.')).toBeTruthy();
    expect(screen.queryByText('Request Error')).toBeNull();
  });

  it('renders enriched rows oldest first with safe campaign and preliminary wording', async () => {
    renderPage();
    await screen.findByText('Citizen #71');

    const rows = screen.getAllByRole('row').slice(1);
    expect(within(rows[0]).getByText('Citizen #71')).toBeTruthy();
    expect(within(rows[0]).getByText('Adult Immunisation')).toBeTruthy();
    expect(within(rows[0]).getByText('ADULT-26')).toBeTruthy();
    expect(within(rows[0]).getByText('Preliminary pass')).toBeTruthy();
    expect(within(rows[0]).getByText('Pending review')).toBeTruthy();
    expect(within(rows[0]).getByRole('link', { name: 'Review' }).getAttribute('href'))
      .toBe('/app/staff/screening/reviews/11');

    expect(within(rows[1]).getByText('Citizen #72')).toBeTruthy();
    expect(within(rows[1]).getByText('Campaign #99')).toBeTruthy();
    expect(within(rows[1]).getByText('Healthcare review required')).toBeTruthy();
    expect(screen.queryByText('Eligible')).toBeNull();
    expect(rows[0].textContent).toContain('2026');
  });

  it('filters by campaign through the server queue query', async () => {
    renderPage();
    await screen.findByText('Citizen #71');

    fireEvent.change(screen.getByLabelText('Campaign'), { target: { value: '22' } });
    await waitFor(() => {
      expect(mocks.listScreeningReviewQueue).toHaveBeenLastCalledWith({ campaignId: '22' });
    });
  });

  it('refreshes both queue and optional campaign metadata', async () => {
    renderPage();
    await screen.findByText('Citizen #71');
    fireEvent.click(screen.getByRole('button', { name: 'Refresh' }));

    await waitFor(() => {
      expect(mocks.listScreeningReviewQueue).toHaveBeenCalledTimes(2);
      expect(mocks.listCampaigns).toHaveBeenCalledTimes(2);
    });
  });

  it('keeps the queue visible when campaign metadata fails', async () => {
    mocks.listCampaigns.mockRejectedValue(new Error('Campaign service unavailable'));
    renderPage();

    expect(await screen.findByText('Citizen #71')).toBeTruthy();
    expect(screen.getByText('Campaign #22')).toBeTruthy();
    expect(screen.getByText(/Campaign details could not be loaded/)).toBeTruthy();
  });
});
