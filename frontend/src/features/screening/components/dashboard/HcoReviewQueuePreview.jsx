import React from 'react';
import { Link } from 'react-router-dom';
import {
  SCREENING_SUBMISSION_STATUS,
  SCREENING_SUBMISSION_STATUS_LABELS,
} from '../../constants/screeningConstants';
import { formatScreeningTimestamp } from '../../utils/screeningHistory';

function getStatusBadgeClass(status) {
  switch (status) {
    case SCREENING_SUBMISSION_STATUS.PENDING_REVIEW:
      return 'badge text-bg-warning text-dark';
    case SCREENING_SUBMISSION_STATUS.CORRECTIONS_REQUIRED:
      return 'badge text-bg-danger';
    case SCREENING_SUBMISSION_STATUS.REVIEWED:
      return 'badge text-bg-success';
    default:
      return 'badge text-bg-secondary';
  }
}

export default function HcoReviewQueuePreview({ submissions = [], loading = false }) {
  const displayItems = submissions.slice(0, 5);

  return (
    <div className="hco-card h-100">
      <div className="hco-card-header">
        <h2 className="hco-card-title">
          <i className="bi bi-clipboard-pulse hco-card-title-icon" aria-hidden="true" />
          <span>Screening Review Queue</span>
        </h2>
        <Link to="/app/staff/screening/reviews" className="hco-card-link">
          <span>View all</span>
          <i className="bi bi-arrow-right" aria-hidden="true" />
        </Link>
      </div>

      <div className="hco-card-body p-0">
        {loading ? (
          <div className="p-3" role="status" aria-label="Loading review queue">
            <div className="hco-skeleton-row" />
            <div className="hco-skeleton-row" />
            <div className="hco-skeleton-row" />
          </div>
        ) : displayItems.length === 0 ? (
          <div className="hco-empty-state">
            <i className="bi bi-clipboard-check hco-empty-icon" aria-hidden="true" />
            <p className="hco-empty-text">No screenings currently require attention.</p>
          </div>
        ) : (
          <div className="table-responsive">
            <table className="table hco-table align-middle">
              <thead>
                <tr>
                  <th scope="col">Citizen</th>
                  <th scope="col">Submission</th>
                  <th scope="col">Submitted</th>
                  <th scope="col">Status</th>
                  <th scope="col" className="text-end">Action</th>
                </tr>
              </thead>
              <tbody>
                {displayItems.map((submission) => {
                  const citizenLabel = submission.citizenFullName
                    || (submission.citizenId ? `Citizen #${submission.citizenId}` : 'Citizen');
                  const submissionRef = `Submission #${submission.id}`;
                  const submittedDate = formatScreeningTimestamp(
                    submission.submittedAt || submission.createdAt
                  );
                  const statusLabel = SCREENING_SUBMISSION_STATUS_LABELS[submission.status]
                    || submission.status
                    || 'Pending';
                  const badgeClass = getStatusBadgeClass(submission.status);

                  return (
                    <tr key={submission.id}>
                      <td className="fw-semibold">{citizenLabel}</td>
                      <td>{submissionRef}</td>
                      <td className="text-secondary">{submittedDate}</td>
                      <td>
                        <span className={badgeClass}>{statusLabel}</span>
                      </td>
                      <td className="text-end">
                        <Link
                          to={`/app/staff/screening/reviews/${submission.id}`}
                          className="hco-btn-review"
                        >
                          Review
                        </Link>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
