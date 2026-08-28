import React, { useState, useEffect, useRef } from 'react';
import {
  getSessionById,
  formatInTimezone,
  scheduleSession,
  startSession,
  completeSession,
  suspendSessionBooking,
  resumeSessionBooking,
} from '../api/sessionApi';
import SessionAssignmentsTab from './SessionAssignmentsTab';
import SessionCapacityPlanTab from './SessionCapacityPlanTab';
import SessionTimeSlotsTab from './SessionTimeSlotsTab';
import SessionChangeControlTab from './SessionChangeControlTab';
import SessionReadinessTab from './SessionReadinessTab';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import LoadingSpinner from '../../../components/common/LoadingSpinner';
import { useAuth } from '../../../auth/context/AuthContext';
import { canManageSessionBooking } from '../../../auth/utils/roleUtils';

export default function SessionDetailsModal({
  isOpen,
  onClose,
  sessionId,
  campaignsMap = {},
  centersMap = {},
  onSessionUpdated,
}) {
  const { user } = useAuth();
  const canManageBooking = canManageSessionBooking(user);

  const [session, setSession] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState('overview');

  // Lifecycle & Booking Action State
  const [submittingAction, setSubmittingAction] = useState(null);
  const [pendingConfirmAction, setPendingConfirmAction] = useState(null);
  const [bookingReason, setBookingReason] = useState('');
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
    setBookingReason('');
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

  const getBookingStatusBadge = (bookingStatus) => {
    switch (bookingStatus) {
      case 'OPEN':
        return <span className="badge bg-success text-white" title="Booking Intake Open">OPEN</span>;
      case 'FULL':
        return <span className="badge bg-warning text-dark" title="Fully Booked (Waitlist Available)">FULL</span>;
      case 'SUSPENDED':
        return <span className="badge bg-danger text-white" title="Booking Intake Suspended">SUSPENDED</span>;
      case 'NOT_OPEN':
        return <span className="badge bg-info text-dark" title="Booking Window Not Open Yet">NOT_OPEN</span>;
      case 'CLOSED':
      default:
        return <span className="badge bg-secondary text-white" title="Booking Closed">CLOSED</span>;
    }
  };

  const handleInitiateAction = (actionType) => {
    setLifecycleError(null);
    setSuccessMessage(null);
    setBookingReason('');
    setPendingConfirmAction(actionType);
  };

  const handleCancelConfirm = () => {
    setPendingConfirmAction(null);
    setBookingReason('');
    setLifecycleError(null);
  };

  const handleExecuteAction = async (actionType) => {
    if (lifecycleMutexOwnerRef.current !== null) {
      return;
    }

    if (
      (actionType === 'SUSPEND_BOOKING' || actionType === 'RESUME_BOOKING') &&
      !bookingReason.trim()
    ) {
      setLifecycleError({ message: 'Reason is required and cannot be blank.' });
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
      } else if (actionType === 'SUSPEND_BOOKING') {
        updated = await suspendSessionBooking(currentSessionId, { reason: bookingReason.trim() });
      } else if (actionType === 'RESUME_BOOKING') {
        updated = await resumeSessionBooking(currentSessionId, { reason: bookingReason.trim() });
      }

      // 1. Authoritative parent collection reconciliation
      if (updated && onSessionUpdated) {
        onSessionUpdated(updated);
      }

      // 2. Guarded modal-local state update
      if (activeSessionIdRef.current === currentSessionId && lifecycleGenRef.current === reqGen) {
        if (updated) {
          setSession(updated);
          let message = `Session #${updated.id} action executed successfully.`;
          if (actionType === 'SCHEDULE') message = `Session #${updated.id} successfully scheduled.`;
          else if (actionType === 'START') message = `Session #${updated.id} successfully started.`;
          else if (actionType === 'COMPLETE') message = `Session #${updated.id} successfully completed.`;
          else if (actionType === 'SUSPEND_BOOKING') message = `Session #${updated.id} booking intake successfully suspended.`;
          else if (actionType === 'RESUME_BOOKING') message = `Session #${updated.id} booking intake successfully resumed.`;

          setSuccessMessage(message);
        }
        setPendingConfirmAction(null);
        setBookingReason('');
      }
    } catch (err) {
      if (activeSessionIdRef.current === currentSessionId && lifecycleGenRef.current === reqGen) {
        setLifecycleError(err);
      }
    } finally {
      if (activeSessionIdRef.current === currentSessionId && lifecycleGenRef.current === reqGen) {
        setSubmittingAction(null);
      }
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
                <li className="nav-item">
                  <button
                    type="button"
                    className={`nav-link ${activeTab === 'time-slots' ? 'active fw-bold text-primary' : 'text-secondary'}`}
                    onClick={() => setActiveTab('time-slots')}
                  >
                    Time Slots
                  </button>
                </li>
                <li className="nav-item">
                  <button
                    type="button"
                    className={`nav-link ${activeTab === 'change-control' ? 'active fw-bold text-primary' : 'text-secondary'}`}
                    onClick={() => setActiveTab('change-control')}
                  >
                    Change Control
                  </button>
                </li>
                <li className="nav-item">
                  <button
                    type="button"
                    className={`nav-link ${activeTab === 'readiness' ? 'active fw-bold text-primary' : 'text-secondary'}`}
                    onClick={() => setActiveTab('readiness')}
                  >
                    Readiness & Diagnostics
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
                {/* Session Lifecycle Management & Booking Action Card */}
                <div className="card border p-3 bg-light">
                  <div className="d-flex flex-wrap align-items-center justify-content-between gap-3">
                    <div>
                      <span className="text-muted small d-block">Lifecycle & Booking Intake Status</span>
                      <div className="d-flex flex-wrap align-items-center gap-2 mt-1">
                        {getStatusBadge(session.lifecycleStatus)}
                        {session.bookingStatus && (
                          <div className="d-inline-flex align-items-center gap-1">
                            <span className="small text-muted">Booking:</span>
                            {getBookingStatusBadge(session.bookingStatus)}
                          </div>
                        )}
                        {session.bookingSuspended && (
                          <span className="badge bg-danger text-white">MANUALLY SUSPENDED</span>
                        )}
                      </div>
                    </div>

                    {/* Status-dependent Action Buttons */}
                    <div className="d-flex flex-wrap align-items-center gap-2">
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
                        <>
                          <button
                            type="button"
                            className="btn btn-sm btn-success fw-semibold text-white"
                            onClick={() => handleInitiateAction('START')}
                            disabled={submittingAction !== null}
                          >
                            Start Session
                          </button>

                          {/* Suspend / Resume Booking Controls for ADMIN and CENTER_MANAGER */}
                          {canManageBooking && !session.bookingSuspended && (
                            <button
                              type="button"
                              className="btn btn-sm btn-outline-danger fw-semibold"
                              onClick={() => handleInitiateAction('SUSPEND_BOOKING')}
                              disabled={submittingAction !== null}
                              title="Authoritatively suspend citizen booking intake"
                            >
                              Suspend Booking
                            </button>
                          )}

                          {canManageBooking && session.bookingSuspended && (
                            <button
                              type="button"
                              className="btn btn-sm btn-outline-success fw-semibold"
                              onClick={() => handleInitiateAction('RESUME_BOOKING')}
                              disabled={submittingAction !== null}
                              title="Authoritatively resume citizen booking intake"
                            >
                              Resume Booking
                            </button>
                          )}
                        </>
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
                        {pendingConfirmAction === 'SUSPEND_BOOKING' && 'Confirm Booking Intake Suspension'}
                        {pendingConfirmAction === 'RESUME_BOOKING' && 'Confirm Booking Intake Resumption'}
                      </h6>
                      <p className="small text-secondary mb-3">
                        {pendingConfirmAction === 'SCHEDULE' &&
                          'Transitioning this session to SCHEDULED will validate operational readiness and lock draft assignments. The backend requires at least one active station, at least one active vaccinator, and an approved, non-stale capacity plan with positive capacity.'}
                        {pendingConfirmAction === 'START' &&
                          'Transitioning this session to IN_PROGRESS begins active session operations. The backend requires the current time to have reached or passed the scheduled session start time.'}
                        {pendingConfirmAction === 'COMPLETE' &&
                          'Transitioning this session to COMPLETED concludes all vaccination activities. This is a terminal lifecycle state that cannot be undone.'}
                        {pendingConfirmAction === 'SUSPEND_BOOKING' &&
                          'Authoritatively pauses new citizen booking intake for this scheduled session. Existing confirmed appointments and waitlists remain preserved.'}
                        {pendingConfirmAction === 'RESUME_BOOKING' &&
                          'Authoritatively resumes citizen booking intake for this scheduled session.'}
                      </p>

                      {/* Reason input for booking suspension/resumption */}
                      {(pendingConfirmAction === 'SUSPEND_BOOKING' || pendingConfirmAction === 'RESUME_BOOKING') && (
                        <div className="mb-3">
                          <label htmlFor="bookingActionReason" className="form-label small fw-semibold text-dark mb-1">
                            Reason <span className="text-danger">*</span>
                          </label>
                          <textarea
                            id="bookingActionReason"
                            className="form-control form-control-sm"
                            rows={3}
                            maxLength={500}
                            placeholder={
                              pendingConfirmAction === 'SUSPEND_BOOKING'
                                ? 'State the operational reason for suspending booking intake (e.g. temporary staff shortage)...'
                                : 'State the reason for resuming booking intake...'
                            }
                            value={bookingReason}
                            onChange={(e) => setBookingReason(e.target.value)}
                            disabled={submittingAction !== null}
                          />
                          <div className="d-flex justify-content-between form-text text-muted small mt-1">
                            <span>Mandatory reason (max 500 characters).</span>
                            <span>{bookingReason.length} / 500</span>
                          </div>
                        </div>
                      )}

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
                              : pendingConfirmAction === 'SUSPEND_BOOKING'
                              ? 'btn-danger text-white'
                              : pendingConfirmAction === 'RESUME_BOOKING'
                              ? 'btn-success text-white'
                              : 'btn-dark'
                          }`}
                          onClick={() => handleExecuteAction(pendingConfirmAction)}
                          disabled={
                            submittingAction !== null ||
                            ((pendingConfirmAction === 'SUSPEND_BOOKING' || pendingConfirmAction === 'RESUME_BOOKING') &&
                              !bookingReason.trim())
                          }
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
                                : pendingConfirmAction === 'COMPLETE'
                                ? 'Complete'
                                : pendingConfirmAction === 'SUSPEND_BOOKING'
                                ? 'Suspend'
                                : 'Resume'
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

            {!loading && session && activeTab === 'time-slots' && (
              <SessionTimeSlotsTab session={session} />
            )}

            {!loading && session && activeTab === 'change-control' && (
              <SessionChangeControlTab
                session={session}
                campaignsMap={campaignsMap}
                centersMap={centersMap}
                onSessionUpdated={(updatedSession) => {
                  if (onSessionUpdated) {
                    onSessionUpdated(updatedSession);
                  }
                  if (activeSessionIdRef.current === updatedSession.id) {
                    setSession(updatedSession);
                  }
                }}
              />
            )}

            {!loading && session && activeTab === 'readiness' && (
              <SessionReadinessTab
                session={session}
                onNavigateToTab={(tabKey) => setActiveTab(tabKey)}
              />
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
