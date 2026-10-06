import React, { useCallback, useEffect, useId, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../../auth/context/AuthContext';
import { getVaccinatorQueue } from '../../appointment-management/api/checkInApi';
import {
  createVaccinationDraft,
  listMyVaccinationDrafts,
} from '../api/vaccinationDraftApi';
import { listVaccinatorCorrectionRequests } from '../api/certificateApi';
import AppointmentStatusBadge from '../../appointment-management/components/AppointmentStatusBadge';
import { getLocalCalendarToday, toApiIsoDate } from '../utils/dateUtils';

const VACCINATOR_ROLE = 'ROLE_VACCINATOR';

const ACTIVE_STATUSES = ['IN_PROGRESS', 'READY_FOR_FINALIZATION'];

const DRAFT_STATUS_LABELS = {
  IN_PROGRESS: 'In Progress',
  READY_FOR_FINALIZATION: 'Ready for Finalization',
  FINALIZED: 'Finalized',
};

function formatDateTime(isoString) {
  if (!isoString) return 'N/A';
  return new Date(isoString).toLocaleString(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  });
}

/**
 * Vaccinator operational workspace section rendered on the staff dashboard (Option B).
 * Compact, task-oriented dashboard providing:
 *  1. Header: "Vaccinator Dashboard" with subtitle "Manage today's vaccination work".
 *  2. Current Session Card: Center name, Date, Start-End time, Active Session status badge,
 *     and friendly "Change Session" dropdown selector.
 *  3. Summary Metric Cards: Waiting in Queue, Active Drafts, Corrections Needed.
 *  4. Two-Column Task Workspace:
 *     - Left: Checked-In Queue with citizen details and "Begin Vaccination Draft" action.
 *     - Right: Active Work with drafts ("Open Draft") and approved corrections ("ACTION REQUIRED", "Open Correction").
 *
 * @param {Object} props
 * @param {Array} [props.sessions] - Assigned operational sessions list for the vaccinator. Defaults to empty array if none assigned.
 * @param {number|string} [props.initialSlotId] - Optional initial slot identifier to load.
 */
export default function VaccinatorDashboardSection({
  sessions: propSessions,
  initialSlotId,
} = {}) {
  const { user } = useAuth();
  const isVaccinator = Boolean(user?.roles?.includes(VACCINATOR_ROLE));
  const sessionSelectorId = useId();

  // Sessions state: strictly requires real sessions passed via props; defaults to empty array
  const availableSessions = Array.isArray(propSessions) ? propSessions : [];
  const [selectedSessionId, setSelectedSessionId] = useState(() => {
    if (initialSlotId !== undefined && initialSlotId !== null) {
      const match = availableSessions.find((s) => String(s.slotId) === String(initialSlotId) || String(s.id) === String(initialSlotId));
      if (match) return match.id;
    }
    return availableSessions.length > 0 ? availableSessions[0].id : null;
  });

  const activeSession = availableSessions.find((s) => s.id === selectedSessionId) || (availableSessions.length === 1 ? availableSessions[0] : null);

  // Queue state
  const [loadedSlotId, setLoadedSlotId] = useState(null);
  const [queue, setQueue] = useState(null);
  const [queueLoading, setQueueLoading] = useState(false);
  const [queueError, setQueueError] = useState(null);
  const [pendingAppointmentIds, setPendingAppointmentIds] = useState(() => new Set());
  const pendingAppointmentIdsRef = useRef(new Set());
  const [feedback, setFeedback] = useState(null);

  // Monotonic queue request tracker to prevent out-of-order race conditions
  const queueRequestRef = useRef(0);

  // Drafts state
  const [drafts, setDrafts] = useState(null);
  const [draftsLoading, setDraftsLoading] = useState(true);
  const [draftsError, setDraftsError] = useState(null);
  const draftsRequestRef = useRef(0);

  // Correction requests state
  const [corrections, setCorrections] = useState(null);

  const refreshDrafts = useCallback(async () => {
    const requestId = ++draftsRequestRef.current;
    setDraftsLoading(true);
    setDraftsError(null);
    try {
      const data = await listMyVaccinationDrafts();
      if (requestId === draftsRequestRef.current) {
        setDrafts(Array.isArray(data) ? data : []);
      }
      return { ok: true, stale: requestId !== draftsRequestRef.current };
    } catch (err) {
      const message = err.message || 'Unable to load your vaccination drafts.';
      if (requestId === draftsRequestRef.current) {
        setDrafts(null);
        setDraftsError(message);
      }
      return { ok: false, stale: requestId !== draftsRequestRef.current, message };
    } finally {
      if (requestId === draftsRequestRef.current) {
        setDraftsLoading(false);
      }
    }
  }, []);

  const refreshCorrections = useCallback(async () => {
    try {
      const data = await listVaccinatorCorrectionRequests();
      setCorrections(Array.isArray(data) ? data : []);
    } catch {
      setCorrections([]);
    }
  }, []);

  useEffect(() => {
    if (isVaccinator) {
      refreshDrafts();
      refreshCorrections();
    }
  }, [isVaccinator, refreshDrafts, refreshCorrections]);

  // Execute loading queue for a target slot ID
  const executeLoadQueue = useCallback(async (targetSlotId) => {
    const parsed = parseInt(targetSlotId, 10);
    if (Number.isNaN(parsed) || parsed <= 0) return;

    const currentRequestId = ++queueRequestRef.current;
    setQueueLoading(true);
    setQueueError(null);
    setQueue(null);
    setLoadedSlotId(null);
    setFeedback(null);

    try {
      const data = await getVaccinatorQueue(parsed);
      if (currentRequestId === queueRequestRef.current) {
        setQueue(Array.isArray(data) ? data : []);
        setLoadedSlotId(String(parsed));
      }
    } catch (err) {
      if (currentRequestId === queueRequestRef.current) {
        setQueue(null);
        setLoadedSlotId(null);
        setQueueError(err.message || 'Failed to fetch the checked-in queue.');
      }
    } finally {
      if (currentRequestId === queueRequestRef.current) {
        setQueueLoading(false);
      }
    }
  }, []);

  // When activeSession changes or on initial mount, auto-load its queue
  useEffect(() => {
    if (isVaccinator && activeSession && activeSession.slotId) {
      executeLoadQueue(activeSession.slotId);
    } else if (!activeSession) {
      queueRequestRef.current += 1;
      setQueue(null);
      setLoadedSlotId(null);
      setQueueError(null);
      setQueueLoading(false);
    }
  }, [isVaccinator, activeSession, executeLoadQueue]);

  const handleSessionChange = (event) => {
    const newSessionId = event.target.value ? Number(event.target.value) : null;
    setSelectedSessionId(newSessionId);
    const nextSession = availableSessions.find((s) => s.id === newSessionId);
    if (nextSession && nextSession.slotId) {
      executeLoadQueue(nextSession.slotId);
    } else {
      queueRequestRef.current += 1;
      setQueue(null);
      setLoadedSlotId(null);
      setQueueError(null);
      setQueueLoading(false);
    }
  };

  const beginDraftFromAppointment = async (appointment) => {
    // Safety check: row must belong to currently loaded queue
    if (
      !loadedSlotId
      || String(appointment.slotId) !== loadedSlotId
      || !queue
      || !queue.some((a) => a.id === appointment.id && String(a.slotId) === loadedSlotId)
    ) {
      setFeedback({
        type: 'danger',
        title: 'Context Mismatch',
        message: 'Cannot begin draft: the selected appointment does not belong to the currently loaded queue.',
      });
      return;
    }

    if (pendingAppointmentIdsRef.current.has(appointment.id)) return;

    const nextPending = new Set(pendingAppointmentIdsRef.current);
    nextPending.add(appointment.id);
    pendingAppointmentIdsRef.current = nextPending;
    setPendingAppointmentIds(nextPending);
    setFeedback(null);

    try {
      const localToday = getLocalCalendarToday();
      await createVaccinationDraft({
        appointmentId: appointment.id,
        administrationTime: toApiIsoDate(localToday),
      });
      const refreshResult = await refreshDrafts();
      setFeedback({
        type: refreshResult.ok || refreshResult.stale ? 'success' : 'warning',
        title: 'Draft Started',
        message: refreshResult.ok || refreshResult.stale
          ? `A vaccination draft was started for Appointment #${appointment.id}. Find it under Active Work or open the Vaccination Drafts page.`
          : `Draft created for Appointment #${appointment.id}, but the active drafts list could not be refreshed. Use Retry to reload it.`,
      });
    } catch (err) {
      setFeedback({
        type: 'danger',
        title: 'Draft Not Started',
        message: err.message || `Unable to start a vaccination draft for Appointment #${appointment.id}.`,
      });
    } finally {
      const remaining = new Set(pendingAppointmentIdsRef.current);
      remaining.delete(appointment.id);
      pendingAppointmentIdsRef.current = remaining;
      setPendingAppointmentIds(remaining);
    }
  };

  if (!isVaccinator) {
    return null;
  }

  // Active drafts exclude FINALIZED drafts
  const activeDrafts = drafts ? drafts.filter((d) => ACTIVE_STATUSES.includes(d.status)) : null;
  const approvedCorrections = corrections ? corrections.filter((c) => c.status === 'APPROVED') : [];

  return (
    <section aria-label="Vaccinator Workspace" className="mb-5">
      {/* Option B: Clean Header */}
      <div className="mb-4">
        <h2 className="h3 fw-bold mb-1" id="vaccinator-workspace-title">
          Vaccinator Dashboard
        </h2>
        <div className="small text-secondary fw-medium mb-1">
          Welcome, {user?.fullName || user?.email || 'Vaccinator'} • Vaccinator
        </div>
        <p className="text-muted mb-0">Manage today&apos;s vaccination work</p>
      </div>

      {feedback && (
        <div className={`alert alert-${feedback.type} alert-dismissible fade show`} role="alert">
          <strong className="fw-bold d-block">{feedback.title}</strong>
          <span>{feedback.message}</span>
          <button
            type="button"
            className="btn-close"
            aria-label="Close"
            onClick={() => setFeedback(null)}
          ></button>
        </div>
      )}

      {/* Option B: Current Session Card */}
      <div className="card border-0 shadow-sm mb-4 bg-light">
        <div className="card-body p-4">
          {activeSession ? (
            <div className="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3">
              <div>
                <div className="d-flex align-items-center gap-2 mb-2">
                  <span className="badge bg-success-subtle text-success border border-success fw-semibold px-2 py-1">
                    Active Session
                  </span>
                  <span className="text-secondary small">
                    {activeSession.date} · {activeSession.startTime} – {activeSession.endTime}
                  </span>
                </div>
                <h3 className="h5 fw-bold mb-1 text-dark">
                  {activeSession.centerName}
                </h3>
                <div className="small text-muted">
                  Session Hours: {activeSession.startTime} – {activeSession.endTime}
                </div>
              </div>

              {availableSessions.length > 1 && (
                <div className="d-flex align-items-center gap-2">
                  <label htmlFor={sessionSelectorId} className="small fw-semibold text-secondary text-nowrap mb-0">
                    Change Session ▼
                  </label>
                  <select
                    id={sessionSelectorId}
                    aria-label="Change Session"
                    className="form-select form-select-sm"
                    value={activeSession.id}
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
          ) : (
            <div className="text-center py-2 text-muted">
              <p className="mb-0 fw-medium">No active vaccination session is currently assigned.</p>
            </div>
          )}
        </div>
      </div>

      {/* Top Summary Metrics */}
      <div className="row g-3 mb-4">
        <div className="col-12 col-sm-4">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body p-3">
              <div className="d-flex justify-content-between align-items-center">
                <span className="small text-secondary fw-semibold">Waiting in Queue</span>
                <span className="badge bg-primary-subtle text-primary border border-primary">Live</span>
              </div>
              <div className="h3 fw-bold mb-0 text-dark mt-2">
                {queue !== null ? queue.length : activeSession ? '…' : '0'}
              </div>
              <div className="small text-muted mt-1">
                {activeSession ? 'Checked-in citizens' : 'No active session'}
              </div>
            </div>
          </div>
        </div>

        <div className="col-12 col-sm-4">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body p-3">
              <div className="d-flex justify-content-between align-items-center">
                <span className="small text-secondary fw-semibold">Active Drafts</span>
                <span className="badge bg-warning-subtle text-warning border border-warning">Active Work</span>
              </div>
              <div className="h3 fw-bold mb-0 text-dark mt-2">
                {activeDrafts !== null ? activeDrafts.length : '…'}
              </div>
              <div className="small text-muted mt-1">Unfinalized drafts pending completion</div>
            </div>
          </div>
        </div>

        <div className="col-12 col-sm-4">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body p-3">
              <div className="d-flex justify-content-between align-items-center">
                <span className="small text-secondary fw-semibold">Corrections Needed</span>
                <span className={`badge ${approvedCorrections.length > 0 ? 'bg-danger text-white' : 'bg-secondary-subtle text-secondary'}`}>
                  {approvedCorrections.length > 0 ? 'Action Required' : 'Up to date'}
                </span>
              </div>
              <div className="h3 fw-bold mb-0 text-dark mt-2">
                {corrections !== null ? approvedCorrections.length : '…'}
              </div>
              <div className="small text-muted mt-1">Admin approved · Ready to apply</div>
            </div>
          </div>
        </div>
      </div>

      {/* Two-Column Task Workspace */}
      <div className="row g-4">
        {/* Left column: Checked-In Queue */}
        <div className="col-lg-7">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body p-4">
              <div className="d-flex justify-content-between align-items-center mb-3">
                <h3 className="h5 fw-bold mb-0">Checked-In Queue</h3>
                {activeSession && queue && (
                  <span className="badge bg-light text-dark border">
                    {queue.length} citizen{queue.length === 1 ? '' : 's'}
                  </span>
                )}
              </div>

              {/* Queue loading state */}
              {queueLoading && (
                <div className="text-center py-4">
                  <div className="spinner-border text-primary" role="status">
                    <span className="visually-hidden">Loading queue…</span>
                  </div>
                </div>
              )}

              {/* Queue error state */}
              {!queueLoading && queueError && (
                <div className="alert alert-danger mb-0 small" role="alert">
                  <strong className="d-block mb-1">Error Loading Queue</strong>
                  <span>{queueError}</span>
                  <div className="mt-2">
                    <button
                      type="button"
                      className="btn btn-outline-danger btn-sm"
                      onClick={() => activeSession && executeLoadQueue(activeSession.slotId)}
                    >
                      Retry
                    </button>
                  </div>
                </div>
              )}

              {/* No active session state */}
              {!queueLoading && !queueError && !activeSession && (
                <div className="alert alert-light border text-center py-4 mb-0">
                  <p className="text-muted small mb-0">Select or receive an assigned vaccination session to view the queue.</p>
                </div>
              )}

              {/* Empty queue state */}
              {!queueLoading && !queueError && activeSession && queue !== null && queue.length === 0 && (
                <div className="alert alert-light border text-center py-4 mb-0">
                  <p className="text-muted small mb-0">No citizens are currently waiting for this session.</p>
                </div>
              )}

              {/* Loaded queue list */}
              {!queueLoading && !queueError && activeSession && queue !== null && queue.length > 0 && (
                <div className="vstack gap-3">
                  {queue.map((appointment) => (
                    <div key={appointment.id} className="border rounded p-3">
                      <div className="d-flex justify-content-between align-items-start mb-2">
                        <div>
                          <div className="fw-semibold">Appointment #{appointment.id}</div>
                          <div className="small text-secondary">
                            Citizen #{appointment.citizenId} · Campaign #{appointment.campaignId}
                          </div>
                        </div>
                        <AppointmentStatusBadge status={appointment.status} />
                      </div>
                      <div className="d-flex justify-content-end gap-2">
                        {activeDrafts?.some((d) => d.appointmentId === appointment.id) ? (
                          <Link
                            to="/app/staff/vaccination-drafts"
                            className="btn btn-outline-primary btn-sm"
                            aria-label={`Open existing vaccination draft for appointment ${appointment.id}`}
                          >
                            Open Draft
                          </Link>
                        ) : (
                          <button
                            type="button"
                            className="btn btn-primary btn-sm"
                            aria-label={`Begin vaccination draft for appointment ${appointment.id}`}
                            disabled={pendingAppointmentIds.has(appointment.id)}
                            onClick={() => beginDraftFromAppointment(appointment)}
                          >
                            {pendingAppointmentIds.has(appointment.id) ? (
                              <>
                                <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                                Starting…
                              </>
                            ) : (
                              'Begin Vaccination Draft'
                            )}
                          </button>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Right column: Active Work & Drafts */}
        <div className="col-lg-5">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body p-4 d-flex flex-column">
              <div className="d-flex justify-content-between align-items-center mb-3">
                <div>
                  <h3 className="h5 fw-bold mb-0">Active Work</h3>
                  <h4 className="visually-hidden">My Active Drafts</h4>
                </div>
                <span className="badge text-bg-light border text-dark">
                  {activeDrafts ? activeDrafts.length : '…'}
                </span>
              </div>

              {/* Action Required: Approved Corrections */}
              {approvedCorrections.length > 0 && (
                <div className="alert alert-warning border mb-3 p-3">
                  <div className="d-flex justify-content-between align-items-center mb-2">
                    <span className="badge bg-danger text-white fw-bold">ACTION REQUIRED</span>
                    <span className="small text-muted">
                      {approvedCorrections.length} correction{approvedCorrections.length === 1 ? '' : 's'}
                    </span>
                  </div>
                  <div className="small text-dark mb-2">
                    Admin approved corrections are ready for you to review and apply to vaccination records.
                  </div>
                  <div className="d-flex justify-content-end">
                    <Link
                      to="/app/staff/correction-requests"
                      className="btn btn-sm btn-primary fw-semibold"
                    >
                      Open Correction
                    </Link>
                  </div>
                </div>
              )}

              {/* Drafts loading state */}
              {draftsLoading && (
                <div className="text-center py-4">
                  <div className="spinner-border spinner-border-sm text-primary" role="status">
                    <span className="visually-hidden">Loading drafts…</span>
                  </div>
                </div>
              )}

              {/* Drafts error state */}
              {!draftsLoading && draftsError && (
                <div className="alert alert-danger mb-3 small" role="alert">
                  <strong className="d-block mb-1">Could Not Load Drafts</strong>
                  <span>{draftsError}</span>
                  <div className="mt-2">
                    <button
                      type="button"
                      className="btn btn-outline-danger btn-sm"
                      onClick={refreshDrafts}
                    >
                      Retry
                    </button>
                  </div>
                </div>
              )}

              {/* Empty drafts state */}
              {!draftsLoading && !draftsError && activeDrafts && activeDrafts.length === 0 && (
                <div className="alert alert-light border text-center py-3 mb-3">
                  <p className="text-muted small mb-0">No active vaccination drafts.</p>
                </div>
              )}

              {/* Active drafts summary list with Open Draft link */}
              {!draftsLoading && !draftsError && activeDrafts && activeDrafts.length > 0 && (
                <div className="vstack gap-3 mb-3 flex-grow-1">
                  {activeDrafts.map((draft) => (
                    <div key={draft.id} className="border rounded p-3">
                      <div className="d-flex justify-content-between align-items-start mb-2">
                        <div className="fw-semibold">Draft #{draft.id}</div>
                        <span
                          className={`badge ${
                            draft.status === 'IN_PROGRESS' ? 'text-bg-info' : 'text-bg-warning'
                          }`}
                        >
                          {DRAFT_STATUS_LABELS[draft.status] || draft.status}
                        </span>
                      </div>
                      <div className="small text-secondary mb-1">
                        Appointment #{draft.appointmentId}
                        {draft.doseNumber ? ` · Dose ${draft.doseNumber}` : ''}
                        {draft.vaccineBatchId ? ` · Batch #${draft.vaccineBatchId}` : ''}
                      </div>
                      <div className="d-flex justify-content-between align-items-center mt-2 pt-2 border-top">
                        <span className="small text-secondary">
                          {formatDateTime(draft.createdAt)}
                        </span>
                        <Link
                          to="/app/staff/vaccination-drafts"
                          className="btn btn-sm btn-outline-primary py-0 px-2 small"
                        >
                          Open Draft
                        </Link>
                      </div>
                    </div>
                  ))}
                </div>
              )}

              {/* Dedicated navigation link to full Vaccination Drafts page */}
              <div className="mt-auto pt-2">
                <Link
                  to="/app/staff/vaccination-drafts"
                  className="btn btn-outline-primary btn-sm w-100 fw-semibold"
                >
                  Open Vaccination Drafts
                </Link>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
