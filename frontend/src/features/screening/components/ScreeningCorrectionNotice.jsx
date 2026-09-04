import React from 'react';
import { SCREENING_SUBMISSION_STATUS } from '../constants/screeningConstants';
import { formatScreeningTimestamp } from '../utils/screeningHistory';

export default function ScreeningCorrectionNotice({ submission }) {
  if (!submission?.correctionInstructions) return null;
  const historical = submission.status !== SCREENING_SUBMISSION_STATUS.CORRECTIONS_REQUIRED;
  return (
    <aside className="alert alert-warning text-break mt-3">
      <h3 className="h6">{historical ? 'Corrections requested — historical information' : 'Corrections requested'}</h3>
      <p className="mb-1" style={{ whiteSpace: 'pre-wrap' }}>{submission.correctionInstructions}</p>
      {submission.correctionRequestedAt && <p className="small mb-0">Requested: {formatScreeningTimestamp(submission.correctionRequestedAt)}</p>}
    </aside>
  );
}
