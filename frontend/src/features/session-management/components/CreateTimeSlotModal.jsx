import React, { useState, useEffect } from 'react';
import { createTimeSlot, localToUtcIso, utcToLocalDateAndTime } from '../api/sessionApi';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';

export default function CreateTimeSlotModal({
  isOpen,
  onClose,
  session,
  onTimeSlotCreated,
}) {
  const [startTime, setStartTime] = useState('');
  const [endTime, setEndTime] = useState('');
  const [capacity, setCapacity] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  const tz = session?.scheduleTimezone || 'UTC';

  useEffect(() => {
    if (isOpen && session) {
      const sessionTimes = utcToLocalDateAndTime(session.startAt, tz);
      const sessionEndTimes = utcToLocalDateAndTime(session.endAt, tz);
      setStartTime(sessionTimes.time || '');
      setEndTime(sessionEndTimes.time || '');
      setCapacity('10');
      setError(null);
      setSubmitting(false);
    } else {
      setStartTime('');
      setEndTime('');
      setCapacity('');
      setError(null);
      setSubmitting(false);
    }
  }, [isOpen, session, tz]);

  if (!isOpen || !session) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);

    if (!startTime || !endTime) {
      setError(new Error('Start and end times are required'));
      return;
    }

    if (startTime >= endTime) {
      setError(new Error('Slot start time must be strictly before end time'));
      return;
    }

    const capNum = parseInt(capacity, 10);
    if (isNaN(capNum) || capNum <= 0) {
      setError(new Error('Capacity must be a positive number (at least 1)'));
      return;
    }

    setSubmitting(true);

    try {
      const sessionLocal = utcToLocalDateAndTime(session.startAt, tz);
      const sessionDate = sessionLocal.date;

      const startUtcIso = localToUtcIso(sessionDate, startTime, tz);
      const endUtcIso = localToUtcIso(sessionDate, endTime, tz);

      const payload = {
        startTime: startUtcIso,
        endTime: endUtcIso,
        capacity: capNum,
      };

      const created = await createTimeSlot(session.id, payload);
      setSubmitting(false);
      if (onTimeSlotCreated) {
        onTimeSlotCreated(created);
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
      aria-labelledby="createTimeSlotModalTitle"
      aria-modal="true"
      style={{ backgroundColor: 'rgba(0, 0, 0, 0.5)' }}
    >
      <div className="modal-dialog modal-dialog-centered" role="document">
        <div className="modal-content shadow">
          <div className="modal-header bg-light">
            <h5 className="modal-title" id="createTimeSlotModalTitle">
              Add Operational Time Slot
            </h5>
            <button
              type="button"
              className="btn-close"
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

              <div className="alert alert-light border small text-muted mb-3 py-2">
                <div>
                  <strong>Session ID:</strong> #{session.id} &nbsp;|&nbsp;{' '}
                  <strong>Timezone:</strong> <code>{tz}</code>
                </div>
              </div>

              <div className="row g-3">
                <div className="col-md-6">
                  <label htmlFor="slotStartTime" className="form-label small fw-semibold text-secondary">
                    Start Time ({tz}) <span className="text-danger">*</span>
                  </label>
                  <input
                    type="time"
                    id="slotStartTime"
                    className="form-control"
                    value={startTime}
                    onChange={(e) => setStartTime(e.target.value)}
                    required
                    disabled={submitting}
                  />
                </div>

                <div className="col-md-6">
                  <label htmlFor="slotEndTime" className="form-label small fw-semibold text-secondary">
                    End Time ({tz}) <span className="text-danger">*</span>
                  </label>
                  <input
                    type="time"
                    id="slotEndTime"
                    className="form-control"
                    value={endTime}
                    onChange={(e) => setEndTime(e.target.value)}
                    required
                    disabled={submitting}
                  />
                </div>

                <div className="col-12">
                  <label htmlFor="slotCapacity" className="form-label small fw-semibold text-secondary">
                    Bookable Capacity <span className="text-danger">*</span>
                  </label>
                  <input
                    type="number"
                    id="slotCapacity"
                    className="form-control"
                    value={capacity}
                    min="1"
                    step="1"
                    onChange={(e) => setCapacity(e.target.value)}
                    placeholder="e.g. 20"
                    required
                    disabled={submitting}
                  />
                  <div className="form-text text-muted small">
                    Positive integer representing the maximum appointment capacity for this interval. Total session slots cannot exceed approved capacity plan.
                  </div>
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
                    Creating Slot...
                  </>
                ) : (
                  'Create Time Slot'
                )}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
