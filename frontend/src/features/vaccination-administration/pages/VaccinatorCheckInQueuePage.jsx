import React, { useCallback, useEffect, useId, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import PageHeader from '../../../components/common/PageHeader';
import AppointmentStatusBadge from '../../appointment-management/components/AppointmentStatusBadge';
import {
  getMyAssignedVaccinationSessions,
  getVaccinatorQueue,
} from '../../appointment-management/api/checkInApi';
import { listMyVaccinationDrafts } from '../api/vaccinationDraftApi';

const ACTIVE_DRAFT_STATUSES = ['IN_PROGRESS', 'READY_FOR_FINALIZATION'];

function slotIdsFor(session) {
  const nested = Array.isArray(session?.slots)
    ? session.slots.map((slot) => slot?.id).filter(Boolean)
    : [];
  return nested.length > 0 ? nested : [session?.slotId].filter(Boolean);
}

function dateLabel(session) {
  if (session?.date) return session.date;
  if (!session?.startAt) return '';
  return new Date(session.startAt).toLocaleDateString(undefined, { dateStyle: 'medium' });
}

function timeLabel(session) {
  if (session?.startTime && session?.endTime) return `${session.startTime}–${session.endTime}`;
  if (!session?.startAt || !session?.endAt) return '';
  const options = { hour: '2-digit', minute: '2-digit' };
  return `${new Date(session.startAt).toLocaleTimeString([], options)}–${new Date(session.endAt).toLocaleTimeString([], options)}`;
}

/** Read-only clinical queue for the signed-in Vaccinator's assigned sessions. */
export default function VaccinatorCheckInQueuePage({ sessions: propSessions, initialSlotId } = {}) {
  const navigate = useNavigate();
  const sessionSelectorId = useId();
  const [sessions, setSessions] = useState(() => (Array.isArray(propSessions) ? propSessions : []));
  const [sessionsLoading, setSessionsLoading] = useState(!Array.isArray(propSessions));
  const [sessionsError, setSessionsError] = useState(null);
  const [selectedSessionId, setSelectedSessionId] = useState(null);
  const [queue, setQueue] = useState(null);
  const [queueLoading, setQueueLoading] = useState(false);
  const [queueError, setQueueError] = useState(null);
  const [drafts, setDrafts] = useState([]);
  const [feedback, setFeedback] = useState(null);

  const loadSessions = useCallback(async () => {
    if (Array.isArray(propSessions)) {
      setSessions(propSessions);
      setSessionsLoading(false);
      return;
    }
    setSessionsLoading(true);
    setSessionsError(null);
    try {
      const data = await getMyAssignedVaccinationSessions();
      setSessions(Array.isArray(data) ? data : []);
    } catch (error) {
      setSessions([]);
      setSessionsError(error.message || 'Unable to load your assigned sessions.');
    } finally {
      setSessionsLoading(false);
    }
  }, [propSessions]);

  useEffect(() => { loadSessions(); }, [loadSessions]);

  useEffect(() => {
    if (sessions.length === 0) {
      setSelectedSessionId(null);
      return;
    }
    const requested = initialSlotId == null
      ? null
      : sessions.find((session) =>
          String(session.id) === String(initialSlotId) ||
          slotIdsFor(session).some((slotId) => String(slotId) === String(initialSlotId)));
    setSelectedSessionId((current) => {
      if (sessions.some((session) => String(session.id) === String(current))) return current;
      return (requested || sessions[0]).id;
    });
  }, [initialSlotId, sessions]);

  const activeSession = useMemo(
    () => sessions.find((session) => String(session.id) === String(selectedSessionId)) || null,
    [selectedSessionId, sessions]
  );

  const loadDrafts = useCallback(async () => {
    try {
      const data = await listMyVaccinationDrafts();
      setDrafts(Array.isArray(data) ? data : []);
    } catch {
      setDrafts([]);
    }
  }, []);

  useEffect(() => { loadDrafts(); }, [loadDrafts]);

  const loadQueue = useCallback(async (session) => {
    const slotIds = slotIdsFor(session);
    if (slotIds.length === 0) {
      setQueue([]);
      return;
    }
    setQueueLoading(true);
    setQueueError(null);
    setFeedback(null);
    try {
      const results = await Promise.all(slotIds.map((slotId) => getVaccinatorQueue(slotId)));
      const unique = new Map();
      results.flat().forEach((appointment) => unique.set(appointment.id, appointment));
      setQueue([...unique.values()]);
    } catch (error) {
      setQueue(null);
      setQueueError(error.message || 'Failed to fetch the checked-in queue.');
    } finally {
      setQueueLoading(false);
    }
  }, []);

  useEffect(() => {
    if (activeSession) loadQueue(activeSession);
    else {
      setQueue(null);
      setQueueError(null);
    }
  }, [activeSession, loadQueue]);

  const handleStartVaccination = (appointment) => {
    navigate('/app/staff/vaccination-drafts', { state: { preselectedAppointment: appointment } });
  };

  return (
    <div>
      <PageHeader title="Check-In Queue" subtitle="Citizens checked in and ready for vaccination." />
      {feedback && <div className={`alert alert-${feedback.type}`} role="alert">{feedback.message}</div>}
      {sessionsLoading && <div className="text-center py-5"><div className="spinner-border text-primary" role="status"><span className="visually-hidden">Loading assigned sessions…</span></div></div>}
      {!sessionsLoading && sessionsError && <div className="alert alert-danger" role="alert">{sessionsError}<button type="button" className="btn btn-outline-danger btn-sm ms-3" onClick={loadSessions}>Retry</button></div>}
      {!sessionsLoading && !sessionsError && sessions.length === 0 && <div className="alert alert-light border text-center py-4">No active vaccination session is currently assigned to you.</div>}

      {!sessionsLoading && !sessionsError && activeSession && <>
        <div className="card border mb-4 bg-light shadow-sm">
          <div className="card-body p-3 d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3">
            <div>
              <span className="badge bg-success me-2">ASSIGNED SESSION</span>
              <span className="fw-bold">Session #{activeSession.id} · {activeSession.centerName || 'Assigned Center'}</span>
              <div className="text-secondary small mt-1">{[dateLabel(activeSession), timeLabel(activeSession)].filter(Boolean).join(' · ')}</div>
            </div>
            {sessions.length > 1 && <div className="d-flex align-items-center gap-2">
              <label htmlFor={sessionSelectorId} className="small fw-semibold mb-0">Session</label>
              <select id={sessionSelectorId} className="form-select form-select-sm" value={activeSession.id} onChange={(event) => setSelectedSessionId(event.target.value)}>
                {sessions.map((session) => <option key={session.id} value={session.id}>Session #{session.id} · {session.centerName}</option>)}
              </select>
            </div>}
          </div>
        </div>

        {queueLoading && <div className="text-center py-5"><div className="spinner-border text-primary" role="status"><span className="visually-hidden">Loading queue…</span></div></div>}
        {!queueLoading && queueError && <div className="alert alert-danger" role="alert"><strong>Error Loading Queue:</strong> {queueError}<button type="button" className="btn btn-outline-danger btn-sm ms-3" onClick={() => loadQueue(activeSession)}>Retry</button></div>}
        {!queueLoading && !queueError && queue?.length === 0 && <div className="card border-0 shadow-sm text-center py-5"><div className="card-body"><h2 className="h5">Queue Empty</h2><p className="text-muted mb-0">No checked-in patients are currently ready for vaccination.</p></div></div>}
        {!queueLoading && !queueError && queue?.length > 0 && <div className="card border-0 shadow-sm">
          <div className="card-header bg-white py-3 d-flex justify-content-between align-items-center">
            <h2 className="h6 fw-bold mb-0">Ready for Vaccination ({queue.length})</h2>
            <button type="button" className="btn btn-outline-secondary btn-sm" onClick={() => loadQueue(activeSession)}>Refresh</button>
          </div>
          <div className="table-responsive"><table className="table table-hover align-middle mb-0" aria-label="Checked-in vaccination queue">
            <thead className="table-light"><tr><th>Appointment</th><th>Citizen</th><th>Campaign</th><th>Status</th><th className="text-end">Action</th></tr></thead>
            <tbody>{queue.map((appointment) => {
              const hasDraft = drafts.some((draft) => draft.appointmentId === appointment.id && ACTIVE_DRAFT_STATUSES.includes(draft.status));
              return <tr key={appointment.id}>
                <td className="fw-semibold">Appointment #{appointment.id}</td>
                <td>{appointment.citizenName || `Citizen #${appointment.citizenId}`}</td>
                <td>{appointment.campaignName || `Campaign #${appointment.campaignId}`}</td>
                <td><AppointmentStatusBadge status={appointment.status} /></td>
                <td className="text-end"><button type="button" className="btn btn-primary btn-sm" aria-label={`Start vaccination for appointment ${appointment.id}`} onClick={() => handleStartVaccination(appointment)}>{hasDraft ? 'Open Draft' : 'Start Vaccination'}</button></td>
              </tr>;
            })}</tbody>
          </table></div>
        </div>}
      </>}
    </div>
  );
}
