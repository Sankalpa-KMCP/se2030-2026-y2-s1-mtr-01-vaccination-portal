import React from 'react';

/**
 * Returns a Bootstrap badge class based on the CampaignStatus enum value.
 */
function getStatusBadgeClass(status) {
  switch (status) {
    case 'DRAFT':
      return 'badge bg-secondary';
    case 'SCHEDULED':
      return 'badge bg-info text-dark';
    case 'PUBLISHED':
      return 'badge bg-primary';
    case 'ACTIVE':
      return 'badge bg-success';
    case 'SUSPENDED':
      return 'badge bg-warning text-dark';
    case 'CANCELLED':
      return 'badge bg-danger';
    case 'COMPLETED':
      return 'badge bg-dark';
    case 'ARCHIVED':
      return 'badge bg-secondary opacity-75';
    default:
      return 'badge bg-secondary';
  }
}

/**
 * Formats ISO timestamps or date strings into user-friendly locale format.
 */
function formatDate(dateStr) {
  if (!dateStr) return '—';
  try {
    const d = new Date(dateStr);
    return isNaN(d.getTime()) ? dateStr : d.toLocaleDateString();
  } catch {
    return dateStr;
  }
}

function formatDateTime(dateTimeStr) {
  if (!dateTimeStr) return '—';
  try {
    const d = new Date(dateTimeStr);
    return isNaN(d.getTime()) ? dateTimeStr : d.toLocaleString();
  } catch {
    return dateTimeStr;
  }
}

export default function CampaignList({
  campaigns,
  selectedStatus,
  onStatusFilterChange,
  onSelectCampaign,
  onInitiateAction,
  onCreateNew,
}) {
  const statusOptions = [
    { label: 'All Statuses', value: '' },
    { label: 'DRAFT', value: 'DRAFT' },
    { label: 'SCHEDULED', value: 'SCHEDULED' },
    { label: 'PUBLISHED', value: 'PUBLISHED' },
    { label: 'ACTIVE', value: 'ACTIVE' },
    { label: 'SUSPENDED', value: 'SUSPENDED' },
    { label: 'COMPLETED', value: 'COMPLETED' },
    { label: 'CANCELLED', value: 'CANCELLED' },
    { label: 'ARCHIVED', value: 'ARCHIVED' },
  ];

  return (
    <div className="card shadow-sm border-0">
      <div className="card-header bg-white border-bottom p-3 d-flex flex-wrap justify-content-between align-items-center gap-3">
        <div className="d-flex align-items-center gap-2">
          <label htmlFor="statusFilter" className="form-label mb-0 fw-semibold text-secondary small">
            Filter Status:
          </label>
          <select
            id="statusFilter"
            className="form-select form-select-sm"
            style={{ width: 'auto' }}
            value={selectedStatus}
            onChange={(e) => onStatusFilterChange(e.target.value)}
          >
            {statusOptions.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>

        <button
          type="button"
          className="btn btn-primary btn-sm fw-semibold shadow-sm"
          onClick={onCreateNew}
        >
          + New Campaign
        </button>
      </div>

      <div className="card-body p-0">
        {campaigns.length === 0 ? (
          <div className="text-center py-5 text-secondary">
            <h5 className="h6 text-muted mb-2">No vaccination campaigns found</h5>
            <p className="small text-muted mb-3">
              {selectedStatus
                ? `No campaigns match the filter '${selectedStatus}'.`
                : 'No campaigns have been created yet.'}
            </p>
            <button
              type="button"
              className="btn btn-outline-primary btn-sm"
              onClick={onCreateNew}
            >
              Create First Campaign
            </button>
          </div>
        ) : (
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead className="table-light text-secondary small">
                <tr>
                  <th scope="col">Code</th>
                  <th scope="col">Campaign Name</th>
                  <th scope="col">Vaccine</th>
                  <th scope="col">Target Group</th>
                  <th scope="col">Service Window</th>
                  <th scope="col">Booking Window</th>
                  <th scope="col">Status</th>
                  <th scope="col" className="text-end pe-3">Actions</th>
                </tr>
              </thead>
              <tbody>
                {campaigns.map((camp) => (
                  <tr key={camp.id}>
                    <td className="fw-semibold font-monospace small">{camp.code}</td>
                    <td>
                      <div className="fw-semibold text-dark">{camp.name}</div>
                    </td>
                    <td>
                      <span className="badge bg-light text-dark border">
                        {camp.vaccineTypeCode || 'N/A'}
                      </span>
                    </td>
                    <td>
                      <span className="small text-muted">{camp.targetGroupName || 'N/A'}</span>
                    </td>
                    <td className="small">
                      <div>{formatDate(camp.serviceStartDate)}</div>
                      <div className="text-muted" style={{ fontSize: '0.75rem' }}>
                        to {formatDate(camp.serviceEndDate)}
                      </div>
                    </td>
                    <td className="small">
                      <div>{formatDateTime(camp.bookingOpenAt)}</div>
                      <div className="text-muted" style={{ fontSize: '0.75rem' }}>
                        to {formatDateTime(camp.bookingCloseAt)}
                      </div>
                    </td>
                    <td>
                      <span className={getStatusBadgeClass(camp.status)}>{camp.status}</span>
                    </td>
                    <td className="text-end pe-3">
                      <div className="btn-group btn-group-sm" role="group" aria-label="Campaign Actions">
                        <button
                          type="button"
                          className="btn btn-outline-secondary"
                          onClick={() => onSelectCampaign(camp.id)}
                          title="View Details"
                        >
                          Details
                        </button>

                        {camp.status === 'DRAFT' && (
                          <button
                            type="button"
                            className="btn btn-outline-info text-dark"
                            onClick={() => onInitiateAction('SCHEDULE', camp)}
                          >
                            Schedule
                          </button>
                        )}

                        {camp.status === 'SCHEDULED' && (
                          <button
                            type="button"
                            className="btn btn-outline-primary"
                            onClick={() => onInitiateAction('PUBLISH', camp)}
                          >
                            Publish
                          </button>
                        )}

                        {camp.status === 'PUBLISHED' && (
                          <button
                            type="button"
                            className="btn btn-outline-success"
                            onClick={() => onInitiateAction('ACTIVATE', camp)}
                          >
                            Activate
                          </button>
                        )}

                        {camp.status === 'ACTIVE' && (
                          <button
                            type="button"
                            className="btn btn-outline-warning text-dark"
                            onClick={() => onInitiateAction('SUSPEND', camp)}
                          >
                            Suspend
                          </button>
                        )}

                        {camp.status === 'SUSPENDED' && (
                          <button
                            type="button"
                            className="btn btn-outline-success"
                            onClick={() => onInitiateAction('RESUME', camp)}
                          >
                            Resume
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
      <div className="card-footer bg-light py-2 text-muted small d-flex justify-content-between">
        <span>Total Campaigns: {campaigns.length}</span>
        <span>MF-02 Campaign Core Engine</span>
      </div>
    </div>
  );
}
