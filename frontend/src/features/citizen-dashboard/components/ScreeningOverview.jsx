import React from 'react';
import { Link } from 'react-router-dom';
import DashboardIcon from './DashboardIcon';
import { formatRecordedDate, formatStatus, selectLatest } from '../utils/citizenDashboardState';

const PRELIMINARY_LABELS = {
  PRELIMINARY_PASS: 'Preliminary screening passed',
  PRELIMINARY_REVIEW_REQUIRED: 'Healthcare review required',
};

function campaignLabel(campaigns, campaignId) {
  const campaign = (campaigns || []).find((item) => String(item.id) === String(campaignId));
  return campaign?.name || `Campaign #${campaignId}`;
}

export default function ScreeningOverview({ resource }) {
  const submissionsAvailable = !resource.errors?.submissions;
  const latestSubmission = submissionsAvailable
    ? selectLatest(resource.submissions, ['createdAt', 'submittedAt'])
    : null;
  const latestAssessment = !resource.errors?.assessments
    ? selectLatest(resource.assessments, ['reviewedAt', 'createdAt'])
    : null;

  const isEligible = latestAssessment?.status === 'ELIGIBLE';
  const isCorrections = latestSubmission?.status === 'CORRECTIONS_REQUIRED';
  const isPending = latestSubmission?.status === 'PENDING_REVIEW';
  const isIneligible = latestAssessment?.status === 'INELIGIBLE' || latestAssessment?.status === 'REVOKED';

  // Determine tone, label, and copy
  let statusTone = 'neutral';
  let statusLabel = 'NO RECORD';
  let statusHeadline = 'Check your eligibility';
  let statusDesc = 'Submit a screening questionnaire to establish your campaign eligibility.';

  if (isEligible) {
    statusTone = 'positive';
    statusLabel = 'ELIGIBLE';
    statusHeadline = 'You are eligible for vaccination';
    statusDesc = 'Based on your screening results, you meet the eligibility criteria for the selected campaigns.';
  } else if (isCorrections) {
    statusTone = 'attention';
    statusLabel = 'CORRECTIONS REQUIRED';
    statusHeadline = 'Clarification requested';
    statusDesc = 'A healthcare reviewer requested clarification on your screening answers.';
  } else if (isPending) {
    statusTone = 'pending';
    statusLabel = 'PENDING REVIEW';
    statusHeadline = 'Screening awaiting review';
    statusDesc = 'Your questionnaire has been submitted and is currently being evaluated.';
  } else if (isIneligible) {
    statusTone = 'danger';
    statusLabel = formatStatus(latestAssessment.status).toUpperCase();
    statusHeadline = 'Not eligible for vaccination';
    statusDesc = 'Recorded clinical assessment indicates criteria not currently met.';
  } else if (latestAssessment) {
    statusTone = 'neutral';
    statusLabel = formatStatus(latestAssessment.status).toUpperCase();
    statusHeadline = 'Assessment recorded';
    statusDesc = 'Review your recorded campaign eligibility decision.';
  }

  return (
    <section
      className={`citizen-panel citizen-eligibility-card citizen-eligibility-card--${statusTone}`}
      aria-labelledby="citizen-screening-title"
      aria-live="polite"
    >
      <header className="citizen-panel__header">
        <span className="citizen-panel__icon" aria-hidden="true">
          <DashboardIcon name="check" size={20} />
        </span>
        <div>
          <p className="citizen-dashboard__eyebrow">Eligibility Status</p>
          <h2 id="citizen-screening-title">Eligibility Status</h2>
        </div>
      </header>

      {resource.loading && (
        <div className="citizen-panel__state" role="status">
          <span className="citizen-panel__pulse" aria-hidden="true" />
          <p><strong>Loading screening records</strong><span>Checking submissions and recorded eligibility decisions.</span></p>
        </div>
      )}

      {!resource.loading && resource.errors?.submissions && (
        <div className="citizen-panel__state citizen-panel__state--error" role="alert">
          <p><strong>Screening history could not be loaded</strong><span>No screening status has been inferred.</span></p>
        </div>
      )}

      {!resource.loading && submissionsAvailable && resource.submissions.length === 0 && (
        <div className="citizen-panel__state citizen-panel__state--empty citizen-eligibility-card__empty">
          <p><strong>No screening submitted</strong><span>Choose an available campaign to begin its questionnaire.</span></p>
          <Link className="citizen-action-link citizen-action-link--secondary" to="/app/screening">
            Choose a campaign <DashboardIcon name="arrow" size={16} />
          </Link>
        </div>
      )}

      {!resource.loading && (latestSubmission || latestAssessment) && (
        <div className="citizen-eligibility-card__highlight">
          <div className="citizen-eligibility-card__icon-wrap">
            <span className={`citizen-eligibility-card__icon citizen-eligibility-card__icon--${statusTone}`} aria-hidden="true">
              {isEligible ? '✓' : isCorrections ? '!' : isPending ? '⏳' : 'ℹ'}
            </span>
          </div>
          <div className="citizen-eligibility-card__copy">
            <div className="citizen-eligibility-card__tag-row">
              <span className="citizen-eligibility-card__tag">{statusLabel}</span>
              {latestAssessment && (
                <span className="citizen-eligibility-card__decision-pill">
                  {formatStatus(latestAssessment.status)}
                </span>
              )}
            </div>
            <strong className="citizen-eligibility-card__headline">{statusHeadline}</strong>
            <p className="citizen-eligibility-card__desc">{statusDesc}</p>

            {latestAssessment?.validUntil && (
              <div className="citizen-eligibility-card__validity">
                <span className="citizen-eligibility-card__validity-icon" aria-hidden="true">📅</span>
                <span>Valid until: <strong>{formatRecordedDate(latestAssessment.validUntil)}</strong></span>
              </div>
            )}
          </div>
        </div>
      )}

      {!resource.loading && latestSubmission && (
        <div className="citizen-eligibility-card__submission-row">
          <p className="citizen-record__label">Screening campaign</p>
          <h3 className="citizen-eligibility-card__campaign-heading">
            {campaignLabel(resource.campaigns, latestSubmission.campaignId)}
          </h3>
          {latestSubmission.preliminaryResult && (
            <div className="citizen-screening-fact">
              <span>Preliminary result</span>
              <strong>{PRELIMINARY_LABELS[latestSubmission.preliminaryResult] || 'Unavailable'}</strong>
              <small>This is not a final eligibility decision.</small>
            </div>
          )}
        </div>
      )}

      {!resource.loading && resource.errors?.assessments && (
        <p className="citizen-panel__note citizen-panel__note--warning">Recorded assessment information is temporarily unavailable.</p>
      )}
      {!resource.loading && resource.errors?.campaigns && latestSubmission && (
        <p className="citizen-panel__note citizen-panel__note--warning">Campaign names are unavailable at this time.</p>
      )}

      {!resource.loading && (
        <nav className="citizen-panel__links" aria-label="Screening and eligibility links">
          <Link to="/app/screening/eligibility" className="citizen-action-link citizen-action-link--primary citizen-eligibility-card__cta">
            View Eligibility <DashboardIcon name="arrow" size={16} />
          </Link>
        </nav>
      )}
    </section>
  );
}
