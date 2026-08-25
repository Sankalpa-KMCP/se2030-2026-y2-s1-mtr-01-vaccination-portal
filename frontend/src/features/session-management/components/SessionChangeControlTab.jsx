import React, { useState, useEffect, useCallback, useRef } from 'react';
import {
  previewSessionCancellation,
  cancelSession,
  previewSessionReschedule,
  rescheduleSession,
  previewSessionTransfer,
  transferSession,
  listSessionChangeEvents,
  formatInTimezone,
  localToUtcIso,
  utcToLocalDateAndTime,
} from '../api/sessionApi';
import { getCenterAssignments } from '../../campaign-management/api/campaignConfigurationApi';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import LoadingSpinner from '../../../components/common/LoadingSpinner';

/**
 * Session Change Control tab component (F4C).
 * Provides preview-first workflows for session cancellation, same-center rescheduling,
 * and center transfer, alongside an append-only audit event history table.
 *
 * @param {Object} props
 * @param {Object} props.session - Current session object
 * @param {Object} [props.campaignsMap={}] - Map of campaign IDs to campaign summaries
 * @param {Object} [props.centersMap={}] - Map of center IDs to center summaries
 * @param {Function} [props.onSessionUpdated] - Callback to notify parent collection on successful change
 */
export default function SessionChangeControlTab({
  session,
  campaignsMap = {},
  centersMap = {},
  onSessionUpdated,
}) {
  // Action selection: 'NONE' | 'CANCEL' | 'RESCHEDULE' | 'TRANSFER'
  const [activeAction, setActiveAction] = useState('NONE');

  // Change Event Audit History State
  const [history, setHistory] = useState([]);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [historyError, setHistoryError] = useState(null);

  // Form State: Reschedule
  const [rescheduleDate, setRescheduleDate] = useState('');
  const [rescheduleStartTime, setRescheduleStartTime] = useState('');
  const [rescheduleEndTime, setRescheduleEndTime] = useState('');
  const [rescheduleBookingCloseMode, setRescheduleBookingCloseMode] = useState('SAME_AS_START');
  const [rescheduleBookingCloseDate, setRescheduleBookingCloseDate] = useState('');
  const [rescheduleBookingCloseTime, setRescheduleBookingCloseTime] = useState('');

  // Form State: Transfer
  const [transferCenterId, setTransferCenterId] = useState('');
  const [transferDate, setTransferDate] = useState('');
  const [transferStartTime, setTransferStartTime] = useState('');
  const [transferEndTime, setTransferEndTime] = useState('');
  const [transferBookingCloseMode, setTransferBookingCloseMode] = useState('SAME_AS_START');
  const [transferBookingCloseDate, setTransferBookingCloseDate] = useState('');
  const [transferBookingCloseTime, setTransferBookingCloseTime] = useState('');
  const [campaignCenters, setCampaignCenters] = useState([]);
  const [loadingCampaignCenters, setLoadingCampaignCenters] = useState(false);

  // Preview State
  const [previewResult, setPreviewResult] = useState(null);
  const [loadingPreview, setLoadingPreview] = useState(false);
  const [previewError, setPreviewError] = useState(null);
  const previewSnapshotRef = useRef(null);

  // Confirmation State
  const [reason, setReason] = useState('');
  const [submittingConfirm, setSubmittingConfirm] = useState(false);
  const [confirmError, setConfirmError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Mutex & Concurrency Tokens
  const actionMutexOwnerRef = useRef(null);
  const requestSeqRef = useRef(0);
  const sessionGenRef = useRef(0);
  const activeSessionIdRef = useRef(session?.id);
  const historyReqSeqRef = useRef(0);
  const confirmHeadingRef = useRef(null);

  const tz = session?.scheduleTimezone || 'UTC';
  const isDraft = session?.lifecycleStatus === 'DRAFT';
  const isScheduled = session?.lifecycleStatus === 'SCHEDULED';
  const isMutationAllowed = isDraft || isScheduled;

  // Initialize and load audit history on session change
  const fetchHistory = useCallback(() => {
    if (!session?.id) return;
    const currentSessionId = session.id;
    const currentGen = sessionGenRef.current;
    const reqToken = ++historyReqSeqRef.current;

    setLoadingHistory(true);
    setHistoryError(null);

    listSessionChangeEvents(currentSessionId)
      .then((data) => {
        if (
          activeSessionIdRef.current === currentSessionId &&
          sessionGenRef.current === currentGen &&
          historyReqSeqRef.current === reqToken
        ) {
          setHistory(data || []);
          setLoadingHistory(false);
        }
      })
      .catch((err) => {
        if (
          activeSessionIdRef.current === currentSessionId &&
          sessionGenRef.current === currentGen &&
          historyReqSeqRef.current === reqToken
        ) {
          setHistoryError(err);
          setLoadingHistory(false);
        }
      });
  }, [session?.id]);

  useEffect(() => {
    activeSessionIdRef.current = session?.id;
    sessionGenRef.current += 1;
    actionMutexOwnerRef.current = null;
    setActiveAction('NONE');
    setPreviewResult(null);
    previewSnapshotRef.current = null;
    setPreviewError(null);
    setConfirmError(null);
    setSuccessMessage(null);
    setReason('');
    setLoadingPreview(false);
    setSubmittingConfirm(false);

    fetchHistory();
  }, [session?.id, fetchHistory]);

  // Load campaign center assignments for transfer options
  useEffect(() => {
    if (!session?.campaignId || activeAction !== 'TRANSFER') {
      return;
    }

    let isMounted = true;
    setLoadingCampaignCenters(true);

    getCenterAssignments(session.campaignId)
      .then((data) => {
        if (!isMounted) return;
        const validAssignments = (data || []).filter(
          (a) => a.status === 'ACTIVE' || a.status === 'FUTURE'
        );
        setCampaignCenters(validAssignments);
        setLoadingCampaignCenters(false);
      })
      .catch(() => {
        if (!isMounted) return;
        // Fallback to centers list from centersMap if specific campaign assignments query fails
        const fallback = Object.values(centersMap).map((c) => ({
          centerId: c.id,
          centerName: c.name,
          centerCode: c.code,
        }));
        setCampaignCenters(fallback);
        setLoadingCampaignCenters(false);
      });

    return () => {
      isMounted = false;
    };
  }, [session?.campaignId, activeAction, centersMap]);

  // Handle switching active change action
  const handleSelectAction = (actionType) => {
    if (actionMutexOwnerRef.current !== null) {
      return; // Mutex locked by in-flight operation
    }

    setActiveAction(actionType);
    setPreviewResult(null);
    previewSnapshotRef.current = null;
    setPreviewError(null);
    setConfirmError(null);
    setSuccessMessage(null);
    setReason('');

    if (actionType === 'RESCHEDULE') {
      const startParsed = utcToLocalDateAndTime(session.startAt, tz);
      const endParsed = utcToLocalDateAndTime(session.endAt, tz);
      const bookingParsed = utcToLocalDateAndTime(session.bookingCloseAt, tz);

      setRescheduleDate(startParsed.date);
      setRescheduleStartTime(startParsed.time);
      setRescheduleEndTime(endParsed.time);

      if (session.bookingCloseAt && session.bookingCloseAt !== session.startAt) {
        setRescheduleBookingCloseMode('CUSTOM');
        setRescheduleBookingCloseDate(bookingParsed.date);
        setRescheduleBookingCloseTime(bookingParsed.time);
      } else {
        setRescheduleBookingCloseMode('SAME_AS_START');
        setRescheduleBookingCloseDate('');
        setRescheduleBookingCloseTime('');
      }
    } else if (actionType === 'TRANSFER') {
      const startParsed = utcToLocalDateAndTime(session.startAt, tz);
      const endParsed = utcToLocalDateAndTime(session.endAt, tz);
      const bookingParsed = utcToLocalDateAndTime(session.bookingCloseAt, tz);

      setTransferCenterId('');
      setTransferDate(startParsed.date);
      setTransferStartTime(startParsed.time);
      setTransferEndTime(endParsed.time);

      if (session.bookingCloseAt && session.bookingCloseAt !== session.startAt) {
        setTransferBookingCloseMode('CUSTOM');
        setTransferBookingCloseDate(bookingParsed.date);
        setTransferBookingCloseTime(bookingParsed.time);
      } else {
        setTransferBookingCloseMode('SAME_AS_START');
        setTransferBookingCloseDate('');
        setTransferBookingCloseTime('');
      }
    }
  };

  // Invalidate preview whenever form fields are edited
  const handleFieldChange = (setter) => (e) => {
    setter(e.target.value);
    if (previewResult !== null) {
      setPreviewResult(null);
      previewSnapshotRef.current = null;
    }
    setConfirmError(null);
  };

  // Request authoritative change preview from backend
  const handleExecutePreview = async () => {
    if (actionMutexOwnerRef.current !== null) {
      return;
    }

    setPreviewError(null);
    setConfirmError(null);
    setSuccessMessage(null);

    const currentSessionId = session.id;
    const currentGen = sessionGenRef.current;
    const reqToken = ++requestSeqRef.current;
    actionMutexOwnerRef.current = reqToken;
    setLoadingPreview(true);

    try {
      let previewData = null;
      let snapshotMeta = null;

      if (activeAction === 'CANCEL') {
        previewData = await previewSessionCancellation(currentSessionId);
        snapshotMeta = {
          action: 'CANCEL',
          sessionId: currentSessionId,
        };
      } else if (activeAction === 'RESCHEDULE') {
        if (!rescheduleDate || !rescheduleStartTime || !rescheduleEndTime) {
          throw new Error('Reschedule date, start time, and end time are required.');
        }
        if (rescheduleStartTime >= rescheduleEndTime) {
          throw new Error('Reschedule start time must be strictly before end time.');
        }

        const startAtIso = localToUtcIso(rescheduleDate, rescheduleStartTime, tz);
        const endAtIso = localToUtcIso(rescheduleDate, rescheduleEndTime, tz);
        let bookingCloseAtIso = null;

        if (rescheduleBookingCloseMode === 'SAME_AS_START') {
          bookingCloseAtIso = startAtIso;
        } else {
          if (!rescheduleBookingCloseDate || !rescheduleBookingCloseTime) {
            throw new Error('Custom booking close date and time are required.');
          }
          bookingCloseAtIso = localToUtcIso(
            rescheduleBookingCloseDate,
            rescheduleBookingCloseTime,
            tz
          );
          if (bookingCloseAtIso > startAtIso) {
            throw new Error('Booking close time cannot be after session start time.');
          }
        }

        const payload = {
          newStartAt: startAtIso,
          newEndAt: endAtIso,
          newBookingCloseAt: bookingCloseAtIso,
        };

        previewData = await previewSessionReschedule(currentSessionId, payload);
        snapshotMeta = {
          action: 'RESCHEDULE',
          sessionId: currentSessionId,
          payload,
        };
      } else if (activeAction === 'TRANSFER') {
        if (!transferCenterId) {
          throw new Error('Target vaccination center is required for center transfer.');
        }
        if (!transferDate || !transferStartTime || !transferEndTime) {
          throw new Error('Transfer date, start time, and end time are required.');
        }
        if (transferStartTime >= transferEndTime) {
          throw new Error('Transfer start time must be strictly before end time.');
        }

        const targetCenter = centersMap[transferCenterId];
        const targetTz = targetCenter?.timezone || 'UTC';

        const startAtIso = localToUtcIso(transferDate, transferStartTime, targetTz);
        const endAtIso = localToUtcIso(transferDate, transferEndTime, targetTz);
        let bookingCloseAtIso = null;

        if (transferBookingCloseMode === 'SAME_AS_START') {
          bookingCloseAtIso = startAtIso;
        } else {
          if (!transferBookingCloseDate || !transferBookingCloseTime) {
            throw new Error('Custom booking close date and time are required.');
          }
          bookingCloseAtIso = localToUtcIso(
            transferBookingCloseDate,
            transferBookingCloseTime,
            targetTz
          );
          if (bookingCloseAtIso > startAtIso) {
            throw new Error('Booking close time cannot be after session start time.');
          }
        }

        const payload = {
          newCenterId: Number(transferCenterId),
          newStartAt: startAtIso,
          newEndAt: endAtIso,
          newBookingCloseAt: bookingCloseAtIso,
        };

        previewData = await previewSessionTransfer(currentSessionId, payload);
        snapshotMeta = {
          action: 'TRANSFER',
          sessionId: currentSessionId,
          payload,
        };
      }

      if (
        activeSessionIdRef.current === currentSessionId &&
        sessionGenRef.current === currentGen
      ) {
        setPreviewResult(previewData);
        previewSnapshotRef.current = snapshotMeta;
        setReason('');
      }
    } catch (err) {
      if (
        activeSessionIdRef.current === currentSessionId &&
        sessionGenRef.current === currentGen
      ) {
        setPreviewError(err);
        setPreviewResult(null);
        previewSnapshotRef.current = null;
      }
    } finally {
      if (
        activeSessionIdRef.current === currentSessionId &&
        sessionGenRef.current === currentGen
      ) {
        setLoadingPreview(false);
      }
      if (actionMutexOwnerRef.current === reqToken) {
        actionMutexOwnerRef.current = null;
      }
    }
  };

  // Confirm and persist the session change
  const handleConfirmChange = async () => {
    if (actionMutexOwnerRef.current !== null) {
      return;
    }

    if (!previewResult || !previewResult.previewToken) {
      setConfirmError(new Error('Please generate an authoritative preview before confirming.'));
      return;
    }

    const trimmedReason = reason.trim();
    if (!trimmedReason) {
      setConfirmError(new Error('Change reason is required.'));
      return;
    }
    if (trimmedReason.length > 500) {
      setConfirmError(new Error('Change reason cannot exceed 500 characters.'));
      return;
    }

    const currentSessionId = session.id;
    const currentGen = sessionGenRef.current;
    const reqToken = ++requestSeqRef.current;
    actionMutexOwnerRef.current = reqToken;
    setSubmittingConfirm(true);
    setConfirmError(null);

    try {
      let updated = null;

      if (activeAction === 'CANCEL') {
        const payload = {
          previewToken: previewResult.previewToken,
          reason: trimmedReason,
        };
        updated = await cancelSession(currentSessionId, payload);
      } else if (activeAction === 'RESCHEDULE') {
        const snapshot = previewSnapshotRef.current?.payload;
        if (!snapshot) {
          throw new Error('Reschedule parameters became stale. Please preview the change again.');
        }
        const payload = {
          newStartAt: snapshot.newStartAt,
          newEndAt: snapshot.newEndAt,
          newBookingCloseAt: snapshot.newBookingCloseAt,
          previewToken: previewResult.previewToken,
          reason: trimmedReason,
        };
        updated = await rescheduleSession(currentSessionId, payload);
      } else if (activeAction === 'TRANSFER') {
        const snapshot = previewSnapshotRef.current?.payload;
        if (!snapshot) {
          throw new Error('Transfer parameters became stale. Please preview the change again.');
        }
        const payload = {
          newCenterId: snapshot.newCenterId,
          newStartAt: snapshot.newStartAt,
          newEndAt: snapshot.newEndAt,
          newBookingCloseAt: snapshot.newBookingCloseAt,
          previewToken: previewResult.previewToken,
          reason: trimmedReason,
        };
        updated = await transferSession(currentSessionId, payload);
      }

      // 1. Authoritative parent collection reconciliation (ALWAYS triggered unconditionally)
      if (updated && onSessionUpdated) {
        onSessionUpdated(updated);
      }

      // 2. Guarded modal-local state update (ONLY if modal still views target session)
      if (
        activeSessionIdRef.current === currentSessionId &&
        sessionGenRef.current === currentGen
      ) {
        const actionLabel =
          activeAction === 'CANCEL'
            ? 'cancelled'
            : activeAction === 'RESCHEDULE'
            ? 'rescheduled'
            : 'transferred';
        setSuccessMessage(`Session #${updated.id} successfully ${actionLabel}.`);
        setActiveAction('NONE');
        setPreviewResult(null);
        previewSnapshotRef.current = null;
        setReason('');
        fetchHistory();
      }
    } catch (err) {
      if (
        activeSessionIdRef.current === currentSessionId &&
        sessionGenRef.current === currentGen
      ) {
        setConfirmError(err);
      }
    } finally {
      if (
        activeSessionIdRef.current === currentSessionId &&
        sessionGenRef.current === currentGen
      ) {
        setSubmittingConfirm(false);
      }
      if (actionMutexOwnerRef.current === reqToken) {
        actionMutexOwnerRef.current = null;
      }
    }
  };

  const getChangeTypeBadge = (changeType) => {
    switch (changeType) {
      case 'CANCELLED':
        return <span className="badge bg-danger text-white">CANCELLED</span>;
      case 'RESCHEDULED':
        return <span className="badge bg-primary text-white">RESCHEDULED</span>;
      case 'CENTER_TRANSFERRED':
        return <span className="badge bg-info text-dark">TRANSFERRED</span>;
      default:
        return <span className="badge bg-secondary text-white">{changeType}</span>;
    }
  };

  const campaign = session ? campaignsMap[session.campaignId] : null;

  return (
    <div className="d-flex flex-column gap-4">
      {/* Global Success Notification */}
      {successMessage && (
        <div
          className="alert alert-success alert-dismissible fade show border-0 shadow-sm mb-0"
          role="status"
        >
          <strong>Success:</strong> {successMessage}
          <button
            type="button"
            className="btn-close"
            aria-label="Close"
            onClick={() => setSuccessMessage(null)}
          />
        </div>
      )}

      {/* Mutation Availability Check & Action Selector */}
      {isMutationAllowed ? (
        <div className="card border p-3 bg-light">
          <div className="d-flex flex-wrap align-items-center justify-content-between gap-3">
            <div>
              <div className="d-flex align-items-center gap-2 mb-1">
                <h6 className="fw-bold text-dark mb-0">Session Change Operations</h6>
                {campaign && (
                  <span className="badge bg-light text-dark border small">
                    {campaign.code ? `[${campaign.code}] ` : ''}{campaign.name}
                  </span>
                )}
              </div>
              <p className="text-secondary small mb-0">
                Preview impact and apply change control mutations for this{' '}
                <span className="badge bg-secondary">{session.lifecycleStatus}</span> session.
              </p>
            </div>

            <div className="btn-group" role="group" aria-label="Change control actions">
              <button
                type="button"
                className={`btn btn-sm ${
                  activeAction === 'CANCEL' ? 'btn-danger text-white' : 'btn-outline-danger'
                }`}
                onClick={() => handleSelectAction(activeAction === 'CANCEL' ? 'NONE' : 'CANCEL')}
                disabled={actionMutexOwnerRef.current !== null}
              >
                Cancel Session
              </button>
              <button
                type="button"
                className={`btn btn-sm ${
                  activeAction === 'RESCHEDULE' ? 'btn-primary' : 'btn-outline-primary'
                }`}
                onClick={() =>
                  handleSelectAction(activeAction === 'RESCHEDULE' ? 'NONE' : 'RESCHEDULE')
                }
                disabled={actionMutexOwnerRef.current !== null}
              >
                Reschedule
              </button>
              <button
                type="button"
                className={`btn btn-sm ${
                  activeAction === 'TRANSFER' ? 'btn-dark' : 'btn-outline-dark'
                }`}
                onClick={() =>
                  handleSelectAction(activeAction === 'TRANSFER' ? 'NONE' : 'TRANSFER')
                }
                disabled={actionMutexOwnerRef.current !== null}
              >
                Transfer Center
              </button>
            </div>
          </div>

          {/* Active Action Form Card */}
          {activeAction !== 'NONE' && (
            <div
              className="mt-3 p-3 rounded border bg-white shadow-sm"
              role="region"
              aria-labelledby="activeActionHeading"
            >
              <div className="d-flex justify-content-between align-items-center mb-3">
                <h6 id="activeActionHeading" className="fw-bold text-dark mb-0">
                  {activeAction === 'CANCEL' && 'Session Cancellation Workflow'}
                  {activeAction === 'RESCHEDULE' && 'Session Reschedule Workflow'}
                  {activeAction === 'TRANSFER' && 'Session Center Transfer Workflow'}
                </h6>
                <button
                  type="button"
                  className="btn btn-sm btn-outline-secondary"
                  onClick={() => handleSelectAction('NONE')}
                  disabled={actionMutexOwnerRef.current !== null}
                >
                  Close Action
                </button>
              </div>

              {/* Cancellation Form */}
              {activeAction === 'CANCEL' && (
                <div className="mb-3">
                  <div className="alert alert-warning py-2 small mb-3">
                    <strong>Warning:</strong> Cancelling this session will transition it to the
                    terminal <span className="badge bg-danger">CANCELLED</span> status.
                    {isScheduled && (
                      <span className="d-block mt-1">
                        Authoritative appointment and waitlist impact will be assessed via the
                        impact reader.
                      </span>
                    )}
                  </div>

                  {!previewResult && (
                    <div className="d-flex justify-content-start">
                      <button
                        type="button"
                        className="btn btn-sm btn-danger fw-semibold"
                        onClick={handleExecutePreview}
                        disabled={loadingPreview || actionMutexOwnerRef.current !== null}
                      >
                        {loadingPreview ? (
                          <>
                            <span
                              className="spinner-border spinner-border-sm me-2"
                              role="status"
                              aria-hidden="true"
                            />
                            Calculating Cancellation Impact...
                          </>
                        ) : (
                          'Preview Cancellation Impact'
                        )}
                      </button>
                    </div>
                  )}
                </div>
              )}

              {/* Reschedule Form */}
              {activeAction === 'RESCHEDULE' && (
                <div className="mb-3">
                  <p className="text-secondary small mb-3">
                    Proposed schedule will be evaluated in the session's timezone:{' '}
                    <code>{tz}</code>.
                  </p>

                  <div className="row g-3">
                    <div className="col-md-4">
                      <label htmlFor="rescheduleDate" className="form-label small fw-semibold">
                        New Session Date <span className="text-danger">*</span>
                      </label>
                      <input
                        type="date"
                        id="rescheduleDate"
                        className="form-control form-control-sm"
                        value={rescheduleDate}
                        onChange={handleFieldChange(setRescheduleDate)}
                        disabled={loadingPreview || submittingConfirm}
                        required
                      />
                    </div>

                    <div className="col-md-4">
                      <label htmlFor="rescheduleStartTime" className="form-label small fw-semibold">
                        New Start Time ({tz}) <span className="text-danger">*</span>
                      </label>
                      <input
                        type="time"
                        id="rescheduleStartTime"
                        className="form-control form-control-sm"
                        value={rescheduleStartTime}
                        onChange={handleFieldChange(setRescheduleStartTime)}
                        disabled={loadingPreview || submittingConfirm}
                        required
                      />
                    </div>

                    <div className="col-md-4">
                      <label htmlFor="rescheduleEndTime" className="form-label small fw-semibold">
                        New End Time ({tz}) <span className="text-danger">*</span>
                      </label>
                      <input
                        type="time"
                        id="rescheduleEndTime"
                        className="form-control form-control-sm"
                        value={rescheduleEndTime}
                        onChange={handleFieldChange(setRescheduleEndTime)}
                        disabled={loadingPreview || submittingConfirm}
                        required
                      />
                    </div>

                    {/* Booking Intake Close Mode */}
                    <div className="col-md-12">
                      <label className="form-label small fw-semibold d-block mb-1">
                        Booking Intake Close Time
                      </label>
                      <div className="d-flex align-items-center gap-3 mb-2">
                        <div className="form-check">
                          <input
                            className="form-check-input"
                            type="radio"
                            name="rescheduleBookingCloseMode"
                            id="rescheduleBookingCloseSame"
                            value="SAME_AS_START"
                            checked={rescheduleBookingCloseMode === 'SAME_AS_START'}
                            onChange={handleFieldChange(setRescheduleBookingCloseMode)}
                            disabled={loadingPreview || submittingConfirm}
                          />
                          <label
                            className="form-check-label small"
                            htmlFor="rescheduleBookingCloseSame"
                          >
                            Same as Session Start
                          </label>
                        </div>
                        <div className="form-check">
                          <input
                            className="form-check-input"
                            type="radio"
                            name="rescheduleBookingCloseMode"
                            id="rescheduleBookingCloseCustom"
                            value="CUSTOM"
                            checked={rescheduleBookingCloseMode === 'CUSTOM'}
                            onChange={handleFieldChange(setRescheduleBookingCloseMode)}
                            disabled={loadingPreview || submittingConfirm}
                          />
                          <label
                            className="form-check-label small"
                            htmlFor="rescheduleBookingCloseCustom"
                          >
                            Custom Close Time
                          </label>
                        </div>
                      </div>

                      {rescheduleBookingCloseMode === 'CUSTOM' && (
                        <div className="row g-2 p-2 bg-light rounded border">
                          <div className="col-md-6">
                            <label
                              htmlFor="rescheduleBookingCloseDate"
                              className="form-label small text-muted"
                            >
                              Booking Close Date
                            </label>
                            <input
                              type="date"
                              id="rescheduleBookingCloseDate"
                              className="form-control form-control-sm"
                              value={rescheduleBookingCloseDate}
                              onChange={handleFieldChange(setRescheduleBookingCloseDate)}
                              disabled={loadingPreview || submittingConfirm}
                            />
                          </div>
                          <div className="col-md-6">
                            <label
                              htmlFor="rescheduleBookingCloseTime"
                              className="form-label small text-muted"
                            >
                              Booking Close Time ({tz})
                            </label>
                            <input
                              type="time"
                              id="rescheduleBookingCloseTime"
                              className="form-control form-control-sm"
                              value={rescheduleBookingCloseTime}
                              onChange={handleFieldChange(setRescheduleBookingCloseTime)}
                              disabled={loadingPreview || submittingConfirm}
                            />
                          </div>
                        </div>
                      )}
                    </div>
                  </div>

                  {!previewResult && (
                    <div className="d-flex justify-content-start mt-3">
                      <button
                        type="button"
                        className="btn btn-sm btn-primary fw-semibold"
                        onClick={handleExecutePreview}
                        disabled={loadingPreview || actionMutexOwnerRef.current !== null}
                      >
                        {loadingPreview ? (
                          <>
                            <span
                              className="spinner-border spinner-border-sm me-2"
                              role="status"
                              aria-hidden="true"
                            />
                            Calculating Reschedule Preview...
                          </>
                        ) : (
                          'Preview Reschedule Impact'
                        )}
                      </button>
                    </div>
                  )}
                </div>
              )}

              {/* Transfer Form */}
              {activeAction === 'TRANSFER' && (
                <div className="mb-3">
                  <p className="text-secondary small mb-3">
                    Select a target vaccination center authorized for this campaign. The schedule
                    will be evaluated against the target center&apos;s operating hours and timezone.
                  </p>

                  <div className="row g-3">
                    <div className="col-md-12">
                      <label htmlFor="transferCenterId" className="form-label small fw-semibold">
                        Target Vaccination Center <span className="text-danger">*</span>
                      </label>
                      <select
                        id="transferCenterId"
                        className="form-select form-select-sm"
                        value={transferCenterId}
                        onChange={handleFieldChange(setTransferCenterId)}
                        disabled={
                          loadingCampaignCenters || loadingPreview || submittingConfirm
                        }
                        required
                      >
                        <option value="">-- Select Target Vaccination Center --</option>
                        {campaignCenters.map((ca) => {
                          const cId = ca.centerId || ca.id;
                          const cObj = centersMap[cId];
                          const cName = cObj?.name || ca.centerName || `Center #${cId}`;
                          const cCode = cObj?.code || ca.centerCode || '';
                          const cTz = cObj?.timezone || 'UTC';
                          const isCurrent = Number(cId) === Number(session.centerId);
                          return (
                            <option key={cId} value={cId}>
                              {cCode ? `[${cCode}] ` : ''}
                              {cName} ({cTz}) {isCurrent ? '— (Current Center)' : ''}
                            </option>
                          );
                        })}
                      </select>
                      {loadingCampaignCenters && (
                        <div className="text-muted small mt-1">Loading authorized centers...</div>
                      )}
                    </div>

                    {transferCenterId && (
                      <>
                        <div className="col-md-4">
                          <label htmlFor="transferDate" className="form-label small fw-semibold">
                            Session Date <span className="text-danger">*</span>
                          </label>
                          <input
                            type="date"
                            id="transferDate"
                            className="form-control form-control-sm"
                            value={transferDate}
                            onChange={handleFieldChange(setTransferDate)}
                            disabled={loadingPreview || submittingConfirm}
                            required
                          />
                        </div>

                        <div className="col-md-4">
                          <label
                            htmlFor="transferStartTime"
                            className="form-label small fw-semibold"
                          >
                            Start Time ({centersMap[transferCenterId]?.timezone || 'UTC'}){' '}
                            <span className="text-danger">*</span>
                          </label>
                          <input
                            type="time"
                            id="transferStartTime"
                            className="form-control form-control-sm"
                            value={transferStartTime}
                            onChange={handleFieldChange(setTransferStartTime)}
                            disabled={loadingPreview || submittingConfirm}
                            required
                          />
                        </div>

                        <div className="col-md-4">
                          <label htmlFor="transferEndTime" className="form-label small fw-semibold">
                            End Time ({centersMap[transferCenterId]?.timezone || 'UTC'}){' '}
                            <span className="text-danger">*</span>
                          </label>
                          <input
                            type="time"
                            id="transferEndTime"
                            className="form-control form-control-sm"
                            value={transferEndTime}
                            onChange={handleFieldChange(setTransferEndTime)}
                            disabled={loadingPreview || submittingConfirm}
                            required
                          />
                        </div>

                        {/* Booking Intake Close Mode */}
                        <div className="col-md-12">
                          <label className="form-label small fw-semibold d-block mb-1">
                            Booking Intake Close Time
                          </label>
                          <div className="d-flex align-items-center gap-3 mb-2">
                            <div className="form-check">
                              <input
                                className="form-check-input"
                                type="radio"
                                name="transferBookingCloseMode"
                                id="transferBookingCloseSame"
                                value="SAME_AS_START"
                                checked={transferBookingCloseMode === 'SAME_AS_START'}
                                onChange={handleFieldChange(setTransferBookingCloseMode)}
                                disabled={loadingPreview || submittingConfirm}
                              />
                              <label
                                className="form-check-label small"
                                htmlFor="transferBookingCloseSame"
                              >
                                Same as Session Start
                              </label>
                            </div>
                            <div className="form-check">
                              <input
                                className="form-check-input"
                                type="radio"
                                name="transferBookingCloseMode"
                                id="transferBookingCloseCustom"
                                value="CUSTOM"
                                checked={transferBookingCloseMode === 'CUSTOM'}
                                onChange={handleFieldChange(setTransferBookingCloseMode)}
                                disabled={loadingPreview || submittingConfirm}
                              />
                              <label
                                className="form-check-label small"
                                htmlFor="transferBookingCloseCustom"
                              >
                                Custom Close Time
                              </label>
                            </div>
                          </div>

                          {transferBookingCloseMode === 'CUSTOM' && (
                            <div className="row g-2 p-2 bg-light rounded border">
                              <div className="col-md-6">
                                <label
                                  htmlFor="transferBookingCloseDate"
                                  className="form-label small text-muted"
                                >
                                  Booking Close Date
                                </label>
                                <input
                                  type="date"
                                  id="transferBookingCloseDate"
                                  className="form-control form-control-sm"
                                  value={transferBookingCloseDate}
                                  onChange={handleFieldChange(setTransferBookingCloseDate)}
                                  disabled={loadingPreview || submittingConfirm}
                                />
                              </div>
                              <div className="col-md-6">
                                <label
                                  htmlFor="transferBookingCloseTime"
                                  className="form-label small text-muted"
                                >
                                  Booking Close Time (
                                  {centersMap[transferCenterId]?.timezone || 'UTC'})
                                </label>
                                <input
                                  type="time"
                                  id="transferBookingCloseTime"
                                  className="form-control form-control-sm"
                                  value={transferBookingCloseTime}
                                  onChange={handleFieldChange(setTransferBookingCloseTime)}
                                  disabled={loadingPreview || submittingConfirm}
                                />
                              </div>
                            </div>
                          )}
                        </div>
                      </>
                    )}
                  </div>

                  {!previewResult && transferCenterId && (
                    <div className="d-flex justify-content-start mt-3">
                      <button
                        type="button"
                        className="btn btn-sm btn-dark fw-semibold"
                        onClick={handleExecutePreview}
                        disabled={loadingPreview || actionMutexOwnerRef.current !== null}
                      >
                        {loadingPreview ? (
                          <>
                            <span
                              className="spinner-border spinner-border-sm me-2"
                              role="status"
                              aria-hidden="true"
                            />
                            Calculating Transfer Preview...
                          </>
                        ) : (
                          'Preview Center Transfer Impact'
                        )}
                      </button>
                    </div>
                  )}
                </div>
              )}

              {/* Preview Error Banner */}
              {previewError && (
                <div className="mt-3">
                  <ApiErrorAlert error={previewError} onDismiss={() => setPreviewError(null)} />
                </div>
              )}

              {/* Authoritative Preview Display & Confirmation Section */}
              {previewResult && (
                <div
                  className="mt-3 p-3 rounded border bg-light"
                  role="region"
                  aria-live="polite"
                  aria-labelledby="previewImpactHeading"
                >
                  <h6
                    id="previewImpactHeading"
                    ref={confirmHeadingRef}
                    tabIndex="-1"
                    className="fw-bold text-dark mb-3"
                  >
                    Authoritative Impact Preview
                  </h6>

                  {/* Impact Summary Badges */}
                  <div className="row g-3 mb-3">
                    <div className="col-md-6">
                      <div className="p-3 bg-white rounded border d-flex align-items-center justify-content-between">
                        <div>
                          <span className="text-muted small d-block">Affected Appointments</span>
                          <strong className="fs-5 text-dark">
                            {previewResult.affectedAppointmentCount}
                          </strong>
                        </div>
                        <span className="badge bg-primary rounded-pill px-3 py-2">
                          {previewResult.affectedAppointmentCount === 0 ? 'No Impact' : 'Requires Rebooking'}
                        </span>
                      </div>
                    </div>

                    <div className="col-md-6">
                      <div className="p-3 bg-white rounded border d-flex align-items-center justify-content-between">
                        <div>
                          <span className="text-muted small d-block">Affected Waitlist Entries</span>
                          <strong className="fs-5 text-dark">
                            {previewResult.affectedWaitlistCount}
                          </strong>
                        </div>
                        <span className="badge bg-secondary rounded-pill px-3 py-2">
                          {previewResult.affectedWaitlistCount === 0 ? 'No Impact' : 'Notification Pending'}
                        </span>
                      </div>
                    </div>
                  </div>

                  {/* Schedule / Snapshot Comparison */}
                  {previewResult.oldSnapshot && previewResult.newSnapshot && (
                    <div className="row g-3 mb-3">
                      <div className="col-md-6">
                        <div className="card border-0 bg-white p-3 h-100">
                          <span className="text-muted small fw-bold d-block mb-2">
                            Current Session State
                          </span>
                          <div className="small">
                            <div className="mb-1">
                              <strong>Center:</strong>{' '}
                              {centersMap[previewResult.oldSnapshot.centerId]?.name ||
                                `Center #${previewResult.oldSnapshot.centerId}`}
                            </div>
                            <div className="mb-1">
                              <strong>Start:</strong>{' '}
                              {formatInTimezone(
                                previewResult.oldSnapshot.startAt,
                                previewResult.oldSnapshot.scheduleTimezone
                              )}
                            </div>
                            <div className="mb-1">
                              <strong>End:</strong>{' '}
                              {formatInTimezone(
                                previewResult.oldSnapshot.endAt,
                                previewResult.oldSnapshot.scheduleTimezone
                              )}
                            </div>
                            <div className="mb-1">
                              <strong>Booking Close:</strong>{' '}
                              {formatInTimezone(
                                previewResult.oldSnapshot.bookingCloseAt,
                                previewResult.oldSnapshot.scheduleTimezone
                              )}
                            </div>
                            <div>
                              <strong>Status:</strong>{' '}
                              <span className="badge bg-secondary">
                                {previewResult.oldSnapshot.lifecycleStatus}
                              </span>
                            </div>
                          </div>
                        </div>
                      </div>

                      <div className="col-md-6">
                        <div className="card border-0 bg-white p-3 h-100 border-start border-primary border-3">
                          <span className="text-primary small fw-bold d-block mb-2">
                            Proposed Session State
                          </span>
                          <div className="small">
                            <div className="mb-1">
                              <strong>Center:</strong>{' '}
                              {centersMap[previewResult.newSnapshot.centerId]?.name ||
                                `Center #${previewResult.newSnapshot.centerId}`}
                            </div>
                            <div className="mb-1">
                              <strong>Start:</strong>{' '}
                              {formatInTimezone(
                                previewResult.newSnapshot.startAt,
                                previewResult.newSnapshot.scheduleTimezone
                              )}
                            </div>
                            <div className="mb-1">
                              <strong>End:</strong>{' '}
                              {formatInTimezone(
                                previewResult.newSnapshot.endAt,
                                previewResult.newSnapshot.scheduleTimezone
                              )}
                            </div>
                            <div className="mb-1">
                              <strong>Booking Close:</strong>{' '}
                              {formatInTimezone(
                                previewResult.newSnapshot.bookingCloseAt,
                                previewResult.newSnapshot.scheduleTimezone
                              )}
                            </div>
                            <div>
                              <strong>Status:</strong>{' '}
                              <span className="badge bg-primary">
                                {previewResult.newSnapshot.lifecycleStatus}
                              </span>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  )}

                  {/* Cryptographic Preview Token Display */}
                  <div className="mb-3">
                    <span className="text-muted small d-block">
                      Authoritative Verification Token
                    </span>
                    <code
                      className="text-break small bg-white p-1 rounded border d-block font-monospace"
                      style={{ fontSize: '0.75rem' }}
                    >
                      {previewResult.previewToken}
                    </code>
                  </div>

                  {/* Reason Input */}
                  <div className="mb-3">
                    <label htmlFor="changeReasonInput" className="form-label small fw-semibold">
                      Reason for Session Change <span className="text-danger">*</span>
                    </label>
                    <textarea
                      id="changeReasonInput"
                      className="form-control form-control-sm"
                      rows="3"
                      placeholder="Enter a mandatory audit reason explaining this session change (max 500 characters)..."
                      value={reason}
                      onChange={(e) => {
                        setReason(e.target.value);
                        setConfirmError(null);
                      }}
                      maxLength={500}
                      disabled={submittingConfirm}
                      required
                    />
                    <div className="d-flex justify-content-between text-muted small mt-1">
                      <span>Mandatory audit trail reason</span>
                      <span>{reason.length} / 500</span>
                    </div>
                  </div>

                  {/* Confirmation Error Banner */}
                  {confirmError && (
                    <div className="mb-3">
                      <ApiErrorAlert error={confirmError} onDismiss={() => setConfirmError(null)} />
                    </div>
                  )}

                  {/* Confirmation Action Buttons */}
                  <div className="d-flex justify-content-end gap-2">
                    <button
                      type="button"
                      className="btn btn-sm btn-secondary"
                      onClick={() => {
                        setPreviewResult(null);
                        previewSnapshotRef.current = null;
                        setConfirmError(null);
                      }}
                      disabled={submittingConfirm}
                    >
                      Back to Edit
                    </button>
                    <button
                      type="button"
                      className={`btn btn-sm fw-semibold ${
                        activeAction === 'CANCEL'
                          ? 'btn-danger text-white'
                          : activeAction === 'RESCHEDULE'
                          ? 'btn-primary'
                          : 'btn-dark'
                      }`}
                      onClick={handleConfirmChange}
                      disabled={submittingConfirm || actionMutexOwnerRef.current !== null}
                    >
                      {submittingConfirm ? (
                        <>
                          <span
                            className="spinner-border spinner-border-sm me-2"
                            role="status"
                            aria-hidden="true"
                          />
                          Applying Change...
                        </>
                      ) : (
                        `Confirm ${
                          activeAction === 'CANCEL'
                            ? 'Cancellation'
                            : activeAction === 'RESCHEDULE'
                            ? 'Reschedule'
                            : 'Transfer'
                        }`
                      )}
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      ) : (
        <div className="alert alert-info py-2 small mb-0">
          <strong>Notice:</strong> This session is in{' '}
          <span className="badge bg-secondary">{session.lifecycleStatus}</span> status. Change
          operations (Cancel, Reschedule, Transfer) are not permitted on{' '}
          {session.lifecycleStatus} sessions. Full audit event history is available below.
        </div>
      )}

      {/* Change Events Audit History Card */}
      <div className="card border p-3">
        <div className="d-flex justify-content-between align-items-center mb-3">
          <div>
            <h6 className="fw-bold text-dark mb-1">Session Change Audit History</h6>
            <p className="text-secondary small mb-0">
              Immutable, append-only log of change control events for Session #{session.id}.
            </p>
          </div>
          <button
            type="button"
            className="btn btn-sm btn-outline-secondary"
            onClick={fetchHistory}
            disabled={loadingHistory}
          >
            {loadingHistory ? 'Refreshing...' : 'Refresh History'}
          </button>
        </div>

        {historyError && (
          <ApiErrorAlert error={historyError} onDismiss={() => setHistoryError(null)} />
        )}

        {loadingHistory ? (
          <div className="py-4 text-center">
            <LoadingSpinner message="Loading session change history..." />
          </div>
        ) : history.length === 0 ? (
          <div className="text-center py-4 text-muted small bg-light rounded border border-dashed">
            No change events recorded for this session.
          </div>
        ) : (
          <div className="table-responsive">
            <table className="table table-sm table-hover align-middle mb-0">
              <thead className="table-light small">
                <tr>
                  <th scope="col" style={{ width: '60px' }}>#</th>
                  <th scope="col">Timestamp</th>
                  <th scope="col">Change Type</th>
                  <th scope="col">Actor</th>
                  <th scope="col">Reason</th>
                  <th scope="col" className="text-center">Appointments</th>
                  <th scope="col" className="text-center">Waitlist</th>
                </tr>
              </thead>
              <tbody className="small">
                {history.map((event) => (
                  <tr key={event.id}>
                    <td className="font-monospace text-muted">{event.id}</td>
                    <td>
                      <div>{formatInTimezone(event.createdAt, tz)}</div>
                      <div
                        className="text-muted font-monospace"
                        style={{ fontSize: '0.75rem' }}
                      >
                        UTC: {event.createdAt}
                      </div>
                    </td>
                    <td>{getChangeTypeBadge(event.changeType)}</td>
                    <td>
                      <span className="badge bg-light text-dark border font-monospace">
                        {event.actorId ? `Actor #${event.actorId}` : '—'}
                      </span>
                    </td>
                    <td className="text-break" style={{ maxWidth: '280px' }}>
                      {event.reason}
                    </td>
                    <td className="text-center">
                      <span
                        className={`badge ${
                          event.affectedAppointmentCount > 0 ? 'bg-warning text-dark' : 'bg-light text-muted border'
                        }`}
                      >
                        {event.affectedAppointmentCount}
                      </span>
                    </td>
                    <td className="text-center">
                      <span
                        className={`badge ${
                          event.affectedWaitlistCount > 0 ? 'bg-secondary text-white' : 'bg-light text-muted border'
                        }`}
                      >
                        {event.affectedWaitlistCount}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
