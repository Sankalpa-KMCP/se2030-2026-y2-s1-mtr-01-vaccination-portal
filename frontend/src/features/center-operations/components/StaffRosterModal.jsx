import React, { useState, useEffect } from 'react';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';

const STAFF_ROLES = [
  { value: 'VACCINATOR', label: 'Vaccinator', description: 'Clinical staff administering vaccine doses' },
  { value: 'CHECK_IN_STAFF', label: 'Check-In Staff', description: 'Administrative staff managing reception, triage, and check-in' },
  { value: 'SUPERVISOR', label: 'Supervisor', description: 'Clinical lead or operational supervisor overseeing sessions' },
];

const STAFF_STATUSES = [
  { value: 'ACTIVE', label: 'Active', badgeClass: 'bg-success' },
  { value: 'INACTIVE', label: 'Inactive', badgeClass: 'bg-secondary' },
];

/**
 * Modal component for registering a new staff member or editing an existing roster entry.
 */
export default function StaffRosterModal({
  show,
  mode = 'register',
  center,
  rosterEntry,
  onClose,
  onSubmit,
  loading = false,
  error = null,
}) {
  const [userId, setUserId] = useState('');
  const [role, setRole] = useState('VACCINATOR');
  const [status, setStatus] = useState('ACTIVE');
  const [clientErrors, setClientErrors] = useState({});

  useEffect(() => {
    if (!show) {
      setUserId('');
      setRole('VACCINATOR');
      setStatus('ACTIVE');
      setClientErrors({});
      return;
    }

    if (mode === 'edit' && rosterEntry) {
      setUserId(String(rosterEntry.userId || ''));
      setRole(rosterEntry.role || 'VACCINATOR');
      setStatus(rosterEntry.status || 'ACTIVE');
    } else {
      setUserId('');
      setRole('VACCINATOR');
      setStatus('ACTIVE');
    }
    setClientErrors({});
  }, [show, mode, rosterEntry]);

  if (!show) return null;

  const validate = () => {
    const errors = {};

    if (mode === 'register') {
      const trimmedUserId = userId.trim();
      if (!trimmedUserId) {
        errors.userId = 'User ID is required.';
      } else if (!/^[1-9]\d*$/.test(trimmedUserId)) {
        errors.userId = 'User ID must be a positive integer (e.g. 101).';
      }
    }

    if (!role) {
      errors.role = 'Staff role is required.';
    }

    if (!status) {
      errors.status = 'Staff status is required.';
    }

    setClientErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!validate() || loading) return;

    if (mode === 'register') {
      const payload = {
        userId: parseInt(userId.trim(), 10),
        role,
        status,
      };
      onSubmit(payload);
    } else {
      const payload = {
        role,
        status,
      };
      onSubmit(payload);
    }
  };

  const isEdit = mode === 'edit';
  const modalTitle = isEdit ? 'Edit Staff Roster Record' : 'Register Staff Member';

  return (
    <>
      <div className="modal-backdrop fade show" style={{ zIndex: 1050 }} />
      <div
        className="modal fade show d-block"
        tabIndex="-1"
        role="dialog"
        aria-modal="true"
        aria-labelledby="staff-roster-modal-title"
        style={{ zIndex: 1055 }}
      >
        <div className="modal-dialog modal-dialog-centered">
          <div className="modal-content shadow">
            <div className="modal-header bg-light">
              <h5 className="modal-title fw-bold" id="staff-roster-modal-title">
                {modalTitle}
              </h5>
              <button
                type="button"
                className="btn-close"
                aria-label="Close"
                onClick={onClose}
                disabled={loading}
              />
            </div>

            <form onSubmit={handleSubmit} noValidate>
              <div className="modal-body">
                {error && <ApiErrorAlert error={error} />}

                {center && (
                  <div className="mb-3 p-2 bg-light border rounded small">
                    <span className="text-muted">Target Center: </span>
                    <strong className="text-dark">
                      [{center.code}] {center.name}
                    </strong>
                  </div>
                )}

                {/* User ID Field */}
                <div className="mb-3">
                  <label htmlFor="staff-user-id" className="form-label fw-semibold">
                    User ID <span className="text-danger">*</span>
                  </label>
                  {isEdit ? (
                    <div>
                      <input
                        type="text"
                        id="staff-user-id"
                        className="form-control font-monospace bg-light"
                        value={userId}
                        disabled
                        readOnly
                      />
                      <div className="form-text small">
                        User ID is immutable after registration.
                      </div>
                    </div>
                  ) : (
                    <div>
                      <input
                        type="text"
                        id="staff-user-id"
                        className={`form-control font-monospace ${clientErrors.userId ? 'is-invalid' : ''}`}
                        placeholder="e.g. 101"
                        value={userId}
                        onChange={(e) => {
                          setUserId(e.target.value);
                          if (clientErrors.userId) {
                            setClientErrors((prev) => ({ ...prev, userId: null }));
                          }
                        }}
                        disabled={loading}
                        required
                        autoFocus
                      />
                      {clientErrors.userId ? (
                        <div className="invalid-feedback">{clientErrors.userId}</div>
                      ) : (
                        <div className="form-text small">
                          Enter the existing administrative user identifier (positive integer) to roster at this center.
                        </div>
                      )}
                    </div>
                  )}
                </div>

                {/* Staff Role Field */}
                <div className="mb-3">
                  <label htmlFor="staff-role-select" className="form-label fw-semibold">
                    Staff Role <span className="text-danger">*</span>
                  </label>
                  <select
                    id="staff-role-select"
                    className={`form-select ${clientErrors.role ? 'is-invalid' : ''}`}
                    value={role}
                    onChange={(e) => {
                      setRole(e.target.value);
                      if (clientErrors.role) {
                        setClientErrors((prev) => ({ ...prev, role: null }));
                      }
                    }}
                    disabled={loading}
                    required
                  >
                    {STAFF_ROLES.map((r) => (
                      <option key={r.value} value={r.value}>
                        {r.label}
                      </option>
                    ))}
                  </select>
                  {clientErrors.role ? (
                    <div className="invalid-feedback">{clientErrors.role}</div>
                  ) : (
                    <div className="form-text small">
                      {STAFF_ROLES.find((r) => r.value === role)?.description}
                    </div>
                  )}
                </div>

                {/* Status Field */}
                <div className="mb-3">
                  <label htmlFor="staff-status-select" className="form-label fw-semibold">
                    Operational Status <span className="text-danger">*</span>
                  </label>
                  <select
                    id="staff-status-select"
                    className={`form-select ${clientErrors.status ? 'is-invalid' : ''}`}
                    value={status}
                    onChange={(e) => {
                      setStatus(e.target.value);
                      if (clientErrors.status) {
                        setClientErrors((prev) => ({ ...prev, status: null }));
                      }
                    }}
                    disabled={loading}
                    required
                  >
                    {STAFF_STATUSES.map((s) => (
                      <option key={s.value} value={s.value}>
                        {s.label}
                      </option>
                    ))}
                  </select>
                  {clientErrors.status && (
                    <div className="invalid-feedback">{clientErrors.status}</div>
                  )}
                  {isEdit && (
                    <div className="form-text small">
                      To deactivate this staff member, set status to <strong>Inactive</strong>.
                    </div>
                  )}
                </div>
              </div>

              <div className="modal-footer">
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
                  className="btn btn-primary px-4 fw-semibold"
                  disabled={loading}
                >
                  {loading ? (
                    <>
                      <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true" />
                      {isEdit ? 'Saving...' : 'Registering...'}
                    </>
                  ) : (
                    <>{isEdit ? 'Save Changes' : 'Register Staff Member'}</>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </>
  );
}
