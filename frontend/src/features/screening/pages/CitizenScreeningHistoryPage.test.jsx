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
  it('renders the page heading and citizen navigation while records load', () => {
    mocks.listCitizenScreeningSubmissions.mockReturnValue(new Promise(() => {}));
    show();
    expect(screen.getByRole('heading', { level: 1, name: 'Screening History' })).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Back to Dashboard' }).getAttribute('href')).toBe('/app/citizen');
    expect(screen.getByRole('link', { name: 'View My Eligibility' }).getAttribute('href')).toBe('/app/screening/eligibility');
  });
  it('shows loading', () => {
    mocks.listCitizenScreeningSubmissions.mockReturnValue(new Promise(() => {}));
    show();
    expect(screen.getByRole('status').textContent).toContain('Loading your screening history...');
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
    const records = within(await screen.findByRole('list', { name: 'Screening history records' })).getAllByRole('article');
    expect(records).toHaveLength(3);
    expect(records.map((record) => within(record).getByRole('link', { name: 'View Details' }).getAttribute('href'))).toEqual([
      '/app/screening/history/43', '/app/screening/history/42', '/app/screening/history/41',
    ]);
    expect(within(records[0]).getByText(/2026-08-30/)).toBeTruthy();
    expect(within(records[0]).getByRole('heading', { name: 'Historical campaign' })).toBeTruthy();
    expect(within(records[0]).getByText('Not yet recorded')).toBeTruthy();
    expect(within(records[0]).queryByText('Ineligible')).toBeNull();
    expect(within(records[1]).getByText('Ineligible')).toBeTruthy();
    expect(within(records[2]).getByRole('heading', { name: 'Campaign #23' })).toBeTruthy();
    expect(within(records[2]).getByText('Corrections required')).toBeTruthy();
    expect(within(records[0]).getByText('Preliminary screening passed')).toBeTruthy();
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
    expect(await screen.findByRole('list', { name: 'Screening history records' })).toBeTruthy();
    expect(screen.getAllByRole('heading', { name: 'Campaign #22' })).toHaveLength(2);
    expect(screen.getByRole('heading', { name: 'Campaign #23' })).toBeTruthy();
  });
});
