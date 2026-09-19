import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useAuth } from '../../../auth/context/AuthContext';
import { getVaccinatorQueue } from '../../appointment-management/api/checkInApi';
import {
  createVaccinationDraft,
  discardVaccinationDraft,
  getVaccinationDraft,
  listMyVaccinationDrafts,
  updateVaccinationDraft,
} from '../api/vaccinationDraftApi';
import AppointmentStatusBadge from '../../appointment-management/components/AppointmentStatusBadge';

const VACCINATOR_ROLE = 'ROLE_VACCINATOR';

const DRAFT_STATUS_LABELS = {
  IN_PROGRESS: 'In Progress',
  READY_FOR_FINALIZATION: 'Ready for Finalization',
};

function formatDateTime(isoString) {
  if (!isoString) return 'N/A';
  return new Date(isoString).toLocaleString(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  });
}

function formatDateOnly(isoDate) {
  if (!isoDate) return '—';
  return new Date(`${isoDate}T00:00:00`).toLocaleDateString(undefined, {
    dateStyle: 'medium',
  });
}

function toLocalInputValue(isoString) {
  if (!isoString) return '';
  const date = new Date(isoString);
  if (Number.isNaN(date.getTime())) return '';
  const pad = (value) => String(value).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

/**
 * Vaccinator operational workspace section rendered on the staff dashboard.
 *
 * Provides backend-supported capabilities only:
 *  1. Explicit Slot ID entry (GET /api/v1/appointments/queue?slotId={slotId})
 *     — strictly authorized for ROLE_VACCINATOR. No session/time-slot lookups.
 *  2. Queue with monotonic request tracking and instant stale invalidation.
 *  3. Begin a vaccination draft for a checked-in appointment (POST /api/v1/vaccination/drafts).
 *  4. "My Active Drafts" list with distinct loading, error, empty, and data states.
 *  5. View draft with fresh server fetch (GET /api/v1/vaccination/drafts/{id}).
 *  6. Edit draft with fresh baseline, true PATCH payload (changed-only fields), and clearing validation.
 *  7. Discard draft with mandatory reason, keeping modal open with reason on failure.
 *  8. Accessible single-active-dialog architecture with Escape-key and focus restoration.
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
  const [processingAppointmentId, setProcessingAppointmentId] = useState(null);
  const [feedback, setFeedback] = useState(null);

  // Monotonic queue request tracker to prevent out-of-order race conditions
  const queueRequestRef = useRef(0);

  // Drafts state
  const [drafts, setDrafts] = useState(null);
  const [draftsLoading, setDraftsLoading] = useState(true);
  const [draftsError, setDraftsError] = useState(null);

  // Single active dialog: null | { type: 'view'|'edit'|'discard', draftId, draft }
  const [activeDialog, setActiveDialog] = useState(null);
  const triggerButtonRef = useRef(null);

  // View dialog state
  const [viewData, setViewData] = useState(null);
  const [viewLoading, setViewLoading] = useState(false);
  const [viewError, setViewError] = useState(null);

  // Edit dialog state
  const [editBaseline, setEditBaseline] = useState(null);
  const [editForm, setEditForm] = useState(null);
  const [editLoading, setEditLoading] = useState(false);
  const [editError, setEditError] = useState(null);
  const [editSubmitting, setEditSubmitting] = useState(false);

  // Discard dialog state
  const [discardReason, setDiscardReason] = useState('');
  const [discardError, setDiscardError] = useState(null);
  const [discardSubmitting, setDiscardSubmitting] = useState(false);

  const refreshDrafts = useCallback(async () => {
    setDraftsLoading(true);
    setDraftsError(null);
    try {
      const data = await listMyVaccinationDrafts();
      setDrafts(Array.isArray(data) ? data : []);
    } catch (err) {
      setDrafts(null);
      setDraftsError(err.message || 'Unable to load your vaccination drafts.');
    } finally {
      setDraftsLoading(false);
    }
  }, []);

  useEffect(() => {
    if (isVaccinator) {
      refreshDrafts();
    }
  }, [isVaccinator, refreshDrafts]);

  // Handle closing any active dialog with focus restoration
  const closeActiveDialog = useCallback(() => {
    setActiveDialog(null);
    setViewData(null);
    setViewError(null);
    setEditBaseline(null);
    setEditForm(null);
    setEditError(null);
    setDiscardError(null);
    if (triggerButtonRef.current && typeof triggerButtonRef.current.focus === 'function') {
      triggerButtonRef.current.focus();
    }
  }, []);

  // Escape key handler to close dialog
  useEffect(() => {
    if (!activeDialog) return;
    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        closeActiveDialog();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [activeDialog, closeActiveDialog]);

  // Slot input change immediately invalidates old queue to prevent stale context
  const handleSlotInputChange = (event) => {
    const rawVal = event.target.value;
    const cleanVal = rawVal.replace(/[^0-9]/g, '');
    setSlotInput(cleanVal);

    // If input changed away from loadedSlotId, clear queue and context immediately
    if (loadedSlotId !== null && cleanVal !== loadedSlotId) {
      setQueue(null);
      setLoadedSlotId(null);
      setQueueError(null);
    }
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
    if (!loadedSlotId || !queue || !queue.some((a) => a.id === appointment.id)) {
      setFeedback({
        type: 'danger',
        title: 'Context Mismatch',
        message: 'Cannot begin draft: the selected appointment does not belong to the currently loaded queue.',
      });
      return;
    }

    setProcessingAppointmentId(appointment.id);
    setFeedback(null);

    try {
      await createVaccinationDraft({ appointmentId: appointment.id });
      await refreshDrafts();
      setFeedback({
        type: 'success',
        title: 'Draft Started',
        message: `A vaccination draft was started for Appointment #${appointment.id}. Find it under My Active Drafts.`,
      });
    } catch (err) {
      setFeedback({
        type: 'danger',
        title: 'Draft Not Started',
        message: err.message || `Unable to start a vaccination draft for Appointment #${appointment.id}.`,
      });
    } finally {
      setProcessingAppointmentId(null);
    }
  };

  // Open View dialog and fetch fresh draft
  const openViewDialog = async (draftId, event) => {
    triggerButtonRef.current = event?.currentTarget || null;
    setActiveDialog({ type: 'view', draftId });
    setViewLoading(true);
    setViewError(null);
    setViewData(null);

    try {
      const freshDraft = await getVaccinationDraft(draftId);
      setViewData(freshDraft);
    } catch (err) {
      setViewError(err.message || `Unable to load details for Draft #${draftId}.`);
    } finally {
      setViewLoading(false);
    }
  };

  // Open Edit dialog and fetch fresh baseline
  const openEditDialog = async (draftId, event) => {
    triggerButtonRef.current = event?.currentTarget || null;
    setActiveDialog({ type: 'edit', draftId });
    setEditLoading(true);
    setEditError(null);
    setEditBaseline(null);
    setEditForm(null);

    try {
      const freshDraft = await getVaccinationDraft(draftId);
      setEditBaseline(freshDraft);
      setEditForm({
        vaccineBatchId: freshDraft.vaccineBatchId != null ? String(freshDraft.vaccineBatchId) : '',
        doseNumber: freshDraft.doseNumber != null ? String(freshDraft.doseNumber) : '',
        administrationTime: toLocalInputValue(freshDraft.administrationTime),
        nextDoseDate: freshDraft.nextDoseDate || '',
        authorizedNotes: freshDraft.authorizedNotes || '',
      });
    } catch (err) {
      setEditError(err.message || `Unable to load details for Draft #${draftId}.`);
    } finally {
      setEditLoading(false);
    }
  };

  const handleEditFormChange = (field) => (event) => {
    const { value } = event.target;
    setEditForm((current) => ({ ...current, [field]: value }));
  };

  // Submit Edit with true PATCH semantics and clearing validation
  const handleEditSubmit = async (event) => {
    event.preventDefault();
    if (!editBaseline || !editForm) return;

    setEditError(null);

    // 1. Validation: clearing existing non-empty values is not supported by backend PATCH
    const isClearingExisting = (
      (editBaseline.vaccineBatchId != null && editForm.vaccineBatchId.trim() === '') ||
      (editBaseline.doseNumber != null && editForm.doseNumber.trim() === '') ||
      (editBaseline.administrationTime != null && editForm.administrationTime.trim() === '') ||
      (Boolean(editBaseline.nextDoseDate) && editForm.nextDoseDate.trim() === '') ||
      (Boolean(editBaseline.authorizedNotes) && editForm.authorizedNotes.trim() === '')
    );

    if (isClearingExisting) {
      setEditError('Clearing this existing value is not supported by the current API.');
      return;
    }

    // 2. Construct PATCH body containing ONLY changed values
    const patchBody = {};

    if (editForm.vaccineBatchId.trim() !== '') {
      const num = Number(editForm.vaccineBatchId.trim());
      if (num !== editBaseline.vaccineBatchId) {
        patchBody.vaccineBatchId = num;
      }
    }

    if (editForm.doseNumber.trim() !== '') {
      const num = Number(editForm.doseNumber.trim());
      if (num !== editBaseline.doseNumber) {
        patchBody.doseNumber = num;
      }
    }

    if (editForm.administrationTime.trim() !== '') {
      const formIso = new Date(editForm.administrationTime).toISOString();
      const baselineIso = editBaseline.administrationTime
        ? new Date(editBaseline.administrationTime).toISOString()
        : null;
      if (formIso !== baselineIso) {
        patchBody.administrationTime = formIso;
      }
    }

    if (editForm.nextDoseDate.trim() !== '') {
      if (editForm.nextDoseDate.trim() !== (editBaseline.nextDoseDate || '')) {
        patchBody.nextDoseDate = editForm.nextDoseDate.trim();
      }
    }

    if (editForm.authorizedNotes.trim() !== '') {
      if (editForm.authorizedNotes.trim() !== (editBaseline.authorizedNotes || '')) {
        patchBody.authorizedNotes = editForm.authorizedNotes.trim();
      }
    }

    // If no field changed, do not call API
    if (Object.keys(patchBody).length === 0) {
      setEditError('No changes to save.');
      return;
    }

    setEditSubmitting(true);
    try {
      await updateVaccinationDraft(editBaseline.id, patchBody);
      closeActiveDialog();
      await refreshDrafts();
      setFeedback({
        type: 'success',
        title: 'Draft Updated',
        message: `Draft #${editBaseline.id} was updated successfully.`,
      });
    } catch (err) {
      setEditError(err.message || `Unable to update Draft #${editBaseline.id}.`);
    } finally {
      setEditSubmitting(false);
    }
  };

  // Open Discard dialog
  const openDiscardDialog = (draft, event) => {
    triggerButtonRef.current = event?.currentTarget || null;
    setActiveDialog({ type: 'discard', draftId: draft.id, draft });
    setDiscardReason('');
    setDiscardError(null);
    setDiscardSubmitting(false);
  };

  // Confirm Discard - failure keeps modal open with entered reason intact
  const confirmDiscardDraft = async (event) => {
    event.preventDefault();
    if (!activeDialog || activeDialog.type !== 'discard') return;

    const trimmedReason = discardReason.trim();
    if (!trimmedReason) {
      setDiscardError('A deletion reason is required to discard a vaccination draft.');
      return;
    }

    if (trimmedReason.length > 1000) {
      setDiscardError('Audit reason must not exceed 1000 characters.');
      return;
    }

    setDiscardSubmitting(true);
    setDiscardError(null);
    const targetId = activeDialog.draftId;

    try {
      await discardVaccinationDraft(targetId, trimmedReason);
      closeActiveDialog();
      setDiscardReason('');
      await refreshDrafts();
      setFeedback({
        type: 'success',
        title: 'Draft Discarded',
        message: `Draft #${targetId} was discarded. The reason was recorded for audit.`,
      });
    } catch (err) {
      // Keep dialog open, keep discardReason intact for retry
      setDiscardError(err.message || `Unable to discard Draft #${targetId}.`);
    } finally {
      setDiscardSubmitting(false);
    }
  };

  if (!isVaccinator) {
    return null;
  }

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
                          disabled={processingAppointmentId === appointment.id}
                          onClick={() => beginDraftFromAppointment(appointment)}
                        >
                          {processingAppointmentId === appointment.id ? (
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

        {/* Right column: My Active Drafts */}
        <div className="col-lg-5">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body p-4">
              <div className="d-flex justify-content-between align-items-center mb-3">
                <h3 className="h5 fw-bold mb-0">My Active Drafts</h3>
                <span className="badge text-bg-light border text-dark">
                  {drafts ? drafts.length : '…'}
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
                <div className="alert alert-danger mb-0 small" role="alert">
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
              {!draftsLoading && !draftsError && drafts && drafts.length === 0 && (
                <p className="text-muted small mb-0">
                  You have no vaccination drafts yet. Start one from the checked-in queue.
                </p>
              )}

              {/* Drafts list */}
              {!draftsLoading && !draftsError && drafts && drafts.length > 0 && (
                <div className="vstack gap-3">
                  {drafts.map((draft) => (
                    <div key={draft.id} className="border rounded p-3">
                      <div className="d-flex justify-content-between align-items-start mb-2">
                        <div className="fw-semibold">Draft #{draft.id}</div>
                        <span className={`badge ${draft.status === 'IN_PROGRESS' ? 'text-bg-info' : 'text-bg-secondary'}`}>
                          {DRAFT_STATUS_LABELS[draft.status] || draft.status}
                        </span>
                      </div>
                      <div className="small text-secondary mb-2">
                        Appointment #{draft.appointmentId}
                        {draft.doseNumber ? ` · Dose ${draft.doseNumber}` : ''}
                        {draft.vaccineBatchId ? ` · Batch #${draft.vaccineBatchId}` : ''}
                      </div>
                      <div className="small text-secondary mb-3">
                        Created {formatDateTime(draft.createdAt)}
                      </div>
                      <div className="d-flex justify-content-end gap-2">
                        <button
                          type="button"
                          className="btn btn-outline-secondary btn-sm"
                          aria-label={`View vaccination draft ${draft.id}`}
                          onClick={(e) => openViewDialog(draft.id, e)}
                        >
                          View
                        </button>
                        <button
                          type="button"
                          className="btn btn-outline-primary btn-sm"
                          aria-label={`Edit vaccination draft ${draft.id}`}
                          onClick={(e) => openEditDialog(draft.id, e)}
                        >
                          Edit
                        </button>
                        <button
                          type="button"
                          className="btn btn-outline-danger btn-sm"
                          aria-label={`Discard vaccination draft ${draft.id}`}
                          onClick={(e) => openDiscardDialog(draft, e)}
                        >
                          Discard
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Draft View Modal */}
      {activeDialog?.type === 'view' && (
        <div
          className="modal fade show d-block bg-dark bg-opacity-50"
          tabIndex="-1"
          role="dialog"
          aria-modal="true"
          aria-labelledby="view-draft-modal-title"
        >
          <div className="modal-dialog modal-dialog-centered" role="document">
            <div className="modal-content border-0 shadow">
              <div className="modal-header">
                <h5 className="modal-title" id="view-draft-modal-title">
                  Draft #{activeDialog.draftId}
                </h5>
                <button
                  type="button"
                  className="btn-close"
                  aria-label="Close dialog"
                  onClick={closeActiveDialog}
                ></button>
              </div>
              <div className="modal-body p-4">
                {viewLoading && (
                  <div className="text-center py-4">
                    <div className="spinner-border spinner-border-sm text-primary" role="status">
                      <span className="visually-hidden">Loading draft details…</span>
                    </div>
                  </div>
                )}

                {!viewLoading && viewError && (
                  <div className="alert alert-danger mb-0 small" role="alert">
                    {viewError}
                  </div>
                )}

                {!viewLoading && !viewError && viewData && (
                  <dl className="row mb-0">
                    <dt className="col-5">Status</dt>
                    <dd className="col-7">{DRAFT_STATUS_LABELS[viewData.status] || viewData.status}</dd>
                    <dt className="col-5">Appointment</dt>
                    <dd className="col-7">#{viewData.appointmentId}</dd>
                    <dt className="col-5">Vaccine Batch</dt>
                    <dd className="col-7">{viewData.vaccineBatchId ? `#${viewData.vaccineBatchId}` : '—'}</dd>
                    <dt className="col-5">Dose</dt>
                    <dd className="col-7">{viewData.doseNumber ?? '—'}</dd>
                    <dt className="col-5">Administered</dt>
                    <dd className="col-7">{formatDateTime(viewData.administrationTime)}</dd>
                    <dt className="col-5">Next Dose</dt>
                    <dd className="col-7">{formatDateOnly(viewData.nextDoseDate)}</dd>
                    <dt className="col-5">Physically Confirmed</dt>
                    <dd className="col-7">{viewData.physicalAdministrationConfirmed ? 'Yes' : 'No'}</dd>
                    <dt className="col-5">Created</dt>
                    <dd className="col-7">{formatDateTime(viewData.createdAt)}</dd>
                    <dt className="col-5">Updated</dt>
                    <dd className="col-7">{formatDateTime(viewData.updatedAt)}</dd>
                    <dt className="col-5">Notes</dt>
                    <dd className="col-7">{viewData.authorizedNotes || '—'}</dd>
                  </dl>
                )}
              </div>
              <div className="modal-footer">
                <button type="button" className="btn btn-secondary" onClick={closeActiveDialog}>
                  Close
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Draft Edit Modal */}
      {activeDialog?.type === 'edit' && (
        <div
          className="modal fade show d-block bg-dark bg-opacity-50"
          tabIndex="-1"
          role="dialog"
          aria-modal="true"
          aria-labelledby="edit-draft-modal-title"
        >
          <div className="modal-dialog modal-dialog-centered" role="document">
            <div className="modal-content border-0 shadow">
              <div className="modal-header">
                <h5 className="modal-title" id="edit-draft-modal-title">
                  Edit Draft #{activeDialog.draftId}
                </h5>
                <button
                  type="button"
                  className="btn-close"
                  aria-label="Close dialog"
                  onClick={closeActiveDialog}
                ></button>
              </div>

              {editLoading && (
                <div className="modal-body p-4 text-center">
                  <div className="spinner-border spinner-border-sm text-primary" role="status">
                    <span className="visually-hidden">Loading latest draft…</span>
                  </div>
                </div>
              )}

              {!editLoading && editForm && (
                <form onSubmit={handleEditSubmit}>
                  <div className="modal-body p-4">
                    {editError && (
                      <div className="alert alert-danger mb-3 small" role="alert">
                        {editError}
                      </div>
                    )}

                    <div className="row g-3">
                      <div className="col-6">
                        <label htmlFor="draft-edit-batch" className="form-label small fw-semibold mb-1">
                          Vaccine Batch ID
                        </label>
                        <input
                          type="number"
                          id="draft-edit-batch"
                          className="form-control form-control-sm"
                          min="1"
                          step="1"
                          value={editForm.vaccineBatchId}
                          onChange={handleEditFormChange('vaccineBatchId')}
                          onKeyDown={(event) => {
                            if (['-', 'e', '+', '.'].includes(event.key)) {
                              event.preventDefault();
                            }
                          }}
                        />
                      </div>
                      <div className="col-6">
                        <label htmlFor="draft-edit-dose" className="form-label small fw-semibold mb-1">
                          Dose Number
                        </label>
                        <input
                          type="number"
                          id="draft-edit-dose"
                          className="form-control form-control-sm"
                          min="1"
                          step="1"
                          value={editForm.doseNumber}
                          onChange={handleEditFormChange('doseNumber')}
                          onKeyDown={(event) => {
                            if (['-', 'e', '+', '.'].includes(event.key)) {
                              event.preventDefault();
                            }
                          }}
                        />
                      </div>
                      <div className="col-12 col-md-6">
                        <label htmlFor="draft-edit-time" className="form-label small fw-semibold mb-1">
                          Administration Time
                        </label>
                        <input
                          type="datetime-local"
                          id="draft-edit-time"
                          className="form-control form-control-sm"
                          value={editForm.administrationTime}
                          onChange={handleEditFormChange('administrationTime')}
                        />
                      </div>
                      <div className="col-12 col-md-6">
                        <label htmlFor="draft-edit-next-dose" className="form-label small fw-semibold mb-1">
                          Next Dose Date
                        </label>
                        <input
                          type="date"
                          id="draft-edit-next-dose"
                          className="form-control form-control-sm"
                          value={editForm.nextDoseDate}
                          onChange={handleEditFormChange('nextDoseDate')}
                        />
                      </div>
                      <div className="col-12">
                        <label htmlFor="draft-edit-notes" className="form-label small fw-semibold mb-1">
                          Authorized Notes
                        </label>
                        <textarea
                          id="draft-edit-notes"
                          className="form-control form-control-sm"
                          rows="3"
                          maxLength={2000}
                          value={editForm.authorizedNotes}
                          onChange={handleEditFormChange('authorizedNotes')}
                        ></textarea>
                      </div>
                    </div>
                  </div>
                  <div className="modal-footer">
                    <button type="button" className="btn btn-secondary" onClick={closeActiveDialog}>
                      Cancel
                    </button>
                    <button
                      type="submit"
                      className="btn btn-primary fw-bold"
                      disabled={editSubmitting}
                    >
                      {editSubmitting ? (
                        <>
                          <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                          Saving…
                        </>
                      ) : (
                        'Save Changes'
                      )}
                    </button>
                  </div>
                </form>
              )}

              {!editLoading && !editForm && editError && (
                <div className="modal-body p-4">
                  <div className="alert alert-danger mb-0 small" role="alert">
                    {editError}
                  </div>
                  <div className="modal-footer px-0 pb-0 mt-3">
                    <button type="button" className="btn btn-secondary" onClick={closeActiveDialog}>
                      Close
                    </button>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Draft Discard Modal with mandatory audited reason */}
      {activeDialog?.type === 'discard' && (
        <div
          className="modal fade show d-block bg-dark bg-opacity-50"
          tabIndex="-1"
          role="dialog"
          aria-modal="true"
          aria-labelledby="discard-draft-modal-title"
        >
          <div className="modal-dialog modal-dialog-centered" role="document">
            <div className="modal-content border-0 shadow">
              <div className="modal-header bg-danger text-white">
                <h5 className="modal-title" id="discard-draft-modal-title">
                  Discard Draft #{activeDialog.draftId}
                </h5>
                <button
                  type="button"
                  className="btn-close btn-close-white"
                  aria-label="Close dialog"
                  onClick={closeActiveDialog}
                ></button>
              </div>
              <form onSubmit={confirmDiscardDraft}>
                <div className="modal-body p-4">
                  {discardError && (
                    <div className="alert alert-danger mb-3 small" role="alert">
                      {discardError}
                    </div>
                  )}

                  <p className="small mb-3">
                    Discarding Draft #{activeDialog.draftId}
                    {activeDialog.draft?.appointmentId ? ` (Appointment #${activeDialog.draft.appointmentId})` : ''} is
                    permanent. An audit reason is mandatory.
                  </p>
                  <label htmlFor="draft-discard-reason" className="form-label fw-semibold">
                    Audit Reason <span className="text-danger">*</span>
                  </label>
                  <textarea
                    id="draft-discard-reason"
                    className="form-control"
                    rows="3"
                    maxLength={1000}
                    required
                    value={discardReason}
                    onChange={(event) => setDiscardReason(event.target.value)}
                    placeholder="e.g. Entered for the wrong appointment"
                  ></textarea>
                </div>
                <div className="modal-footer">
                  <button type="button" className="btn btn-secondary" onClick={closeActiveDialog}>
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="btn btn-danger fw-bold"
                    disabled={discardSubmitting}
                  >
                    {discardSubmitting ? (
                      <>
                        <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                        Discarding…
                      </>
                    ) : (
                      'Discard Draft'
                    )}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}
