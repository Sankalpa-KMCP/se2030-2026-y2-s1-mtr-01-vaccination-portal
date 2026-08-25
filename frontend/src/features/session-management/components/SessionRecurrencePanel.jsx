import React, { useState, useEffect, useCallback, useRef } from 'react';
import {
  listRecurrenceRules,
  getRecurrenceRuleById,
  updateRecurrenceRule,
  generateRecurrenceOccurrences,
  previewSeriesChange,
  confirmSeriesChange,
  previewSeriesCancellation,
  confirmSeriesCancellation,
  isValidIanaTimezone,
  formatLocalTimeForDto,
} from '../api/sessionApi';
import CreateRecurrenceRuleModal from './CreateRecurrenceRuleModal';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import LoadingSpinner from '../../../components/common/LoadingSpinner';

const WEEKDAYS = [
  { id: 'MONDAY', label: 'Mon' },
  { id: 'TUESDAY', label: 'Tue' },
  { id: 'WEDNESDAY', label: 'Wed' },
  { id: 'THURSDAY', label: 'Thu' },
  { id: 'FRIDAY', label: 'Fri' },
  { id: 'SATURDAY', label: 'Sat' },
  { id: 'SUNDAY', label: 'Sun' },
];

export default function SessionRecurrencePanel({
  campaignsList = [],
  centersList = [],
  campaignsMap = {},
  centersMap = {},
  recurrenceOperationOwnerRef = null,
  recurrenceRefreshGen = 0,
  onRecurrenceDataChanged,
  onSessionDataChanged,
}) {
  // Inventory state
  const [rules, setRules] = useState([]);
  const [rulesLoading, setRulesLoading] = useState(false);
  const [rulesError, setRulesError] = useState(null);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);

  const [filters, setFilters] = useState({
    campaignId: '',
    centerId: '',
    status: '',
  });

  // Selected rule detail state
  const [selectedRuleId, setSelectedRuleId] = useState(null);
  const [selectedRule, setSelectedRule] = useState(null);
  const [ruleDetailLoading, setRuleDetailLoading] = useState(false);
  const [ruleDetailError, setRuleDetailError] = useState(null);

  // Active sub-action tab: 'view' | 'generate' | 'update_definition' | 'series_change' | 'series_cancel'
  const [activeAction, setActiveAction] = useState('view');

  // Generation action state
  const [genStartDate, setGenStartDate] = useState('');
  const [genEndDate, setGenEndDate] = useState('');
  const [genLoading, setGenLoading] = useState(false);
  const [genError, setGenError] = useState(null);
  const [genResult, setGenResult] = useState(null);

  // Update future rule definition state (PUT)
  const [updateWeekdays, setUpdateWeekdays] = useState([]);
  const [updateStartDate, setUpdateStartDate] = useState('');
  const [updateEndDate, setUpdateEndDate] = useState('');
  const [updateStartTime, setUpdateStartTime] = useState('09:00:00');
  const [updateEndTime, setUpdateEndTime] = useState('17:00:00');
  const [updateLoading, setUpdateLoading] = useState(false);
  const [updateError, setUpdateError] = useState(null);
  const [updateSuccessMsg, setUpdateSuccessMsg] = useState(null);

  // Series Change state (POST change-preview / change)
  const [changeWeekdays, setChangeWeekdays] = useState([]);
  const [changeStartDate, setChangeStartDate] = useState('');
  const [changeEndDate, setChangeEndDate] = useState('');
  const [changeStartTime, setChangeStartTime] = useState('09:00:00');
  const [changeEndTime, setChangeEndTime] = useState('17:00:00');
  const [changeReason, setChangeReason] = useState('');
  const [changePreviewLoading, setChangePreviewLoading] = useState(false);
  const [changePreviewError, setChangePreviewError] = useState(null);
  const [changePreviewData, setChangePreviewData] = useState(null);
  const [changePreviewFingerprint, setChangePreviewFingerprint] = useState(null);
  const [changeConfirmLoading, setChangeConfirmLoading] = useState(false);
  const [changeConfirmError, setChangeConfirmError] = useState(null);
  const [changeResultData, setChangeResultData] = useState(null);

  // Series Cancellation state (POST cancellation-preview / cancel)
  const [cancelReason, setCancelReason] = useState('');
  const [cancelPreviewLoading, setCancelPreviewLoading] = useState(false);
  const [cancelPreviewError, setCancelPreviewError] = useState(null);
  const [cancelPreviewData, setCancelPreviewData] = useState(null);
  const [cancelConfirmLoading, setCancelConfirmLoading] = useState(false);
  const [cancelConfirmError, setCancelConfirmError] = useState(null);
  const [cancelResultData, setCancelResultData] = useState(null);

  // Global notification banner for panel
  const [panelSuccessMsg, setPanelSuccessMsg] = useState(null);

  // Async race safety refs
  const isMountedRef = useRef(true);
  const selectionGenerationRef = useRef(0);
  const selectedRuleIdRef = useRef(selectedRuleId);
  selectedRuleIdRef.current = selectedRuleId;

  const latestRulesReqRef = useRef(null);
  const latestRuleDetailReqRef = useRef(null);
  const localOpFallbackRef = useRef(null);
  const changeConfirmButtonRef = useRef(null);
  const cancelConfirmButtonRef = useRef(null);

  // Use parent-owned gate if provided, else fallback to component-local
  const activeOpGateRef = recurrenceOperationOwnerRef || localOpFallbackRef;

  // Ref to hold latest filter state for safe async refresh
  const latestFiltersRef = useRef(filters);
  latestFiltersRef.current = filters;

  useEffect(() => {
    isMountedRef.current = true;
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  // -----------------------------------------------------------------------------------------------
  // 1. Fetch Recurrence Rule Inventory
  // -----------------------------------------------------------------------------------------------
  const fetchRules = useCallback(() => {
    const currentReqId = Symbol('rules_req');
    latestRulesReqRef.current = currentReqId;

    setRulesLoading(true);
    setRulesError(null);

    const cur = latestFiltersRef.current;
    const query = {
      campaignId: cur.campaignId || null,
      centerId: cur.centerId || null,
      status: cur.status || null,
    };

    listRecurrenceRules(query)
      .then((data) => {
        if (!isMountedRef.current) return;
        if (latestRulesReqRef.current !== currentReqId) return;
        setRules(data || []);
        setRulesLoading(false);
      })
      .catch((err) => {
        if (!isMountedRef.current) return;
        if (latestRulesReqRef.current !== currentReqId) return;
        setRulesError(err);
        setRulesLoading(false);
      });
  }, []);

  useEffect(() => {
    fetchRules();
  }, [filters.campaignId, filters.centerId, filters.status, recurrenceRefreshGen, fetchRules]);

  // -----------------------------------------------------------------------------------------------
  // 2. Fetch Selected Rule Details
  // -----------------------------------------------------------------------------------------------
  const fetchRuleDetails = useCallback((ruleId) => {
    if (!ruleId) {
      setSelectedRule(null);
      return;
    }

    const currentReqId = Symbol('rule_detail_req');
    latestRuleDetailReqRef.current = currentReqId;

    setRuleDetailLoading(true);
    setRuleDetailError(null);

    getRecurrenceRuleById(ruleId)
      .then((data) => {
        if (!isMountedRef.current) return;
        if (latestRuleDetailReqRef.current !== currentReqId) return;
        setSelectedRule(data);
        setRuleDetailLoading(false);

        // Pre-populate update and change form state without losing seconds
        if (data) {
          const days = data.weekdays || [];
          setUpdateWeekdays([...days]);
          setUpdateStartDate(data.startDate || '');
          setUpdateEndDate(data.endDate || '');
          setUpdateStartTime(data.startTime || '09:00:00');
          setUpdateEndTime(data.endTime || '17:00:00');

          setChangeWeekdays([...days]);
          setChangeStartDate(data.startDate || '');
          setChangeEndDate(data.endDate || '');
          setChangeStartTime(data.startTime || '09:00:00');
          setChangeEndTime(data.endTime || '17:00:00');
        }
      })
      .catch((err) => {
        if (!isMountedRef.current) return;
        if (latestRuleDetailReqRef.current !== currentReqId) return;
        setRuleDetailError(err);
        setRuleDetailLoading(false);
      });
  }, []);

  useEffect(() => {
    fetchRuleDetails(selectedRuleId);
  }, [selectedRuleId, fetchRuleDetails]);

  // Reset all sub-action state when switching rules or closing
  const handleSelectRule = (ruleId) => {
    selectionGenerationRef.current += 1;
    selectedRuleIdRef.current = ruleId;
    setSelectedRuleId(ruleId);
    setActiveAction('view');
    setGenError(null);
    setGenResult(null);
    setUpdateError(null);
    setUpdateSuccessMsg(null);
    setChangePreviewData(null);
    setChangePreviewFingerprint(null);
    setChangePreviewError(null);
    setChangeConfirmError(null);
    setChangeResultData(null);
    setChangeReason('');
    setCancelPreviewData(null);
    setCancelPreviewError(null);
    setCancelConfirmError(null);
    setCancelResultData(null);
    setCancelReason('');
  };

  // -----------------------------------------------------------------------------------------------
  // 3. Occurrence Generation Action
  // -----------------------------------------------------------------------------------------------
  const handleGenerateOccurrences = async () => {
    if (!selectedRuleId) return;
    if (activeOpGateRef.current) return; // Mutex guard

    // Validate date window inputs: both or neither
    if ((genStartDate && !genEndDate) || (!genStartDate && genEndDate)) {
      setGenError(
        new Error('Generation window requires both a start date and an end date, or leave both empty for the full rule horizon.')
      );
      return;
    }
    if (genStartDate && genEndDate && genStartDate > genEndDate) {
      setGenError(new Error('Window end date must be on or after start date.'));
      return;
    }

    const opId = Symbol('gen_op');
    activeOpGateRef.current = opId;

    const ruleIdAtStart = selectedRuleId;
    const selectionGenAtStart = selectionGenerationRef.current;

    setGenLoading(true);
    setGenError(null);
    setGenResult(null);

    const payload = (genStartDate && genEndDate) ? {
      requestedStartDate: genStartDate,
      requestedEndDate: genEndDate,
    } : null;

    try {
      const res = await generateRecurrenceOccurrences(selectedRuleId, payload);
      // Unconditional parent reconciliation on successful backend mutation
      if (onRecurrenceDataChanged) onRecurrenceDataChanged();
      if (onSessionDataChanged) onSessionDataChanged();

      const isStillCurrent =
        isMountedRef.current &&
        selectedRuleIdRef.current === ruleIdAtStart &&
        selectionGenerationRef.current === selectionGenAtStart;

      if (isStillCurrent) {
        setGenResult(res);
        setPanelSuccessMsg(
          `Occurrences generated for Rule #${selectedRuleId}: ${res.createdCount} new session(s) created, ${res.existingCount} already present.`
        );
      }
    } catch (err) {
      const isStillCurrent =
        isMountedRef.current &&
        selectedRuleIdRef.current === ruleIdAtStart &&
        selectionGenerationRef.current === selectionGenAtStart;

      if (isStillCurrent) {
        setGenError(err);
      }
    } finally {
      if (activeOpGateRef.current === opId) {
        activeOpGateRef.current = null;
      }
      if (
        isMountedRef.current &&
        selectedRuleIdRef.current === ruleIdAtStart &&
        selectionGenerationRef.current === selectionGenAtStart
      ) {
        setGenLoading(false);
      }
    }
  };

  // -----------------------------------------------------------------------------------------------
  // 4. Update Future Rule Schedule Definition (PUT)
  // -----------------------------------------------------------------------------------------------
  const handleUpdateDefinition = async (e) => {
    e.preventDefault();
    if (!selectedRule || !selectedRuleId) return;
    if (activeOpGateRef.current) return;

    if (!updateWeekdays || updateWeekdays.length === 0) {
      setUpdateError(new Error('At least one recurrence weekday must be selected.'));
      return;
    }
    if (!updateStartDate || !updateEndDate) {
      setUpdateError(new Error('Start date and end date are required.'));
      return;
    }
    if (updateStartDate > updateEndDate) {
      setUpdateError(new Error('End date must be on or after start date.'));
      return;
    }
    if (updateStartTime >= updateEndTime) {
      setUpdateError(new Error('End time must be after start time.'));
      return;
    }

    // Authoritative center timezone validation: must use current center metadata, not historical rule timezone
    const currentCenter = centersMap[selectedRule.centerId];
    const authoritativeTimezone = currentCenter?.timezone || null;
    if (!authoritativeTimezone || !isValidIanaTimezone(authoritativeTimezone)) {
      setUpdateError(
        new Error('Selected center does not have valid authoritative IANA timezone metadata configured.')
      );
      return;
    }

    const opId = Symbol('update_def_op');
    activeOpGateRef.current = opId;

    const ruleIdAtStart = selectedRuleId;
    const selectionGenAtStart = selectionGenerationRef.current;

    setUpdateLoading(true);
    setUpdateError(null);
    setUpdateSuccessMsg(null);

    const formattedStartTime = formatLocalTimeForDto(updateStartTime);
    const formattedEndTime = formatLocalTimeForDto(updateEndTime);

    const payload = {
      weekdays: updateWeekdays,
      startDate: updateStartDate,
      endDate: updateEndDate,
      startTime: formattedStartTime,
      endTime: formattedEndTime,
      timezone: authoritativeTimezone.trim(),
    };

    try {
      const updated = await updateRecurrenceRule(selectedRuleId, payload);
      // Unconditional parent reconciliation
      if (onRecurrenceDataChanged) onRecurrenceDataChanged();
      if (onSessionDataChanged) onSessionDataChanged();

      const isStillCurrent =
        isMountedRef.current &&
        selectedRuleIdRef.current === ruleIdAtStart &&
        selectionGenerationRef.current === selectionGenAtStart;

      if (isStillCurrent) {
        setSelectedRule(updated);
        setUpdateSuccessMsg(`Recurrence Rule #${selectedRuleId} definition updated successfully.`);
        setPanelSuccessMsg(`Recurrence Rule #${selectedRuleId} schedule definition updated.`);
      }
    } catch (err) {
      const isStillCurrent =
        isMountedRef.current &&
        selectedRuleIdRef.current === ruleIdAtStart &&
        selectionGenerationRef.current === selectionGenAtStart;

      if (isStillCurrent) {
        setUpdateError(err);
      }
    } finally {
      if (activeOpGateRef.current === opId) {
        activeOpGateRef.current = null;
      }
      if (
        isMountedRef.current &&
        selectedRuleIdRef.current === ruleIdAtStart &&
        selectionGenerationRef.current === selectionGenAtStart
      ) {
        setUpdateLoading(false);
      }
    }
  };

  // -----------------------------------------------------------------------------------------------
  // 5. Series Change (Preview -> Confirm)
  // -----------------------------------------------------------------------------------------------
  const computeChangeFingerprint = (weekdays, startD, endD, startT, endT, tz) => {
    return JSON.stringify({
      weekdays: [...weekdays].sort(),
      startDate: startD,
      endDate: endD,
      startTime: startT,
      endTime: endT,
      timezone: tz,
    });
  };

  const handlePreviewSeriesChange = async () => {
    if (!selectedRule || !selectedRuleId) return;
    if (activeOpGateRef.current) return;

    if (!changeWeekdays || changeWeekdays.length === 0) {
      setChangePreviewError(new Error('At least one recurrence weekday must be selected.'));
      return;
    }
    if (!changeStartDate || !changeEndDate) {
      setChangePreviewError(new Error('Start date and end date are required.'));
      return;
    }
    if (changeStartDate > changeEndDate) {
      setChangePreviewError(new Error('End date must be on or after start date.'));
      return;
    }
    if (changeStartTime >= changeEndTime) {
      setChangePreviewError(new Error('End time must be after start time.'));
      return;
    }

    // Authoritative center timezone validation
    const currentCenter = centersMap[selectedRule.centerId];
    const authoritativeTimezone = currentCenter?.timezone || null;
    if (!authoritativeTimezone || !isValidIanaTimezone(authoritativeTimezone)) {
      setChangePreviewError(
        new Error('Selected center does not have valid authoritative IANA timezone metadata configured.')
      );
      return;
    }

    const opId = Symbol('change_preview_op');
    activeOpGateRef.current = opId;

    const ruleIdAtStart = selectedRuleId;
    const selectionGenAtStart = selectionGenerationRef.current;

    setChangePreviewLoading(true);
    setChangePreviewError(null);
    setChangePreviewData(null);
    setChangeConfirmError(null);
    setChangeResultData(null);

    const formattedStartTime = formatLocalTimeForDto(changeStartTime);
    const formattedEndTime = formatLocalTimeForDto(changeEndTime);

    const payload = {
      weekdays: changeWeekdays,
      startDate: changeStartDate,
      endDate: changeEndDate,
      startTime: formattedStartTime,
      endTime: formattedEndTime,
      timezone: authoritativeTimezone.trim(),
    };

    const fingerprint = computeChangeFingerprint(
      changeWeekdays,
      changeStartDate,
      changeEndDate,
      formattedStartTime,
      formattedEndTime,
      authoritativeTimezone.trim()
    );

    try {
      const preview = await previewSeriesChange(selectedRuleId, payload);
      const isStillCurrent =
        isMountedRef.current &&
        selectedRuleIdRef.current === ruleIdAtStart &&
        selectionGenerationRef.current === selectionGenAtStart;

      if (isStillCurrent) {
        setChangePreviewData({
          ...preview,
          capturedTimezone: authoritativeTimezone.trim(),
        });
        setChangePreviewFingerprint(fingerprint);
        // Focus confirm action when preview becomes actionable
        setTimeout(() => {
          changeConfirmButtonRef.current?.focus();
        }, 100);
      }
    } catch (err) {
      const isStillCurrent =
        isMountedRef.current &&
        selectedRuleIdRef.current === ruleIdAtStart &&
        selectionGenerationRef.current === selectionGenAtStart;

      if (isStillCurrent) {
        setChangePreviewError(err);
      }
    } finally {
      if (activeOpGateRef.current === opId) {
        activeOpGateRef.current = null;
      }
      if (
        isMountedRef.current &&
        selectedRuleIdRef.current === ruleIdAtStart &&
        selectionGenerationRef.current === selectionGenAtStart
      ) {
        setChangePreviewLoading(false);
      }
    }
  };

  const handleConfirmSeriesChange = async () => {
    if (!selectedRule || !selectedRuleId || !changePreviewData) return;
    if (activeOpGateRef.current) return;

    // Check if current center timezone changed since preview
    const currentCenter = centersMap[selectedRule.centerId];
    if (!currentCenter?.timezone || currentCenter.timezone.trim() !== changePreviewData.capturedTimezone) {
      setChangeConfirmError(
        new Error('Vaccination center timezone changed after preview was generated. Please re-preview before confirming.')
      );
      setChangePreviewData(null);
      setChangePreviewFingerprint(null);
      return;
    }

    const currentFormattedStartTime = formatLocalTimeForDto(changeStartTime);
    const currentFormattedEndTime = formatLocalTimeForDto(changeEndTime);

    const currentFingerprint = computeChangeFingerprint(
      changeWeekdays,
      changeStartDate,
      changeEndDate,
      currentFormattedStartTime,
      currentFormattedEndTime,
      changePreviewData.capturedTimezone
    );

    if (currentFingerprint !== changePreviewFingerprint) {
      setChangeConfirmError(
        new Error('Schedule definition was modified after preview. Please re-preview before confirming.')
      );
      setChangePreviewData(null);
      setChangePreviewFingerprint(null);
      return;
    }

    const trimmedReason = changeReason.trim();
    if (!trimmedReason) {
      setChangeConfirmError(new Error('A change reason is required to confirm series modification.'));
      return;
    }
    if (trimmedReason.length > 500) {
      setChangeConfirmError(new Error('Change reason cannot exceed 500 characters.'));
      return;
    }

    const opId = Symbol('change_confirm_op');
    activeOpGateRef.current = opId;

    const ruleIdAtStart = selectedRuleId;
    const selectionGenAtStart = selectionGenerationRef.current;

    setChangeConfirmLoading(true);
    setChangeConfirmError(null);

    const payload = {
      weekdays: changeWeekdays,
      startDate: changeStartDate,
      endDate: changeEndDate,
      startTime: currentFormattedStartTime,
      endTime: currentFormattedEndTime,
      timezone: changePreviewData.capturedTimezone,
      previewToken: changePreviewData.previewToken,
      reason: trimmedReason,
      // CRITICAL: actorId is strictly omitted (server derives from UserPrincipal)
    };

    try {
      const result = await confirmSeriesChange(selectedRuleId, payload);
      // Unconditional parent reconciliation
      if (onRecurrenceDataChanged) onRecurrenceDataChanged();
      if (onSessionDataChanged) onSessionDataChanged();

      const isStillCurrent =
        isMountedRef.current &&
        selectedRuleIdRef.current === ruleIdAtStart &&
        selectionGenerationRef.current === selectionGenAtStart;

      if (isStillCurrent) {
        setChangeResultData(result);
        setSelectedRule(result.rule);
        setChangePreviewData(null);
        setChangePreviewFingerprint(null);
        setPanelSuccessMsg(
          `Series change confirmed for Rule #${selectedRuleId}: ${result.rescheduledCount} rescheduled, ${result.cancelledCount} cancelled, ${result.newCount} created.`
        );
      }
    } catch (err) {
      const isStillCurrent =
        isMountedRef.current &&
        selectedRuleIdRef.current === ruleIdAtStart &&
        selectionGenerationRef.current === selectionGenAtStart;

      if (isStillCurrent) {
        setChangeConfirmError(err);
      }
    } finally {
      if (activeOpGateRef.current === opId) {
        activeOpGateRef.current = null;
      }
      if (
        isMountedRef.current &&
        selectedRuleIdRef.current === ruleIdAtStart &&
        selectionGenerationRef.current === selectionGenAtStart
      ) {
        setChangeConfirmLoading(false);
      }
    }
  };

  // -----------------------------------------------------------------------------------------------
  // 6. Series Cancellation (Preview -> Confirm)
  // -----------------------------------------------------------------------------------------------
  const handlePreviewSeriesCancellation = async () => {
    if (!selectedRule || !selectedRuleId) return;
    if (activeOpGateRef.current) return;

    const opId = Symbol('cancel_preview_op');
    activeOpGateRef.current = opId;

    const ruleIdAtStart = selectedRuleId;
    const selectionGenAtStart = selectionGenerationRef.current;

    setCancelPreviewLoading(true);
    setCancelPreviewError(null);
    setCancelPreviewData(null);
    setCancelConfirmError(null);
    setCancelResultData(null);

    try {
      const preview = await previewSeriesCancellation(selectedRuleId);
      const isStillCurrent =
        isMountedRef.current &&
        selectedRuleIdRef.current === ruleIdAtStart &&
        selectionGenerationRef.current === selectionGenAtStart;

      if (isStillCurrent) {
        setCancelPreviewData(preview);
        setTimeout(() => {
          cancelConfirmButtonRef.current?.focus();
        }, 100);
      }
    } catch (err) {
      const isStillCurrent =
        isMountedRef.current &&
        selectedRuleIdRef.current === ruleIdAtStart &&
        selectionGenerationRef.current === selectionGenAtStart;

      if (isStillCurrent) {
        setCancelPreviewError(err);
      }
    } finally {
      if (activeOpGateRef.current === opId) {
        activeOpGateRef.current = null;
      }
      if (
        isMountedRef.current &&
        selectedRuleIdRef.current === ruleIdAtStart &&
        selectionGenerationRef.current === selectionGenAtStart
      ) {
        setCancelPreviewLoading(false);
      }
    }
  };

  const handleConfirmSeriesCancellation = async () => {
    if (!selectedRule || !selectedRuleId || !cancelPreviewData) return;
    if (activeOpGateRef.current) return;

    const trimmedReason = cancelReason.trim();
    if (!trimmedReason) {
      setCancelConfirmError(new Error('A cancellation reason is required to confirm series cancellation.'));
      return;
    }
    if (trimmedReason.length > 500) {
      setCancelConfirmError(new Error('Cancellation reason cannot exceed 500 characters.'));
      return;
    }

    const opId = Symbol('cancel_confirm_op');
    activeOpGateRef.current = opId;

    const ruleIdAtStart = selectedRuleId;
    const selectionGenAtStart = selectionGenerationRef.current;

    setCancelConfirmLoading(true);
    setCancelConfirmError(null);

    const payload = {
      previewToken: cancelPreviewData.previewToken,
      reason: trimmedReason,
      // CRITICAL: actorId is strictly omitted (server derives from UserPrincipal)
    };

    try {
      const result = await confirmSeriesCancellation(selectedRuleId, payload);
      // Unconditional parent reconciliation
      if (onRecurrenceDataChanged) onRecurrenceDataChanged();
      if (onSessionDataChanged) onSessionDataChanged();

      const isStillCurrent =
        isMountedRef.current &&
        selectedRuleIdRef.current === ruleIdAtStart &&
        selectionGenerationRef.current === selectionGenAtStart;

      if (isStillCurrent) {
        setCancelResultData(result);
        setSelectedRule(result.rule);
        setCancelPreviewData(null);
        setPanelSuccessMsg(
          `Series cancellation confirmed for Rule #${selectedRuleId}: ${result.cancelledCount} session(s) cancelled.`
        );
      }
    } catch (err) {
      const isStillCurrent =
        isMountedRef.current &&
        selectedRuleIdRef.current === ruleIdAtStart &&
        selectionGenerationRef.current === selectionGenAtStart;

      if (isStillCurrent) {
        setCancelConfirmError(err);
      }
    } finally {
      if (activeOpGateRef.current === opId) {
        activeOpGateRef.current = null;
      }
      if (
        isMountedRef.current &&
        selectedRuleIdRef.current === ruleIdAtStart &&
        selectionGenerationRef.current === selectionGenAtStart
      ) {
        setCancelConfirmLoading(false);
      }
    }
  };

  // Helper to toggle weekday in form
  const toggleUpdateWeekday = (day) => {
    setUpdateWeekdays((prev) =>
      prev.includes(day) ? prev.filter((d) => d !== day) : [...prev, day]
    );
  };

  const toggleChangeWeekday = (day) => {
    setChangeWeekdays((prev) => {
      const next = prev.includes(day) ? prev.filter((d) => d !== day) : [...prev, day];
      // Editing weekdays invalidates active preview
      if (changePreviewData) {
        setChangePreviewData(null);
        setChangePreviewFingerprint(null);
      }
      return next;
    });
  };

  const handleCreateRuleSuccess = (newRule) => {
    setPanelSuccessMsg(`Recurrence Rule #${newRule.id} created successfully.`);
    if (onRecurrenceDataChanged) onRecurrenceDataChanged();
    handleSelectRule(newRule.id);
  };

  // Compute cancellation categorized numbers accurately
  const cancelledEntries = cancelPreviewData?.occurrenceEntries?.filter((e) => e.category === 'REMOVED') || [];
  const cancellationToCancelCount = cancelledEntries.length;
  const cancellationPreservedCount = cancelPreviewData?.occurrenceEntries?.filter((e) => e.category === 'PRESERVED_EXCEPTION').length || 0;
  const cancellationIneligibleCount = cancelPreviewData?.occurrenceEntries?.filter((e) => e.category === 'INELIGIBLE').length || 0;

  return (
    <div className="card shadow-sm border-0">
      <div className="card-header bg-white border-bottom py-3 d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3">
        <div>
          <h2 className="h4 text-dark fw-bold mb-1">Session Recurrence Rules & Series Management</h2>
          <p className="text-secondary small mb-0">
            Automated session occurrence materialization, schedule updates, and series-wide change controls
          </p>
        </div>
        <button
          type="button"
          className="btn btn-primary d-inline-flex align-items-center gap-2"
          onClick={() => setIsCreateModalOpen(true)}
          id="btn-create-recurrence-rule"
        >
          <span className="fs-5 lh-1">+</span>
          Create Recurrence Rule
        </button>
      </div>

      <div className="card-body p-4">
        {/* Global Panel Success Alert */}
        {panelSuccessMsg && (
          <div className="alert alert-success alert-dismissible fade show border-0 shadow-sm mb-4" role="status" aria-live="polite">
            <strong>Success:</strong> {panelSuccessMsg}
            <button
              type="button"
              className="btn-close"
              aria-label="Close message"
              onClick={() => setPanelSuccessMsg(null)}
            />
          </div>
        )}

        {/* Filter Bar */}
        <div className="bg-light p-3 rounded mb-4 border">
          <div className="row g-3 align-items-end">
            <div className="col-12 col-md-4">
              <label htmlFor="filter-rule-campaign" className="form-label small fw-semibold text-muted mb-1">
                Filter by Campaign
              </label>
              <select
                id="filter-rule-campaign"
                className="form-select form-select-sm"
                value={filters.campaignId}
                onChange={(e) => setFilters((prev) => ({ ...prev, campaignId: e.target.value }))}
              >
                <option value="">All Campaigns</option>
                {campaignsList.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name} ({c.code || c.id})
                  </option>
                ))}
              </select>
            </div>

            <div className="col-12 col-md-4">
              <label htmlFor="filter-rule-center" className="form-label small fw-semibold text-muted mb-1">
                Filter by Vaccination Center
              </label>
              <select
                id="filter-rule-center"
                className="form-select form-select-sm"
                value={filters.centerId}
                onChange={(e) => setFilters((prev) => ({ ...prev, centerId: e.target.value }))}
              >
                <option value="">All Centers</option>
                {centersList.map((cnt) => (
                  <option key={cnt.id} value={cnt.id}>
                    {cnt.name} ({cnt.code || cnt.id})
                  </option>
                ))}
              </select>
            </div>

            <div className="col-12 col-md-2">
              <label htmlFor="filter-rule-status" className="form-label small fw-semibold text-muted mb-1">
                Status
              </label>
              <select
                id="filter-rule-status"
                className="form-select form-select-sm"
                value={filters.status}
                onChange={(e) => setFilters((prev) => ({ ...prev, status: e.target.value }))}
              >
                <option value="">All Statuses</option>
                <option value="ACTIVE">ACTIVE</option>
                <option value="CANCELLED">CANCELLED</option>
              </select>
            </div>

            <div className="col-12 col-md-2 d-flex gap-2">
              <button
                type="button"
                className="btn btn-outline-secondary btn-sm flex-fill"
                onClick={() => setFilters({ campaignId: '', centerId: '', status: '' })}
              >
                Reset
              </button>
              <button
                type="button"
                className="btn btn-secondary btn-sm flex-fill"
                onClick={fetchRules}
                disabled={rulesLoading}
              >
                Refresh
              </button>
            </div>
          </div>
        </div>

        {/* Rule Inventory List */}
        <div className="mb-4">
          <h3 className="h6 text-dark fw-bold mb-2">Recurrence Rules Inventory ({rules.length})</h3>

          {rulesError && <ApiErrorAlert error={rulesError} />}

          {rulesLoading ? (
            <div className="p-4 text-center">
              <LoadingSpinner message="Loading session recurrence rules..." />
            </div>
          ) : rules.length === 0 ? (
            <div className="alert alert-info border-0 mb-0" role="status">
              No session recurrence rules found for the selected filter criteria.
            </div>
          ) : (
            <div className="table-responsive">
              <table className="table table-hover table-bordered align-middle mb-0">
                <thead className="table-light">
                  <tr>
                    <th scope="col">Rule ID</th>
                    <th scope="col">Campaign</th>
                    <th scope="col">Center</th>
                    <th scope="col">Weekdays</th>
                    <th scope="col">Date Range</th>
                    <th scope="col">Operating Time</th>
                    <th scope="col">Timezone</th>
                    <th scope="col">Status</th>
                    <th scope="col" className="text-center">Action</th>
                  </tr>
                </thead>
                <tbody>
                  {rules.map((rule) => {
                    const isSelected = selectedRuleId === rule.id;
                    const camp = campaignsMap[rule.campaignId];
                    const cent = centersMap[rule.centerId];
                    const isCancelled = rule.status === 'CANCELLED';

                    return (
                      <tr key={rule.id} className={isSelected ? 'table-primary' : ''}>
                        <td className="fw-bold">#{rule.id}</td>
                        <td>{camp?.name || `Campaign #${rule.campaignId}`}</td>
                        <td>{cent?.name || `Center #${rule.centerId}`}</td>
                        <td>
                          <div className="d-flex flex-wrap gap-1">
                            {rule.weekdays?.map((w) => (
                              <span key={w} className="badge bg-light text-dark border">
                                {w.substring(0, 3)}
                              </span>
                            ))}
                          </div>
                        </td>
                        <td>
                          {rule.startDate} <span className="text-muted">to</span> {rule.endDate}
                        </td>
                        <td>
                          {rule.startTime} - {rule.endTime}
                        </td>
                        <td>
                          <span className="badge bg-secondary">{rule.timezone}</span>
                        </td>
                        <td>
                          <span className={`badge ${isCancelled ? 'bg-danger' : 'bg-success'}`}>
                            {rule.status}
                          </span>
                        </td>
                        <td className="text-center">
                          <button
                            type="button"
                            className={`btn btn-sm ${isSelected ? 'btn-primary' : 'btn-outline-primary'}`}
                            onClick={() => handleSelectRule(rule.id)}
                            id={`btn-select-rule-${rule.id}`}
                          >
                            {isSelected ? 'Managing' : 'Manage Series'}
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Selected Rule Management Workspace */}
        {selectedRuleId && (
          <div className="card border shadow-sm mt-4">
            <div className="card-header bg-light d-flex justify-content-between align-items-center py-2">
              <h3 className="h6 text-dark fw-bold mb-0">
                Manage Series for Recurrence Rule #{selectedRuleId}
                {selectedRule && (
                  <span className={`badge ms-2 ${selectedRule.status === 'CANCELLED' ? 'bg-danger' : 'bg-success'}`}>
                    {selectedRule.status}
                  </span>
                )}
              </h3>
              <button
                type="button"
                className="btn-close"
                aria-label="Close rule workspace"
                onClick={() => handleSelectRule(null)}
              />
            </div>

            <div className="card-body p-4">
              {ruleDetailError && <ApiErrorAlert error={ruleDetailError} />}

              {ruleDetailLoading ? (
                <div className="p-4 text-center">
                  <LoadingSpinner message="Loading recurrence rule details..." />
                </div>
              ) : selectedRule ? (
                <div>
                  {/* Rule summary ribbon */}
                  <div className="bg-light p-3 rounded mb-4 border">
                    <div className="row g-3 small">
                      <div className="col-12 col-md-3">
                        <span className="text-muted d-block">Campaign:</span>
                        <strong className="text-dark">
                          {campaignsMap[selectedRule.campaignId]?.name || `#${selectedRule.campaignId}`}
                        </strong>
                      </div>
                      <div className="col-12 col-md-3">
                        <span className="text-muted d-block">Center:</span>
                        <strong className="text-dark">
                          {centersMap[selectedRule.centerId]?.name || `#${selectedRule.centerId}`}
                        </strong>
                      </div>
                      <div className="col-12 col-md-3">
                        <span className="text-muted d-block">Schedule Window:</span>
                        <strong className="text-dark">
                          {selectedRule.startDate} to {selectedRule.endDate} ({selectedRule.startTime} - {selectedRule.endTime})
                        </strong>
                      </div>
                      <div className="col-12 col-md-3">
                        <span className="text-muted d-block">Timezone & Generation Key:</span>
                        <span className="badge bg-secondary me-2">{selectedRule.timezone}</span>
                        <code className="small text-muted">{selectedRule.generationKey?.substring(0, 8)}...</code>
                      </div>
                    </div>
                  </div>

                  {/* Operation Nav Pills with full tab accessibility */}
                  <ul className="nav nav-pills mb-4 border-bottom pb-3 gap-2" role="tablist" aria-label="Recurrence rule operations">
                    <li className="nav-item" role="presentation">
                      <button
                        type="button"
                        className={`nav-link btn-sm ${activeAction === 'view' ? 'active' : ''}`}
                        onClick={() => setActiveAction('view')}
                        role="tab"
                        id="tab-rule-overview"
                        aria-selected={activeAction === 'view'}
                        aria-controls="panel-rule-action-content"
                      >
                        Overview
                      </button>
                    </li>
                    <li className="nav-item" role="presentation">
                      <button
                        type="button"
                        className={`nav-link btn-sm ${activeAction === 'generate' ? 'active' : ''}`}
                        onClick={() => setActiveAction('generate')}
                        disabled={selectedRule.status === 'CANCELLED'}
                        role="tab"
                        id="tab-generate-occurrences"
                        aria-selected={activeAction === 'generate'}
                        aria-controls="panel-rule-action-content"
                      >
                        Generate Occurrences
                      </button>
                    </li>
                    <li className="nav-item" role="presentation">
                      <button
                        type="button"
                        className={`nav-link btn-sm ${activeAction === 'update_definition' ? 'active' : ''}`}
                        onClick={() => setActiveAction('update_definition')}
                        disabled={selectedRule.status === 'CANCELLED'}
                        role="tab"
                        id="tab-update-definition"
                        aria-selected={activeAction === 'update_definition'}
                        aria-controls="panel-rule-action-content"
                      >
                        Update Future Definition (PUT)
                      </button>
                    </li>
                    <li className="nav-item" role="presentation">
                      <button
                        type="button"
                        className={`nav-link btn-sm ${activeAction === 'series_change' ? 'active' : ''}`}
                        onClick={() => setActiveAction('series_change')}
                        disabled={selectedRule.status === 'CANCELLED'}
                        role="tab"
                        id="tab-series-change"
                        aria-selected={activeAction === 'series_change'}
                        aria-controls="panel-rule-action-content"
                      >
                        Series Change (Preview & Confirm)
                      </button>
                    </li>
                    <li className="nav-item" role="presentation">
                      <button
                        type="button"
                        className={`nav-link btn-sm text-danger ${activeAction === 'series_cancel' ? 'active bg-danger text-white' : ''}`}
                        onClick={() => setActiveAction('series_cancel')}
                        disabled={selectedRule.status === 'CANCELLED'}
                        role="tab"
                        id="tab-series-cancel"
                        aria-selected={activeAction === 'series_cancel'}
                        aria-controls="panel-rule-action-content"
                      >
                        Series Cancellation (Preview & Confirm)
                      </button>
                    </li>
                  </ul>

                  {/* Action Content Container */}
                  <div
                    id="panel-rule-action-content"
                    role="tabpanel"
                    aria-labelledby={`tab-${activeAction === 'view' ? 'rule-overview' : activeAction === 'generate' ? 'generate-occurrences' : activeAction === 'update_definition' ? 'update-definition' : activeAction === 'series_change' ? 'series-change' : 'series-cancel'}`}
                    tabIndex="0"
                  >
                    {/* Tab 1: Overview */}
                    {activeAction === 'view' && (
                      <div>
                        <h4 className="h6 text-dark fw-bold mb-2">Recurrence Configuration Summary</h4>
                        <p className="text-secondary small">
                          This recurrence rule generates operational session instances according to its bounded date range and wall-clock times.
                        </p>
                        <dl className="row mb-0 small">
                          <dt className="col-sm-3 text-muted">Rule ID:</dt>
                          <dd className="col-sm-9">#{selectedRule.id}</dd>

                          <dt className="col-sm-3 text-muted">Active Weekdays:</dt>
                          <dd className="col-sm-9">{selectedRule.weekdays?.join(', ')}</dd>

                          <dt className="col-sm-3 text-muted">Effective Range:</dt>
                          <dd className="col-sm-9">{selectedRule.startDate} to {selectedRule.endDate}</dd>

                          <dt className="col-sm-3 text-muted">Operating Hours:</dt>
                          <dd className="col-sm-9">{selectedRule.startTime} - {selectedRule.endTime}</dd>

                          <dt className="col-sm-3 text-muted">Status:</dt>
                          <dd className="col-sm-9">
                            <span className={`badge ${selectedRule.status === 'CANCELLED' ? 'bg-danger' : 'bg-success'}`}>
                              {selectedRule.status}
                            </span>
                          </dd>
                        </dl>
                      </div>
                    )}

                    {/* Tab 2: Generate Occurrences */}
                    {activeAction === 'generate' && (
                      <div className="border rounded p-3 bg-light">
                        <h4 className="h6 text-dark fw-bold mb-1">Generate Concrete Session Occurrences</h4>
                        <p className="text-secondary small mb-3">
                          Materializes session instances into the operational inventory. Generation is idempotent: existing session records are safely preserved without creating duplicates. Provide both a start and end date to generate a specific window, or leave both empty for the full rule range.
                        </p>

                        {genError && <ApiErrorAlert error={genError} />}

                        {genResult && (
                          <div className="alert alert-success border-0 shadow-sm mb-3" role="status" aria-live="polite">
                            <h5 className="h6 fw-bold mb-1">Generation Complete</h5>
                            <p className="mb-0 small">
                              <strong>{genResult.createdCount}</strong> new session(s) created; <strong>{genResult.existingCount}</strong> existing occurrence(s) recognized.
                            </p>
                          </div>
                        )}

                        <div className="row g-3 align-items-end mb-3">
                          <div className="col-12 col-md-4">
                            <label htmlFor="gen-start-date" className="form-label small fw-semibold text-muted mb-1">
                              Optional Window Start Date
                            </label>
                            <input
                              type="date"
                              id="gen-start-date"
                              className="form-control form-control-sm"
                              value={genStartDate}
                              onChange={(e) => setGenStartDate(e.target.value)}
                              disabled={genLoading}
                            />
                          </div>
                          <div className="col-12 col-md-4">
                            <label htmlFor="gen-end-date" className="form-label small fw-semibold text-muted mb-1">
                              Optional Window End Date
                            </label>
                            <input
                              type="date"
                              id="gen-end-date"
                              className="form-control form-control-sm"
                              value={genEndDate}
                              onChange={(e) => setGenEndDate(e.target.value)}
                              disabled={genLoading}
                            />
                          </div>
                          <div className="col-12 col-md-4">
                            <button
                              type="button"
                              className="btn btn-primary btn-sm w-100"
                              onClick={handleGenerateOccurrences}
                              disabled={genLoading}
                              id="btn-run-generate"
                            >
                              {genLoading ? 'Generating Occurrences...' : 'Run Idempotent Generation'}
                            </button>
                          </div>
                        </div>
                      </div>
                    )}

                    {/* Tab 3: Update Future Rule Definition (PUT) */}
                    {activeAction === 'update_definition' && (
                      <div className="border rounded p-3 bg-light">
                        <h4 className="h6 text-dark fw-bold mb-1">Update Future Rule Definition (Unmaterialized Rules Only)</h4>
                        <p className="text-secondary small mb-3">
                          Directly replaces the recurrence rule definition for an active rule before any session occurrences have been generated. If sessions have already been generated from this rule, use the Series Change workflow instead.
                        </p>

                        {updateError && <ApiErrorAlert error={updateError} />}

                        {updateSuccessMsg && (
                          <div className="alert alert-success border-0 shadow-sm mb-3" role="status" aria-live="polite">
                            {updateSuccessMsg}
                          </div>
                        )}

                        <form onSubmit={handleUpdateDefinition}>
                          <div className="row g-3 mb-3">
                            <div className="col-12">
                              <label id="update-weekdays-label" className="form-label small fw-semibold text-muted d-block mb-1">
                                Weekdays
                              </label>
                              <div className="btn-group flex-wrap w-100" role="group" aria-labelledby="update-weekdays-label">
                                {WEEKDAYS.map((day) => {
                                  const isSel = updateWeekdays.includes(day.id);
                                  return (
                                    <button
                                      key={day.id}
                                      type="button"
                                      className={`btn btn-sm ${isSel ? 'btn-primary text-white fw-bold' : 'btn-outline-secondary'}`}
                                      onClick={() => toggleUpdateWeekday(day.id)}
                                      disabled={updateLoading}
                                    >
                                      {day.label}
                                    </button>
                                  );
                                })}
                              </div>
                            </div>

                            <div className="col-12 col-md-6">
                              <label htmlFor="update-start-date" className="form-label small fw-semibold text-muted mb-1">
                                Start Date
                              </label>
                              <input
                                type="date"
                                id="update-start-date"
                                className="form-control form-control-sm"
                                value={updateStartDate}
                                onChange={(e) => setUpdateStartDate(e.target.value)}
                                disabled={updateLoading}
                              />
                            </div>

                            <div className="col-12 col-md-6">
                              <label htmlFor="update-end-date" className="form-label small fw-semibold text-muted mb-1">
                                End Date
                              </label>
                              <input
                                type="date"
                                id="update-end-date"
                                className="form-control form-control-sm"
                                value={updateEndDate}
                                onChange={(e) => setUpdateEndDate(e.target.value)}
                                disabled={updateLoading}
                              />
                            </div>

                            <div className="col-12 col-md-6">
                              <label htmlFor="update-start-time" className="form-label small fw-semibold text-muted mb-1">
                                Start Time
                              </label>
                              <input
                                type="time"
                                step="1"
                                id="update-start-time"
                                className="form-control form-control-sm"
                                value={updateStartTime}
                                onChange={(e) => setUpdateStartTime(e.target.value)}
                                disabled={updateLoading}
                              />
                            </div>

                            <div className="col-12 col-md-6">
                              <label htmlFor="update-end-time" className="form-label small fw-semibold text-muted mb-1">
                                End Time
                              </label>
                              <input
                                type="time"
                                step="1"
                                id="update-end-time"
                                className="form-control form-control-sm"
                                value={updateEndTime}
                                onChange={(e) => setUpdateEndTime(e.target.value)}
                                disabled={updateLoading}
                              />
                            </div>
                          </div>

                          <button
                            type="submit"
                            className="btn btn-primary btn-sm"
                            disabled={updateLoading}
                            id="btn-submit-update-definition"
                          >
                            {updateLoading ? 'Saving Definition...' : 'Save Definition Update'}
                          </button>
                        </form>
                      </div>
                    )}

                    {/* Tab 4: Series Change Control (Preview -> Confirm) */}
                    {activeAction === 'series_change' && (
                      <div className="border rounded p-3 bg-light">
                        <h4 className="h6 text-dark fw-bold mb-1">Series-Wide Schedule Change Control</h4>
                        <p className="text-secondary small mb-3">
                          Two-step series change control: preview schedule shifts, appointment impacts, and cancellation categorizations across all materialized occurrences before confirming.
                        </p>

                        {changePreviewError && <ApiErrorAlert error={changePreviewError} />}
                        {changeConfirmError && <ApiErrorAlert error={changeConfirmError} />}

                        {changeResultData && (
                          <div className="alert alert-success border-0 shadow-sm mb-3" role="status" aria-live="polite">
                            <h5 className="h6 fw-bold mb-1">Series Change Confirmed & Applied</h5>
                            <p className="mb-0 small">
                              Rescheduled: <strong>{changeResultData.rescheduledCount}</strong> | Cancelled: <strong>{changeResultData.cancelledCount}</strong> | New: <strong>{changeResultData.newCount}</strong> | Preserved: <strong>{changeResultData.preservedCount}</strong> | Audit Events Created: <strong>{changeResultData.eventsCreatedCount}</strong>
                            </p>
                          </div>
                        )}

                        {/* Proposed Schedule Inputs */}
                        <div className="row g-3 mb-3">
                          <div className="col-12">
                            <label id="change-weekdays-label" className="form-label small fw-semibold text-muted d-block mb-1">
                              Proposed Weekdays
                            </label>
                            <div className="btn-group flex-wrap w-100" role="group" aria-labelledby="change-weekdays-label">
                              {WEEKDAYS.map((day) => {
                                const isSel = changeWeekdays.includes(day.id);
                                return (
                                  <button
                                    key={day.id}
                                    type="button"
                                    className={`btn btn-sm ${isSel ? 'btn-primary text-white fw-bold' : 'btn-outline-secondary'}`}
                                    onClick={() => toggleChangeWeekday(day.id)}
                                    disabled={changePreviewLoading || changeConfirmLoading}
                                  >
                                    {day.label}
                                  </button>
                                );
                              })}
                            </div>
                          </div>

                          <div className="col-12 col-md-6">
                            <label htmlFor="change-start-date" className="form-label small fw-semibold text-muted mb-1">
                              Proposed Start Date
                            </label>
                            <input
                              type="date"
                              id="change-start-date"
                              className="form-control form-control-sm"
                              value={changeStartDate}
                              onChange={(e) => {
                                setChangeStartDate(e.target.value);
                                if (changePreviewData) setChangePreviewData(null);
                              }}
                              disabled={changePreviewLoading || changeConfirmLoading}
                            />
                          </div>

                          <div className="col-12 col-md-6">
                            <label htmlFor="change-end-date" className="form-label small fw-semibold text-muted mb-1">
                              Proposed End Date
                            </label>
                            <input
                              type="date"
                              id="change-end-date"
                              className="form-control form-control-sm"
                              value={changeEndDate}
                              onChange={(e) => {
                                setChangeEndDate(e.target.value);
                                if (changePreviewData) setChangePreviewData(null);
                              }}
                              disabled={changePreviewLoading || changeConfirmLoading}
                            />
                          </div>

                          <div className="col-12 col-md-6">
                            <label htmlFor="change-start-time" className="form-label small fw-semibold text-muted mb-1">
                              Proposed Start Time ({centersMap[selectedRule.centerId]?.timezone || selectedRule.timezone})
                            </label>
                            <input
                              type="time"
                              step="1"
                              id="change-start-time"
                              className="form-control form-control-sm"
                              value={changeStartTime}
                              onChange={(e) => {
                                setChangeStartTime(e.target.value);
                                if (changePreviewData) setChangePreviewData(null);
                              }}
                              disabled={changePreviewLoading || changeConfirmLoading}
                            />
                          </div>

                          <div className="col-12 col-md-6">
                            <label htmlFor="change-end-time" className="form-label small fw-semibold text-muted mb-1">
                              Proposed End Time ({centersMap[selectedRule.centerId]?.timezone || selectedRule.timezone})
                            </label>
                            <input
                              type="time"
                              step="1"
                              id="change-end-time"
                              className="form-control form-control-sm"
                              value={changeEndTime}
                              onChange={(e) => {
                                setChangeEndTime(e.target.value);
                                if (changePreviewData) setChangePreviewData(null);
                              }}
                              disabled={changePreviewLoading || changeConfirmLoading}
                            />
                          </div>
                        </div>

                        <div className="d-flex gap-2 mb-4">
                          <button
                            type="button"
                            className="btn btn-outline-primary btn-sm"
                            onClick={handlePreviewSeriesChange}
                            disabled={changePreviewLoading || changeConfirmLoading}
                            id="btn-preview-series-change"
                          >
                            {changePreviewLoading ? 'Calculating Impact...' : 'Step 1: Preview Series Change'}
                          </button>
                        </div>

                        {/* Preview Impact Presentation & Confirmation Box */}
                        {changePreviewData && (
                          <div className="card border-primary shadow-sm mb-3" aria-live="polite">
                            <div className="card-header bg-primary text-white py-2">
                              <h5 className="h6 fw-bold mb-0">Series Change Impact Assessment</h5>
                            </div>
                            <div className="card-body p-3">
                              <div className="row g-3 mb-3 text-center">
                                <div className="col-6 col-md-3">
                                  <div className="p-2 border rounded bg-light">
                                    <span className="small text-muted d-block">Occurrences</span>
                                    <strong className="fs-5">{changePreviewData.occurrenceEntries?.length || 0}</strong>
                                  </div>
                                </div>
                                <div className="col-6 col-md-3">
                                  <div className="p-2 border rounded bg-light">
                                    <span className="small text-muted d-block">Affected Appointments</span>
                                    <strong className="fs-5 text-warning">{changePreviewData.totalAffectedAppointmentsCount}</strong>
                                  </div>
                                </div>
                                <div className="col-6 col-md-3">
                                  <div className="p-2 border rounded bg-light">
                                    <span className="small text-muted d-block">Affected Waitlist</span>
                                    <strong className="fs-5 text-secondary">{changePreviewData.totalAffectedWaitlistCount}</strong>
                                  </div>
                                </div>
                                <div className="col-6 col-md-3">
                                  <div className="p-2 border rounded bg-light">
                                    <span className="small text-muted d-block">Preview Token</span>
                                    <code className="small text-truncate d-block">{changePreviewData.previewToken?.substring(0, 12)}...</code>
                                  </div>
                                </div>
                              </div>

                              {/* Occurrence Breakdown Table */}
                              {changePreviewData.occurrenceEntries?.length > 0 && (
                                <div className="table-responsive mb-3" style={{ maxHeight: '200px' }}>
                                  <table className="table table-sm table-bordered mb-0 small">
                                    <thead className="table-light">
                                      <tr>
                                        <th>Date</th>
                                        <th>Category</th>
                                        <th>Session ID</th>
                                        <th>Appointments</th>
                                        <th>Waitlist</th>
                                      </tr>
                                    </thead>
                                    <tbody>
                                      {changePreviewData.occurrenceEntries.map((occ) => (
                                        <tr key={occ.occurrenceKey}>
                                          <td>{occ.originOccurrenceDate}</td>
                                          <td>
                                            <span className="badge bg-secondary">{occ.category}</span>
                                          </td>
                                          <td>{occ.sessionId ? `#${occ.sessionId}` : '—'}</td>
                                          <td>{occ.affectedAppointmentCount}</td>
                                          <td>{occ.affectedWaitlistCount}</td>
                                        </tr>
                                      ))}
                                    </tbody>
                                  </table>
                                </div>
                              )}

                              {/* Step 2: Reason & Confirm */}
                              <div className="border-top pt-3">
                                <label htmlFor="change-reason" className="form-label small fw-semibold">
                                  Reason for Series Change <span className="text-danger">*</span> (Max 500 chars)
                                </label>
                                <textarea
                                  id="change-reason"
                                  className="form-control form-control-sm mb-3"
                                  rows="2"
                                  placeholder="Explain why this series is being modified (e.g. Center schedule optimization, public holiday schedule)..."
                                  value={changeReason}
                                  onChange={(e) => setChangeReason(e.target.value)}
                                  maxLength={500}
                                  disabled={changeConfirmLoading}
                                />

                                <button
                                  type="button"
                                  ref={changeConfirmButtonRef}
                                  className="btn btn-primary btn-sm"
                                  onClick={handleConfirmSeriesChange}
                                  disabled={changeConfirmLoading || !changeReason.trim()}
                                  id="btn-confirm-series-change"
                                >
                                  {changeConfirmLoading ? 'Applying Changes...' : 'Step 2: Confirm Series Change'}
                                </button>
                              </div>
                            </div>
                          </div>
                        )}
                      </div>
                    )}

                    {/* Tab 5: Series Cancellation (Preview -> Confirm) */}
                    {activeAction === 'series_cancel' && (
                      <div className="border border-danger rounded p-3 bg-light">
                        <h4 className="h6 text-danger fw-bold mb-1">Series-Wide Cancellation Control</h4>
                        <p className="text-secondary small mb-3">
                          Cancels the recurrence rule and all eligible active future occurrences across the campaign. Preserves historical change records and audit provenance.
                        </p>

                        {cancelPreviewError && <ApiErrorAlert error={cancelPreviewError} />}
                        {cancelConfirmError && <ApiErrorAlert error={cancelConfirmError} />}

                        {cancelResultData && (
                          <div className="alert alert-danger border-0 shadow-sm mb-3" role="status" aria-live="polite">
                            <h5 className="h6 fw-bold mb-1">Series Cancellation Applied</h5>
                            <p className="mb-0 small">
                              Cancelled Occurrences: <strong>{cancelResultData.cancelledCount}</strong> | Audit Events Created: <strong>{cancelResultData.eventsCreatedCount}</strong>
                            </p>
                          </div>
                        )}

                        <div className="d-flex gap-2 mb-4">
                          <button
                            type="button"
                            className="btn btn-outline-danger btn-sm"
                            onClick={handlePreviewSeriesCancellation}
                            disabled={cancelPreviewLoading || cancelConfirmLoading}
                            id="btn-preview-series-cancel"
                          >
                            {cancelPreviewLoading ? 'Calculating Impact...' : 'Step 1: Preview Series Cancellation'}
                          </button>
                        </div>

                        {/* Cancellation Impact Assessment */}
                        {cancelPreviewData && (
                          <div className="card border-danger shadow-sm mb-3" aria-live="polite">
                            <div className="card-header bg-danger text-white py-2">
                              <h5 className="h6 fw-bold mb-0">Series Cancellation Impact Assessment</h5>
                            </div>
                            <div className="card-body p-3">
                              <div className="row g-3 mb-3 text-center">
                                <div className="col-6 col-md-3">
                                  <div className="p-2 border rounded bg-light">
                                    <span className="small text-muted d-block">Occurrences to Cancel</span>
                                    <strong className="fs-5 text-danger">{cancellationToCancelCount}</strong>
                                  </div>
                                </div>
                                <div className="col-6 col-md-3">
                                  <div className="p-2 border rounded bg-light">
                                    <span className="small text-muted d-block">Preserved Exceptions</span>
                                    <strong className="fs-5 text-dark">{cancellationPreservedCount}</strong>
                                  </div>
                                </div>
                                <div className="col-6 col-md-3">
                                  <div className="p-2 border rounded bg-light">
                                    <span className="small text-muted d-block">Ineligible / Elapsed</span>
                                    <strong className="fs-5 text-secondary">{cancellationIneligibleCount}</strong>
                                  </div>
                                </div>
                                <div className="col-6 col-md-3">
                                  <div className="p-2 border rounded bg-light">
                                    <span className="small text-muted d-block">Affected Appointments</span>
                                    <strong className="fs-5 text-warning">{cancelPreviewData.totalAffectedAppointmentsCount}</strong>
                                  </div>
                                </div>
                              </div>

                              {/* Step 2: Reason & Confirm */}
                              <div className="border-top pt-3">
                                <label htmlFor="cancel-reason" className="form-label small fw-semibold text-danger">
                                  Cancellation Reason <span className="text-danger">*</span> (Max 500 chars)
                                </label>
                                <textarea
                                  id="cancel-reason"
                                  className="form-control form-control-sm mb-3"
                                  rows="2"
                                  placeholder="Explain why this entire series is being cancelled (e.g. Center emergency closure, campaign scope reduction)..."
                                  value={cancelReason}
                                  onChange={(e) => setCancelReason(e.target.value)}
                                  maxLength={500}
                                  disabled={cancelConfirmLoading}
                                />

                                <button
                                  type="button"
                                  ref={cancelConfirmButtonRef}
                                  className="btn btn-danger btn-sm"
                                  onClick={handleConfirmSeriesCancellation}
                                  disabled={cancelConfirmLoading || !cancelReason.trim()}
                                  id="btn-confirm-series-cancel"
                                >
                                  {cancelConfirmLoading ? 'Cancelling Series...' : 'Step 2: Confirm Series Cancellation'}
                                </button>
                              </div>
                            </div>
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                </div>
              ) : null}
            </div>
          </div>
        )}
      </div>

      {/* Create Recurrence Rule Modal */}
      <CreateRecurrenceRuleModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        onSuccess={handleCreateRuleSuccess}
        campaigns={campaignsList}
        centersMap={centersMap}
        recurrenceOperationOwnerRef={activeOpGateRef}
      />
    </div>
  );
}
