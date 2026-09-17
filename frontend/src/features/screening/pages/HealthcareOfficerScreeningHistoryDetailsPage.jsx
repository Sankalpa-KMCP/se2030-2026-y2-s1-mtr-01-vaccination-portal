import React, { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import LoadingSpinner from '../../../components/common/LoadingSpinner';
import PageHeader from '../../../components/common/PageHeader';
import { getCampaignById } from '../../campaign-management/api/campaignApi';
import {
  getEligibilityAssessmentBySubmission,
  getScreeningHistoryDetails,
} from '../api/screeningApi';
import {
  CITIZEN_PRELIMINARY_RESULT_LABELS,
  ELIGIBILITY_ASSESSMENT_STATUS_LABELS,
  PRELIMINARY_SCREENING_RESULT,
  SCREENING_SUBMISSION_STATUS_LABELS,
} from '../constants/screeningConstants';
import { campaignName, formatScreeningTimestamp } from '../utils/screeningHistory';

const QUEUE_PATH = '/app/staff/screening/reviews';

function answerDisplay(answer) {
  if (answer.displayAnswer) return answer.displayAnswer;

  const selectedLabels = (answer.selectedOptions || [])
    .map((option) => option.label || option.code)
    .filter(Boolean);
  if (selectedLabels.length > 0) return selectedLabels.join(', ');

  const raw = answer.typedAnswer;
  if (answer.questionType === 'BOOLEAN' && typeof raw === 'string') {
    if (raw.toLowerCase() === 'true') return 'Yes';
    if (raw.toLowerCase() === 'false') return 'No';
  }
  return raw === undefined || raw === null || raw === '' ? 'No answer recorded' : String(raw);
}

function preliminaryCallout(result) {
  if (result === PRELIMINARY_SCREENING_RESULT.PASS) {
    return 'Preliminary pass — final healthcare review pending';
  }
  if (result === PRELIMINARY_SCREENING_RESULT.REVIEW_REQUIRED) {
    return 'Healthcare review required';
  }
  return CITIZEN_PRELIMINARY_RESULT_LABELS[result] || result || 'Preliminary result unavailable';
}

export default function HealthcareOfficerScreeningHistoryDetailsPage() {
  const { submissionId } = useParams();
  const [details, setDetails] = useState(null);
  const [campaign, setCampaign] = useState(null);
  const [assessment, setAssessment] = useState(null);
  const [campaignWarning, setCampaignWarning] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);

  useEffect(() => {
    let active = true;
    async function loadData() {
      setLoading(true);
      setLoadError(null);
      setCampaignWarning(null);
      try {
        const [historyResult, assessmentResult] = await Promise.allSettled([
          getScreeningHistoryDetails(submissionId),
          getEligibilityAssessmentBySubmission(submissionId),
        ]);

        if (!active) return;

        if (historyResult.status === 'rejected') {
          throw historyResult.reason;
        }

        const submissionData = historyResult.value;
        if (!submissionData) {
          throw new Error('Screening details are unavailable.');
        }

        setDetails(submissionData);
        setAssessment(assessmentResult.status === 'fulfilled' ? assessmentResult.value : null);

        if (submissionData.campaignId !== undefined && submissionData.campaignId !== null) {
          try {
            const campaignData = await getCampaignById(submissionData.campaignId);
            if (active) setCampaign(campaignData);
          } catch {
            if (active) {
              setCampaignWarning('Campaign details could not be loaded. The screening history remains available.');
            }
          }
        }
      } catch (error) {
        if (active) setLoadError(error);
      } finally {
        if (active) setLoading(false);
      }
    }

    loadData();
    return () => { active = false; };
  }, [submissionId]);

  const answers = useMemo(
    () => [...(details?.answers || [])].sort((left, right) => (left.displayOrder ?? 0) - (right.displayOrder ?? 0)),
    [details?.answers],
  );

  const displayedCampaignName = campaignName(campaign, details?.campaignId);

  if (loading) return <LoadingSpinner message="Loading screening history details..." />;

  if (loadError || !details) {
    return (
      <section aria-labelledby="screening-history-error-title">
        <PageHeader
          id="screening-history-error-title"
          title="Previous Screening Details"
          eyebrow="Healthcare Officer"
        />
        <ApiErrorAlert error={loadError || 'The screening history could not be loaded.'} />
        <div className="d-flex flex-wrap gap-2">
          <Link className="btn btn-outline-secondary" to={QUEUE_PATH}>
            Back to Review Queue
          </Link>
        </div>
      </section>
    );
  }

  return (
    <section aria-labelledby="screening-history-details-title">
      <PageHeader
        id="screening-history-details-title"
        eyebrow="Healthcare Officer"
        title="Previous Screening Details"
        subtitle={`${displayedCampaignName} · Citizen ID: #${details.citizenId} · Submission ID: #${details.id ?? submissionId}`}
        actions={(
          <Link className="btn btn-outline-primary" to={QUEUE_PATH}>
            Back to Review Queue
          </Link>
        )}
      />

      {campaignWarning && <div className="alert alert-warning" role="status">{campaignWarning}</div>}

      <div className="card border-0 shadow-sm mb-4">
        <div className="card-body p-4">
          <div className="row g-3">
            <div className="col-md-4">
              <div className="small text-secondary">Citizen ID</div>
              <div className="fw-semibold">#{details.citizenId}</div>
            </div>
            <div className="col-md-4">
              <div className="small text-secondary">Campaign</div>
              <div className="fw-semibold">{displayedCampaignName}</div>
              {campaign?.code && <div className="small text-secondary">{campaign.code}</div>}
            </div>
            <div className="col-md-4">
              <div className="small text-secondary">Screening status</div>
              <div className="fw-semibold">
                {SCREENING_SUBMISSION_STATUS_LABELS[details.status] || details.status}
              </div>
            </div>
            <div className="col-md-4">
              <div className="small text-secondary">Submitted</div>
              <div>{formatScreeningTimestamp(details.submittedAt || details.createdAt)}</div>
            </div>
            <div className="col-md-4">
              <div className="small text-secondary">Questionnaire version</div>
              <div>#{details.questionnaireVersionNumber ?? details.questionnaireVersionId ?? 'N/A'}</div>
            </div>
            <div className="col-md-4">
              <div className="small text-secondary">Submission ID</div>
              <div>#{details.id ?? submissionId}</div>
            </div>
          </div>
        </div>
      </div>

      <div className={`alert ${details.preliminaryResult === PRELIMINARY_SCREENING_RESULT.REVIEW_REQUIRED ? 'alert-warning' : 'alert-info'} mb-4`}>
        <strong>{preliminaryCallout(details.preliminaryResult)}</strong>
        <div className="small mt-1">This preliminary result is not a final eligibility decision.</div>
      </div>

      <div className="card border-0 shadow-sm mb-4">
        <div className="card-body p-4">
          <h2 className="h5 mb-3">Submitted questionnaire answers</h2>
          {answers.length === 0 ? (
            <p className="text-secondary mb-0">No answers recorded for this screening.</p>
          ) : (
            <div className="vstack gap-3">
              {answers.map((answer) => (
                <article className="border rounded p-3" key={answer.id || answer.questionId}>
                  <div className="d-flex justify-content-between gap-3">
                    <h3 className="h6 mb-2">{answer.questionText || 'Question text unavailable'}</h3>
                  </div>
                  <div className="small text-secondary mb-1">Answer</div>
                  <div className="fw-semibold text-break">{answerDisplay(answer)}</div>
                  {answer.questionCode && (
                    <div className="small text-secondary mt-2">Question code: {answer.questionCode}</div>
                  )}
                </article>
              ))}
            </div>
          )}
        </div>
      </div>

      {(details.correctionInstructions || details.correctionRequestedAt) && (
        <div className="card border-warning-subtle shadow-sm mb-4">
          <div className="card-body p-4">
            <h2 className="h5 mb-3">Previous correction request</h2>
            {details.correctionInstructions && <p className="mb-2">{details.correctionInstructions}</p>}
            {details.correctionRequestedAt && (
              <div className="small text-secondary">
                Requested {formatScreeningTimestamp(details.correctionRequestedAt)}
              </div>
            )}
          </div>
        </div>
      )}

      <div className="card border-0 shadow-sm mb-4">
        <div className="card-body p-4">
          <h2 className="h5 mb-3">Recorded final eligibility assessment</h2>
          {assessment ? (
            <dl className="row mb-0">
              <dt className="col-sm-3">Final status</dt>
              <dd className="col-sm-9">
                <span className="badge text-bg-light border text-dark">
                  {ELIGIBILITY_ASSESSMENT_STATUS_LABELS[assessment.status] || assessment.status}
                </span>
              </dd>
              <dt className="col-sm-3">Reason</dt>
              <dd className="col-sm-9 text-break">{assessment.reason || 'None recorded'}</dd>
              <dt className="col-sm-3">Reviewed</dt>
              <dd className="col-sm-9">{formatScreeningTimestamp(assessment.reviewedAt)}</dd>
              {assessment.validUntil && (
                <>
                  <dt className="col-sm-3">Valid until</dt>
                  <dd className="col-sm-9">{formatScreeningTimestamp(assessment.validUntil)}</dd>
                </>
              )}
              {assessment.reviewerId && (
                <>
                  <dt className="col-sm-3">Reviewer</dt>
                  <dd className="col-sm-9">Reviewer #{assessment.reviewerId}</dd>
                </>
              )}
              {assessment.revokedAt && (
                <>
                  <dt className="col-sm-3">Revoked on</dt>
                  <dd className="col-sm-9">{formatScreeningTimestamp(assessment.revokedAt)}</dd>
                </>
              )}
              {assessment.revocationReason && (
                <>
                  <dt className="col-sm-3">Revocation reason</dt>
                  <dd className="col-sm-9 text-break">{assessment.revocationReason}</dd>
                </>
              )}
              {assessment.rescreeningAuthorizedAt && (
                <>
                  <dt className="col-sm-3">Rescreening authorized</dt>
                  <dd className="col-sm-9">{formatScreeningTimestamp(assessment.rescreeningAuthorizedAt)}</dd>
                  {assessment.rescreeningReason && (
                    <>
                      <dt className="col-sm-3">Rescreening reason</dt>
                      <dd className="col-sm-9 text-break">{assessment.rescreeningReason}</dd>
                    </>
                  )}
                </>
              )}
            </dl>
          ) : (
            <p className="text-secondary mb-0">No final eligibility assessment has been recorded for this screening.</p>
          )}
        </div>
      </div>
    </section>
  );
}
