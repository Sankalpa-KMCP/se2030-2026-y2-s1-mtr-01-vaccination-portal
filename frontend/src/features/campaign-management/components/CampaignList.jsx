import React from 'react';
import { useAuth } from '../../../auth/context/AuthContext';
import { canManageCampaigns, hasRole } from '../../../auth/utils/roleUtils';
import StatusBadge from '../../../components/common/StatusBadge';
import EmptyState from '../../../components/common/EmptyState';
import { campaignStatusVariants } from '../../../components/common/statusVariants';

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

const EDITABLE_STATUSES = new Set(['DRAFT', 'SCHEDULED', 'PUBLISHED', 'ACTIVE', 'SUSPENDED']);

export default function CampaignList({
  campaigns,
  selectedStatus,
  onStatusFilterChange,
  onSelectCampaign,
  onInitiateAction,
  onCreateNew,
  onEditCampaign,
}) {
  const { user } = useAuth();
  const isManager = canManageCampaigns(user);
  const isAdmin = hasRole(user, 'ROLE_ADMIN');

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

        {isManager && (
          <button
            type="button"
            className="btn btn-primary btn-sm fw-semibold shadow-sm"
            onClick={onCreateNew}
          >
            + New Campaign
          </button>
        )}
      </div>

      <div className="card-body p-0">
        {campaigns.length === 0 ? (
          <EmptyState
            icon="bi-megaphone"
            title="No vaccination campaigns found"
            action={isManager && (
              <button
                type="button"
                className="btn btn-outline-primary btn-sm"
                onClick={onCreateNew}
              >
                Create First Campaign
              </button>
            )}
          >
            {selectedStatus
              ? `No campaigns match the filter '${selectedStatus}'.`
              : 'No campaigns have been created yet.'}
          </EmptyState>
        ) : (
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead className="table-light text-secondary small">
                <tr>
                  <th scope="col">Code</th>
                  <th scope="col">Name</th>
                  <th scope="col">Vaccine</th>
                  <th scope="col">Target Group</th>
                  <th scope="col">Service Period</th>
                  <th scope="col">Booking Window</th>
                  <th scope="col">Status</th>
                  <th scope="col" className="text-end pe-3">Actions</th>
                </tr>
              </thead>
              <tbody>
                {campaigns.map((camp) => (
                  <tr key={camp.id}>
                    <td className="fw-semibold text-primary">{camp.code}</td>
                    <td>
                      <div className="fw-semibold">{camp.name}</div>
                    </td>
                    <td>
                      <span className="badge bg-light text-dark border">
                        {camp.vaccineTypeCode || 'N/A'}
                      </span>
                    </td>
                    <td>
                      <span className="badge bg-light text-dark border">
                        {camp.targetGroupCode || 'N/A'}
                      </span>
                    </td>
                    <td className="small">
                      <div>{formatDate(camp.serviceStartDate)}</div>
                      <div className="text-muted fs-7">
                        to {formatDate(camp.serviceEndDate)}
                      </div>
                    </td>
                    <td className="small">
                      <div>{formatDateTime(camp.bookingOpenAt)}</div>
                      <div className="text-muted fs-7">
                        to {formatDateTime(camp.bookingCloseAt)}
                      </div>
                    </td>
                    <td>
                      <StatusBadge status={camp.status} variants={campaignStatusVariants} />
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

                        {isManager && EDITABLE_STATUSES.has(camp.status) && (
                          <button
                            type="button"
                            className="btn btn-outline-primary"
                            onClick={() => onEditCampaign && onEditCampaign(camp)}
                            title="Edit Campaign"
                          >
                            Edit
                          </button>
                        )}

                        {isManager && camp.status === 'DRAFT' && (
                          <>
                            <button
                              type="button"
                              className="btn btn-outline-info text-dark"
                              onClick={() => onInitiateAction('SCHEDULE', camp)}
                            >
                              Schedule
                            </button>
                            <button
                              type="button"
                              className="btn btn-outline-danger"
                              onClick={() => onInitiateAction('CANCEL', camp)}
                            >
                              Cancel
                            </button>
                          </>
                        )}

                        {isManager && camp.status === 'SCHEDULED' && (
                          <>
                            <button
                              type="button"
                              className="btn btn-outline-primary"
                              onClick={() => onInitiateAction('PUBLISH', camp)}
                            >
                              Publish
                            </button>
                            <button
                              type="button"
                              className="btn btn-outline-danger"
                              onClick={() => onInitiateAction('CANCEL', camp)}
                            >
                              Cancel
                            </button>
                          </>
                        )}

                        {isManager && camp.status === 'PUBLISHED' && (
                          <>
                            <button
                              type="button"
                              className="btn btn-outline-success"
                              onClick={() => onInitiateAction('ACTIVATE', camp)}
                            >
                              Activate
                            </button>
                            <button
                              type="button"
                              className="btn btn-outline-dark"
                              onClick={() => onInitiateAction('COMPLETE', camp)}
                            >
                              Complete
                            </button>
                            <button
                              type="button"
                              className="btn btn-outline-danger"
                              onClick={() => onInitiateAction('CANCEL', camp)}
                            >
                              Cancel
                            </button>
                          </>
                        )}

                        {isManager && camp.status === 'ACTIVE' && (
                          <>
                            <button
                              type="button"
                              className="btn btn-outline-warning text-dark"
                              onClick={() => onInitiateAction('SUSPEND', camp)}
                            >
                              Suspend
                            </button>
                            <button
                              type="button"
                              className="btn btn-outline-dark"
                              onClick={() => onInitiateAction('COMPLETE', camp)}
                            >
                              Complete
                            </button>
                            <button
                              type="button"
                              className="btn btn-outline-danger"
                              onClick={() => onInitiateAction('CANCEL', camp)}
                            >
                              Cancel
                            </button>
                          </>
                        )}

                        {isManager && camp.status === 'SUSPENDED' && (
                          <>
                            <button
                              type="button"
                              className="btn btn-outline-success"
                              onClick={() => onInitiateAction('RESUME', camp)}
                            >
                              Resume
                            </button>
                            <button
                              type="button"
                              className="btn btn-outline-dark"
                              onClick={() => onInitiateAction('COMPLETE', camp)}
                            >
                              Complete
                            </button>
                            <button
                              type="button"
                              className="btn btn-outline-danger"
                              onClick={() => onInitiateAction('CANCEL', camp)}
                            >
                              Cancel
                            </button>
                          </>
                        )}

                        {isAdmin && (camp.status === 'COMPLETED' || camp.status === 'CANCELLED') && (
                          <button
                            type="button"
                            className="btn btn-outline-secondary"
                            onClick={() => onInitiateAction('ARCHIVE', camp)}
                          >
                            Archive
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
