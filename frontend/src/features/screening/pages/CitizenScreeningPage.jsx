import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useAuth } from '../../../auth/context/AuthContext';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import LoadingSpinner from '../../../components/common/LoadingSpinner';
import {
  getActiveScreeningSubmission,
  getCitizenScreeningForm,
  submitScreening,
} from '../api/screeningApi';
import ScreeningQuestionField from '../components/ScreeningQuestionField';
import ScreeningSubmissionResult from '../components/ScreeningSubmissionResult';
import {
  CITIZEN_PRELIMINARY_RESULT_LABELS,
  SCREENING_SUBMISSION_STATUS,
  SCREENING_SUBMISSION_STATUS_LABELS,
} from '../constants/screeningConstants';
import { serializeScreeningAnswers, validateScreeningAnswers } from '../utils/screeningAnswers';

function formatTimestamp(value) {
  if (!value) return null;
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(date);
}

function ActiveSubmissionState({ submission }) {
  const isCorrectionsRequired = submission.status === SCREENING_SUBMISSION_STATUS.CORRECTIONS_REQUIRED;
  const isDraft = submission.status === SCREENING_SUBMISSION_STATUS.DRAFT;
  const heading = isCorrectionsRequired
    ? 'Corrections are required for this screening.'
    : isDraft
      ? 'A screening draft already exists.'
      : 'Your screening has already been submitted and is awaiting review.';

  return (
    <div className="card border-warning shadow-sm">
      <div className="card-body p-4">
        <span className="badge text-bg-warning mb-3">
          {SCREENING_SUBMISSION_STATUS_LABELS[submission.status] || submission.status}
        </span>
        <h1 className="h3">{heading}</h1>
        {submission.submittedAt && <p className="text-muted">Submitted {formatTimestamp(submission.submittedAt)}</p>}
        {submission.preliminaryResult && (
          <p><strong>Preliminary result:</strong>{' '}
            {CITIZEN_PRELIMINARY_RESULT_LABELS[submission.preliminaryResult] || submission.preliminaryResult}
          </p>
        )}
        {isCorrectionsRequired && submission.correctionInstructions && (
          <div className="alert alert-warning">
            <strong>Correction instructions</strong>
            <p className="mb-0 mt-1">{submission.correctionInstructions}</p>
          </div>
        )}
        {isCorrectionsRequired && (
          <Link className="btn btn-primary" to={`/app/screening/history/${submission.id}/correct`}>
            Correct Screening
          </Link>
        )}
        {isDraft && (
          <p className="mb-0">A second screening cannot be started while this draft remains active.</p>
        )}
      </div>
    </div>
  );
}

export default function CitizenScreeningPage() {
  const { campaignId } = useParams();
  const { user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [screeningForm, setScreeningForm] = useState(null);
  const [activeSubmission, setActiveSubmission] = useState(null);
  const [answers, setAnswers] = useState({});
  const [validationErrors, setValidationErrors] = useState({});
  const [consentChecked, setConsentChecked] = useState(false);
  const [consentError, setConsentError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [submissionResult, setSubmissionResult] = useState(null);

  const sortedQuestions = useMemo(
    () => [...(screeningForm?.questions || [])].sort((left, right) => left.displayOrder - right.displayOrder),
    [screeningForm],
  );

  const loadScreening = useCallback(async () => {
    setLoading(true);
    setError(null);
    setScreeningForm(null);
    setActiveSubmission(null);
    setSubmissionResult(null);
    setAnswers({});
    setValidationErrors({});
    setConsentChecked(false);
    setConsentError('');

    if (!user?.id) {
      setError('Your authenticated citizen profile is missing an account identifier.');
      setLoading(false);
      return;
    }

    try {
      const existingSubmission = await getActiveScreeningSubmission(user.id, campaignId);
      if (existingSubmission) {
        setActiveSubmission(existingSubmission);
        return;
      }
      setScreeningForm(await getCitizenScreeningForm(campaignId));
    } catch (requestError) {
      setError(requestError);
    } finally {
      setLoading(false);
    }
  }, [campaignId, user?.id]);

  useEffect(() => {
    loadScreening();
  }, [loadScreening]);

  const updateAnswer = (questionId, value) => {
    setAnswers((current) => ({ ...current, [questionId]: value }));
    setValidationErrors((current) => {
      if (!current[questionId]) return current;
      const next = { ...current };
      delete next[questionId];
      return next;
    });
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    const nextValidationErrors = validateScreeningAnswers(sortedQuestions, answers);
    setValidationErrors(nextValidationErrors);
    const nextConsentError = consentChecked ? '' : 'You must read and agree to the consent notice before submitting.';
    setConsentError(nextConsentError);

    if (Object.keys(nextValidationErrors).length > 0 || nextConsentError) return;

    setSubmitting(true);
    setError(null);
    try {
      const result = await submitScreening({
        citizenId: user.id,
        campaignId: screeningForm.campaignId,
        questionnaireVersionId: screeningForm.questionnaireVersionId,
        consentNoticeVersionId: screeningForm.consentNotice.id,
        answers: serializeScreeningAnswers(sortedQuestions, answers),
      });
      setSubmissionResult(result);
    } catch (requestError) {
      setError(requestError);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <LoadingSpinner message="Loading your screening..." />;

  return (
    <section aria-labelledby="citizen-screening-title">
      <Link className="btn btn-link px-0 mb-3" to="/app/screening">← Back to campaigns</Link>

      {submissionResult && <ScreeningSubmissionResult submission={submissionResult} />}
      {!submissionResult && activeSubmission && <ActiveSubmissionState submission={activeSubmission} />}

      {!submissionResult && !activeSubmission && (
        <>
          <div className="mb-4">
            <span className="badge text-bg-primary mb-2">Citizen Screening</span>
            <h1 className="display-6 fw-semibold mb-2" id="citizen-screening-title">Pre-Vaccination Screening</h1>
            <p className="text-muted mb-0">Answer the campaign questionnaire and review the consent notice before submitting.</p>
          </div>

          <ApiErrorAlert error={error} />
          {error?.status === 409 && (
            <button className="btn btn-outline-primary mb-4" type="button" onClick={loadScreening}>
              Reload screening form
            </button>
          )}

          {!error && screeningForm && sortedQuestions.length === 0 && (
            <div className="alert alert-info">This campaign does not currently have screening questions available.</div>
          )}

          {!error && screeningForm && sortedQuestions.length > 0 && (
            <form onSubmit={handleSubmit} noValidate>
              <div className="card border-0 shadow-sm mb-4">
                <div className="card-body p-4">
                  <h2 className="h4 mb-1">Screening questions</h2>
                  <p className="text-muted small">Questionnaire version {screeningForm.questionnaireVersionNumber}</p>
                  {sortedQuestions.map((question) => (
                    <ScreeningQuestionField
                      key={question.id}
                      question={question}
                      value={answers[question.id]}
                      onChange={(value) => updateAnswer(question.id, value)}
                      error={validationErrors[question.id]}
                    />
                  ))}
                </div>
              </div>

              <div className="card border-primary-subtle shadow-sm mb-4">
                <div className="card-body p-4">
                  <h2 className="h4">{screeningForm.consentNotice.title}</h2>
                  <p className="text-muted small">Consent notice version {screeningForm.consentNotice.versionNumber}</p>
                  <div className="bg-light border rounded p-3 mb-3" style={{ whiteSpace: 'pre-wrap' }}>
                    {screeningForm.consentNotice.content}
                  </div>
                  <div className="form-check">
                    <input
                      className="form-check-input"
                      id="screening-consent"
                      type="checkbox"
                      checked={consentChecked}
                      aria-invalid={Boolean(consentError)}
                      aria-describedby={consentError ? 'screening-consent-error' : undefined}
                      onChange={(event) => {
                        setConsentChecked(event.target.checked);
                        if (event.target.checked) setConsentError('');
                      }}
                    />
                    <label className="form-check-label" htmlFor="screening-consent">
                      I have read and agree to the consent notice above.
                    </label>
                  </div>
                  {consentError && <div className="invalid-feedback d-block" id="screening-consent-error">{consentError}</div>}
                </div>
              </div>

              <button className="btn btn-primary" type="submit" disabled={submitting}>
                {submitting ? 'Submitting screening...' : 'Submit screening'}
              </button>
            </form>
          )}
        </>
      )}
    </section>
  );
}
