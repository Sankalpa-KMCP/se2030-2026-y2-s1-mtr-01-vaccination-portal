import React, { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../../auth/context/AuthContext';
import CitizenErrorState from '../../citizen-ui/components/CitizenErrorState';
import CitizenPageHeader from '../../citizen-ui/components/CitizenPageHeader';
import CitizenSection from '../../citizen-ui/components/CitizenSection';
import CitizenStatePanel from '../../citizen-ui/components/CitizenStatePanel';
import CitizenStatusBadge from '../../citizen-ui/components/CitizenStatusBadge';
import { getCitizenCampaignEligibility } from '../api/screeningApi';
import RecordedEligibilityAssessment from '../components/RecordedEligibilityAssessment';
import ScreeningCorrectionNotice from '../components/ScreeningCorrectionNotice';
import { CITIZEN_PRELIMINARY_RESULT_LABELS, ELIGIBILITY_ASSESSMENT_STATUS, SCREENING_SUBMISSION_STATUS_LABELS } from '../constants/screeningConstants';
import useCitizenScreeningRecords from '../hooks/useCitizenScreeningRecords';
import { assessmentsBySubmission, campaignName, currentEligibilityLabel, latestCampaignRecords, screeningWorkflowLabel } from '../utils/screeningHistory';

function screeningTone(status) {
  if (status === 'CORRECTIONS_REQUIRED') return 'attention';
  if (status === 'REVIEWED') return 'positive';
  if (status === 'PENDING_REVIEW') return 'pending';
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
    // Publish each settled check independently so one unavailable campaign cannot hide others.
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

  if (loading) return (
    <CitizenStatePanel eyebrow="Eligibility records" title="Loading your eligibility records..." tone="loading" status>
      <p>Checking screening submissions and recorded campaign decisions.</p>
    </CitizenStatePanel>
  );
  return (
    <>
      {baseError && <CitizenErrorState error={baseError} />}
      {errors.campaigns && <CitizenStatePanel title="Campaign information unavailable" tone="warning"><p>Your records remain visible using campaign references.</p></CitizenStatePanel>}
      {!baseError && records.length === 0 && (
        <CitizenStatePanel
          eyebrow="Eligibility records"
          title="No screening or eligibility records are available yet."
          actions={<Link className="citizen-action citizen-action--primary" to="/app/screening">Start Pre-Vaccination Screening</Link>}
        >
          <p>Complete a campaign screening to begin building your eligibility record.</p>
        </CitizenStatePanel>
      )}
      {!baseError && records.length > 0 && <CitizenSection
        eyebrow="By campaign"
        title="Your eligibility records"
        titleId="eligibility-records-title"
        description="Screening progress, recorded assessments, and current eligibility remain separate so each status keeps its proper meaning."
        aside={<CitizenStatusBadge tone="info">{records.length} campaign{records.length === 1 ? '' : 's'}</CitizenStatusBadge>}
      ><div className="citizen-page__stack">{records.map(({ campaignId, submission, assessment }) => {
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
        return (
          <article className="citizen-detail-panel" key={campaignId} aria-labelledby={`eligibility-campaign-${campaignId}`}>
              <header className="citizen-detail-panel__header citizen-campaign-card__heading">
                <div>
                  <p className="citizen-ui-eyebrow">Campaign eligibility</p>
                  <h2 id={`eligibility-campaign-${campaignId}`}>{campaignName(campaign, campaignId)}</h2>
                  {campaign?.code && <p>Campaign code: {campaign.code}</p>}
                </div>
                <CitizenStatusBadge tone={screeningTone(submission?.status)}>
                  {screeningStatusLabel}
                </CitizenStatusBadge>
              </header>
              <div className="citizen-detail-panel__body citizen-page__split">
                <section aria-labelledby={`screening-state-${campaignId}`}>
                  <p className="citizen-ui-eyebrow">Screening stage</p>
                  <h3 className="h6" id={`screening-state-${campaignId}`}>Current screening</h3>
                  {submission && submission.status !== 'CORRECTIONS_REQUIRED' && workflowLabel !== preliminaryLabel && <p>{workflowLabel}</p>}
                  {submission?.preliminaryResult && <div className="citizen-state citizen-state--neutral">
                    <span className="citizen-state__marker" aria-hidden="true" />
                    <div className="citizen-state__copy">
                      <p className="citizen-ui-eyebrow">Preliminary result</p>
                      <strong>{preliminaryLabel}</strong>
                      <p>This preliminary result is not a final eligibility decision.</p>
                    </div>
                  </div>}
                <ScreeningCorrectionNotice submission={submission} />
                {submission?.status === 'REVIEWED' && !linked && <p>Eligibility decision unavailable</p>}
                </section>
                <section aria-labelledby={`assessment-state-${campaignId}`}>
                {assessment ? <div>
                  <h3 className="h6" id={`assessment-state-${campaignId}`}>{previous ? 'Previous recorded assessment' : 'Recorded assessment'}</h3>
                  {previous && <p className="small text-muted">This decision belongs to a different screening, not the current screening shown above.</p>}
                  <RecordedEligibilityAssessment assessment={assessment} />
                  {previous && <Link className="small" to={`/app/screening/history/${assessment.submissionId}`}>View assessed screening</Link>}
                </div> : <><p className="citizen-ui-eyebrow">Recorded decision</p><h3 className="h6" id={`assessment-state-${campaignId}`}>No assessment recorded</h3></>}
                <div className="citizen-state mt-3" aria-live="polite">
                  <span className="citizen-state__marker" aria-hidden="true" />
                  <div className="citizen-state__copy">
                  <p className="citizen-ui-eyebrow">Current eligibility</p>
                  <p>{currentEligibilityLabel(check, submission, linked)}</p>
                  {assessment?.status === ELIGIBILITY_ASSESSMENT_STATUS.ELIGIBLE && check?.status === 'fulfilled' && check.value?.isEligible === false && <>
                    <p className="small text-muted">The recorded decision remains Eligible. Its validity may have elapsed or the campaign service period may have ended.</p>
                    {campaign?.serviceEndDate && <p className="small">Campaign service end: {campaign.serviceEndDate}</p>}
                  </>}
                  </div>
                </div>
                </section>
              </div>
              {submission && <div className="citizen-form-panel__body border-top"><Link className="citizen-action citizen-action--secondary" to={`/app/screening/history/${submission.id}`}>View Screening</Link></div>}
            </article>
        );
      })}</div></CitizenSection>}
    </>
  );
}

export default function CitizenEligibilityPage() {
  const { user } = useAuth();
  return (
    <section className="citizen-page" aria-labelledby="citizen-eligibility-title">
      <CitizenPageHeader
        eyebrow="Screening & Eligibility"
        title="My Eligibility"
        titleId="citizen-eligibility-title"
        subtitle="Your screening progress, recorded decisions, and current eligibility by campaign."
        backTo="/app/citizen"
        backLabel="Back to Dashboard"
        actions={<Link to="/app/screening/history">View Screening History</Link>}
      />
      {user?.id ? <EligibilityRecords key={user.id} citizenId={user.id} />
        : <CitizenErrorState error="Your authenticated citizen profile is missing an account identifier." />}
    </section>
  );
}
