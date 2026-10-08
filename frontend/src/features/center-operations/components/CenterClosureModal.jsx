import React, { useState, useEffect } from 'react';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import {
  localToUtcIso,
  utcToLocalDateAndTime,
  isValidIanaTimezone,
} from '../../../shared/time/timezoneUtils';

function toTimezoneDatetimeInputString(isoString, timeZone = 'UTC') {
  if (!isoString) return '';
  const { date, time } = utcToLocalDateAndTime(isoString, timeZone);
  if (!date || !time) return '';
  return `${date}T${time}`;
}

export default function CenterClosureModal({
  show,
  mode = 'create', // 'create' | 'edit'
  center,
  closure = null,
  loading = false,
  apiError = null,
  onClose,
  onSubmit,
}) {
  const isEdit = mode === 'edit';

  const [startAt, setStartAt] = useState('');
  const [endAt, setEndAt] = useState('');
  const [reason, setReason] = useState('');
  const [clientError, setClientError] = useState(null);

  useEffect(() => {
    if (show) {
      const tz = (center?.timezone && isValidIanaTimezone(center.timezone))
        ? center.timezone.trim()
        : 'UTC';

      if (isEdit && closure) {
        setStartAt(toTimezoneDatetimeInputString(closure.startAt, tz));
        setEndAt(toTimezoneDatetimeInputString(closure.endAt, tz));
        setReason(closure.reason || '');
      } else {
        const now = new Date();
        const tomorrow = new Date(now.getTime() + 24 * 60 * 60 * 1000);
        const { date: tomorrowDate } = utcToLocalDateAndTime(tomorrow.toISOString(), tz);
        const defaultDate = tomorrowDate || new Date().toISOString().split('T')[0];

        setStartAt(`${defaultDate}T08:00`);
        setEndAt(`${defaultDate}T17:00`);
        setReason('');
      }
      setClientError(null);
    }
  }, [show, isEdit, closure, center]);

  if (!show) {
    return null;
  }

  const handleSubmit = (e) => {
    e.preventDefault();
    setClientError(null);

    if (!startAt) {
      setClientError('Closure start date and time is required.');
      return;
    }
    if (!endAt) {
      setClientError('Closure end date and time is required.');
      return;
    }

    const tz = (center?.timezone && isValidIanaTimezone(center.timezone))
      ? center.timezone.trim()
      : 'UTC';

    const startParts = startAt.split('T');
    const endParts = endAt.split('T');

    if (startParts.length !== 2 || endParts.length !== 2) {
      setClientError('Please enter valid dates and times.');
      return;
    }

    const [startDateStr, startTimeStr] = startParts;
    const [endDateStr, endTimeStr] = endParts;

    let startUtcIso;
    let endUtcIso;

    try {
      startUtcIso = localToUtcIso(startDateStr, startTimeStr, tz);
    } catch (err) {
      setClientError(`Closure start time error: ${err.message}`);
      return;
    }

    try {
      endUtcIso = localToUtcIso(endDateStr, endTimeStr, tz);
    } catch (err) {
      setClientError(`Closure end time error: ${err.message}`);
      return;
    }

    const startMs = new Date(startUtcIso).getTime();
    const endMs = new Date(endUtcIso).getTime();

    if (startMs >= endMs) {
      setClientError('Closure start time must be strictly before closure end time.');
      return;
    }

    const trimmedReason = reason.trim();
    if (!trimmedReason) {
      setClientError('Closure justification reason is required.');
      return;
    }
    if (trimmedReason.length > 255) {
      setClientError('Closure reason must not exceed 255 characters.');
      return;
    }

    onSubmit({
      startAt: startUtcIso,
      endAt: endUtcIso,
      reason: trimmedReason,
    });
  };

  return (
    <div className="modal show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
      <div className="modal-dialog modal-dialog-centered">
        <div className="modal-content shadow-lg border-0">
          <div className="modal-header bg-light">
            <h5 className="modal-title">
              <i className="bi bi-calendar-x me-2"></i>
              {isEdit ? 'Edit Center Closure' : 'Schedule Temporary Closure'}
            </h5>
            <button
              type="button"
              className="btn-close"
              aria-label="Close"
              onClick={onClose}
              disabled={loading}
            ></button>
          </div>

          <form onSubmit={handleSubmit} noValidate>
            <div className="modal-body p-4">
              {center && (
                <div className="alert alert-light border small text-muted mb-3 py-2 px-3 d-flex justify-content-between align-items-center flex-wrap gap-2">
                  <div>
                    <span className="fw-semibold text-dark">Center:</span> {center.name}{' '}
                    <span className="badge bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle ms-1 font-monospace">
                      {center.code}
                    </span>
                  </div>
                  {center.timezone && (
                    <div>
                      <span className="fw-semibold text-dark">Timezone:</span>{' '}
                      <code className="text-secondary">{center.timezone}</code>
                    </div>
                  )}
                </div>
              )}

              <ApiErrorAlert error={clientError || apiError} />

              <div className="row g-3">
                <div className="col-12">
                  <label htmlFor="closureStartAtInput" className="form-label fw-semibold text-dark small">
                    Closure Start Date & Time <span className="text-danger">*</span>
                  </label>
                  <input
                    type="datetime-local"
                    id="closureStartAtInput"
                    className="form-control font-monospace"
                    value={startAt}
                    onChange={(e) => setStartAt(e.target.value)}
                    disabled={loading}
                    required
                  />
                  <div className="form-text small text-muted">
                    Exact timestamp when center closure commences ({center?.timezone || 'center timezone'}).
                  </div>
                </div>

                <div className="col-12">
                  <label htmlFor="closureEndAtInput" className="form-label fw-semibold text-dark small">
                    Closure End Date & Time <span className="text-danger">*</span>
                  </label>
                  <input
                    type="datetime-local"
                    id="closureEndAtInput"
                    className="form-control font-monospace"
                    value={endAt}
                    onChange={(e) => setEndAt(e.target.value)}
                    disabled={loading}
                    required
                  />
                  <div className="form-text small text-muted">
                    Exact timestamp when center resumes normal operations ({center?.timezone || 'center timezone'}).
                  </div>
                </div>

                <div className="col-12">
                  <label htmlFor="closureReasonInput" className="form-label fw-semibold text-dark small">
                    Reason / Administrative Justification <span className="text-danger">*</span>
                  </label>
                  <textarea
                    id="closureReasonInput"
                    className="form-control"
                    rows={3}
                    placeholder="e.g. Public Holiday Closure, Facility Sanitization, Deep Freeze Maintenance..."
                    maxLength={255}
                    value={reason}
                    onChange={(e) => setReason(e.target.value)}
                    disabled={loading}
                    required
                  />
                  <div className="form-text small text-muted">
                    Clear reason for closure (maximum 255 characters).
                  </div>
                </div>
              </div>
            </div>

            <div className="modal-footer bg-light">
              <button
                type="button"
                className="btn btn-outline-secondary"
                onClick={onClose}
                disabled={loading}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="btn btn-primary fw-semibold px-4"
                disabled={loading}
              >
                {loading ? (
                  <>
                    <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                    Saving Closure...
                  </>
                ) : isEdit ? (
                  'Save Closure Changes'
                ) : (
                  'Schedule Closure'
                )}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
