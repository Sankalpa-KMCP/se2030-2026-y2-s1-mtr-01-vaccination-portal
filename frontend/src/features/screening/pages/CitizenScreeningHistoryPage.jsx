import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../../auth/context/AuthContext';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import CitizenPageHeader from '../../citizen-ui/components/CitizenPageHeader';
import CitizenSection from '../../citizen-ui/components/CitizenSection';
import CitizenStatePanel from '../../citizen-ui/components/CitizenStatePanel';
import CitizenStatusBadge from '../../citizen-ui/components/CitizenStatusBadge';
import { CITIZEN_PRELIMINARY_RESULT_LABELS, ELIGIBILITY_ASSESSMENT_STATUS_LABELS, SCREENING_SUBMISSION_STATUS_LABELS } from '../constants/screeningConstants';
import useCitizenScreeningRecords from '../hooks/useCitizenScreeningRecords';
import { assessmentsBySubmission, campaignName, formatScreeningTimestamp } from '../utils/screeningHistory';

function screeningTone(status) {
  if (status === 'CORRECTIONS_REQUIRED') return 'attention';
  if (status === 'REVIEWED') return 'positive';
  if (status === 'PENDING_REVIEW') return 'pending';
  return 'neutral';
}

function HistoryRecords({ citizenId }) {
  const { loading, submissions, assessments, campaigns, errors } = useCitizenScreeningRecords(citizenId);
  if (loading) return <CitizenStatePanel title="Loading your screening history..." tone="loading" status><p>Retrieving your recorded submissions and assessments.</p></CitizenStatePanel>;
  const linkedAssessments = assessmentsBySubmission(assessments);
  const campaignMap = new Map(campaigns.map((campaign) => [String(campaign.id), campaign]));
  return (
    <>
      {errors.submissions && <div className="citizen-page__notice"><ApiErrorAlert error={errors.submissions} /></div>}
      {errors.assessments && <CitizenStatePanel title="Final assessment information is unavailable" tone="warning"><p>Your screening history is still shown.</p></CitizenStatePanel>}
      {errors.campaigns && <CitizenStatePanel title="Campaign information is unavailable" tone="warning"><p>Campaign references are shown instead.</p></CitizenStatePanel>}
      {!errors.submissions && submissions.length === 0 && (
        <CitizenStatePanel
          title="No screening history is available yet."
          actions={<Link className="citizen-action citizen-action--primary" to="/app/screening">Start Screening</Link>}
        >
          <p>Your completed submissions will appear here in recorded order.</p>
        </CitizenStatePanel>
      )}
      {!errors.submissions && submissions.length > 0 && <CitizenSection
        eyebrow="Recorded submissions"
        title="Screening record timeline"
        titleId="screening-record-list-title"
        description="Recorded decisions apply only to their linked screening. See My Eligibility for current campaign eligibility."
        aside={<CitizenStatusBadge tone="info">{submissions.length} record{submissions.length === 1 ? '' : 's'}</CitizenStatusBadge>}
      >
        <ol className="citizen-record-list" aria-label="Screening history records">
          {submissions.map((submission) => {
            const assessment = linkedAssessments.get(String(submission.id));
            const screeningLabel = SCREENING_SUBMISSION_STATUS_LABELS[submission.status] || 'Unavailable';
            const preliminaryLabel = CITIZEN_PRELIMINARY_RESULT_LABELS[submission.preliminaryResult] || 'Not available';
            const assessmentLabel = errors.assessments ? 'Unavailable' : assessment
              ? ELIGIBILITY_ASSESSMENT_STATUS_LABELS[assessment.status] || 'Unavailable'
              : 'Not yet recorded';
            return <li key={submission.id}>
              <article className="citizen-record-card" aria-labelledby={`history-record-${submission.id}`}>
                <div className="citizen-record-card__identity">
                  <p className="citizen-ui-eyebrow">Screening #{submission.id}</p>
                  <h3 id={`history-record-${submission.id}`}>{campaignName(campaignMap.get(String(submission.campaignId)), submission.campaignId)}</h3>
                  <p>Submitted {formatScreeningTimestamp(submission.submittedAt)}</p>
                  <CitizenStatusBadge tone={screeningTone(submission.status)}>{screeningLabel}</CitizenStatusBadge>
                </div>
                <div className="citizen-record-card__facts">
                  <div className="citizen-record-card__fact"><span>Preliminary result</span><strong>{preliminaryLabel}</strong></div>
                  <div className="citizen-record-card__fact"><span>Final recorded assessment</span><strong>{assessmentLabel}</strong></div>
                  <div className="citizen-record-card__fact"><span>Campaign reference</span><strong>#{submission.campaignId}</strong></div>
                </div>
                <div className="citizen-record-card__action">
                  <Link className="citizen-action citizen-action--secondary" to={`/app/screening/history/${submission.id}`}>View Details</Link>
                </div>
              </article>
            </li>;
          })}
        </ol>
      </CitizenSection>}
    </>
  );
}

export default function CitizenScreeningHistoryPage() {
  const { user } = useAuth();
  return (
    <section className="citizen-page" aria-labelledby="citizen-history-title">
      <CitizenPageHeader
        eyebrow="Screening & Eligibility"
        title="Screening History"
        titleId="citizen-history-title"
        subtitle="Your recorded screening submissions, preliminary results, and final eligibility assessments."
        backTo="/app/citizen"
        backLabel="Back to Dashboard"
        actions={<Link to="/app/screening/eligibility">View My Eligibility</Link>}
      />
      {user?.id ? <HistoryRecords key={user.id} citizenId={user.id} />
        : <ApiErrorAlert error="Your authenticated citizen profile is missing an account identifier." />}
    </section>
  );
}
