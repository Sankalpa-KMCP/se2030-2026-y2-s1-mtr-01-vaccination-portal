import {
  PRELIMINARY_SCREENING_RESULT,
  SCREENING_SUBMISSION_STATUS,
  SCREENING_SUBMISSION_STATUS_LABELS,
} from '../constants/screeningConstants';

export function formatScreeningTimestamp(value) {
  if (!value) return 'Not available';
  // Calendar-only dates must not be interpreted as midnight UTC and shifted a day.
  if (/^\d{4}-\d{2}-\d{2}$/.test(value)) return value;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value
    : new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(date);
}

export function campaignName(campaign, campaignId) {
  return campaign?.name || `Campaign #${campaignId}`;
}

export function assessmentsBySubmission(assessments) {
  return new Map(assessments.map((assessment) => [String(assessment.submissionId), assessment]));
}

export function latestCampaignRecords(submissions, assessments) {
  const campaigns = new Map();
  const add = (record, field, timestamp) => {
    const id = String(record.campaignId);
    if (!campaigns.has(id)) campaigns.set(id, { campaignId: record.campaignId });
    const group = campaigns.get(id);
    if (!group[field] || Date.parse(record[timestamp]) > Date.parse(group[field][timestamp])) {
      group[field] = record;
    }
  };
  submissions.forEach((submission) => add(submission, 'submission', 'createdAt'));
  assessments.forEach((assessment) => add(assessment, 'assessment', 'reviewedAt'));
  return [...campaigns.values()];
}

export function screeningWorkflowLabel(submission) {
  if (!submission) return 'No screening submitted';
  switch (submission.status) {
    case SCREENING_SUBMISSION_STATUS.DRAFT: return 'Screening not yet submitted';
    case SCREENING_SUBMISSION_STATUS.PENDING_REVIEW:
      return submission.preliminaryResult === PRELIMINARY_SCREENING_RESULT.REVIEW_REQUIRED
        ? 'Healthcare review required' : 'Awaiting review';
    case SCREENING_SUBMISSION_STATUS.CORRECTIONS_REQUIRED: return 'Corrections required';
    case SCREENING_SUBMISSION_STATUS.REVIEWED: return 'Review completed';
    default: return SCREENING_SUBMISSION_STATUS_LABELS[submission.status] || 'Screening status unavailable';
  }
}

export function currentEligibilityLabel(check, submission, linkedAssessment) {
  if (!check) return 'Checking current eligibility…';
  if (check.status !== 'fulfilled' || typeof check.value?.isEligible !== 'boolean') {
    return 'Unable to confirm current eligibility';
  }
  const awaitingDecision = !linkedAssessment && (
    submission?.status === SCREENING_SUBMISSION_STATUS.PENDING_REVIEW
    || submission?.status === SCREENING_SUBMISSION_STATUS.CORRECTIONS_REQUIRED
  );
  if (!check.value.isEligible && awaitingDecision) return 'Awaiting final review';
  return check.value.isEligible ? 'Currently eligible' : 'Not currently eligible';
}
