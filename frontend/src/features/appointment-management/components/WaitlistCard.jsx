import React, { useState } from 'react';

function WaitlistStatusBadge({ status }) {
  if (!status) return null;

  const config = {
    WAITING: { label: 'Waiting in Queue', bgClass: 'bg-warning text-dark' },
    PROMOTED: { label: 'Promoted to Appointment', bgClass: 'bg-success' },
    SKIPPED_INELIGIBLE: { label: 'Skipped (Ineligible)', bgClass: 'bg-secondary' },
    SKIPPED_DUPLICATE_APPOINTMENT: { label: 'Skipped (Duplicate Booking)', bgClass: 'bg-secondary' },
    CANCELLED: { label: 'Cancelled', bgClass: 'bg-secondary' },
    SESSION_CANCELLED: { label: 'Session Cancelled', bgClass: 'bg-danger' },
    EXPIRED: { label: 'Expired', bgClass: 'bg-dark' },
  };

  const badgeInfo = config[status] || { label: status, bgClass: 'bg-dark' };

  return (
    <span className={`badge ${badgeInfo.bgClass} px-3 py-2 fs-7 fw-bold shadow-sm`}>
      {badgeInfo.label}
    </span>
  );
}

/**
 * Card displaying details of a citizen's waitlist entry and action controls (US-05).
 *
 * @param {Object} props
 * @param {Object} props.entry - WaitlistEntryResponse
 * @param {Function} props.onLeave - Callback when leaving waitlist (receives entryId)
 * @param {boolean} [props.isLeaving=false] - Whether leave operation is in progress
 */
export default function WaitlistCard({ entry, onLeave, isLeaving = false }) {
  const [showConfirmLeave, setShowConfirmLeave] = useState(false);

  if (!entry) return null;

  const formatDate = (isoString) => {
    if (!isoString) return 'N/A';
    return new Date(isoString).toLocaleString(undefined, {
      dateStyle: 'medium',
      timeStyle: 'short',
    });
  };

  const isWaiting = entry.status === 'WAITING';

  const handleConfirmLeave = (e) => {
    e.preventDefault();
    onLeave(entry.id);
    setShowConfirmLeave(false);
  };

  return (
    <div className="card shadow-sm border-0 mb-3">
      <div className="card-body p-4">
        <div className="d-flex justify-content-between align-items-start mb-3">
          <div>
            <h5 className="card-title text-primary mb-1 fw-bold">
              Waitlist Entry #{entry.id}
            </h5>
            <small className="text-muted">
              Joined on: {formatDate(entry.joinedAt)}
            </small>
          </div>
          <WaitlistStatusBadge status={entry.status} />
        </div>

        <div className="row g-2 my-2 py-2 bg-light rounded px-2">
          <div className="col-sm-4">
            <small className="text-secondary d-block">Citizen ID</small>
            <span className="fw-semibold text-dark small">#{entry.citizenId}</span>
          </div>
          <div className="col-sm-4">
            <small className="text-secondary d-block">Campaign ID</small>
            <span className="fw-semibold text-dark small">#{entry.campaignId}</span>
          </div>
          <div className="col-sm-4">
            <small className="text-secondary d-block">Session ID</small>
            <span className="fw-semibold text-dark small">#{entry.sessionId}</span>
          </div>
        </div>

        {isWaiting && (
          <div className="d-flex justify-content-end gap-2 mt-3 pt-2 border-top">
            <button
              type="button"
              className="btn btn-outline-danger btn-sm px-3 fw-semibold"
              disabled={isLeaving}
              onClick={() => setShowConfirmLeave(true)}
            >
              {isLeaving ? 'Leaving...' : 'Leave Waitlist'}
            </button>
          </div>
        )}
      </div>

      {showConfirmLeave && (
        <div className="modal fade show d-block bg-dark bg-opacity-50" tabIndex="-1" role="dialog" aria-modal="true">
          <div className="modal-dialog modal-dialog-centered" role="document">
            <div className="modal-content border-0 shadow">
              <div className="modal-header bg-danger text-white">
                <h5 className="modal-title fw-bold">Confirm Leaving Waitlist</h5>
                <button
                  type="button"
                  className="btn-close btn-close-white"
                  aria-label="Close"
                  onClick={() => setShowConfirmLeave(false)}
                ></button>
              </div>
              <div className="modal-body p-4">
                <p className="mb-3">
                  Are you sure you want to leave the waitlist for Session <strong>#{entry.sessionId}</strong>?
                  You will forfeit your current position in the queue.
                </p>

                <div className="d-flex justify-content-end gap-2 mt-4">
                  <button
                    type="button"
                    className="btn btn-secondary"
                    onClick={() => setShowConfirmLeave(false)}
                  >
                    Stay on Waitlist
                  </button>
                  <button
                    type="button"
                    className="btn btn-danger fw-bold"
                    onClick={handleConfirmLeave}
                  >
                    Yes, Leave Waitlist
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
