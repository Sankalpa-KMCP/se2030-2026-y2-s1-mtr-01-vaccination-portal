import React, { useState, useEffect } from 'react';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';

const TARGET_TYPES = [
  { value: 'PROVINCE', label: 'Province', hint: 'Administrative province code (e.g. WP, CP, SP, NP)' },
  { value: 'DISTRICT', label: 'District', hint: 'Administrative district name/code (e.g. COLOMBO, GAMPAHA, KANDY)' },
  { value: 'HEALTH_REGION', label: 'Health Region', hint: 'Regional Health Directorate code (e.g. HR-NORTH, HR-WEST)' },
  { value: 'CENTER', label: 'Vaccination Center', hint: 'Specific center location code (e.g. CTR-001, CTR-NCMC)' },
];

/**
 * Modal for creating an administrative location target for a campaign.
 */
export default function LocationTargetModal({
  isOpen,
  onClose,
  onSubmit,
  loading = false,
  error = null,
}) {
  const [targetType, setTargetType] = useState('PROVINCE');
  const [targetCode, setTargetCode] = useState('');
  const [clientError, setClientError] = useState(null);

  useEffect(() => {
    if (isOpen) {
      setTargetType('PROVINCE');
      setTargetCode('');
      setClientError(null);
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const currentTypeConfig = TARGET_TYPES.find((t) => t.value === targetType) || TARGET_TYPES[0];

  const handleSubmit = (e) => {
    e.preventDefault();
    setClientError(null);

    const trimmedCode = targetCode.trim();
    if (!trimmedCode) {
      setClientError('Target code is required and cannot be blank.');
      return;
    }

    if (trimmedCode.length > 64) {
      setClientError('Target code must not exceed 64 characters.');
      return;
    }

    const payload = {
      targetType,
      targetCode: trimmedCode.toUpperCase(),
    };

    onSubmit(payload);
  };

  return (
    <div
      className="modal fade show d-block"
      tabIndex="-1"
      role="dialog"
      aria-modal="true"
      style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}
    >
      <div className="modal-dialog modal-dialog-centered" role="document">
        <div className="modal-content shadow">
          <div className="modal-header bg-light">
            <h5 className="modal-title h6 fw-bold mb-0">
              <i className="bi bi-geo-alt me-2 text-primary"></i>
              Add Location Target
            </h5>
            <button
              type="button"
              className="btn-close"
              aria-label="Close"
              onClick={onClose}
              disabled={loading}
            ></button>
          </div>

          <form onSubmit={handleSubmit}>
            <div className="modal-body">
              <ApiErrorAlert error={error || clientError} />

              <div className="mb-3">
                <label htmlFor="targetTypeSelect" className="form-label small fw-semibold">
                  Target Type <span className="text-danger">*</span>
                </label>
                <select
                  id="targetTypeSelect"
                  className="form-select form-select-sm"
                  value={targetType}
                  onChange={(e) => setTargetType(e.target.value)}
                  disabled={loading}
                >
                  {TARGET_TYPES.map((t) => (
                    <option key={t.value} value={t.value}>
                      {t.label}
                    </option>
                  ))}
                </select>
                <div className="form-text small text-muted">
                  {currentTypeConfig.hint}
                </div>
              </div>

              <div className="mb-3">
                <label htmlFor="targetCodeInput" className="form-label small fw-semibold">
                  Target Code <span className="text-danger">*</span>
                </label>
                <input
                  id="targetCodeInput"
                  type="text"
                  className="form-control form-control-sm font-monospace text-uppercase"
                  placeholder="e.g. COLOMBO, WP, HR-NORTH"
                  value={targetCode}
                  maxLength={64}
                  onChange={(e) => setTargetCode(e.target.value)}
                  disabled={loading}
                  autoFocus
                />
                <div className="form-text small text-muted">
                  Target codes are automatically trimmed and normalized to uppercase.
                </div>
              </div>
            </div>

            <div className="modal-footer bg-light py-2">
              <button
                type="button"
                className="btn btn-sm btn-outline-secondary"
                onClick={onClose}
                disabled={loading}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="btn btn-sm btn-primary"
                disabled={loading}
              >
                {loading ? (
                  <>
                    <span className="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span>
                    Adding...
                  </>
                ) : (
                  'Add Target'
                )}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
