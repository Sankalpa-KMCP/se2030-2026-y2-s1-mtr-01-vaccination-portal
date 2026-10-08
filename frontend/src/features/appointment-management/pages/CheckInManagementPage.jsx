import React, { useState, useEffect, useCallback, useRef } from 'react';
import CheckInPanel from '../components/CheckInPanel';
import PageHeader from '../../../components/common/PageHeader';
import {
  getVaccinatorQueue,
  checkIn,
  reverseCheckIn,
  markNoShow,
} from '../api/checkInApi';
import { listTimeSlots } from '../../session-management/api/sessionApi';
import { useAuth } from '../../../auth/context/AuthContext';
import { hasRole } from '../../../auth/utils/roleUtils';

/**
 * Staff-facing Vaccinator Queue & Check-In Management Page.
 * Restricted to ADMIN, CENTER_MANAGER, and VACCINATOR. Vaccinators receive
 * read-only queue inspection while mutation controls remain manager-only.
 * Provides live queue inspection per time slot, arrival check-in with staff override,
 * check-in reversal, and mark no-show capabilities.
 *
 * <p><b>User Story:</b>
 * <ul>
 *   <li>US-26: Vaccinator views list of checked-in appointments; clinic staff process arrival and queue status.</li>
 * </ul>
 *
 * <p><b>Business Rules Handled in UI:</b>
 * <ul>
 *   <li>MF03-BR-09: Standard check-in window is 30 minutes before to 30 minutes after slot start; handles override reason prompts.</li>
 *   <li>MF03-BR-10: The vaccinator queue contains CHECKED_IN appointments.</li>
 *   <li>MF03-BR-15: Reverse check-in resets erroneous check-in back to CONFIRMED.</li>
 * </ul>
 */
export default function CheckInManagementPage() {
  const { user } = useAuth();
  const canMutate = hasRole(user, 'ROLE_ADMIN', 'ROLE_CENTER_MANAGER');

  const [slotId, setSlotId] = useState('');
  const [loadedSlotId, setLoadedSlotId] = useState(null);
  const [queue, setQueue] = useState(null);
  const [loading, setLoading] = useState(false);
  const [processingId, setProcessingId] = useState(null);
  const [feedback, setFeedback] = useState(null);

  // Optional Session -> TimeSlot lookup helper
  const [lookupSessionId, setLookupSessionId] = useState('');
  const [availableSlots, setAvailableSlots] = useState([]);
  const [loadingSlots, setLoadingSlots] = useState(false);
  const queueRequestRef = useRef(0);
  const activeQueueRef = useRef({ slotId: null, appointments: [] });

  const invalidateQueueContext = useCallback(() => {
    queueRequestRef.current += 1;
    activeQueueRef.current = { slotId: null, appointments: [] };
    setLoadedSlotId(null);
    setQueue(null);
    setLoading(false);
  }, []);

  const updateSlotId = useCallback((nextSlotId) => {
    invalidateQueueContext();
    setFeedback(null);
    setSlotId(nextSlotId);
  }, [invalidateQueueContext]);

  const fetchQueue = useCallback(async (targetSlotId) => {
    const parsedSlotId = Number(targetSlotId);
    if (!Number.isInteger(parsedSlotId) || parsedSlotId <= 0) return;

    const normalizedSlotId = String(parsedSlotId);
    const requestId = ++queueRequestRef.current;
    setLoading(true);
    setFeedback(null);
    setQueue(null);
    setLoadedSlotId(null);
    activeQueueRef.current = { slotId: null, appointments: [] };

    try {
      const data = await getVaccinatorQueue(normalizedSlotId);
      if (requestId === queueRequestRef.current) {
        const appointments = Array.isArray(data) ? data : [];
        setQueue(appointments);
        setLoadedSlotId(normalizedSlotId);
        activeQueueRef.current = { slotId: normalizedSlotId, appointments };
      }
    } catch (err) {
      if (requestId === queueRequestRef.current) {
        setFeedback({
          type: 'danger',
          title: 'Error Loading Queue',
          message: err.message || `Failed to fetch vaccinator queue for Slot #${normalizedSlotId}.`,
        });
        setQueue(null);
        setLoadedSlotId(null);
        activeQueueRef.current = { slotId: null, appointments: [] };
      }
    } finally {
      if (requestId === queueRequestRef.current) {
        setLoading(false);
      }
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
        updateSlotId(String(slots[0].id));
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

  const getCurrentMutationSlot = (appointmentId) => {
    const current = activeQueueRef.current;
    const belongsToCurrentQueue = current.slotId && current.appointments.some(
      (appointment) => appointment.id === appointmentId && String(appointment.slotId) === current.slotId,
    );
    if (!belongsToCurrentQueue) {
      setFeedback({
        type: 'danger',
        title: 'Queue Context Changed',
        message: 'Reload the current slot before performing an appointment action.',
      });
      return null;
    }
    return current.slotId;
  };

  const handleCheckIn = async (appointmentId, { isStaffOverride, staffReason }) => {
    const actionSlotId = getCurrentMutationSlot(appointmentId);
    if (!actionSlotId) return;
    setProcessingId(appointmentId);
    setFeedback(null);

    try {
      const updated = await checkIn(appointmentId, isStaffOverride, staffReason);
      setFeedback({
        type: 'success',
        title: 'Check-In Complete',
        message: `Appointment #${updated.id} for Citizen #${updated.citizenId} is successfully CHECKED_IN.`,
      });
      if (activeQueueRef.current.slotId === actionSlotId) {
        fetchQueue(actionSlotId);
      }
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

  const handleReverseCheckIn = async (appointmentId, reason) => {
    const actionSlotId = getCurrentMutationSlot(appointmentId);
    if (!actionSlotId) return;
    setProcessingId(appointmentId);
    setFeedback(null);

    try {
      const updated = await reverseCheckIn(appointmentId, reason);
      setFeedback({
        type: 'success',
        title: 'Check-In Reversed',
        message: `Appointment #${updated.id} has been returned to CONFIRMED status.`,
      });
      if (activeQueueRef.current.slotId === actionSlotId) {
        fetchQueue(actionSlotId);
      }
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
    const actionSlotId = getCurrentMutationSlot(appointmentId);
    if (!actionSlotId) return;
    setProcessingId(appointmentId);
    setFeedback(null);

    try {
      const updated = await markNoShow(appointmentId);
      setFeedback({
        type: 'success',
        title: 'Marked No-Show',
        message: `Appointment #${updated.id} has been marked as NO_SHOW.`,
      });
      if (activeQueueRef.current.slotId === actionSlotId) {
        fetchQueue(actionSlotId);
      }
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
        subtitle={
          canMutate
            ? 'Monitor the on-site clinic queue, process arrival check-ins, apply authorized overrides, or record no-shows.'
            : 'Monitor the on-site clinic queue for your active session slot.'
        }
      />

      {/* Primary Slot Selector Card */}
      <div className="card shadow-sm border-0 mb-4">
        <div className="card-body p-4">
          <form onSubmit={handleSlotSubmit} className="row g-3 align-items-end">
            <div className="col-md-6 col-lg-5">
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
                    updateSlotId('');
                    return;
                  }
                  const cleanVal = val.replace(/[^0-9]/g, '');
                  if (cleanVal === '') {
                    updateSlotId('');
                    return;
                  }
                  const num = parseInt(cleanVal, 10);
                  if (num > 0) {
                    updateSlotId(String(num));
                  }
                }}
                onKeyDown={(e) => {
                  if (e.key === '-' || e.key === 'e' || e.key === '+' || e.key === '.') {
                    e.preventDefault();
                  }
                }}
                placeholder="Enter the active Time Slot ID"
                min="1"
                step="1"
                required
              />
            </div>
            <div className="col-md-4 col-lg-3">
              <button
                type="submit"
                className="btn btn-primary px-4 fw-bold shadow-sm w-100"
                disabled={loading}
              >
                {loading ? 'Loading Queue...' : 'Load Queue'}
              </button>
            </div>
          </form>

          {/* Quick lookup by Session ID helper - restricted to roles authorized for TimeSlot lookup */}
          {canMutate && (
            <div className="mt-3 pt-3 border-top">
              <details>
                <summary className="text-secondary small fw-semibold cursor-pointer">
                  <i className="bi bi-search me-1" aria-hidden="true"></i>
                  Don't know the Time Slot ID? Find slots by Session ID
                </summary>
                <div className="row g-2 align-items-center mt-2">
                  <div className="col-auto">
                    <label htmlFor="lookupSessionIdInput" className="visually-hidden">Session ID</label>
                    <input
                      type="number"
                      id="lookupSessionIdInput"
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
                      <label htmlFor="availableTimeSlotSelect" className="visually-hidden">Available Time Slot</label>
                      <select
                        id="availableTimeSlotSelect"
                        className="form-select form-select-sm"
                        value={slotId}
                        onChange={(e) => updateSlotId(e.target.value)}
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
          )}
        </div>
      </div>

      {/* Feedback Notification */}
      {feedback && (
        <div
          className={`alert alert-${feedback.type} alert-dismissible fade show border-0 shadow-sm mb-4`}
          role="alert"
        >
          <p className="h6 alert-heading fw-bold mb-1">{feedback.title}</p>
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
        <div className="text-center py-5" role="status" aria-live="polite" aria-atomic="true">
          <div className="spinner-border text-primary" style={{ width: '3rem', height: '3rem' }} aria-hidden="true"></div>
          <p className="text-muted mt-2 fw-semibold">Loading queue for Slot #{slotId}...</p>
        </div>
      )}

      {/* Queue Results List */}
      {!loading && loadedSlotId !== null && queue !== null && (
        <section className="queue-section" aria-labelledby="check-in-queue-title">
          <div className="visually-hidden" role="status" aria-live="polite" aria-atomic="true">
            Queue loaded for Slot #{loadedSlotId}. {queue.length} {queue.length === 1 ? 'appointment' : 'appointments'} found.
          </div>
          <div className="d-flex justify-content-between align-items-center mb-3">
            <h2 id="check-in-queue-title" className="h5 fw-bold mb-0">
              Vaccinator Queue for Slot #{loadedSlotId}
            </h2>
            <span className="badge bg-secondary fs-7">
              {queue.length} {queue.length === 1 ? 'Appointment' : 'Appointments'}
            </span>
          </div>

          {queue.length === 0 ? (
            <div className="alert alert-info border-0 shadow-sm p-4 text-center">
              <h3 className="h6 alert-heading fw-bold mb-1">Queue is Empty</h3>
              <p className="mb-0 text-muted small">
                No checked-in appointments found for Slot #{loadedSlotId}.
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
                  canMutate={canMutate}
                />
              ))}
            </div>
          )}
        </section>
      )}
    </div>
  );
}
