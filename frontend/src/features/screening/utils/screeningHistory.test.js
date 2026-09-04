import { describe, expect, it } from 'vitest';
import { ELIGIBILITY_ASSESSMENT_STATUS_LABELS, PRELIMINARY_SCREENING_RESULT, SCREENING_SUBMISSION_STATUS } from '../constants/screeningConstants';
import { assessmentsBySubmission, campaignName, currentEligibilityLabel, formatScreeningTimestamp, latestCampaignRecords, screeningWorkflowLabel } from './screeningHistory';

describe('citizen screening history composition', () => {
  it.each(['PENDING_REVIEW', 'CORRECTIONS_REQUIRED'])('uses neutral pending wording for %s only when false and unassessed', (status) => {
    const submission = { status };
    const falseCheck = { status: 'fulfilled', value: { isEligible: false } };
    expect(currentEligibilityLabel(falseCheck, submission)).toBe('Awaiting final review');
    expect(currentEligibilityLabel(falseCheck, submission, { status: 'INELIGIBLE' })).toBe('Not currently eligible');
    expect(currentEligibilityLabel({ status: 'fulfilled', value: { isEligible: true } }, submission)).toBe('Currently eligible');
    expect(currentEligibilityLabel({ status: 'rejected' }, submission)).toBe('Unable to confirm current eligibility');
    expect(currentEligibilityLabel(undefined, submission)).toBe('Checking current eligibility…');
  });
  it('preserves the current-false label for a reviewed recorded Eligible assessment', () => {
    expect(currentEligibilityLabel({ status: 'fulfilled', value: { isEligible: false } }, { status: 'REVIEWED' }, { status: 'ELIGIBLE' })).toBe('Not currently eligible');
  });
  it('keeps final status labels separate from workflow and preliminary statuses', () => {
    expect(ELIGIBILITY_ASSESSMENT_STATUS_LABELS).toEqual({ ELIGIBLE: 'Eligible', INELIGIBLE: 'Ineligible', EXPIRED: 'Expired', REVOKED: 'Revoked' });
    for (const status of [...Object.values(PRELIMINARY_SCREENING_RESULT), ...Object.values(SCREENING_SUBMISSION_STATUS)]) {
      expect(ELIGIBILITY_ASSESSMENT_STATUS_LABELS[status]).toBeUndefined();
    }
  });
  it('joins by normalized submission ID rather than campaign ID', () => {
    const first = { submissionId: 42, campaignId: 22, status: 'INELIGIBLE' };
    const second = { submissionId: '43', campaignId: 22, status: 'ELIGIBLE' };
    const joined = assessmentsBySubmission([first, second]);
    expect(joined.get('42')).toBe(first);
    expect(joined.get('43')).toBe(second);
    expect(joined.get('22')).toBeUndefined();
    expect(joined.get('44')).toBeUndefined();
  });
  it('selects newest submission and assessment independently without mutating history order', () => {
    const oldSubmission = Object.freeze({ id: 42, campaignId: 22, createdAt: '2026-08-01T10:00:00' });
    const newSubmission = Object.freeze({ id: 43, campaignId: '22', createdAt: '2026-09-01T10:00:00' });
    const oldAssessment = Object.freeze({ id: 1, submissionId: 42, campaignId: 22, reviewedAt: '2026-08-02T10:00:00' });
    const newAssessment = Object.freeze({ id: 2, submissionId: 42, campaignId: 22, reviewedAt: '2026-08-03T10:00:00' });
    const submissions = Object.freeze([oldSubmission, newSubmission]);
    const assessments = Object.freeze([oldAssessment, newAssessment, { id: 3, campaignId: 23, submissionId: 41 }]);
    const records = latestCampaignRecords(submissions, assessments);
    expect(records).toHaveLength(2);
    expect(records[0]).toEqual({ campaignId: 22, submission: newSubmission, assessment: newAssessment });
    expect(records[1].campaignId).toBe(23);
    expect(records[1].submission).toBeUndefined();
    expect(submissions[0]).toBe(oldSubmission);
  });
  it('keeps backend ordering as the tie breaker', () => {
    const records = [{ id: 43, campaignId: 22, createdAt: '2026-08-01' }, { id: 42, campaignId: 22, createdAt: '2026-08-01' }];
    expect(latestCampaignRecords(records, [])[0].submission.id).toBe(43);
  });
  it('returns no campaigns for no records', () => {
    expect(latestCampaignRecords([], [])).toEqual([]);
  });
  it.each([
    [undefined, 'Checking current eligibility…'],
    [{ status: 'rejected' }, 'Unable to confirm current eligibility'],
    [{ status: 'fulfilled', value: null }, 'Unable to confirm current eligibility'],
    [{ status: 'fulfilled', value: { isEligible: 'false' } }, 'Unable to confirm current eligibility'],
    [{ status: 'fulfilled', value: { isEligible: false } }, 'Not currently eligible'],
    [{ status: 'fulfilled', value: { isEligible: true } }, 'Currently eligible'],
  ])('uses only a confirmed boolean for current eligibility: %j', (check, expected) => {
    expect(currentEligibilityLabel(check)).toBe(expected);
  });
  it.each([
    [null, 'No screening submitted'], [{ status: 'DRAFT' }, 'Screening not yet submitted'],
    [{ status: 'PENDING_REVIEW', preliminaryResult: 'PRELIMINARY_PASS' }, 'Awaiting review'],
    [{ status: 'PENDING_REVIEW', preliminaryResult: 'PRELIMINARY_REVIEW_REQUIRED' }, 'Healthcare review required'],
    [{ status: 'CORRECTIONS_REQUIRED' }, 'Corrections required'], [{ status: 'REVIEWED' }, 'Review completed'],
    [{ status: 'UNKNOWN' }, 'Screening status unavailable'],
  ])('keeps workflow neutral for %j', (submission, label) => {
    expect(screeningWorkflowLabel(submission)).toBe(label);
  });
  it('preserves calendar dates without timezone conversion and formats timestamps locally', () => {
    expect(formatScreeningTimestamp('2026-08-29')).toBe('2026-08-29');
    const timestamp = '2026-08-29T23:30:00Z';
    expect(formatScreeningTimestamp(timestamp)).toBe(new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(timestamp)));
    expect(formatScreeningTimestamp(null)).toBe('Not available');
    expect(formatScreeningTimestamp('legacy date')).toBe('legacy date');
  });
  it('uses campaign names with a missing metadata fallback', () => {
    expect(campaignName({ name: 'Historical campaign' }, 22)).toBe('Historical campaign');
    expect(campaignName(null, 22)).toBe('Campaign #22');
  });
});
