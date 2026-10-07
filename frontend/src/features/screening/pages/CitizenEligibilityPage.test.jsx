import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import CitizenEligibilityPage from './CitizenEligibilityPage';

const mocks = vi.hoisted(() => {
  const listCitizenCampaigns = vi.fn();
  return {
    user: { id: 71 },
    listCitizenScreeningSubmissions: vi.fn(),
    listCitizenEligibilityAssessments: vi.fn(),
    listCampaigns: listCitizenCampaigns,
    listCitizenCampaigns,
    getCitizenCampaignEligibility: vi.fn(),
  };
});
vi.mock('../../../auth/context/AuthContext', () => ({ useAuth: () => ({ user: mocks.user }) }));
vi.mock('../api/screeningApi', () => mocks);
vi.mock('../../campaign-management/api/campaignApi', () => mocks);
const submission = (overrides = {}) => ({ id: 42, citizenId: 71, campaignId: 22, status: 'PENDING_REVIEW', preliminaryResult: 'PRELIMINARY_PASS', createdAt: '2026-08-29T12:00:00', ...overrides });
const assessment = (overrides = {}) => ({ id: 5, submissionId: 42, campaignId: 22, status: 'ELIGIBLE', reviewedAt: '2026-08-30', validUntil: '2026-09-30', clinicalNotes: 'PRIVATE CLINICAL NOTES', ...overrides });
const page = () => <MemoryRouter><CitizenEligibilityPage /></MemoryRouter>;

beforeEach(() => {
  vi.resetAllMocks();
  mocks.user = { id: 71 };
  mocks.listCitizenScreeningSubmissions.mockResolvedValue([submission()]);
  mocks.listCitizenEligibilityAssessments.mockResolvedValue([]);
  mocks.listCitizenCampaigns.mockResolvedValue([{ id: 22, name: 'Archived campaign', code: 'OLD', status: 'COMPLETED' }, { id: 99, name: 'Unrelated campaign' }]);
  mocks.getCitizenCampaignEligibility.mockResolvedValue({ isEligible: false });
});
afterEach(cleanup);

describe('citizen eligibility', () => {
  it('renders the page heading and citizen navigation while records load', () => {
    mocks.listCitizenScreeningSubmissions.mockReturnValue(new Promise(() => {}));
    render(page());
    expect(screen.getByRole('heading', { level: 1, name: 'My Eligibility' })).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Back to Dashboard' }).getAttribute('href')).toBe('/app/citizen');
    expect(screen.getByRole('link', { name: 'View Screening History' }).getAttribute('href')).toBe('/app/screening/history');
  });
  it.each([
    ['PENDING_REVIEW', 'PRELIMINARY_PASS'],
    ['PENDING_REVIEW', 'PRELIMINARY_REVIEW_REQUIRED'],
    ['CORRECTIONS_REQUIRED', 'PRELIMINARY_PASS'],
  ])('keeps a false current check neutral for %s / %s without a linked assessment', async (status, preliminaryResult) => {
    mocks.listCitizenScreeningSubmissions.mockResolvedValue([submission({ status, preliminaryResult })]);
    render(page());
    expect(await screen.findByText('Awaiting final review')).toBeTruthy();
    expect(screen.queryByText('Not currently eligible')).toBeNull();
    expect(screen.queryByText('Ineligible', { exact: true })).toBeNull();
    expect(screen.queryByText('Eligible', { exact: true })).toBeNull();
  });
  it('shows loading until the shared base load completes', () => {
    mocks.listCitizenScreeningSubmissions.mockReturnValue(new Promise(() => {}));
    render(page());
    expect(screen.getByRole('status').textContent).toContain('Loading your eligibility records...');
  });
  it.each(['listCitizenScreeningSubmissions', 'listCitizenEligibilityAssessments'])('reports a base %s error without inventing an empty record set', async (method) => {
    mocks[method].mockRejectedValue(new Error('Records unavailable'));
    render(page());
    expect(await screen.findByText('Records unavailable')).toBeTruthy();
    expect(screen.queryByText('No screening or eligibility records are available yet.')).toBeNull();
    expect(mocks.getCitizenCampaignEligibility).not.toHaveBeenCalled();
  });
  it('shows a neutral empty state and no unrelated campaigns or eligibility calls', async () => {
    mocks.listCitizenScreeningSubmissions.mockResolvedValue([]);
    render(page());
    expect(await screen.findByText('No screening or eligibility records are available yet.')).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Start Pre-Vaccination Screening' }).getAttribute('href')).toBe('/app/screening');
    expect(screen.queryByText(/ineligible/i)).toBeNull();
    expect(screen.queryByText('Unrelated campaign')).toBeNull();
    expect(mocks.getCitizenCampaignEligibility).not.toHaveBeenCalled();
  });
  it('uses the authenticated citizen ID and treats preliminary pass as awaiting review, not eligible', async () => {
    render(page());
    expect(await screen.findByText('Awaiting review')).toBeTruthy();
    expect(screen.getByText(/Preliminary screening passed/)).toBeTruthy();
    expect(screen.getByText('This preliminary result is not a final eligibility decision.')).toBeTruthy();
    expect(screen.queryByText('Eligible', { exact: true })).toBeNull();
    expect(screen.queryByText('Currently eligible', { exact: true })).toBeNull();
    expect(mocks.listCitizenScreeningSubmissions).toHaveBeenCalledExactlyOnceWith(71);
    expect(mocks.listCitizenEligibilityAssessments).toHaveBeenCalledExactlyOnceWith(71);
    expect(mocks.listCampaigns).toHaveBeenCalled();
    await waitFor(() => expect(mocks.getCitizenCampaignEligibility).toHaveBeenCalledExactlyOnceWith(71, 22));
    expect(screen.getByRole('heading', { name: 'Archived campaign' })).toBeTruthy();
    expect(screen.getAllByRole('link', { name: 'View Screening' })[0].getAttribute('href')).toBe('/app/screening/history/42');
    expect(screen.getAllByRole('link', { name: 'View Screening History' })[0].getAttribute('href')).toBe('/app/screening/history');
  });
  it('shows healthcare review required without claiming final ineligibility', async () => {
    mocks.listCitizenScreeningSubmissions.mockResolvedValue([submission({ preliminaryResult: 'PRELIMINARY_REVIEW_REQUIRED' })]);
    render(page());
    expect(await screen.findByText('Healthcare review required')).toBeTruthy();
    expect(screen.queryByText('Ineligible', { exact: true })).toBeNull();
  });
  it('shows correction instructions and date without edit or resubmit actions', async () => {
    mocks.listCitizenScreeningSubmissions.mockResolvedValue([submission({ status: 'CORRECTIONS_REQUIRED', correctionInstructions: 'Clarify your answer.', correctionRequestedAt: '2026-08-30' })]);
    render(page());
    expect((await screen.findAllByText('Corrections required'))[0]).toBeTruthy();
    expect(screen.getAllByText('Clarify your answer.')[0]).toBeTruthy();
    expect(screen.getAllByText(/2026-08-30/)[0]).toBeTruthy();
    expect(screen.queryByRole('button')).toBeNull();
    expect(screen.queryByRole('link', { name: /edit|resubmit|rescreen|book appointment/i })).toBeNull();
  });
  it('shows currently eligible with recorded review/validity dates and no clinical notes', async () => {
    mocks.listCitizenScreeningSubmissions.mockResolvedValue([submission({ status: 'REVIEWED' })]);
    mocks.listCitizenEligibilityAssessments.mockResolvedValue([assessment()]);
    mocks.getCitizenCampaignEligibility.mockResolvedValue({ isEligible: true });
    render(page());
    expect(await screen.findByText('Currently eligible')).toBeTruthy();
    expect(screen.getAllByText('Eligible', { exact: true })[0]).toBeTruthy();
    expect(screen.getAllByText('2026-08-30')[0]).toBeTruthy();
    expect(screen.getAllByText('2026-09-30')[0]).toBeTruthy();
    expect(screen.queryByText('PRIVATE CLINICAL NOTES')).toBeNull();
  });
  it('keeps recorded Eligible separate from a false current check', async () => {
    mocks.listCitizenScreeningSubmissions.mockResolvedValue([submission({ status: 'REVIEWED' })]);
    mocks.listCitizenEligibilityAssessments.mockResolvedValue([assessment()]);
    render(page());
    expect(await screen.findByText('Not currently eligible')).toBeTruthy();
    expect(screen.getAllByText('Eligible', { exact: true })[0]).toBeTruthy();
    expect(screen.getByText(/validity may have elapsed or the campaign service period may have ended/)).toBeTruthy();
    expect(screen.queryByText('Ineligible', { exact: true })).toBeNull();
  });
  it.each([['INELIGIBLE', 'Ineligible'], ['EXPIRED', 'Expired'], ['REVOKED', 'Revoked']])('renders the recorded %s state distinctly', async (status, label) => {
    mocks.listCitizenScreeningSubmissions.mockResolvedValue([submission({ status: 'REVIEWED' })]);
    mocks.listCitizenEligibilityAssessments.mockResolvedValue([assessment({ status, reason: 'Recorded reason', ...(status === 'REVOKED' ? { revokedAt: '2026-09-02', revocationReason: 'Recorded revocation reason' } : {}) })]);
    render(page());
    expect((await screen.findAllByText(label, { exact: true }))[0]).toBeTruthy();
    expect(screen.getAllByText('Recorded reason')[0]).toBeTruthy();
    if (status === 'REVOKED') {
      expect(screen.getAllByText('2026-09-02')[0]).toBeTruthy();
      expect(screen.getAllByText('Recorded revocation reason')[0]).toBeTruthy();
    }
    if (status === 'EXPIRED') expect(screen.queryByText('Ineligible', { exact: true })).toBeNull();
  });
  it('isolates current-check failures per campaign without converting failure to false', async () => {
    mocks.listCitizenScreeningSubmissions.mockResolvedValue([submission(), submission({ id: 43, campaignId: 23 })]);
    mocks.getCitizenCampaignEligibility.mockImplementation((citizenId, campaignId) => campaignId === 22 ? Promise.reject(new Error('Unavailable')) : Promise.resolve({ citizenId, campaignId, isEligible: true }));
    render(page());
    expect(await screen.findByText('Unable to confirm current eligibility')).toBeTruthy();
    expect(await screen.findByText('Currently eligible')).toBeTruthy();
    expect(screen.queryByText('Not currently eligible')).toBeNull();
  });
  it('shows a newer pending screening separately from its older assessment', async () => {
    mocks.listCitizenScreeningSubmissions.mockResolvedValue([submission({ id: 43, createdAt: '2026-09-02' }), submission({ status: 'REVIEWED' })]);
    mocks.listCitizenEligibilityAssessments.mockResolvedValue([assessment()]);
    render(page());
    expect(await screen.findByText('Awaiting review')).toBeTruthy();
    expect(await screen.findByText('Awaiting final review')).toBeTruthy();
    expect(screen.queryByText('Not currently eligible')).toBeNull();
    expect(screen.getByRole('heading', { name: 'Previous recorded assessment' })).toBeTruthy();
    expect(screen.getByText(/different screening, not the current screening/)).toBeTruthy();
    expect(screen.getAllByRole('link', { name: 'View Screening' })[0].getAttribute('href')).toBe('/app/screening/history/43');
    expect(screen.getByRole('link', { name: 'View assessed screening' }).getAttribute('href')).toBe('/app/screening/history/42');
    expect(screen.getAllByRole('article')).toHaveLength(1);
  });
  it.each([[], null])('retains campaign-reference fallback with missing metadata %s', async (campaigns) => {
    if (campaigns === null) mocks.listCitizenCampaigns.mockRejectedValue(new Error('Campaign lookup failed'));
    else mocks.listCitizenCampaigns.mockResolvedValue(campaigns);
    render(page());
    expect(await screen.findByRole('heading', { name: 'Campaign #22' })).toBeTruthy();
    expect(screen.getByText('Awaiting review')).toBeTruthy();
  });
  it.each([['DRAFT', 'Screening not yet submitted'], ['REVIEWED', 'Review completed']])('handles %s without inventing a decision', async (status, text) => {
    mocks.listCitizenScreeningSubmissions.mockResolvedValue([submission({ status, preliminaryResult: null })]);
    render(page());
    expect(await screen.findByText(text)).toBeTruthy();
    if (status === 'REVIEWED') expect(screen.getByText('Eligibility decision unavailable')).toBeTruthy();
    expect(screen.queryByText('Eligible', { exact: true })).toBeNull();
  });
  it('includes assessment-only campaigns without fabricating a submission', async () => {
    mocks.listCitizenScreeningSubmissions.mockResolvedValue([]);
    mocks.listCitizenEligibilityAssessments.mockResolvedValue([assessment()]);
    render(page());
    expect(await screen.findByText('No screening submitted')).toBeTruthy();
    expect(screen.queryByRole('link', { name: 'View Screening', exact: true })).toBeNull();
  });
  it('ignores a previous citizen’s late requests after account change', async () => {
    let resolveOld;
    mocks.listCitizenScreeningSubmissions.mockReturnValueOnce(new Promise((resolve) => { resolveOld = resolve; }));
    const view = render(page());
    mocks.user = { id: 72 };
    mocks.listCitizenScreeningSubmissions.mockResolvedValue([]);
    view.rerender(page());
    expect(await screen.findByText('No screening or eligibility records are available yet.')).toBeTruthy();
    await act(async () => { resolveOld([submission()]); });
    expect(screen.queryByRole('article')).toBeNull();
    expect(mocks.listCitizenScreeningSubmissions).toHaveBeenLastCalledWith(72);
  });
  it('keeps missing current-check payload unknown', async () => {
    mocks.getCitizenCampaignEligibility.mockResolvedValue(null);
    render(page());
    expect(await screen.findByText('Unable to confirm current eligibility')).toBeTruthy();
    expect(within(screen.getByRole('article')).queryByText('Not currently eligible')).toBeNull();
  });

  // Focused tests for Requirement 14
  it('displays real campaign name and removes technical campaign warning when lookup succeeds', async () => {
    mocks.listCitizenCampaigns.mockResolvedValue([{ id: 22, name: 'Seasonal Influenza Vaccination 2026', code: 'FLU-2026' }]);
    render(page());
    expect(await screen.findByRole('heading', { name: 'Seasonal Influenza Vaccination 2026' })).toBeTruthy();
    expect(screen.queryByText('Campaign information unavailable')).toBeNull();
    expect(screen.queryByText(/record references remain visible/i)).toBeNull();
  });

  it('shows citizen-friendly fallback when campaign lookup fails without technical wording', async () => {
    mocks.listCitizenCampaigns.mockRejectedValue(new Error('Service unavailable'));
    render(page());
    expect(await screen.findByText('Campaign details are temporarily unavailable.')).toBeTruthy();
    expect(screen.queryByText('Campaign information unavailable')).toBeNull();
    expect(screen.queryByText(/record references remain visible/i)).toBeNull();
  });

  it('renders historical corrections as resolved without action button', async () => {
    mocks.listCitizenScreeningSubmissions.mockResolvedValue([submission({
      status: 'REVIEWED',
      correctionInstructions: 'Verify vaccination date.',
      correctionRequestedAt: '2026-08-25T14:00:00',
    })]);
    mocks.listCitizenEligibilityAssessments.mockResolvedValue([assessment()]);
    mocks.getCitizenCampaignEligibility.mockResolvedValue({ isEligible: true });
    render(page());
    expect(await screen.findByText('Corrections requested (resolved)')).toBeTruthy();
    expect(screen.getAllByText('Resolved')[0]).toBeTruthy();
    expect(screen.queryByRole('link', { name: 'Correct Screening' })).toBeNull();
  });

  it('renders prominent Action Required card and Correct Screening link for active corrections', async () => {
    mocks.listCitizenScreeningSubmissions.mockResolvedValue([submission({
      status: 'CORRECTIONS_REQUIRED',
      correctionInstructions: 'Please upload a clearer image of your certificate.',
      correctionRequestedAt: '2026-08-30T10:00:00',
    })]);
    render(page());
    expect(await screen.findByRole('alert')).toBeTruthy();
    expect(screen.getAllByText('Please upload a clearer image of your certificate.')[0]).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Correct Screening' }).getAttribute('href')).toBe('/app/screening/history/42');
  });

  it('renders recent activity with real chronological events and link to history', async () => {
    mocks.listCitizenScreeningSubmissions.mockResolvedValue([submission({
      status: 'REVIEWED',
      createdAt: '2026-08-20T10:00:00',
      correctionInstructions: 'Verify contact details',
      correctionRequestedAt: '2026-08-22T12:00:00',
    })]);
    mocks.listCitizenEligibilityAssessments.mockResolvedValue([assessment({
      reviewedAt: '2026-08-25T15:00:00',
    })]);
    render(page());
    expect(await screen.findByText('Recent Activity')).toBeTruthy();
    expect(screen.getAllByText('Screening submitted')[0]).toBeTruthy();
    expect(screen.getByText('Eligibility recorded')).toBeTruthy();
    expect(screen.getByText('Completed')).toBeTruthy();
    expect(screen.getByRole('link', { name: /View Full History/i }).getAttribute('href')).toBe('/app/screening/history');
  });
});
