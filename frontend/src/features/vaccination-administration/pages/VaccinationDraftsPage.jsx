import React, { useCallback, useEffect, useState } from 'react';
import { vaccinationDraftApi } from '../api/vaccinationDraftApi';
import { formatLocalDateOnly, getLocalCalendarToday, toApiIsoDate } from '../utils/dateUtils';

const emptyForm = {
  appointmentId: '',
  vaccineBatchId: '',
  doseNumber: '',
  administrationTime: getLocalCalendarToday(),
  nextDoseDate: '',
  authorizedNotes: '',
};
const editableStatuses = ['IN_PROGRESS'];
const fields = [
  ['appointmentId', 'Appointment ID', 'number'],
  ['vaccineBatchId', 'Vaccine batch ID', 'number'],
  ['doseNumber', 'Dose number', 'number'],
  ['administrationTime', 'Administration date', 'date'],
  ['nextDoseDate', 'Next dose date', 'date'],
];

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
  const [drafts, setDrafts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [activeTab, setActiveTab] = useState('active');
  const [form, setForm] = useState(null);
  const [initialForm, setInitialForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [viewingDraft, setViewingDraft] = useState(null);
  const [error, setError] = useState(null);
  const [message, setMessage] = useState('');

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

  const resetForm = () => { setForm(null); setEditingId(null); };

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

  const confirm = async (id) => {
    if (!window.confirm(`Confirm physical administration for vaccination draft ${id}?`)) return;
    setBusy(true); setError(null); setMessage('');
    try {
      await vaccinationDraftApi.confirmAdministration(id);
      setMessage('Physical administration confirmed successfully.');
      await load();
    } catch (exception) { setError(exception); }
    finally { setBusy(false); }
  };

  const finalize = async (id) => {
    if (!window.confirm(`Finalize vaccination draft ${id}? This creates an immutable permanent record, deducts inventory, completes the appointment, and issues a digital certificate.`)) return;
    setBusy(true); setError(null); setMessage('');
    try {
      const response = await vaccinationDraftApi.finalize(id);
      setMessage(`Vaccination finalized successfully. Certificate reference: ${response?.certificateReference || 'Issued'}.`);
      await load();
    } catch (exception) { setError(exception); }
    finally { setBusy(false); }
  };

  const activeDrafts = drafts.filter(
    (draft) => draft.status === 'IN_PROGRESS' || draft.status === 'READY_FOR_FINALIZATION',
  );
  const historyDrafts = drafts.filter((draft) => draft.status === 'FINALIZED');
  const displayedDrafts = activeTab === 'active' ? activeDrafts : historyDrafts;

  return (
    <section aria-labelledby="vaccination-drafts-title">
      <div className="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4">
        <div>
          <span className="badge text-bg-primary mb-2">MF-05</span>
          <h1 id="vaccination-drafts-title" className="h2">Vaccination Drafts</h1>
          <p className="text-muted mb-0">Manage vaccination drafts from creation through physical administration confirmation and finalization.</p>
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
        <h2 className="h5">{editingId !== null ? `Edit draft ${editingId}` : 'Create draft'}</h2>
        <p className="small text-muted">Appointment ID is required. Other fields may be left blank when creating an incomplete draft.</p>
        <form onSubmit={submit} aria-label="Vaccination draft form">
          <fieldset disabled={busy}>
            <div className="row g-3">
              {fields.map(([field, label, type]) => <div key={field} className="col-12 col-md-6">
                <label className="form-label" htmlFor={`draft-${field}`}>{label}</label>
                <input id={`draft-${field}`} className="form-control" type={type} value={form[field]}
                  required={field === 'appointmentId'} min={type === 'number' ? 1 : undefined}
                  step={type === 'number' ? 1 : undefined}
                  disabled={
                    (field === 'appointmentId' && editingId !== null) ||
                    field === 'administrationTime'
                  }
                  onChange={(event) => setForm((current) => ({ ...current, [field]: event.target.value }))} />
              </div>)}
              <div className="col-12">
                <label className="form-label" htmlFor="draft-notes">Authorized notes</label>
                <textarea id="draft-notes" className="form-control" maxLength={2000} rows={3} value={form.authorizedNotes}
                  onChange={(event) => setForm((current) => ({ ...current, authorizedNotes: event.target.value }))} />
              </div>
              <div className="col-12 d-flex gap-2">
                <button type="submit" className="btn btn-primary">{editingId !== null ? 'Update Draft' : 'Create Draft'}</button>
                <button type="button" className="btn btn-outline-secondary" onClick={resetForm}>Cancel</button>
              </div>
            </div>
          </fieldset>
        </form>
      </div></div>}

      <ul className="nav nav-tabs mb-3" role="tablist">
        <li className="nav-item" role="presentation">
          <button
            type="button"
            className={`nav-link ${activeTab === 'active' ? 'active' : ''}`}
            id="active-drafts-tab"
            role="tab"
            aria-selected={activeTab === 'active'}
            onClick={() => setActiveTab('active')}
          >
            Active Drafts ({activeDrafts.length})
          </button>
        </li>
        <li className="nav-item" role="presentation">
          <button
            type="button"
            className={`nav-link ${activeTab === 'history' ? 'active' : ''}`}
            id="draft-history-tab"
            role="tab"
            aria-selected={activeTab === 'history'}
            onClick={() => setActiveTab('history')}
          >
            Draft History ({historyDrafts.length})
          </button>
        </li>
      </ul>

      {loading ? <p role="status">Loading drafts…</p> : displayedDrafts.length === 0 ? (
        !error && (
          <div className="alert alert-info">
            {activeTab === 'active'
              ? 'No active vaccination drafts yet. Create a draft for a checked-in appointment.'
              : 'No finalized draft history yet.'}
          </div>
        )
      ) : <div className="table-responsive"><table className="table table-striped align-middle">
        <caption>{activeTab === 'active' ? 'Active vaccination drafts' : 'Finalized vaccination draft history'}</caption>
        <thead><tr>{['ID', 'Appointment ID', 'Vaccine batch ID', 'Dose number', 'Administration date', 'Next dose date', 'Authorized notes', 'Status', 'Physical confirmation (read-only)', 'Actions']
          .map((label) => <th scope="col" key={label}>{label}</th>)}</tr></thead>
        <tbody>{displayedDrafts.map((draft) => <tr key={draft.id}>
          <td>{draft.id}</td><td>{draft.appointmentId}</td><td>{draft.vaccineBatchId ?? '—'}</td><td>{draft.doseNumber ?? '—'}</td>
          <td>{formatLocalDateOnly(draft.administrationTime) || '—'}</td>
          <td>{draft.nextDoseDate || '—'}</td><td className="text-break">{draft.authorizedNotes || '—'}</td><td>{draft.status}</td>
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
                <button type="button" className="btn btn-sm btn-outline-success me-2" aria-label={`Confirm administration for draft ${draft.id}`}
                  disabled={busy} onClick={() => confirm(draft.id)}>Confirm Administration</button>
              </>
            )}
            {draft.status === 'READY_FOR_FINALIZATION' && (
              <button type="button" className="btn btn-sm btn-success" aria-label={`Finalize draft ${draft.id}`}
                disabled={busy} onClick={() => finalize(draft.id)}>Finalize</button>
            )}
          </td>
        </tr>)}</tbody>
      </table></div>}
    </section>
  );
}
