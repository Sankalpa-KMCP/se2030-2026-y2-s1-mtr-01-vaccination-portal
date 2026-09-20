import React from 'react';
import { Link } from 'react-router-dom';
import { SCREENING_SUBMISSION_STATUS } from '../constants/screeningConstants';
import { formatScreeningTimestamp } from '../utils/screeningHistory';
import CitizenStatusBadge from '../../citizen-ui/components/CitizenStatusBadge';
import './ScreeningCorrectionNotice.css';

export default function ScreeningCorrectionNotice({ submission, action }) {
  if (!submission?.correctionInstructions) return null;

  const isCurrent = submission.status === SCREENING_SUBMISSION_STATUS.CORRECTIONS_REQUIRED;

  if (isCurrent) {
    const defaultAction = (submission.id || submission.submissionId) ? (
      <Link
        className="btn btn-primary citizen-action citizen-action--primary"
        to={`/app/screening/history/${submission.id ?? submission.submissionId}/correct`}
      >
        Correct Screening
      </Link>
    ) : null;
    const effectiveAction = action !== undefined ? action : defaultAction;

    return (
      <aside className="citizen-correction-notice citizen-correction-notice--active" aria-label="Action Required">
        <div className="citizen-correction-notice__header">
          <div className="citizen-correction-notice__title-wrap">
            <svg
              className="citizen-correction-notice__icon citizen-correction-notice__icon--warning"
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              aria-hidden="true"
            >
              <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
              <line x1="12" y1="9" x2="12" y2="13" />
              <line x1="12" y1="17" x2="12.01" y2="17" />
            </svg>
            <div>
              <p className="citizen-correction-notice__eyebrow">Corrections requested</p>
              <h3 className="citizen-correction-notice__title">Action Required</h3>
            </div>
          </div>
          <CitizenStatusBadge tone="attention">Action Required</CitizenStatusBadge>
        </div>
        <div className="citizen-correction-notice__body">
          <p className="citizen-correction-notice__instructions" style={{ whiteSpace: 'pre-wrap' }}>
            {submission.correctionInstructions}
          </p>
          {submission.correctionRequestedAt && (
            <p className="citizen-correction-notice__meta">
              Requested: {formatScreeningTimestamp(submission.correctionRequestedAt)}
            </p>
          )}
          {effectiveAction && <div className="citizen-correction-notice__action">{effectiveAction}</div>}
        </div>
      </aside>
    );
  }

  // Resolved / historical correction notice
  return (
    <aside className="citizen-correction-notice citizen-correction-notice--resolved" aria-label="Previous correction request">
      <div className="citizen-correction-notice__header">
        <div className="citizen-correction-notice__title-wrap">
          <svg
            className="citizen-correction-notice__icon citizen-correction-notice__icon--history"
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            aria-hidden="true"
          >
            <circle cx="12" cy="12" r="10" />
            <polyline points="12 6 12 12 16 14" />
          </svg>
          <div>
            <h3 className="citizen-correction-notice__title">Previous correction request</h3>
          </div>
        </div>
        <CitizenStatusBadge tone="neutral">Resolved</CitizenStatusBadge>
      </div>
      <div className="citizen-correction-notice__body">
        <p className="citizen-correction-notice__instructions" style={{ whiteSpace: 'pre-wrap' }}>
          {submission.correctionInstructions}
        </p>
        {submission.correctionRequestedAt && (
          <p className="citizen-correction-notice__meta">
            Requested: {formatScreeningTimestamp(submission.correctionRequestedAt)}
          </p>
        )}
      </div>
    </aside>
  );
}
