import React, { useState, useEffect } from 'react';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import LoadingSpinner from '../../../components/common/LoadingSpinner';

/**
 * Modal for creating, updating, ending, or cancelling a campaign-center assignment.
 */
export default function CenterAssignmentModal({
  mode, // 'CREATE' | 'UPDATE_FUTURE' | 'END_ACTIVE' | 'CANCEL_CONFIRM'
  assignment, // Selected assignment for update/end/cancel
  activeCenters = [],
  loadingCenters = false,
  onSubmit,
  onCancel,
  loading = false,
  error = null,
}) {
  const [selectedCenterId, setSelectedCenterId] = useState('');
  const [activeFrom, setActiveFrom] = useState('');
  const [activeTo, setActiveTo] = useState('');
  const [clientError, setClientError] = useState(null);

  const todayStr = new Date().toISOString().split('T')[0];

  useEffect(() => {
    setClientError(null);
    if (mode === 'CREATE') {
      setSelectedCenterId(activeCenters.length > 0 ? String(activeCenters[0].id) : '');
      setActiveFrom(todayStr);
      setActiveTo('');
    } else if (mode === 'UPDATE_FUTURE' && assignment) {
      setSelectedCenterId(String(assignment.centerId));
      setActiveFrom(assignment.activeFrom || '');
      setActiveTo(assignment.activeTo || '');
    } else if (mode === 'END_ACTIVE' && assignment) {
      setSelectedCenterId(String(assignment.centerId));
      setActiveFrom(assignment.activeFrom || '');
      // End date must be after today
      const tomorrow = new Date();
      tomorrow.setDate(tomorrow.getDate() + 1);
      setActiveTo(tomorrow.toISOString().split('T')[0]);
    }
  }, [mode, assignment, activeCenters, todayStr]);

  const handleSubmit = (e) => {
    e.preventDefault();
    setClientError(null);

    if (mode === 'CREATE') {
      if (!selectedCenterId) {
        setClientError('Please select a vaccination center.');
        return;
      }
      if (!activeFrom) {
        setClientError('Active From date is required.');
        return;
      }
      if (activeTo && activeTo <= activeFrom) {
        setClientError('Active To date must be strictly after Active From date.');
        return;
      }

      onSubmit({
        centerId: Number(selectedCenterId),
        activeFrom,
        activeTo: activeTo.trim() ? activeTo : null,
      });
    } else if (mode === 'UPDATE_FUTURE') {
      if (!activeFrom) {
        setClientError('Active From date is required.');
        return;
      }
      if (activeFrom <= todayStr) {
        setClientError('Updated assignment start date must be strictly in the future (after today).');
        return;
      }
      if (activeTo && activeTo <= activeFrom) {
        setClientError('Active To date must be strictly after Active From date.');
        return;
      }

      onSubmit({
        activeFrom,
        activeTo: activeTo.trim() ? activeTo : null,
      });
    } else if (mode === 'END_ACTIVE') {
      if (!activeTo) {
        setClientError('Assignment End Date is required.');
        return;
      }
      if (activeTo <= todayStr) {
        setClientError('Assignment end date must be strictly after today.');
        return;
      }
      if (assignment?.activeFrom && activeTo <= assignment.activeFrom) {
        setClientError('Assignment end date must be after the assignment start date.');
        return;
      }

      onSubmit({
        activeTo,
      });
    } else if (mode === 'CANCEL_CONFIRM') {
      onSubmit({});
    }
  };

  const getModalTitle = () => {
    switch (mode) {
      case 'CREATE': return 'Assign Vaccination Center';
      case 'UPDATE_FUTURE': return 'Edit Future Center Assignment Period';
      case 'END_ACTIVE': return 'End Active Center Assignment Early';
      case 'CANCEL_CONFIRM': return 'Cancel Future Center Assignment';
      default: return 'Center Assignment';
    }
  };

  return (
    <div
      className="modal fade show d-block"
      tabIndex="-1"
      role="dialog"
      style={{ backgroundColor: 'rgba(0, 0, 0, 0.5)', zIndex: 1060 }}
    >
      <div className="modal-dialog modal-dialog-centered" role="document">
        <div className="modal-content shadow border-0">
          <div className="modal-header bg-light">
            <h5 className="modal-title fw-bold fs-6 mb-0">{getModalTitle()}</h5>
            <button
              type="button"
              className="btn-close"
              aria-label="Close"
              onClick={onCancel}
              disabled={loading}
            />
          </div>

          <form onSubmit={handleSubmit}>
            <div className="modal-body p-4">
              <ApiErrorAlert error={error || clientError} />

              {/* Mode: CANCEL_CONFIRM */}
              {mode === 'CANCEL_CONFIRM' && assignment && (
                <div>
                  <div className="alert alert-warning border small mb-3">
                    <strong>Warning:</strong> You are about to cancel the scheduled assignment for{' '}
                    <strong>{assignment.centerName} ({assignment.centerCode})</strong>.
                  </div>
                  <p className="small text-muted mb-0">
                    Scheduled Period: <strong>{assignment.activeFrom}</strong> to{' '}
                    <strong>{assignment.activeTo || 'Open-ended'}</strong>.
                    <br />
                    Once cancelled, this record will be marked as <code>CANCELLED</code> in the history audit log.
                  </p>
                </div>
              )}

              {/* Mode: END_ACTIVE */}
              {mode === 'END_ACTIVE' && assignment && (
                <div>
                  <div className="mb-3 p-3 bg-light rounded border small">
                    <div><strong>Center: </strong>{assignment.centerName} ({assignment.centerCode})</div>
                    <div><strong>Active From: </strong>{assignment.activeFrom}</div>
                    <div><strong>Current End: </strong>{assignment.activeTo || 'Open-ended'}</div>
                  </div>

                  <div className="mb-3">
                    <label htmlFor="endActiveTo" className="form-label small fw-semibold">
                      New End Date (Exclusive) <span className="text-danger">*</span>
                    </label>
                    <input
                      type="date"
                      id="endActiveTo"
                      className="form-control form-control-sm"
                      value={activeTo}
                      min={todayStr}
                      onChange={(e) => setActiveTo(e.target.value)}
                      required
                      disabled={loading}
                    />
                    <div className="form-text small text-muted">
                      Must be strictly after today ({todayStr}). Existing sessions on or after this date must not conflict.
                    </div>
                  </div>
                </div>
              )}

              {/* Mode: CREATE or UPDATE_FUTURE */}
              {(mode === 'CREATE' || mode === 'UPDATE_FUTURE') && (
                <div>
                  {mode === 'CREATE' ? (
                    <div className="mb-3">
                      <label htmlFor="centerSelect" className="form-label small fw-semibold">
                        Vaccination Center <span className="text-danger">*</span>
                      </label>
                      {loadingCenters ? (
                        <div className="text-muted small py-2">
                          <LoadingSpinner message="Loading active centers..." />
                        </div>
                      ) : activeCenters.length > 0 ? (
                        <select
                          id="centerSelect"
                          className="form-select form-select-sm"
                          value={selectedCenterId}
                          onChange={(e) => setSelectedCenterId(e.target.value)}
                          required
                          disabled={loading}
                        >
                          {activeCenters.map((c) => (
                            <option key={c.id} value={c.id}>
                              {c.name} ({c.code}) — {c.timezone || 'UTC'}
                            </option>
                          ))}
                        </select>
                      ) : (
                        <div className="alert alert-warning small py-2 mb-0">
                          No active vaccination centers available.
                        </div>
                      )}
                    </div>
                  ) : (
                    <div className="mb-3 p-3 bg-light rounded border small">
                      <div><strong>Center: </strong>{assignment?.centerName} ({assignment?.centerCode})</div>
                      <div className="text-muted mt-1">Center assignment can only have its active date period updated.</div>
                    </div>
                  )}

                  <div className="row g-3">
                    <div className="col-md-6">
                      <label htmlFor="activeFromInput" className="form-label small fw-semibold">
                        Active From Date <span className="text-danger">*</span>
                      </label>
                      <input
                        type="date"
                        id="activeFromInput"
                        className="form-control form-control-sm"
                        value={activeFrom}
                        onChange={(e) => setActiveFrom(e.target.value)}
                        required
                        disabled={loading}
                      />
                    </div>
                    <div className="col-md-6">
                      <label htmlFor="activeToInput" className="form-label small fw-semibold">
                        Active To Date (Optional)
                      </label>
                      <input
                        type="date"
                        id="activeToInput"
                        className="form-control form-control-sm"
                        value={activeTo}
                        onChange={(e) => setActiveTo(e.target.value)}
                        disabled={loading}
                      />
                      <div className="form-text small text-muted">
                        Leave blank for open-ended assignment.
                      </div>
                    </div>
                  </div>
                </div>
              )}
            </div>

            <div className="modal-footer bg-light">
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                onClick={onCancel}
                disabled={loading}
              >
                Cancel
              </button>
              <button
                type="submit"
                className={`btn btn-sm ${mode === 'CANCEL_CONFIRM' ? 'btn-danger' : mode === 'END_ACTIVE' ? 'btn-warning' : 'btn-primary'}`}
                disabled={loading || (mode === 'CREATE' && activeCenters.length === 0)}
              >
                {loading
                  ? 'Processing...'
                  : mode === 'CANCEL_CONFIRM'
                  ? 'Confirm Cancellation'
                  : mode === 'END_ACTIVE'
                  ? 'Set End Date'
                  : mode === 'UPDATE_FUTURE'
                  ? 'Update Period'
                  : 'Assign Center'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
