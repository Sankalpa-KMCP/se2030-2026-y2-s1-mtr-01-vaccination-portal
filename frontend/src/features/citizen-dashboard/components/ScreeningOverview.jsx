import React from 'react';
import { Link } from 'react-router-dom';
import DashboardIcon from './DashboardIcon';
import { formatStatus, selectLatest } from '../utils/citizenDashboardState';

const PRELIMINARY_LABELS = {
  PRELIMINARY_PASS: 'Preliminary screening passed',
  PRELIMINARY_REVIEW_REQUIRED: 'Healthcare review required',
};

function campaignLabel(campaigns, campaignId) {
  const campaign = campaigns.find((item) => String(item.id) === String(campaignId));
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

  return (
    <section className="citizen-panel" aria-labelledby="citizen-screening-title" aria-live="polite">
      <header className="citizen-panel__header">
        <span className="citizen-panel__icon"><DashboardIcon name="clipboard" /></span>
        <div>
          <p className="citizen-dashboard__eyebrow">Screening &amp; eligibility</p>
          <h2 id="citizen-screening-title">Your recorded progress</h2>
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
        <div className="citizen-panel__state">
          <p><strong>No screening submitted</strong><span>Choose an available campaign to begin its questionnaire.</span></p>
          <Link className="citizen-action-link citizen-action-link--secondary" to="/app/screening">
            Choose a campaign <DashboardIcon name="arrow" size={16} />
          </Link>
        </div>
      )}

      {!resource.loading && latestSubmission && (
        <article className="citizen-record">
          <div className="citizen-record__heading">
            <div>
              <p className="citizen-record__label">Latest screening</p>
              <h3>{campaignLabel(resource.campaigns, latestSubmission.campaignId)}</h3>
            </div>
            <span className={`citizen-status citizen-status--${latestSubmission.status === 'CORRECTIONS_REQUIRED' ? 'attention' : 'neutral'}`}>
              {formatStatus(latestSubmission.status)}
            </span>
          </div>
          {latestSubmission.preliminaryResult && (
            <div className="citizen-screening-fact">
              <span>Preliminary result</span>
              <strong>{PRELIMINARY_LABELS[latestSubmission.preliminaryResult] || 'Unavailable'}</strong>
              <small>This is not a final eligibility decision.</small>
            </div>
          )}
        </article>
      )}

      {!resource.loading && latestAssessment && (
        <div className="citizen-recorded-decision">
          <span>Recorded assessment</span>
          <strong>{formatStatus(latestAssessment.status)}</strong>
          <small>Use My Eligibility to confirm current campaign eligibility.</small>
        </div>
      )}

      {!resource.loading && resource.errors?.assessments && (
        <p className="citizen-panel__note citizen-panel__note--warning">Recorded assessment information is temporarily unavailable.</p>
      )}
      {!resource.loading && resource.errors?.campaigns && latestSubmission && (
        <p className="citizen-panel__note citizen-panel__note--warning">Campaign names are unavailable; record references remain visible.</p>
      )}

      {!resource.loading && (
        <nav className="citizen-panel__links" aria-label="Screening and eligibility links">
          <Link to="/app/screening/eligibility">View My Eligibility <DashboardIcon name="arrow" size={16} /></Link>
          <Link to="/app/screening/history">Screening history</Link>
        </nav>
      )}
    </section>
  );
}
