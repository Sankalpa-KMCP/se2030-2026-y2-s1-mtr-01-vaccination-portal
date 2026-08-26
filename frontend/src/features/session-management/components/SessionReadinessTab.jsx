import React, { useState, useEffect, useRef, useCallback } from 'react';
import { getSessionReadiness } from '../api/sessionApi';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import LoadingSpinner from '../../../components/common/LoadingSpinner';

/**
 * Tab component presenting authoritative point-in-time operational readiness diagnostics
 * for a vaccination session.
 *
 * Consumes GET /api/v1/sessions/{id}/readiness directly from backend evaluator.
 */
export default function SessionReadinessTab({ session, onNavigateToTab }) {
  const [readiness, setReadiness] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [lastChecked, setLastChecked] = useState(null);

  const activeSessionIdRef = useRef(session?.id);
  const activeRequestTokenRef = useRef(0);
  const requestSeqRef = useRef(0);
  const isMountedRef = useRef(true);

  const fetchReadiness = useCallback((sessionId) => {
    if (!sessionId) return;
    const reqToken = ++requestSeqRef.current;
    activeRequestTokenRef.current = reqToken;
    setLoading(true);
    setError(null);

    getSessionReadiness(sessionId)
      .then((data) => {
        if (
          isMountedRef.current &&
          activeSessionIdRef.current === sessionId &&
          activeRequestTokenRef.current === reqToken
        ) {
          setReadiness(data);
          setLastChecked(new Date());
          setLoading(false);
        }
      })
      .catch((err) => {
        if (
          isMountedRef.current &&
          activeSessionIdRef.current === sessionId &&
          activeRequestTokenRef.current === reqToken
        ) {
          setError(err);
          setLoading(false);
        }
      });
  }, []);

  useEffect(() => {
    isMountedRef.current = true;
    activeSessionIdRef.current = session?.id;
    setReadiness(null);
    setError(null);
    setLastChecked(null);

    if (session?.id) {
      fetchReadiness(session.id);
    }

    return () => {
      isMountedRef.current = false;
    };
  }, [session?.id, fetchReadiness]);

  const handleRecheck = () => {
    if (loading || !session?.id) return;
    fetchReadiness(session.id);
  };

  const isDraft = session?.lifecycleStatus === 'DRAFT';

  return (
    <div className="d-flex flex-column gap-3" role="tabpanel" aria-label="Readiness & Diagnostics">
      {/* Header & Controls */}
      <div className="d-flex flex-wrap align-items-center justify-content-between gap-2 pb-2 border-bottom">
        <div>
          <h6 className="h6 fw-bold text-dark mb-0">Operational Readiness & Diagnostics</h6>
          <span className="text-muted small">
            Authoritative evaluation of session resources, staff qualifications, and capacity plans.
          </span>
        </div>
        <button
          type="button"
          className="btn btn-sm btn-outline-primary fw-semibold d-flex align-items-center gap-1"
          onClick={handleRecheck}
          disabled={loading || !session?.id}
        >
          {loading && (
            <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true" />
          )}
          <span>Re-check Readiness</span>
        </button>
      </div>

      {/* Snapshot Semantics Notice */}
      <div className="alert alert-info py-2 px-3 mb-0 small border-0 bg-light text-secondary d-flex align-items-center gap-2">
        <span className="fw-semibold text-dark">Point-in-Time Diagnostic Snapshot:</span>
        <span>
          This evaluation reflects live backend configuration at this check. Authoritative validation and
          locking are enforced during session scheduling.
        </span>
      </div>

      {/* API Error Display */}
      {error && (
        <ApiErrorAlert error={error} onDismiss={() => setError(null)} />
      )}

      {/* Loading State */}
      {loading && !readiness && (
        <LoadingSpinner message="Evaluating operational readiness diagnostics..." />
      )}

      {/* Diagnostic Evaluation Content */}
      {!loading && readiness && (
        <div className="d-flex flex-column gap-3">
          {/* Readiness Status Banner */}
          {readiness.ready ? (
            <div className="alert alert-success border-success py-3 px-3 mb-0 d-flex flex-column gap-1" role="status">
              <div className="d-flex align-items-center gap-2">
                <span className="badge bg-success text-white px-2 py-1">READY</span>
                <strong className="text-success h6 mb-0">Current diagnostic: Ready for scheduling</strong>
              </div>
              <p className="small mb-0 text-dark">
                All operational requirements (assigned stations, qualified vaccinators, and approved capacity plan)
                are satisfied at this check. Scheduling will be revalidated when the session is scheduled.
              </p>
            </div>
          ) : (
            <div className="alert alert-warning border-warning py-3 px-3 mb-0 d-flex flex-column gap-1" role="status">
              <div className="d-flex align-items-center gap-2">
                <span className="badge bg-warning text-dark px-2 py-1">NOT READY</span>
                <strong className="text-dark h6 mb-0">Current diagnostic: Not ready for scheduling</strong>
              </div>
              <p className="small mb-0 text-secondary">
                One or more operational prerequisites are missing or invalid. Review the blocking violations below.
              </p>
            </div>
          )}

          {/* Blocking Violations List */}
          {readiness.violations && readiness.violations.length > 0 ? (
            <div className="card border-danger shadow-none">
              <div className="card-header bg-danger bg-opacity-10 py-2 px-3 border-danger">
                <strong className="text-danger small">
                  Blocking Violations ({readiness.violations.length})
                </strong>
              </div>
              <ul className="list-group list-group-flush small">
                {readiness.violations.map((violation, index) => (
                  <li key={index} className="list-group-item d-flex align-items-start gap-2 py-2">
                    <span className="text-danger fw-bold">•</span>
                    <span className="text-dark">{violation}</span>
                  </li>
                ))}
              </ul>
            </div>
          ) : (
            readiness.ready && (
              <div className="card border bg-light shadow-none p-3">
                <span className="text-muted small">No blocking violations detected.</span>
              </div>
            )
          )}

          {/* Operational Metrics Grid */}
          <div className="card border p-3">
            <h6 className="fw-bold text-primary mb-3 small text-uppercase">Diagnostic Operational Metrics</h6>
            <div className="row g-3">
              <div className="col-6 col-md-4">
                <span className="text-muted small d-block">Assigned Vaccinators</span>
                <strong className="h6 mb-0 text-dark">
                  {readiness.assignedVaccinators != null ? readiness.assignedVaccinators : '—'}
                </strong>
              </div>

              <div className="col-6 col-md-4">
                <span className="text-muted small d-block">Available Stations</span>
                <strong className="h6 mb-0 text-dark">
                  {readiness.availableStations != null ? readiness.availableStations : '—'}
                </strong>
              </div>

              <div className="col-6 col-md-4">
                <span className="text-muted small d-block">Effective Stations</span>
                <strong className="h6 mb-0 text-dark">
                  {readiness.effectiveStations != null ? readiness.effectiveStations : '—'}
                </strong>
              </div>

              <div className="col-6 col-md-4">
                <span className="text-muted small d-block">Recommended Throughput</span>
                <strong className="h6 mb-0 text-dark">
                  {readiness.recommendedThroughput != null
                    ? `${readiness.recommendedThroughput} doses`
                    : '—'}
                </strong>
              </div>

              <div className="col-6 col-md-4">
                <span className="text-muted small d-block">Approved Capacity</span>
                <strong className="h6 mb-0 text-dark">
                  {readiness.approvedCapacity != null
                    ? `${readiness.approvedCapacity} doses`
                    : '—'}
                </strong>
              </div>

              <div className="col-6 col-md-4">
                <span className="text-muted small d-block">Capacity Plan Revision</span>
                <strong className="h6 mb-0 text-dark">
                  {readiness.revision != null ? `Rev #${readiness.revision}` : '—'}
                </strong>
              </div>
            </div>
          </div>

          {/* Corrective Navigation (Generic shortcuts for DRAFT sessions) */}
          {isDraft && onNavigateToTab && (
            <div className="card border bg-light p-3">
              <span className="fw-semibold text-dark small mb-2 d-block">Operational Actions & Remediation:</span>
              <div className="d-flex flex-wrap gap-2">
                <button
                  type="button"
                  className="btn btn-sm btn-outline-primary"
                  onClick={() => onNavigateToTab('assignments')}
                >
                  Go to Operational Assignments
                </button>
                <button
                  type="button"
                  className="btn btn-sm btn-outline-primary"
                  onClick={() => onNavigateToTab('capacity')}
                >
                  Go to Capacity Plan
                </button>
                {readiness.ready && (
                  <button
                    type="button"
                    className="btn btn-sm btn-primary fw-semibold"
                    onClick={() => onNavigateToTab('overview')}
                  >
                    Go to Overview (Schedule Session)
                  </button>
                )}
              </div>
            </div>
          )}

          {/* Non-DRAFT Informational Notice */}
          {!isDraft && (
            <div className="card border bg-light p-2">
              <span className="text-muted small">
                Session is currently in <code>{session?.lifecycleStatus}</code> status. Operational readiness diagnostics
                are primarily actionable when configuring a DRAFT session for scheduling.
              </span>
            </div>
          )}

          {/* Diagnostic Timestamp Footer */}
          <div className="d-flex justify-content-between align-items-center text-muted small px-1">
            <span>
              Session ID: <code>#{session?.id}</code>
            </span>
            <span>
              Last evaluated:{' '}
              <code>{lastChecked ? lastChecked.toLocaleTimeString() : '—'}</code>
            </span>
          </div>
        </div>
      )}
    </div>
  );
}
