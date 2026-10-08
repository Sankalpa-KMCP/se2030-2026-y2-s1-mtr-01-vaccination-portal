import React, { useCallback, useEffect, useId, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import PageHeader from '../../../components/common/PageHeader';
import AppointmentStatusBadge from '../../appointment-management/components/AppointmentStatusBadge';
import { getVaccinatorQueue } from '../../appointment-management/api/checkInApi';
import {
  createVaccinationDraft,
  listMyVaccinationDrafts,
} from '../api/vaccinationDraftApi';
import { getLocalCalendarToday, toApiIsoDate } from '../utils/dateUtils';

const ACTIVE_DRAFT_STATUSES = ['IN_PROGRESS', 'READY_FOR_FINALIZATION'];

/**
 * Vaccinator Check-In Queue Page (MF-05).
 *
 * Dedicated clinical read-only work queue for Vaccinators:
 * - Lists checked-in citizens ready for vaccination in the vaccinator's assigned operational session.
 * - Absolutely NO raw manual Time Slot ID entry.
 * - Allows "Start Vaccination" which creates/opens the draft for that appointment.
 * - Strictly NO check-in mutation controls (Check-In, Reverse Check-In, No-Show are owned by Admin/Center Manager).
 *
 * @param {Object} props
 * @param {Array} [props.sessions] - Optional assigned operational sessions.
 * @param {number|string} [props.initialSlotId] - Optional initial slot ID.
 */
export default function VaccinatorCheckInQueuePage({
  sessions: propSessions,
  initialSlotId,
} = {}) {
  const navigate = useNavigate();
  const sessionSelectorId = useId();

  const availableSessions = Array.isArray(propSessions) ? propSessions : [];
  const [selectedSessionId, setSelectedSessionId] = useState(() => {
    if (initialSlotId !== undefined && initialSlotId !== null) {
      const match = availableSessions.find(
        (s) => String(s.slotId) === String(initialSlotId) || String(s.id) === String(initialSlotId)
      );
      if (match) return match.id;
    }
    return availableSessions.length > 0 ? availableSessions[0].id : null;
  });

  const activeSession =
    availableSessions.find((s) => s.id === selectedSessionId) ||
    (availableSessions.length === 1 ? availableSessions[0] : null);

  const [queue, setQueue] = useState(null);
  const [queueLoading, setQueueLoading] = useState(false);
  const [queueError, setQueueError] = useState(null);
  const [drafts, setDrafts] = useState([]);
  const [processingId, setProcessingId] = useState(null);
  const [feedback, setFeedback] = useState(null);

  // Fetch active drafts to know if a draft already exists for an appointment
  const loadDrafts = useCallback(async () => {
    try {
      const data = await listMyVaccinationDrafts();
      setDrafts(Array.isArray(data) ? data : []);
    } catch {
      setDrafts([]);
    }
  }, []);

  useEffect(() => {
    loadDrafts();
  }, [loadDrafts]);

  // Load the queue for the active session's slot
  const loadQueue = useCallback(async (slotId) => {
    const parsed = parseInt(slotId, 10);
    if (Number.isNaN(parsed) || parsed <= 0) return;

    setQueueLoading(true);
    setQueueError(null);
    setFeedback(null);

    try {
      const data = await getVaccinatorQueue(parsed);
      setQueue(Array.isArray(data) ? data : []);
    } catch (err) {
      setQueue(null);
      setQueueError(err.message || 'Failed to fetch the checked-in queue.');
    } finally {
      setQueueLoading(false);
    }
  }, []);

  useEffect(() => {
    if (activeSession && activeSession.slotId) {
      loadQueue(activeSession.slotId);
    } else {
      setQueue(null);
      setQueueError(null);
      setQueueLoading(false);
    }
  }, [activeSession, loadQueue]);

  const handleSessionChange = (e) => {
    const nextId = e.target.value ? Number(e.target.value) : null;
    setSelectedSessionId(nextId);
  };

  const handleStartVaccination = async (appointment) => {
    setProcessingId(appointment.id);
    setFeedback(null);

    try {
      // Check if an active unfinalized draft already exists
      const existingDraft = drafts.find(
        (d) => d.appointmentId === appointment.id && ACTIVE_DRAFT_STATUSES.includes(d.status)
      );

      if (existingDraft) {
        navigate('/app/staff/vaccination-drafts', {
          state: { preselectedAppointment: appointment },
        });
        return;
      }

      // Create new draft
      const localToday = getLocalCalendarToday();
      await createVaccinationDraft({
        appointmentId: appointment.id,
        administrationTime: toApiIsoDate(localToday),
      });

      navigate('/app/staff/vaccination-drafts', {
        state: { preselectedAppointment: appointment },
      });
    } catch (err) {
      setFeedback({
        type: 'danger',
        message: err.message || `Unable to start vaccination draft for Appointment #${appointment.id}.`,
      });
    } finally {
      setProcessingId(null);
    }
  };

  return (
    <div>
      <PageHeader
        title="Check-In Queue"
        subtitle="Citizens checked in and ready for vaccination."
      />

      {feedback && (
        <div className={`alert alert-${feedback.type} alert-dismissible mb-4`} role="alert">
          <div>{feedback.message}</div>
          <button
            type="button"
            className="btn-close"
            aria-label="Close"
            onClick={() => setFeedback(null)}
          />
        </div>
      )}

      {/* Operational Session Banner / Selector */}
      {availableSessions.length > 0 ? (
        <div className="card border mb-4 bg-light shadow-sm">
          <div className="card-body p-3 d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3">
            <div>
              <div className="d-flex align-items-center gap-2 mb-1">
                <span className="badge bg-success">ACTIVE SESSION</span>
                <span className="fw-bold text-dark">{activeSession?.centerName || 'Assigned Center'}</span>
              </div>
              <div className="text-secondary small">
                {[
                  activeSession?.date,
                  activeSession?.startTime && activeSession?.endTime
                    ? `${activeSession.startTime} – ${activeSession.endTime}`
                    : null,
                ]
                  .filter(Boolean)
                  .join(' · ')}
              </div>
            </div>

            {availableSessions.length > 1 && (
              <div className="d-flex align-items-center gap-2">
                <label htmlFor={sessionSelectorId} className="small fw-semibold text-secondary text-nowrap mb-0">
                  Switch Session:
                </label>
                <select
                  id={sessionSelectorId}
                  className="form-select form-select-sm bg-white"
                  value={activeSession?.id || ''}
                  onChange={handleSessionChange}
                  disabled={queueLoading}
                >
                  {availableSessions.map((session) => (
                    <option key={session.id} value={session.id}>
                      {session.date} • {session.startTime}–{session.endTime} • {session.centerName}
                    </option>
                  ))}
                </select>
              </div>
            )}
          </div>
        </div>
      ) : (
        <div className="alert alert-light border mb-4 text-center py-4">
          <p className="text-muted small mb-0">
            No active vaccination session is currently assigned to you.
          </p>
        </div>
      )}

      {/* Queue Loading State */}
      {queueLoading && (
        <div className="text-center py-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Loading queue…</span>
          </div>
        </div>
      )}

      {/* Queue Error State */}
      {!queueLoading && queueError && (
        <div className="alert alert-danger mb-4" role="alert">
          <strong>Error Loading Queue:</strong> {queueError}
          <div className="mt-2">
            <button
              type="button"
              className="btn btn-outline-danger btn-sm"
              onClick={() => activeSession?.slotId && loadQueue(activeSession.slotId)}
            >
              Retry
            </button>
          </div>
        </div>
      )}

      {/* Empty Queue State */}
      {!queueLoading && !queueError && activeSession && queue !== null && queue.length === 0 && (
        <div className="card border-0 shadow-sm text-center py-5 px-3">
          <div className="card-body">
            <div className="text-muted mb-2">
              <i className="bi bi-person-check fs-1 text-secondary" aria-hidden="true" />
            </div>
            <h3 className="h5 fw-semibold text-dark mb-1">Queue Empty</h3>
            <p className="text-muted small mb-0">
              No checked-in patients are currently ready for vaccination.
            </p>
          </div>
        </div>
      )}

      {/* Populated Queue View */}
      {!queueLoading && !queueError && activeSession && queue !== null && queue.length > 0 && (
        <div className="card border-0 shadow-sm">
          <div className="card-header bg-white py-3 border-bottom d-flex justify-content-between align-items-center">
            <h2 className="h6 fw-bold mb-0 text-dark">
              Checked-In Patients ({queue.length})
            </h2>
            <button
              type="button"
              className="btn btn-outline-secondary btn-sm"
              onClick={() => activeSession?.slotId && loadQueue(activeSession.slotId)}
            >
              Refresh Queue
            </button>
          </div>

          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0" aria-label="Checked-in vaccination queue">
              <thead className="table-light">
                <tr>
                  <th scope="col">Appointment</th>
                  <th scope="col">Citizen</th>
                  <th scope="col">Campaign</th>
                  <th scope="col">Status</th>
                  <th scope="col" className="text-end">Actions</th>
                </tr>
              </thead>
              <tbody>
                {queue.map((appointment) => {
                  const hasActiveDraft = drafts.some(
                    (d) => d.appointmentId === appointment.id && ACTIVE_DRAFT_STATUSES.includes(d.status)
                  );
                  const isProcessing = processingId === appointment.id;

                  return (
                    <tr key={appointment.id}>
                      <td className="fw-semibold">
                        Appointment #{appointment.id}
                      </td>
                      <td>
                        {appointment.citizenName || `Citizen #${appointment.citizenId}`}
                      </td>
                      <td>
                        {appointment.campaignName || `Campaign #${appointment.campaignId}`}
                      </td>
                      <td>
                        <AppointmentStatusBadge status={appointment.status} />
                      </td>
                      <td className="text-end">
                        <button
                          type="button"
                          className="btn btn-primary btn-sm"
                          disabled={isProcessing}
                          onClick={() => handleStartVaccination(appointment)}
                          aria-label={`Start vaccination for appointment ${appointment.id}`}
                        >
                          {isProcessing ? (
                            <>
                              <span
                                className="spinner-border spinner-border-sm me-1"
                                role="status"
                                aria-hidden="true"
                              />
                              Starting…
                            </>
                          ) : hasActiveDraft ? (
                            'Open Draft'
                          ) : (
                            'Start Vaccination'
                          )}
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
