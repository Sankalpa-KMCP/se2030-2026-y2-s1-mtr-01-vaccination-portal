import React, { useState } from 'react';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';

export default function CampaignForm({ onSubmit, onCancel, loading, error }) {
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [vaccineTypeId, setVaccineTypeId] = useState(1);
  const [targetGroupId, setTargetGroupId] = useState(1);
  const [serviceStartDate, setServiceStartDate] = useState('');
  const [serviceEndDate, setServiceEndDate] = useState('');
  const [bookingOpenAt, setBookingOpenAt] = useState('');
  const [bookingCloseAt, setBookingCloseAt] = useState('');
  const [clientError, setClientError] = useState(null);

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
        <ApiErrorAlert error={clientError || error} onDismiss={() => setClientError(null)} />

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

            {/* Vaccine Type & Target Group Master References */}
            <div className="col-md-6">
              <label htmlFor="vaccineTypeId" className="form-label fw-semibold text-secondary small">
                Vaccine Type ID <span className="text-danger">*</span>
              </label>
              <input
                id="vaccineTypeId"
                type="number"
                min={1}
                className="form-control"
                value={vaccineTypeId}
                onChange={(e) => setVaccineTypeId(e.target.value)}
                required
              />
              <div className="form-text small">Master ID corresponding to approved Vaccine Type.</div>
            </div>

            <div className="col-md-6">
              <label htmlFor="targetGroupId" className="form-label fw-semibold text-secondary small">
                Target Group ID <span className="text-danger">*</span>
              </label>
              <input
                id="targetGroupId"
                type="number"
                min={1}
                className="form-control"
                value={targetGroupId}
                onChange={(e) => setTargetGroupId(e.target.value)}
                required
              />
              <div className="form-text small">Master ID corresponding to eligible Target Demographic Group.</div>
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
              disabled={loading}
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
