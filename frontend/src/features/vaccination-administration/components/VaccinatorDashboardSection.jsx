import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../../auth/context/AuthContext';
import { getVaccinatorQueue } from '../../appointment-management/api/checkInApi';
import {
  createVaccinationDraft,
  listMyVaccinationDrafts,
} from '../api/vaccinationDraftApi';
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
 * Vaccinator operational workspace section rendered on the staff dashboard.
 *
 * Provides:
 *  1. Explicit Slot ID entry (GET /api/v1/appointments/queue?slotId={slotId})
 *     to view checked-in appointments.
 *  2. "Begin Vaccination Draft" from a checked-in appointment with automatic
 *     default local calendar administration date.
 *  3. "My Active Drafts" summary (strictly IN_PROGRESS and READY_FOR_FINALIZATION;
 *     FINALIZED excluded).
 *  4. Direct navigation link to the dedicated Vaccination Drafts page for all
 *     lifecycle mutations (Edit, Discard, Confirm Administration, Finalize).
 */
export default function VaccinatorDashboardSection() {
  const { user } = useAuth();
  const isVaccinator = Boolean(user?.roles?.includes(VACCINATOR_ROLE));

  // Slot ID and Queue state
  const [slotInput, setSlotInput] = useState('');
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

  useEffect(() => {
    if (isVaccinator) {
      refreshDrafts();
    }
  }, [isVaccinator, refreshDrafts]);

  // Slot input change immediately invalidates old queue to prevent stale context
  const handleSlotInputChange = (event) => {
    const rawVal = event.target.value;
    const cleanVal = rawVal.replace(/[^0-9]/g, '');
    setSlotInput(cleanVal);
    queueRequestRef.current += 1;
    setQueueLoading(false);
    setQueue(null);
    setLoadedSlotId(null);
    setQueueError(null);
  };

  // Submit slot to load queue with monotonic sequence ID
  const handleSlotSubmit = async (event) => {
    event.preventDefault();
    const trimmed = slotInput.trim();
    if (!trimmed) return;

    const parsed = parseInt(trimmed, 10);
    if (Number.isNaN(parsed) || parsed <= 0) return;

    const currentRequestId = ++queueRequestRef.current;
    setQueueLoading(true);
    setQueueError(null);
    setQueue(null);
    setLoadedSlotId(null);
    setFeedback(null);

    try {
      const data = await getVaccinatorQueue(parsed);
      // Stale-response guard: only apply if this request is still latest
      if (currentRequestId === queueRequestRef.current) {
        setQueue(Array.isArray(data) ? data : []);
        setLoadedSlotId(String(parsed));
      }
    } catch (err) {
      if (currentRequestId === queueRequestRef.current) {
        setQueue(null);
        setLoadedSlotId(null);
        setQueueError(err.message || `Failed to fetch the queue for Slot #${parsed}.`);
      }
    } finally {
      if (currentRequestId === queueRequestRef.current) {
        setQueueLoading(false);
      }
    }
  };

  const beginDraftFromAppointment = async (appointment) => {
    // Safety check: row must belong to currently loaded queue
    if (
      !loadedSlotId
      || String(appointment.slotId) !== loadedSlotId
      || !queue
      || !queue.some((a) => a.id === appointment.id && String(a.slotId) !== loadedSlotId && a.id === appointment.id ? false : a.id === appointment.id && String(a.slotId) === loadedSlotId)
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
          ? `A vaccination draft was started for Appointment #${appointment.id}. Find it under My Active Drafts or open the Vaccination Drafts page.`
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

  return (
    <section aria-labelledby="vaccinator-workspace-title" className="mb-5">
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h2 className="h3 mb-0" id="vaccinator-workspace-title">Vaccinator Workspace</h2>
        {loadedSlotId ? (
          <span className="badge text-bg-primary">Active Slot #{loadedSlotId}</span>
        ) : (
          <span className="badge text-bg-secondary">No Slot Selected</span>
        )}
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

      <div className="row g-4">
        {/* Left column: Checked-In Queue */}
        <div className="col-lg-7">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body p-4">
              <h3 className="h5 fw-bold mb-3">Checked-In Queue</h3>

              {/* Explicit Slot ID context entry */}
              <form onSubmit={handleSlotSubmit} className="row g-2 align-items-end mb-3">
                <div className="col-12 col-sm-7 col-md-8">
                  <label htmlFor="vaccinator-slot-input" className="form-label small fw-semibold mb-1">
                    Time Slot ID <span className="text-danger">*</span>
                  </label>
                  <input
                    type="number"
                    id="vaccinator-slot-input"
                    className="form-control form-control-sm"
                    min="1"
                    step="1"
                    required
                    placeholder="Enter active Time Slot ID"
                    value={slotInput}
                    onChange={handleSlotInputChange}
                    onKeyDown={(event) => {
                      if (['-', 'e', '+', '.'].includes(event.key)) {
                        event.preventDefault();
                      }
                    }}
                  />
                </div>
                <div className="col-12 col-sm-5 col-md-4">
                  <button
                    type="submit"
                    className="btn btn-primary btn-sm w-100 fw-semibold"
                    disabled={queueLoading || !slotInput.trim()}
                  >
                    {queueLoading ? 'Loading…' : 'Load Queue'}
                  </button>
                </div>
              </form>

              {/* Queue loading state */}
              {queueLoading && (
                <div className="text-center py-4">
                  <div className="spinner-border text-primary" role="status">
                    <span className="visually-hidden">Loading queue…</span>
                  </div>
                </div>
              )}

              {/* Queue error state (distinct from empty state) */}
              {!queueLoading && queueError && (
                <div className="alert alert-danger mb-0 small" role="alert">
                  <strong className="d-block mb-1">Error Loading Queue</strong>
                  <span>{queueError}</span>
                </div>
              )}

              {/* Idle state before slot is loaded */}
              {!queueLoading && !queueError && loadedSlotId === null && (
                <p className="text-muted small mb-0">
                  Enter an active Time Slot ID above and select Load Queue to view checked-in appointments.
                </p>
              )}

              {/* Empty queue state */}
              {!queueLoading && !queueError && loadedSlotId !== null && queue !== null && queue.length === 0 && (
                <div className="alert alert-light border mb-0 small">
                  No checked-in appointments found for Slot #{loadedSlotId}.
                </div>
              )}

              {/* Loaded queue list */}
              {!queueLoading && !queueError && loadedSlotId !== null && queue !== null && queue.length > 0 && (
                <div className="vstack gap-3">
                  {queue.map((appointment) => (
                    <div key={appointment.id} className="border rounded p-3">
                      <div className="d-flex justify-content-between align-items-start mb-2">
                        <div>
                          <div className="fw-semibold">Appointment #{appointment.id}</div>
                          <div className="small text-secondary">
                            Citizen #{appointment.citizenId} · Campaign #{appointment.campaignId} · Slot #{appointment.slotId}
                          </div>
                        </div>
                        <AppointmentStatusBadge status={appointment.status} />
                      </div>
                      <div className="d-flex justify-content-end gap-2">
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
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Right column: My Active Drafts (Summary View Only) */}
        <div className="col-lg-5">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body p-4 d-flex flex-column">
              <div className="d-flex justify-content-between align-items-center mb-3">
                <h3 className="h5 fw-bold mb-0">My Active Drafts</h3>
                <span className="badge text-bg-light border text-dark">
                  {activeDrafts ? activeDrafts.length : '…'}
                </span>
              </div>

              {/* Drafts loading state */}
              {draftsLoading && (
                <div className="text-center py-4">
                  <div className="spinner-border spinner-border-sm text-primary" role="status">
                    <span className="visually-hidden">Loading drafts…</span>
                  </div>
                </div>
              )}

              {/* Drafts error state (distinct from empty state) */}
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

              {/* Active drafts summary list */}
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
                      <div className="small text-secondary">
                        Created {formatDateTime(draft.createdAt)}
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
