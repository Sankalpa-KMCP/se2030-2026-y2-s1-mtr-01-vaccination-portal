import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useAuth } from '../../../auth/context/AuthContext';
import CitizenErrorState from '../../citizen-ui/components/CitizenErrorState';
import CitizenPageHeader from '../../citizen-ui/components/CitizenPageHeader';
import CitizenStatePanel from '../../../shared/ui/citizen/CitizenStatePanel';
import CitizenStatusBadge from '../../../shared/ui/citizen/CitizenStatusBadge';
import CitizenWorkflowGuide from '../../../shared/ui/citizen/CitizenWorkflowGuide';
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
    <div className="citizen-detail-panel citizen-active-screening">
      <div className="citizen-detail-panel__header">
        <CitizenStatusBadge tone={isCorrectionsRequired ? 'attention' : 'pending'}>
          {SCREENING_SUBMISSION_STATUS_LABELS[submission.status] || submission.status}
        </CitizenStatusBadge>
        <h2 className="h3">{heading}</h2>
      </div>
      <div className="citizen-detail-panel__body">
        {submission.submittedAt && <p className="text-muted">Submitted {formatTimestamp(submission.submittedAt)}</p>}
        {submission.preliminaryResult && (
          <p><strong>Preliminary result:</strong>{' '}
            {CITIZEN_PRELIMINARY_RESULT_LABELS[submission.preliminaryResult] || submission.preliminaryResult}
          </p>
        )}
        {isCorrectionsRequired && submission.correctionInstructions && (
          <div className="citizen-state citizen-state--warning">
            <span className="citizen-state__marker" aria-hidden="true" />
            <div className="citizen-state__copy">
            <strong>Correction instructions</strong>
            <p className="mb-0 mt-1">{submission.correctionInstructions}</p>
            </div>
          </div>
        )}
        {isCorrectionsRequired && (
          <Link className="btn btn-primary citizen-action citizen-action--primary mt-3" to={`/app/screening/history/${submission.id}/correct`}>
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

  return (
    <section className="citizen-page" aria-labelledby="citizen-screening-title">
      <CitizenPageHeader
        eyebrow="Pre-Vaccination Screening"
        title="Screening Questionnaire"
        titleId="citizen-screening-title"
        subtitle="Answer the campaign questionnaire and review the consent notice before submitting."
        backTo="/app/screening"
        backLabel="Back to campaigns"
        meta={screeningForm?.questionnaireVersionNumber
          ? `Questionnaire version ${screeningForm.questionnaireVersionNumber}`
          : undefined}
      />

      <CitizenWorkflowGuide
        titleId="screening-workflow-guide"
        steps={['Review questions', 'Confirm consent', 'Submit screening']}
      />

      {loading && (
        <CitizenStatePanel eyebrow="Screening form" title="Loading your screening" tone="loading" status>
          <p>Checking for an existing submission and loading the campaign questionnaire.</p>
        </CitizenStatePanel>
      )}

      {!loading && submissionResult && <div className="citizen-screening-result"><ScreeningSubmissionResult submission={submissionResult} /></div>}
      {!loading && !submissionResult && activeSubmission && <ActiveSubmissionState submission={activeSubmission} />}

      {!loading && !submissionResult && !activeSubmission && (
        <>
          {error && (
            <CitizenErrorState
              error={error}
              actions={error?.status === 409 ? (
                <button className="citizen-action citizen-action--secondary" type="button" onClick={loadScreening}>
                  Reload screening form
                </button>
              ) : undefined}
            />
          )}

          {!error && screeningForm && sortedQuestions.length === 0 && (
            <CitizenStatePanel title="No screening questions available">
              <p>This campaign does not currently have screening questions available.</p>
            </CitizenStatePanel>
          )}

          {!error && screeningForm && sortedQuestions.length > 0 && (
            <form onSubmit={handleSubmit} noValidate>
              <div className="citizen-page__stack">
                <section className="citizen-form-panel" aria-labelledby="screening-questions-title">
                  <header className="citizen-form-panel__header">
                    <p className="citizen-ui-eyebrow">Step 1 of 3</p>
                    <h2 id="screening-questions-title">Screening questions</h2>
                    <p>Questionnaire version {screeningForm.questionnaireVersionNumber}. Required questions are marked with an asterisk.</p>
                  </header>
                  <div className="citizen-form-panel__body citizen-question-list">
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
                </section>

                <section className="citizen-form-panel citizen-consent-panel" aria-labelledby="screening-consent-title">
                  <header className="citizen-form-panel__header">
                    <p className="citizen-ui-eyebrow">Step 2 of 3</p>
                    <h2 id="screening-consent-title">{screeningForm.consentNotice.title}</h2>
                    <p>Consent notice version {screeningForm.consentNotice.versionNumber}</p>
                  </header>
                  <div className="citizen-form-panel__body">
                  <div className="citizen-consent-copy">
                    {screeningForm.consentNotice.content}
                  </div>
                  <div className="form-check mt-3 p-3 ps-5 rounded bg-white">
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
                </section>

                <div className="citizen-submit-bar">
                  <div>
                    <p className="citizen-ui-eyebrow">Step 3 of 3</p>
                    <p>Review your answers and consent before sending this screening.</p>
                  </div>
                  <button className="citizen-action citizen-action--primary" type="submit" disabled={submitting}>
                    {submitting ? 'Submitting screening...' : 'Submit screening'}
                  </button>
                </div>
              </div>
            </form>
          )}
        </>
      )}
    </section>
  );
}
