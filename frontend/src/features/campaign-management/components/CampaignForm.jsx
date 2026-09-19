import React, { useState, useEffect, useRef } from 'react';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import { listVaccineTypes, listTargetGroups } from '../api/campaignApi';
import { getPublishedQuestionnaireVersions } from '../api/campaignConfigurationApi';

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
  const [questionnaireVersionId, setQuestionnaireVersionId] = useState(
    initialCampaign?.questionnaireVersionId ? String(initialCampaign.questionnaireVersionId) : ''
  );
  const [publishedQuestionnaires, setPublishedQuestionnaires] = useState([]);
  const [questionnairesLoading, setQuestionnairesLoading] = useState(false);
  const [questionnairesError, setQuestionnairesError] = useState(null);
  const [clientError, setClientError] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});

  // Master data lookup state (relevant for create mode)
  const [vaccineTypes, setVaccineTypes] = useState([]);
  const [targetGroups, setTargetGroups] = useState([]);
  const [masterDataLoading, setMasterDataLoading] = useState(!isEditMode);
  const [masterDataError, setMasterDataError] = useState(null);

  const clearFieldErrors = (...fields) => {
    setFieldErrors((prev) => {
      const next = { ...prev };
      let changed = false;
      fields.forEach((f) => {
        if (next[f]) {
          delete next[f];
          changed = true;
        }
      });
      return changed ? next : prev;
    });
  };

  const campaignKeyRef = useRef(
    initialCampaign ? (initialCampaign.id ?? initialCampaign.code ?? 'initial') : null
  );

  useEffect(() => {
    const key = initialCampaign ? (initialCampaign.id ?? initialCampaign.code ?? 'loaded') : null;
    if (initialCampaign && key !== campaignKeyRef.current) {
      campaignKeyRef.current = key;
      setCode(initialCampaign.code || '');
      setName(initialCampaign.name || '');
      setDescription(initialCampaign.description || '');
      setVaccineTypeId(initialCampaign.vaccineType?.id || initialCampaign.vaccineTypeId || '');
      setTargetGroupId(initialCampaign.targetGroup?.id || initialCampaign.targetGroupId || '');
      setServiceStartDate(initialCampaign.serviceStartDate || '');
      setServiceEndDate(initialCampaign.serviceEndDate || '');
      setBookingOpenAt(formatForDateTimeLocal(initialCampaign.bookingOpenAt));
      setBookingCloseAt(formatForDateTimeLocal(initialCampaign.bookingCloseAt));
      setQuestionnaireVersionId(
        initialCampaign.questionnaireVersionId ? String(initialCampaign.questionnaireVersionId) : ''
      );
      setFieldErrors({});
      setClientError(null);
    }
  }, [initialCampaign]);

  useEffect(() => {
    let isMounted = true;
    if (typeof getPublishedQuestionnaireVersions === 'function') {
      setQuestionnairesLoading(true);
      setQuestionnairesError(null);
      getPublishedQuestionnaireVersions()
        .then((data) => {
          if (!isMounted) return;
          setPublishedQuestionnaires(Array.isArray(data) ? data : []);
        })
        .catch((err) => {
          if (!isMounted) return;
          setQuestionnairesError(err?.message || 'Failed to load published questionnaires.');
        })
        .finally(() => {
          if (isMounted) setQuestionnairesLoading(false);
        });
    }
    return () => {
      isMounted = false;
    };
  }, []);

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

  const validateForm = () => {
    const errs = {};

    if (!isEditMode) {
      if (!code.trim()) {
        errs.code = 'Campaign code is required.';
      }
      if (!vaccineTypeId) {
        errs.vaccineTypeId = 'Please select a valid active Vaccine Type.';
      }
      if (!targetGroupId) {
        errs.targetGroupId = 'Please select a valid active Target Demographic Group.';
      }
    }

    if (!name.trim()) {
      errs.name = 'Campaign name is required.';
    }

    // Service window validation
    if (!serviceStartDate) {
      errs.serviceStartDate = 'Service start date is required.';
    }
    if (!serviceEndDate) {
      errs.serviceEndDate = 'Service end date is required.';
    } else if (serviceStartDate) {
      if (serviceEndDate < serviceStartDate) {
        errs.serviceEndDate = 'Service end date cannot be before the service start date.';
      } else if (serviceEndDate === serviceStartDate) {
        errs.serviceEndDate = 'Service end date must be after the service start date.';
      }
    }

    // Citizen booking window validation
    if (!bookingOpenAt) {
      errs.bookingOpenAt = 'Citizen booking open timestamp is required.';
    }
    if (!bookingCloseAt) {
      errs.bookingCloseAt = 'Citizen booking close timestamp is required.';
    } else {
      if (bookingOpenAt) {
        const openTime = new Date(bookingOpenAt).getTime();
        const closeTime = new Date(bookingCloseAt).getTime();
        if (!isNaN(openTime) && !isNaN(closeTime)) {
          if (closeTime <= openTime) {
            errs.bookingCloseAt = 'Booking close time must be after the booking open time.';
          }
        }
      }
      // Booking close vs service window containment
      if (serviceEndDate && !errs.bookingCloseAt) {
        const bookingCloseDateStr = bookingCloseAt.slice(0, 10);
        if (bookingCloseDateStr > serviceEndDate) {
          errs.bookingCloseAt = 'Booking must close no later than the allowed campaign service window.';
        }
      }
    }

    return errs;
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    setClientError(null);

    const errs = validateForm();
    if (Object.keys(errs).length > 0) {
      setFieldErrors(errs);
      return;
    }
    setFieldErrors({});

    if (isEditMode) {
      // In edit mode: strictly approved fields only (no code, vaccineType, targetGroup, status, or id)
      const payload = {
        name: name.trim(),
        description: description.trim() || null,
        serviceStartDate,
        serviceEndDate,
        bookingOpenAt: new Date(bookingOpenAt).toISOString(),
        bookingCloseAt: new Date(bookingCloseAt).toISOString(),
        questionnaireVersionId: questionnaireVersionId ? Number(questionnaireVersionId) : null,
      };

      onSubmit(payload);
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
      questionnaireVersionId: questionnaireVersionId ? Number(questionnaireVersionId) : null,
    };

    onSubmit(payload);
  };

  const handleCancel = () => {
    setFieldErrors({});
    setClientError(null);
    onCancel();
  };

  return (
    <div className="card shadow-sm border-0 mb-4">
      <div className="card-header bg-primary text-white py-3">
        <h5 className="mb-0 fw-bold">{isEditMode ? 'Edit Campaign' : 'Create New Vaccination Campaign'}</h5>
      </div>
      <div className="card-body p-4">
        <ApiErrorAlert
          error={clientError || (!isEditMode ? masterDataError : null) || questionnairesError || error}
          onDismiss={() => {
            setClientError(null);
            setQuestionnairesError(null);
          }}
        />

        <form onSubmit={handleSubmit} noValidate>
          <div className="row g-3">
            {/* Campaign Code & Name */}
            <div className="col-md-4">
              <label htmlFor="campaignCode" className="form-label fw-semibold text-secondary small">
                Campaign Code {!isEditMode && <span className="text-danger">*</span>}
              </label>
              <input
                id="campaignCode"
                type="text"
                className={`form-control ${fieldErrors.code ? 'is-invalid' : ''}`}
                placeholder="e.g. COVID19-2026-Q1"
                maxLength={64}
                value={code}
                onChange={(e) => {
                  setCode(e.target.value);
                  clearFieldErrors('code');
                }}
                disabled={isEditMode}
                readOnly={isEditMode}
              />
              {fieldErrors.code && <div className="invalid-feedback d-block">{fieldErrors.code}</div>}
              {isEditMode && <div className="form-text small">Campaign code is immutable.</div>}
            </div>

            <div className="col-md-8">
              <label htmlFor="campaignName" className="form-label fw-semibold text-secondary small">
                Campaign Name <span className="text-danger">*</span>
              </label>
              <input
                id="campaignName"
                type="text"
                className={`form-control ${fieldErrors.name ? 'is-invalid' : ''}`}
                placeholder="e.g. National COVID-19 Booster Rollout 2026"
                maxLength={128}
                value={name}
                onChange={(e) => {
                  setName(e.target.value);
                  clearFieldErrors('name');
                }}
              />
              {fieldErrors.name && <div className="invalid-feedback d-block">{fieldErrors.name}</div>}
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
                    className={`form-select ${fieldErrors.vaccineTypeId ? 'is-invalid' : ''}`}
                    value={vaccineTypeId}
                    onChange={(e) => {
                      setVaccineTypeId(e.target.value);
                      clearFieldErrors('vaccineTypeId');
                    }}
                    disabled={masterDataLoading || vaccineTypes.length === 0}
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
                  {fieldErrors.vaccineTypeId && (
                    <div className="invalid-feedback d-block">{fieldErrors.vaccineTypeId}</div>
                  )}
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
                    className={`form-select ${fieldErrors.targetGroupId ? 'is-invalid' : ''}`}
                    value={targetGroupId}
                    onChange={(e) => {
                      setTargetGroupId(e.target.value);
                      clearFieldErrors('targetGroupId');
                    }}
                    disabled={masterDataLoading || targetGroups.length === 0}
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
                  {fieldErrors.targetGroupId && (
                    <div className="invalid-feedback d-block">{fieldErrors.targetGroupId}</div>
                  )}
                  <div className="form-text small">Select an eligible active target demographic group.</div>
                </>
              )}
            </div>

            {/* Screening Questionnaire Selection */}
            <div className="col-12">
              <label htmlFor="questionnaireVersionId" className="form-label fw-semibold text-secondary small">
                Screening Questionnaire
              </label>
              <select
                id="questionnaireVersionId"
                className="form-select"
                value={questionnaireVersionId}
                onChange={(e) => setQuestionnaireVersionId(e.target.value)}
                disabled={questionnairesLoading}
              >
                <option value="">-- Select a published questionnaire --</option>
                {isEditMode &&
                  questionnaireVersionId &&
                  !publishedQuestionnaires.some((qv) => String(qv.id) === String(questionnaireVersionId)) && (
                    <option value={questionnaireVersionId}>
                      Assigned Version #{questionnaireVersionId} (Current)
                    </option>
                  )}
                {publishedQuestionnaires.map((qv) => (
                  <option key={qv.id} value={qv.id}>
                    {qv.questionnaireName} (v{qv.versionNumber}) - {qv.questionnaireCode}
                  </option>
                ))}
              </select>
              <div className="form-text small">
                Select an active published clinical screening questionnaire version for this campaign.
              </div>
            </div>

            {/* Service Period */}
            <div className="col-md-6">
              <label htmlFor="serviceStartDate" className="form-label fw-semibold text-secondary small">
                Service Start Date <span className="text-danger">*</span>
              </label>
              <input
                id="serviceStartDate"
                type="date"
                className={`form-control ${fieldErrors.serviceStartDate ? 'is-invalid' : ''}`}
                value={serviceStartDate}
                max={serviceEndDate || undefined}
                onChange={(e) => {
                  setServiceStartDate(e.target.value);
                  clearFieldErrors('serviceStartDate', 'serviceEndDate');
                }}
              />
              {fieldErrors.serviceStartDate && (
                <div className="invalid-feedback d-block">{fieldErrors.serviceStartDate}</div>
              )}
            </div>

            <div className="col-md-6">
              <label htmlFor="serviceEndDate" className="form-label fw-semibold text-secondary small">
                Service End Date <span className="text-danger">*</span>
              </label>
              <input
                id="serviceEndDate"
                type="date"
                className={`form-control ${fieldErrors.serviceEndDate ? 'is-invalid' : ''}`}
                value={serviceEndDate}
                min={serviceStartDate || undefined}
                onChange={(e) => {
                  setServiceEndDate(e.target.value);
                  clearFieldErrors('serviceStartDate', 'serviceEndDate', 'bookingCloseAt');
                }}
              />
              {fieldErrors.serviceEndDate && (
                <div className="invalid-feedback d-block">{fieldErrors.serviceEndDate}</div>
              )}
            </div>

            {/* Booking Window */}
            <div className="col-md-6">
              <label htmlFor="bookingOpenAt" className="form-label fw-semibold text-secondary small">
                Citizen Booking Open Timestamp <span className="text-danger">*</span>
              </label>
              <input
                id="bookingOpenAt"
                type="datetime-local"
                className={`form-control ${fieldErrors.bookingOpenAt ? 'is-invalid' : ''}`}
                value={bookingOpenAt}
                max={bookingCloseAt || (serviceEndDate ? `${serviceEndDate}T23:59` : undefined)}
                onChange={(e) => {
                  setBookingOpenAt(e.target.value);
                  clearFieldErrors('bookingOpenAt', 'bookingCloseAt');
                }}
              />
              {fieldErrors.bookingOpenAt && (
                <div className="invalid-feedback d-block">{fieldErrors.bookingOpenAt}</div>
              )}
            </div>

            <div className="col-md-6">
              <label htmlFor="bookingCloseAt" className="form-label fw-semibold text-secondary small">
                Citizen Booking Close Timestamp <span className="text-danger">*</span>
              </label>
              <input
                id="bookingCloseAt"
                type="datetime-local"
                className={`form-control ${fieldErrors.bookingCloseAt ? 'is-invalid' : ''}`}
                value={bookingCloseAt}
                min={bookingOpenAt || undefined}
                max={serviceEndDate ? `${serviceEndDate}T23:59` : undefined}
                onChange={(e) => {
                  setBookingCloseAt(e.target.value);
                  clearFieldErrors('bookingCloseAt');
                }}
              />
              {fieldErrors.bookingCloseAt && (
                <div className="invalid-feedback d-block">{fieldErrors.bookingCloseAt}</div>
              )}
            </div>
          </div>

          <hr className="my-4" />

          <div className="d-flex justify-content-end gap-2">
            <button
              type="button"
              className="btn btn-outline-secondary"
              onClick={handleCancel}
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
