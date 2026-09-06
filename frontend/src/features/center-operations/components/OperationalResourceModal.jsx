import React, { useState, useEffect } from 'react';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';

const RESOURCE_TYPES = [
  { value: 'ROOM', label: 'Room / Space', description: 'Consultation room, triage cubicle, or observation area' },
  { value: 'VACCINATION_STATION', label: 'Vaccination Station', description: 'Designated physical desk or booth for vaccine administration' },
  { value: 'EQUIPMENT', label: 'Equipment', description: 'Medical refrigerators, mobile cooler units, or diagnostic devices' },
];

const RESOURCE_STATUSES = [
  { value: 'ACTIVE', label: 'Active', badgeClass: 'bg-success-subtle text-success-emphasis border border-success-subtle' },
  { value: 'MAINTENANCE', label: 'Maintenance', badgeClass: 'bg-warning-subtle text-warning-emphasis border border-warning-subtle' },
  { value: 'INACTIVE', label: 'Inactive', badgeClass: 'bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle' },
];

export default function OperationalResourceModal({
  show,
  mode = 'create', // 'create' | 'edit'
  center,
  resource = null,
  loading = false,
  apiError = null,
  onClose,
  onSubmit,
}) {
  const isEdit = mode === 'edit';

  const [code, setCode] = useState('');
  const [resourceType, setResourceType] = useState('ROOM');
  const [name, setName] = useState('');
  const [status, setStatus] = useState('ACTIVE');
  const [clientError, setClientError] = useState(null);

  useEffect(() => {
    if (show) {
      if (isEdit && resource) {
        setCode(resource.code || '');
        setResourceType(resource.resourceType || 'ROOM');
        setName(resource.name || '');
        setStatus(resource.status || 'ACTIVE');
      } else {
        setCode('');
        setResourceType('ROOM');
        setName('');
        setStatus('ACTIVE');
      }
      setClientError(null);
    }
  }, [show, isEdit, resource]);

  if (!show) {
    return null;
  }

  const handleSubmit = (e) => {
    e.preventDefault();
    setClientError(null);

    const trimmedName = name.trim();
    if (!trimmedName) {
      setClientError('Resource name is required and cannot be blank.');
      return;
    }
    if (trimmedName.length > 128) {
      setClientError('Resource name must not exceed 128 characters.');
      return;
    }

    if (!status) {
      setClientError('Resource status is required.');
      return;
    }

    if (!isEdit) {
      const trimmedCode = code.trim();
      if (!trimmedCode) {
        setClientError('Resource code is required and cannot be blank.');
        return;
      }
      if (trimmedCode.length > 64) {
        setClientError('Resource code must not exceed 64 characters.');
        return;
      }
      if (!resourceType) {
        setClientError('Resource type is required.');
        return;
      }

      onSubmit({
        code: trimmedCode,
        resourceType,
        name: trimmedName,
        status,
      });
    } else {
      onSubmit({
        name: trimmedName,
        status,
      });
    }
  };

  const selectedTypeObj = RESOURCE_TYPES.find((t) => t.value === resourceType);

  return (
    <div className="modal show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
      <div className="modal-dialog modal-dialog-centered">
        <div className="modal-content shadow-lg border-0">
          <div className="modal-header bg-light">
            <h5 className="modal-title">
              <i className={`bi ${isEdit ? 'bi-pencil-square' : 'bi-plus-circle'} me-2`}></i>
              {isEdit ? 'Edit Operational Resource' : 'Add Operational Resource'}
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
            <div className="modal-body">
              {center && (
                <div className="alert alert-light border small text-muted mb-3 py-2 px-3">
                  <span className="fw-semibold text-dark">Target Center:</span> {center.name}{' '}
                  <span className="badge bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle ms-1 font-monospace">
                    {center.code}
                  </span>
                </div>
              )}

              <ApiErrorAlert error={clientError || apiError} />

              {!isEdit ? (
                <>
                  <div className="mb-3">
                    <label htmlFor="resourceCodeInput" className="form-label fw-semibold text-dark small">
                      Resource Code <span className="text-danger">*</span>
                    </label>
                    <input
                      type="text"
                      id="resourceCodeInput"
                      className="form-control font-monospace"
                      placeholder="e.g. ROOM-101, STN-01, FRIDGE-A"
                      maxLength={64}
                      value={code}
                      onChange={(e) => setCode(e.target.value)}
                      disabled={loading}
                      required
                    />
                    <div className="form-text small text-muted">
                      Unique physical identifier within this center (maximum 64 characters). Immutable once registered.
                    </div>
                  </div>

                  <div className="mb-3">
                    <label htmlFor="resourceTypeSelect" className="form-label fw-semibold text-dark small">
                      Resource Type <span className="text-danger">*</span>
                    </label>
                    <select
                      id="resourceTypeSelect"
                      className="form-select"
                      value={resourceType}
                      onChange={(e) => setResourceType(e.target.value)}
                      disabled={loading}
                      required
                    >
                      {RESOURCE_TYPES.map((t) => (
                        <option key={t.value} value={t.value}>
                          {t.label}
                        </option>
                      ))}
                    </select>
                    {selectedTypeObj && (
                      <div className="form-text small text-muted mt-1">
                        {selectedTypeObj.description}
                      </div>
                    )}
                  </div>
                </>
              ) : (
                <div className="card bg-light border-0 mb-3">
                  <div className="card-body py-2 px-3">
                    <div className="row g-2 small">
                      <div className="col-6">
                        <span className="text-muted d-block">Resource Code (Immutable):</span>
                        <span className="font-monospace fw-bold text-dark">{code}</span>
                      </div>
                      <div className="col-6">
                        <span className="text-muted d-block">Resource Type (Immutable):</span>
                        <span className="badge bg-secondary text-white">
                          {selectedTypeObj?.label || resourceType}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              )}

              <div className="mb-3">
                <label htmlFor="resourceNameInput" className="form-label fw-semibold text-dark small">
                  Resource Name <span className="text-danger">*</span>
                </label>
                <input
                  type="text"
                  id="resourceNameInput"
                  className="form-control"
                  placeholder="e.g. Primary Immunization Booth 1, Main Cold Storage"
                  maxLength={128}
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  disabled={loading}
                  required
                />
                <div className="form-text small text-muted">
                  Descriptive title for scheduling display (maximum 128 characters).
                </div>
              </div>

              <div className="mb-3">
                <label htmlFor="resourceStatusSelect" className="form-label fw-semibold text-dark small">
                  Operational Status <span className="text-danger">*</span>
                </label>
                <select
                  id="resourceStatusSelect"
                  className="form-select"
                  value={status}
                  onChange={(e) => setStatus(e.target.value)}
                  disabled={loading}
                  required
                >
                  {RESOURCE_STATUSES.map((s) => (
                    <option key={s.value} value={s.value}>
                      {s.label}
                    </option>
                  ))}
                </select>
                <div className="form-text small text-muted">
                  {status === 'ACTIVE' && 'Available for operational scheduling and session planning.'}
                  {status === 'MAINTENANCE' && 'Temporarily undergoing service, calibration, or repairs.'}
                  {status === 'INACTIVE' && 'Decommissioned or retired from operational assignment.'}
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
                className="btn btn-primary"
                disabled={loading}
              >
                {loading ? (
                  <>
                    <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                    {isEdit ? 'Saving Changes...' : 'Registering Resource...'}
                  </>
                ) : (
                  <>
                    <i className={`bi ${isEdit ? 'bi-check-circle' : 'bi-plus-lg'} me-1`}></i>
                    {isEdit ? 'Save Changes' : 'Register Resource'}
                  </>
                )}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
