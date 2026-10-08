import React, { useCallback, useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { vaccinationDraftApi } from '../api/vaccinationDraftApi';
import { getAvailableBatches } from '../api/certificateApi';
import { formatLocalDateOnly, getLocalCalendarToday, toApiIsoDate } from '../utils/dateUtils';

function TruncatedNotes({ text, maxLength = 60 }) {
  const [expanded, setExpanded] = useState(false);
  if (!text) return '—';
  if (text.length <= maxLength) return <span>{text}</span>;
  return (
    <span>
      {expanded ? text : `${text.slice(0, maxLength)}…`}{' '}
      <button
        type="button"
        className="btn btn-link btn-sm p-0 align-baseline text-decoration-none"
        onClick={() => setExpanded(!expanded)}
      >
        {expanded ? 'Show less' : 'Show more'}
      </button>
    </span>
  );
}

const emptyForm = {
  appointmentId: '',
  vaccineBatchId: '',
  doseNumber: '',
  administrationTime: getLocalCalendarToday(),
  nextDoseDate: '',
  authorizedNotes: '',
};
const editableStatuses = ['IN_PROGRESS'];
const DRAFT_STATUS_LABELS = {
  IN_PROGRESS: 'In Progress',
  READY_FOR_FINALIZATION: 'Ready for Finalization',
  FINALIZED: 'Finalized',
};

function getDraftStatusBadge(status) {
  const label = DRAFT_STATUS_LABELS[status] || status;
  const badgeClass = status === 'IN_PROGRESS'
    ? 'bg-info-subtle text-info-emphasis border border-info-subtle'
    : 'bg-warning-subtle text-warning-emphasis border border-warning-subtle';
  return (
    <span className={`badge ${badgeClass} fw-semibold`}>
      {label}
    </span>
  );
}

function draftForm(draft) {
  return {
    appointmentId: String(draft.appointmentId),
    vaccineBatchId: draft.vaccineBatchId == null ? '' : String(draft.vaccineBatchId),
    doseNumber: draft.doseNumber == null ? '' : String(draft.doseNumber),
    administrationTime: formatLocalDateOnly(draft.administrationTime) || getLocalCalendarToday(),
    nextDoseDate: draft.nextDoseDate || '',
    authorizedNotes: draft.authorizedNotes || '',
  };
}

export default function VaccinationDraftsPage() {
  const location = useLocation();
  const preselectedAppointment = location.state?.preselectedAppointment || null;

  const [drafts, setDrafts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [form, setForm] = useState(null);
  const [activeAppointmentContext, setActiveAppointmentContext] = useState(null);
  const [initialForm, setInitialForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [viewingDraft, setViewingDraft] = useState(null);
  const [error, setError] = useState(null);
  const [message, setMessage] = useState('');
  const [availableBatches, setAvailableBatches] = useState([]);
  const [loadingBatches, setLoadingBatches] = useState(false);

  useEffect(() => {
    let active = true;
    const apptId = form?.appointmentId;
    if (apptId && !isNaN(Number(apptId)) && Number(apptId) > 0) {
      setLoadingBatches(true);
      getAvailableBatches(apptId)
        .then((data) => {
          if (active) {
            setAvailableBatches(Array.isArray(data) ? data : []);
          }
        })
        .catch(() => {
          if (active) {
            setAvailableBatches([]);
          }
        })
        .finally(() => {
          if (active) {
            setLoadingBatches(false);
          }
        });
    } else {
      setAvailableBatches([]);
      setLoadingBatches(false);
    }
    return () => {
      active = false;
    };
  }, [form?.appointmentId]);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setDrafts(await vaccinationDraftApi.list());
    } catch (exception) {
      setError(exception);
    } finally {
      setLoading(false);
    }
  }, []);
  useEffect(() => { load(); }, [load]);

  // If navigated from Check-In Queue with a preselected appointment, auto-initialize draft form
  useEffect(() => {
    if (preselectedAppointment && preselectedAppointment.id) {
      setActiveAppointmentContext(preselectedAppointment);
      const values = {
        ...emptyForm,
        appointmentId: String(preselectedAppointment.id),
        administrationTime: getLocalCalendarToday(),
      };
      setForm(values);
      setInitialForm(values);
      setEditingId(null);
    }
  }, [preselectedAppointment]);

  const resetForm = () => {
    setForm(null);
    setEditingId(null);
    setActiveAppointmentContext(null);
  };

  const view = async (id) => {
    setBusy(true);
    setError(null);
    setMessage('');
    try {
      setViewingDraft(await vaccinationDraftApi.get(id));
    } catch (exception) { setError(exception); }
    finally { setBusy(false); }
  };

  const create = () => {
    const values = {
      ...emptyForm,
      administrationTime: getLocalCalendarToday(),
    };

    setForm(values);
    setInitialForm(values);
    setEditingId(null);
    setActiveAppointmentContext(null);
    setError(null);
    setMessage('');
  };

  const edit = async (id) => {
    setBusy(true); setError(null); setMessage('');
    try {
      const draft = await vaccinationDraftApi.get(id);
      if (!editableStatuses.includes(draft.status)) throw new Error('Only an unfinalized draft may be changed.');
      const values = draftForm(draft);
      setForm(values); setInitialForm(values); setEditingId(id);
      setViewingDraft(null);
    } catch (exception) { setError(exception); }
    finally { setBusy(false); }
  };

  const submit = async (event) => {
    event.preventDefault();
    setBusy(true); setError(null); setMessage('');
    try {
      const payload = {};
      for (const [field, value] of Object.entries(form)) {
        if (editingId !== null && (field === 'appointmentId' || value === initialForm[field])) continue;
        if (value === '') {
          if (editingId !== null && field === 'authorizedNotes') payload[field] = '';
          else if (editingId !== null) throw new Error('Optional IDs and dates cannot be cleared by this API. Restore the value or enter a replacement.');
          continue;
        }
        payload[field] = ['appointmentId', 'vaccineBatchId', 'doseNumber'].includes(field)
          ? Number(value)
          : field === 'administrationTime'
            ? toApiIsoDate(value)
            : value;
      }
      if (editingId !== null && Object.keys(payload).length === 0) throw new Error('Change at least one editable field.');
      if (editingId !== null) await vaccinationDraftApi.update(editingId, payload);
      else await vaccinationDraftApi.create(payload);
      setMessage(editingId !== null ? 'Draft updated successfully.' : 'Draft created successfully.');
      resetForm();
      await load();
    } catch (exception) { setError(exception); }
    finally { setBusy(false); }
  };

  const discard = async (id) => {
    if (!window.confirm(`Discard vaccination draft ${id}? This action cannot be undone.`)) return;
    setBusy(true); setError(null); setMessage('');
    try {
      await vaccinationDraftApi.remove(id);
      if (editingId === id) resetForm();
      setMessage('Draft discarded successfully.');
      await load();
    } catch (exception) { setError(exception); }
    finally { setBusy(false); }
  };

  const confirmAndFinalize = async (target) => {
    const draft = typeof target === 'object' && target !== null ? target : drafts.find((d) => d.id === target);
    const draftId = draft ? draft.id : target;
    const isReady = draft?.status === 'READY_FOR_FINALIZATION';
    const confirmMessage = isReady
      ? 'Physical administration has already been confirmed for this draft. Complete finalization now?\n\nThis will permanently record the vaccination, deduct inventory, complete the appointment, and issue the official digital certificate.'
      : 'Confirm that the vaccine has physically been administered to this citizen and finalize the vaccination?\n\nThis action will permanently record the vaccination, deduct inventory, complete the appointment, and issue the official digital certificate.\n\nThis action cannot be edited or discarded afterward.';

    if (!window.confirm(confirmMessage)) return;

    setBusy(true);
    setError(null);
    setMessage('');
    try {
      await vaccinationDraftApi.confirmAndFinalize(draftId);
      setDrafts((prev) => prev.filter((d) => d.id !== draftId));
      if (viewingDraft?.id === draftId) setViewingDraft(null);
      if (editingId === draftId) resetForm();

      setMessage('Vaccination confirmed and finalized successfully. The permanent vaccination record and digital certificate are now available.');
      await load();
    } catch (exception) {
      setError(exception);
    } finally {
      setBusy(false);
    }
  };

  const activeDrafts = drafts.filter(
    (draft) => draft.status === 'IN_PROGRESS' || draft.status === 'READY_FOR_FINALIZATION',
  );

  return (
    <section aria-labelledby="vaccination-drafts-title">
      <div className="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4">
        <div>
          <span className="badge text-bg-primary mb-2">MF-05</span>
          <h1 id="vaccination-drafts-title" className="h2">Vaccination Drafts</h1>
          <p className="text-muted mb-0">Manage unfinished vaccination drafts through physical administration and finalization. Permanent records are available in Vaccination Records.</p>
        </div>
        <div className="d-flex gap-2">
          <button type="button" className="btn btn-outline-primary" disabled={loading || busy}
            onClick={() => { setError(null); load(); }}>Refresh</button>
          <button type="button" className="btn btn-primary" disabled={loading || busy} onClick={create}>Create Draft</button>
        </div>
      </div>

      {error && <div role="alert" className="alert alert-danger">
        <div>{error.message || 'Unable to complete the request.'}</div>
        {error.data?.fieldErrors?.length > 0 && <ul className="mb-0 mt-2">
          {error.data.fieldErrors.map(({ field, message: detail }, index) => <li key={`${field}-${index}`}>{field}: {detail}</li>)}
        </ul>}
      </div>}
      {message && <div role="status" className="alert alert-success">{message}</div>}
      {busy && <p role="status">Processing request…</p>}

      {viewingDraft && <div className="card shadow-sm mb-4" aria-labelledby="view-draft-title"><div className="card-body">
        <div className="d-flex flex-wrap justify-content-between align-items-start gap-2 mb-3">
          <h2 id="view-draft-title" className="h5 mb-0">View draft {viewingDraft.id}</h2>
          <button type="button" className="btn btn-sm btn-outline-secondary" onClick={() => setViewingDraft(null)}>Close</button>
        </div>
        <dl className="row mb-0">
          <dt className="col-sm-4">Draft ID</dt><dd className="col-sm-8">{viewingDraft.id}</dd>
          <dt className="col-sm-4">Appointment ID</dt><dd className="col-sm-8">{viewingDraft.appointmentId}</dd>
          <dt className="col-sm-4">Vaccine Batch ID</dt><dd className="col-sm-8">{viewingDraft.vaccineBatchId ?? '—'}</dd>
          <dt className="col-sm-4">Dose Number</dt><dd className="col-sm-8">{viewingDraft.doseNumber ?? '—'}</dd>
          <dt className="col-sm-4">Administration Date</dt><dd className="col-sm-8">{formatLocalDateOnly(viewingDraft.administrationTime) || '—'}</dd>
          <dt className="col-sm-4">Next Dose Date</dt><dd className="col-sm-8">{viewingDraft.nextDoseDate || '—'}</dd>
          <dt className="col-sm-4">Authorized Notes</dt><dd className="col-sm-8 text-break">{viewingDraft.authorizedNotes || '—'}</dd>
          <dt className="col-sm-4">Status</dt><dd className="col-sm-8">{viewingDraft.status}</dd>
          <dt className="col-sm-4">Physical Confirmation</dt>
          <dd className="col-sm-8">{viewingDraft.physicalAdministrationConfirmed ? 'Confirmed' : 'Not confirmed'}</dd>
        </dl>
      </div></div>}

      {form && <div className="card shadow-sm mb-4"><div className="card-body">
        <h2 className="h5">{editingId !== null ? `Edit draft ${editingId}` : 'Create Vaccination Draft'}</h2>
        <p className="small text-muted mb-4">Checked-in appointment ID is required. Other fields may be completed during clinical administration.</p>
        <form onSubmit={submit} aria-label="Vaccination draft form">
          <fieldset disabled={busy}>
            {/* Section 1: Patient & Appointment Identification */}
            <div className="mb-4">
              <h3 className="h6 text-uppercase text-secondary fw-semibold border-bottom pb-2 mb-3">
                Patient / Appointment
              </h3>
              {activeAppointmentContext ? (
                <div className="card border bg-light p-3" aria-label="Patient and appointment summary">
                  <div className="d-flex flex-wrap justify-content-between align-items-center gap-2">
                    <div>
                      <div className="fw-bold fs-6 text-dark">
                        {activeAppointmentContext.citizenName || `Citizen #${activeAppointmentContext.citizenId}`}
                      </div>
                      <div className="text-secondary small">
                        Appointment #{activeAppointmentContext.id}
                        {activeAppointmentContext.campaignName ? ` · ${activeAppointmentContext.campaignName}` : (activeAppointmentContext.campaignId ? ` · Campaign #${activeAppointmentContext.campaignId}` : '')}
                      </div>
                    </div>
                    <div>
                      <span className="badge bg-success-subtle text-success-emphasis border border-success-subtle fw-semibold">
                        Checked In ✓
                      </span>
                    </div>
                  </div>
                </div>
              ) : (
                <div className="row g-3">
                  <div className="col-12 col-md-6">
                    <label className="form-label fw-medium" htmlFor="draft-appointmentId">
                      Appointment ID
                    </label>
                    <input
                      id="draft-appointmentId"
                      aria-label="Appointment ID"
                      className="form-control"
                      type="number"
                      value={form.appointmentId}
                      required
                      min="1"
                      step="1"
                      disabled={editingId !== null}
                      placeholder="e.g. 112"
                      onChange={(event) => setForm((current) => ({ ...current, appointmentId: event.target.value }))}
                    />
                    <div className="form-text text-muted small">
                      Enter a checked-in appointment number.
                    </div>
                  </div>
                </div>
              )}
            </div>

            {/* Section 2: Clinical Vaccination Details */}
            <div className="mb-4">
              <h3 className="h6 text-uppercase text-secondary fw-semibold border-bottom pb-2 mb-3">
                Vaccination Details
              </h3>
              <div className="row g-3">
                <div className="col-12 col-md-6">
                  <label className="form-label fw-medium" htmlFor="draft-vaccineBatchId">
                    Vaccine Batch *
                  </label>
                  {availableBatches.length > 0 || form.vaccineBatchId ? (
                    <select
                      id="draft-vaccineBatchId"
                      aria-label="Vaccine Batch"
                      className="form-select"
                      required
                      value={form.vaccineBatchId}
                      onChange={(event) => setForm((current) => ({ ...current, vaccineBatchId: event.target.value }))}
                    >
                      <option value="">-- Select an available batch --</option>
                      {availableBatches.map((b) => (
                        <option key={b.id} value={String(b.id)}>
                          {b.batchNumber} — {b.vaccineName} — {b.availableQuantity} available · Expires {b.expiryDate}
                        </option>
                      ))}
                      {form.vaccineBatchId && !availableBatches.some((b) => String(b.id) === String(form.vaccineBatchId)) && (
                        <option value={form.vaccineBatchId}>
                          Batch #{form.vaccineBatchId} (Selected)
                        </option>
                      )}
                    </select>
                  ) : (
                    <select
                      id="draft-vaccineBatchId"
                      aria-label="Vaccine Batch"
                      className="form-select"
                      disabled
                      value=""
                    >
                      <option value="">
                        {loadingBatches ? 'Loading available batches…' : 'No compatible batches available'}
                      </option>
                    </select>
                  )}
                  {loadingBatches && (
                    <div className="form-text text-primary small">
                      <span className="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span>
                      Loading compatible batches…
                    </div>
                  )}
                  {!loadingBatches && form.appointmentId && availableBatches.length === 0 && (
                    <div className="form-text text-danger small">
                      No compatible vaccine batches with available stock are currently available for this appointment.
                    </div>
                  )}
                </div>

                <div className="col-12 col-md-6">
                  <label className="form-label fw-medium" htmlFor="draft-doseNumber">
                    Dose Number *
                  </label>
                  <input
                    id="draft-doseNumber"
                    aria-label="Dose Number"
                    className="form-control"
                    type="number"
                    min="1"
                    step="1"
                    placeholder="e.g. 1"
                    required
                    value={form.doseNumber}
                    onChange={(event) => setForm((current) => ({ ...current, doseNumber: event.target.value }))}
                  />
                  <div className="form-text text-muted small">
                    Enter the dose sequence being administered, e.g. 1 for first dose or 2 for second dose.
                  </div>
                </div>

                <div className="col-12 col-md-6">
                  <label className="form-label fw-medium" htmlFor="draft-administrationTime">
                    Administration date
                  </label>
                  <input
                    id="draft-administrationTime"
                    aria-label="Administration date"
                    className="form-control"
                    type="date"
                    value={form.administrationTime}
                    disabled
                  />
                  <div className="form-text text-muted small">
                    Set automatically to today's date for clinical administration.
                  </div>
                </div>

                <div className="col-12 col-md-6">
                  <label className="form-label fw-medium" htmlFor="draft-nextDoseDate">
                    Next Dose Date (optional)
                  </label>
                  <input
                    id="draft-nextDoseDate"
                    aria-label="Next dose date"
                    className="form-control"
                    type="date"
                    value={form.nextDoseDate}
                    min={form.administrationTime || undefined}
                    onChange={(event) => setForm((current) => ({ ...current, nextDoseDate: event.target.value }))}
                  />
                  <div className="form-text text-muted small">
                    Leave blank if no further dose is currently required or scheduled.
                  </div>
                </div>

                <div className="col-12">
                  <label className="form-label fw-medium" htmlFor="draft-notes">
                    Authorized notes
                  </label>
                  <textarea
                    id="draft-notes"
                    aria-label="Authorized notes"
                    className="form-control"
                    maxLength={2000}
                    rows={3}
                    placeholder="Enter clinical observations, manufacturer details, or special administration notes..."
                    value={form.authorizedNotes}
                    onChange={(event) => setForm((current) => ({ ...current, authorizedNotes: event.target.value }))}
                  />
                </div>
              </div>
            </div>

            <div className="d-flex gap-2 pt-2">
              <button
                type="submit"
                className="btn btn-primary"
                disabled={
                  !form.appointmentId ||
                  !form.vaccineBatchId ||
                  (editingId === null && availableBatches.length === 0) ||
                  !form.doseNumber ||
                  Number(form.doseNumber) < 1
                }
              >
                {editingId !== null ? 'Update Draft' : 'Create Draft'}
              </button>
              <button type="button" className="btn btn-outline-secondary" onClick={resetForm}>
                Cancel
              </button>
            </div>
          </fieldset>
        </form>
      </div></div>}

      {loading ? <p role="status">Loading drafts…</p> : activeDrafts.length === 0 ? (
        !error && (
          <div className="alert alert-info">
            No active vaccination drafts yet. Create a draft for a checked-in appointment.
          </div>
        )
      ) : (
        <>
          {/* Desktop Table View */}
          <div className="d-none d-md-block table-responsive">
            <table className="table table-striped align-middle">
              <caption>Active vaccination drafts</caption>
              <thead>
                <tr>
                  {['ID', 'Appointment ID', 'Vaccine batch ID', 'Dose number', 'Administration date', 'Next dose date', 'Authorized notes', 'Status', 'Physical confirmation (read-only)', 'Actions']
                    .map((label) => <th scope="col" key={label}>{label}</th>)}
                </tr>
              </thead>
              <tbody>
                {activeDrafts.map((draft) => (
                  <tr key={draft.id}>
                    <td>{draft.id}</td>
                    <td>{draft.appointmentId}</td>
                    <td>{draft.vaccineBatchId ?? '—'}</td>
                    <td>{draft.doseNumber ?? '—'}</td>
                    <td>{formatLocalDateOnly(draft.administrationTime) || '—'}</td>
                    <td>{draft.nextDoseDate || '—'}</td>
                    <td className="text-break"><TruncatedNotes text={draft.authorizedNotes} /></td>
                    <td>{getDraftStatusBadge(draft.status)}</td>
                    <td>{draft.physicalAdministrationConfirmed ? 'Confirmed' : 'Not confirmed'}</td>
                    <td className="text-nowrap">
                      <button type="button" className="btn btn-sm btn-outline-secondary me-2" aria-label={`View draft ${draft.id}`}
                        disabled={busy} onClick={() => view(draft.id)}>View</button>
                      {draft.status === 'IN_PROGRESS' && (
                        <>
                          <button type="button" className="btn btn-sm btn-outline-primary me-2" aria-label={`Edit draft ${draft.id}`}
                            disabled={busy} onClick={() => edit(draft.id)}>Edit</button>
                          <button type="button" className="btn btn-sm btn-outline-danger me-2" aria-label={`Discard draft ${draft.id}`}
                            disabled={busy} onClick={() => discard(draft.id)}>Discard Draft</button>
                          <button type="button" className="btn btn-sm btn-success me-2" aria-label={`Confirm and finalize draft ${draft.id}`}
                            disabled={busy} onClick={() => confirmAndFinalize(draft)}>Confirm &amp; Finalize</button>
                        </>
                      )}
                      {draft.status === 'READY_FOR_FINALIZATION' && (
                        <button type="button" className="btn btn-sm btn-success" aria-label={`Confirm and finalize draft ${draft.id}`}
                          disabled={busy} onClick={() => confirmAndFinalize(draft)}>Confirm &amp; Finalize</button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Mobile Card View */}
          <div className="d-md-none vstack gap-3 mb-4">
            {activeDrafts.map((draft) => (
              <div key={draft.id} className="card shadow-sm border">
                <div className="card-body p-3">
                  <div className="d-flex justify-content-between align-items-start mb-2">
                    <div>
                      <span className="fw-bold">Draft #{draft.id}</span>
                      <div className="small text-secondary">
                        Appt #{draft.appointmentId} · Admin: {formatLocalDateOnly(draft.administrationTime) || '—'}
                      </div>
                    </div>
                    {getDraftStatusBadge(draft.status)}
                  </div>

                  <div className="small text-secondary mb-2">
                    <div><strong>Batch:</strong> {draft.vaccineBatchId ? `Batch #${draft.vaccineBatchId}` : '—'}</div>
                    <div><strong>Dose:</strong> {draft.doseNumber ? `Dose ${draft.doseNumber}` : '—'}</div>
                    {draft.nextDoseDate && <div><strong>Next Dose:</strong> {draft.nextDoseDate}</div>}
                    <div>
                      <strong>Physical:</strong>{' '}
                      <span className={draft.physicalAdministrationConfirmed ? 'text-success fw-semibold' : 'text-secondary'}>
                        {draft.physicalAdministrationConfirmed ? 'Administered' : 'Pending confirmation'}
                      </span>
                    </div>
                    {draft.authorizedNotes && (
                      <div className="mt-1">
                        <strong>Clinical Notes:</strong> Note: {draft.authorizedNotes}
                      </div>
                    )}
                  </div>

                  <div className="d-flex flex-wrap gap-2 pt-2 border-top">
                    <button type="button" className="btn btn-sm btn-outline-secondary flex-fill"
                      disabled={busy} onClick={() => view(draft.id)}>View Details</button>
                    {draft.status === 'IN_PROGRESS' && (
                      <>
                        <button type="button" className="btn btn-sm btn-outline-primary flex-fill"
                          disabled={busy} onClick={() => edit(draft.id)}>Edit</button>
                        <button type="button" className="btn btn-sm btn-outline-danger flex-fill"
                          disabled={busy} onClick={() => discard(draft.id)}>Discard</button>
                        <button type="button" className="btn btn-sm btn-success w-100"
                          disabled={busy} onClick={() => confirmAndFinalize(draft)}>Confirm &amp; Finalize</button>
                      </>
                    )}
                    {draft.status === 'READY_FOR_FINALIZATION' && (
                      <button type="button" className="btn btn-sm btn-success w-100"
                        disabled={busy} onClick={() => confirmAndFinalize(draft)}>Confirm &amp; Finalize</button>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </>
      )}
    </section>
  );
}
