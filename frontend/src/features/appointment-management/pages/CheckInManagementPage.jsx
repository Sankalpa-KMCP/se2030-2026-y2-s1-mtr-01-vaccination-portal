import React, { useState, useEffect, useCallback } from 'react';
import CheckInPanel from '../components/CheckInPanel';
import PageHeader from '../../../components/common/PageHeader';
import {
  getVaccinatorQueue,
  checkIn,
  reverseCheckIn,
  markNoShow,
} from '../api/checkInApi';
import { listTimeSlots } from '../../session-management/api/sessionApi';

/**
 * Staff-facing Vaccinator Queue & Check-In Management Page (US-04).
 * Restricted to staff roles (ADMIN, HEALTHCARE_OFFICER, CENTER_MANAGER).
 * Provides live queue inspection per time slot, arrival check-in with staff override,
 * check-in reversal, and mark no-show capabilities.
 */
export default function CheckInManagementPage() {
  const [slotId, setSlotId] = useState('4001');
  const [queue, setQueue] = useState(null);
  const [loading, setLoading] = useState(false);
  const [processingId, setProcessingId] = useState(null);
  const [feedback, setFeedback] = useState(null);

  // Optional Session -> TimeSlot lookup helper
  const [lookupSessionId, setLookupSessionId] = useState('');
  const [availableSlots, setAvailableSlots] = useState([]);
  const [loadingSlots, setLoadingSlots] = useState(false);

  const fetchQueue = useCallback(async (targetSlotId) => {
    if (!targetSlotId) return;
    setLoading(true);
    setFeedback(null);

    try {
      const data = await getVaccinatorQueue(targetSlotId);
      setQueue(data || []);
    } catch (err) {
      setFeedback({
        type: 'danger',
        title: 'Error Loading Queue',
        message: err.message || `Failed to fetch vaccinator queue for Slot #${targetSlotId}.`,
      });
      setQueue([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (slotId) {
      fetchQueue(slotId);
    }
  }, [slotId, fetchQueue]);

  const handleSlotSubmit = (e) => {
    e.preventDefault();
    fetchQueue(slotId);
  };

  const handleLookupSessionSlots = async (e) => {
    e.preventDefault();
    if (!lookupSessionId) return;

    setLoadingSlots(true);
    try {
      const slots = await listTimeSlots(lookupSessionId);
      setAvailableSlots(slots || []);
      if (slots && slots.length > 0) {
        setSlotId(String(slots[0].id));
      }
    } catch (err) {
      setFeedback({
        type: 'danger',
        title: 'Session Lookup Failed',
        message: err.message || `Unable to load time slots for Session #${lookupSessionId}.`,
      });
      setAvailableSlots([]);
    } finally {
      setLoadingSlots(false);
    }
  };

  const handleCheckIn = async (appointmentId, { isStaffOverride, staffReason }) => {
    setProcessingId(appointmentId);
    setFeedback(null);

    try {
      const updated = await checkIn(appointmentId, isStaffOverride, staffReason);
      setFeedback({
        type: 'success',
        title: 'Check-In Complete',
        message: `Appointment #${updated.id} for Citizen #${updated.citizenId} is successfully CHECKED_IN.`,
      });
      fetchQueue(slotId);
    } catch (err) {
      let userMsg = err.message || 'Failed to complete check-in.';
      if (err.status === 400 && err.message && err.message.includes('window')) {
        userMsg = 'Outside Check-In Window: Citizen arrival must be within the scheduled slot window. Apply staff override with reason if authorized.';
      }
      setFeedback({
        type: 'danger',
        title: 'Check-In Rejected',
        message: userMsg,
      });
    } finally {
      setProcessingId(null);
    }
  };

  const handleReverseCheckIn = async (appointmentId) => {
    setProcessingId(appointmentId);
    setFeedback(null);

    try {
      const updated = await reverseCheckIn(appointmentId);
      setFeedback({
        type: 'success',
        title: 'Check-In Reversed',
        message: `Appointment #${updated.id} has been returned to CONFIRMED status.`,
      });
      fetchQueue(slotId);
    } catch (err) {
      setFeedback({
        type: 'danger',
        title: 'Reversal Failed',
        message: err.message || 'Unable to reverse check-in status.',
      });
    } finally {
      setProcessingId(null);
    }
  };

  const handleMarkNoShow = async (appointmentId) => {
    setProcessingId(appointmentId);
    setFeedback(null);

    try {
      const updated = await markNoShow(appointmentId);
      setFeedback({
        type: 'success',
        title: 'Marked No-Show',
        message: `Appointment #${updated.id} has been marked as NO_SHOW.`,
      });
      fetchQueue(slotId);
    } catch (err) {
      let userMsg = err.message || 'Failed to mark appointment as no-show.';
      if (err.status === 400 && err.message && err.message.includes('window')) {
        userMsg = 'Invalid Window: Cannot mark appointment as no-show until the designated check-in window has elapsed.';
      }
      setFeedback({
        type: 'danger',
        title: 'Action Rejected',
        message: userMsg,
      });
    } finally {
      setProcessingId(null);
    }
  };

  return (
    <div>
      <PageHeader
        title="Vaccinator Queue & Check-In"
        subtitle="Monitor the on-site clinic queue, process arrival check-ins, apply authorized overrides, or record no-shows."
      />

      {/* Primary Slot Selector Card */}
      <div className="card shadow-sm border-0 mb-4">
        <div className="card-body p-4">
          <form onSubmit={handleSlotSubmit} className="row g-3 align-items-end">
            <div className="col-md-4">
              <label htmlFor="slotIdInput" className="form-label fw-bold">
                Active Time Slot ID <span className="text-danger">*</span>
              </label>
              <input
                type="number"
                id="slotIdInput"
                className="form-control"
                value={slotId}
                onChange={(e) => {
                  const val = e.target.value;
                  if (val === '') {
                    setSlotId('');
                    return;
                  }
                  const cleanVal = val.replace(/[^0-9]/g, '');
                  if (cleanVal === '') {
                    setSlotId('');
                    return;
                  }
                  const num = parseInt(cleanVal, 10);
                  if (num > 0) {
                    setSlotId(String(num));
                  }
                }}
                onKeyDown={(e) => {
                  if (e.key === '-' || e.key === 'e' || e.key === '+' || e.key === '.') {
                    e.preventDefault();
                  }
                }}
                placeholder="e.g. 4001"
                min="1"
                step="1"
                required
              />
            </div>
            <div className="col-md-3">
              <button
                type="submit"
                className="btn btn-primary px-4 fw-bold shadow-sm w-100"
                disabled={loading}
              >
                {loading ? 'Loading Queue...' : 'Load Queue'}
              </button>
            </div>
            <div className="col-md-5 text-md-end text-muted small">
              Queue queries endpoint: <code>GET /api/v1/appointments/queue?slotId={slotId || '{slotId}'}</code>
            </div>
          </form>

          {/* Quick lookup by Session ID helper */}
          <div className="mt-3 pt-3 border-top">
            <details>
              <summary className="text-secondary small fw-semibold cursor-pointer">
                <i className="bi bi-search me-1" aria-hidden="true"></i>
                Don't know the Time Slot ID? Find slots by Session ID
              </summary>
              <div className="row g-2 align-items-center mt-2">
                <div className="col-auto">
                  <input
                    type="number"
                    className="form-control form-control-sm"
                    placeholder="Enter Session ID"
                    value={lookupSessionId}
                    onChange={(e) => {
                      const val = e.target.value;
                      if (val === '') {
                        setLookupSessionId('');
                        return;
                      }
                      const cleanVal = val.replace(/[^0-9]/g, '');
                      if (cleanVal === '') {
                        setLookupSessionId('');
                        return;
                      }
                      const num = parseInt(cleanVal, 10);
                      if (num > 0) {
                        setLookupSessionId(String(num));
                      }
                    }}
                    onKeyDown={(e) => {
                      if (e.key === '-' || e.key === 'e' || e.key === '+' || e.key === '.') {
                        e.preventDefault();
                      }
                    }}
                    min="1"
                    step="1"
                  />
                </div>
                <div className="col-auto">
                  <button
                    type="button"
                    className="btn btn-outline-secondary btn-sm"
                    disabled={loadingSlots || !lookupSessionId}
                    onClick={handleLookupSessionSlots}
                  >
                    {loadingSlots ? 'Searching...' : 'Find Slots'}
                  </button>
                </div>
                {availableSlots.length > 0 && (
                  <div className="col-auto">
                    <select
                      className="form-select form-select-sm"
                      value={slotId}
                      onChange={(e) => setSlotId(e.target.value)}
                    >
                      {availableSlots.map((s) => (
                        <option key={s.id} value={s.id}>
                          Slot #{s.id} ({s.startTime} - {s.endTime})
                        </option>
                      ))}
                    </select>
                  </div>
                )}
              </div>
            </details>
          </div>
        </div>
      </div>

      {/* Feedback Notification */}
      {feedback && (
        <div
          className={`alert alert-${feedback.type} alert-dismissible fade show border-0 shadow-sm mb-4`}
          role="alert"
        >
          <h4 className="h6 alert-heading fw-bold mb-1">{feedback.title}</h4>
          <p className="mb-0">{feedback.message}</p>
          <button
            type="button"
            className="btn-close"
            aria-label="Close"
            onClick={() => setFeedback(null)}
          ></button>
        </div>
      )}

      {/* Loading Spinner */}
      {loading && (
        <div className="text-center py-5">
          <div className="spinner-border text-primary" style={{ width: '3rem', height: '3rem' }} role="status">
            <span className="visually-hidden">Loading...</span>
          </div>
          <p className="text-muted mt-2 fw-semibold">Loading queue for Slot #{slotId}...</p>
        </div>
      )}

      {/* Queue Results List */}
      {!loading && queue !== null && (
        <div className="queue-section">
          <div className="d-flex justify-content-between align-items-center mb-3">
            <h3 className="h5 fw-bold mb-0">
              Vaccinator Queue for Slot #{slotId}
            </h3>
            <span className="badge bg-secondary fs-7">
              {queue.length} {queue.length === 1 ? 'Appointment' : 'Appointments'}
            </span>
          </div>

          {queue.length === 0 ? (
            <div className="alert alert-info border-0 shadow-sm p-4 text-center">
              <h4 className="h6 alert-heading fw-bold mb-1">Queue is Empty</h4>
              <p className="mb-0 text-muted small">
                No active confirmed or checked-in appointments found for Slot #{slotId}.
              </p>
            </div>
          ) : (
            <div className="queue-list">
              {queue.map((appointment) => (
                <CheckInPanel
                  key={appointment.id}
                  appointment={appointment}
                  onCheckIn={handleCheckIn}
                  onReverseCheckIn={handleReverseCheckIn}
                  onMarkNoShow={handleMarkNoShow}
                  isProcessing={processingId === appointment.id}
                />
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
