import React, { useCallback, useEffect, useRef, useState } from 'react';
import AppointmentStatusBadge from './AppointmentStatusBadge';

function getFocusableElements(container) {
  if (!container) return [];
  return Array.from(container.querySelectorAll(
    'a[href], button:not([disabled]), input:not([disabled]), textarea:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])',
  ));
}

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
  canMutate = true,
}) {
  const [activeDialog, setActiveDialog] = useState(null);
  const [isStaffOverride, setIsStaffOverride] = useState(false);
  const [staffReason, setStaffReason] = useState('');
  const [reversalReason, setReversalReason] = useState('');
  const activeDialogRef = useRef(null);
  const triggerButtonRef = useRef(null);
  const dialogRef = useRef(null);
  const initialFocusRef = useRef(null);
  const panelRef = useRef(null);

  const openDialog = useCallback((dialog, trigger) => {
    triggerButtonRef.current = trigger;
    activeDialogRef.current = dialog;
    setActiveDialog(dialog);
  }, []);

  const closeDialog = useCallback(() => {
    activeDialogRef.current = null;
    setActiveDialog(null);
    const trigger = triggerButtonRef.current;
    const panel = panelRef.current;
    window.setTimeout(() => {
      if (activeDialogRef.current !== null) return;
      if (trigger?.isConnected && !trigger.disabled && typeof trigger.focus === 'function') {
        trigger.focus();
        return;
      }
      if (panel?.isConnected && typeof panel.focus === 'function') {
        panel.focus();
      }
    }, 0);
  }, []);

  useEffect(() => {
    if (!activeDialog) return;

    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        event.preventDefault();
        closeDialog();
        return;
      }
      if (event.key !== 'Tab') return;

      const focusable = getFocusableElements(dialogRef.current);
      if (focusable.length === 0) {
        event.preventDefault();
        dialogRef.current?.focus();
        return;
      }

      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (event.shiftKey && (document.activeElement === first || !dialogRef.current?.contains(document.activeElement))) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && (document.activeElement === last || !dialogRef.current?.contains(document.activeElement))) {
        event.preventDefault();
        first.focus();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [activeDialog, closeDialog]);

  useEffect(() => {
    if (!activeDialog) return;
    (initialFocusRef.current || dialogRef.current)?.focus();
  }, [activeDialog]);

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
    closeDialog();
    setIsStaffOverride(false);
    setStaffReason('');
  };

  const handleReverseSubmit = (e) => {
    e.preventDefault();
    onReverseCheckIn(appointment.id, reversalReason.trim());
    closeDialog();
    setReversalReason('');
  };

  const handleNoShowSubmit = (e) => {
    e.preventDefault();
    onMarkNoShow(appointment.id);
    closeDialog();
  };

  const appointmentHeadingId = `check-in-appointment-title-${appointment.id}`;
  const checkInDialogTitleId = `check-in-dialog-title-${appointment.id}`;
  const reverseDialogTitleId = `reverse-check-in-dialog-title-${appointment.id}`;
  const noShowDialogTitleId = `no-show-dialog-title-${appointment.id}`;

  return (
    <article
      ref={panelRef}
      tabIndex="-1"
      aria-labelledby={appointmentHeadingId}
      className="card shadow-sm border-0 mb-3 hover-shadow transition-all focus-ring focus-ring-primary"
    >
      <div className="card-body p-4">
        <div className="d-flex justify-content-between align-items-start mb-3">
          <div>
            <h3 id={appointmentHeadingId} className="card-title h5 text-primary mb-1 fw-bold">
              Appointment #{appointment.id}
            </h3>
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
        {canMutate && (
          <div className="d-flex flex-wrap justify-content-end gap-2 mt-3 pt-2 border-top">
            {isConfirmed && (
              <>
                <button
                  type="button"
                  className="btn btn-outline-danger btn-sm px-3 fw-semibold"
                  disabled={isProcessing}
                  aria-label={`Mark appointment ${appointment.id} as no-show`}
                  onClick={(event) => openDialog('no-show', event.currentTarget)}
                >
                  Mark No-Show
                </button>
                <button
                  type="button"
                  className="btn btn-success btn-sm px-4 fw-bold shadow-sm"
                  disabled={isProcessing}
                  aria-label={`Check in appointment ${appointment.id}`}
                  onClick={(event) => openDialog('check-in', event.currentTarget)}
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
                aria-label={`Reverse check-in for appointment ${appointment.id}`}
                onClick={(event) => openDialog('reverse', event.currentTarget)}
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
        )}
      </div>

      {/* Check-In Modal with optional Staff Override */}
      {canMutate && activeDialog === 'check-in' && (
        <div
          ref={dialogRef}
          className="modal fade show d-block bg-dark bg-opacity-50"
          tabIndex="-1"
          role="dialog"
          aria-modal="true"
          aria-labelledby={checkInDialogTitleId}
        >
          <div className="modal-dialog modal-dialog-centered" role="document">
            <div className="modal-content border-0 shadow">
              <div className="modal-header bg-success text-white">
                <h2 id={checkInDialogTitleId} className="modal-title h5 fw-bold">Confirm Check-In</h2>
                <button
                  type="button"
                  className="btn-close btn-close-white"
                  aria-label="Close check-in dialog"
                  onClick={closeDialog}
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
                    ref={initialFocusRef}
                    type="button"
                    className="btn btn-secondary"
                    onClick={closeDialog}
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
      {canMutate && activeDialog === 'reverse' && (
        <div
          ref={dialogRef}
          className="modal fade show d-block bg-dark bg-opacity-50"
          tabIndex="-1"
          role="dialog"
          aria-modal="true"
          aria-labelledby={reverseDialogTitleId}
        >
          <div className="modal-dialog modal-dialog-centered" role="document">
            <div className="modal-content border-0 shadow">
              <div className="modal-header bg-warning text-dark">
                <h2 id={reverseDialogTitleId} className="modal-title h5 fw-bold">Confirm Reverse Check-In</h2>
                <button
                  type="button"
                  className="btn-close"
                  aria-label="Close reverse check-in dialog"
                  onClick={closeDialog}
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
                  <div className="mt-3">
                    <label htmlFor={`reversalReason_${appointment.id}`} className="form-label fw-semibold">
                      Reversal Reason <span className="text-danger">*</span>
                    </label>
                    <textarea
                      id={`reversalReason_${appointment.id}`}
                      className="form-control"
                      value={reversalReason}
                      onChange={(event) => setReversalReason(event.target.value)}
                      maxLength={500}
                      required
                    />
                  </div>
                </div>
                <div className="modal-footer">
                  <button
                    ref={initialFocusRef}
                    type="button"
                    className="btn btn-secondary"
                    onClick={closeDialog}
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
      {canMutate && activeDialog === 'no-show' && (
        <div
          ref={dialogRef}
          className="modal fade show d-block bg-dark bg-opacity-50"
          tabIndex="-1"
          role="dialog"
          aria-modal="true"
          aria-labelledby={noShowDialogTitleId}
        >
          <div className="modal-dialog modal-dialog-centered" role="document">
            <div className="modal-content border-0 shadow">
              <div className="modal-header bg-danger text-white">
                <h2 id={noShowDialogTitleId} className="modal-title h5 fw-bold">Confirm Mark No-Show</h2>
                <button
                  type="button"
                  className="btn-close btn-close-white"
                  aria-label="Close no-show dialog"
                  onClick={closeDialog}
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
                    ref={initialFocusRef}
                    type="button"
                    className="btn btn-secondary"
                    onClick={closeDialog}
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
    </article>
  );
}
