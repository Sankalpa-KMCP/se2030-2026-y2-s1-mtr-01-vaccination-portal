import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

const mocks = vi.hoisted(() => ({
  getScreeningReviewDetails: vi.fn(),
  recordEligibilityDecision: vi.fn(),
  requestScreeningCorrections: vi.fn(),
  revokeAssessment: vi.fn(),
  getCampaignById: vi.fn(),
}));

vi.mock('../api/screeningApi', () => ({
  getScreeningReviewDetails: mocks.getScreeningReviewDetails,
  recordEligibilityDecision: mocks.recordEligibilityDecision,
  requestScreeningCorrections: mocks.requestScreeningCorrections,
  revokeAssessment: mocks.revokeAssessment,
}));
vi.mock('../../campaign-management/api/campaignApi', () => ({
  getCampaignById: mocks.getCampaignById,
}));

import HealthcareOfficerScreeningReviewDetailsPage from './HealthcareOfficerScreeningReviewDetailsPage';

const pendingDetails = {
  submissionId: 42,
  citizenId: 71,
  citizenFullName: 'Kawya Arusha',
  campaignId: 22,
  questionnaireVersionId: 90,
  status: 'PENDING_REVIEW',
  preliminaryResult: 'PRELIMINARY_PASS',
  submittedAt: '2026-09-05T02:30:00Z',
  correctionInstructions: null,
  correctionRequestedAt: null,
  assessment: null,
  answers: [
    { id: 6, questionCode: 'NOTES', questionText: 'Text question', questionType: 'TEXT', displayOrder: 6, required: false, typedAnswer: 'No concerns', selectedOptions: [] },
    { id: 2, questionCode: 'VACCINE', questionText: 'Single choice question', questionType: 'SINGLE_CHOICE', displayOrder: 2, required: true, typedAnswer: 'A', selectedOptions: [{ code: 'A', label: 'Option Alpha', value: 'A' }] },
    { id: 1, questionCode: 'CONSENT', questionText: 'Boolean question', questionType: 'BOOLEAN', displayOrder: 1, required: true, typedAnswer: 'true', selectedOptions: [] },
    { id: 4, questionCode: 'TEMP', questionText: 'Numeric question', questionType: 'NUMERIC', displayOrder: 4, required: true, typedAnswer: '37.50', selectedOptions: [] },
    { id: 3, questionCode: 'CONDITIONS', questionText: 'Multi choice question', questionType: 'MULTI_CHOICE', displayOrder: 3, required: true, typedAnswer: 'ASTHMA,DIABETES', selectedOptions: [{ code: 'ASTHMA', label: 'Asthma' }, { code: 'DIABETES', label: 'Diabetes' }] },
    { id: 7, questionCode: 'RAW', questionText: 'Unresolved choice question', questionType: 'SINGLE_CHOICE', displayOrder: 7, required: false, typedAnswer: 'LEGACY_VALUE', selectedOptions: [] },
    { id: 5, questionCode: 'DATE', questionText: 'Date question', questionType: 'DATE', displayOrder: 5, required: true, typedAnswer: '2026-09-05', selectedOptions: [] },
  ],
  triggeredRuleEvidence: [
    { id: 2, ruleCode: 'SECOND', message: 'Second evidence', preliminaryResult: 'PRELIMINARY_PASS', priority: 2, evaluationOrder: 2 },
    { id: 1, ruleCode: 'FIRST', message: 'First evidence', preliminaryResult: 'PRELIMINARY_REVIEW_REQUIRED', priority: 1, evaluationOrder: 1 },
  ],
};

const campaign = { id: 22, name: 'Adult Immunisation', code: 'ADULT-26' };

function details(overrides = {}) {
  return { ...pendingDetails, ...overrides };
}

function reviewedDetails(status = 'ELIGIBLE') {
  return details({
    status: 'REVIEWED',
    assessment: {
      id: 80,
      reviewerId: 42,
      status,
      reason: status === 'ELIGIBLE' ? 'Eligible after review' : 'Temporarily ineligible',
      clinicalNotes: 'Private notes must not render',
      reviewedAt: '2026-09-07T02:00:00Z',
      validUntil: status === 'ELIGIBLE' ? '2027-01-01T00:00:00Z' : null,
      revokedAt: status === 'REVOKED' ? '2026-09-08T02:00:00Z' : null,
      revocationReason: status === 'REVOKED' ? 'Eligibility withdrawn' : null,
    },
  });
}

function renderPage(submissionId = 42) {
  return render(
    <MemoryRouter initialEntries={[`/app/staff/screening/reviews/${submissionId}`]}>
      <Routes>
        <Route
          path="/app/staff/screening/reviews/:submissionId"
          element={<HealthcareOfficerScreeningReviewDetailsPage />}
        />
      </Routes>
    </MemoryRouter>,
  );
}

async function waitForPage() {
  return screen.findByRole('heading', { name: 'Screening Review Details' });
}

describe('HealthcareOfficerScreeningReviewDetailsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.getScreeningReviewDetails.mockResolvedValue(details());
    mocks.getCampaignById.mockResolvedValue(campaign);
    mocks.requestScreeningCorrections.mockResolvedValue({ status: 'CORRECTIONS_REQUIRED' });
    mocks.recordEligibilityDecision.mockResolvedValue({ status: 'ELIGIBLE' });
    mocks.revokeAssessment.mockResolvedValue({ status: 'REVOKED' });
  });

  afterEach(cleanup);

  it('shows a loading state and requests the staff review contract', async () => {
    let resolveDetails;
    mocks.getScreeningReviewDetails.mockReturnValue(new Promise((resolve) => { resolveDetails = resolve; }));
    renderPage();

    expect(screen.getByRole('status').textContent).toContain('Loading screening review...');
    resolveDetails(details());
    await waitForPage();
    expect(mocks.getScreeningReviewDetails).toHaveBeenCalledExactlyOnceWith('42');
  });

  it.each([
    [403, 'Permission Denied'],
    [404, 'Submission unavailable'],
    [500, 'Review service unavailable'],
  ])('handles HTTP %s load failures with recovery navigation', async (status, message) => {
    mocks.getScreeningReviewDetails.mockRejectedValue(Object.assign(new Error(message), { status }));
    renderPage();

    expect(await screen.findByText(message)).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Reload' })).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Back to Review Queue' }).getAttribute('href'))
      .toBe('/app/staff/screening/reviews');
  });

  it('keeps the citizen ID visible when the citizen name is unavailable', async () => {
    mocks.getScreeningReviewDetails.mockResolvedValue(details({ citizenFullName: null }));
    renderPage();
    await waitForPage();
    expect(screen.getByText('Name unavailable')).toBeTruthy();
    expect(screen.getByText('Citizen ID: #71')).toBeTruthy();
  });

  it('renders metadata, ordered answers, option labels, friendly values, and rule evidence', async () => {
    renderPage();
    await waitForPage();

    expect(screen.getByText('Kawya Arusha')).toBeTruthy();
    expect(screen.getByText('Citizen ID: #71')).toBeTruthy();
    expect(screen.getAllByText('Adult Immunisation').length).toBeGreaterThan(0);
    expect(screen.getByText('ADULT-26')).toBeTruthy();
    expect(screen.getByText('Pending review')).toBeTruthy();
    expect(screen.getByText('#90')).toBeTruthy();
    expect(screen.getByText('Preliminary pass — final healthcare review pending')).toBeTruthy();
    expect(screen.queryByText('Eligible')).toBeNull();

    const questionHeadings = screen.getAllByRole('heading', { level: 3 }).map((heading) => heading.textContent);
    expect(questionHeadings).toEqual([
      'Boolean question',
      'Single choice question',
      'Multi choice question',
      'Numeric question',
      'Date question',
      'Text question',
      'Unresolved choice question',
    ]);
    for (const answer of ['Yes', 'Option Alpha', 'Asthma, Diabetes', '37.50', '2026-09-05', 'No concerns', 'LEGACY_VALUE']) {
      expect(screen.getByText(answer)).toBeTruthy();
    }
    expect(screen.getByText('First evidence')).toBeTruthy();
    expect(screen.getByText('Second evidence')).toBeTruthy();
  });

  it('uses healthcare-review wording for review-required results', async () => {
    mocks.getScreeningReviewDetails.mockResolvedValue(details({ preliminaryResult: 'PRELIMINARY_REVIEW_REQUIRED' }));
    renderPage();
    expect(await screen.findByText('Healthcare review required')).toBeTruthy();
  });

  it('falls back to the campaign ID without hiding screening data when enrichment fails', async () => {
    mocks.getCampaignById.mockRejectedValue(new Error('Campaign unavailable'));
    renderPage();

    await waitForPage();
    expect(screen.getAllByText('Campaign #22').length).toBeGreaterThan(0);
    expect(screen.getByText(/Campaign details could not be loaded/)).toBeTruthy();
    expect(screen.getByText('Boolean question')).toBeTruthy();
  });

  it('shows a neutral state when no eligibility rules were triggered', async () => {
    mocks.getScreeningReviewDetails.mockResolvedValue(details({ triggeredRuleEvidence: [] }));
    renderPage();
    expect(await screen.findByText('No eligibility rules were triggered by this screening.')).toBeTruthy();
  });

  it('displays historical correction information without using it as the action state', async () => {
    mocks.getScreeningReviewDetails.mockResolvedValue(details({
      correctionInstructions: 'Review the vaccination date.',
      correctionRequestedAt: '2026-09-06T02:00:00Z',
    }));
    renderPage();
    await waitForPage();

    expect(screen.getByText('Review the vaccination date.')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Request Corrections' })).toBeTruthy();
  });

  it('renders an existing eligible assessment with the focused revoke action', async () => {
    mocks.getScreeningReviewDetails.mockResolvedValue(reviewedDetails());
    renderPage();
    await waitForPage();

    expect(screen.getByRole('heading', { name: 'Recorded final decision' })).toBeTruthy();
    expect(screen.getByText('Eligible')).toBeTruthy();
    expect(screen.getByText('Eligible after review')).toBeTruthy();
    expect(screen.getByText('Reviewer #42')).toBeTruthy();
    expect(screen.queryByText('Private notes must not render')).toBeNull();
    expect(screen.queryByRole('button', { name: 'Request Corrections' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Mark Eligible' })).toBeNull();
    expect(screen.getByRole('button', { name: 'Revoke Eligibility' })).toBeTruthy();
  });

  it.each(['INELIGIBLE', 'REVOKED', 'EXPIRED'])(
    'does not offer revocation when the assessment is %s',
    async (status) => {
      mocks.getScreeningReviewDetails.mockResolvedValue(reviewedDetails(status));
      renderPage();
      await waitForPage();
      expect(screen.queryByRole('button', { name: 'Revoke Eligibility' })).toBeNull();
    },
  );

  it.each(['CORRECTIONS_REQUIRED', 'REVIEWED', 'DRAFT'])(
    'hides all mutation actions when status is %s',
    async (status) => {
      mocks.getScreeningReviewDetails.mockResolvedValue(details({
        status,
        correctionInstructions: 'Historical instructions',
        assessment: status === 'REVIEWED' ? reviewedDetails().assessment : null,
      }));
      renderPage();
      await waitForPage();
      expect(screen.queryByRole('button', { name: 'Request Corrections' })).toBeNull();
      expect(screen.queryByRole('button', { name: 'Mark Eligible' })).toBeNull();
      expect(screen.queryByRole('button', { name: 'Mark Ineligible' })).toBeNull();
    },
  );

  it.each(['', '   '])('blocks blank correction instructions: %j', async (value) => {
    renderPage();
    await waitForPage();
    fireEvent.click(screen.getByRole('button', { name: 'Request Corrections' }));
    fireEvent.change(screen.getByLabelText('Instructions'), { target: { value } });
    fireEvent.click(screen.getByRole('button', { name: 'Send Correction Request' }));

    expect(screen.getByText('Instructions are required.')).toBeTruthy();
    expect(mocks.requestScreeningCorrections).not.toHaveBeenCalled();
  });

  it('submits trimmed corrections, disables controls, refreshes, and hides actions after success', async () => {
    let resolveMutation;
    mocks.requestScreeningCorrections.mockReturnValue(new Promise((resolve) => { resolveMutation = resolve; }));
    mocks.getScreeningReviewDetails
      .mockResolvedValueOnce(details())
      .mockResolvedValueOnce(details({
        status: 'CORRECTIONS_REQUIRED',
        correctionInstructions: 'Review the date.',
        correctionRequestedAt: '2026-09-07T02:00:00Z',
      }));
    renderPage();
    await waitForPage();
    fireEvent.click(screen.getByRole('button', { name: 'Request Corrections' }));
    fireEvent.change(screen.getByLabelText('Instructions'), { target: { value: '  Review the date.  ' } });
    fireEvent.click(screen.getByRole('button', { name: 'Send Correction Request' }));

    expect(screen.getByRole('button', { name: 'Submitting...' }).disabled).toBe(true);
    expect(screen.getByRole('button', { name: 'Mark Eligible' }).disabled).toBe(true);
    resolveMutation({ status: 'CORRECTIONS_REQUIRED' });

    expect(await screen.findByText('Corrections requested successfully.')).toBeTruthy();
    expect(mocks.requestScreeningCorrections).toHaveBeenCalledExactlyOnceWith('42', 'Review the date.');
    await waitFor(() => expect(mocks.getScreeningReviewDetails).toHaveBeenCalledTimes(2));
    expect(screen.getByText('Corrections required')).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Mark Eligible' })).toBeNull();
  });

  it('preserves correction instructions after an API validation error', async () => {
    mocks.requestScreeningCorrections.mockRejectedValue(Object.assign(new Error('Instructions rejected'), { status: 400 }));
    renderPage();
    await waitForPage();
    fireEvent.click(screen.getByRole('button', { name: 'Request Corrections' }));
    fireEvent.change(screen.getByLabelText('Instructions'), { target: { value: 'Keep this instruction' } });
    fireEvent.click(screen.getByRole('button', { name: 'Send Correction Request' }));

    expect(await screen.findByText('Instructions rejected')).toBeTruthy();
    expect(screen.getByLabelText('Instructions').value).toBe('Keep this instruction');
  });

  it('handles a stale correction conflict by refreshing the latest state', async () => {
    mocks.requestScreeningCorrections.mockRejectedValue(Object.assign(new Error('Conflict'), { status: 409 }));
    mocks.getScreeningReviewDetails
      .mockResolvedValueOnce(details())
      .mockResolvedValueOnce(reviewedDetails());
    renderPage();
    await waitForPage();
    fireEvent.click(screen.getByRole('button', { name: 'Request Corrections' }));
    fireEvent.change(screen.getByLabelText('Instructions'), { target: { value: 'Review again' } });
    fireEvent.click(screen.getByRole('button', { name: 'Send Correction Request' }));

    expect(await screen.findByText(/updated by another action/)).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Reload latest review state' })).toBeTruthy();
    await screen.findByRole('heading', { name: 'Recorded final decision' });
    expect(screen.queryByRole('button', { name: 'Mark Eligible' })).toBeNull();
  });

  it('validates eligible reason and future validity before submission', async () => {
    renderPage();
    await waitForPage();
    fireEvent.click(screen.getByRole('button', { name: 'Mark Eligible' }));
    fireEvent.click(screen.getByRole('button', { name: 'Confirm Eligible' }));
    expect(screen.getByText('Reason is required.')).toBeTruthy();

    fireEvent.change(screen.getByLabelText('Reason'), { target: { value: 'Eligible after review' } });
    fireEvent.click(screen.getByRole('button', { name: 'Confirm Eligible' }));
    expect(screen.getByText('Valid until is required for an eligible decision.')).toBeTruthy();

    fireEvent.change(screen.getByLabelText('Valid until'), { target: { value: '2020-01-01T00:00' } });
    fireEvent.click(screen.getByRole('button', { name: 'Confirm Eligible' }));
    expect(screen.getByText('Valid until must be a future date and time.')).toBeTruthy();
    expect(mocks.recordEligibilityDecision).not.toHaveBeenCalled();
  });

  it('submits the eligible ISO payload without reviewerId or clinicalNotes', async () => {
    mocks.getScreeningReviewDetails
      .mockResolvedValueOnce(details())
      .mockResolvedValueOnce(reviewedDetails('ELIGIBLE'));
    renderPage();
    await waitForPage();
    fireEvent.click(screen.getByRole('button', { name: 'Mark Eligible' }));
    fireEvent.change(screen.getByLabelText('Reason'), { target: { value: '  Eligible after review  ' } });
    fireEvent.change(screen.getByLabelText('Valid until'), { target: { value: '2099-01-01T12:30' } });
    fireEvent.click(screen.getByRole('button', { name: 'Confirm Eligible' }));

    expect(await screen.findByText('Eligibility decision recorded.')).toBeTruthy();
    expect(mocks.recordEligibilityDecision).toHaveBeenCalledExactlyOnceWith('42', {
      status: 'ELIGIBLE',
      reason: 'Eligible after review',
      validUntil: new Date('2099-01-01T12:30').toISOString(),
    });
    const payload = mocks.recordEligibilityDecision.mock.calls[0][1];
    expect(payload).not.toHaveProperty('reviewerId');
    expect(payload).not.toHaveProperty('clinicalNotes');
    expect(screen.getByRole('heading', { name: 'Recorded final decision' })).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Mark Ineligible' })).toBeNull();
  });

  it('preserves eligible form values after a backend validation error', async () => {
    mocks.recordEligibilityDecision.mockRejectedValue(Object.assign(new Error('Validity rejected'), { status: 400 }));
    renderPage();
    await waitForPage();
    fireEvent.click(screen.getByRole('button', { name: 'Mark Eligible' }));
    fireEvent.change(screen.getByLabelText('Reason'), { target: { value: 'Keep this reason' } });
    fireEvent.change(screen.getByLabelText('Valid until'), { target: { value: '2099-01-01T12:30' } });
    fireEvent.click(screen.getByRole('button', { name: 'Confirm Eligible' }));

    expect(await screen.findByText('Validity rejected')).toBeTruthy();
    expect(screen.getByLabelText('Reason').value).toBe('Keep this reason');
    expect(screen.getByLabelText('Valid until').value).toBe('2099-01-01T12:30');
  });

  it('handles an eligible decision conflict and reloads the reviewed state', async () => {
    mocks.recordEligibilityDecision.mockRejectedValue(Object.assign(new Error('Conflict'), { status: 409 }));
    mocks.getScreeningReviewDetails
      .mockResolvedValueOnce(details())
      .mockResolvedValueOnce(reviewedDetails());
    renderPage();
    await waitForPage();
    fireEvent.click(screen.getByRole('button', { name: 'Mark Eligible' }));
    fireEvent.change(screen.getByLabelText('Reason'), { target: { value: 'Eligible after review' } });
    fireEvent.change(screen.getByLabelText('Valid until'), { target: { value: '2099-01-01T12:30' } });
    fireEvent.click(screen.getByRole('button', { name: 'Confirm Eligible' }));

    expect(await screen.findByText(/updated by another action/)).toBeTruthy();
    expect(await screen.findByRole('heading', { name: 'Recorded final decision' })).toBeTruthy();
  });

  it('submits an ineligible decision without validUntil, reviewerId, or clinicalNotes', async () => {
    mocks.getScreeningReviewDetails
      .mockResolvedValueOnce(details())
      .mockResolvedValueOnce(reviewedDetails('INELIGIBLE'));
    renderPage();
    await waitForPage();
    fireEvent.click(screen.getByRole('button', { name: 'Mark Ineligible' }));

    expect(screen.queryByLabelText('Valid until')).toBeNull();
    fireEvent.click(screen.getByRole('button', { name: 'Confirm Ineligible' }));
    expect(screen.getByText('Reason is required.')).toBeTruthy();
    fireEvent.change(screen.getByLabelText('Reason'), { target: { value: '  Temporarily ineligible  ' } });
    fireEvent.click(screen.getByRole('button', { name: 'Confirm Ineligible' }));

    expect(await screen.findByText('Eligibility decision recorded.')).toBeTruthy();
    expect(mocks.recordEligibilityDecision).toHaveBeenCalledExactlyOnceWith('42', {
      status: 'INELIGIBLE',
      reason: 'Temporarily ineligible',
    });
    expect(screen.getByText('Ineligible')).toBeTruthy();
    expect(screen.getByText('Temporarily ineligible')).toBeTruthy();
  });

  it('disables every mutation action while a decision is being submitted', async () => {
    let resolveDecision;
    mocks.recordEligibilityDecision.mockReturnValue(new Promise((resolve) => { resolveDecision = resolve; }));
    mocks.getScreeningReviewDetails
      .mockResolvedValueOnce(details())
      .mockResolvedValueOnce(reviewedDetails('INELIGIBLE'));
    renderPage();
    await waitForPage();
    fireEvent.click(screen.getByRole('button', { name: 'Mark Ineligible' }));
    fireEvent.change(screen.getByLabelText('Reason'), { target: { value: 'Not eligible' } });
    fireEvent.click(screen.getByRole('button', { name: 'Confirm Ineligible' }));

    expect(screen.getByRole('button', { name: 'Recording...' }).disabled).toBe(true);
    expect(screen.getByRole('button', { name: 'Request Corrections' }).disabled).toBe(true);
    expect(screen.getByRole('button', { name: 'Mark Eligible' }).disabled).toBe(true);
    expect(screen.getByRole('button', { name: 'Mark Ineligible' }).disabled).toBe(true);
    resolveDecision({ status: 'INELIGIBLE' });
    await screen.findByText('Eligibility decision recorded.');
  });

  it.each(['', '   '])('blocks a blank revocation reason: %j', async (value) => {
    mocks.getScreeningReviewDetails.mockResolvedValue(reviewedDetails());
    renderPage();
    await waitForPage();
    fireEvent.click(screen.getByRole('button', { name: 'Revoke Eligibility' }));
    fireEvent.change(screen.getByLabelText('Revocation reason'), { target: { value } });
    fireEvent.click(screen.getByRole('button', { name: 'Confirm Revocation' }));

    expect(screen.getByText('Revocation reason is required.')).toBeTruthy();
    expect(mocks.revokeAssessment).not.toHaveBeenCalled();
  });

  it('submits a trimmed reason once, disables controls, refreshes, and displays revoked state', async () => {
    let resolveRevocation;
    mocks.revokeAssessment.mockReturnValue(new Promise((resolve) => { resolveRevocation = resolve; }));
    mocks.getScreeningReviewDetails
      .mockResolvedValueOnce(reviewedDetails())
      .mockResolvedValueOnce(reviewedDetails('REVOKED'));
    renderPage();
    await waitForPage();
    fireEvent.click(screen.getByRole('button', { name: 'Revoke Eligibility' }));
    fireEvent.change(screen.getByLabelText('Revocation reason'), {
      target: { value: '  Eligibility withdrawn  ' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Confirm Revocation' }));

    expect(screen.getByRole('button', { name: 'Revoking...' }).disabled).toBe(true);
    expect(screen.getByRole('button', { name: 'Revoke Eligibility' }).disabled).toBe(true);
    expect(mocks.revokeAssessment).toHaveBeenCalledExactlyOnceWith(80, 'Eligibility withdrawn');
    resolveRevocation({ status: 'REVOKED' });

    expect(await screen.findByText('Eligibility revoked successfully.')).toBeTruthy();
    await waitFor(() => expect(mocks.getScreeningReviewDetails).toHaveBeenCalledTimes(2));
    expect(screen.getAllByText('Revoked').length).toBeGreaterThan(0);
    expect(screen.getByText('Eligibility withdrawn')).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Revoke Eligibility' })).toBeNull();
  });

  it('shows a backend revocation error and preserves the entered reason', async () => {
    mocks.getScreeningReviewDetails.mockResolvedValue(reviewedDetails());
    mocks.revokeAssessment.mockRejectedValue(Object.assign(
      new Error('Assessment has already been revoked'),
      { status: 400 },
    ));
    renderPage();
    await waitForPage();
    fireEvent.click(screen.getByRole('button', { name: 'Revoke Eligibility' }));
    fireEvent.change(screen.getByLabelText('Revocation reason'), {
      target: { value: 'Keep this revocation reason' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Confirm Revocation' }));

    expect(await screen.findByText('Assessment has already been revoked')).toBeTruthy();
    expect(screen.getByLabelText('Revocation reason').value).toBe('Keep this revocation reason');
  });
});
