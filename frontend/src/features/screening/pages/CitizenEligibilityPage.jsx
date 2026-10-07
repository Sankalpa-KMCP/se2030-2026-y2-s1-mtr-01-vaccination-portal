import React, { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../../auth/context/AuthContext';
import CitizenErrorState from '../../../shared/ui/citizen/CitizenErrorState';
import CitizenStatePanel from '../../../shared/ui/citizen/CitizenStatePanel';
import { getCitizenCampaignEligibility } from '../api/screeningApi';
import {
  CITIZEN_PRELIMINARY_RESULT_LABELS,
  ELIGIBILITY_ASSESSMENT_STATUS,
  ELIGIBILITY_ASSESSMENT_STATUS_LABELS,
  SCREENING_SUBMISSION_STATUS,
  SCREENING_SUBMISSION_STATUS_LABELS,
} from '../constants/screeningConstants';
import useCitizenScreeningRecords from '../hooks/useCitizenScreeningRecords';
import {
  assessmentsBySubmission,
  campaignName,
  currentEligibilityLabel,
  formatScreeningTimestamp,
  latestCampaignRecords,
  screeningWorkflowLabel,
} from '../utils/screeningHistory';
import citizenHealthBanner from '../../../shared/assets/citizen-health-banner.png';
import './CitizenEligibilityPage.css';

function getCampaignActivityEvents(submission, assessment) {
  const events = [];

  // 1. Assessment / recorded decision event
  if (assessment?.reviewedAt) {
    const isRevoked = assessment.status === ELIGIBILITY_ASSESSMENT_STATUS.REVOKED;
    events.push({
      id: `assessment-${assessment.id || 'reviewed'}`,
      timestamp: assessment.reviewedAt,
      title: isRevoked ? 'Eligibility revoked' : 'Eligibility recorded',
      description: isRevoked
        ? (assessment.revocationReason || 'Eligibility status was revoked.')
        : (assessment.reason || (assessment.status === ELIGIBILITY_ASSESSMENT_STATUS.ELIGIBLE ? 'You are eligible for vaccination.' : 'Eligibility assessment completed.')),
      badgeText: ELIGIBILITY_ASSESSMENT_STATUS_LABELS[assessment.status] || assessment.status,
      badgeTone: assessment.status === ELIGIBILITY_ASSESSMENT_STATUS.ELIGIBLE
        ? 'positive'
        : (isRevoked || assessment.status === ELIGIBILITY_ASSESSMENT_STATUS.INELIGIBLE ? 'critical' : 'neutral'),
    });
  }

  // 2. Distinct revocation event if timestamp differs from reviewedAt
  if (assessment?.revokedAt && assessment.revokedAt !== assessment.reviewedAt) {
    events.push({
      id: `revocation-${assessment.id}`,
      timestamp: assessment.revokedAt,
      title: 'Eligibility revoked',
      description: assessment.revocationReason || 'Eligibility status was revoked.',
      badgeText: 'Revoked',
      badgeTone: 'critical',
    });
  }

  // 3. Corrections requested event
  if (submission?.correctionInstructions) {
    const isCurrent = submission.status === SCREENING_SUBMISSION_STATUS.CORRECTIONS_REQUIRED;
    events.push({
      id: `corrections-${submission.id}`,
      timestamp: submission.correctionRequestedAt || submission.createdAt,
      title: 'Corrections requested',
      description: submission.correctionInstructions,
      badgeText: isCurrent ? 'Action Required' : 'Resolved',
      badgeTone: isCurrent ? 'attention' : 'neutral',
    });
  }

  // 4. Screening submission event
  if (submission?.createdAt) {
    events.push({
      id: `submission-${submission.id}`,
      timestamp: submission.createdAt,
      title: 'Screening submitted',
      description: 'Your screening questionnaire was submitted.',
      badgeText: 'Completed',
      badgeTone: 'positive',
    });
  }

  return events.sort((a, b) => (Date.parse(b.timestamp) || 0) - (Date.parse(a.timestamp) || 0));
}

function screeningStatusTone(status) {
  if (status === SCREENING_SUBMISSION_STATUS.CORRECTIONS_REQUIRED) return 'attention';
  if (status === SCREENING_SUBMISSION_STATUS.REVIEWED) return 'positive';
  if (status === SCREENING_SUBMISSION_STATUS.PENDING_REVIEW) return 'pending';
  return 'neutral';
}

function EligibilityRecords({ citizenId }) {
  const { loading, submissions, assessments, campaigns, errors } = useCitizenScreeningRecords(citizenId);
  const [checks, setChecks] = useState({});
  const records = useMemo(() => latestCampaignRecords(submissions, assessments), [submissions, assessments]);
  const linkedAssessments = assessmentsBySubmission(assessments);
  const campaignMap = new Map(campaigns.map((campaign) => [String(campaign.id), campaign]));
  const baseError = errors.submissions || errors.assessments;

  useEffect(() => {
    let active = true;
    if (loading || baseError) return undefined;
    Promise.allSettled(records.map(async ({ campaignId }) => {
      let result;
      try {
        result = { status: 'fulfilled', value: await getCitizenCampaignEligibility(citizenId, campaignId) };
      } catch (reason) {
        result = { status: 'rejected', reason };
      }
      if (active) setChecks((current) => ({ ...current, [campaignId]: result }));
    }));
    return () => { active = false; };
  }, [citizenId, loading, baseError, records]);

  if (loading) {
    return (
      <CitizenStatePanel eyebrow="Eligibility records" title="Loading your eligibility records..." tone="loading" status>
        <p>Checking screening submissions and recorded campaign decisions.</p>
      </CitizenStatePanel>
    );
  }

  return (
    <>
      {baseError && <CitizenErrorState error={baseError} />}
      {errors.campaigns && (
        <div className="citizen-elig-fallback-alert" role="status">
          <p className="mb-0">Campaign details are temporarily unavailable.</p>
        </div>
      )}
      {!baseError && records.length === 0 && (
        <CitizenStatePanel
          eyebrow="Eligibility records"
          title="No screening or eligibility records are available yet."
          actions={<Link className="citizen-action citizen-action--primary citizen-elig-btn citizen-elig-btn--primary" to="/app/screening">Start Pre-Vaccination Screening</Link>}
        >
          <p>Complete a campaign screening to begin building your eligibility record.</p>
        </CitizenStatePanel>
      )}
      {!baseError && records.length > 0 && (
        <div className="citizen-elig-stack">
          {records.map(({ campaignId, submission, assessment }) => {
            const campaign = campaignMap.get(String(campaignId));
            const linked = submission && linkedAssessments.get(String(submission.id));
            const previous = assessment && (!submission || String(assessment.submissionId) !== String(submission.id));
            const check = checks[campaignId];
            const workflowLabel = screeningWorkflowLabel(submission);
            const preliminaryLabel = submission?.preliminaryResult
              ? CITIZEN_PRELIMINARY_RESULT_LABELS[submission.preliminaryResult] || 'Unavailable'
              : null;
            const screeningStatusLabel = submission?.status
              ? SCREENING_SUBMISSION_STATUS_LABELS[submission.status] || submission.status
              : workflowLabel;

            const currentEligibilityText = currentEligibilityLabel(check, submission, linked);
            const isEligible = check?.status === 'fulfilled' && check.value?.isEligible === true;
            const isAwaiting = currentEligibilityText === 'Awaiting final review';
            const isNotEligible = currentEligibilityText === 'Not currently eligible';
            const isRevoked = assessment?.status === ELIGIBILITY_ASSESSMENT_STATUS.REVOKED;
            const isIneligible = assessment?.status === ELIGIBILITY_ASSESSMENT_STATUS.INELIGIBLE;
            const isCorrections = submission?.status === SCREENING_SUBMISSION_STATUS.CORRECTIONS_REQUIRED;

            let currentTone = 'neutral';
            let currentBadgeCode = 'STATUS UNCONFIRMED';
            let summaryHeadline = currentEligibilityText;
            let summaryDescription = 'Review your current campaign eligibility status and recorded history.';

            if (isEligible) {
              currentTone = 'eligible';
              currentBadgeCode = 'ELIGIBLE';
              summaryHeadline = 'Currently eligible';
              summaryDescription = 'Based on your screening results, you meet the eligibility criteria for this campaign.';
            } else if (isAwaiting) {
              currentTone = 'pending';
              currentBadgeCode = 'PENDING';
              summaryHeadline = 'Awaiting final review';
              summaryDescription = 'Your screening has been submitted and is awaiting healthcare review.';
            } else if (isNotEligible) {
              currentTone = isRevoked ? 'revoked' : 'ineligible';
              currentBadgeCode = isRevoked ? 'REVOKED' : 'INELIGIBLE';
              summaryHeadline = 'Not currently eligible';
              summaryDescription = 'Based on the latest clinical assessment, eligibility is not active for this campaign.';
            }

            const activityEvents = getCampaignActivityEvents(submission, assessment);

            return (
              <article
                className="citizen-elig-campaign-card citizen-elig-stack"
                key={campaignId}
                aria-labelledby={`eligibility-campaign-${campaignId}`}
              >
                {/* 2. CURRENT ELIGIBILITY SUMMARY */}
                <section className="citizen-elig-summary" aria-label="Current eligibility status">
                  <div className="citizen-elig-summary__grid">
                    <div className="citizen-elig-summary__left">
                      <p className="citizen-elig-eyebrow">Current eligibility</p>
                      <div className="citizen-elig-summary__state-row">
                        <div
                          className={`citizen-elig-status-icon citizen-elig-status-icon--${currentTone}`}
                          aria-hidden="true"
                        >
                          {isEligible ? (
                            <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                              <polyline points="20 6 9 17 4 12" />
                            </svg>
                          ) : isAwaiting ? (
                            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                              <circle cx="12" cy="12" r="10" />
                              <polyline points="12 6 12 12 16 14" />
                            </svg>
                          ) : isRevoked || isIneligible ? (
                            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                              <circle cx="12" cy="12" r="10" />
                              <line x1="15" y1="9" x2="9" y2="15" />
                              <line x1="9" y1="9" x2="15" y2="15" />
                            </svg>
                          ) : (
                            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                              <circle cx="12" cy="12" r="10" />
                              <line x1="12" y1="16" x2="12" y2="12" />
                              <line x1="12" y1="8" x2="12.01" y2="8" />
                            </svg>
                          )}
                        </div>
                        <div className="citizen-elig-summary__info">
                          <div>
                            <span className={`citizen-elig-badge citizen-elig-badge--${currentTone}`}>
                              {currentBadgeCode}
                            </span>
                          </div>
                          <h2 className="citizen-elig-summary__headline">{summaryHeadline}</h2>
                          {isEligible && (
                            <p className="fw-semibold text-dark mb-1">You are currently eligible for vaccination</p>
                          )}
                          <p className="citizen-elig-summary__description">{summaryDescription}</p>
                        </div>
                      </div>
                    </div>

                    <div className="citizen-elig-summary__right">
                      <div className="citizen-elig-meta-list">
                        <div className="citizen-elig-meta-item">
                          <div className="citizen-elig-meta-icon" aria-hidden="true">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                              <path d="m18 2 4 4" />
                              <path d="m17 7 3-3" />
                              <path d="M19 9 8.7 19.3c-.4.4-1 .4-1.4 0l-2.6-2.6c-.4-.4-.4-1 0-1.4L15 5" />
                              <path d="m9 11 4 4" />
                              <path d="m5 19-3 3" />
                              <path d="m14 4 6 6" />
                            </svg>
                          </div>
                          <div className="citizen-elig-meta-text">
                            <span className="citizen-elig-meta-label">Campaign</span>
                            <span className="citizen-elig-meta-value">{campaignName(campaign, campaignId)}</span>
                          </div>
                        </div>

                        {assessment?.validUntil && (
                          <div className="citizen-elig-meta-item">
                            <div className="citizen-elig-meta-icon" aria-hidden="true">
                              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
                                <line x1="16" y1="2" x2="16" y2="6" />
                                <line x1="8" y1="2" x2="8" y2="6" />
                                <line x1="3" y1="10" x2="21" y2="10" />
                              </svg>
                            </div>
                            <div className="citizen-elig-meta-text">
                              <span className="citizen-elig-meta-label">Valid until</span>
                              <span className="citizen-elig-meta-value">{formatScreeningTimestamp(assessment.validUntil)}</span>
                            </div>
                          </div>
                        )}

                        {assessment?.reviewedAt && (
                          <div className="citizen-elig-meta-item">
                            <div className="citizen-elig-meta-icon" aria-hidden="true">
                              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                                <polyline points="14 2 14 8 20 8" />
                                <line x1="16" y1="13" x2="8" y2="13" />
                                <line x1="16" y1="17" x2="8" y2="17" />
                                <polyline points="10 9 9 9 8 9" />
                              </svg>
                            </div>
                            <div className="citizen-elig-meta-text">
                              <span className="citizen-elig-meta-label">Reviewed on</span>
                              <span className="citizen-elig-meta-value">{formatScreeningTimestamp(assessment.reviewedAt)}</span>
                            </div>
                          </div>
                        )}
                      </div>

                      {submission && (
                        <Link
                          className="citizen-elig-btn citizen-elig-btn--primary"
                          to={`/app/screening/history/${submission.id}`}
                        >
                          View Screening <span aria-hidden="true">→</span>
                        </Link>
                      )}
                    </div>
                  </div>
                </section>

                {/* 3. CAMPAIGN ELIGIBILITY RECORD */}
                <section className="citizen-elig-record" aria-labelledby={`eligibility-campaign-${campaignId}`}>
                  <header className="citizen-elig-record__header">
                    <div className="citizen-elig-record__title-group">
                      <p className="citizen-elig-eyebrow">Campaign eligibility record</p>
                      <h2 id={`eligibility-campaign-${campaignId}`}>{campaignName(campaign, campaignId)}</h2>
                      {campaign?.code && <p className="citizen-elig-record__code">Campaign code: {campaign.code}</p>}
                    </div>
                    <span className={`citizen-elig-badge citizen-elig-badge--${screeningStatusTone(submission?.status)}`}>
                      {!submission ? 'No screening record' : screeningStatusLabel}
                    </span>
                  </header>

                  {/* Prominent Current Corrections Banner */}
                  {isCorrections && submission.correctionInstructions && (
                    <div className="citizen-elig-corrections-alert" role="alert">
                      <h3>Corrections requested</h3>
                      <p>{submission.correctionInstructions}</p>
                      {submission.correctionRequestedAt && (
                        <span className="small text-muted">
                          Requested: {formatScreeningTimestamp(submission.correctionRequestedAt)}
                        </span>
                      )}
                    </div>
                  )}

                  <div className="citizen-elig-record__grid">
                    {/* Left Column: Screening Progress */}
                    <div className="citizen-elig-progress-panel">
                      <h3 className="citizen-elig-progress-panel__title">Screening Progress</h3>
                      <p className="citizen-elig-progress-panel__subtitle">Your progress through the screening process.</p>

                      <div className="citizen-elig-progress-timeline">
                        {!submission ? (
                          <div className="citizen-elig-progress-step">
                            <div className="citizen-elig-progress-step__left">
                              <div className="citizen-elig-progress-step__icon">
                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                  <circle cx="12" cy="12" r="10" />
                                  <line x1="12" y1="8" x2="12" y2="12" />
                                  <line x1="12" y1="16" x2="12.01" y2="16" />
                                </svg>
                              </div>
                              <div className="citizen-elig-progress-step__content">
                                <span className="citizen-elig-progress-step__label">No screening submitted</span>
                              </div>
                            </div>
                            <span className="citizen-elig-progress-step__check citizen-elig-progress-step__check--pending" aria-hidden="true">•</span>
                          </div>
                        ) : submission.status === SCREENING_SUBMISSION_STATUS.DRAFT ? (
                          <div className="citizen-elig-progress-step">
                            <div className="citizen-elig-progress-step__left">
                              <div className="citizen-elig-progress-step__icon">
                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                  <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
                                  <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
                                </svg>
                              </div>
                              <div className="citizen-elig-progress-step__content">
                                <span className="citizen-elig-progress-step__label">Screening not yet submitted</span>
                              </div>
                            </div>
                            <span className="citizen-elig-progress-step__check citizen-elig-progress-step__check--pending" aria-hidden="true">•</span>
                          </div>
                        ) : (
                          <>
                            {/* Step 1: Screening submitted */}
                            <div className="citizen-elig-progress-step">
                              <div className="citizen-elig-progress-step__left">
                                <div className="citizen-elig-progress-step__icon">
                                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                                    <polyline points="14 2 14 8 20 8" />
                                  </svg>
                                </div>
                                <div className="citizen-elig-progress-step__content">
                                  <span className="citizen-elig-progress-step__label">Screening submitted</span>
                                  {submission.createdAt && (
                                    <span className="citizen-elig-progress-step__time">
                                      {formatScreeningTimestamp(submission.createdAt)}
                                    </span>
                                  )}
                                </div>
                              </div>
                              <span className="citizen-elig-progress-step__check citizen-elig-progress-step__check--completed" aria-hidden="true">
                                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3">
                                  <polyline points="20 6 9 17 4 12" />
                                </svg>
                              </span>
                            </div>

                            {/* Step 2: Review completed / Healthcare review */}
                            <div className="citizen-elig-progress-step">
                              <div className="citizen-elig-progress-step__left">
                                <div className="citizen-elig-progress-step__icon">
                                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                                  </svg>
                                </div>
                                <div className="citizen-elig-progress-step__content">
                                  <span className="citizen-elig-progress-step__label">
                                    {submission.status === SCREENING_SUBMISSION_STATUS.REVIEWED
                                      ? 'Review completed'
                                      : (workflowLabel === preliminaryLabel ? 'Awaiting review' : (workflowLabel || 'Awaiting review'))}
                                  </span>
                                  {submission.status === SCREENING_SUBMISSION_STATUS.REVIEWED && assessment?.reviewedAt && (
                                    <span className="citizen-elig-progress-step__time">
                                      {formatScreeningTimestamp(assessment.reviewedAt)}
                                    </span>
                                  )}
                                </div>
                              </div>
                              <span
                                className={`citizen-elig-progress-step__check ${
                                  submission.status === SCREENING_SUBMISSION_STATUS.REVIEWED
                                    ? 'citizen-elig-progress-step__check--completed'
                                    : 'citizen-elig-progress-step__check--pending'
                                }`}
                                aria-hidden="true"
                              >
                                {submission.status === SCREENING_SUBMISSION_STATUS.REVIEWED ? (
                                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3">
                                    <polyline points="20 6 9 17 4 12" />
                                  </svg>
                                ) : '•'}
                              </span>
                            </div>

                            {/* Step 3: Corrections requested (historical or active) */}
                            {submission.correctionInstructions && (
                              <div className="citizen-elig-progress-step">
                                <div className="citizen-elig-progress-step__left">
                                  <div className="citizen-elig-progress-step__icon citizen-elig-progress-step__icon--attention">
                                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                      <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
                                      <line x1="12" y1="9" x2="12" y2="13" />
                                      <line x1="12" y1="17" x2="12.01" y2="17" />
                                    </svg>
                                  </div>
                                  <div className="citizen-elig-progress-step__content">
                                    <span className="citizen-elig-progress-step__label">
                                      {isCorrections ? 'Corrections requested' : 'Corrections requested (resolved)'}
                                    </span>
                                    {submission.correctionRequestedAt && (
                                      <span className="citizen-elig-progress-step__time">
                                        {formatScreeningTimestamp(submission.correctionRequestedAt)}
                                      </span>
                                    )}
                                  </div>
                                </div>
                                <span
                                  className={`citizen-elig-progress-step__check ${
                                    isCorrections
                                      ? 'citizen-elig-progress-step__check--attention'
                                      : 'citizen-elig-progress-step__check--completed'
                                  }`}
                                  aria-hidden="true"
                                >
                                  {isCorrections ? '!' : (
                                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3">
                                      <polyline points="20 6 9 17 4 12" />
                                    </svg>
                                  )}
                                </span>
                              </div>
                            )}

                            {/* Step 4: Final decision recorded */}
                            <div className="citizen-elig-progress-step">
                              <div className="citizen-elig-progress-step__left">
                                <div className="citizen-elig-progress-step__icon">
                                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <polyline points="9 11 12 14 22 4" />
                                    <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11" />
                                  </svg>
                                </div>
                                <div className="citizen-elig-progress-step__content">
                                  <span className="citizen-elig-progress-step__label">
                                    {assessment ? 'Final decision recorded' : 'Final decision not yet recorded'}
                                  </span>
                                  {assessment?.reviewedAt && (
                                    <span className="citizen-elig-progress-step__time">
                                      {formatScreeningTimestamp(assessment.reviewedAt)}
                                    </span>
                                  )}
                                </div>
                              </div>
                              <span
                                className={`citizen-elig-progress-step__check ${
                                  assessment
                                    ? 'citizen-elig-progress-step__check--completed'
                                    : 'citizen-elig-progress-step__check--pending'
                                }`}
                                aria-hidden="true"
                              >
                                {assessment ? (
                                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3">
                                    <polyline points="20 6 9 17 4 12" />
                                  </svg>
                                ) : '•'}
                              </span>
                            </div>
                          </>
                        )}
                      </div>

                      {submission?.status === SCREENING_SUBMISSION_STATUS.REVIEWED && !linked && (
                        <p className="small text-muted mt-3 mb-0">Eligibility decision unavailable</p>
                      )}
                    </div>

                    {/* Right Column: Preliminary Result + Eligibility Decision */}
                    <div className="citizen-elig-decisions-panel">
                      {submission?.preliminaryResult && (
                        <div className="citizen-elig-prelim-card">
                          <div className="citizen-elig-prelim-card__icon" aria-hidden="true">
                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                              <circle cx="12" cy="12" r="10" />
                              <line x1="12" y1="16" x2="12" y2="12" />
                              <line x1="12" y1="8" x2="12.01" y2="8" />
                            </svg>
                          </div>
                          <div className="citizen-elig-prelim-card__body">
                            <span className="citizen-elig-eyebrow">Preliminary Result</span>
                            <h4 className="citizen-elig-prelim-card__headline">{preliminaryLabel}</h4>
                            <p className="citizen-elig-prelim-card__disclaimer">
                              This preliminary result is not a final eligibility decision.
                            </p>
                          </div>
                        </div>
                      )}

                      <div className="citizen-elig-decision-card">
                        <h3 className="citizen-elig-decision-card__title" id={`assessment-state-${campaignId}`}>
                          {previous ? 'Previous recorded assessment' : 'Eligibility Decision'}
                        </h3>
                        {previous && (
                          <div className="citizen-elig-previous-note">
                            This decision belongs to a different screening, not the current screening shown above.
                          </div>
                        )}

                        {assessment ? (
                          <div className="citizen-elig-kv-table">
                            <div className="citizen-elig-kv-row">
                              <span className="citizen-elig-kv-key">Status</span>
                              <span className="citizen-elig-kv-val">
                                <span className={`citizen-elig-badge citizen-elig-badge--${
                                  assessment.status === ELIGIBILITY_ASSESSMENT_STATUS.ELIGIBLE
                                    ? 'eligible'
                                    : (assessment.status === ELIGIBILITY_ASSESSMENT_STATUS.REVOKED || assessment.status === ELIGIBILITY_ASSESSMENT_STATUS.INELIGIBLE ? 'ineligible' : 'neutral')
                                }`}>
                                  {ELIGIBILITY_ASSESSMENT_STATUS_LABELS[assessment.status] || 'Decision unavailable'}
                                </span>
                              </span>
                            </div>

                            {assessment.reason && (
                              <div className="citizen-elig-kv-row">
                                <span className="citizen-elig-kv-key">Reason</span>
                                <span className="citizen-elig-kv-val">{assessment.reason}</span>
                              </div>
                            )}

                            {assessment.reviewedAt && (
                              <div className="citizen-elig-kv-row">
                                <span className="citizen-elig-kv-key">Reviewed on</span>
                                <span className="citizen-elig-kv-val">{formatScreeningTimestamp(assessment.reviewedAt)}</span>
                              </div>
                            )}

                            {assessment.validUntil && (
                              <div className="citizen-elig-kv-row">
                                <span className="citizen-elig-kv-key">Valid until</span>
                                <span className="citizen-elig-kv-val">{formatScreeningTimestamp(assessment.validUntil)}</span>
                              </div>
                            )}

                            {assessment.revokedAt && (
                              <div className="citizen-elig-kv-row">
                                <span className="citizen-elig-kv-key">Revoked on</span>
                                <span className="citizen-elig-kv-val">{formatScreeningTimestamp(assessment.revokedAt)}</span>
                              </div>
                            )}

                            {assessment.revocationReason && (
                              <div className="citizen-elig-kv-row">
                                <span className="citizen-elig-kv-key">Revocation reason</span>
                                <span className="citizen-elig-kv-val">{assessment.revocationReason}</span>
                              </div>
                            )}

                            {previous && (
                              <div className="mt-2">
                                <Link className="small" to={`/app/screening/history/${assessment.submissionId}`}>
                                  View assessed screening
                                </Link>
                              </div>
                            )}

                            {assessment.status === ELIGIBILITY_ASSESSMENT_STATUS.ELIGIBLE && check?.status === 'fulfilled' && check.value?.isEligible === false && (
                              <div className="small text-muted mt-2">
                                <p className="mb-1">
                                  The recorded decision remains Eligible. Its validity may have elapsed or the campaign service period may have ended.
                                </p>
                                {campaign?.serviceEndDate && <p className="mb-0">Campaign service end: {campaign.serviceEndDate}</p>}
                              </div>
                            )}
                          </div>
                        ) : (
                          <div>
                            <p className="citizen-ui-eyebrow">Recorded decision</p>
                            <h4 className="h6 mb-0">No assessment recorded</h4>
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                </section>

                {/* 4. RECENT ACTIVITY */}
                <section className="citizen-elig-activity" aria-label="Recent activity">
                  <header className="citizen-elig-activity__header">
                    <div className="citizen-elig-activity__header-left">
                      <div className="citizen-elig-activity__header-icon" aria-hidden="true">
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                          <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
                        </svg>
                      </div>
                      <div>
                        <h3 className="citizen-elig-activity__title">Recent Activity</h3>
                        <p className="citizen-elig-activity__subtitle">A history of important events for this campaign.</p>
                      </div>
                    </div>
                    <Link to="/app/screening/history" className="citizen-elig-activity__link">
                      View Full History <span aria-hidden="true">→</span>
                    </Link>
                  </header>

                  <div className="citizen-elig-activity__timeline">
                    {activityEvents.length === 0 ? (
                      <p className="text-muted small mb-0 py-2">No recent activity recorded for this campaign.</p>
                    ) : (
                      activityEvents.map((event) => (
                        <div key={event.id} className="citizen-elig-activity__item">
                          <span className="citizen-elig-activity__timestamp">
                            {formatScreeningTimestamp(event.timestamp)}
                          </span>
                          <div className="citizen-elig-activity__dot-col">
                            <span
                              className={`citizen-elig-activity__dot citizen-elig-activity__dot--${event.badgeTone}`}
                              aria-hidden="true"
                            >
                              {event.badgeTone === 'positive' ? (
                                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3">
                                  <polyline points="20 6 9 17 4 12" />
                                </svg>
                              ) : event.badgeTone === 'attention' ? (
                                '!'
                              ) : (
                                <svg width="10" height="10" viewBox="0 0 24 24" fill="currentColor">
                                  <circle cx="12" cy="12" r="8" />
                                </svg>
                              )}
                            </span>
                          </div>
                          <div className="citizen-elig-activity__content">
                            <span className="citizen-elig-activity__event-title">{event.title}</span>
                            <span className="citizen-elig-activity__event-desc">{event.description}</span>
                          </div>
                          <div>
                            <span className={`citizen-elig-badge citizen-elig-badge--${event.badgeTone}`}>
                              {event.badgeText}
                            </span>
                          </div>
                        </div>
                      ))
                    )}
                  </div>
                </section>

                {/* 5. BOTTOM ACTIONS */}
                <section className="citizen-elig-actions" aria-label="Campaign actions">
                  <div className="citizen-elig-actions__left">
                    <div className="citizen-elig-actions__icon" aria-hidden="true">
                      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                        <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />
                      </svg>
                    </div>
                    <div>
                      <h3 className="citizen-elig-actions__title">Actions</h3>
                      <p className="citizen-elig-actions__subtitle">Manage your screening and eligibility for this campaign.</p>
                    </div>
                  </div>

                  <div className="citizen-elig-actions__buttons">
                    {submission && (
                      <Link
                        className="citizen-elig-btn citizen-elig-btn--primary"
                        to={`/app/screening/history/${submission.id}`}
                      >
                        View Screening <span aria-hidden="true">→</span>
                      </Link>
                    )}
                    <Link
                      className="citizen-elig-btn citizen-elig-btn--outline"
                      to="/app/screening/history"
                    >
                      View Screening History
                    </Link>
                    {isCorrections && (
                      <Link
                        className="citizen-elig-btn citizen-elig-btn--attention"
                        to={`/app/screening/history/${submission.id}`}
                      >
                        Correct Screening
                      </Link>
                    )}
                  </div>
                </section>
              </article>
            );
          })}
        </div>
      )}
    </>
  );
}

export default function CitizenEligibilityPage() {
  const { user } = useAuth();

  return (
    <section className="citizen-elig-page" aria-labelledby="citizen-eligibility-title">
      {/* 1. PAGE HEADER / HERO */}
      <header className="citizen-elig-hero">
        <div className="citizen-elig-hero__content">
          <Link to="/app/citizen" className="citizen-elig-back-link">
            <span aria-hidden="true">←</span> Back to Dashboard
          </Link>
          <p className="citizen-elig-eyebrow">SCREENING & ELIGIBILITY</p>
          <h1 className="citizen-elig-hero__title" id="citizen-eligibility-title">My Eligibility</h1>
          <p className="citizen-elig-hero__subtitle">
            Your screening progress, recorded decisions, and current eligibility by campaign.
          </p>
        </div>

        <div className="citizen-elig-hero__aside">
          <div className="citizen-elig-hero__illustration" aria-hidden="true">
            <img src={citizenHealthBanner} alt="" />
          </div>
          <Link to="/app/screening/history" className="citizen-elig-btn citizen-elig-btn--outline">
            View Screening History
          </Link>
        </div>
      </header>

      <main className="mt-4">
        {user?.id ? (
          <EligibilityRecords key={user.id} citizenId={user.id} />
        ) : (
          <CitizenErrorState error="Your authenticated citizen profile is missing an account identifier." />
        )}
      </main>
    </section>
  );
}
