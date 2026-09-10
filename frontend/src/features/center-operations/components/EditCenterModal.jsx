import React, { useState, useEffect } from 'react';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';

export default function EditCenterModal({
  show,
  center,
  loading = false,
  apiError = null,
  onClose,
  onSubmit,
}) {
  const [name, setName] = useState('');
  const [address, setAddress] = useState('');
  const [contact, setContact] = useState('');
  const [safeConcurrentOccupancy, setSafeConcurrentOccupancy] = useState('');
  const [sessionServiceCeiling, setSessionServiceCeiling] = useState('');
  const [clientError, setClientError] = useState(null);

  useEffect(() => {
    if (show && center) {
      setName(center.name || '');
      setAddress(center.address || '');
      setContact(center.contact || '');
      setSafeConcurrentOccupancy(
        center.safeConcurrentOccupancy != null ? String(center.safeConcurrentOccupancy) : ''
      );
      setSessionServiceCeiling(
        center.sessionServiceCeiling != null ? String(center.sessionServiceCeiling) : ''
      );
      setClientError(null);
    }
  }, [show, center]);

  if (!show || !center) {
    return null;
  }

  const handleSubmit = (e) => {
    e.preventDefault();
    setClientError(null);

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

    onSubmit({
      name: trimmedName,
      address: trimmedAddress,
      contact: trimmedContact,
      safeConcurrentOccupancy: occupancyNum,
      sessionServiceCeiling: ceilingNum,
    });
  };

  return (
    <div className="modal show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
      <div className="modal-dialog modal-dialog-centered modal-lg">
        <div className="modal-content shadow-lg border-0">
          <div className="modal-header bg-light">
            <h5 className="modal-title">
              <i className="bi bi-pencil-square me-2"></i>
              Edit Center Details
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
              {/* Immutable Identity Info Banner */}
              <div className="card bg-light border mb-3">
                <div className="card-body py-2 px-3">
                  <div className="row g-2 small">
                    <div className="col-sm-6">
                      <span className="text-muted d-block">Center Code (Immutable):</span>
                      <span className="font-monospace fw-bold text-dark">{center.code}</span>
                    </div>
                    <div className="col-sm-6">
                      <span className="text-muted d-block">Timezone (Immutable):</span>
                      <span className="fw-semibold text-dark">{center.timezone || 'UTC'}</span>
                    </div>
                  </div>
                </div>
              </div>

              <ApiErrorAlert error={clientError || apiError} />

              <div className="row g-3">
                <div className="col-12">
                  <label htmlFor="editCenterName" className="form-label fw-semibold text-dark small">
                    Center Name <span className="text-danger">*</span>
                  </label>
                  <input
                    type="text"
                    id="editCenterName"
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
                  <label htmlFor="editCenterAddress" className="form-label fw-semibold text-dark small">
                    Physical Address <span className="text-danger">*</span>
                  </label>
                  <textarea
                    id="editCenterAddress"
                    className="form-control"
                    rows={2}
                    placeholder="Full street address, district, and postal code..."
                    maxLength={255}
                    value={address}
                    onChange={(e) => setAddress(e.target.value)}
                    disabled={loading}
                    required
                  />
                </div>

                <div className="col-12">
                  <label htmlFor="editCenterContact" className="form-label fw-semibold text-dark small">
                    Contact Information <span className="text-danger">*</span>
                  </label>
                  <input
                    type="text"
                    id="editCenterContact"
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
                  <label htmlFor="editCenterOccupancy" className="form-label fw-semibold text-dark small">
                    Safe Concurrent Occupancy <span className="text-danger">*</span>
                  </label>
                  <input
                    type="number"
                    id="editCenterOccupancy"
                    className="form-control"
                    min={1}
                    value={safeConcurrentOccupancy}
                    onChange={(e) => setSafeConcurrentOccupancy(e.target.value)}
                    disabled={loading}
                    required
                  />
                  <div className="form-text small text-muted">
                    Authoritative safe concurrent occupancy limit for on-premise capacity calculation.
                  </div>
                </div>

                <div className="col-md-6">
                  <label htmlFor="editCenterCeiling" className="form-label fw-semibold text-dark small">
                    Session Service Ceiling (Optional)
                  </label>
                  <input
                    type="number"
                    id="editCenterCeiling"
                    className="form-control"
                    min={1}
                    placeholder="Unrestricted if empty"
                    value={sessionServiceCeiling}
                    onChange={(e) => setSessionServiceCeiling(e.target.value)}
                    disabled={loading}
                  />
                  <div className="form-text small text-muted">
                    Optional ceiling limit on total appointment bookings per session.
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
                    Saving Changes...
                  </>
                ) : (
                  <>
                    <i className="bi bi-check-lg me-1"></i>
                    Save Changes
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
