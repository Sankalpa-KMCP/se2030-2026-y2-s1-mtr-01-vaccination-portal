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
 * Session Capacity Planning tab component (F5B).
 * Supports previewing throughput from operational assignments, approving standard or overridden
 * capacity plans, and inspecting immutable historical revisions.
 *
 * @param {Object} props
 * @param {Object} props.session - Current session object
 */
export default function SessionCapacityPlanTab({ session }) {
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
  const tz = session?.scheduleTimezone || 'UTC';
  const isDraft = session?.lifecycleStatus === 'DRAFT';

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
    // Clear create error on input change
    if (createError) setCreateError(null);
  };

  const handlePreview = async (e) => {
    if (e) e.preventDefault();
    if (!session?.id) return;

    setPreviewError(null);
    setCreateError(null);
    setSuccessMessage(null);

    // Client-side validation for preview inputs
    const fullyEquippedRaw = formData.fullyEquippedStationCount;
    if (fullyEquippedRaw === '' || fullyEquippedRaw === null || isNaN(parseInt(fullyEquippedRaw, 10))) {
      setPreviewError(new Error('Fully equipped station count must be a non-negative integer.'));
      return;
    }
    const fullyEquipped = parseInt(fullyEquippedRaw, 10);
    if (fullyEquipped < 0) {
      setPreviewError(new Error('Fully equipped station count must be non-negative.'));
      return;
    }

    let safeOcc = null;
    if (formData.safeConcurrentOccupancy !== '' && formData.safeConcurrentOccupancy !== null) {
      safeOcc = parseInt(formData.safeConcurrentOccupancy, 10);
      if (isNaN(safeOcc) || safeOcc < 1) {
        setPreviewError(new Error('Safe concurrent occupancy must be a positive integer if specified.'));
        return;
      }
    }

    let dwell = null;
    if (formData.expectedDwellMinutes !== '' && formData.expectedDwellMinutes !== null) {
      dwell = parseInt(formData.expectedDwellMinutes, 10);
      if (isNaN(dwell) || dwell < 1) {
        setPreviewError(new Error('Expected dwell minutes must be a positive integer if specified.'));
        return;
      }
    }

    let accompanying = null;
    if (formData.expectedAccompanyingPersonsPerBooking !== '' && formData.expectedAccompanyingPersonsPerBooking !== null) {
      accompanying = parseFloat(formData.expectedAccompanyingPersonsPerBooking);
      if (isNaN(accompanying) || accompanying < 0.0 || accompanying > 1.0) {
        setPreviewError(new Error('Expected accompanying ratio must be between 0.0 and 1.0.'));
        return;
      }
    }

    let approvedCap = null;
    if (formData.approvedCapacity !== '' && formData.approvedCapacity !== null) {
      approvedCap = parseInt(formData.approvedCapacity, 10);
      if (isNaN(approvedCap) || approvedCap < 0) {
        setPreviewError(new Error('Approved capacity must be a non-negative integer if specified.'));
        return;
      }
    }

    const payload = {
      fullyEquippedStationCount: fullyEquipped,
      safeConcurrentOccupancy: safeOcc,
      expectedDwellMinutes: dwell,
      expectedAccompanyingPersonsPerBooking: accompanying,
      approvedCapacity: approvedCap,
      overrideReason: formData.overrideReason?.trim() || null,
    };

    setLoadingPreview(true);

    try {
      const result = await previewCapacityPlan(session.id, payload);
      setPreviewResult(result);
      setLoadingPreview(false);
    } catch (err) {
      setPreviewError(err);
      setLoadingPreview(false);
    }
  };

  const handleUseRecommendedThroughput = () => {
    if (previewResult?.recommendedThroughput !== undefined) {
      setFormData((prev) => ({
        ...prev,
        approvedCapacity: String(previewResult.recommendedThroughput),
      }));
    }
  };

  const handleCreatePlan = async (e) => {
    if (e) e.preventDefault();
    if (!session?.id) return;

    setCreateError(null);
    setSuccessMessage(null);

    // Client-side validation for creation inputs
    const fullyEquippedRaw = formData.fullyEquippedStationCount;
    if (fullyEquippedRaw === '' || fullyEquippedRaw === null || isNaN(parseInt(fullyEquippedRaw, 10))) {
      setCreateError(new Error('Fully equipped station count is required and must be non-negative.'));
      return;
    }
    const fullyEquipped = parseInt(fullyEquippedRaw, 10);
    if (fullyEquipped < 0) {
      setCreateError(new Error('Fully equipped station count must be non-negative.'));
      return;
    }

    const approvedCapRaw = formData.approvedCapacity;
    if (approvedCapRaw === '' || approvedCapRaw === null || isNaN(parseInt(approvedCapRaw, 10))) {
      setCreateError(new Error('Approved capacity is required to create a capacity plan.'));
      return;
    }
    const approvedCap = parseInt(approvedCapRaw, 10);
    if (approvedCap < 0) {
      setCreateError(new Error('Approved capacity must be a non-negative integer.'));
      return;
    }

    let safeOcc = null;
    if (formData.safeConcurrentOccupancy !== '' && formData.safeConcurrentOccupancy !== null) {
      safeOcc = parseInt(formData.safeConcurrentOccupancy, 10);
      if (isNaN(safeOcc) || safeOcc < 1) {
        setCreateError(new Error('Safe concurrent occupancy must be a positive integer if specified.'));
        return;
      }
    }

    let dwell = null;
    if (formData.expectedDwellMinutes !== '' && formData.expectedDwellMinutes !== null) {
      dwell = parseInt(formData.expectedDwellMinutes, 10);
      if (isNaN(dwell) || dwell < 1) {
        setCreateError(new Error('Expected dwell minutes must be a positive integer if specified.'));
        return;
      }
    }

    let accompanying = null;
    if (formData.expectedAccompanyingPersonsPerBooking !== '' && formData.expectedAccompanyingPersonsPerBooking !== null) {
      accompanying = parseFloat(formData.expectedAccompanyingPersonsPerBooking);
      if (isNaN(accompanying) || accompanying < 0.0 || accompanying > 1.0) {
        setCreateError(new Error('Expected accompanying ratio must be between 0.0 and 1.0.'));
        return;
      }
    }

    const reason = formData.overrideReason?.trim() || null;

    // If previewResult is available, enforce known client-side checks for user convenience
    if (previewResult) {
      if (approvedCap > previewResult.recommendedThroughput && (!reason || reason.length === 0)) {
        setCreateError(new Error('An override reason is required when approved capacity exceeds recommended throughput.'));
        return;
      }
      if (previewResult.sessionServiceCeiling != null && approvedCap > previewResult.sessionServiceCeiling) {
        setCreateError(
          new Error(`Approved capacity (${approvedCap}) cannot exceed the center's session service ceiling (${previewResult.sessionServiceCeiling} doses).`)
        );
        return;
      }
    }

    if (reason && reason.length > 255) {
      setCreateError(new Error('Override reason must not exceed 255 characters.'));
      return;
    }

    // Exact backend payload (approvedBy is omitted and managed authoritatively by backend AUTH-3)
    const payload = {
      fullyEquippedStationCount: fullyEquipped,
      approvedCapacity: approvedCap,
      safeConcurrentOccupancy: safeOcc,
      expectedDwellMinutes: dwell,
      expectedAccompanyingPersonsPerBooking: accompanying,
      overrideReason: reason,
    };

    setSubmittingCreate(true);

    try {
      const created = await createCapacityPlan(session.id, payload);
      setSubmittingCreate(false);
      setSuccessMessage(`Capacity Plan Revision #${created.revision} created and approved successfully.`);
      fetchHistory(session.id);
      setPreviewResult(null);
    } catch (err) {
      setSubmittingCreate(false);
      if (err?.status === 403) {
        setCreateError(new Error('You are not authorized to approve a capacity override. Role ROLE_CENTER_MANAGER is required.'));
      } else {
        setCreateError(err);
      }
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
              {isDraft && ' Configure assumptions below and calculate a preview to create the initial revision.'}
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
      {isDraft && (
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
                    placeholder="Enter planned capacity or preview recommendation"
                    value={formData.approvedCapacity}
                    onChange={handleInputChange}
                  />
                  <div className="form-text small">
                    Total booking capacity approved for this session.
                  </div>
                </div>

                <div className="col-md-6">
                  <label htmlFor="overrideReason" className="form-label fw-semibold small">
                    Capacity Override Reason <span className="text-muted fw-normal">(required if exceeding recommendation)</span>
                  </label>
                  <textarea
                    id="overrideReason"
                    name="overrideReason"
                    rows="2"
                    maxLength="255"
                    className="form-control form-control-sm"
                    placeholder="Provide justification if approved capacity exceeds recommended throughput"
                    value={formData.overrideReason}
                    onChange={handleInputChange}
                  />
                  <div className="form-text small d-flex justify-content-between">
                    <span>Must not exceed 255 characters.</span>
                    <span>{formData.overrideReason.length}/255</span>
                  </div>
                </div>
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
                {formData.approvedCapacity !== '' && parseInt(formData.approvedCapacity, 10) > previewResult.recommendedThroughput && (
                  <div className="alert alert-warning border-0 shadow-sm small mb-3" role="alert">
                    <div className="fw-bold mb-1">⚠️ Capacity Override Detected</div>
                    <div>
                      The target approved capacity (<strong>{formData.approvedCapacity} doses</strong>) exceeds the recommended throughput (<strong>{previewResult.recommendedThroughput} doses</strong>).
                    </div>
                    <ul className="mb-0 mt-1 ps-3">
                      <li>A non-blank <strong>Override Reason</strong> is required.</li>
                      <li>Backend authorization requires authenticated <strong>ROLE_CENTER_MANAGER</strong>.</li>
                      {previewResult.sessionServiceCeiling != null && (
                        <li>Hard session service ceiling (<strong>{previewResult.sessionServiceCeiling} doses</strong>) cannot be exceeded under any circumstances.</li>
                      )}
                    </ul>
                  </div>
                )}

                {/* Hard Ceiling Violation Warning */}
                {formData.approvedCapacity !== '' && previewResult.sessionServiceCeiling != null && parseInt(formData.approvedCapacity, 10) > previewResult.sessionServiceCeiling && (
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
                    disabled={submittingCreate || loadingPreview}
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
