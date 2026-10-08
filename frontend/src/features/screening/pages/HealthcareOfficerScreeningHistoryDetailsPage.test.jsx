import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

const mocks = vi.hoisted(() => ({
  getScreeningHistoryDetails: vi.fn(),
  getEligibilityAssessmentBySubmission: vi.fn(),
  getCampaignById: vi.fn(),
}));

vi.mock('../api/screeningApi', () => ({
  getScreeningHistoryDetails: mocks.getScreeningHistoryDetails,
  getEligibilityAssessmentBySubmission: mocks.getEligibilityAssessmentBySubmission,
}));
vi.mock('../../campaign-management/api/campaignApi', () => ({
  getCampaignById: mocks.getCampaignById,
}));

import HealthcareOfficerScreeningHistoryDetailsPage from './HealthcareOfficerScreeningHistoryDetailsPage';

const baseHistoryDetails = {
  id: 15,
  citizenId: 71,
  campaignId: 22,
  questionnaireVersionId: 90,
  questionnaireVersionNumber: 1,
  status: 'REVIEWED',
  preliminaryResult: 'PRELIMINARY_PASS',
  submittedAt: '2026-08-15T09:30:00Z',
  createdAt: '2026-08-15T09:30:00Z',
  correctionInstructions: null,
  correctionRequestedAt: null,
  answers: [
    { id: 1, questionId: 101, questionCode: 'CONSENT', questionText: 'Consent to vaccination', questionType: 'BOOLEAN', displayOrder: 1, typedAnswer: 'true', displayAnswer: 'Yes', selectedOptions: [] },
    { id: 2, questionId: 102, questionCode: 'FEVER', questionText: 'Current fever', questionType: 'SINGLE_CHOICE', displayOrder: 2, typedAnswer: 'NO_FEVER', displayAnswer: 'No fever', selectedOptions: [{ code: 'NO_FEVER', label: 'No fever' }] },
  ],
};

const baseAssessment = {
  id: 40,
  submissionId: 15,
  citizenId: 71,
  campaignId: 22,
  reviewerId: 88,
  status: 'ELIGIBLE',
  reason: 'Approved based on clear clinical screening.',
  reviewedAt: '2026-08-15T10:00:00Z',
  validUntil: '2026-09-15T10:00:00Z',
  revokedAt: null,
  revocationReason: null,
};

const campaign = { id: 22, name: 'COVID-19 Booster Campaign', code: 'COV-BOOST' };

function renderPage(submissionId = 15) {
  return render(
    <MemoryRouter initialEntries={[`/app/staff/screening/history/${submissionId}`]}>
      <Routes>
        <Route
          path="/app/staff/screening/history/:submissionId"
          element={<HealthcareOfficerScreeningHistoryDetailsPage />}
        />
      </Routes>
    </MemoryRouter>,
  );
}

describe('HealthcareOfficerScreeningHistoryDetailsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.getScreeningHistoryDetails.mockResolvedValue(baseHistoryDetails);
    mocks.getEligibilityAssessmentBySubmission.mockResolvedValue(baseAssessment);
    mocks.getCampaignById.mockResolvedValue(campaign);
  });

  afterEach(cleanup);

  it('shows loading state while retrieving history details', async () => {
    let resolveHistory;
    mocks.getScreeningHistoryDetails.mockReturnValue(new Promise((resolve) => { resolveHistory = resolve; }));
    renderPage();

    expect(screen.getByRole('status').textContent).toContain('Loading screening history details...');
    resolveHistory(baseHistoryDetails);
    expect(await screen.findByRole('heading', { name: 'Previous Screening Details' })).toBeTruthy();
  });

  it('renders load error when historical details API fails', async () => {
    mocks.getScreeningHistoryDetails.mockRejectedValue(new Error('Screening details not found'));
    renderPage();

    expect(await screen.findByText('Screening details not found')).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Back to Review Queue' }).getAttribute('href'))
      .toBe('/app/staff/screening/reviews');
  });

  it('renders metadata, campaign, answers, and ELIGIBLE assessment cleanly in read-only mode', async () => {
    renderPage();

    expect(await screen.findByRole('heading', { name: 'Previous Screening Details' })).toBeTruthy();
    expect(screen.getByText(/Citizen ID: #71/)).toBeTruthy();
    expect(screen.getByText(/Submission ID: #15/)).toBeTruthy();
    expect(screen.getByText('COVID-19 Booster Campaign')).toBeTruthy();
    expect(screen.getByText('COV-BOOST')).toBeTruthy();
    expect(screen.getAllByText('Reviewed').length).toBeGreaterThan(0);
    expect(screen.getByText('Preliminary pass — final healthcare review pending')).toBeTruthy();
    expect(screen.getByText('This preliminary result is not a final eligibility decision.')).toBeTruthy();

    // Answers
    expect(screen.getByText('Consent to vaccination')).toBeTruthy();
    expect(screen.getByText('Yes')).toBeTruthy();
    expect(screen.getByText('Current fever')).toBeTruthy();
    expect(screen.getByText('No fever')).toBeTruthy();

    // Assessment
    expect(screen.getByText('Eligible')).toBeTruthy();
    expect(screen.getByText('Approved based on clear clinical screening.')).toBeTruthy();
    expect(screen.getByText('Reviewer #88')).toBeTruthy();

    // Verify STRICT READ-ONLY: No decision or mutation actions
    expect(screen.queryByRole('button', { name: 'Mark Eligible' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Mark Ineligible' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Request Corrections' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Revoke Eligibility' })).toBeNull();
    expect(screen.queryByRole('textbox')).toBeNull();
  });

  it('renders INELIGIBLE assessment with reason', async () => {
    mocks.getEligibilityAssessmentBySubmission.mockResolvedValue({
      ...baseAssessment,
      status: 'INELIGIBLE',
      reason: 'Citizen reported contraindication.',
      validUntil: null,
    });
    renderPage();

    expect(await screen.findByText('Ineligible')).toBeTruthy();
    expect(screen.getByText('Citizen reported contraindication.')).toBeTruthy();
  });

  it('renders REVOKED assessment with revocation timestamp and reason', async () => {
    mocks.getEligibilityAssessmentBySubmission.mockResolvedValue({
      ...baseAssessment,
      status: 'REVOKED',
      revokedAt: '2026-08-20T14:15:00Z',
      revocationReason: 'Medical condition changed after screening.',
    });
    renderPage();

    expect(await screen.findByText('Revoked')).toBeTruthy();
    expect(screen.getByText('Revocation reason')).toBeTruthy();
    expect(screen.getByText('Medical condition changed after screening.')).toBeTruthy();
    expect(screen.getByText('Revoked on')).toBeTruthy();
  });

  it('renders EXPIRED assessment status', async () => {
    mocks.getEligibilityAssessmentBySubmission.mockResolvedValue({
      ...baseAssessment,
      status: 'EXPIRED',
      validUntil: '2026-08-01T00:00:00Z',
    });
    renderPage();

    expect(await screen.findByText('Expired')).toBeTruthy();
  });

  it('handles submission without recorded assessment gracefully', async () => {
    mocks.getEligibilityAssessmentBySubmission.mockResolvedValue(null);
    renderPage();

    expect(await screen.findByText('No final eligibility assessment has been recorded for this screening.')).toBeTruthy();
  });

  it('renders campaign warning without breaking history if campaign fetch fails', async () => {
    mocks.getCampaignById.mockRejectedValue(new Error('Campaign service timeout'));
    renderPage();

    expect(await screen.findByText(/Campaign details could not be loaded/)).toBeTruthy();
    expect(screen.getByText('Campaign #22')).toBeTruthy();
    expect(screen.getByText('Consent to vaccination')).toBeTruthy();
  });

  it('renders previous correction request if present on the submission', async () => {
    mocks.getScreeningHistoryDetails.mockResolvedValue({
      ...baseHistoryDetails,
      correctionInstructions: 'Please specify the exact date of your previous vaccination.',
      correctionRequestedAt: '2026-08-16T11:00:00Z',
    });
    renderPage();

    expect(await screen.findByText('Previous correction request')).toBeTruthy();
    expect(screen.getByText('Please specify the exact date of your previous vaccination.')).toBeTruthy();
  });
});
