import React, { useState, useEffect } from 'react';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import { listVaccineTypes, listTargetGroups } from '../api/campaignApi';

export default function CampaignForm({ onSubmit, onCancel, loading, error }) {
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [vaccineTypeId, setVaccineTypeId] = useState('');
  const [targetGroupId, setTargetGroupId] = useState('');
  const [serviceStartDate, setServiceStartDate] = useState('');
  const [serviceEndDate, setServiceEndDate] = useState('');
  const [bookingOpenAt, setBookingOpenAt] = useState('');
  const [bookingCloseAt, setBookingCloseAt] = useState('');
  const [clientError, setClientError] = useState(null);

  // Master data lookup state
  const [vaccineTypes, setVaccineTypes] = useState([]);
  const [targetGroups, setTargetGroups] = useState([]);
  const [masterDataLoading, setMasterDataLoading] = useState(true);
  const [masterDataError, setMasterDataError] = useState(null);

  useEffect(() => {
    let isMounted = true;
    setMasterDataLoading(true);
    setMasterDataError(null);

    Promise.all([listVaccineTypes(), listTargetGroups()])
      .then(([vTypes, tGroups]) => {
        if (!isMounted) return;
        const validVTypes = vTypes || [];
        const validTGroups = tGroups || [];
        setVaccineTypes(validVTypes);
        setTargetGroups(validTGroups);

        if (validVTypes.length > 0) {
          setVaccineTypeId(validVTypes[0].id);
        }
        if (validTGroups.length > 0) {
          setTargetGroupId(validTGroups[0].id);
        }
      })
      .catch((err) => {
        if (!isMounted) return;
        setMasterDataError(err?.message || 'Failed to load master data for vaccine types and target groups.');
      })
      .finally(() => {
        if (isMounted) setMasterDataLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, []);

  const handleSubmit = (e) => {
    e.preventDefault();
    setClientError(null);

    // Client-side date and constraint validation
    if (!code.trim()) {
      setClientError('Campaign code is required.');
      return;
    }
    if (!name.trim()) {
      setClientError('Campaign name is required.');
      return;
    }
    if (!vaccineTypeId) {
      setClientError('Please select a valid active Vaccine Type.');
      return;
    }
    if (!targetGroupId) {
      setClientError('Please select a valid active Target Demographic Group.');
      return;
    }
    if (!serviceStartDate || !serviceEndDate) {
      setClientError('Service start and end dates are required.');
      return;
    }
    if (new Date(serviceStartDate) > new Date(serviceEndDate)) {
      setClientError('Service start date must not be after service end date.');
      return;
    }
    if (!bookingOpenAt || !bookingCloseAt) {
      setClientError('Booking open and close timestamps are required.');
      return;
    }
    if (new Date(bookingOpenAt) > new Date(bookingCloseAt)) {
      setClientError('Booking open timestamp must not be after booking close timestamp.');
      return;
    }

    // Prepare ISO timestamps for Instant backend mapping
    const payload = {
      code: code.trim(),
      name: name.trim(),
      description: description.trim() || null,
      vaccineTypeId: Number(vaccineTypeId),
      targetGroupId: Number(targetGroupId),
      serviceStartDate,
      serviceEndDate,
      bookingOpenAt: new Date(bookingOpenAt).toISOString(),
      bookingCloseAt: new Date(bookingCloseAt).toISOString(),
    };

    onSubmit(payload);
  };

  return (
    <div className="card shadow-sm border-0 mb-4">
      <div className="card-header bg-primary text-white py-3">
        <h5 className="mb-0 fw-bold">Create New Vaccination Campaign</h5>
      </div>
      <div className="card-body p-4">
        <ApiErrorAlert error={clientError || masterDataError || error} onDismiss={() => setClientError(null)} />

        <form onSubmit={handleSubmit}>
          <div className="row g-3">
            {/* Campaign Code & Name */}
            <div className="col-md-4">
              <label htmlFor="campaignCode" className="form-label fw-semibold text-secondary small">
                Campaign Code <span className="text-danger">*</span>
              </label>
              <input
                id="campaignCode"
                type="text"
                className="form-control"
                placeholder="e.g. COVID19-2026-Q1"
                maxLength={64}
                value={code}
                onChange={(e) => setCode(e.target.value)}
                required
              />
            </div>

            <div className="col-md-8">
              <label htmlFor="campaignName" className="form-label fw-semibold text-secondary small">
                Campaign Name <span className="text-danger">*</span>
              </label>
              <input
                id="campaignName"
                type="text"
                className="form-control"
                placeholder="e.g. National COVID-19 Booster Rollout 2026"
                maxLength={128}
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
              />
            </div>

            {/* Description */}
            <div className="col-12">
              <label htmlFor="campaignDescription" className="form-label fw-semibold text-secondary small">
                Description (Optional)
              </label>
              <textarea
                id="campaignDescription"
                className="form-control"
                rows={2}
                placeholder="Detailed objectives and clinical guidelines for this campaign..."
                maxLength={1000}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </div>

            {/* Vaccine Type & Target Group Master Selectors */}
            <div className="col-md-6">
              <label htmlFor="vaccineTypeId" className="form-label fw-semibold text-secondary small">
                Vaccine Type <span className="text-danger">*</span>
              </label>
              <select
                id="vaccineTypeId"
                className="form-select"
                value={vaccineTypeId}
                onChange={(e) => setVaccineTypeId(e.target.value)}
                disabled={masterDataLoading || vaccineTypes.length === 0}
                required
              >
                {masterDataLoading ? (
                  <option value="">Loading vaccine types...</option>
                ) : vaccineTypes.length === 0 ? (
                  <option value="">No active vaccine types available</option>
                ) : (
                  vaccineTypes.map((vt) => (
                    <option key={vt.id} value={vt.id}>
                      {vt.name} ({vt.code})
                    </option>
                  ))
                )}
              </select>
              <div className="form-text small">Select an approved active vaccine type.</div>
            </div>

            <div className="col-md-6">
              <label htmlFor="targetGroupId" className="form-label fw-semibold text-secondary small">
                Target Demographic Group <span className="text-danger">*</span>
              </label>
              <select
                id="targetGroupId"
                className="form-select"
                value={targetGroupId}
                onChange={(e) => setTargetGroupId(e.target.value)}
                disabled={masterDataLoading || targetGroups.length === 0}
                required
              >
                {masterDataLoading ? (
                  <option value="">Loading target demographic groups...</option>
                ) : targetGroups.length === 0 ? (
                  <option value="">No active target demographic groups available</option>
                ) : (
                  targetGroups.map((tg) => (
                    <option key={tg.id} value={tg.id}>
                      {tg.code} - {tg.description}
                    </option>
                  ))
                )}
              </select>
              <div className="form-text small">Select an eligible active target demographic group.</div>
            </div>

            {/* Service Period */}
            <div className="col-md-6">
              <label htmlFor="serviceStartDate" className="form-label fw-semibold text-secondary small">
                Service Start Date <span className="text-danger">*</span>
              </label>
              <input
                id="serviceStartDate"
                type="date"
                className="form-control"
                value={serviceStartDate}
                onChange={(e) => setServiceStartDate(e.target.value)}
                required
              />
            </div>

            <div className="col-md-6">
              <label htmlFor="serviceEndDate" className="form-label fw-semibold text-secondary small">
                Service End Date <span className="text-danger">*</span>
              </label>
              <input
                id="serviceEndDate"
                type="date"
                className="form-control"
                value={serviceEndDate}
                onChange={(e) => setServiceEndDate(e.target.value)}
                required
              />
            </div>

            {/* Booking Window */}
            <div className="col-md-6">
              <label htmlFor="bookingOpenAt" className="form-label fw-semibold text-secondary small">
                Citizen Booking Open Timestamp <span className="text-danger">*</span>
              </label>
              <input
                id="bookingOpenAt"
                type="datetime-local"
                className="form-control"
                value={bookingOpenAt}
                onChange={(e) => setBookingOpenAt(e.target.value)}
                required
              />
            </div>

            <div className="col-md-6">
              <label htmlFor="bookingCloseAt" className="form-label fw-semibold text-secondary small">
                Citizen Booking Close Timestamp <span className="text-danger">*</span>
              </label>
              <input
                id="bookingCloseAt"
                type="datetime-local"
                className="form-control"
                value={bookingCloseAt}
                onChange={(e) => setBookingCloseAt(e.target.value)}
                required
              />
            </div>
          </div>

          <hr className="my-4" />

          <div className="d-flex justify-content-end gap-2">
            <button
              type="button"
              className="btn btn-outline-secondary"
              onClick={onCancel}
              disabled={loading}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="btn btn-primary fw-semibold px-4"
              disabled={loading || masterDataLoading || vaccineTypes.length === 0 || targetGroups.length === 0}
            >
              {loading ? (
                <>
                  <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true" />
                  Saving...
                </>
              ) : (
                'Save Campaign'
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
