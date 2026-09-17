import React, { useState } from 'react';
import { rescheduleAppointment } from '../api/appointmentApi';
import { searchAvailableAppointments } from '../api/availabilityApi';

/**
 * Reschedule Dialog modal component allowing a citizen to select a new slot and reschedule an appointment (US-03).
 */
export default function RescheduleDialog({ appointment, citizenId, onClose, onSuccess }) {
  const getTodayStr = () => new Date().toISOString().split('T')[0];
  const getNextWeekStr = () => {
    const d = new Date();
    d.setDate(d.getDate() + 7);
    return d.toISOString().split('T')[0];
  };

  const [newSlotId, setNewSlotId] = useState('');
  const [fromDate, setFromDate] = useState(getTodayStr());
  const [toDate, setToDate] = useState(getNextWeekStr());
  const [isStaffOverride, setIsStaffOverride] = useState(false);
  const [staffReason, setStaffReason] = useState('');

  const [availableSlots, setAvailableSlots] = useState([]);
  const [searching, setSearching] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  if (!appointment) return null;

  const handleSearchSlots = async () => {
    setError(null);
    setSearching(true);
    try {
      const results = await searchAvailableAppointments({
        citizenId,
        campaignId: appointment.campaignId,
        fromDate,
        toDate,
      });
      setAvailableSlots(results || []);
    } catch (err) {
      setError(err.message || 'Failed to search available slots.');
    } finally {
      setSearching(false);
    }
  };

  const handleRescheduleSubmit = async (e) => {
    e.preventDefault();
    if (!newSlotId) {
      setError('Please select or specify a new slot ID.');
      return;
    }

    setError(null);
    setSubmitting(true);

    try {
      const result = await rescheduleAppointment(appointment.id, {
        requestingCitizenId: citizenId,
        newSlotId,
        isStaffOverride,
        staffReason: isStaffOverride ? staffReason : null,
      });
      onSuccess(result);
    } catch (err) {
      let userMessage = err.message || 'Failed to reschedule appointment.';

      if (err.status === 403 || err.error === 'FORBIDDEN') {
        userMessage = '24-Hour Notice Rule (MF03-BR-04): Rescheduling within 24 hours of session start requires staff override with reason.';
      } else if (err.status === 409 || err.error === 'CONFLICT') {
        userMessage = 'Slot Unavailable: The selected time slot does not have available capacity or conflicts with another booking.';
      }

      setError(userMessage);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="modal fade show d-block bg-dark bg-opacity-50" tabIndex="-1" role="dialog" aria-modal="true">
      <div className="modal-dialog modal-dialog-centered modal-lg" role="document">
        <div className="modal-content border-0 shadow">
          <div className="modal-header bg-light">
            <h5 className="modal-title fw-bold">Reschedule Appointment #{appointment.id}</h5>
            <button type="button" className="btn-close" aria-label="Close" onClick={onClose}></button>
          </div>

          <div className="modal-body p-4">
            {error && (
              <div className="alert alert-danger border-0 shadow-sm mb-3" role="alert">
                <strong className="d-block mb-1">Reschedule Request Rejected</strong>
                <small>{error}</small>
              </div>
            )}

            <div className="bg-light p-3 rounded mb-3 border">
              <small className="text-muted d-block">Current Booking Details</small>
              <div className="row mt-1">
                <div className="col-6"><strong>Campaign:</strong> #{appointment.campaignId}</div>
                <div className="col-6"><strong>Current Slot:</strong> #{appointment.slotId}</div>
              </div>
            </div>

            <form onSubmit={handleRescheduleSubmit}>
              <div className="mb-3">
                <label className="form-label fw-semibold">Step 1: Search New Available Slots</label>
                <div className="row g-2">
                  <div className="col-sm-5">
                    <input
                      type="date"
                      className="form-control form-control-sm"
                      value={fromDate}
                      onChange={(e) => setFromDate(e.target.value)}
                    />
                  </div>
                  <div className="col-sm-5">
                    <input
                      type="date"
                      className="form-control form-control-sm"
                      value={toDate}
                      onChange={(e) => setToDate(e.target.value)}
                    />
                  </div>
                  <div className="col-sm-2">
                    <button
                      type="button"
                      className="btn btn-sm btn-outline-primary w-100"
                      onClick={handleSearchSlots}
                      disabled={searching}
                    >
                      {searching ? '...' : 'Search'}
                    </button>
                  </div>
                </div>
              </div>

              {availableSlots.length > 0 && (
                <div className="mb-3">
                  <label className="form-label fw-semibold">Available Slots Found</label>
                  <div className="list-group max-vh-25 overflow-auto">
                    {availableSlots.map((s) => (
                      <button
                        key={s.slotId}
                        type="button"
                        className={`list-group-item list-group-item-action d-flex justify-content-between align-items-center ${
                          String(newSlotId) === String(s.slotId) ? 'active' : ''
                        }`}
                        onClick={() => setNewSlotId(s.slotId)}
                      >
                        <div>
                          <strong>{s.centerName || `Center #${s.centerId}`}</strong>
                          <div className="small opacity-75">
                            {new Date(s.startAt).toLocaleString()} - {new Date(s.endAt).toLocaleTimeString()}
                          </div>
                        </div>
                        <span className="badge bg-light text-dark">{s.remainingCapacity} Left</span>
                      </button>
                    ))}
                  </div>
                </div>
              )}

              <div className="mb-3">
                <label htmlFor="newSlotIdInput" className="form-label fw-semibold">
                  Step 2: Selected New Slot ID <span className="text-danger">*</span>
                </label>
                <input
                  type="number"
                  id="newSlotIdInput"
                  className="form-control"
                  value={newSlotId}
                  onChange={(e) => {
                    const val = e.target.value;
                    if (val === '') {
                      setNewSlotId('');
                      return;
                    }
                    const cleanVal = val.replace(/[^0-9]/g, '');
                    if (cleanVal === '') {
                      setNewSlotId('');
                      return;
                    }
                    const num = parseInt(cleanVal, 10);
                    if (num > 0) {
                      setNewSlotId(String(num));
                    }
                  }}
                  onKeyDown={(e) => {
                    if (e.key === '-' || e.key === 'e' || e.key === '+' || e.key === '.') {
                      e.preventDefault();
                    }
                  }}
                  placeholder="Enter or select slot ID"
                  min="1"
                  step="1"
                  required
                />
              </div>

              <div className="form-check mb-3">
                <input
                  type="checkbox"
                  id="staffOverrideCheck"
                  className="form-check-input"
                  checked={isStaffOverride}
                  onChange={(e) => setIsStaffOverride(e.target.checked)}
                />
                <label htmlFor="staffOverrideCheck" className="form-check-label small">
                  Apply Staff Override (Required if within 24 hours of session start per MF03-BR-04)
                </label>
              </div>

              {isStaffOverride && (
                <div className="mb-3">
                  <label htmlFor="staffReasonInput" className="form-label fw-semibold">
                    Staff Override Reason <span className="text-danger">*</span>
                  </label>
                  <input
                    type="text"
                    id="staffReasonInput"
                    className="form-control"
                    value={staffReason}
                    onChange={(e) => setStaffReason(e.target.value)}
                    placeholder="Enter reason for late rescheduling"
                    required={isStaffOverride}
                  />
                </div>
              )}

              <div className="modal-footer px-0 pb-0">
                <button type="button" className="btn btn-secondary" onClick={onClose} disabled={submitting}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary fw-bold" disabled={submitting || !newSlotId}>
                  {submitting ? 'Rescheduling...' : 'Confirm Reschedule'}
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}
