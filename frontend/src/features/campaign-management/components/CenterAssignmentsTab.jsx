import React from 'react';
import LoadingSpinner from '../../../components/common/LoadingSpinner';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';

function formatDate(dateStr) {
  if (!dateStr) return 'Open-ended';
  try {
    const d = new Date(dateStr);
    return isNaN(d.getTime()) ? dateStr : d.toLocaleDateString();
  } catch {
    return dateStr;
  }
}

export default function CenterAssignmentsTab({
  campaign,
  assignments = [],
  loading = false,
  error = null,
  onOpenCreateModal,
  onOpenUpdateModal,
  onOpenEndModal,
  onOpenCancelModal,
}) {
  const isTerminal = ['CANCELLED', 'COMPLETED', 'ARCHIVED'].includes(campaign?.status);
  const todayStr = new Date().toISOString().split('T')[0];

  const currentAssignments = assignments.filter(
    (a) => a.status === 'ACTIVE' && a.activeFrom <= todayStr && (a.activeTo == null || a.activeTo > todayStr)
  );

  const futureAssignments = assignments.filter(
    (a) => a.status === 'ACTIVE' && a.activeFrom > todayStr
  );

  const getClassificationBadge = (a) => {
    if (a.status === 'CANCELLED') {
      return <span className="badge bg-danger">Cancelled</span>;
    }
    if (a.activeFrom <= todayStr && (a.activeTo == null || a.activeTo > todayStr)) {
      return <span className="badge bg-success">Active</span>;
    }
    if (a.activeFrom > todayStr) {
      return <span className="badge bg-info text-dark">Scheduled</span>;
    }
    return <span className="badge bg-secondary">Ended</span>;
  };

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <div>
          <h6 className="fw-bold text-dark mb-0">Authorized Vaccination Centers</h6>
          <span className="small text-muted">
            Manage physical vaccination centers participating in this campaign.
          </span>
        </div>
        {!isTerminal && (
          <button
            type="button"
            className="btn btn-sm btn-primary"
            onClick={onOpenCreateModal}
          >
            + Assign Vaccination Center
          </button>
        )}
      </div>

      <ApiErrorAlert error={error} />

      {loading ? (
        <LoadingSpinner message="Loading center assignments..." />
      ) : assignments.length === 0 ? (
        <div className="alert alert-warning border small text-dark my-3">
          <strong>No centers assigned yet.</strong>
          <p className="mb-0 mt-1">
            This campaign requires at least one active center assignment overlapping its service period
            (<code>{campaign?.serviceStartDate || 'Start Date'}</code> to <code>{campaign?.serviceEndDate || 'End Date'}</code>)
            before it can be approved for scheduling.
          </p>
        </div>
      ) : (
        <div>
          {/* Active Center Assignments Section */}
          <div className="mb-4">
            <h6 className="small fw-bold text-uppercase text-secondary mb-2">
              Currently Effective Centers ({currentAssignments.length})
            </h6>

            {currentAssignments.length > 0 ? (
              <div className="row g-3">
                {currentAssignments.map((a) => (
                  <div key={a.id} className="col-md-6">
                    <div className="card border-success shadow-sm h-100">
                      <div className="card-body p-3">
                        <div className="d-flex justify-content-between align-items-start mb-2">
                          <h6 className="card-title fw-bold text-success mb-0">
                            {a.centerName || `Center #${a.centerId}`}
                          </h6>
                          <span className="badge bg-success-subtle text-success border border-success-subtle">
                            Active
                          </span>
                        </div>
                        <div className="small text-muted font-monospace mb-2">
                          Code: {a.centerCode || '—'}
                        </div>
                        <div className="small mb-3">
                          <div>
                            <strong>Active From: </strong>
                            {formatDate(a.activeFrom)}
                          </div>
                          <div>
                            <strong>Active To: </strong>
                            {formatDate(a.activeTo)}
                          </div>
                        </div>

                        {!isTerminal && (
                          <div className="border-top pt-2 text-end">
                            <button
                              type="button"
                              className="btn btn-outline-warning btn-sm text-dark py-0 px-2"
                              style={{ fontSize: '0.75rem' }}
                              onClick={() => onOpenEndModal(a)}
                            >
                              End Early
                            </button>
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="p-3 bg-light rounded border text-muted small">
                No centers currently effective today ({todayStr}).
              </div>
            )}
          </div>

          {/* Scheduled Future Center Assignments */}
          {futureAssignments.length > 0 && (
            <div className="mb-4">
              <h6 className="small fw-bold text-uppercase text-secondary mb-2">
                Scheduled Future Centers ({futureAssignments.length})
              </h6>

              <div className="row g-3">
                {futureAssignments.map((a) => (
                  <div key={a.id} className="col-md-6">
                    <div className="card border-info shadow-sm h-100">
                      <div className="card-body p-3">
                        <div className="d-flex justify-content-between align-items-start mb-2">
                          <h6 className="card-title fw-bold text-info-emphasis mb-0">
                            {a.centerName || `Center #${a.centerId}`}
                          </h6>
                          <span className="badge bg-info-subtle text-info-emphasis border border-info-subtle">
                            Scheduled
                          </span>
                        </div>
                        <div className="small text-muted font-monospace mb-2">
                          Code: {a.centerCode || '—'}
                        </div>
                        <div className="small mb-3">
                          <div>
                            <strong>Starts: </strong>
                            {formatDate(a.activeFrom)}
                          </div>
                          <div>
                            <strong>Ends: </strong>
                            {formatDate(a.activeTo)}
                          </div>
                        </div>

                        {!isTerminal && (
                          <div className="border-top pt-2 d-flex justify-content-end gap-2">
                            <button
                              type="button"
                              className="btn btn-outline-primary btn-sm py-0 px-2"
                              style={{ fontSize: '0.75rem' }}
                              onClick={() => onOpenUpdateModal(a)}
                            >
                              Edit Period
                            </button>
                            <button
                              type="button"
                              className="btn btn-outline-danger btn-sm py-0 px-2"
                              style={{ fontSize: '0.75rem' }}
                              onClick={() => onOpenCancelModal(a)}
                            >
                              Cancel
                            </button>
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Complete Assignment Audit History */}
          <div>
            <h6 className="small fw-bold text-uppercase text-secondary mb-2">
              Assignment Audit History ({assignments.length})
            </h6>

            <div className="table-responsive">
              <table className="table table-sm table-striped small mb-0 align-middle">
                <thead>
                  <tr>
                    <th>Center</th>
                    <th>Period</th>
                    <th>Status</th>
                    <th className="text-end">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {assignments.map((a) => {
                    const isFuture = a.status === 'ACTIVE' && a.activeFrom > todayStr;
                    const isCurrent = a.status === 'ACTIVE' && a.activeFrom <= todayStr && (a.activeTo == null || a.activeTo > todayStr);

                    return (
                      <tr key={a.id}>
                        <td>
                          <div className="fw-semibold text-dark">{a.centerName}</div>
                          <div className="text-muted font-monospace" style={{ fontSize: '0.75rem' }}>
                            {a.centerCode} (ID #{a.id})
                          </div>
                        </td>
                        <td>
                          {formatDate(a.activeFrom)} &rarr; {formatDate(a.activeTo)}
                        </td>
                        <td>{getClassificationBadge(a)}</td>
                        <td className="text-end">
                          {!isTerminal && (
                            <div className="d-flex justify-content-end gap-1">
                              {isCurrent && (
                                <button
                                  type="button"
                                  className="btn btn-outline-warning btn-sm py-0 px-2 text-dark"
                                  style={{ fontSize: '0.75rem' }}
                                  onClick={() => onOpenEndModal(a)}
                                >
                                  End
                                </button>
                              )}
                              {isFuture && (
                                <>
                                  <button
                                    type="button"
                                    className="btn btn-outline-primary btn-sm py-0 px-2"
                                    style={{ fontSize: '0.75rem' }}
                                    onClick={() => onOpenUpdateModal(a)}
                                  >
                                    Edit
                                  </button>
                                  <button
                                    type="button"
                                    className="btn btn-outline-danger btn-sm py-0 px-2"
                                    style={{ fontSize: '0.75rem' }}
                                    onClick={() => onOpenCancelModal(a)}
                                  >
                                    Cancel
                                  </button>
                                </>
                              )}
                            </div>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
