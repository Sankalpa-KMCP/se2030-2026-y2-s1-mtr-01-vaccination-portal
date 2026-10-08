import React from 'react';

/**
 * Card component displaying details of a single available vaccination session/slot (US-03).
 *
 * <p><b>User Story:</b>
 * <ul>
 *   <li>US-03: View session start/end time, clinic center details, and book an open place.</li>
 * </ul>
 *
 * <p><b>Business Rules Enforced in UI:</b>
 * <ul>
 *   <li>MF03-BR-03: Reflects remaining capacity; disables booking button when remaining capacity is zero.</li>
 * </ul>
 */
export default function SessionAvailabilityCard({ session, onBook, isBooking = false }) {
  if (!session) return null;

  const formatDate = (isoString) => {
    if (!isoString) return 'N/A';
    const d = new Date(isoString);
    return d.toLocaleString(undefined, {
      dateStyle: 'medium',
      timeStyle: 'short',
    });
  };

  const isFull = session.remainingCapacity <= 0;

  return (
    <div className="card shadow-sm border-0 mb-3 hover-shadow transition-all">
      <div className="card-body p-4">
        <div className="d-flex justify-content-between align-items-start mb-2">
          <div>
            <h5 className="card-title text-primary mb-1 fw-bold">
              {session.centerName || `Center #${session.centerId}`}
            </h5>
            <p className="card-subtitle text-muted small mb-0">
              {session.centerAddress || 'Vaccination Center'}
            </p>
          </div>
          <span className={`badge fs-6 ${isFull ? 'bg-danger' : 'bg-success'}`}>
            {isFull ? 'Full' : `${session.remainingCapacity} Places Left`}
          </span>
        </div>

        <div className="row g-2 my-2 py-2 bg-light rounded px-2">
          <div className="col-sm-6">
            <small className="text-secondary d-block">Start Time</small>
            <span className="fw-semibold text-dark small">{formatDate(session.startAt)}</span>
          </div>
          <div className="col-sm-6">
            <small className="text-secondary d-block">End Time</small>
            <span className="fw-semibold text-dark small">{formatDate(session.endAt)}</span>
          </div>
        </div>

        <div className="d-flex justify-content-between align-items-center mt-3 pt-2 border-top">
          <small className="text-muted">
            Session #{session.sessionId || session.slotId}
          </small>
          <button
            type="button"
            className="btn btn-primary px-4 fw-semibold shadow-sm"
            disabled={isFull || isBooking}
            onClick={() => onBook(session)}
          >
            {isBooking ? (
              <>
                <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                Booking...
              </>
            ) : (
              'Book Appointment'
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
