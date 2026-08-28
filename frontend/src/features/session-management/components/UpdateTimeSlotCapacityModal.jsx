import React, { useState, useEffect } from 'react';
import { updateTimeSlotCapacity, formatTimeInTimezone } from '../api/sessionApi';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';

export default function UpdateTimeSlotCapacityModal({
  isOpen,
  onClose,
  session,
  slot,
  onCapacityUpdated,
}) {
  const [capacity, setCapacity] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  const tz = session?.scheduleTimezone || 'UTC';

  useEffect(() => {
    if (isOpen && slot) {
      setCapacity(String(slot.capacity || ''));
      setError(null);
      setSubmitting(false);
    } else {
      setCapacity('');
      setError(null);
      setSubmitting(false);
    }
  }, [isOpen, slot]);

  if (!isOpen || !session || !slot) return null;

  const reservedCount = slot.reservedCount || 0;
  const minAllowed = Math.max(1, reservedCount);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);

    const capNum = parseInt(capacity, 10);
    if (isNaN(capNum) || capNum <= 0) {
      setError(new Error('Capacity must be a positive integer (at least 1)'));
      return;
    }

    if (capNum < reservedCount) {
      setError(
        new Error(
          `Cannot reduce slot capacity (${capNum}) below existing reserved bookings (${reservedCount})`
        )
      );
      return;
    }

    setSubmitting(true);

    try {
      const updated = await updateTimeSlotCapacity(session.id, slot.id, { capacity: capNum });
      setSubmitting(false);
      if (onCapacityUpdated) {
        onCapacityUpdated(updated);
      }
      onClose();
    } catch (err) {
      setSubmitting(false);
      setError(err);
    }
  };

  return (
    <div
      className="modal fade show d-block"
      tabIndex="-1"
      role="dialog"
      aria-labelledby="updateCapacityModalTitle"
      aria-modal="true"
      style={{ backgroundColor: 'rgba(0, 0, 0, 0.5)' }}
    >
      <div className="modal-dialog modal-dialog-centered" role="document">
        <div className="modal-content shadow">
          <div className="modal-header bg-primary text-white">
            <h5 className="modal-title" id="updateCapacityModalTitle">
              Edit Time Slot Capacity
            </h5>
            <button
              type="button"
              className="btn-close btn-close-white"
              aria-label="Close"
              onClick={onClose}
              disabled={submitting}
            />
          </div>

          <form onSubmit={handleSubmit}>
            <div className="modal-body p-4">
              {error && (
                <div className="mb-3">
                  <ApiErrorAlert error={error} onDismiss={() => setError(null)} />
                </div>
              )}

              <div className="card bg-light border-0 p-3 mb-3">
                <div className="row g-2 small">
                  <div className="col-md-6">
                    <span className="text-muted d-block">Time Slot Window ({tz})</span>
                    <strong className="text-dark">
                      {formatTimeInTimezone(slot.startTime, tz)} – {formatTimeInTimezone(slot.endTime, tz)}
                    </strong>
                  </div>
                  <div className="col-md-3">
                    <span className="text-muted d-block">Current Capacity</span>
                    <strong className="text-dark">{slot.capacity}</strong>
                  </div>
                  <div className="col-md-3">
                    <span className="text-muted d-block">Reserved Bookings</span>
                    <span className="badge bg-warning text-dark">{reservedCount}</span>
                  </div>
                </div>
              </div>

              <div className="mb-3">
                <label htmlFor="editSlotCapacity" className="form-label small fw-semibold text-secondary">
                  New Bookable Capacity <span className="text-danger">*</span>
                </label>
                <input
                  type="number"
                  id="editSlotCapacity"
                  className="form-control"
                  value={capacity}
                  min={minAllowed}
                  step="1"
                  onChange={(e) => setCapacity(e.target.value)}
                  required
                  disabled={submitting}
                />
                <div className="form-text text-muted small mt-1">
                  {reservedCount > 0 ? (
                    <span className="text-danger fw-semibold">
                      Protected floor: Capacity cannot be set below {reservedCount} active reserved appointments.
                    </span>
                  ) : (
                    'Must be a positive integer greater than or equal to 1.'
                  )}
                </div>
              </div>
            </div>

            <div className="modal-footer bg-light">
              <button
                type="button"
                className="btn btn-secondary"
                onClick={onClose}
                disabled={submitting}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="btn btn-primary fw-semibold"
                disabled={submitting}
              >
                {submitting ? (
                  <>
                    <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true" />
                    Updating Capacity...
                  </>
                ) : (
                  'Update Capacity'
                )}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
