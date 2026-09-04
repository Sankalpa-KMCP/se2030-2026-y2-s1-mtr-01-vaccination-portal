import React from 'react';
import { ELIGIBILITY_ASSESSMENT_STATUS_LABELS } from '../constants/screeningConstants';
import { formatScreeningTimestamp } from '../utils/screeningHistory';

/** Explicit citizen-safe fields only; never render clinicalNotes or reviewer/rescreening internals. */
export default function RecordedEligibilityAssessment({ assessment }) {
  return (
    <dl className="row mb-0">
      <dt className="col-sm-4">Recorded decision</dt>
      <dd className="col-sm-8"><span className="badge text-bg-light border text-dark">
        {ELIGIBILITY_ASSESSMENT_STATUS_LABELS[assessment.status] || 'Decision unavailable'}
      </span></dd>
      {assessment.reason && <><dt className="col-sm-4">Reason</dt><dd className="col-sm-8 text-break">{assessment.reason}</dd></>}
      <dt className="col-sm-4">Reviewed</dt><dd className="col-sm-8">{formatScreeningTimestamp(assessment.reviewedAt)}</dd>
      {assessment.validUntil && <><dt className="col-sm-4">Valid until</dt><dd className="col-sm-8">{formatScreeningTimestamp(assessment.validUntil)}</dd></>}
      {assessment.revokedAt && <><dt className="col-sm-4">Revoked on</dt><dd className="col-sm-8">{formatScreeningTimestamp(assessment.revokedAt)}</dd></>}
      {assessment.revocationReason && <><dt className="col-sm-4">Revocation reason</dt><dd className="col-sm-8 text-break">{assessment.revocationReason}</dd></>}
    </dl>
  );
}
