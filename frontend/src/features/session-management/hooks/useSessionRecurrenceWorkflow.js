import { useState, useEffect, useCallback, useRef } from "react";
import { listRecurrenceRules, getRecurrenceRuleById, updateRecurrenceRule, generateRecurrenceOccurrences, previewSeriesChange, confirmSeriesChange, previewSeriesCancellation, confirmSeriesCancellation, isValidIanaTimezone, formatLocalTimeForDto } from "../api/sessionApi";



export default function useSessionRecurrenceWorkflow({ centersMap, recurrenceOperationOwnerRef, recurrenceRefreshGen, onRecurrenceDataChanged, onSessionDataChanged }) {
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


  return {
    rules,
    rulesLoading,
    rulesError,
    isCreateModalOpen,
    setIsCreateModalOpen,
    filters,
    setFilters,
    selectedRuleId,
    selectedRule,
    ruleDetailLoading,
    ruleDetailError,
    activeAction,
    setActiveAction,
    genStartDate,
    setGenStartDate,
    genEndDate,
    setGenEndDate,
    genLoading,
    genError,
    genResult,
    updateWeekdays,
    updateStartDate,
    setUpdateStartDate,
    updateEndDate,
    setUpdateEndDate,
    updateStartTime,
    setUpdateStartTime,
    updateEndTime,
    setUpdateEndTime,
    updateLoading,
    updateError,
    updateSuccessMsg,
    changeWeekdays,
    changeStartDate,
    setChangeStartDate,
    changeEndDate,
    setChangeEndDate,
    changeStartTime,
    setChangeStartTime,
    changeEndTime,
    setChangeEndTime,
    changeReason,
    setChangeReason,
    changePreviewLoading,
    changePreviewError,
    changePreviewData,
    setChangePreviewData,
    changeConfirmLoading,
    changeConfirmError,
    changeResultData,
    cancelReason,
    setCancelReason,
    cancelPreviewLoading,
    cancelPreviewError,
    cancelPreviewData,
    cancelConfirmLoading,
    cancelConfirmError,
    cancelResultData,
    panelSuccessMsg,
    setPanelSuccessMsg,
    changeConfirmButtonRef,
    cancelConfirmButtonRef,
    activeOpGateRef,
    fetchRules,
    handleSelectRule,
    handleGenerateOccurrences,
    handleUpdateDefinition,
    handlePreviewSeriesChange,
    handleConfirmSeriesChange,
    handlePreviewSeriesCancellation,
    handleConfirmSeriesCancellation,
    toggleUpdateWeekday,
    toggleChangeWeekday,
    handleCreateRuleSuccess,
    cancellationToCancelCount,
    cancellationPreservedCount,
    cancellationIneligibleCount
  };
}
