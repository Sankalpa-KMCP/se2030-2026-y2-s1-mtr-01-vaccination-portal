import React, { useState } from 'react';
import AppointmentStatusBadge from './AppointmentStatusBadge';

/**
 * Panel component displaying details of a queued appointment and providing
 * operational controls for staff (Check-In, Reverse Check-In, Mark No-Show).
 *
 * <p><b>User Story:</b>
 * <ul>
 *   <li>US-26: View checked-in queue and perform clinic arrival check-in, reversal, and no-show processing.</li>
 * </ul>
 *
 * <p><b>Business Rules Enforced in UI:</b>
 * <ul>
 *   <li>MF03-BR-09: Check-in modal allows staff override checkbox and requires override reason if outside 30-minute window.</li>
 *   <li>MF03-BR-10: Shows "Check In" button strictly when status is CONFIRMED.</li>
 *   <li>MF03-BR-15: Shows "Reverse Check-In" button strictly when status is CHECKED_IN.</li>
 * </ul>
 *
 * @param {Object} props
 * @param {Object} props.appointment - AppointmentResponse object
 * @param {Function} props.onCheckIn - (appointmentId, { isStaffOverride, staffReason }) => Promise<void>
 * @param {Function} props.onReverseCheckIn - (appointmentId) => Promise<void>
 * @param {Function} props.onMarkNoShow - (appointmentId) => Promise<void>
 * @param {boolean} [props.isProcessing=false] - Whether an action on this appointment is in progress
 */
export default function CheckInPanel({
  appointment,
  onCheckIn,
  onReverseCheckIn,
  onMarkNoShow,
  isProcessing = false,
}) {
  const [showCheckInModal, setShowCheckInModal] = useState(false);
  const [isStaffOverride, setIsStaffOverride] = useState(false);
  const [staffReason, setStaffReason] = useState('');

  const [showReverseModal, setShowReverseModal] = useState(false);
  const [showNoShowModal, setShowNoShowModal] = useState(false);

  if (!appointment) return null;

  const formatDate = (isoString) => {
    if (!isoString) return 'N/A';
    return new Date(isoString).toLocaleString(undefined, {
      dateStyle: 'medium',
      timeStyle: 'short',
    });
  };

  const isConfirmed = appointment.status === 'CONFIRMED';
  const isCheckedIn = appointment.status === 'CHECKED_IN';

  const handleCheckInSubmit = (e) => {
    e.preventDefault();
    onCheckIn(appointment.id, {
      isStaffOverride,
      staffReason: isStaffOverride ? staffReason : null,
    });
    setShowCheckInModal(false);
    setIsStaffOverride(false);
    setStaffReason('');
  };

  const handleReverseSubmit = (e) => {
    e.preventDefault();
    onReverseCheckIn(appointment.id);
    setShowReverseModal(false);
  };

  const handleNoShowSubmit = (e) => {
    e.preventDefault();
    onMarkNoShow(appointment.id);
    setShowNoShowModal(false);
  };

  return (
    <div className="card shadow-sm border-0 mb-3 hover-shadow transition-all">
      <div className="card-body p-4">
        <div className="d-flex justify-content-between align-items-start mb-3">
          <div>
            <h5 className="card-title text-primary mb-1 fw-bold">
              Appointment #{appointment.id}
            </h5>
            <small className="text-muted">
              Booked on: {formatDate(appointment.bookedAt)}
            </small>
          </div>
          <AppointmentStatusBadge status={appointment.status} />
        </div>

        <div className="row g-2 my-2 py-2 bg-light rounded px-2">
          <div className="col-sm-4">
            <small className="text-secondary d-block">Citizen ID</small>
            <span className="fw-semibold text-dark small">#{appointment.citizenId}</span>
          </div>
          <div className="col-sm-4">
            <small className="text-secondary d-block">Campaign ID</small>
            <span className="fw-semibold text-dark small">#{appointment.campaignId}</span>
          </div>
          <div className="col-sm-4">
            <small className="text-secondary d-block">Time Slot ID</small>
            <span className="fw-semibold text-dark small">#{appointment.slotId}</span>
          </div>
        </div>

        {appointment.cancelledAt && (
          <div className="mt-2 text-danger small">
            Cancelled on: {formatDate(appointment.cancelledAt)}
          </div>
        )}

        {/* Operational Actions */}
        <div className="d-flex justify-content-end gap-2 mt-3 pt-2 border-top">
          {isConfirmed && (
            <>
              <button
                type="button"
                className="btn btn-outline-danger btn-sm px-3 fw-semibold"
                disabled={isProcessing}
                onClick={() => setShowNoShowModal(true)}
              >
                Mark No-Show
              </button>
              <button
                type="button"
                className="btn btn-success btn-sm px-4 fw-bold shadow-sm"
                disabled={isProcessing}
                onClick={() => setShowCheckInModal(true)}
              >
                {isProcessing ? (
                  <>
                    <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                    Processing...
                  </>
                ) : (
                  'Check In'
                )}
              </button>
            </>
          )}

          {isCheckedIn && (
            <button
              type="button"
              className="btn btn-outline-warning btn-sm px-3 fw-semibold text-dark"
              disabled={isProcessing}
              onClick={() => setShowReverseModal(true)}
            >
              {isProcessing ? (
                <>
                  <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                  Processing...
                </>
              ) : (
                'Reverse Check-In'
              )}
            </button>
          )}
        </div>
      </div>

      {/* Check-In Modal with optional Staff Override */}
      {showCheckInModal && (
        <div className="modal fade show d-block bg-dark bg-opacity-50" tabIndex="-1" role="dialog" aria-modal="true">
          <div className="modal-dialog modal-dialog-centered" role="document">
            <div className="modal-content border-0 shadow">
              <div className="modal-header bg-success text-white">
                <h5 className="modal-title fw-bold">Confirm Check-In</h5>
                <button
                  type="button"
                  className="btn-close btn-close-white"
                  aria-label="Close"
                  onClick={() => setShowCheckInModal(false)}
                ></button>
              </div>
              <form onSubmit={handleCheckInSubmit}>
                <div className="modal-body p-4">
                  <p className="mb-3">
                    Check in Citizen <strong>#{appointment.citizenId}</strong> for Appointment <strong>#{appointment.id}</strong> (Slot #{appointment.slotId})?
                  </p>

                  <div className="form-check mb-3">
                    <input
                      type="checkbox"
                      id={`staffOverrideCheck_${appointment.id}`}
                      className="form-check-input"
                      checked={isStaffOverride}
                      onChange={(e) => setIsStaffOverride(e.target.checked)}
                    />
                    <label htmlFor={`staffOverrideCheck_${appointment.id}`} className="form-check-label small">
                      Apply Staff Override (Required if citizen is arriving outside the canonical check-in window)
                    </label>
                  </div>

                  {isStaffOverride && (
                    <div className="mb-3">
                      <label htmlFor={`staffReason_${appointment.id}`} className="form-label fw-semibold">
                        Override Reason <span className="text-danger">*</span>
                      </label>
                      <input
                        type="text"
                        id={`staffReason_${appointment.id}`}
                        className="form-control"
                        value={staffReason}
                        onChange={(e) => setStaffReason(e.target.value)}
                        placeholder="e.g. Clinic authorized early/late arrival"
                        required={isStaffOverride}
                      />
                    </div>
                  )}
                </div>
                <div className="modal-footer">
                  <button
                    type="button"
                    className="btn btn-secondary"
                    onClick={() => setShowCheckInModal(false)}
                  >
                    Cancel
                  </button>
                  <button type="submit" className="btn btn-success fw-bold">
                    Complete Check-In
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}

      {/* Reverse Check-In Confirmation Modal */}
      {showReverseModal && (
        <div className="modal fade show d-block bg-dark bg-opacity-50" tabIndex="-1" role="dialog" aria-modal="true">
          <div className="modal-dialog modal-dialog-centered" role="document">
            <div className="modal-content border-0 shadow">
              <div className="modal-header bg-warning text-dark">
                <h5 className="modal-title fw-bold">Confirm Reverse Check-In</h5>
                <button
                  type="button"
                  className="btn-close"
                  aria-label="Close"
                  onClick={() => setShowReverseModal(false)}
                ></button>
              </div>
              <form onSubmit={handleReverseSubmit}>
                <div className="modal-body p-4">
                  <p className="mb-2">
                    Are you sure you want to reverse the check-in for Appointment <strong>#{appointment.id}</strong> (Citizen #{appointment.citizenId})?
                  </p>
                  <p className="text-muted small mb-0">
                    This will reset the appointment status back to <strong>CONFIRMED</strong>.
                  </p>
                </div>
                <div className="modal-footer">
                  <button
                    type="button"
                    className="btn btn-secondary"
                    onClick={() => setShowReverseModal(false)}
                  >
                    Keep Checked In
                  </button>
                  <button type="submit" className="btn btn-warning fw-bold text-dark">
                    Yes, Reverse Check-In
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}

      {/* Mark No-Show Confirmation Modal */}
      {showNoShowModal && (
        <div className="modal fade show d-block bg-dark bg-opacity-50" tabIndex="-1" role="dialog" aria-modal="true">
          <div className="modal-dialog modal-dialog-centered" role="document">
            <div className="modal-content border-0 shadow">
              <div className="modal-header bg-danger text-white">
                <h5 className="modal-title fw-bold">Confirm Mark No-Show</h5>
                <button
                  type="button"
                  className="btn-close btn-close-white"
                  aria-label="Close"
                  onClick={() => setShowNoShowModal(false)}
                ></button>
              </div>
              <form onSubmit={handleNoShowSubmit}>
                <div className="modal-body p-4">
                  <p className="mb-2">
                    Are you sure you want to mark Appointment <strong>#{appointment.id}</strong> as <strong>NO-SHOW</strong>?
                  </p>
                  <p className="text-muted small mb-0">
                    This records that Citizen #{appointment.citizenId} did not arrive within the designated check-in window.
                  </p>
                </div>
                <div className="modal-footer">
                  <button
                    type="button"
                    className="btn btn-secondary"
                    onClick={() => setShowNoShowModal(false)}
                  >
                    Cancel
                  </button>
                  <button type="submit" className="btn btn-danger fw-bold">
                    Yes, Mark No-Show
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
