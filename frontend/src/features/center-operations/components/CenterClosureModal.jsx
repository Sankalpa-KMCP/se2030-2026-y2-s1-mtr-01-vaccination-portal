import React, { useState, useEffect } from 'react';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';

function toLocalDatetimeInputString(isoString) {
  if (!isoString) return '';
  try {
    const date = new Date(isoString);
    if (isNaN(date.getTime())) return '';
    // Format as YYYY-MM-DDTHH:mm in local time
    const pad = (n) => String(n).padStart(2, '0');
    const year = date.getFullYear();
    const month = pad(date.getMonth() + 1);
    const day = pad(date.getDate());
    const hours = pad(date.getHours());
    const minutes = pad(date.getMinutes());
    return `${year}-${month}-${day}T${hours}:${minutes}`;
  } catch {
    return '';
  }
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
      if (isEdit && closure) {
        setStartAt(toLocalDatetimeInputString(closure.startAt));
        setEndAt(toLocalDatetimeInputString(closure.endAt));
        setReason(closure.reason || '');
      } else {
        const now = new Date();
        const tomorrow = new Date(now.getTime() + 24 * 60 * 60 * 1000);
        tomorrow.setHours(8, 0, 0, 0);
        const tomorrowEnd = new Date(now.getTime() + 24 * 60 * 60 * 1000);
        tomorrowEnd.setHours(17, 0, 0, 0);

        setStartAt(toLocalDatetimeInputString(tomorrow.toISOString()));
        setEndAt(toLocalDatetimeInputString(tomorrowEnd.toISOString()));
        setReason('');
      }
      setClientError(null);
    }
  }, [show, isEdit, closure]);

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

    const startDate = new Date(startAt);
    const endDate = new Date(endAt);

    if (isNaN(startDate.getTime()) || isNaN(endDate.getTime())) {
      setClientError('Please enter valid dates and times.');
      return;
    }

    if (startDate >= endDate) {
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
      startAt: startDate.toISOString(),
      endAt: endDate.toISOString(),
      reason: trimmedReason,
    });
  };

  return (
    <div className="modal show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
      <div className="modal-dialog modal-dialog-centered">
        <div className="modal-content shadow-lg border-0">
          <div className="modal-header bg-primary text-white">
            <h5 className="modal-title">
              <i className="bi bi-calendar-x me-2"></i>
              {isEdit ? 'Edit Center Closure' : 'Schedule Temporary Closure'}
            </h5>
            <button
              type="button"
              className="btn-close btn-close-white"
              aria-label="Close"
              onClick={onClose}
              disabled={loading}
            ></button>
          </div>

          <form onSubmit={handleSubmit} noValidate>
            <div className="modal-body p-4">
              {center && (
                <div className="alert alert-light border small text-muted mb-3 py-2 px-3">
                  <span className="fw-semibold text-dark">Center:</span> {center.name}{' '}
                  <span className="badge bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle ms-1 font-monospace">
                    {center.code}
                  </span>
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
                    Exact timestamp when center closure commences.
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
                    Exact timestamp when center resumes normal operations.
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
