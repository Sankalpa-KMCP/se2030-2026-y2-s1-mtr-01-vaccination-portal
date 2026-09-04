import React, { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../../auth/context/AuthContext';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import LoadingSpinner from '../../../components/common/LoadingSpinner';
import { getCitizenCampaignEligibility } from '../api/screeningApi';
import RecordedEligibilityAssessment from '../components/RecordedEligibilityAssessment';
import ScreeningCorrectionNotice from '../components/ScreeningCorrectionNotice';
import { CITIZEN_PRELIMINARY_RESULT_LABELS, ELIGIBILITY_ASSESSMENT_STATUS } from '../constants/screeningConstants';
import useCitizenScreeningRecords from '../hooks/useCitizenScreeningRecords';
import { assessmentsBySubmission, campaignName, currentEligibilityLabel, latestCampaignRecords, screeningWorkflowLabel } from '../utils/screeningHistory';

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

  if (loading) return <LoadingSpinner message="Loading your eligibility records..." />;
  return (
    <section aria-labelledby="citizen-eligibility-title">
      <div className="mb-4">
        <h1 className="display-6 fw-semibold" id="citizen-eligibility-title">My Eligibility</h1>
        <p className="text-muted">Your screening progress, recorded decisions and current eligibility by campaign.</p>
        <Link to="/app/screening/history">View Screening History</Link>
      </div>
      <ApiErrorAlert error={baseError} />
      {errors.campaigns && <p className="alert alert-warning">Campaign information is unavailable. Your records remain visible using campaign references.</p>}
      {!baseError && records.length === 0 && <div className="card bg-light border-0"><div className="card-body p-4">
        <p>No screening or eligibility records are available yet.</p>
        <Link className="btn btn-primary" to="/app/screening">Start Pre-Vaccination Screening</Link>
      </div></div>}
      {!baseError && <div className="row g-4">{records.map(({ campaignId, submission, assessment }) => {
        const campaign = campaignMap.get(String(campaignId));
        const linked = submission && linkedAssessments.get(String(submission.id));
        const previous = assessment && (!submission || String(assessment.submissionId) !== String(submission.id));
        const check = checks[campaignId];
        return (
          <div className="col-12 col-xl-6" key={campaignId}>
            <article className="card h-100 shadow-sm border-primary-subtle" aria-labelledby={`eligibility-campaign-${campaignId}`}>
              <div className="card-body p-4">
                <h2 className="h4" id={`eligibility-campaign-${campaignId}`}>{campaignName(campaign, campaignId)}</h2>
                {campaign?.code && <p className="small text-muted">Campaign code: {campaign.code}</p>}
                <h3 className="h6 mt-3">Current screening</h3>
                <p>{screeningWorkflowLabel(submission)}</p>
                {submission?.preliminaryResult && <div className="small text-muted mb-3">
                  <p className="mb-1">Preliminary result: {CITIZEN_PRELIMINARY_RESULT_LABELS[submission.preliminaryResult] || 'Unavailable'}.</p>
                  <p>This preliminary result is not a final eligibility decision.</p>
                </div>}
                <ScreeningCorrectionNotice submission={submission} />
                {submission?.status === 'REVIEWED' && !linked && <p>Eligibility decision unavailable</p>}
                {assessment && <div className="border-top pt-3 mt-3">
                  <h3 className="h6">{previous ? 'Previous recorded assessment' : 'Recorded assessment'}</h3>
                  {previous && <p className="small text-muted">This decision belongs to a different screening, not the current screening shown above.</p>}
                  <RecordedEligibilityAssessment assessment={assessment} />
                  {previous && <Link className="small" to={`/app/screening/history/${assessment.submissionId}`}>View assessed screening</Link>}
                </div>}
                <div className="border-top pt-3 mt-3" aria-live="polite">
                  <h3 className="h6">Current eligibility</h3>
                  <p>{currentEligibilityLabel(check, submission, linked)}</p>
                  {assessment?.status === ELIGIBILITY_ASSESSMENT_STATUS.ELIGIBLE && check?.status === 'fulfilled' && check.value?.isEligible === false && <>
                    <p className="small text-muted">The recorded decision remains Eligible. Its validity may have elapsed or the campaign service period may have ended.</p>
                    {campaign?.serviceEndDate && <p className="small">Campaign service end: {campaign.serviceEndDate}</p>}
                  </>}
                </div>
                {submission && <Link className="btn btn-outline-primary" to={`/app/screening/history/${submission.id}`}>View Screening</Link>}
              </div>
            </article>
          </div>
        );
      })}</div>}
    </section>
  );
}

export default function CitizenEligibilityPage() {
  const { user } = useAuth();
  return user?.id ? <EligibilityRecords key={user.id} citizenId={user.id} />
    : <ApiErrorAlert error="Your authenticated citizen profile is missing an account identifier." />;
}
