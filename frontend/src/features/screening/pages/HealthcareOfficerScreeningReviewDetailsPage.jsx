import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import LoadingSpinner from '../../../components/common/LoadingSpinner';
import PageHeader from '../../../components/common/PageHeader';
import { getCampaignById } from '../../campaign-management/api/campaignApi';
import {
  getScreeningReviewDetails,
  recordEligibilityDecision,
  requestScreeningCorrections,
  revokeAssessment,
} from '../api/screeningApi';
import {
  ELIGIBILITY_ASSESSMENT_STATUS,
  ELIGIBILITY_ASSESSMENT_STATUS_LABELS,
  PRELIMINARY_SCREENING_RESULT,
  SCREENING_SUBMISSION_STATUS,
  SCREENING_SUBMISSION_STATUS_LABELS,
} from '../constants/screeningConstants';
import { formatScreeningTimestamp } from '../utils/screeningHistory';

const QUEUE_PATH = '/app/staff/screening/reviews';

function answerDisplay(answer) {
  const selectedLabels = (answer.selectedOptions || [])
    .map((option) => option.label)
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
  return result || 'Preliminary result unavailable';
}

function staleError(error) {
  if (error?.status !== 409) return error;
  return Object.assign(
    new Error('This screening was updated by another action. Reload the latest review state.'),
    { status: 409, data: error.data },
  );
}

export default function HealthcareOfficerScreeningReviewDetailsPage() {
  const { submissionId } = useParams();
  const [details, setDetails] = useState(null);
  const [campaign, setCampaign] = useState(null);
  const [campaignWarning, setCampaignWarning] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [mutationError, setMutationError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);
  const [action, setAction] = useState(null);
  const [instructions, setInstructions] = useState('');
  const [reason, setReason] = useState('');
  const [revocationReason, setRevocationReason] = useState('');
  const [validUntil, setValidUntil] = useState('');
  const [formError, setFormError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const loadDetails = useCallback(async ({ silent = false } = {}) => {
    if (!silent) setLoading(true);
    setLoadError(null);
    try {
      const result = await getScreeningReviewDetails(submissionId);
      setDetails(result);
      setCampaign(null);
      setCampaignWarning(null);
      if (result?.campaignId !== undefined && result?.campaignId !== null) {
        try {
          setCampaign(await getCampaignById(result.campaignId));
        } catch {
          setCampaignWarning('Campaign details could not be loaded. The screening review remains available.');
        }
      }
    } catch (error) {
      setLoadError(error);
    } finally {
      if (!silent) setLoading(false);
    }
  }, [submissionId]);

  useEffect(() => {
    loadDetails();
  }, [loadDetails]);

  const answers = useMemo(
    () => [...(details?.answers || [])].sort((left, right) => left.displayOrder - right.displayOrder),
    [details?.answers],
  );
  const ruleEvidence = useMemo(
    () => [...(details?.triggeredRuleEvidence || [])]
      .sort((left, right) => left.evaluationOrder - right.evaluationOrder),
    [details?.triggeredRuleEvidence],
  );
  const canMutate = details?.status === SCREENING_SUBMISSION_STATUS.PENDING_REVIEW;
  const canRevoke = details?.assessment?.status === ELIGIBILITY_ASSESSMENT_STATUS.ELIGIBLE;
  const campaignName = campaign?.name || (details ? `Campaign #${details.campaignId}` : 'Campaign');

  const openAction = (nextAction) => {
    setAction(nextAction);
    setFormError(null);
    setMutationError(null);
    setSuccessMessage(null);
  };

  const handleCorrections = async (event) => {
    event.preventDefault();
    const trimmedInstructions = instructions.trim();
    if (!trimmedInstructions) {
      setFormError('Instructions are required.');
      return;
    }

    setSubmitting(true);
    setFormError(null);
    setMutationError(null);
    try {
      await requestScreeningCorrections(submissionId, trimmedInstructions);
      setSuccessMessage('Corrections requested successfully.');
      setAction(null);
      await loadDetails({ silent: true });
    } catch (error) {
      setMutationError(staleError(error));
      if (error?.status === 409) await loadDetails({ silent: true });
    } finally {
      setSubmitting(false);
    }
  };

  const handleDecision = async (event) => {
    event.preventDefault();
    const trimmedReason = reason.trim();
    if (!trimmedReason) {
      setFormError('Reason is required.');
      return;
    }

    const payload = { status: action, reason: trimmedReason };
    if (action === 'ELIGIBLE') {
      if (!validUntil) {
        setFormError('Valid until is required for an eligible decision.');
        return;
      }
      const validUntilDate = new Date(validUntil);
      if (Number.isNaN(validUntilDate.getTime()) || validUntilDate.getTime() <= Date.now()) {
        setFormError('Valid until must be a future date and time.');
        return;
      }
      payload.validUntil = validUntilDate.toISOString();
    }

    setSubmitting(true);
    setFormError(null);
    setMutationError(null);
    try {
      await recordEligibilityDecision(submissionId, payload);
      setSuccessMessage('Eligibility decision recorded.');
      setAction(null);
      await loadDetails({ silent: true });
    } catch (error) {
      setMutationError(staleError(error));
      if (error?.status === 409) await loadDetails({ silent: true });
    } finally {
      setSubmitting(false);
    }
  };

  const handleRevocation = async (event) => {
    event.preventDefault();
    const trimmedReason = revocationReason.trim();
    if (!trimmedReason) {
      setFormError('Revocation reason is required.');
      return;
    }

    setSubmitting(true);
    setFormError(null);
    setMutationError(null);
    try {
      await revokeAssessment(details.assessment.id, trimmedReason);
      setSuccessMessage('Eligibility revoked successfully.');
      setAction(null);
      setRevocationReason('');
      await loadDetails({ silent: true });
    } catch (error) {
      setMutationError(staleError(error));
      if (error?.status === 409) await loadDetails({ silent: true });
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <LoadingSpinner message="Loading screening review..." />;

  if (loadError || !details) {
    return (
      <section aria-labelledby="screening-review-error-title">
        <PageHeader id="screening-review-error-title" title="Screening Review" eyebrow="Healthcare Officer" />
        <ApiErrorAlert error={loadError || 'The screening review could not be loaded.'} />
        <div className="d-flex flex-wrap gap-2">
          <button type="button" className="btn btn-primary" onClick={() => loadDetails()}>
            Reload
          </button>
          <Link className="btn btn-outline-secondary" to={QUEUE_PATH}>Back to Review Queue</Link>
        </div>
      </section>
    );
  }

  return (
    <section aria-labelledby="screening-review-details-title">
      <PageHeader
        id="screening-review-details-title"
        eyebrow="Healthcare Officer"
        title="Screening Review Details"
        subtitle={`${campaignName} · ${details.citizenFullName || 'Name unavailable'} · Citizen ID: #${details.citizenId}`}
        actions={<Link className="btn btn-outline-primary" to={QUEUE_PATH}>Back to Review Queue</Link>}
      />

      {successMessage && <div className="alert alert-success" role="status">{successMessage}</div>}
      {campaignWarning && <div className="alert alert-warning" role="status">{campaignWarning}</div>}
      {mutationError && (
        <div className="mb-4">
          <ApiErrorAlert error={mutationError} onDismiss={() => setMutationError(null)} />
          {mutationError.status === 409 && (
            <button type="button" className="btn btn-outline-primary" onClick={() => loadDetails()}>
              Reload latest review state
            </button>
          )}
        </div>
      )}

      <div className="card border-0 shadow-sm mb-4">
        <div className="card-body p-4">
          <div className="row g-3">
            <div className="col-md-4">
              <div className="small text-secondary">Citizen</div>
              <div className="fw-semibold">{details.citizenFullName || 'Name unavailable'}</div>
              <div className="small text-secondary">Citizen ID: #{details.citizenId}</div>
            </div>
            <div className="col-md-4">
              <div className="small text-secondary">Campaign</div>
              <div className="fw-semibold">{campaignName}</div>
              {campaign?.code && <div className="small text-secondary">{campaign.code}</div>}
            </div>
            <div className="col-md-4"><div className="small text-secondary">Status</div><div className="fw-semibold">{SCREENING_SUBMISSION_STATUS_LABELS[details.status] || details.status}</div></div>
            <div className="col-md-4"><div className="small text-secondary">Submitted</div><div>{formatScreeningTimestamp(details.submittedAt)}</div></div>
            <div className="col-md-4"><div className="small text-secondary">Questionnaire version</div><div>#{details.questionnaireVersionId}</div></div>
            <div className="col-md-4"><div className="small text-secondary">Submission ID</div><div>#{details.submissionId}</div></div>
          </div>
        </div>
      </div>

      <div className={`alert ${details.preliminaryResult === PRELIMINARY_SCREENING_RESULT.REVIEW_REQUIRED ? 'alert-warning' : 'alert-info'} mb-4`}>
        <strong>{preliminaryCallout(details.preliminaryResult)}</strong>
        <div className="small mt-1">This preliminary result is not a final eligibility decision.</div>
      </div>

      <div className="card border-0 shadow-sm mb-4">
        <div className="card-body p-4">
          <h2 className="h5 mb-3">Questionnaire answers</h2>
          <div className="vstack gap-3">
            {answers.map((answer) => (
              <article className="border rounded p-3" key={answer.id || answer.questionnaireItemId || answer.questionId}>
                <div className="d-flex justify-content-between gap-3">
                  <h3 className="h6 mb-2">{answer.questionText}</h3>
                  {answer.required && <span className="badge bg-secondary-subtle text-secondary-emphasis align-self-start">Required</span>}
                </div>
                <div className="small text-secondary mb-1">Answer</div>
                <div className="fw-semibold text-break">{answerDisplay(answer)}</div>
                {answer.questionCode && <div className="small text-secondary mt-2">Question code: {answer.questionCode}</div>}
              </article>
            ))}
          </div>
        </div>
      </div>

      <div className="card border-0 shadow-sm mb-4">
        <div className="card-body p-4">
          <h2 className="h5 mb-3">Triggered eligibility rules</h2>
          {ruleEvidence.length === 0 ? (
            <p className="text-secondary mb-0">No eligibility rules were triggered by this screening.</p>
          ) : (
            <div className="vstack gap-3">
              {ruleEvidence.map((evidence) => (
                <div className="border-start border-4 border-warning ps-3" key={evidence.id || evidence.ruleCode}>
                  <div className="fw-semibold">{evidence.message}</div>
                  <div className="small text-secondary">
                    {preliminaryCallout(evidence.preliminaryResult)}
                    {evidence.ruleCode ? ` · ${evidence.ruleCode}` : ''}
                  </div>
                </div>
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
            {details.correctionRequestedAt && <div className="small text-secondary">Requested {formatScreeningTimestamp(details.correctionRequestedAt)}</div>}
          </div>
        </div>
      )}

      {details.assessment && (
        <div className="card border-success-subtle shadow-sm mb-4">
          <div className="card-body p-4">
            <h2 className="h5 mb-3">Recorded final decision</h2>
            <dl className="row mb-0">
              <dt className="col-sm-3">Final status</dt>
              <dd className="col-sm-9">{ELIGIBILITY_ASSESSMENT_STATUS_LABELS[details.assessment.status] || details.assessment.status}</dd>
              <dt className="col-sm-3">Reason</dt>
              <dd className="col-sm-9">{details.assessment.reason}</dd>
              <dt className="col-sm-3">Reviewed</dt>
              <dd className="col-sm-9">{formatScreeningTimestamp(details.assessment.reviewedAt)}</dd>
              <dt className="col-sm-3">Valid until</dt>
              <dd className="col-sm-9">{formatScreeningTimestamp(details.assessment.validUntil)}</dd>
              <dt className="col-sm-3">Reviewer</dt>
              <dd className="col-sm-9">Reviewer #{details.assessment.reviewerId}</dd>
              {details.assessment.revokedAt && (
                <>
                  <dt className="col-sm-3">Revoked</dt>
                  <dd className="col-sm-9">{formatScreeningTimestamp(details.assessment.revokedAt)}</dd>
                </>
              )}
              {details.assessment.revocationReason && (
                <>
                  <dt className="col-sm-3">Revocation reason</dt>
                  <dd className="col-sm-9 text-break">{details.assessment.revocationReason}</dd>
                </>
              )}
            </dl>
            {canRevoke && (
              <div className="border-top mt-3 pt-3">
                <button
                  type="button"
                  className="btn btn-outline-danger"
                  disabled={submitting}
                  onClick={() => openAction('REVOKE')}
                >
                  Revoke Eligibility
                </button>
                {action === 'REVOKE' && (
                  <form className="mt-3" onSubmit={handleRevocation}>
                    {formError && <div className="alert alert-danger" role="alert">{formError}</div>}
                    <label className="form-label fw-semibold" htmlFor="revocation-reason">Revocation reason</label>
                    <textarea
                      id="revocation-reason"
                      className="form-control mb-3"
                      rows="4"
                      value={revocationReason}
                      onChange={(event) => setRevocationReason(event.target.value)}
                      disabled={submitting}
                    />
                    <button type="submit" className="btn btn-danger" disabled={submitting}>
                      {submitting ? 'Revoking...' : 'Confirm Revocation'}
                    </button>
                  </form>
                )}
              </div>
            )}
          </div>
        </div>
      )}

      {canMutate && (
        <div className="card border-primary-subtle shadow-sm">
          <div className="card-body p-4">
            <h2 className="h5 mb-3">Review action</h2>
            <div className="d-flex flex-wrap gap-2 mb-3">
              <button type="button" className="btn btn-outline-warning" disabled={submitting} onClick={() => openAction('CORRECTIONS')}>
                Request Corrections
              </button>
              <button type="button" className="btn btn-success" disabled={submitting} onClick={() => openAction('ELIGIBLE')}>
                Mark Eligible
              </button>
              <button type="button" className="btn btn-outline-danger" disabled={submitting} onClick={() => openAction('INELIGIBLE')}>
                Mark Ineligible
              </button>
            </div>

            {formError && <div className="alert alert-danger" role="alert">{formError}</div>}

            {action === 'CORRECTIONS' && (
              <form onSubmit={handleCorrections}>
                <label className="form-label fw-semibold" htmlFor="correction-instructions">Instructions</label>
                <textarea
                  id="correction-instructions"
                  className="form-control mb-3"
                  rows="4"
                  value={instructions}
                  onChange={(event) => setInstructions(event.target.value)}
                  disabled={submitting}
                />
                <button type="submit" className="btn btn-warning" disabled={submitting}>
                  {submitting ? 'Submitting...' : 'Send Correction Request'}
                </button>
              </form>
            )}

            {(action === 'ELIGIBLE' || action === 'INELIGIBLE') && (
              <form onSubmit={handleDecision}>
                <div className="mb-3">
                  <label className="form-label fw-semibold" htmlFor="decision-reason">Reason</label>
                  <textarea
                    id="decision-reason"
                    className="form-control"
                    rows="4"
                    value={reason}
                    onChange={(event) => setReason(event.target.value)}
                    disabled={submitting}
                  />
                </div>
                {action === 'ELIGIBLE' && (
                  <div className="mb-3">
                    <label className="form-label fw-semibold" htmlFor="decision-valid-until">Valid until</label>
                    <input
                      id="decision-valid-until"
                      className="form-control"
                      type="datetime-local"
                      value={validUntil}
                      onChange={(event) => setValidUntil(event.target.value)}
                      disabled={submitting}
                    />
                    <div className="form-text">Eligibility must remain valid until a future date and time.</div>
                  </div>
                )}
                <button type="submit" className="btn btn-primary" disabled={submitting}>
                  {submitting ? 'Recording...' : `Confirm ${action === 'ELIGIBLE' ? 'Eligible' : 'Ineligible'}`}
                </button>
              </form>
            )}
          </div>
        </div>
      )}
    </section>
  );
}
