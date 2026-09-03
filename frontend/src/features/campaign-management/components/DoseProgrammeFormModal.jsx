import React, { useState } from 'react';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';

export default function DoseProgrammeFormModal({
  mode = 'CREATE',
  currentVersion = null,
  onSubmit,
  onCancel,
  loading,
  error,
}) {
  const isNewVersion = mode === 'NEW_VERSION';

  const [doseCategory, setDoseCategory] = useState(currentVersion?.doseCategory || 'PRIMARY');
  const [permittedDoseNumbers, setPermittedDoseNumbers] = useState(currentVersion?.permittedDoseNumbers || '1,2');
  const [minimumIntervalDays, setMinimumIntervalDays] = useState(currentVersion?.minimumIntervalDays ?? 28);
  const [previousEvidenceRequired, setPreviousEvidenceRequired] = useState(currentVersion?.previousEvidenceRequired || false);
  const [expectedServiceMinutes, setExpectedServiceMinutes] = useState(currentVersion?.expectedServiceMinutes ?? 15);
  const [dateValue, setDateValue] = useState('');
  const [clientError, setClientError] = useState(null);

  const handleSubmit = (e) => {
    e.preventDefault();
    setClientError(null);

    if (!permittedDoseNumbers.trim()) {
      setClientError('Permitted dose numbers are required (e.g. "1,2" or "3").');
      return;
    }

    if (minimumIntervalDays === '' || Number(minimumIntervalDays) < 0) {
      setClientError('Minimum interval days must be a non-negative number.');
      return;
    }

    if (!dateValue) {
      setClientError(
        isNewVersion
          ? 'Effective start date is required for the new version.'
          : 'Active from date is required.'
      );
      return;
    }

    const payload = {
      doseCategory,
      permittedDoseNumbers: permittedDoseNumbers.trim(),
      minimumIntervalDays: Number(minimumIntervalDays),
      previousEvidenceRequired: Boolean(previousEvidenceRequired),
      expectedServiceMinutes: expectedServiceMinutes !== '' ? Number(expectedServiceMinutes) : null,
    };

    if (isNewVersion) {
      payload.effectiveStartDate = dateValue;
    } else {
      payload.activeFrom = dateValue;
    }

    onSubmit(payload);
  };

  return (
    <div
      className="modal fade show d-block"
      tabIndex="-1"
      role="dialog"
      style={{ backgroundColor: 'rgba(0, 0, 0, 0.5)', zIndex: 1060 }}
    >
      <div className="modal-dialog modal-dialog-centered" role="document">
        <div className="modal-content shadow">
          <form onSubmit={handleSubmit}>
            <div className="modal-header bg-primary text-white">
              <h5 className="modal-title fw-bold">
                {isNewVersion ? 'Create New Dose Programme Version' : 'Configure Initial Dose Programme'}
              </h5>
              <button
                type="button"
                className="btn-close btn-close-white"
                aria-label="Close"
                onClick={onCancel}
                disabled={loading}
              />
            </div>

            <div className="modal-body p-4">
              <ApiErrorAlert error={clientError || error} onDismiss={() => setClientError(null)} />

              {isNewVersion && currentVersion && (
                <div className="alert alert-info py-2 small mb-3">
                  Creating a new version will supersede current <strong>Version {currentVersion.versionNumber}</strong> on the effective start date.
                </div>
              )}

              <div className="row g-3">
                {/* Dose Category */}
                <div className="col-md-6">
                  <label htmlFor="doseCategorySelect" className="form-label fw-semibold text-secondary small">
                    Dose Category <span className="text-danger">*</span>
                  </label>
                  <select
                    id="doseCategorySelect"
                    className="form-select form-select-sm"
                    value={doseCategory}
                    onChange={(e) => setDoseCategory(e.target.value)}
                    required
                  >
                    <option value="PRIMARY">PRIMARY (Primary Series)</option>
                    <option value="BOOSTER">BOOSTER (Booster / Subsequent)</option>
                  </select>
                </div>

                {/* Permitted Dose Numbers */}
                <div className="col-md-6">
                  <label htmlFor="permittedDoseInput" className="form-label fw-semibold text-secondary small">
                    Permitted Doses <span className="text-danger">*</span>
                  </label>
                  <input
                    id="permittedDoseInput"
                    type="text"
                    className="form-control form-control-sm"
                    placeholder="e.g. 1,2 or 3"
                    value={permittedDoseNumbers}
                    onChange={(e) => setPermittedDoseNumbers(e.target.value)}
                    required
                  />
                  <div className="form-text small" style={{ fontSize: '0.75rem' }}>
                    Comma-separated dose sequence numbers.
                  </div>
                </div>

                {/* Minimum Interval Days */}
                <div className="col-md-6">
                  <label htmlFor="minIntervalInput" className="form-label fw-semibold text-secondary small">
                    Minimum Interval (Days) <span className="text-danger">*</span>
                  </label>
                  <input
                    id="minIntervalInput"
                    type="number"
                    min={0}
                    className="form-control form-control-sm"
                    value={minimumIntervalDays}
                    onChange={(e) => setMinimumIntervalDays(e.target.value)}
                    required
                  />
                  <div className="form-text small" style={{ fontSize: '0.75rem' }}>
                    Required spacing between consecutive doses.
                  </div>
                </div>

                {/* Expected Service Minutes */}
                <div className="col-md-6">
                  <label htmlFor="serviceMinutesInput" className="form-label fw-semibold text-secondary small">
                    Expected Service Minutes
                  </label>
                  <input
                    id="serviceMinutesInput"
                    type="number"
                    min={1}
                    className="form-control form-control-sm"
                    placeholder="e.g. 15"
                    value={expectedServiceMinutes}
                    onChange={(e) => setExpectedServiceMinutes(e.target.value)}
                  />
                  <div className="form-text small" style={{ fontSize: '0.75rem' }}>
                    Estimated consultation time per citizen.
                  </div>
                </div>

                {/* Effective / Active Date */}
                <div className="col-12">
                  <label htmlFor="activeDateInput" className="form-label fw-semibold text-secondary small">
                    {isNewVersion ? 'Effective Start Date' : 'Active From Date'} <span className="text-danger">*</span>
                  </label>
                  <input
                    id="activeDateInput"
                    type="date"
                    className="form-control form-control-sm"
                    value={dateValue}
                    onChange={(e) => setDateValue(e.target.value)}
                    required
                  />
                  <div className="form-text small" style={{ fontSize: '0.75rem' }}>
                    {isNewVersion
                      ? 'Date from which this new version becomes clinically active.'
                      : 'Date when the initial clinical dose configuration takes effect.'}
                  </div>
                </div>

                {/* Previous Evidence Required Checkbox */}
                <div className="col-12">
                  <div className="form-check mt-2">
                    <input
                      id="evidenceCheck"
                      className="form-check-input"
                      type="checkbox"
                      checked={previousEvidenceRequired}
                      onChange={(e) => setPreviousEvidenceRequired(e.target.checked)}
                    />
                    <label htmlFor="evidenceCheck" className="form-check-label small fw-semibold text-dark">
                      Mandatory Previous Dose Evidence
                    </label>
                    <div className="text-muted small" style={{ fontSize: '0.75rem' }}>
                      Require verification of previous dose certificates before appointment booking.
                    </div>
                  </div>
                </div>
              </div>
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
                className="btn btn-primary btn-sm fw-semibold"
                disabled={loading}
              >
                {loading ? (
                  <>
                    <span className="spinner-border spinner-border-sm me-2" role="status" />
                    Saving...
                  </>
                ) : isNewVersion ? (
                  'Publish New Version'
                ) : (
                  'Save Initial Programme'
                )}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
