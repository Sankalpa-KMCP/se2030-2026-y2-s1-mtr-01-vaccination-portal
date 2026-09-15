import React from 'react';
import {
  CITIZEN_PRELIMINARY_RESULT_LABELS,
  PRELIMINARY_SCREENING_RESULT,
  SCREENING_SUBMISSION_STATUS_LABELS,
} from '../constants/screeningConstants';

function formatTimestamp(value) {
  if (!value) return 'Not available';
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(date);
}

export default function ScreeningSubmissionResult({ submission }) {
  const preliminaryLabel = CITIZEN_PRELIMINARY_RESULT_LABELS[submission.preliminaryResult]
    || submission.preliminaryResult
    || 'Pending assessment';

  return (
    <div className="card border-success shadow-sm" aria-live="polite">
      <div className="card-body p-4">
        <span className="badge text-bg-success mb-3">Submission successful</span>
        <h2 className="h3 mb-3">Your screening was submitted</h2>
        <dl className="row mb-3">
          <dt className="col-sm-4">Submitted</dt>
          <dd className="col-sm-8">{formatTimestamp(submission.submittedAt)}</dd>
          <dt className="col-sm-4">Status</dt>
          <dd className="col-sm-8">
            {SCREENING_SUBMISSION_STATUS_LABELS[submission.status] || submission.status}
          </dd>
          <dt className="col-sm-4">Preliminary result</dt>
          <dd className="col-sm-8">{preliminaryLabel}</dd>
        </dl>
        {submission.preliminaryResult === PRELIMINARY_SCREENING_RESULT.PASS && (
          <p className="alert alert-info mb-0">
            This is a preliminary screening result. Final eligibility may depend on later portal review.
          </p>
        )}
        {submission.preliminaryResult === PRELIMINARY_SCREENING_RESULT.REVIEW_REQUIRED && (
          <p className="alert alert-warning mb-0">
            A healthcare professional must review your screening. This is not a diagnosis or final ineligibility decision.
          </p>
        )}
      </div>
    </div>
  );
}
