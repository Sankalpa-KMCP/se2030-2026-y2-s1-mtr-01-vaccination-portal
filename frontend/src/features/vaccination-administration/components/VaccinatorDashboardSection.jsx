import React, { useCallback, useEffect, useId, useMemo, useState } from 'react';
import { useAuth } from '../../../auth/context/AuthContext';
import {
  getMyAssignedVaccinationSessions,
  getVaccinatorQueue,
} from '../../appointment-management/api/checkInApi';
import { listVaccinatorCorrectionRequests, listVaccinatorRecords } from '../api/certificateApi';
import { getLocalCalendarToday } from '../utils/dateUtils';

const VACCINATOR_ROLE = 'ROLE_VACCINATOR';

function slotIdsFor(session) {
  const nested = Array.isArray(session?.slots)
    ? session.slots.map((slot) => slot?.id).filter(Boolean)
    : [];
  return nested.length > 0 ? nested : [session?.slotId].filter(Boolean);
}

function sessionWhen(session) {
  if (!session) return 'No session assigned';
  if (session.date) return `${session.date}${session.startTime ? ` · ${session.startTime}–${session.endTime || ''}` : ''}`;
  if (!session.startAt) return session.centerName || 'Assigned session';
  const start = new Date(session.startAt);
  const end = session.endAt ? new Date(session.endAt) : null;
  return `${start.toLocaleDateString(undefined, { dateStyle: 'medium' })} · ${start.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}${end ? `–${end.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}` : ''}`;
}

function MetricCard({ label, value, detail, tone = 'primary' }) {
  return <div className="col-12 col-sm-6 col-lg-3">
    <div className="card border h-100 shadow-sm">
      <div className="card-body p-3">
        <div className={`small fw-semibold text-${tone} mb-2`}>{label}</div>
        <div className="h3 fw-bold text-dark mb-1">{value}</div>
        <div className="small text-muted text-truncate">{detail}</div>
      </div>
    </div>
  </div>;
}

/** Compact, live Vaccinator workspace for the shared staff dashboard. */
export default function VaccinatorDashboardSection({ sessions: propSessions, initialSlotId, showHeader = true } = {}) {
  const { user } = useAuth();
  const isVaccinator = Boolean(user?.roles?.includes(VACCINATOR_ROLE));
  const selectorId = useId();
  const [sessions, setSessions] = useState(() => (Array.isArray(propSessions) ? propSessions : []));
  const [sessionsLoading, setSessionsLoading] = useState(!Array.isArray(propSessions));
  const [sessionsError, setSessionsError] = useState(null);
  const [selectedSessionId, setSelectedSessionId] = useState(null);
  const [queue, setQueue] = useState(null);
  const [queueLoading, setQueueLoading] = useState(false);
  const [queueError, setQueueError] = useState(null);
  const [corrections, setCorrections] = useState([]);
  const [records, setRecords] = useState([]);

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
      setSessionsError(error.message || 'Unable to load assigned sessions.');
    } finally {
      setSessionsLoading(false);
    }
  }, [propSessions]);

  const loadSupportingData = useCallback(async () => {
    const [correctionResult, recordResult] = await Promise.allSettled([
      listVaccinatorCorrectionRequests(),
      listVaccinatorRecords(),
    ]);
    setCorrections(correctionResult.status === 'fulfilled' && Array.isArray(correctionResult.value) ? correctionResult.value : []);
    setRecords(recordResult.status === 'fulfilled' && Array.isArray(recordResult.value) ? recordResult.value : []);
  }, []);

  useEffect(() => {
    if (!isVaccinator) return;
    loadSessions();
    loadSupportingData();
  }, [isVaccinator, loadSessions, loadSupportingData]);

  useEffect(() => {
    if (sessions.length === 0) {
      setSelectedSessionId(null);
      return;
    }
    const requested = initialSlotId == null ? null : sessions.find((session) =>
      String(session.id) === String(initialSlotId) || slotIdsFor(session).some((id) => String(id) === String(initialSlotId)));
    setSelectedSessionId((current) => sessions.some((session) => String(session.id) === String(current))
      ? current
      : (requested || sessions[0]).id);
  }, [initialSlotId, sessions]);

  const activeSession = useMemo(
    () => sessions.find((session) => String(session.id) === String(selectedSessionId)) || null,
    [selectedSessionId, sessions]
  );

  const loadQueue = useCallback(async (session) => {
    const slotIds = slotIdsFor(session);
    if (slotIds.length === 0) {
      setQueue([]);
      return;
    }
    setQueueLoading(true);
    setQueueError(null);
    try {
      const results = await Promise.all(slotIds.map((slotId) => getVaccinatorQueue(slotId)));
      const unique = new Map();
      results.flat().forEach((appointment) => unique.set(appointment.id, appointment));
      setQueue([...unique.values()]);
    } catch (error) {
      setQueue(null);
      setQueueError(error.message || 'Unable to load the checked-in queue.');
    } finally {
      setQueueLoading(false);
    }
  }, []);

  useEffect(() => {
    if (activeSession) loadQueue(activeSession);
    else setQueue(null);
  }, [activeSession, loadQueue]);

  const approvedCorrections = useMemo(
    () => corrections.filter((request) => request.status === 'APPROVED'),
    [corrections]
  );
  const finalizedToday = useMemo(() => {
    const today = getLocalCalendarToday();
    return records.filter((record) => String(record.finalizedAt || record.administrationTime || '').slice(0, 10) === today).length;
  }, [records]);

  if (!isVaccinator) return null;

  return <section aria-label="Vaccinator Workspace" className="mb-5">
    {showHeader && <div className="mb-4">
      <h2 className="h3 fw-bold mb-1">Vaccinator Dashboard</h2>
      <p className="text-muted mb-0">Manage today&apos;s vaccination work</p>
    </div>}

    {sessionsError && <div className="alert alert-danger" role="alert">{sessionsError}<button type="button" className="btn btn-outline-danger btn-sm ms-3" onClick={loadSessions}>Retry</button></div>}

    <div className="row g-3 mb-4" aria-label="Vaccinator metrics">
      <MetricCard label="Ready Patients" value={queueLoading ? '…' : queue?.length || 0} detail={activeSession ? 'Checked-in citizens' : 'No assigned session'} />
      <MetricCard label="Corrections Needed" value={approvedCorrections.length} detail="Approved amendments to apply" tone={approvedCorrections.length ? 'danger' : 'success'} />
      <MetricCard label="Finalized Today" value={finalizedToday} detail="Finalized by you today" tone="success" />
      <MetricCard label="Today’s Session" value={sessionsLoading ? '…' : activeSession ? `#${activeSession.id}` : 'None'} detail={activeSession ? `${activeSession.centerName || 'Assigned center'} · ${sessionWhen(activeSession)}` : 'No session assigned'} tone="secondary" />
    </div>

    <div className="card border-0 shadow-sm">
      <div className="card-body p-4">
        <div className="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3 mb-3">
          <div>
            <h3 className="h5 fw-bold mb-1">Today's Checked-In Patients</h3>
            <div className="small text-muted">Only citizens already checked in for your assigned session appear here.</div>
          </div>
          {sessions.length > 1 && <div className="d-flex align-items-center gap-2">
            <label htmlFor={selectorId} className="small fw-semibold mb-0">Session</label>
            <select id={selectorId} className="form-select form-select-sm" value={activeSession?.id || ''} onChange={(event) => setSelectedSessionId(event.target.value)}>
              {sessions.map((session) => <option key={session.id} value={session.id}>Session #{session.id} · {session.centerName}</option>)}
            </select>
          </div>}
        </div>

        {sessionsLoading && <div className="text-center py-4"><div className="spinner-border text-primary" role="status"><span className="visually-hidden">Loading assigned session…</span></div></div>}
        {!sessionsLoading && !activeSession && !sessionsError && <div className="alert alert-light border mb-0">No active vaccination session is currently assigned.</div>}
        {queueLoading && <div className="text-center py-4"><div className="spinner-border text-primary" role="status"><span className="visually-hidden">Loading queue…</span></div></div>}
        {!queueLoading && queueError && <div className="alert alert-danger mb-0" role="alert"><strong>Error Loading Queue:</strong> {queueError}<button type="button" className="btn btn-outline-danger btn-sm ms-3" onClick={() => loadQueue(activeSession)}>Retry</button></div>}
        {!queueLoading && !queueError && activeSession && queue?.length === 0 && <div className="alert alert-light border mb-0">No checked-in patients are currently ready for vaccination.</div>}
        {!queueLoading && !queueError && queue?.length > 0 && <div className="vstack gap-2">
          {queue.map((appointment) => (
            <div key={appointment.id} className="border rounded-3 p-3">
              <div className="fw-semibold">Appointment #{appointment.id}</div>
              <div className="small text-muted">{appointment.citizenName || `Citizen #${appointment.citizenId}`} · {appointment.campaignName || `Campaign #${appointment.campaignId}`}</div>
            </div>
          ))}
        </div>}
      </div>
    </div>
  </section>;
}
