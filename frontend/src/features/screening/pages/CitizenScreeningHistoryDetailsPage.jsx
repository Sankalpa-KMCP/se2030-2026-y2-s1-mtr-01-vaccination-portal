import React, { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useAuth } from '../../../auth/context/AuthContext';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import LoadingSpinner from '../../../components/common/LoadingSpinner';
import { getCampaignById } from '../../campaign-management/api/campaignApi';
import { getEligibilityAssessmentBySubmission, getScreeningHistoryDetails } from '../api/screeningApi';
import RecordedEligibilityAssessment from '../components/RecordedEligibilityAssessment';
import ScreeningCorrectionNotice from '../components/ScreeningCorrectionNotice';
import {
  CITIZEN_PRELIMINARY_RESULT_LABELS,
  SCREENING_SUBMISSION_STATUS,
  SCREENING_SUBMISSION_STATUS_LABELS,
} from '../constants/screeningConstants';
import { campaignName, formatScreeningTimestamp } from '../utils/screeningHistory';

function HistoryDetails({ submissionId }) {
  const [state, setState] = useState({ loading: true, details: null, error: null });
  const [supplement, setSupplement] = useState({ campaign: null, assessment: null, loading: true, errors: {} });
  useEffect(() => {
    let active = true;
    async function load() {
      try {
        const details = await getScreeningHistoryDetails(submissionId);
        if (!active) return;
        if (!details) throw new Error('Screening details are unavailable.');
        setState({ loading: false, details, error: null });
        const results = await Promise.allSettled([
          getCampaignById(details.campaignId), getEligibilityAssessmentBySubmission(submissionId),
        ]);
        if (!active) return;
        const next = { loading: false, errors: {} };
        ['campaign', 'assessment'].forEach((key, index) => {
          next[key] = results[index].status === 'fulfilled' ? results[index].value : null;
          if (results[index].status === 'rejected') next.errors[key] = results[index].reason;
        });
        setSupplement(next);
      } catch (error) {
        if (active) setState({ loading: false, details: null, error });
      }
    }
    load();
    return () => { active = false; };
  }, [submissionId]);

  if (state.loading) return <LoadingSpinner message="Loading screening details..." />;
  const { details } = state;
  return (
    <section aria-labelledby="screening-history-details-title">
      <Link className="btn btn-link px-0 mb-3" to="/app/screening/history">← Back to Screening History</Link>
      <h1 className="display-6 fw-semibold mb-4" id="screening-history-details-title">Screening Details</h1>
      <ApiErrorAlert error={state.error} />
      {details && <>
        {supplement.errors.campaign && <p className="alert alert-warning">Campaign information is unavailable. Your screening details remain available.</p>}
        <div className="card border-0 shadow-sm mb-4"><div className="card-body p-4">
          <h2 className="h4">{campaignName(supplement.campaign, details.campaignId)}</h2>
          {supplement.campaign?.code && <p className="small text-muted">Campaign code: {supplement.campaign.code}</p>}
          <dl className="row mb-0">
            <dt className="col-sm-4">Submitted</dt><dd className="col-sm-8">{formatScreeningTimestamp(details.submittedAt)}</dd>
            <dt className="col-sm-4">Screening status</dt><dd className="col-sm-8">{SCREENING_SUBMISSION_STATUS_LABELS[details.status] || 'Unavailable'}</dd>
            <dt className="col-sm-4">Preliminary result</dt><dd className="col-sm-8">{CITIZEN_PRELIMINARY_RESULT_LABELS[details.preliminaryResult] || 'Not available'}</dd>
            <dt className="col-sm-4">Questionnaire version</dt><dd className="col-sm-8">{details.questionnaireVersionNumber ?? 'Not available'}</dd>
          </dl>
          <p className="small text-muted mt-2 mb-0">A preliminary result is not a final eligibility decision.</p>
          <ScreeningCorrectionNotice submission={details} />
          {details.status === SCREENING_SUBMISSION_STATUS.CORRECTIONS_REQUIRED && (
            <Link
              className="btn btn-primary mt-3"
              to={`/app/screening/history/${details.id ?? submissionId}/correct`}
            >
              Correct Screening
            </Link>
          )}
        </div></div>
        <div className="card border-0 shadow-sm mb-4"><div className="card-body p-4">
          <h2 className="h4 mb-3">Submitted answers</h2>
          {details.answers.length === 0 && <p>No answers are available for this screening.</p>}
          <ol className="list-unstyled mb-0">{details.answers.map((answer) => <li className="border-top py-3 text-break" key={answer.id ?? answer.questionId}>
            <h3 className="h6">{answer.questionText || 'Question text unavailable'}</h3>
            <p className="mb-0" style={{ whiteSpace: 'pre-wrap' }}>{answer.displayAnswer ?? 'Answer unavailable'}</p>
          </li>)}</ol>
        </div></div>
        <div className="card border-0 shadow-sm"><div className="card-body p-4">
          <h2 className="h4 mb-3">Final recorded assessment</h2>
          {supplement.loading ? <LoadingSpinner message="Loading assessment information..." />
            : supplement.errors.assessment ? <><p>Final assessment information is unavailable.</p><ApiErrorAlert error={supplement.errors.assessment} /></>
              : supplement.assessment ? <RecordedEligibilityAssessment assessment={supplement.assessment} />
                : <p>No final eligibility assessment has been recorded for this screening yet.</p>}
        </div></div>
      </>}
    </section>
  );
}

export default function CitizenScreeningHistoryDetailsPage() {
  const { submissionId } = useParams();
  const { user } = useAuth();
  return <HistoryDetails key={`${user?.id}:${submissionId}`} submissionId={submissionId} />;
}
