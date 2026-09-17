import React, { useState } from 'react';
import AppointmentStatusBadge from './AppointmentStatusBadge';

/**
 * Card component displaying details of a citizen's appointment and providing action controls (US-03).
 *
 * <p><b>User Story:</b>
 * <ul>
 *   <li>US-03: View booked appointments, initiate cancellation, and open rescheduling dialog.</li>
 * </ul>
 *
 * <p><b>Business Rules Enforced in UI:</b>
 * <ul>
 *   <li>MF03-BR-04: Displays cancellation confirmation modal with staff override checkbox and mandatory
 *       audit reason input for late cancellations within 24 hours of session start.</li>
 *   <li>MF03-BR-11: Restricts cancellation and rescheduling controls strictly to CONFIRMED appointments;
 *       COMPLETED appointments cannot be modified.</li>
 * </ul>
 */
export default function AppointmentCard({ appointment, onCancel, onReschedule }) {
  const [showConfirmCancel, setShowConfirmCancel] = useState(false);
  const [isStaffOverride, setIsStaffOverride] = useState(false);
  const [staffReason, setStaffReason] = useState('');

  if (!appointment) return null;

  const formatDate = (isoString) => {
    if (!isoString) return 'N/A';
    return new Date(isoString).toLocaleString(undefined, {
      dateStyle: 'medium',
      timeStyle: 'short',
    });
  };

  const isConfirmed = appointment.status === 'CONFIRMED';

  const handleConfirmCancelSubmit = (e) => {
    e.preventDefault();
    onCancel(appointment.id, { isStaffOverride, staffReason: isStaffOverride ? staffReason : null });
    setShowConfirmCancel(false);
  };

  return (
    <div className="card shadow-sm border-0 mb-3">
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
            <small className="text-secondary d-block">Slot / Session ID</small>
            <span className="fw-semibold text-dark small">#{appointment.slotId}</span>
          </div>
        </div>

        {appointment.cancelledAt && (
          <div className="mt-2 text-danger small">
            Cancelled on: {formatDate(appointment.cancelledAt)}
          </div>
        )}

        {isConfirmed && (
          <div className="d-flex justify-content-end gap-2 mt-3 pt-2 border-top">
            <button
              type="button"
              className="btn btn-outline-secondary btn-sm px-3 fw-semibold"
              onClick={() => onReschedule(appointment)}
            >
              Reschedule
            </button>
            <button
              type="button"
              className="btn btn-outline-danger btn-sm px-3 fw-semibold"
              onClick={() => setShowConfirmCancel(true)}
            >
              Cancel Appointment
            </button>
          </div>
        )}
      </div>

      {showConfirmCancel && (
        <div className="modal fade show d-block bg-dark bg-opacity-50" tabIndex="-1" role="dialog" aria-modal="true">
          <div className="modal-dialog modal-dialog-centered" role="document">
            <div className="modal-content border-0 shadow">
              <div className="modal-header bg-danger text-white">
                <h5 className="modal-title fw-bold">Confirm Cancellation</h5>
                <button
                  type="button"
                  className="btn-close btn-close-white"
                  aria-label="Close"
                  onClick={() => setShowConfirmCancel(false)}
                ></button>
              </div>
              <div className="modal-body p-4">
                <p className="mb-3">
                  Are you sure you want to cancel Appointment <strong>#{appointment.id}</strong>?
                  This action will release your reserved slot.
                </p>

                <form onSubmit={handleConfirmCancelSubmit}>
                  <div className="form-check mb-3">
                    <input
                      type="checkbox"
                      id={`staffOverrideCheck_${appointment.id}`}
                      className="form-check-input"
                      checked={isStaffOverride}
                      onChange={(e) => setIsStaffOverride(e.target.checked)}
                    />
                    <label htmlFor={`staffOverrideCheck_${appointment.id}`} className="form-check-label small">
                      Apply Staff Override (Required if within 24 hours of session start per MF03-BR-04)
                    </label>
                  </div>

                  {isStaffOverride && (
                    <div className="mb-3">
                      <label htmlFor={`staffReason_${appointment.id}`} className="form-label fw-semibold">
                        Staff Reason <span className="text-danger">*</span>
                      </label>
                      <input
                        type="text"
                        id={`staffReason_${appointment.id}`}
                        className="form-control"
                        value={staffReason}
                        onChange={(e) => setStaffReason(e.target.value)}
                        placeholder="Enter cancellation reason"
                        required={isStaffOverride}
                      />
                    </div>
                  )}

                  <div className="modal-footer px-0 pb-0">
                    <button
                      type="button"
                      className="btn btn-secondary"
                      onClick={() => setShowConfirmCancel(false)}
                    >
                      Keep Appointment
                    </button>
                    <button type="submit" className="btn btn-danger fw-bold">
                      Yes, Cancel Appointment
                    </button>
                  </div>
                </form>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
