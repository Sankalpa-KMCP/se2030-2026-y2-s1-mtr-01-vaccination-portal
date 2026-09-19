import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import LoadingSpinner from '../../../components/common/LoadingSpinner';
import PageHeader from '../../../components/common/PageHeader';
import { getCampaignById, listCampaigns } from '../../campaign-management/api/campaignApi';
import {
  getScreeningReviewDetails,
  listCitizenEligibilityAssessments,
  listCitizenScreeningSubmissions,
  recordEligibilityDecision,
  requestScreeningCorrections,
  revokeAssessment,
} from '../api/screeningApi';
import {
  CITIZEN_PRELIMINARY_RESULT_LABELS,
  ELIGIBILITY_ASSESSMENT_STATUS,
  ELIGIBILITY_ASSESSMENT_STATUS_LABELS,
  PRELIMINARY_SCREENING_RESULT,
  PRELIMINARY_SCREENING_RESULT_LABELS,
  SCREENING_SUBMISSION_STATUS,
  SCREENING_SUBMISSION_STATUS_LABELS,
} from '../constants/screeningConstants';
import { campaignName, formatScreeningTimestamp } from '../utils/screeningHistory';

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

function sortNewestFirst(submissions) {
  return [...submissions].sort((left, right) => {
    const leftTime = Date.parse(left.submittedAt || left.createdAt) || 0;
    const rightTime = Date.parse(right.submittedAt || right.createdAt) || 0;
    const byTime = rightTime - leftTime;
    return byTime || Number(right.id || 0) - Number(left.id || 0);
  });
}

export default function HealthcareOfficerScreeningReviewDetailsPage() {
  const { submissionId } = useParams();
  const [details, setDetails] = useState(null);
  const [campaign, setCampaign] = useState(null);
  const [campaignWarning, setCampaignWarning] = useState(null);
  const [previousSubmissions, setPreviousSubmissions] = useState([]);
  const [citizenAssessments, setCitizenAssessments] = useState([]);
  const [allCampaigns, setAllCampaigns] = useState([]);
  const [historyError, setHistoryError] = useState(null);
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

      const citizenId = result?.citizenId;
      const campaignId = result?.campaignId;

      const enrichmentPromises = [];

      if (campaignId !== undefined && campaignId !== null) {
        enrichmentPromises.push(
          getCampaignById(campaignId)
            .then((camp) => ({ type: 'campaign', value: camp }))
            .catch((err) => ({ type: 'campaign', error: err })),
        );
      }

      if (citizenId !== undefined && citizenId !== null) {
        enrichmentPromises.push(
          listCitizenScreeningSubmissions(citizenId)
            .then((subs) => ({ type: 'submissions', value: subs }))
            .catch((err) => ({ type: 'submissions', error: err })),
        );
        enrichmentPromises.push(
          listCitizenEligibilityAssessments(citizenId)
            .then((assess) => ({ type: 'assessments', value: assess }))
            .catch((err) => ({ type: 'assessments', error: err })),
        );
        enrichmentPromises.push(
          listCampaigns()
            .then((camps) => ({ type: 'campaigns', value: camps }))
            .catch((err) => ({ type: 'campaigns', error: err })),
        );
      }

      const enrichmentResults = await Promise.all(enrichmentPromises);

      let prevSubs = [];
      let assessList = [];
      let campsList = [];
      let histErr = null;

      for (const res of enrichmentResults) {
        if (res.type === 'campaign') {
          if (res.error) {
            setCampaignWarning('Campaign details could not be loaded. The screening review remains available.');
          } else {
            setCampaign(res.value);
          }
        } else if (res.type === 'submissions') {
          if (res.error) {
            histErr = 'Previous screening history could not be loaded for this citizen.';
          } else if (Array.isArray(res.value)) {
            prevSubs = res.value.filter((sub) => String(sub.id) !== String(submissionId));
          }
        } else if (res.type === 'assessments') {
          if (Array.isArray(res.value)) {
            assessList = res.value;
          }
        } else if (res.type === 'campaigns') {
          if (Array.isArray(res.value)) {
            campsList = res.value;
          }
        }
      }

      setPreviousSubmissions(sortNewestFirst(prevSubs));
      setCitizenAssessments(assessList);
      setAllCampaigns(campsList);
      setHistoryError(histErr);
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
  const campaignsById = useMemo(
    () => new Map(allCampaigns.map((c) => [String(c.id), c])),
    [allCampaigns],
  );
  const assessmentsBySubId = useMemo(() => {
    const map = new Map();
    citizenAssessments.forEach((assessment) => {
      if (assessment?.submissionId !== undefined && assessment?.submissionId !== null) {
        map.set(String(assessment.submissionId), assessment);
      }
    });
    return map;
  }, [citizenAssessments]);

  const canMutate = details?.status === SCREENING_SUBMISSION_STATUS.PENDING_REVIEW;
  const canRevoke = details?.assessment?.status === ELIGIBILITY_ASSESSMENT_STATUS.ELIGIBLE;
  const displayedCampaignName = campaign?.name || (details ? `Campaign #${details.campaignId}` : 'Campaign');

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
        subtitle={`${displayedCampaignName} · ${details.citizenFullName || 'Name unavailable'} · Citizen ID: #${details.citizenId}`}
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
              <div className="fw-semibold">{displayedCampaignName}</div>
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

      <div className="card border-0 shadow-sm mb-4">
        <div className="card-body p-4">
          <h2 className="h5 mb-3">Previous Screening &amp; Eligibility History</h2>
          {historyError ? (
            <div className="alert alert-warning mb-0" role="status">
              {historyError}
            </div>
          ) : previousSubmissions.length === 0 ? (
            <p className="text-secondary mb-0">
              No previous screening history found for this citizen.
            </p>
          ) : (
            <div className="vstack gap-3">
              {previousSubmissions.map((sub) => {
                const subCampaign = campaignsById.get(String(sub.campaignId));
                const subCampaignName = campaignName(subCampaign, sub.campaignId);
                const linkedAssessment = assessmentsBySubId.get(String(sub.id));
                const prelimLabel = sub.preliminaryResult === PRELIMINARY_SCREENING_RESULT.PASS
                  ? 'Preliminary pass'
                  : sub.preliminaryResult === PRELIMINARY_SCREENING_RESULT.REVIEW_REQUIRED
                    ? 'Healthcare review required'
                    : (PRELIMINARY_SCREENING_RESULT_LABELS[sub.preliminaryResult] || sub.preliminaryResult || 'Unavailable');

                return (
                  <article className="border rounded p-3" key={sub.id}>
                    <div className="d-flex flex-wrap justify-content-between align-items-start gap-2 mb-2">
                      <div>
                        <div className="fw-semibold">{subCampaignName}</div>
                        <div className="small text-secondary">
                          Submission #{sub.id} · Submitted {formatScreeningTimestamp(sub.submittedAt || sub.createdAt)}
                          {subCampaign?.code ? ` · ${subCampaign.code}` : ''}
                        </div>
                      </div>
                      <span className="badge bg-secondary-subtle text-secondary-emphasis">
                        {SCREENING_SUBMISSION_STATUS_LABELS[sub.status] || sub.status}
                      </span>
                    </div>

                    <div className="row g-2 mt-1 mb-2">
                      <div className="col-sm-6 col-md-3">
                        <div className="small text-secondary">Preliminary result</div>
                        <div className="fw-medium">{prelimLabel}</div>
                      </div>
                      <div className="col-sm-6 col-md-3">
                        <div className="small text-secondary">Final recorded status</div>
                        <div className="fw-medium">
                          {linkedAssessment
                            ? (ELIGIBILITY_ASSESSMENT_STATUS_LABELS[linkedAssessment.status] || linkedAssessment.status)
                            : 'Not yet recorded'}
                        </div>
                      </div>
                      {linkedAssessment?.reviewedAt && (
                        <div className="col-sm-6 col-md-3">
                          <div className="small text-secondary">Reviewed date</div>
                          <div>{formatScreeningTimestamp(linkedAssessment.reviewedAt)}</div>
                        </div>
                      )}
                      {linkedAssessment?.validUntil && (
                        <div className="col-sm-6 col-md-3">
                          <div className="small text-secondary">Valid until</div>
                          <div>{formatScreeningTimestamp(linkedAssessment.validUntil)}</div>
                        </div>
                      )}
                    </div>

                    {linkedAssessment?.reason && (
                      <div className="small mb-2">
                        <span className="text-secondary">Decision reason: </span>
                        <span className="text-break">{linkedAssessment.reason}</span>
                      </div>
                    )}

                    {linkedAssessment?.revokedAt && (
                      <div className="small text-danger mb-2">
                        <span>Revoked on {formatScreeningTimestamp(linkedAssessment.revokedAt)}</span>
                        {linkedAssessment.revocationReason && (
                          <span className="text-break"> — {linkedAssessment.revocationReason}</span>
                        )}
                      </div>
                    )}

                    <div className="mt-2 pt-2 border-top">
                      <Link
                        className="btn btn-sm btn-outline-primary"
                        to={`/app/staff/screening/history/${sub.id}`}
                      >
                        View Previous Screening
                      </Link>
                    </div>
                  </article>
                );
              })}
            </div>
          )}
        </div>
      </div>

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
