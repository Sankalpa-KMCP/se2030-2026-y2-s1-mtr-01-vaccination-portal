/**
 * Module: MF-02 — Vaccination Campaign, Center and Session Management
 * Use Case: UC-10B — Assign Staff / Resources and Approve Capacity
 * Responsibility: Session staff/resource assignment and capacity planning
 */

import React, { useState, useEffect, useCallback, useRef } from 'react';
import {
  listCapacityPlans,
  previewCapacityPlan,
  createCapacityPlan,
  formatInTimezone,
} from '../api/sessionApi';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import LoadingSpinner from '../../../components/common/LoadingSpinner';

/**
 * Helper to strictly validate and parse integer inputs.
 * Rejects decimals (e.g. "2.5"), exponents (e.g. "1e2"), negative signs, and non-numeric characters.
 *
 * @param {string|number|null|undefined} rawVal - Raw form value
 * @param {Object} options
 * @param {string} options.fieldName - Field display name for error message
 * @param {boolean} [options.required=false] - Whether the field is required
 * @param {number} [options.min=0] - Minimum acceptable integer value
 * @returns {{ error: string|null, value: number|null }}
 */
function parseStrictInteger(rawVal, { fieldName, required = false, min = 0 }) {
  if (rawVal === undefined || rawVal === null || String(rawVal).trim() === '') {
    if (required) {
      return { error: `${fieldName} is required.`, value: null };
    }
    return { error: null, value: null };
  }
  const trimmed = String(rawVal).trim();
  if (!/^\d+$/.test(trimmed)) {
    return { error: `${fieldName} must be a valid non-negative whole integer.`, value: null };
  }
  const num = Number(trimmed);
  if (!Number.isSafeInteger(num)) {
    return { error: `${fieldName} exceeds maximum safe integer range.`, value: null };
  }
  if (num < min) {
    return { error: `${fieldName} must be at least ${min}.`, value: null };
  }
  return { error: null, value: num };
}

/**
 * Helper to strictly validate and parse decimal ratio inputs (0.0 to 1.0).
 *
 * @param {string|number|null|undefined} rawVal - Raw form value
 * @param {Object} options
 * @param {string} options.fieldName - Field display name for error message
 * @returns {{ error: string|null, value: number|null }}
 */
function parseStrictDecimalRatio(rawVal, { fieldName }) {
  if (rawVal === undefined || rawVal === null || String(rawVal).trim() === '') {
    return { error: null, value: null };
  }
  const trimmed = String(rawVal).trim();
  const num = Number(trimmed);
  if (isNaN(num) || !isFinite(num) || num < 0.0 || num > 1.0) {
    return { error: `${fieldName} must be a valid number between 0.0 and 1.0.`, value: null };
  }
  return { error: null, value: num };
}

/**
 * Session Capacity Planning tab component (F5B).
 * Supports previewing throughput from operational assignments, approving standard or overridden
 * capacity plans, and inspecting immutable historical revisions.
 *
 * @param {Object} props
 * @param {Object} props.session - Current session object
 * @param {boolean} props.canManage - Whether the current user can preview/create capacity plans
 * @param {boolean} props.canOverride - Whether the current user can approve above recommendation
 */
export default function SessionCapacityPlanTab({ session, canManage = false, canOverride = false }) {
  const [history, setHistory] = useState([]);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [historyError, setHistoryError] = useState(null);

  const [formData, setFormData] = useState({
    fullyEquippedStationCount: '',
    safeConcurrentOccupancy: '',
    expectedDwellMinutes: '',
    expectedAccompanyingPersonsPerBooking: '',
    approvedCapacity: '',
    overrideReason: '',
  });

  const [previewResult, setPreviewResult] = useState(null);
  const [loadingPreview, setLoadingPreview] = useState(false);
  const [previewError, setPreviewError] = useState(null);

  const [submittingCreate, setSubmittingCreate] = useState(false);
  const [createError, setCreateError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  const activeSessionIdRef = useRef(session?.id);
  const previewGenerationRef = useRef(0);
  const previewInFlightRef = useRef(false);
  const createInFlightRef = useRef(false);
  const previousCanPlanRef = useRef(canManage && session?.lifecycleStatus === 'DRAFT');
  const previousCanOverrideRef = useRef(canOverride && session?.lifecycleStatus === 'DRAFT');

  const tz = session?.scheduleTimezone || 'UTC';
  const isDraft = session?.lifecycleStatus === 'DRAFT';
  const canPlan = canManage && isDraft;
  const canOverridePlan = canOverride && isDraft;

  // Load immutable revision history for session
  const fetchHistory = useCallback((sessionId) => {
    if (!sessionId) return;
    setLoadingHistory(true);
    setHistoryError(null);

    listCapacityPlans(sessionId)
      .then((data) => {
        if (activeSessionIdRef.current === sessionId) {
          setHistory(data || []);
          setLoadingHistory(false);
        }
      })
      .catch((err) => {
        if (activeSessionIdRef.current === sessionId) {
          setHistoryError(err);
          setLoadingHistory(false);
        }
      });
  }, []);

  useEffect(() => {
    activeSessionIdRef.current = session?.id;
    previewGenerationRef.current += 1;
    setPreviewResult(null);
    setPreviewError(null);
    setCreateError(null);
    setSuccessMessage(null);
    if (session?.id) {
      fetchHistory(session.id);
    } else {
      setHistory([]);
    }
  }, [session?.id, fetchHistory]);

  useEffect(() => {
    if (previousCanPlanRef.current && !canPlan) {
      previewGenerationRef.current += 1;
      setPreviewResult(null);
      setPreviewError(null);
      setCreateError(null);
      setSuccessMessage(null);
      setFormData((prev) => ({ ...prev, overrideReason: '' }));
    }
    previousCanPlanRef.current = canPlan;
  }, [canPlan]);

  useEffect(() => {
    if (previousCanOverrideRef.current && !canOverridePlan) {
      previewGenerationRef.current += 1;
      setPreviewResult(null);
      setCreateError(null);
      setFormData((prev) => ({ ...prev, overrideReason: '' }));
    }
    previousCanOverrideRef.current = canOverridePlan;
  }, [canOverridePlan]);

  // Derive latest approved plan deterministically from history
  const latestPlan = history.length > 0
    ? history.reduce((latest, current) => (current.revision > (latest?.revision || 0) ? current : latest), history[history.length - 1])
    : null;

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
    // An override reason changes approval validity, but not the server calculation itself.
    if (name !== 'overrideReason') {
      previewGenerationRef.current += 1;
      setPreviewResult(null);
      setPreviewError(null);
    }
    if (createError) setCreateError(null);
  };

  const handlePreview = async (e) => {
    if (e) e.preventDefault();
    if (!session?.id || !canPlan) return;

    // Synchronous re-entry and cross-operation guards
    if (previewInFlightRef.current || createInFlightRef.current) {
      return;
    }

    setPreviewError(null);
    setCreateError(null);
    setSuccessMessage(null);

    // Strict validation
    const fullyEquippedRes = parseStrictInteger(formData.fullyEquippedStationCount, {
      fieldName: 'Fully equipped station count',
      required: true,
      min: 0,
    });
    if (fullyEquippedRes.error) {
      setPreviewError(new Error(fullyEquippedRes.error));
      return;
    }

    const safeOccRes = parseStrictInteger(formData.safeConcurrentOccupancy, {
      fieldName: 'Safe concurrent occupancy',
      required: false,
      min: 1,
    });
    if (safeOccRes.error) {
      setPreviewError(new Error(safeOccRes.error));
      return;
    }

    const dwellRes = parseStrictInteger(formData.expectedDwellMinutes, {
      fieldName: 'Expected dwell minutes',
      required: false,
      min: 1,
    });
    if (dwellRes.error) {
      setPreviewError(new Error(dwellRes.error));
      return;
    }

    const accompanyingRes = parseStrictDecimalRatio(formData.expectedAccompanyingPersonsPerBooking, {
      fieldName: 'Expected accompanying ratio',
    });
    if (accompanyingRes.error) {
      setPreviewError(new Error(accompanyingRes.error));
      return;
    }

    const approvedCapRes = parseStrictInteger(formData.approvedCapacity, {
      fieldName: 'Approved capacity',
      required: false,
      min: 0,
    });
    if (approvedCapRes.error) {
      setPreviewError(new Error(approvedCapRes.error));
      return;
    }

    const payload = {
      fullyEquippedStationCount: fullyEquippedRes.value,
      safeConcurrentOccupancy: safeOccRes.value,
      expectedDwellMinutes: dwellRes.value,
      expectedAccompanyingPersonsPerBooking: accompanyingRes.value,
      approvedCapacity: approvedCapRes.value,
      overrideReason: canOverridePlan ? formData.overrideReason?.trim() || null : null,
    };

    const currentGeneration = ++previewGenerationRef.current;
    previewInFlightRef.current = true;
    setLoadingPreview(true);

    try {
      const result = await previewCapacityPlan(session.id, payload);
      // Discard response if generation or active session changed in-flight
      if (previewGenerationRef.current === currentGeneration && activeSessionIdRef.current === session.id) {
        setPreviewResult(result);
      }
    } catch (err) {
      if (previewGenerationRef.current === currentGeneration && activeSessionIdRef.current === session.id) {
        setPreviewError(err);
      }
    } finally {
      previewInFlightRef.current = false;
      setLoadingPreview(false);
    }
  };

  const handleUseRecommendedThroughput = () => {
    if (previewResult?.recommendedThroughput !== undefined) {
      const recVal = String(previewResult.recommendedThroughput);
      setFormData((prev) => ({
        ...prev,
        approvedCapacity: recVal,
      }));
      setPreviewResult((prev) => (prev ? {
        ...prev,
        projectedApprovedCapacity: previewResult.recommendedThroughput,
        overrideRequired: false,
        overrideValid: true,
      } : null));
    }
  };

  const handleCreatePlan = async (e) => {
    if (e) e.preventDefault();
    if (!session?.id) return;

    if (!canPlan) {
      setCreateError(new Error('You are not authorized to create a capacity plan for this session.'));
      return;
    }

    // Synchronous re-entry and cross-operation guards
    if (createInFlightRef.current || previewInFlightRef.current) {
      return;
    }

    setCreateError(null);
    setSuccessMessage(null);

    // Require current valid preview before creating plan
    if (!previewResult) {
      setCreateError(new Error('Please preview the capacity plan with current values before approving and creating.'));
      return;
    }

    // Strict validation for create inputs
    const fullyEquippedRes = parseStrictInteger(formData.fullyEquippedStationCount, {
      fieldName: 'Fully equipped station count',
      required: true,
      min: 0,
    });
    if (fullyEquippedRes.error) {
      setCreateError(new Error(fullyEquippedRes.error));
      return;
    }

    const approvedCapRes = parseStrictInteger(formData.approvedCapacity, {
      fieldName: 'Approved capacity',
      required: true,
      min: 0,
    });
    if (approvedCapRes.error) {
      setCreateError(new Error(approvedCapRes.error));
      return;
    }

    const safeOccRes = parseStrictInteger(formData.safeConcurrentOccupancy, {
      fieldName: 'Safe concurrent occupancy',
      required: false,
      min: 1,
    });
    if (safeOccRes.error) {
      setCreateError(new Error(safeOccRes.error));
      return;
    }

    const dwellRes = parseStrictInteger(formData.expectedDwellMinutes, {
      fieldName: 'Expected dwell minutes',
      required: false,
      min: 1,
    });
    if (dwellRes.error) {
      setCreateError(new Error(dwellRes.error));
      return;
    }

    const accompanyingRes = parseStrictDecimalRatio(formData.expectedAccompanyingPersonsPerBooking, {
      fieldName: 'Expected accompanying ratio',
    });
    if (accompanyingRes.error) {
      setCreateError(new Error(accompanyingRes.error));
      return;
    }

    const reason = formData.overrideReason?.trim() || null;
    const approvedCap = approvedCapRes.value;
    const overrideRequired = approvedCap > previewResult.recommendedThroughput;

    // Enforce current preview constraints
    if (overrideRequired && !canOverridePlan) {
      setCreateError(new Error('Capacity above the recommendation requires ROLE_CENTER_MANAGER authorization.'));
      return;
    }
    if (overrideRequired && (!reason || reason.length === 0)) {
      setCreateError(new Error('An override reason is required when approved capacity exceeds recommended throughput.'));
      return;
    }
    if (previewResult.sessionServiceCeiling != null && approvedCap > previewResult.sessionServiceCeiling) {
      setCreateError(
        new Error(`Approved capacity (${approvedCap}) cannot exceed the center's session service ceiling (${previewResult.sessionServiceCeiling} doses).`)
      );
      return;
    }

    if (reason && reason.length > 255) {
      setCreateError(new Error('Override reason must not exceed 255 characters.'));
      return;
    }

    // Exact backend payload (approvedBy is omitted and managed authoritatively by backend AUTH-3)
    const payload = {
      fullyEquippedStationCount: fullyEquippedRes.value,
      approvedCapacity: approvedCap,
      safeConcurrentOccupancy: safeOccRes.value,
      expectedDwellMinutes: dwellRes.value,
      expectedAccompanyingPersonsPerBooking: accompanyingRes.value,
      overrideReason: canOverridePlan ? reason : null,
    };

    createInFlightRef.current = true;
    setSubmittingCreate(true);

    try {
      const created = await createCapacityPlan(session.id, payload);
      setSuccessMessage(`Capacity Plan Revision #${created.revision} created and approved successfully.`);
      previewGenerationRef.current += 1;
      setPreviewResult(null);
      fetchHistory(session.id);
    } catch (err) {
      if (err?.status === 403) {
        setCreateError(new Error('You are not authorized to approve a capacity override. Role ROLE_CENTER_MANAGER is required.'));
      } else {
        setCreateError(err);
      }
    } finally {
      createInFlightRef.current = false;
      setSubmittingCreate(false);
    }
  };

  return (
    <div className="d-flex flex-column gap-4">
      {/* Top Alerts */}
      {successMessage && (
        <div className="alert alert-success alert-dismissible fade show border-0 shadow-sm mb-0" role="status">
          <strong>Success:</strong> {successMessage}
          <button
            type="button"
            className="btn-close"
            aria-label="Close"
            onClick={() => setSuccessMessage(null)}
          />
        </div>
      )}

      {createError && <ApiErrorAlert error={createError} onDismiss={() => setCreateError(null)} />}
      {previewError && <ApiErrorAlert error={previewError} onDismiss={() => setPreviewError(null)} />}
      {historyError && <ApiErrorAlert error={historyError} />}

      {/* Operational Guidance */}
      <div className="alert alert-info py-2 px-3 small d-flex flex-column flex-md-row align-items-md-center justify-content-between gap-2 mb-0">
        <div>
          <strong>Operational Throughput Basis:</strong> Recommended throughput is authoritatively calculated from effective vaccination stations (constrained by assigned active vaccinators, available stations, and fully equipped stations) over the session duration.
        </div>
        <div className="text-nowrap">
          <span className="badge bg-primary">Backend Authoritative</span>
        </div>
      </div>

      {!isDraft && (
        <div className="alert alert-secondary py-2 small mb-0">
          Capacity plans are read-only because session is in <strong>{session?.lifecycleStatus}</strong> status. Revisions can only be planned and approved for sessions in DRAFT status.
        </div>
      )}

      {isDraft && !canManage && (
        <div className="alert alert-secondary py-2 small mb-0">
          Your role has read-only access to capacity plan history.
        </div>
      )}

      {/* Section 1: Latest Approved Plan Summary */}
      <div className="card border">
        <div className="card-header bg-light d-flex align-items-center justify-content-between py-2 px-3">
          <div className="d-flex align-items-center gap-2">
            <h6 className="fw-bold text-dark mb-0">Current Approved Capacity Plan</h6>
            {latestPlan && (
              <span className="badge bg-success">Revision #{latestPlan.revision}</span>
            )}
          </div>
          {latestPlan?.approvedCapacity > latestPlan?.recommendedThroughput && (
            <span className="badge bg-warning text-dark">Capacity Override</span>
          )}
        </div>

        <div className="card-body p-3">
          {loadingHistory && <LoadingSpinner message="Loading capacity plan history..." />}

          {!loadingHistory && !latestPlan && (
            <div className="text-center text-muted py-4 small">
              No approved capacity plan has been established for this session yet.
              {canPlan && ' Configure assumptions below and calculate a preview to create the initial revision.'}
            </div>
          )}

          {!loadingHistory && latestPlan && (
            <div className="row g-3">
              <div className="col-sm-6 col-md-3">
                <div className="p-3 bg-light rounded border text-center">
                  <span className="text-muted small d-block">Approved Capacity</span>
                  <span className="h4 fw-bold text-primary mb-0">{latestPlan.approvedCapacity}</span>
                  <span className="text-muted small d-block">doses</span>
                </div>
              </div>

              <div className="col-sm-6 col-md-3">
                <div className="p-3 bg-light rounded border text-center">
                  <span className="text-muted small d-block">Recommended Throughput</span>
                  <span className="h4 fw-bold text-dark mb-0">{latestPlan.recommendedThroughput}</span>
                  <span className="text-muted small d-block">doses</span>
                </div>
              </div>

              <div className="col-sm-6 col-md-3">
                <div className="p-3 bg-light rounded border text-center">
                  <span className="text-muted small d-block">Effective Stations</span>
                  <span className="h4 fw-bold text-dark mb-0">{latestPlan.effectiveStationCount}</span>
                  <span className="text-muted small d-block">
                    {latestPlan.assignedVaccinatorCount} vacc. / {latestPlan.availableStationCount} stn.
                  </span>
                </div>
              </div>

              <div className="col-sm-6 col-md-3">
                <div className="p-3 bg-light rounded border text-center">
                  <span className="text-muted small d-block">Hard Service Ceiling</span>
                  <span className="h4 fw-bold text-secondary mb-0">
                    {latestPlan.sessionServiceCeiling != null ? latestPlan.sessionServiceCeiling : '—'}
                  </span>
                  <span className="text-muted small d-block">max session doses</span>
                </div>
              </div>

              <div className="col-12">
                <div className="table-responsive">
                  <table className="table table-sm table-bordered mb-0 small">
                    <thead className="table-light">
                      <tr>
                        <th scope="col">Duration</th>
                        <th scope="col">Service Pace</th>
                        <th scope="col">Fully Equipped Stations</th>
                        <th scope="col">Safe Concurrent Occupancy</th>
                        <th scope="col">Expected Dwell</th>
                        <th scope="col">Accompanying Ratio</th>
                        <th scope="col">Approved By</th>
                        <th scope="col">Approval Timestamp</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr>
                        <td>{latestPlan.durationMinutes} min</td>
                        <td>{latestPlan.expectedServiceMinutes} min/dose</td>
                        <td>{latestPlan.fullyEquippedStationCount}</td>
                        <td>{latestPlan.safeConcurrentOccupancy} persons</td>
                        <td>{latestPlan.expectedDwellMinutes} min</td>
                        <td>{latestPlan.expectedAccompanyingPersonsPerBooking}</td>
                        <td>
                          {latestPlan.approvedBy ? (
                            <code>Account #{latestPlan.approvedBy}</code>
                          ) : (
                            <span className="text-muted">Anonymous / System</span>
                          )}
                        </td>
                        <td>{formatInTimezone(latestPlan.createdAt, tz)}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>

              {latestPlan.overrideReason && (
                <div className="col-12">
                  <div className="p-2 bg-warning-subtle border border-warning rounded small">
                    <strong>Override Justification:</strong> {latestPlan.overrideReason}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Section 2: Capacity Planning & Preview Form */}
      {canPlan && (
        <div className="card border">
          <div className="card-header bg-light py-2 px-3">
            <h6 className="fw-bold text-dark mb-0">Plan & Preview Capacity</h6>
          </div>

          <div className="card-body p-3">
            <form onSubmit={handlePreview} noValidate>
              <div className="row g-3">
                <div className="col-md-6">
                  <label htmlFor="fullyEquippedStationCount" className="form-label fw-semibold small">
                    Fully Equipped Stations <span className="text-danger">*</span>
                  </label>
                  <input
                    type="number"
                    id="fullyEquippedStationCount"
                    name="fullyEquippedStationCount"
                    className="form-control form-control-sm"
                    min="0"
                    step="1"
                    placeholder="e.g. 2"
                    value={formData.fullyEquippedStationCount}
                    onChange={handleInputChange}
                    required
                  />
                  <div className="form-text small">
                    Number of active stations fully provisioned with vaccine supplies and equipment.
                  </div>
                </div>

                <div className="col-md-6">
                  <label htmlFor="safeConcurrentOccupancy" className="form-label fw-semibold small">
                    Safe Concurrent Occupancy <span className="text-muted fw-normal">(optional)</span>
                  </label>
                  <input
                    type="number"
                    id="safeConcurrentOccupancy"
                    name="safeConcurrentOccupancy"
                    className="form-control form-control-sm"
                    min="1"
                    step="1"
                    placeholder="Hosting center default"
                    value={formData.safeConcurrentOccupancy}
                    onChange={handleInputChange}
                  />
                  <div className="form-text small">
                    Leave blank to use hosting center default limit.
                  </div>
                </div>

                <div className="col-md-6">
                  <label htmlFor="expectedDwellMinutes" className="form-label fw-semibold small">
                    Expected Dwell Time (minutes) <span className="text-muted fw-normal">(optional)</span>
                  </label>
                  <input
                    type="number"
                    id="expectedDwellMinutes"
                    name="expectedDwellMinutes"
                    className="form-control form-control-sm"
                    min="1"
                    step="1"
                    placeholder="Default: 15 min"
                    value={formData.expectedDwellMinutes}
                    onChange={handleInputChange}
                  />
                  <div className="form-text small">
                    Estimated on-site time per citizen including post-vaccination observation.
                  </div>
                </div>

                <div className="col-md-6">
                  <label htmlFor="expectedAccompanyingPersonsPerBooking" className="form-label fw-semibold small">
                    Accompanying Persons Per Booking <span className="text-muted fw-normal">(0.0 - 1.0)</span>
                  </label>
                  <input
                    type="number"
                    id="expectedAccompanyingPersonsPerBooking"
                    name="expectedAccompanyingPersonsPerBooking"
                    className="form-control form-control-sm"
                    min="0.0"
                    max="1.0"
                    step="0.05"
                    placeholder="Default: 0.0"
                    value={formData.expectedAccompanyingPersonsPerBooking}
                    onChange={handleInputChange}
                  />
                  <div className="form-text small">
                    Ratio of companions per citizen booking (e.g. 0.25 = 1 companion per 4 citizens).
                  </div>
                </div>

                <div className="col-md-6">
                  <label htmlFor="approvedCapacity" className="form-label fw-semibold small">
                    Target Approved Capacity <span className="text-danger">*</span>
                  </label>
                  <input
                    type="number"
                    id="approvedCapacity"
                    name="approvedCapacity"
                    className="form-control form-control-sm"
                    min="0"
                    step="1"
                    placeholder="Enter planned capacity or preview recommendation"
                    value={formData.approvedCapacity}
                    onChange={handleInputChange}
                  />
                  <div className="form-text small">
                    Total booking capacity approved for this session.
                  </div>
                </div>

                {canOverridePlan && previewResult &&
                  parseStrictInteger(formData.approvedCapacity, { fieldName: 'Approved capacity' }).value > previewResult.recommendedThroughput && (
                  <div className="col-md-6">
                    <label htmlFor="overrideReason" className="form-label fw-semibold small">
                      Capacity Override Reason <span className="text-muted fw-normal">(required)</span>
                    </label>
                    <textarea
                      id="overrideReason"
                      name="overrideReason"
                      rows="2"
                      maxLength="255"
                      className="form-control form-control-sm"
                      placeholder="Provide justification for exceeding recommended throughput"
                      value={formData.overrideReason}
                      onChange={handleInputChange}
                    />
                    <div className="form-text small d-flex justify-content-between">
                      <span>Must not exceed 255 characters.</span>
                      <span>{formData.overrideReason.length}/255</span>
                    </div>
                  </div>
                )}
              </div>

              <div className="d-flex align-items-center gap-2 mt-3">
                <button
                  type="submit"
                  className="btn btn-sm btn-outline-primary d-inline-flex align-items-center gap-2"
                  disabled={loadingPreview || submittingCreate}
                >
                  {loadingPreview ? (
                    <>
                      <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true" />
                      Calculating Preview...
                    </>
                  ) : (
                    'Preview Calculation'
                  )}
                </button>
              </div>
            </form>

            {/* Preview Results Display */}
            {previewResult && (
              <div className="mt-4 pt-3 border-top">
                <div className="d-flex align-items-center justify-content-between mb-3">
                  <h6 className="fw-bold text-dark mb-0">Server Calculation Preview</h6>
                  <span className="text-muted small">
                    Calculated: {previewResult.calculatedAt ? new Date(previewResult.calculatedAt).toLocaleTimeString() : 'Just now'}
                  </span>
                </div>

                <div className="row g-3 mb-3">
                  <div className="col-md-4">
                    <div className="p-3 bg-primary-subtle border border-primary-subtle rounded text-center">
                      <span className="text-muted small d-block">Recommended Throughput</span>
                      <span className="h4 fw-bold text-primary mb-0">{previewResult.recommendedThroughput}</span>
                      <span className="text-muted small d-block">doses</span>
                    </div>
                  </div>

                  <div className="col-md-4">
                    <div className="p-3 bg-light border rounded text-center">
                      <span className="text-muted small d-block">Effective Stations</span>
                      <span className="h4 fw-bold text-dark mb-0">{previewResult.effectiveStationCount}</span>
                      <span className="text-muted small d-block">
                        min({previewResult.assignedVaccinatorCount} vacc, {previewResult.availableStationCount} avail, {previewResult.fullyEquippedStationCount} equipped)
                      </span>
                    </div>
                  </div>

                  <div className="col-md-4">
                    <div className="p-3 bg-light border rounded text-center">
                      <span className="text-muted small d-block">Service Ceiling</span>
                      <span className="h4 fw-bold text-secondary mb-0">
                        {previewResult.sessionServiceCeiling != null ? previewResult.sessionServiceCeiling : 'None'}
                      </span>
                      <span className="text-muted small d-block">center hard limit</span>
                    </div>
                  </div>
                </div>

                {/* Override Condition Alert */}
                {parseStrictInteger(formData.approvedCapacity, { fieldName: 'Approved capacity' }).value != null &&
                  parseStrictInteger(formData.approvedCapacity, { fieldName: 'Approved capacity' }).value > previewResult.recommendedThroughput && (
                  <div className={`alert ${canOverridePlan ? 'alert-warning' : 'alert-danger'} border-0 shadow-sm small mb-3`} role="alert">
                    <div className="fw-bold mb-1">
                      <i className="bi bi-exclamation-triangle me-1" aria-hidden="true"></i>
                      Capacity Override Detected
                    </div>
                    <div>
                      The target approved capacity (<strong>{formData.approvedCapacity} doses</strong>) exceeds the recommended throughput (<strong>{previewResult.recommendedThroughput} doses</strong>).
                    </div>
                    <ul className="mb-0 mt-1 ps-3">
                      {canOverridePlan ? (
                        <li>A non-blank <strong>Override Reason</strong> is required.</li>
                      ) : (
                        <li>You cannot approve this value. A <strong>CENTER_MANAGER</strong> must authorize capacity above the recommendation.</li>
                      )}
                      {previewResult.sessionServiceCeiling != null && (
                        <li>Hard session service ceiling (<strong>{previewResult.sessionServiceCeiling} doses</strong>) cannot be exceeded under any circumstances.</li>
                      )}
                    </ul>
                  </div>
                )}

                {/* Hard Ceiling Violation Warning */}
                {parseStrictInteger(formData.approvedCapacity, { fieldName: 'Approved capacity' }).value != null &&
                  previewResult.sessionServiceCeiling != null &&
                  parseStrictInteger(formData.approvedCapacity, { fieldName: 'Approved capacity' }).value > previewResult.sessionServiceCeiling && (
                  <div className="alert alert-danger border-0 shadow-sm small mb-3" role="alert">
                    <strong>Hard Ceiling Exceeded:</strong> Target capacity ({formData.approvedCapacity}) exceeds the center's maximum service ceiling of {previewResult.sessionServiceCeiling} doses. The backend will reject this request.
                  </div>
                )}

                <div className="d-flex flex-wrap align-items-center gap-2">
                  <button
                    type="button"
                    className="btn btn-sm btn-secondary"
                    onClick={handleUseRecommendedThroughput}
                  >
                    Set Approved Capacity = {previewResult.recommendedThroughput} (Recommended)
                  </button>

                  <button
                    type="button"
                    className="btn btn-sm btn-primary d-inline-flex align-items-center gap-2"
                    onClick={handleCreatePlan}
                    disabled={submittingCreate || loadingPreview || (!canOverridePlan &&
                      parseStrictInteger(formData.approvedCapacity, { fieldName: 'Approved capacity' }).value > previewResult.recommendedThroughput)}
                  >
                    {submittingCreate ? (
                      <>
                        <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true" />
                        Approving & Persisting...
                      </>
                    ) : (
                      'Approve & Create Capacity Plan'
                    )}
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Section 3: Revision History Table */}
      <div className="card border">
        <div className="card-header bg-light d-flex align-items-center justify-content-between py-2 px-3">
          <div className="d-flex align-items-center gap-2">
            <h6 className="fw-bold text-dark mb-0">Immutable Revision History</h6>
            <span className="badge bg-secondary">{history.length}</span>
          </div>
        </div>

        <div className="card-body p-0">
          {!loadingHistory && history.length === 0 && (
            <div className="text-center text-muted py-4 small">
              No historical revisions found.
            </div>
          )}

          {!loadingHistory && history.length > 0 && (
            <div className="table-responsive">
              <table className="table table-hover table-striped align-middle mb-0" style={{ fontSize: '0.875rem' }}>
                <thead className="table-light">
                  <tr>
                    <th scope="col" style={{ width: '8%' }}>Rev #</th>
                    <th scope="col" style={{ width: '15%' }}>Approved Capacity</th>
                    <th scope="col" style={{ width: '15%' }}>Recommended</th>
                    <th scope="col" style={{ width: '15%' }}>Operational Basis</th>
                    <th scope="col" style={{ width: '17%' }}>Override Status</th>
                    <th scope="col" style={{ width: '15%' }}>Approved By</th>
                    <th scope="col" style={{ width: '15%' }}>Timestamp</th>
                  </tr>
                </thead>
                <tbody>
                  {history.map((p) => {
                    const isOverride = p.approvedCapacity > p.recommendedThroughput;
                    return (
                      <tr key={p.id || p.revision}>
                        <td className="fw-bold font-monospace">#{p.revision}</td>
                        <td className="fw-bold text-primary">{p.approvedCapacity} doses</td>
                        <td>{p.recommendedThroughput} doses</td>
                        <td className="small">
                          {p.effectiveStationCount} stn. ({p.assignedVaccinatorCount} vacc. / {p.availableStationCount} avail. / {p.fullyEquippedStationCount} eq.)
                        </td>
                        <td>
                          {isOverride ? (
                            <div>
                              <span className="badge bg-warning text-dark me-1">OVERRIDE</span>
                              {p.overrideReason && (
                                <div className="text-muted small text-truncate" style={{ maxWidth: '200px' }} title={p.overrideReason}>
                                  {p.overrideReason}
                                </div>
                              )}
                            </div>
                          ) : (
                            <span className="badge bg-light text-secondary border">STANDARD</span>
                          )}
                        </td>
                        <td>
                          {p.approvedBy ? (
                            <code className="text-dark">Account #{p.approvedBy}</code>
                          ) : (
                            <span className="text-muted small">Anonymous / System</span>
                          )}
                        </td>
                        <td className="small text-muted">{formatInTimezone(p.createdAt, tz)}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
