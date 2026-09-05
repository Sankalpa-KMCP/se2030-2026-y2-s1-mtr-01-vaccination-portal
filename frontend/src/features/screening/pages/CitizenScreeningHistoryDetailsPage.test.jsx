import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import CitizenScreeningHistoryDetailsPage from './CitizenScreeningHistoryDetailsPage';

const mocks = vi.hoisted(() => ({ getScreeningHistoryDetails: vi.fn(), getEligibilityAssessmentBySubmission: vi.fn(), getCampaignById: vi.fn() }));
vi.mock('../../../auth/context/AuthContext', () => ({ useAuth: () => ({ user: { id: 71 } }) }));
vi.mock('../api/screeningApi', () => mocks);
vi.mock('../../campaign-management/api/campaignApi', () => mocks);
const answers = [
  ['BOOLEAN', 'Boolean question?', 'Yes'], ['SINGLE_CHOICE', 'Single option question?', 'Option label'],
  ['MULTI_CHOICE', 'Multiple options question?', 'Asthma, Diabetes'], ['NUMBER', 'Number question?', '37.5'],
  ['DATE', 'Date question?', '2026-08-29'], ['TEXT', 'Text question?', 'Citizen supplied text'],
  ['SINGLE_CHOICE', 'Legacy option question?', 'UNKNOWN_LEGACY_CODE'],
].map(([questionType, questionText, displayAnswer], index) => ({ id: index + 1, questionId: index + 100, questionType, questionText, displayAnswer, typedAnswer: 'RAW MUST NOT APPEAR', displayOrder: 100 - index }));
const details = (overrides = {}) => ({ id: 42, campaignId: 22, citizenId: 71, questionnaireVersionId: 90, questionnaireVersionNumber: 3, status: 'PENDING_REVIEW', preliminaryResult: 'PRELIMINARY_PASS', submittedAt: '2026-08-30', answers, ...overrides });
const page = (id = 42) => <MemoryRouter key={id} initialEntries={[`/app/screening/history/${id}`]}><Routes><Route path="/app/screening/history/:submissionId" element={<CitizenScreeningHistoryDetailsPage />} /></Routes></MemoryRouter>;
beforeEach(() => {
  vi.resetAllMocks();
  mocks.getScreeningHistoryDetails.mockResolvedValue(details());
  mocks.getEligibilityAssessmentBySubmission.mockResolvedValue(null);
  mocks.getCampaignById.mockResolvedValue({ id: 22, name: 'Historic campaign', code: 'HISTORIC' });
});
afterEach(cleanup);
describe('citizen historical screening details', () => {
  it('shows primary loading', () => {
    mocks.getScreeningHistoryDetails.mockReturnValue(new Promise(() => {}));
    render(page());
    expect(screen.getByRole('heading', { level: 1, name: 'Screening Details' })).toBeTruthy();
    expect(screen.getByRole('status').textContent).toContain('Loading screening details...');
  });
  it('uses only history-details and citizen-safe supplementary requests with correct IDs', async () => {
    render(page());
    expect(await screen.findByRole('heading', { name: 'Historic campaign' })).toBeTruthy();
    expect(mocks.getScreeningHistoryDetails).toHaveBeenCalledExactlyOnceWith('42');
    expect(mocks.getCampaignById).toHaveBeenCalledExactlyOnceWith(22);
    expect(mocks.getEligibilityAssessmentBySubmission).toHaveBeenCalledExactlyOnceWith('42');
    expect(screen.getByText('Campaign code: HISTORIC')).toBeTruthy();
    expect(screen.getByText('2026-08-30')).toBeTruthy();
    expect(screen.getByText('Questionnaire version').nextElementSibling.textContent).toBe('3');
    expect(screen.getByRole('link', { name: /Back to Screening History/ }).getAttribute('href')).toBe('/app/screening/history');
    expect(screen.queryByRole('button', { name: /edit|resubmit/i })).toBeNull();
  });
  it('renders friendly backend display answers for every type and preserves backend order', async () => {
    render(page());
    await screen.findByRole('heading', { name: 'Historic campaign' });
    expect(screen.getAllByRole('heading', { level: 3 }).map((heading) => heading.textContent)).toEqual(answers.map((answer) => answer.questionText));
    for (const answer of answers) expect(screen.getByText(answer.displayAnswer)).toBeTruthy();
    expect(screen.queryByText('RAW MUST NOT APPEAR')).toBeNull();
  });
  it.each(['CORRECTIONS_REQUIRED', 'PENDING_REVIEW'])('shows correction instructions/date for %s without correction controls', async (status) => {
    mocks.getScreeningHistoryDetails.mockResolvedValue(details({ status, correctionInstructions: 'Please clarify the date.', correctionRequestedAt: '2026-08-28' }));
    render(page());
    expect(await screen.findByText('Please clarify the date.')).toBeTruthy();
    expect(screen.getByText(/2026-08-28/)).toBeTruthy();
    expect(screen.getByRole('heading', { name: status === 'PENDING_REVIEW' ? 'Corrections requested — historical information' : 'Corrections requested' })).toBeTruthy();
    expect(screen.queryByRole('link', { name: /edit|resubmit/i })).toBeNull();
    expect(screen.queryByRole('button')).toBeNull();
  });
  it('treats a null assessment as not yet recorded rather than ineligible', async () => {
    render(page());
    expect(await screen.findByText('No final eligibility assessment has been recorded for this screening yet.')).toBeTruthy();
    expect(screen.queryByText('Ineligible', { exact: true })).toBeNull();
  });
  it.each([['ELIGIBLE', 'Eligible'], ['INELIGIBLE', 'Ineligible'], ['EXPIRED', 'Expired'], ['REVOKED', 'Revoked']])('renders safe recorded %s fields without clinical notes or staff internals', async (status, label) => {
    mocks.getEligibilityAssessmentBySubmission.mockResolvedValue({ submissionId: 42, status, reason: 'Citizen-facing reason', reviewedAt: '2026-08-31', validUntil: '2026-09-30', revokedAt: '2026-09-01', revocationReason: 'Recorded revocation', clinicalNotes: 'SECRET CLINICAL CONTENT', reviewedByName: 'PRIVATE REVIEWER', rescreeningReason: 'INTERNAL RESCREENING' });
    render(page());
    expect(await screen.findByText(label, { exact: true })).toBeTruthy();
    for (const text of ['Citizen-facing reason', '2026-08-31', '2026-09-30', '2026-09-01', 'Recorded revocation']) expect(screen.getByText(text)).toBeTruthy();
    for (const text of ['SECRET CLINICAL CONTENT', 'PRIVATE REVIEWER', 'INTERNAL RESCREENING']) expect(screen.queryByText(text)).toBeNull();
  });
  it('retains readable answers when campaign metadata fails', async () => {
    mocks.getCampaignById.mockRejectedValue(new Error('Metadata unavailable'));
    render(page());
    expect(await screen.findByText(/Your screening details remain available/)).toBeTruthy();
    expect(screen.getByRole('heading', { name: 'Campaign #22' })).toBeTruthy();
    expect(screen.getByText('Asthma, Diabetes')).toBeTruthy();
  });
  it('distinguishes assessment request failure from no recorded assessment', async () => {
    mocks.getEligibilityAssessmentBySubmission.mockRejectedValue(new Error('Assessment lookup failed'));
    render(page());
    expect(await screen.findByText('Final assessment information is unavailable')).toBeTruthy();
    expect(screen.queryByText('No final eligibility assessment has been recorded for this screening yet.')).toBeNull();
    expect(screen.getByText('Asthma, Diabetes')).toBeTruthy();
  });
  it.each([403, 404, 500])('handles primary HTTP %s failure without loading supplements or leaking details', async (status) => {
    mocks.getScreeningHistoryDetails.mockRejectedValue(Object.assign(new Error(`History details unavailable (${status})`), { status }));
    render(page());
    expect((await screen.findByRole('alert')).className).toContain('citizen-state--error');
    expect(screen.queryByText('Asthma, Diabetes')).toBeNull();
    expect(mocks.getCampaignById).not.toHaveBeenCalled();
    expect(mocks.getEligibilityAssessmentBySubmission).not.toHaveBeenCalled();
  });
  it('keeps primary answers visible while supplementary requests are pending', async () => {
    mocks.getCampaignById.mockReturnValue(new Promise(() => {}));
    render(page());
    expect(await screen.findByText('Asthma, Diabetes')).toBeTruthy();
    expect(screen.getByRole('status').textContent).toBe('Loading assessment information...');
  });
  it('does not let a late response from an old detail page overwrite the next page', async () => {
    let resolveOld;
    mocks.getScreeningHistoryDetails.mockReturnValueOnce(new Promise((resolve) => { resolveOld = resolve; }));
    const view = render(page());
    mocks.getScreeningHistoryDetails.mockResolvedValue(details({ id: 43, answers: [{ id: 99, questionText: 'New question?', displayAnswer: 'New answer' }] }));
    view.rerender(page(43));
    expect(await screen.findByText('New answer')).toBeTruthy();
    await act(async () => { resolveOld(details()); });
    expect(screen.queryByText('Asthma, Diabetes')).toBeNull();
    expect(mocks.getEligibilityAssessmentBySubmission).toHaveBeenCalledExactlyOnceWith('43');
  });
});
