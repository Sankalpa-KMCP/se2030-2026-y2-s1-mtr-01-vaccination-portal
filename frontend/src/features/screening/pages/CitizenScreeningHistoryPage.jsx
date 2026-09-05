import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../../auth/context/AuthContext';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import LoadingSpinner from '../../../components/common/LoadingSpinner';
import CitizenPageHeader from '../../citizen-ui/components/CitizenPageHeader';
import { CITIZEN_PRELIMINARY_RESULT_LABELS, ELIGIBILITY_ASSESSMENT_STATUS_LABELS, SCREENING_SUBMISSION_STATUS_LABELS } from '../constants/screeningConstants';
import useCitizenScreeningRecords from '../hooks/useCitizenScreeningRecords';
import { assessmentsBySubmission, campaignName, formatScreeningTimestamp } from '../utils/screeningHistory';

function HistoryRecords({ citizenId }) {
  const { loading, submissions, assessments, campaigns, errors } = useCitizenScreeningRecords(citizenId);
  if (loading) return <LoadingSpinner message="Loading your screening history..." />;
  const linkedAssessments = assessmentsBySubmission(assessments);
  const campaignMap = new Map(campaigns.map((campaign) => [String(campaign.id), campaign]));
  return (
    <>
      <ApiErrorAlert error={errors.submissions} />
      {errors.assessments && <p className="alert alert-warning">Final assessment information is unavailable. Your screening history is still shown.</p>}
      {errors.campaigns && <p className="alert alert-warning">Campaign information is unavailable. Campaign references are shown instead.</p>}
      {!errors.submissions && submissions.length === 0 && <div className="card bg-light border-0"><div className="card-body p-4">
        <p>No screening history is available yet.</p><Link className="btn btn-primary" to="/app/screening">Start Screening</Link>
      </div></div>}
      {!errors.submissions && submissions.length > 0 && <div className="table-responsive border rounded" role="region" aria-label="Screening history table" tabIndex={0}>
        <table className="table table-striped align-middle mb-0">
          <caption className="px-3">Recorded decisions apply only to their linked screening. See My Eligibility for current campaign eligibility.</caption>
          <thead><tr>
            <th scope="col">Submitted</th><th scope="col">Campaign</th><th scope="col">Screening status</th>
            <th scope="col">Preliminary result</th><th scope="col">Final recorded assessment</th><th scope="col">Action</th>
          </tr></thead>
          <tbody>{submissions.map((submission) => {
            const assessment = linkedAssessments.get(String(submission.id));
            return <tr key={submission.id}>
              <td>{formatScreeningTimestamp(submission.submittedAt)}</td>
              <th scope="row">{campaignName(campaignMap.get(String(submission.campaignId)), submission.campaignId)}</th>
              <td>{SCREENING_SUBMISSION_STATUS_LABELS[submission.status] || 'Unavailable'}</td>
              <td>{CITIZEN_PRELIMINARY_RESULT_LABELS[submission.preliminaryResult] || 'Not available'}</td>
              <td>{errors.assessments ? 'Unavailable' : assessment ? ELIGIBILITY_ASSESSMENT_STATUS_LABELS[assessment.status] || 'Unavailable' : 'Not yet recorded'}</td>
              <td><Link className="btn btn-sm btn-outline-primary text-nowrap" to={`/app/screening/history/${submission.id}`}>View Details</Link></td>
            </tr>;
          })}</tbody>
        </table>
      </div>}
    </>
  );
}

export default function CitizenScreeningHistoryPage() {
  const { user } = useAuth();
  return (
    <section aria-labelledby="citizen-history-title">
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
