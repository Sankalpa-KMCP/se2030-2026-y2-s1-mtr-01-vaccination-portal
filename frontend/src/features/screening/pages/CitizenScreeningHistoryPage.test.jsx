import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import CitizenScreeningHistoryPage from './CitizenScreeningHistoryPage';

const mocks = vi.hoisted(() => ({ listCitizenScreeningSubmissions: vi.fn(), listCitizenEligibilityAssessments: vi.fn(), listCampaigns: vi.fn() }));
vi.mock('../../../auth/context/AuthContext', () => ({ useAuth: () => ({ user: { id: 71 } }) }));
vi.mock('../api/screeningApi', () => mocks);
vi.mock('../../campaign-management/api/campaignApi', () => mocks);
const show = () => render(<MemoryRouter><CitizenScreeningHistoryPage /></MemoryRouter>);
beforeEach(() => {
  vi.resetAllMocks();
  mocks.listCitizenScreeningSubmissions.mockResolvedValue([
    { id: 43, campaignId: 22, status: 'PENDING_REVIEW', preliminaryResult: 'PRELIMINARY_PASS', submittedAt: '2026-08-30' },
    { id: 42, campaignId: 22, status: 'REVIEWED', preliminaryResult: 'PRELIMINARY_REVIEW_REQUIRED', submittedAt: '2026-08-29' },
    { id: 41, campaignId: 23, status: 'CORRECTIONS_REQUIRED', preliminaryResult: null, submittedAt: null },
  ]);
  mocks.listCitizenEligibilityAssessments.mockResolvedValue([{ submissionId: 42, campaignId: 22, status: 'INELIGIBLE', clinicalNotes: 'SECRET' }]);
  mocks.listCampaigns.mockResolvedValue([{ id: 22, name: 'Historical campaign' }]);
});
afterEach(cleanup);
describe('citizen screening history', () => {
  it('shows loading', () => {
    mocks.listCitizenScreeningSubmissions.mockReturnValue(new Promise(() => {}));
    show();
    expect(screen.getByRole('status').textContent).toBe('Loading your screening history...');
  });
  it('shows the empty state and start-screening link', async () => {
    mocks.listCitizenScreeningSubmissions.mockResolvedValue([]);
    mocks.listCitizenEligibilityAssessments.mockResolvedValue([]);
    show();
    expect(await screen.findByText('No screening history is available yet.')).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Start Screening' }).getAttribute('href')).toBe('/app/screening');
  });
  it('reports submission failure without presenting empty success', async () => {
    mocks.listCitizenScreeningSubmissions.mockRejectedValue(new Error('History unavailable'));
    show();
    expect(await screen.findByText('History unavailable')).toBeTruthy();
    expect(screen.queryByRole('table')).toBeNull();
    expect(screen.queryByText('No screening history is available yet.')).toBeNull();
  });
  it('preserves backend order and joins by submission ID, never merely campaign ID', async () => {
    show();
    const table = await screen.findByRole('table');
    const rows = within(table).getAllByRole('row').slice(1);
    expect(rows).toHaveLength(3);
    expect(rows.map((row) => within(row).getByRole('link', { name: 'View Details' }).getAttribute('href'))).toEqual([
      '/app/screening/history/43', '/app/screening/history/42', '/app/screening/history/41',
    ]);
    expect(within(rows[0]).getByText('2026-08-30')).toBeTruthy();
    expect(within(rows[0]).getByText('Historical campaign')).toBeTruthy();
    expect(within(rows[0]).getByText('Not yet recorded')).toBeTruthy();
    expect(within(rows[0]).queryByText('Ineligible')).toBeNull();
    expect(within(rows[1]).getByText('Ineligible')).toBeTruthy();
    expect(within(rows[2]).getByText('Campaign #23')).toBeTruthy();
    expect(within(rows[2]).getByText('Corrections required')).toBeTruthy();
    expect(screen.getByRole('columnheader', { name: 'Preliminary result' })).toBeTruthy();
    expect(screen.getByRole('columnheader', { name: 'Final recorded assessment' })).toBeTruthy();
    expect(within(rows[0]).getByText('Preliminary screening passed')).toBeTruthy();
    expect(screen.getByRole('region', { name: 'Screening history table' }).getAttribute('tabindex')).toBe('0');
    expect(screen.queryByText('SECRET')).toBeNull();
    expect(mocks.listCitizenScreeningSubmissions).toHaveBeenCalledExactlyOnceWith(71);
    expect(mocks.listCitizenEligibilityAssessments).toHaveBeenCalledExactlyOnceWith(71);
  });
  it('retains all submission rows when assessments fail', async () => {
    mocks.listCitizenEligibilityAssessments.mockRejectedValue(new Error('Assessment unavailable'));
    show();
    expect(await screen.findByText(/Final assessment information is unavailable/)).toBeTruthy();
    expect(screen.getAllByRole('link', { name: 'View Details' })).toHaveLength(3);
    expect(screen.getAllByText('Unavailable')).toHaveLength(3);
  });
  it('retains rows using campaign references if metadata fails', async () => {
    mocks.listCampaigns.mockRejectedValue(new Error('Campaign unavailable'));
    show();
    expect(await screen.findByRole('table')).toBeTruthy();
    expect(screen.getAllByText('Campaign #22')).toHaveLength(2);
    expect(screen.getByText('Campaign #23')).toBeTruthy();
  });
});
