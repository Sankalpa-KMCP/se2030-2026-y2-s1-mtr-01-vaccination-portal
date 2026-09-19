import React from 'react';

/**
 * Component showing the citizen's current queue position in a waitlist.
 *
 * <p><b>User Story:</b>
 * <ul>
 *   <li>US-05: Citizen waitlist position transparency.</li>
 * </ul>
 *
 * <p><b>Business Rules Enforced in UI:</b>
 * <ul>
 *   <li>MF03-BR-06: Renders 1-based dynamic index calculated from FIFO arrival order.</li>
 * </ul>
 *
 * @param {Object} props
 * @param {number|null} props.position - The 1-based index in the queue
 */
export default function WaitlistPosition({ position }) {
  if (position == null) return null;

  return (
    <div className="card border-primary border-2 bg-light shadow-sm mb-3">
      <div className="card-body p-3 d-flex align-items-center">
        <div
          className="bg-primary text-white rounded-circle d-flex align-items-center justify-content-center me-3 shadow-sm"
          style={{ width: '48px', height: '48px', minWidth: '48px' }}
        >
          <span className="fw-bold fs-5">#{position}</span>
        </div>
        <div>
          <h6 className="mb-0 fw-bold text-primary">Queue Position</h6>
          <p className="mb-0 text-muted small">
            You are <strong>#{position}</strong> in line for this vaccination session.
          </p>
        </div>
      </div>
    </div>
  );
}
