import React, { useState, useEffect, useRef } from 'react';
import {
  getSessionById,
  formatInTimezone,
  scheduleSession,
  startSession,
  completeSession,
} from '../api/sessionApi';
import SessionAssignmentsTab from './SessionAssignmentsTab';
import SessionCapacityPlanTab from './SessionCapacityPlanTab';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import LoadingSpinner from '../../../components/common/LoadingSpinner';

export default function SessionDetailsModal({
  isOpen,
  onClose,
  sessionId,
  campaignsMap = {},
  centersMap = {},
  onSessionUpdated,
}) {
  const [session, setSession] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState('overview');

  // Lifecycle Action State
  const [submittingAction, setSubmittingAction] = useState(null);
  const [pendingConfirmAction, setPendingConfirmAction] = useState(null);
  const [lifecycleError, setLifecycleError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Concurrency & Session Identity Guards with Token-Owned Mutex
  const lifecycleRequestSeqRef = useRef(0);
  const lifecycleMutexOwnerRef = useRef(null);
  const lifecycleGenRef = useRef(0);
  const activeSessionIdRef = useRef(sessionId);
  const confirmHeadingRef = useRef(null);

  useEffect(() => {
    activeSessionIdRef.current = sessionId;
    setLifecycleError(null);
    setSuccessMessage(null);
    setPendingConfirmAction(null);
    setSubmittingAction(null);
    lifecycleMutexOwnerRef.current = null;
    lifecycleGenRef.current += 1;

    if (!isOpen || !sessionId) {
      setSession(null);
      setLoading(false);
      setError(null);
      setActiveTab('overview');
      return;
    }

    setLoading(true);
    setError(null);
    setActiveTab('overview');

    const currentSessionId = sessionId;
    const currentGen = lifecycleGenRef.current;

    getSessionById(sessionId)
      .then((data) => {
        if (activeSessionIdRef.current === currentSessionId && lifecycleGenRef.current === currentGen) {
          setSession(data);
          setLoading(false);
        }
      })
      .catch((err) => {
        if (activeSessionIdRef.current === currentSessionId && lifecycleGenRef.current === currentGen) {
          setError(err);
          setLoading(false);
        }
      });
  }, [isOpen, sessionId]);

  // Manage keyboard focus for inline confirmation announcement
  useEffect(() => {
    if (pendingConfirmAction && confirmHeadingRef.current) {
      confirmHeadingRef.current.focus();
    }
  }, [pendingConfirmAction]);

  if (!isOpen) return null;

  const campaign = session ? campaignsMap[session.campaignId] : null;
  const center = session ? centersMap[session.centerId] : null;
  const tz = session?.scheduleTimezone || 'UTC';

  const getStatusBadge = (status) => {
    switch (status) {
      case 'DRAFT':
        return <span className="badge bg-secondary text-white">DRAFT</span>;
      case 'SCHEDULED':
        return <span className="badge bg-primary text-white">SCHEDULED</span>;
      case 'IN_PROGRESS':
        return <span className="badge bg-success text-white">IN_PROGRESS</span>;
      case 'COMPLETED':
        return <span className="badge bg-dark text-white">COMPLETED</span>;
      case 'CANCELLED':
        return <span className="badge bg-danger text-white">CANCELLED</span>;
      default:
        return <span className="badge bg-light text-dark border">{status || 'UNKNOWN'}</span>;
    }
  };

  const handleInitiateAction = (actionType) => {
    setLifecycleError(null);
    setSuccessMessage(null);
    setPendingConfirmAction(actionType);
  };

  const handleCancelConfirm = () => {
    setPendingConfirmAction(null);
    setLifecycleError(null);
  };

  const handleExecuteAction = async (actionType) => {
    if (lifecycleMutexOwnerRef.current !== null) {
      return;
    }

    const currentSessionId = sessionId;
    const reqGen = ++lifecycleGenRef.current;
    const reqToken = ++lifecycleRequestSeqRef.current;

    lifecycleMutexOwnerRef.current = reqToken;
    setSubmittingAction(actionType);
    setLifecycleError(null);
    setSuccessMessage(null);

    try {
      let updated = null;
      if (actionType === 'SCHEDULE') {
        updated = await scheduleSession(currentSessionId);
      } else if (actionType === 'START') {
        updated = await startSession(currentSessionId);
      } else if (actionType === 'COMPLETE') {
        updated = await completeSession(currentSessionId);
      }

      // 1. Authoritative parent collection reconciliation (ALWAYS triggered on successful mutation)
      if (updated && onSessionUpdated) {
        onSessionUpdated(updated);
      }

      // 2. Guarded modal-local state update (ONLY if modal still views target session)
      if (activeSessionIdRef.current === currentSessionId && lifecycleGenRef.current === reqGen) {
        if (updated) {
          setSession(updated);
          const actionPast =
            actionType === 'SCHEDULE'
              ? 'scheduled'
              : actionType === 'START'
              ? 'started'
              : 'completed';
          setSuccessMessage(`Session #${updated.id} successfully ${actionPast}.`);
        }
        setPendingConfirmAction(null);
      }
    } catch (err) {
      if (activeSessionIdRef.current === currentSessionId && lifecycleGenRef.current === reqGen) {
        setLifecycleError(err);
      }
    } finally {
      if (activeSessionIdRef.current === currentSessionId && lifecycleGenRef.current === reqGen) {
        setSubmittingAction(null);
      }
      // ONLY the request that currently owns the mutex may release it
      if (lifecycleMutexOwnerRef.current === reqToken) {
        lifecycleMutexOwnerRef.current = null;
      }
    }
  };

  return (
    <div
      className="modal fade show d-block"
      tabIndex="-1"
      role="dialog"
      aria-labelledby="sessionDetailsModalTitle"
      style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}
    >
      <div className="modal-dialog modal-lg modal-dialog-centered" role="document">
        <div className="modal-content border-0 shadow">
          <div className="modal-header bg-light border-bottom">
            <div className="d-flex align-items-center gap-2">
              <h5 className="modal-title h5 mb-0 text-dark fw-bold" id="sessionDetailsModalTitle">
                Vaccination Session #{sessionId}
              </h5>
              {session && getStatusBadge(session.lifecycleStatus)}
            </div>
            <button
              type="button"
              className="btn-close"
              aria-label="Close"
              onClick={onClose}
            />
          </div>

          {/* Navigation Tabs */}
          {session && (
            <div className="px-4 pt-3 border-bottom bg-light">
              <ul className="nav nav-tabs card-header-tabs" role="tablist">
                <li className="nav-item">
                  <button
                    type="button"
                    className={`nav-link ${activeTab === 'overview' ? 'active fw-bold text-primary' : 'text-secondary'}`}
                    onClick={() => setActiveTab('overview')}
                  >
                    Overview
                  </button>
                </li>
                <li className="nav-item">
                  <button
                    type="button"
                    className={`nav-link ${activeTab === 'assignments' ? 'active fw-bold text-primary' : 'text-secondary'}`}
                    onClick={() => setActiveTab('assignments')}
                  >
                    Operational Assignments
                  </button>
                </li>
                <li className="nav-item">
                  <button
                    type="button"
                    className={`nav-link ${activeTab === 'capacity' ? 'active fw-bold text-primary' : 'text-secondary'}`}
                    onClick={() => setActiveTab('capacity')}
                  >
                    Capacity Plan
                  </button>
                </li>
              </ul>
            </div>
          )}

          <div className="modal-body p-4">
            {error && <ApiErrorAlert error={error} />}

            {loading && <LoadingSpinner message="Loading session details..." />}

            {!loading && session && activeTab === 'overview' && (
              <div className="d-flex flex-column gap-3">
                {/* Session Lifecycle Management Action Card */}
                <div className="card border p-3 bg-light">
                  <div className="d-flex flex-wrap align-items-center justify-content-between gap-3">
                    <div>
                      <span className="text-muted small d-block">Lifecycle Status & Actions</span>
                      <div className="d-flex align-items-center gap-2 mt-1">
                        {getStatusBadge(session.lifecycleStatus)}
                        <span className="small text-secondary">
                          {session.lifecycleStatus === 'DRAFT' &&
                            'Draft session — assign stations/staff and approve a capacity plan to schedule.'}
                          {session.lifecycleStatus === 'SCHEDULED' &&
                            'Scheduled session — ready for operational start once start time arrives.'}
                          {session.lifecycleStatus === 'IN_PROGRESS' &&
                            'In progress — active vaccination session underway.'}
                          {session.lifecycleStatus === 'COMPLETED' &&
                            'Completed session — all vaccination operations concluded.'}
                          {session.lifecycleStatus === 'CANCELLED' &&
                            'Cancelled session — inactive.'}
                        </span>
                      </div>
                    </div>

                    {/* Status-dependent Action Buttons */}
                    <div className="d-flex align-items-center gap-2">
                      {session.lifecycleStatus === 'DRAFT' && !pendingConfirmAction && (
                        <button
                          type="button"
                          className="btn btn-sm btn-primary fw-semibold"
                          onClick={() => handleInitiateAction('SCHEDULE')}
                          disabled={submittingAction !== null}
                        >
                          Schedule Session
                        </button>
                      )}

                      {session.lifecycleStatus === 'SCHEDULED' && !pendingConfirmAction && (
                        <button
                          type="button"
                          className="btn btn-sm btn-success fw-semibold text-white"
                          onClick={() => handleInitiateAction('START')}
                          disabled={submittingAction !== null}
                        >
                          Start Session
                        </button>
                      )}

                      {session.lifecycleStatus === 'IN_PROGRESS' && !pendingConfirmAction && (
                        <button
                          type="button"
                          className="btn btn-sm btn-dark fw-semibold"
                          onClick={() => handleInitiateAction('COMPLETE')}
                          disabled={submittingAction !== null}
                        >
                          Complete Session
                        </button>
                      )}
                    </div>
                  </div>

                  {/* Inline Confirmation Card */}
                  {pendingConfirmAction && (
                    <div
                      className="mt-3 p-3 rounded border bg-white shadow-sm"
                      role="region"
                      aria-live="polite"
                      aria-labelledby="sessionConfirmHeading"
                    >
                      <h6
                        id="sessionConfirmHeading"
                        ref={confirmHeadingRef}
                        tabIndex="-1"
                        className="fw-bold text-dark mb-2"
                      >
                        {pendingConfirmAction === 'SCHEDULE' && 'Confirm Session Scheduling'}
                        {pendingConfirmAction === 'START' && 'Confirm Session Start'}
                        {pendingConfirmAction === 'COMPLETE' && 'Confirm Session Completion'}
                      </h6>
                      <p className="small text-secondary mb-3">
                        {pendingConfirmAction === 'SCHEDULE' &&
                          'Transitioning this session to SCHEDULED will validate operational readiness and lock draft assignments. The backend requires at least one active station, at least one active vaccinator, and an approved, non-stale capacity plan with positive capacity.'}
                        {pendingConfirmAction === 'START' &&
                          'Transitioning this session to IN_PROGRESS begins active session operations. The backend requires the current time to have reached or passed the scheduled session start time.'}
                        {pendingConfirmAction === 'COMPLETE' &&
                          'Transitioning this session to COMPLETED concludes all vaccination activities. This is a terminal lifecycle state that cannot be undone.'}
                      </p>

                      {/* Action Error during confirmation */}
                      {lifecycleError && (
                        <div className="mb-3">
                          <ApiErrorAlert error={lifecycleError} onDismiss={() => setLifecycleError(null)} />
                          {pendingConfirmAction === 'SCHEDULE' && (
                            <div className="form-text text-muted small mt-1">
                              Tip: Review the <strong>Operational Assignments</strong> and <strong>Capacity Plan</strong> tabs to ensure all readiness requirements are satisfied before scheduling.
                            </div>
                          )}
                        </div>
                      )}

                      <div className="d-flex justify-content-end gap-2">
                        <button
                          type="button"
                          className="btn btn-sm btn-secondary"
                          onClick={handleCancelConfirm}
                          disabled={submittingAction !== null}
                        >
                          Cancel
                        </button>
                        <button
                          type="button"
                          className={`btn btn-sm fw-semibold ${
                            pendingConfirmAction === 'SCHEDULE'
                              ? 'btn-primary'
                              : pendingConfirmAction === 'START'
                              ? 'btn-success text-white'
                              : 'btn-dark'
                          }`}
                          onClick={() => handleExecuteAction(pendingConfirmAction)}
                          disabled={submittingAction !== null}
                        >
                          {submittingAction ? (
                            <>
                              <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true" />
                              Executing...
                            </>
                          ) : (
                            `Confirm ${
                              pendingConfirmAction === 'SCHEDULE'
                                ? 'Schedule'
                                : pendingConfirmAction === 'START'
                                ? 'Start'
                                : 'Complete'
                            }`
                          )}
                        </button>
                      </div>
                    </div>
                  )}

                  {/* General Lifecycle Feedback (when not confirming) */}
                  {!pendingConfirmAction && lifecycleError && (
                    <div className="mt-3">
                      <ApiErrorAlert error={lifecycleError} onDismiss={() => setLifecycleError(null)} />
                      {session.lifecycleStatus === 'DRAFT' && (
                        <div className="form-text text-muted small mt-1">
                          Tip: Review the <strong>Operational Assignments</strong> and <strong>Capacity Plan</strong> tabs to ensure all readiness requirements are satisfied before scheduling.
                        </div>
                      )}
                    </div>
                  )}

                  {!pendingConfirmAction && successMessage && (
                    <div className="alert alert-success alert-dismissible fade show mt-3 mb-0 small py-2 d-flex justify-content-between align-items-center" role="alert">
                      <span>{successMessage}</span>
                      <button
                        type="button"
                        className="btn-close py-2"
                        aria-label="Close"
                        onClick={() => setSuccessMessage(null)}
                      />
                    </div>
                  )}
                </div>

                {/* Campaign & Center Overview Card */}
                <div className="card bg-light border-0 p-3">
                  <div className="row g-3">
                    <div className="col-md-6">
                      <span className="text-muted small d-block">Campaign</span>
                      <strong className="text-dark">
                        {campaign ? `[${campaign.code}] ${campaign.name}` : `Campaign #${session.campaignId}`}
                      </strong>
                    </div>
                    <div className="col-md-6">
                      <span className="text-muted small d-block">Hosting Center</span>
                      <strong className="text-dark">
                        {center ? `[${center.code}] ${center.name}` : `Center #${session.centerId}`}
                      </strong>
                    </div>
                  </div>
                </div>

                {/* Schedule Interval Information */}
                <div className="card border p-3">
                  <h6 className="fw-bold text-primary mb-3">Schedule & Timing</h6>
                  <div className="row g-3">
                    <div className="col-md-4">
                      <span className="text-muted small d-block">Schedule Timezone</span>
                      <code>{session.scheduleTimezone}</code>
                    </div>

                    <div className="col-md-4">
                      <span className="text-muted small d-block">Start Time ({tz})</span>
                      <strong className="text-dark">
                        {formatInTimezone(session.startAt, tz)}
                      </strong>
                      <div className="text-muted small font-monospace" style={{ fontSize: '0.75rem' }}>
                        UTC: {session.startAt}
                      </div>
                    </div>

                    <div className="col-md-4">
                      <span className="text-muted small d-block">End Time ({tz})</span>
                      <strong className="text-dark">
                        {formatInTimezone(session.endAt, tz)}
                      </strong>
                      <div className="text-muted small font-monospace" style={{ fontSize: '0.75rem' }}>
                        UTC: {session.endAt}
                      </div>
                    </div>

                    <div className="col-md-12">
                      <span className="text-muted small d-block">Booking Intake Close Time ({tz})</span>
                      <strong className="text-dark">
                        {formatInTimezone(session.bookingCloseAt, tz)}
                      </strong>
                      <div className="text-muted small font-monospace" style={{ fontSize: '0.75rem' }}>
                        UTC: {session.bookingCloseAt}
                      </div>
                    </div>
                  </div>
                </div>

                {/* Provenance & Recurrence */}
                <div className="card border p-3 bg-light">
                  <h6 className="fw-bold text-secondary mb-2">Provenance & Series Metadata</h6>
                  <div className="row g-2 small">
                    <div className="col-md-4">
                      <span className="text-muted d-block">Recurrence Rule</span>
                      <span>
                        {session.recurrenceRuleId ? `Rule #${session.recurrenceRuleId}` : 'One-Off Session (Manual)'}
                      </span>
                    </div>
                    <div className="col-md-4">
                      <span className="text-muted d-block">Occurrence Key</span>
                      <span className="font-monospace text-dark">
                        {session.occurrenceKey || '—'}
                      </span>
                    </div>
                    <div className="col-md-4">
                      <span className="text-muted d-block">Origin Occurrence Date</span>
                      <span>{session.originOccurrenceDate || '—'}</span>
                    </div>
                  </div>
                </div>

                {/* Audit Timestamps */}
                <div className="d-flex justify-content-between text-muted small px-1">
                  <span>
                    Created: <code>{session.createdAt ? new Date(session.createdAt).toLocaleString() : '—'}</code>
                  </span>
                  <span>
                    Updated: <code>{session.updatedAt ? new Date(session.updatedAt).toLocaleString() : '—'}</code>
                  </span>
                </div>
              </div>
            )}

            {!loading && session && activeTab === 'assignments' && (
              <SessionAssignmentsTab session={session} />
            )}

            {!loading && session && activeTab === 'capacity' && (
              <SessionCapacityPlanTab session={session} />
            )}
          </div>

          <div className="modal-footer bg-light">
            <button type="button" className="btn btn-secondary" onClick={onClose}>
              Close
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
