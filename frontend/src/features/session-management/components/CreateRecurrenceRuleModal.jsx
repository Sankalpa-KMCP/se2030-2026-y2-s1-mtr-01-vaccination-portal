import React, { useState, useEffect } from 'react';
import { createRecurrenceRule } from '../api/sessionApi';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';

const WEEKDAYS = [
  { id: 'MONDAY', label: 'Mon' },
  { id: 'TUESDAY', label: 'Tue' },
  { id: 'WEDNESDAY', label: 'Wed' },
  { id: 'THURSDAY', label: 'Thu' },
  { id: 'FRIDAY', label: 'Fri' },
  { id: 'SATURDAY', label: 'Sat' },
  { id: 'SUNDAY', label: 'Sun' },
];

export default function CreateRecurrenceRuleModal({
  isOpen,
  onClose,
  onSuccess,
  campaigns = [],
  centersMap = {},
}) {
  const [campaignId, setCampaignId] = useState('');
  const [centerId, setCenterId] = useState('');
  const [selectedWeekdays, setSelectedWeekdays] = useState([]);
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [startTime, setStartTime] = useState('09:00');
  const [endTime, setEndTime] = useState('17:00');

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [validationErrors, setValidationErrors] = useState({});

  useEffect(() => {
    if (isOpen) {
      setCampaignId(campaigns.length === 1 ? String(campaigns[0].id) : '');
      setCenterId('');
      setSelectedWeekdays(['MONDAY', 'WEDNESDAY', 'FRIDAY']);
      setStartDate('');
      setEndDate('');
      setStartTime('09:00');
      setEndTime('17:00');
      setError(null);
      setValidationErrors({});
    }
  }, [isOpen, campaigns]);

  if (!isOpen) return null;

  const selectedCenter = centerId ? centersMap[centerId] : null;
  const authoritativeTimezone = selectedCenter?.timezone || null;

  const toggleWeekday = (day) => {
    setSelectedWeekdays((prev) =>
      prev.includes(day) ? prev.filter((d) => d !== day) : [...prev, day]
    );
  };

  const validate = () => {
    const errs = {};
    if (!campaignId) errs.campaignId = 'Campaign is required';
    if (!centerId) errs.centerId = 'Vaccination center is required';
    if (!authoritativeTimezone) {
      errs.centerId = 'Selected center does not have authoritative timezone metadata configured';
    }
    if (!selectedWeekdays || selectedWeekdays.length === 0) {
      errs.weekdays = 'At least one recurrence weekday must be selected';
    }
    if (!startDate) errs.startDate = 'Start date is required';
    if (!endDate) errs.endDate = 'End date is required';
    if (startDate && endDate && startDate > endDate) {
      errs.endDate = 'End date must be on or after start date';
    }
    if (!startTime) errs.startTime = 'Start time is required';
    if (!endTime) errs.endTime = 'End time is required';
    if (startTime && endTime && startTime >= endTime) {
      errs.endTime = 'End time must be after start time';
    }

    setValidationErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) return;

    // Center timezone must be authoritative; do not fall back to browser or default
    if (!authoritativeTimezone) {
      setError(new Error('Cannot create recurrence rule: Selected center has no authoritative timezone.'));
      return;
    }

    setLoading(true);
    setError(null);

    const formattedStartTime = startTime.length === 5 ? `${startTime}:00` : startTime;
    const formattedEndTime = endTime.length === 5 ? `${endTime}:00` : endTime;

    const payload = {
      campaignId: Number(campaignId),
      centerId: Number(centerId),
      weekdays: selectedWeekdays,
      startDate,
      endDate,
      startTime: formattedStartTime,
      endTime: formattedEndTime,
      timezone: authoritativeTimezone,
    };

    try {
      const created = await createRecurrenceRule(payload);
      setLoading(false);
      onSuccess(created);
      onClose();
    } catch (err) {
      setLoading(false);
      setError(err);
    }
  };

  return (
    <div
      className="modal fade show d-block"
      tabIndex="-1"
      role="dialog"
      aria-labelledby="create-recurrence-rule-title"
      aria-modal="true"
      style={{ backgroundColor: 'rgba(0, 0, 0, 0.5)' }}
    >
      <div className="modal-dialog modal-lg modal-dialog-centered" role="document">
        <div className="modal-content shadow border-0">
          <div className="modal-header bg-light">
            <h2 className="modal-title h5 text-dark fw-bold mb-0" id="create-recurrence-rule-title">
              Create Session Recurrence Rule
            </h2>
            <button
              type="button"
              className="btn-close"
              aria-label="Close modal"
              onClick={onClose}
              disabled={loading}
            />
          </div>

          <form onSubmit={handleSubmit} noValidate>
            <div className="modal-body p-4">
              {error && <ApiErrorAlert error={error} />}

              <div className="row g-3">
                {/* Campaign Selection */}
                <div className="col-12 col-md-6">
                  <label htmlFor="create-rule-campaign" className="form-label fw-semibold">
                    Campaign <span className="text-danger">*</span>
                  </label>
                  <select
                    id="create-rule-campaign"
                    className={`form-select ${validationErrors.campaignId ? 'is-invalid' : ''}`}
                    value={campaignId}
                    onChange={(e) => setCampaignId(e.target.value)}
                    disabled={loading}
                  >
                    <option value="">-- Select Campaign --</option>
                    {campaigns.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name} ({c.campaignCode})
                      </option>
                    ))}
                  </select>
                  {validationErrors.campaignId && (
                    <div className="invalid-feedback">{validationErrors.campaignId}</div>
                  )}
                </div>

                {/* Center Selection */}
                <div className="col-12 col-md-6">
                  <label htmlFor="create-rule-center" className="form-label fw-semibold">
                    Vaccination Center <span className="text-danger">*</span>
                  </label>
                  <select
                    id="create-rule-center"
                    className={`form-select ${validationErrors.centerId ? 'is-invalid' : ''}`}
                    value={centerId}
                    onChange={(e) => setCenterId(e.target.value)}
                    disabled={loading}
                  >
                    <option value="">-- Select Center --</option>
                    {Object.values(centersMap).map((cnt) => (
                      <option key={cnt.id} value={cnt.id}>
                        {cnt.name} ({cnt.centerCode})
                      </option>
                    ))}
                  </select>
                  {validationErrors.centerId && (
                    <div className="invalid-feedback">{validationErrors.centerId}</div>
                  )}
                  {authoritativeTimezone && (
                    <div className="form-text text-muted">
                      Authoritative timezone: <span className="badge bg-secondary">{authoritativeTimezone}</span>
                    </div>
                  )}
                </div>

                {/* Weekday Toggles */}
                <div className="col-12">
                  <label id="recurrence-weekdays-group-label" className="form-label fw-semibold d-block">
                    Recurrence Weekdays <span className="text-danger">*</span>
                  </label>
                  <div
                    className="btn-group flex-wrap w-100"
                    role="group"
                    aria-labelledby="recurrence-weekdays-group-label"
                  >
                    {WEEKDAYS.map((day) => {
                      const isSelected = selectedWeekdays.includes(day.id);
                      return (
                        <button
                          key={day.id}
                          type="button"
                          className={`btn btn-sm ${
                            isSelected ? 'btn-primary text-white fw-bold' : 'btn-outline-secondary'
                          }`}
                          onClick={() => toggleWeekday(day.id)}
                          disabled={loading}
                          aria-pressed={isSelected}
                        >
                          {day.label}
                        </button>
                      );
                    })}
                  </div>
                  {validationErrors.weekdays && (
                    <div className="text-danger small mt-1">{validationErrors.weekdays}</div>
                  )}
                </div>

                {/* Start & End Date */}
                <div className="col-12 col-md-6">
                  <label htmlFor="create-rule-start-date" className="form-label fw-semibold">
                    Start Date <span className="text-danger">*</span>
                  </label>
                  <input
                    type="date"
                    id="create-rule-start-date"
                    className={`form-control ${validationErrors.startDate ? 'is-invalid' : ''}`}
                    value={startDate}
                    onChange={(e) => setStartDate(e.target.value)}
                    disabled={loading}
                  />
                  {validationErrors.startDate && (
                    <div className="invalid-feedback">{validationErrors.startDate}</div>
                  )}
                </div>

                <div className="col-12 col-md-6">
                  <label htmlFor="create-rule-end-date" className="form-label fw-semibold">
                    End Date <span className="text-danger">*</span>
                  </label>
                  <input
                    type="date"
                    id="create-rule-end-date"
                    className={`form-control ${validationErrors.endDate ? 'is-invalid' : ''}`}
                    value={endDate}
                    onChange={(e) => setEndDate(e.target.value)}
                    disabled={loading}
                  />
                  {validationErrors.endDate && (
                    <div className="invalid-feedback">{validationErrors.endDate}</div>
                  )}
                </div>

                {/* Start & End Time (Wall clock) */}
                <div className="col-12 col-md-6">
                  <label htmlFor="create-rule-start-time" className="form-label fw-semibold">
                    Start Time <span className="text-danger">*</span>
                  </label>
                  <input
                    type="time"
                    id="create-rule-start-time"
                    className={`form-control ${validationErrors.startTime ? 'is-invalid' : ''}`}
                    value={startTime}
                    onChange={(e) => setStartTime(e.target.value)}
                    disabled={loading}
                  />
                  {validationErrors.startTime && (
                    <div className="invalid-feedback">{validationErrors.startTime}</div>
                  )}
                </div>

                <div className="col-12 col-md-6">
                  <label htmlFor="create-rule-end-time" className="form-label fw-semibold">
                    End Time <span className="text-danger">*</span>
                  </label>
                  <input
                    type="time"
                    id="create-rule-end-time"
                    className={`form-control ${validationErrors.endTime ? 'is-invalid' : ''}`}
                    value={endTime}
                    onChange={(e) => setEndTime(e.target.value)}
                    disabled={loading}
                  />
                  {validationErrors.endTime && (
                    <div className="invalid-feedback">{validationErrors.endTime}</div>
                  )}
                </div>
              </div>
            </div>

            <div className="modal-footer bg-light">
              <button
                type="button"
                className="btn btn-secondary"
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
                {loading ? 'Creating Rule...' : 'Create Recurrence Rule'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
