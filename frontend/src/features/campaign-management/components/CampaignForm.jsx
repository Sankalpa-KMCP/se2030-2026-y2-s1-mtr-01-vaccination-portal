import React, { useState, useEffect } from 'react';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import { listVaccineTypes, listTargetGroups } from '../api/campaignApi';

/**
 * Formats a Date instance, ISO-8601 string, or local string into YYYY-MM-DDTHH:mm for datetime-local inputs.
 */
function formatForDateTimeLocal(dateVal) {
  if (!dateVal) return '';
  if (typeof dateVal === 'string' && /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(dateVal)) {
    return dateVal;
  }
  const d = new Date(dateVal);
  if (isNaN(d.getTime())) return '';
  const pad = (n) => String(n).padStart(2, '0');
  const year = d.getFullYear();
  const month = pad(d.getMonth() + 1);
  const day = pad(d.getDate());
  const hours = pad(d.getHours());
  const minutes = pad(d.getMinutes());
  return `${year}-${month}-${day}T${hours}:${minutes}`;
}

export default function CampaignForm({ onSubmit, onCancel, loading, error, initialCampaign = null }) {
  const isEditMode = Boolean(initialCampaign);

  const [code, setCode] = useState(initialCampaign?.code || '');
  const [name, setName] = useState(initialCampaign?.name || '');
  const [description, setDescription] = useState(initialCampaign?.description || '');
  const [vaccineTypeId, setVaccineTypeId] = useState(
    initialCampaign?.vaccineType?.id || initialCampaign?.vaccineTypeId || ''
  );
  const [targetGroupId, setTargetGroupId] = useState(
    initialCampaign?.targetGroup?.id || initialCampaign?.targetGroupId || ''
  );
  const [serviceStartDate, setServiceStartDate] = useState(initialCampaign?.serviceStartDate || '');
  const [serviceEndDate, setServiceEndDate] = useState(initialCampaign?.serviceEndDate || '');
  const [bookingOpenAt, setBookingOpenAt] = useState(formatForDateTimeLocal(initialCampaign?.bookingOpenAt));
  const [bookingCloseAt, setBookingCloseAt] = useState(formatForDateTimeLocal(initialCampaign?.bookingCloseAt));
  const [clientError, setClientError] = useState(null);

  // Master data lookup state (relevant for create mode)
  const [vaccineTypes, setVaccineTypes] = useState([]);
  const [targetGroups, setTargetGroups] = useState([]);
  const [masterDataLoading, setMasterDataLoading] = useState(!isEditMode);
  const [masterDataError, setMasterDataError] = useState(null);

  useEffect(() => {
    if (initialCampaign) {
      setCode(initialCampaign.code || '');
      setName(initialCampaign.name || '');
      setDescription(initialCampaign.description || '');
      setVaccineTypeId(initialCampaign.vaccineType?.id || initialCampaign.vaccineTypeId || '');
      setTargetGroupId(initialCampaign.targetGroup?.id || initialCampaign.targetGroupId || '');
      setServiceStartDate(initialCampaign.serviceStartDate || '');
      setServiceEndDate(initialCampaign.serviceEndDate || '');
      setBookingOpenAt(formatForDateTimeLocal(initialCampaign.bookingOpenAt));
      setBookingCloseAt(formatForDateTimeLocal(initialCampaign.bookingCloseAt));
    }
  }, [initialCampaign]);

  useEffect(() => {
    if (isEditMode) {
      return;
    }

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

        if (!initialCampaign && validVTypes.length > 0) {
          setVaccineTypeId(validVTypes[0].id);
        }
        if (!initialCampaign && validTGroups.length > 0) {
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
  }, [isEditMode, initialCampaign]);

  const handleSubmit = (e) => {
    e.preventDefault();
    setClientError(null);

    if (isEditMode) {
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

      // In edit mode: strictly approved fields only (no code, vaccineType, targetGroup, status, or id)
      const payload = {
        name: name.trim(),
        description: description.trim() || null,
        serviceStartDate,
        serviceEndDate,
        bookingOpenAt: new Date(bookingOpenAt).toISOString(),
        bookingCloseAt: new Date(bookingCloseAt).toISOString(),
      };

      onSubmit(payload);
      return;
    }

    // Create mode validation & payload
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
        <h5 className="mb-0 fw-bold">{isEditMode ? 'Edit Campaign' : 'Create New Vaccination Campaign'}</h5>
      </div>
      <div className="card-body p-4">
        <ApiErrorAlert error={clientError || (!isEditMode ? masterDataError : null) || error} onDismiss={() => setClientError(null)} />

        <form onSubmit={handleSubmit}>
          <div className="row g-3">
            {/* Campaign Code & Name */}
            <div className="col-md-4">
              <label htmlFor="campaignCode" className="form-label fw-semibold text-secondary small">
                Campaign Code {!isEditMode && <span className="text-danger">*</span>}
              </label>
              <input
                id="campaignCode"
                type="text"
                className="form-control"
                placeholder="e.g. COVID19-2026-Q1"
                maxLength={64}
                value={code}
                onChange={(e) => setCode(e.target.value)}
                disabled={isEditMode}
                readOnly={isEditMode}
                required={!isEditMode}
              />
              {isEditMode && <div className="form-text small">Campaign code is immutable.</div>}
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

            {/* Vaccine Type & Target Group Selectors */}
            <div className="col-md-6">
              <label htmlFor="vaccineTypeId" className="form-label fw-semibold text-secondary small">
                Vaccine Type {!isEditMode && <span className="text-danger">*</span>}
              </label>
              {isEditMode ? (
                <>
                  <input
                    id="vaccineTypeId"
                    type="text"
                    className="form-control"
                    value={
                      initialCampaign.vaccineType?.name
                        ? `${initialCampaign.vaccineType.name} (${initialCampaign.vaccineType.code || ''})`
                        : initialCampaign.vaccineTypeCode || 'N/A'
                    }
                    disabled
                    readOnly
                  />
                  <div className="form-text small">Vaccine type is immutable.</div>
                </>
              ) : (
                <>
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
                </>
              )}
            </div>

            <div className="col-md-6">
              <label htmlFor="targetGroupId" className="form-label fw-semibold text-secondary small">
                Target Demographic Group {!isEditMode && <span className="text-danger">*</span>}
              </label>
              {isEditMode ? (
                <>
                  <input
                    id="targetGroupId"
                    type="text"
                    className="form-control"
                    value={
                      initialCampaign.targetGroup?.description
                        ? `${initialCampaign.targetGroup.code || ''} - ${initialCampaign.targetGroup.description}`
                        : initialCampaign.targetGroupCode || 'N/A'
                    }
                    disabled
                    readOnly
                  />
                  <div className="form-text small">Target demographic group is immutable.</div>
                </>
              ) : (
                <>
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
                </>
              )}
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
              disabled={loading || (!isEditMode && (masterDataLoading || vaccineTypes.length === 0 || targetGroups.length === 0))}
            >
              {loading ? (
                <>
                  <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true" />
                  Saving...
                </>
              ) : isEditMode ? (
                'Save Changes'
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
