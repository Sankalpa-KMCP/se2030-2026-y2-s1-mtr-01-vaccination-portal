import React, { useState, useEffect } from 'react';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';

const TIMEZONE_OPTIONS = [
  { value: 'Asia/Colombo', label: 'Asia/Colombo (UTC+05:30)' },
  { value: 'UTC', label: 'UTC (Coordinated Universal Time)' },
  { value: 'Europe/London', label: 'Europe/London (GMT/BST)' },
  { value: 'America/New_York', label: 'America/New_York (EST/EDT)' },
  { value: 'Asia/Singapore', label: 'Asia/Singapore (SGT, UTC+08:00)' },
  { value: 'Asia/Tokyo', label: 'Asia/Tokyo (JST, UTC+09:00)' },
  { value: 'Australia/Sydney', label: 'Australia/Sydney (AEST/AEDT)' },
];

export default function CreateCenterModal({
  show,
  loading = false,
  apiError = null,
  onClose,
  onSubmit,
}) {
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [address, setAddress] = useState('');
  const [contact, setContact] = useState('');
  const [safeConcurrentOccupancy, setSafeConcurrentOccupancy] = useState('50');
  const [sessionServiceCeiling, setSessionServiceCeiling] = useState('');
  const [timezone, setTimezone] = useState('Asia/Colombo');
  const [clientError, setClientError] = useState(null);

  useEffect(() => {
    if (show) {
      setCode('');
      setName('');
      setAddress('');
      setContact('');
      setSafeConcurrentOccupancy('50');
      setSessionServiceCeiling('');
      setTimezone('Asia/Colombo');
      setClientError(null);
    }
  }, [show]);

  if (!show) {
    return null;
  }

  const handleSubmit = (e) => {
    e.preventDefault();
    setClientError(null);

    const trimmedCode = code.trim();
    if (!trimmedCode) {
      setClientError('Center code is required.');
      return;
    }
    if (trimmedCode.length > 32) {
      setClientError('Center code must not exceed 32 characters.');
      return;
    }

    const trimmedName = name.trim();
    if (!trimmedName) {
      setClientError('Center name is required.');
      return;
    }
    if (trimmedName.length > 128) {
      setClientError('Center name must not exceed 128 characters.');
      return;
    }

    const trimmedAddress = address.trim();
    if (!trimmedAddress) {
      setClientError('Center physical address is required.');
      return;
    }
    if (trimmedAddress.length > 255) {
      setClientError('Address must not exceed 255 characters.');
      return;
    }

    const trimmedContact = contact.trim();
    if (!trimmedContact) {
      setClientError('Contact information is required.');
      return;
    }
    if (trimmedContact.length > 128) {
      setClientError('Contact information must not exceed 128 characters.');
      return;
    }

    const occupancyNum = parseInt(safeConcurrentOccupancy, 10);
    if (isNaN(occupancyNum) || occupancyNum <= 0) {
      setClientError('Safe concurrent occupancy must be a strictly positive integer.');
      return;
    }

    let ceilingNum = null;
    if (sessionServiceCeiling && sessionServiceCeiling.trim() !== '') {
      ceilingNum = parseInt(sessionServiceCeiling, 10);
      if (isNaN(ceilingNum) || ceilingNum <= 0) {
        setClientError('Session service ceiling must be a positive integer if specified.');
        return;
      }
    }

    if (!timezone) {
      setClientError('Center scheduling timezone is required.');
      return;
    }

    onSubmit({
      code: trimmedCode,
      name: trimmedName,
      address: trimmedAddress,
      contact: trimmedContact,
      safeConcurrentOccupancy: occupancyNum,
      sessionServiceCeiling: ceilingNum,
      timezone: timezone.trim(),
    });
  };

  return (
    <div className="modal show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
      <div className="modal-dialog modal-dialog-centered modal-lg">
        <div className="modal-content shadow-lg border-0">
          <div className="modal-header bg-light">
            <h5 className="modal-title">
              <i className="bi bi-hospital me-2"></i>
              Register New Vaccination Center
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
              <ApiErrorAlert error={clientError || apiError} />

              <div className="row g-3">
                <div className="col-md-5">
                  <label htmlFor="createCenterCode" className="form-label fw-semibold text-dark small">
                    Center Code <span className="text-danger">*</span>
                  </label>
                  <input
                    type="text"
                    id="createCenterCode"
                    className="form-control font-monospace"
                    placeholder="e.g. VC-COLOMBO-01"
                    maxLength={32}
                    value={code}
                    onChange={(e) => setCode(e.target.value)}
                    disabled={loading}
                    required
                  />
                  <div className="form-text small text-muted">
                    Unique identification code (max 32 chars). Immutable once registered.
                  </div>
                </div>

                <div className="col-md-7">
                  <label htmlFor="createCenterName" className="form-label fw-semibold text-dark small">
                    Center Name <span className="text-danger">*</span>
                  </label>
                  <input
                    type="text"
                    id="createCenterName"
                    className="form-control"
                    placeholder="e.g. Colombo National Vaccination Center"
                    maxLength={128}
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    disabled={loading}
                    required
                  />
                </div>

                <div className="col-12">
                  <label htmlFor="createCenterAddress" className="form-label fw-semibold text-dark small">
                    Physical Address <span className="text-danger">*</span>
                  </label>
                  <textarea
                    id="createCenterAddress"
                    className="form-control"
                    rows={2}
                    placeholder="Full street address, building, district, and postal code..."
                    maxLength={255}
                    value={address}
                    onChange={(e) => setAddress(e.target.value)}
                    disabled={loading}
                    required
                  />
                </div>

                <div className="col-md-6">
                  <label htmlFor="createCenterContact" className="form-label fw-semibold text-dark small">
                    Contact Information <span className="text-danger">*</span>
                  </label>
                  <input
                    type="text"
                    id="createCenterContact"
                    className="form-control"
                    placeholder="e.g. +94 11 2691111, info@colombo-vc.health.gov.lk"
                    maxLength={128}
                    value={contact}
                    onChange={(e) => setContact(e.target.value)}
                    disabled={loading}
                    required
                  />
                </div>

                <div className="col-md-6">
                  <label htmlFor="createCenterTimezone" className="form-label fw-semibold text-dark small">
                    Scheduling Timezone <span className="text-danger">*</span>
                  </label>
                  <select
                    id="createCenterTimezone"
                    className="form-select"
                    value={timezone}
                    onChange={(e) => setTimezone(e.target.value)}
                    disabled={loading}
                    required
                  >
                    {TIMEZONE_OPTIONS.map((tz) => (
                      <option key={tz.value} value={tz.value}>
                        {tz.label}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="col-md-6">
                  <label htmlFor="createCenterOccupancy" className="form-label fw-semibold text-dark small">
                    Safe Concurrent Occupancy <span className="text-danger">*</span>
                  </label>
                  <input
                    type="number"
                    id="createCenterOccupancy"
                    className="form-control"
                    min={1}
                    value={safeConcurrentOccupancy}
                    onChange={(e) => setSafeConcurrentOccupancy(e.target.value)}
                    disabled={loading}
                    required
                  />
                  <div className="form-text small text-muted">
                    Authoritative physical safety capacity limit for concurrent persons on-premise.
                  </div>
                </div>

                <div className="col-md-6">
                  <label htmlFor="createCenterCeiling" className="form-label fw-semibold text-dark small">
                    Session Service Ceiling (Optional)
                  </label>
                  <input
                    type="number"
                    id="createCenterCeiling"
                    className="form-control"
                    min={1}
                    placeholder="Unrestricted if empty"
                    value={sessionServiceCeiling}
                    onChange={(e) => setSessionServiceCeiling(e.target.value)}
                    disabled={loading}
                  />
                  <div className="form-text small text-muted">
                    Optional ceiling limit on total appointment throughput per session.
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
                    Registering Center...
                  </>
                ) : (
                  <>
                    <i className="bi bi-check-lg me-1"></i>
                    Register Center
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
