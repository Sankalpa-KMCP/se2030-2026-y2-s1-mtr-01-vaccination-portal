import React, { useState } from 'react';
import LoadingSpinner from '../../../components/common/LoadingSpinner';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';

function formatDateTime(instantStr) {
  if (!instantStr) return '—';
  try {
    const d = new Date(instantStr);
    return isNaN(d.getTime()) ? instantStr : d.toLocaleString();
  } catch {
    return instantStr;
  }
}

function getClosureStatusBadge(closure) {
  if (closure.status === 'CANCELLED') {
    return <span className="badge bg-danger-subtle text-danger-emphasis border border-danger-subtle">Cancelled</span>;
  }

  const now = new Date().toISOString();
  if (closure.endAt < now) {
    return <span className="badge bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle">Completed / Past</span>;
  }
  if (closure.startAt <= now && closure.endAt >= now) {
    return <span className="badge bg-warning text-dark border">In Progress (Active)</span>;
  }
  return <span className="badge bg-primary-subtle text-primary-emphasis border border-primary-subtle">Scheduled (Future)</span>;
}

export default function CenterClosuresPanel({
  center,
  closures = [],
  loading = false,
  error = null,
  isAuthorized = false,
  onRefresh,
  onAddClosure,
  onEditClosure,
  onCancelClosure,
}) {
  const [filter, setFilter] = useState('ALL'); // 'ALL' | 'ACTIVE_FUTURE' | 'PAST' | 'CANCELLED'
  const [confirmCancelClosure, setConfirmCancelClosure] = useState(null);
  const [cancelLoading, setCancelLoading] = useState(false);
  const [cancelError, setCancelError] = useState(null);

  const isCenterInactive = center?.status === 'INACTIVE';
  const canMutate = isAuthorized && !isCenterInactive;
  const nowIso = new Date().toISOString();

  const filteredClosures = closures.filter((c) => {
    if (filter === 'ALL') return true;
    if (filter === 'CANCELLED') return c.status === 'CANCELLED';
    if (filter === 'PAST') return c.status !== 'CANCELLED' && c.endAt < nowIso;
    if (filter === 'ACTIVE_FUTURE') return c.status !== 'CANCELLED' && c.endAt >= nowIso;
    return true;
  });

  const handleConfirmCancel = async () => {
    if (!confirmCancelClosure) return;
    setCancelLoading(true);
    setCancelError(null);
    try {
      await onCancelClosure(confirmCancelClosure.id);
      setConfirmCancelClosure(null);
    } catch (err) {
      setCancelError(err);
    } finally {
      setCancelLoading(false);
    }
  };

  return (
    <div className="card shadow-sm border-0">
      <div className="card-header bg-white py-3 border-bottom d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-2">
        <div>
          <h2 className="h5 mb-1 text-primary fw-bold">
            <i className="bi bi-calendar-x me-2"></i>
            Temporary Center Closures & Maintenance
          </h2>
          <p className="small text-muted mb-0">
            Emergency, holiday, and scheduled facility closures that block session occurrences and prevent citizen bookings for {center?.name}.
          </p>
        </div>

        <div className="d-flex gap-2">
          <button
            type="button"
            className="btn btn-sm btn-outline-secondary"
            onClick={onRefresh}
            disabled={loading}
            title="Refresh closure records"
          >
            <i className="bi bi-arrow-clockwise me-1"></i>
            Refresh
          </button>

          {canMutate && (
            <button
              type="button"
              className="btn btn-sm btn-primary fw-semibold"
              onClick={onAddClosure}
              disabled={loading}
            >
              <i className="bi bi-plus-lg me-1"></i>
              Schedule Closure
            </button>
          )}
        </div>
      </div>

      <div className="card-body p-4">
        <ApiErrorAlert error={error || cancelError} onDismiss={() => setCancelError(null)} />

        {/* Filter Tabs */}
        <div className="d-flex flex-wrap gap-2 mb-3">
          <button
            type="button"
            className={`btn btn-sm ${filter === 'ALL' ? 'btn-secondary text-white' : 'btn-outline-secondary'}`}
            onClick={() => setFilter('ALL')}
          >
            All Closures ({closures.length})
          </button>
          <button
            type="button"
            className={`btn btn-sm ${filter === 'ACTIVE_FUTURE' ? 'btn-primary text-white' : 'btn-outline-secondary'}`}
            onClick={() => setFilter('ACTIVE_FUTURE')}
          >
            Scheduled & Active ({closures.filter((c) => c.status !== 'CANCELLED' && c.endAt >= nowIso).length})
          </button>
          <button
            type="button"
            className={`btn btn-sm ${filter === 'PAST' ? 'btn-primary text-white' : 'btn-outline-secondary'}`}
            onClick={() => setFilter('PAST')}
          >
            Completed Past ({closures.filter((c) => c.status !== 'CANCELLED' && c.endAt < nowIso).length})
          </button>
          <button
            type="button"
            className={`btn btn-sm ${filter === 'CANCELLED' ? 'btn-danger text-white' : 'btn-outline-danger'}`}
            onClick={() => setFilter('CANCELLED')}
          >
            Cancelled ({closures.filter((c) => c.status === 'CANCELLED').length})
          </button>
        </div>

        {loading ? (
          <div className="py-5">
            <LoadingSpinner message="Loading center closure records..." />
          </div>
        ) : closures.length === 0 ? (
          <div className="text-center py-5 text-secondary border rounded bg-light">
            <div className="display-6 text-muted mb-3">🏥</div>
            <h5 className="h6 text-muted mb-2">No Closures Recorded</h5>
            <p className="small text-muted mb-3">
              This vaccination center has no scheduled or historical closures. Operations are fully available according to operating hours.
            </p>
            {canMutate && (
              <button
                type="button"
                className="btn btn-primary btn-sm fw-semibold"
                onClick={onAddClosure}
              >
                <i className="bi bi-plus-lg me-1"></i>
                Schedule First Temporary Closure
              </button>
            )}
          </div>
        ) : filteredClosures.length === 0 ? (
          <div className="alert alert-light border text-center py-4">
            <p className="text-muted mb-0 small">
              No closure records matching the selected filter.
            </p>
          </div>
        ) : (
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead className="table-light text-secondary small">
                <tr>
                  <th scope="col">Closure Period</th>
                  <th scope="col">Reason / Justification</th>
                  <th scope="col">Status</th>
                  {canMutate && <th scope="col" className="text-end pe-3">Actions</th>}
                </tr>
              </thead>
              <tbody>
                {filteredClosures.map((c) => {
                  const isCancelled = c.status === 'CANCELLED';
                  const isFuture = c.startAt > nowIso;
                  const canEdit = canMutate && !isCancelled && isFuture;
                  const canCancel = canMutate && !isCancelled && c.endAt > nowIso;

                  return (
                    <tr key={c.id}>
                      <td className="small">
                        <div className="fw-semibold text-dark">
                          From: {formatDateTime(c.startAt)}
                        </div>
                        <div className="text-muted">
                          To: {formatDateTime(c.endAt)}
                        </div>
                      </td>
                      <td>
                        <div className="fw-normal text-dark">{c.reason}</div>
                      </td>
                      <td>
                        {getClosureStatusBadge(c)}
                      </td>
                      {canMutate && (
                        <td className="text-end pe-3">
                          <div className="btn-group btn-group-sm" role="group">
                            {canEdit && (
                              <button
                                type="button"
                                className="btn btn-outline-secondary btn-sm"
                                onClick={() => onEditClosure(c)}
                                title="Edit future closure interval"
                              >
                                <i className="bi bi-pencil me-1"></i>
                                Edit
                              </button>
                            )}
                            {canCancel && (
                              <button
                                type="button"
                                className="btn btn-outline-danger btn-sm"
                                onClick={() => setConfirmCancelClosure(c)}
                                title="Cancel closure and restore operating schedule"
                              >
                                <i className="bi bi-x-circle me-1"></i>
                                Cancel
                              </button>
                            )}
                            {!canEdit && !canCancel && (
                              <span className="text-muted small">—</span>
                            )}
                          </div>
                        </td>
                      )}
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <div className="card-footer bg-light py-2 text-muted small d-flex justify-content-between">
        <span>Total Closures: {closures.length}</span>
        <span>Retained Audit History</span>
      </div>

      {/* Cancel Confirmation Modal */}
      {confirmCancelClosure && (
        <div className="modal show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content shadow-lg border-0">
              <div className="modal-header bg-danger text-white">
                <h5 className="modal-title">
                  <i className="bi bi-exclamation-triangle me-2"></i>
                  Cancel Center Closure
                </h5>
                <button
                  type="button"
                  className="btn-close btn-close-white"
                  aria-label="Close"
                  onClick={() => setConfirmCancelClosure(null)}
                  disabled={cancelLoading}
                ></button>
              </div>
              <div className="modal-body p-4">
                <p className="mb-2 text-dark">
                  Are you sure you want to cancel this temporary closure for <strong>{center?.name}</strong>?
                </p>
                <div className="card bg-light border-0 p-3 small text-muted mb-3">
                  <div><strong>Period:</strong> {formatDateTime(confirmCancelClosure.startAt)} — {formatDateTime(confirmCancelClosure.endAt)}</div>
                  <div><strong>Reason:</strong> {confirmCancelClosure.reason}</div>
                </div>
                <p className="small text-muted mb-0">
                  Cancelling this closure marks the record as CANCELLED while preserving historical audit logs. Regular operating hours and session scheduling for this window will be restored.
                </p>
              </div>
              <div className="modal-footer bg-light">
                <button
                  type="button"
                  className="btn btn-outline-secondary btn-sm"
                  onClick={() => setConfirmCancelClosure(null)}
                  disabled={cancelLoading}
                >
                  Keep Closure
                </button>
                <button
                  type="button"
                  className="btn btn-danger btn-sm fw-semibold"
                  onClick={handleConfirmCancel}
                  disabled={cancelLoading}
                >
                  {cancelLoading ? (
                    <>
                      <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                      Cancelling...
                    </>
                  ) : (
                    'Yes, Cancel Closure'
                  )}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
