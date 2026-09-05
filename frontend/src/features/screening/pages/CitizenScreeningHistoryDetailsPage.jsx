import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useAuth } from '../../../auth/context/AuthContext';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import CitizenPageHeader from '../../citizen-ui/components/CitizenPageHeader';
import CitizenStatePanel from '../../citizen-ui/components/CitizenStatePanel';
import CitizenStatusBadge from '../../citizen-ui/components/CitizenStatusBadge';
import { getCampaignById } from '../../campaign-management/api/campaignApi';
import { getEligibilityAssessmentBySubmission, getScreeningHistoryDetails } from '../api/screeningApi';
import RecordedEligibilityAssessment from '../components/RecordedEligibilityAssessment';
import ScreeningCorrectionNotice from '../components/ScreeningCorrectionNotice';
import { CITIZEN_PRELIMINARY_RESULT_LABELS, SCREENING_SUBMISSION_STATUS_LABELS } from '../constants/screeningConstants';
import { campaignName, formatScreeningTimestamp } from '../utils/screeningHistory';

function screeningTone(status) {
  if (status === 'CORRECTIONS_REQUIRED') return 'attention';
  if (status === 'REVIEWED') return 'positive';
  if (status === 'PENDING_REVIEW') return 'pending';
  return 'neutral';
}

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

  const { details } = state;
  return (
    <section className="citizen-page" aria-labelledby="screening-history-details-title">
      <CitizenPageHeader
        eyebrow="Screening Record"
        title="Screening Details"
        titleId="screening-history-details-title"
        subtitle="Detailed view of your submitted questionnaire responses and evaluation status."
        backTo="/app/screening/history"
        backLabel="Back to Screening History"
        meta={details?.id ? `Submission reference ${details.id}` : undefined}
      />
      {state.loading && <CitizenStatePanel title="Loading screening details..." tone="loading" status><p>Retrieving the selected questionnaire record and its assessment.</p></CitizenStatePanel>}
      {!state.loading && state.error && <div className="citizen-page__notice"><ApiErrorAlert error={state.error} /></div>}
      {!state.loading && details && <>
        {supplement.errors.campaign && <CitizenStatePanel title="Campaign information is unavailable" tone="warning"><p>Your screening details remain available.</p></CitizenStatePanel>}
        <section className="citizen-detail-panel" aria-labelledby="record-summary-title">
          <header className="citizen-detail-panel__header citizen-campaign-card__heading">
            <div>
              <p className="citizen-ui-eyebrow">Record summary</p>
              <h2 id="record-summary-title">{campaignName(supplement.campaign, details.campaignId)}</h2>
              {supplement.campaign?.code && <p>Campaign code: {supplement.campaign.code}</p>}
            </div>
            <CitizenStatusBadge tone={screeningTone(details.status)}>
              {SCREENING_SUBMISSION_STATUS_LABELS[details.status] || 'Unavailable'}
            </CitizenStatusBadge>
          </header>
          <div className="citizen-detail-panel__body">
          <dl className="citizen-detail-grid">
            <div><dt>Submitted</dt><dd>{formatScreeningTimestamp(details.submittedAt)}</dd></div>
            <div><dt>Screening status</dt><dd>{SCREENING_SUBMISSION_STATUS_LABELS[details.status] || 'Unavailable'}</dd></div>
            <div><dt>Preliminary result</dt><dd>{CITIZEN_PRELIMINARY_RESULT_LABELS[details.preliminaryResult] || 'Not available'}</dd></div>
            <div><dt>Questionnaire version</dt><dd>{details.questionnaireVersionNumber ?? 'Not available'}</dd></div>
          </dl>
          <p className="small text-muted mt-3 mb-0">A preliminary result is not a final eligibility decision.</p>
          <ScreeningCorrectionNotice submission={details} />
          </div>
        </section>
        <section className="citizen-detail-panel" aria-labelledby="submitted-answers-title">
          <header className="citizen-detail-panel__header">
            <p className="citizen-ui-eyebrow">Questionnaire record</p>
            <h2 id="submitted-answers-title">Submitted answers</h2>
            <p>Responses appear in their recorded questionnaire order.</p>
          </header>
          <div className="citizen-detail-panel__body">
          {details.answers.length === 0 && <p>No answers are available for this screening.</p>}
          <ol className="citizen-answer-list">{details.answers.map((answer) => <li key={answer.id ?? answer.questionId}>
            <h3>{answer.questionText || 'Question text unavailable'}</h3>
            <p>{answer.displayAnswer ?? 'Answer unavailable'}</p>
          </li>)}</ol>
          </div>
        </section>
        <section className="citizen-detail-panel" aria-labelledby="recorded-assessment-title">
          <header className="citizen-detail-panel__header">
            <p className="citizen-ui-eyebrow">Eligibility record</p>
            <h2 id="recorded-assessment-title">Final recorded assessment</h2>
          </header>
          <div className="citizen-detail-panel__body">
          {supplement.loading ? <CitizenStatePanel title="Loading assessment information..." tone="loading" status />
            : supplement.errors.assessment ? <><p>Final assessment information is unavailable.</p><ApiErrorAlert error={supplement.errors.assessment} /></>
              : supplement.assessment ? <RecordedEligibilityAssessment assessment={supplement.assessment} />
                : <p>No final eligibility assessment has been recorded for this screening yet.</p>}
          </div>
        </section>
      </>}
    </section>
  );
}

export default function CitizenScreeningHistoryDetailsPage() {
  const { submissionId } = useParams();
  const { user } = useAuth();
  return <HistoryDetails key={`${user?.id}:${submissionId}`} submissionId={submissionId} />;
}
