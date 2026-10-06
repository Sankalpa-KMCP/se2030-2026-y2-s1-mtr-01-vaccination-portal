import { useState, useEffect, useCallback, useRef } from "react";
import { previewSessionCancellation, cancelSession, previewSessionReschedule, rescheduleSession, previewSessionTransfer, transferSession, listSessionChangeEvents, localToUtcIso, utcToLocalDateAndTime } from "../api/sessionApi";
import { getCenterAssignments } from "../../campaign-management/api/campaignConfigurationApi";



export default function useSessionChangeControl({ session, campaignsMap, centersMap, onSessionUpdated }) {
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
  const [campaignCentersError, setCampaignCentersError] = useState(null);

  // Preview State
  const [previewResult, setPreviewResult] = useState(null);
  const [loadingPreview, setLoadingPreview] = useState(false);
  const [previewError, setPreviewError] = useState(null);
  const previewSnapshotRef = useRef(null);
  const prevPreviewResultRef = useRef(null);

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

  // Programmatically focus the confirmation heading exactly once when a new preview becomes actionable
  useEffect(() => {
    if (previewResult && !prevPreviewResultRef.current && confirmHeadingRef.current) {
      confirmHeadingRef.current.focus();
    }
    prevPreviewResultRef.current = previewResult;
  }, [previewResult]);

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
    prevPreviewResultRef.current = null;
    setPreviewError(null);
    setConfirmError(null);
    setSuccessMessage(null);
    setReason('');
    setLoadingPreview(false);
    setSubmittingConfirm(false);
    setCampaignCentersError(null);

    fetchHistory();
  }, [session?.id, fetchHistory]);

  // Load campaign center assignments for transfer options (fails closed on error or empty)
  useEffect(() => {
    if (!session?.campaignId || activeAction !== 'TRANSFER') {
      return;
    }

    let isMounted = true;
    setLoadingCampaignCenters(true);
    setCampaignCentersError(null);

    getCenterAssignments(session.campaignId)
      .then((data) => {
        if (!isMounted) return;
        // Filter out inactive assignments AND exclude the current session center
        const validAssignments = (data || []).filter((a) => {
          const cId = a.centerId || a.id;
          const isActiveOrFuture = a.status === 'ACTIVE' || a.status === 'FUTURE';
          const isNotCurrent = Number(cId) !== Number(session.centerId);
          return isActiveOrFuture && isNotCurrent;
        });
        setCampaignCenters(validAssignments);
        setLoadingCampaignCenters(false);
      })
      .catch((err) => {
        if (!isMounted) return;
        setCampaignCenters([]);
        setCampaignCentersError(
          err?.message || 'Failed to load campaign center assignments for transfer.'
        );
        setLoadingCampaignCenters(false);
      });

    return () => {
      isMounted = false;
    };
  }, [session?.campaignId, session?.centerId, activeAction]);

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
      setTransferCenterId('');
      setTransferDate('');
      setTransferStartTime('');
      setTransferEndTime('');
      setTransferBookingCloseMode('SAME_AS_START');
      setTransferBookingCloseDate('');
      setTransferBookingCloseTime('');
    }
  };

  // Handle target center selection in Transfer action
  const handleTransferCenterChange = (e) => {
    const selectedId = e.target.value;
    setTransferCenterId(selectedId);
    if (previewResult !== null) {
      setPreviewResult(null);
      previewSnapshotRef.current = null;
    }
    setConfirmError(null);
    setPreviewError(null);

    if (selectedId) {
      const targetCenter = centersMap[selectedId];
      const targetTz = targetCenter?.timezone?.trim();
      if (targetTz) {
        // Convert session timestamps to the selected destination center's local timezone
        const startParsed = utcToLocalDateAndTime(session.startAt, targetTz);
        const endParsed = utcToLocalDateAndTime(session.endAt, targetTz);
        const bookingParsed = utcToLocalDateAndTime(session.bookingCloseAt, targetTz);

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
      } else {
        setPreviewError(
          new Error(
            "Unable to load the selected center's timezone. Reload center data before previewing the transfer."
          )
        );
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
        if (Number(transferCenterId) === Number(session.centerId)) {
          throw new Error('Cannot transfer to the current center. Use the Reschedule action instead.');
        }

        const targetCenter = centersMap[transferCenterId];
        const targetTz = targetCenter?.timezone?.trim();
        if (!targetTz) {
          throw new Error(
            "Unable to load the selected center's timezone. Reload center data before previewing the transfer."
          );
        }

        if (!transferDate || !transferStartTime || !transferEndTime) {
          throw new Error('Transfer date, start time, and end time are required.');
        }
        if (transferStartTime >= transferEndTime) {
          throw new Error('Transfer start time must be strictly before end time.');
        }

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
          targetTz,
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
        const snapshotTz = previewSnapshotRef.current?.targetTz;
        const currentTargetCenter = centersMap[transferCenterId];
        const currentTargetTz = currentTargetCenter?.timezone?.trim();

        if (
          !snapshot ||
          !snapshotTz ||
          snapshotTz !== currentTargetTz ||
          Number(snapshot.newCenterId) !== Number(transferCenterId)
        ) {
          throw new Error('Transfer parameters or target center became stale. Please preview the change again.');
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



  const campaign = session ? campaignsMap[session.campaignId] : null;


  return {
    activeAction,
    history,
    loadingHistory,
    historyError,
    setHistoryError,
    rescheduleDate,
    setRescheduleDate,
    rescheduleStartTime,
    setRescheduleStartTime,
    rescheduleEndTime,
    setRescheduleEndTime,
    rescheduleBookingCloseMode,
    setRescheduleBookingCloseMode,
    rescheduleBookingCloseDate,
    setRescheduleBookingCloseDate,
    rescheduleBookingCloseTime,
    setRescheduleBookingCloseTime,
    transferCenterId,
    transferDate,
    setTransferDate,
    transferStartTime,
    setTransferStartTime,
    transferEndTime,
    setTransferEndTime,
    transferBookingCloseMode,
    setTransferBookingCloseMode,
    transferBookingCloseDate,
    setTransferBookingCloseDate,
    transferBookingCloseTime,
    setTransferBookingCloseTime,
    campaignCenters,
    loadingCampaignCenters,
    campaignCentersError,
    previewResult,
    setPreviewResult,
    loadingPreview,
    previewError,
    setPreviewError,
    previewSnapshotRef,
    reason,
    setReason,
    submittingConfirm,
    confirmError,
    setConfirmError,
    successMessage,
    setSuccessMessage,
    actionMutexOwnerRef,
    confirmHeadingRef,
    tz,
    isScheduled,
    isMutationAllowed,
    fetchHistory,
    handleSelectAction,
    handleTransferCenterChange,
    handleFieldChange,
    handleExecutePreview,
    handleConfirmChange,
    campaign
  };
}
