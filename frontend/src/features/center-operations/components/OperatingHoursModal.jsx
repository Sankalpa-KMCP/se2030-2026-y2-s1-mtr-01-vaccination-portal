import React, { useState, useEffect } from 'react';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';

const DAYS_OF_WEEK = [
  { value: 'MONDAY', label: 'Monday' },
  { value: 'TUESDAY', label: 'Tuesday' },
  { value: 'WEDNESDAY', label: 'Wednesday' },
  { value: 'THURSDAY', label: 'Thursday' },
  { value: 'FRIDAY', label: 'Friday' },
  { value: 'SATURDAY', label: 'Saturday' },
  { value: 'SUNDAY', label: 'Sunday' },
];

function normalizeTimeForInput(timeStr) {
  if (!timeStr) return '';
  return timeStr.length > 5 ? timeStr.substring(0, 5) : timeStr;
}

export default function OperatingHoursModal({
  show,
  mode = 'create', // 'create' | 'edit' | 'end-date'
  center,
  operatingHour = null,
  loading = false,
  apiError = null,
  onClose,
  onSubmit,
}) {
  const isEndDateMode = mode === 'end-date';
  const isEditMode = mode === 'edit';

  const [dayOfWeek, setDayOfWeek] = useState('MONDAY');
  const [opening, setOpening] = useState('08:00');
  const [closing, setClosing] = useState('17:00');
  const [effectiveFrom, setEffectiveFrom] = useState('');
  const [effectiveTo, setEffectiveTo] = useState('');
  const [endDate, setEndDate] = useState('');
  const [clientError, setClientError] = useState(null);

  useEffect(() => {
    if (show) {
      const today = new Date().toISOString().substring(0, 10);

      if (isEndDateMode && operatingHour) {
        setEndDate(operatingHour.effectiveTo || today);
      } else if (isEditMode && operatingHour) {
        setDayOfWeek(operatingHour.dayOfWeek || 'MONDAY');
        setOpening(normalizeTimeForInput(operatingHour.opening) || '08:00');
        setClosing(normalizeTimeForInput(operatingHour.closing) || '17:00');
        setEffectiveFrom(operatingHour.effectiveFrom || today);
        setEffectiveTo(operatingHour.effectiveTo || '');
      } else {
        setDayOfWeek('MONDAY');
        setOpening('08:00');
        setClosing('17:00');
        setEffectiveFrom(today);
        setEffectiveTo('');
      }
      setClientError(null);
    }
  }, [show, mode, isEndDateMode, isEditMode, operatingHour]);

  if (!show) {
    return null;
  }

  const handleSubmit = (e) => {
    e.preventDefault();
    setClientError(null);

    if (isEndDateMode) {
      if (!endDate) {
        setClientError('End date is required.');
        return;
      }
      if (operatingHour?.effectiveFrom && endDate < operatingHour.effectiveFrom) {
        setClientError(`End date (${endDate}) cannot be earlier than effective start date (${operatingHour.effectiveFrom}).`);
        return;
      }
      if (operatingHour?.effectiveTo && endDate > operatingHour.effectiveTo) {
        setClientError(`End date (${endDate}) cannot extend beyond current effective end date (${operatingHour.effectiveTo}). Use Edit Schedule for extensions.`);
        return;
      }

      onSubmit({ endDate });
      return;
    }

    if (!dayOfWeek) {
      setClientError('Day of week is required.');
      return;
    }
    if (!opening) {
      setClientError('Opening time is required.');
      return;
    }
    if (!closing) {
      setClientError('Closing time is required.');
      return;
    }
    if (opening >= closing) {
      setClientError('Opening time must be strictly before closing time.');
      return;
    }
    if (!effectiveFrom) {
      setClientError('Effective from date is required.');
      return;
    }
    if (effectiveTo && effectiveTo < effectiveFrom) {
      setClientError('Effective to date cannot be earlier than effective from date.');
      return;
    }

    onSubmit({
      dayOfWeek,
      opening: opening.length === 5 ? `${opening}:00` : opening,
      closing: closing.length === 5 ? `${closing}:00` : closing,
      effectiveFrom,
      effectiveTo: effectiveTo.trim() !== '' ? effectiveTo : null,
    });
  };

  const getModalTitle = () => {
    if (isEndDateMode) return 'End Date Operating Hours Shift';
    if (isEditMode) return 'Edit Operating Hours Shift';
    return 'Add Operating Hours Shift';
  };

  return (
    <div className="modal show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
      <div className="modal-dialog modal-dialog-centered">
        <div className="modal-content shadow-lg border-0">
          <div className="modal-header bg-primary text-white">
            <h5 className="modal-title">
              <i className="bi bi-clock-history me-2"></i>
              {getModalTitle()}
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

              {isEndDateMode ? (
                <>
                  <div className="card bg-light border-0 mb-3">
                    <div className="card-body p-3">
                      <h6 className="fw-bold text-dark mb-2">Shift Details</h6>
                      <div className="row g-2 small text-muted">
                        <div className="col-6">
                          Day: <span className="fw-semibold text-dark">{operatingHour?.dayOfWeek}</span>
                        </div>
                        <div className="col-6">
                          Hours: <span className="font-monospace fw-semibold text-dark">{normalizeTimeForInput(operatingHour?.opening)} - {normalizeTimeForInput(operatingHour?.closing)}</span>
                        </div>
                        <div className="col-6">
                          Effective From: <span className="fw-semibold text-dark">{operatingHour?.effectiveFrom}</span>
                        </div>
                        <div className="col-6">
                          Current End: <span className="fw-semibold text-dark">{operatingHour?.effectiveTo || 'Ongoing'}</span>
                        </div>
                      </div>
                    </div>
                  </div>

                  <div className="mb-3">
                    <label htmlFor="endDateInput" className="form-label fw-semibold text-dark small">
                      New Effective End Date <span className="text-danger">*</span>
                    </label>
                    <input
                      type="date"
                      id="endDateInput"
                      className="form-control"
                      value={endDate}
                      min={operatingHour?.effectiveFrom}
                      max={operatingHour?.effectiveTo || undefined}
                      onChange={(e) => setEndDate(e.target.value)}
                      disabled={loading}
                      required
                    />
                    <div className="form-text small text-muted">
                      Setting this end date deactivates this weekly operating shift on and after the specified date. Historical sessions remain intact.
                    </div>
                  </div>
                </>
              ) : (
                <div className="row g-3">
                  <div className="col-12">
                    <label htmlFor="dayOfWeekSelect" className="form-label fw-semibold text-dark small">
                      Day of Week <span className="text-danger">*</span>
                    </label>
                    <select
                      id="dayOfWeekSelect"
                      className="form-select"
                      value={dayOfWeek}
                      onChange={(e) => setDayOfWeek(e.target.value)}
                      disabled={loading}
                      required
                    >
                      {DAYS_OF_WEEK.map((d) => (
                        <option key={d.value} value={d.value}>
                          {d.label}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="col-md-6">
                    <label htmlFor="shiftOpeningTime" className="form-label fw-semibold text-dark small">
                      Opening Time <span className="text-danger">*</span>
                    </label>
                    <input
                      type="time"
                      id="shiftOpeningTime"
                      className="form-control font-monospace"
                      value={opening}
                      onChange={(e) => setOpening(e.target.value)}
                      disabled={loading}
                      required
                    />
                  </div>

                  <div className="col-md-6">
                    <label htmlFor="shiftClosingTime" className="form-label fw-semibold text-dark small">
                      Closing Time <span className="text-danger">*</span>
                    </label>
                    <input
                      type="time"
                      id="shiftClosingTime"
                      className="form-control font-monospace"
                      value={closing}
                      onChange={(e) => setClosing(e.target.value)}
                      disabled={loading}
                      required
                    />
                  </div>

                  <div className="col-md-6">
                    <label htmlFor="effectiveFromInput" className="form-label fw-semibold text-dark small">
                      Effective From <span className="text-danger">*</span>
                    </label>
                    <input
                      type="date"
                      id="effectiveFromInput"
                      className="form-control"
                      value={effectiveFrom}
                      onChange={(e) => setEffectiveFrom(e.target.value)}
                      disabled={loading}
                      required
                    />
                    <div className="form-text small text-muted">First date shift is operational.</div>
                  </div>

                  <div className="col-md-6">
                    <label htmlFor="effectiveToInput" className="form-label fw-semibold text-dark small">
                      Effective To (Optional)
                    </label>
                    <input
                      type="date"
                      id="effectiveToInput"
                      className="form-control"
                      value={effectiveTo}
                      min={effectiveFrom || undefined}
                      onChange={(e) => setEffectiveTo(e.target.value)}
                      disabled={loading}
                    />
                    <div className="form-text small text-muted">Leave empty for ongoing shift.</div>
                  </div>
                </div>
              )}
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
                className={`btn fw-semibold px-4 ${isEndDateMode ? 'btn-warning text-dark' : 'btn-primary'}`}
                disabled={loading}
              >
                {loading ? (
                  <>
                    <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                    Saving Shift...
                  </>
                ) : isEndDateMode ? (
                  'Confirm End Date'
                ) : isEditMode ? (
                  'Save Schedule Changes'
                ) : (
                  'Save Operating Hours'
                )}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
