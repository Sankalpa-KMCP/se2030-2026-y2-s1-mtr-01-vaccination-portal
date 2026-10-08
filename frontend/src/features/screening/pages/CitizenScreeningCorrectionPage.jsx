import React, { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import LoadingSpinner from '../../../components/common/LoadingSpinner';
import { getScreeningCorrectionForm, resubmitScreening } from '../api/screeningApi';
import ScreeningQuestionField from '../components/ScreeningQuestionField';
import {
  CITIZEN_PRELIMINARY_RESULT_LABELS,
  SCREENING_SUBMISSION_STATUS,
  SCREENING_SUBMISSION_STATUS_LABELS,
} from '../constants/screeningConstants';
import { createCorrectionAnswerState } from '../utils/screeningCorrectionAnswers';
import { serializeScreeningAnswers, validateScreeningAnswers } from '../utils/screeningAnswers';
import { formatScreeningTimestamp } from '../utils/screeningHistory';

function correctionError(error, fallbackMessage) {
  const status = error?.status;
  let message = error?.message || fallbackMessage;
  if (status === 404) message = 'The screening submission could not be found.';
  if (status === 409) message = 'This screening is no longer available for correction.';
  return Object.assign(new Error(message), { status, data: error?.data });
}

function CorrectionSuccess({ submission, submissionId }) {
  const preliminaryResult = CITIZEN_PRELIMINARY_RESULT_LABELS[submission.preliminaryResult]
    || submission.preliminaryResult;

  return (
    <div className="card border-success shadow-sm" aria-live="polite">
      <div className="card-body p-4">
        <span className="badge text-bg-success mb-3">Corrections submitted</span>
        <h1 className="h3">Corrections submitted successfully</h1>
        <p>Your screening is awaiting review again.</p>
        <dl className="row mb-3">
          <dt className="col-sm-4">Status</dt>
          <dd className="col-sm-8">
            {SCREENING_SUBMISSION_STATUS_LABELS[submission.status] || submission.status || 'Pending review'}
          </dd>
          {preliminaryResult && (
            <>
              <dt className="col-sm-4">Preliminary result</dt>
              <dd className="col-sm-8">{preliminaryResult}</dd>
            </>
          )}
        </dl>
        {preliminaryResult && (
          <p className="alert alert-info">
            This is a preliminary screening result, not a final eligibility decision.
          </p>
        )}
        <div className="d-flex flex-wrap gap-2">
          <Link className="btn btn-primary" to={`/app/screening/history/${submissionId}`}>
            View Screening Details
          </Link>
          <Link className="btn btn-outline-primary" to="/app/screening/eligibility">
            View My Eligibility
          </Link>
        </div>
      </div>
    </div>
  );
}

export default function CitizenScreeningCorrectionPage() {
  const { submissionId } = useParams();
  const [reloadKey, setReloadKey] = useState(0);
  const [state, setState] = useState({ loading: true, form: null, error: null });
  const [answers, setAnswers] = useState({});
  const [legacyAnswers, setLegacyAnswers] = useState({});
  const [validationErrors, setValidationErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState(null);
  const [submissionResult, setSubmissionResult] = useState(null);

  useEffect(() => {
    let active = true;
    setState({ loading: true, form: null, error: null });
    setSubmissionResult(null);
    setSubmitError(null);

    async function loadCorrectionForm() {
      try {
        const form = await getScreeningCorrectionForm(submissionId);
        if (!active) return;
        if (!form || form.status !== SCREENING_SUBMISSION_STATUS.CORRECTIONS_REQUIRED) {
          throw Object.assign(new Error('Correction request is no longer active.'), { status: 409 });
        }
        const prefilled = createCorrectionAnswerState(form.questions);
        setAnswers(prefilled.answers);
        setLegacyAnswers(prefilled.legacyAnswers);
        setValidationErrors({});
        setState({ loading: false, form, error: null });
      } catch (error) {
        if (active) {
          setState({
            loading: false,
            form: null,
            error: correctionError(error, 'Unable to load the correction form.'),
          });
        }
      }
    }

    loadCorrectionForm();
    return () => { active = false; };
  }, [reloadKey, submissionId]);

  const sortedQuestions = useMemo(
    () => [...(state.form?.questions || [])].sort((left, right) => left.displayOrder - right.displayOrder),
    [state.form],
  );

  const updateAnswer = (questionId, value) => {
    setAnswers((current) => ({ ...current, [questionId]: value }));
    setLegacyAnswers((current) => {
      if (!current[questionId]) return current;
      const next = { ...current };
      delete next[questionId];
      return next;
    });
    setValidationErrors((current) => {
      if (!current[questionId]) return current;
      const next = { ...current };
      delete next[questionId];
      return next;
    });
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    const nextErrors = validateScreeningAnswers(sortedQuestions, answers);
    setValidationErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;

    setSubmitting(true);
    setSubmitError(null);
    try {
      const result = await resubmitScreening(
        submissionId,
        serializeScreeningAnswers(sortedQuestions, answers),
      );
      setSubmissionResult(result);
    } catch (error) {
      setSubmitError(correctionError(error, 'Unable to submit the corrected screening.'));
    } finally {
      setSubmitting(false);
    }
  };

  if (state.loading) return <LoadingSpinner message="Loading correction form..." />;
  if (submissionResult) {
    return <CorrectionSuccess submission={submissionResult} submissionId={submissionId} />;
  }

  const hasConflict = state.error?.status === 409 || submitError?.status === 409;

  return (
    <section aria-labelledby="screening-correction-title">
      <Link className="btn btn-link px-0 mb-3" to={`/app/screening/history/${submissionId}`}>
        ← Back to Screening Details
      </Link>
      <h1 className="display-6 fw-semibold mb-2" id="screening-correction-title">
        Correct Screening
      </h1>
      <p className="text-muted mb-4">Review every answer and submit the complete corrected screening.</p>

      <ApiErrorAlert error={state.error || submitError} />
      {hasConflict && (
        <div className="d-flex flex-wrap gap-2 mb-4">
          <button className="btn btn-outline-primary" type="button" onClick={() => setReloadKey((key) => key + 1)}>
            Reload correction request
          </button>
          <Link className="btn btn-outline-secondary" to={`/app/screening/history/${submissionId}`}>
            Return to Screening Details
          </Link>
        </div>
      )}

      {state.form && (
        <>
          <aside className="alert alert-warning mb-4">
            <h2 className="h5">Correction instructions</h2>
            <p className="mb-1" style={{ whiteSpace: 'pre-wrap' }}>
              {state.form.correctionInstructions || 'Review your answers and provide the requested corrections.'}
            </p>
            {state.form.correctionRequestedAt && (
              <p className="small mb-0">
                Requested: {formatScreeningTimestamp(state.form.correctionRequestedAt)}
              </p>
            )}
          </aside>

          <form onSubmit={handleSubmit} noValidate>
            <div className="card border-0 shadow-sm mb-4">
              <div className="card-body p-4">
                <h2 className="h4 mb-1">Screening answers</h2>
                <p className="text-muted small">
                  Questionnaire version {state.form.questionnaireVersionNumber}
                </p>
                {sortedQuestions.map((question) => (
                  <div key={question.id}>
                    <ScreeningQuestionField
                      question={question}
                      value={answers[question.id]}
                      onChange={(value) => updateAnswer(question.id, value)}
                      error={validationErrors[question.id]}
                    />
                    {legacyAnswers[question.id] && (
                      <p className="alert alert-warning small mt-n2" role="status">
                        Your previous answer ({legacyAnswers[question.id].join(', ')}) no longer matches an available
                        choice. Please select a valid replacement.
                      </p>
                    )}
                  </div>
                ))}
              </div>
            </div>

            <button className="btn btn-primary" type="submit" disabled={submitting}>
              {submitting ? 'Submitting corrections...' : 'Submit Corrections'}
            </button>
          </form>
        </>
      )}
    </section>
  );
}
