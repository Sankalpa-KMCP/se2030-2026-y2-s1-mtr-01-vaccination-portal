import React, { useState } from 'react';
import LoadingSpinner from '../../../components/common/LoadingSpinner';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';

const TYPE_FILTER_TABS = [
  { id: 'ALL', label: 'All Resources' },
  { id: 'ROOM', label: 'Rooms / Spaces' },
  { id: 'VACCINATION_STATION', label: 'Vaccination Stations' },
  { id: 'EQUIPMENT', label: 'Equipment' },
];

function getResourceTypeBadge(type) {
  switch (type) {
    case 'ROOM':
      return <span className="badge bg-primary-subtle text-primary-emphasis border border-primary-subtle">Room / Space</span>;
    case 'VACCINATION_STATION':
      return <span className="badge bg-info-subtle text-info-emphasis border border-info-subtle">Vaccination Station</span>;
    case 'EQUIPMENT':
      return <span className="badge bg-warning-subtle text-warning-emphasis border border-warning-subtle">Equipment</span>;
    default:
      return <span className="badge bg-secondary text-white">{type}</span>;
  }
}

function getResourceStatusBadge(status) {
  switch (status) {
    case 'ACTIVE':
      return <span className="badge bg-success-subtle text-success-emphasis border border-success-subtle">Active</span>;
    case 'MAINTENANCE':
      return <span className="badge bg-warning-subtle text-warning-emphasis border border-warning-subtle">Maintenance</span>;
    case 'INACTIVE':
      return <span className="badge bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle">Inactive</span>;
    default:
      return <span className="badge bg-light text-dark border">{status}</span>;
  }
}

export default function OperationalResourcesPanel({
  center,
  resources = [],
  loading = false,
  error = null,
  onRefresh,
  onAddResource,
  onEditResource,
}) {
  const [activeFilter, setActiveFilter] = useState('ALL');

  const isCenterInactive = center?.status === 'INACTIVE';

  const filteredResources = resources.filter((res) => {
    if (activeFilter === 'ALL') return true;
    return res.resourceType === activeFilter;
  });

  const getCountForFilter = (filterId) => {
    if (filterId === 'ALL') return resources.length;
    return resources.filter((r) => r.resourceType === filterId).length;
  };

  return (
    <div className="card shadow-sm border-0">
      <div className="card-header bg-white py-3 border-bottom d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-2">
        <div>
          <h2 className="h5 mb-1 text-primary fw-bold">
            <i className="bi bi-box-seam me-2"></i>
            Operational Physical Resources
          </h2>
          <p className="small text-muted mb-0">
            Rooms, vaccination stations, and medical equipment available for session scheduling.
          </p>
        </div>

        <div className="d-flex gap-2">
          <button
            type="button"
            className="btn btn-sm btn-outline-secondary"
            onClick={onRefresh}
            disabled={loading}
            title="Refresh resource inventory"
          >
            <i className="bi bi-arrow-clockwise me-1"></i>
            Refresh
          </button>

          {!isCenterInactive && (
            <button
              type="button"
              className="btn btn-sm btn-primary"
              onClick={onAddResource}
              disabled={loading}
            >
              <i className="bi bi-plus-lg me-1"></i>
              Add Resource
            </button>
          )}
        </div>
      </div>

      <div className="card-body p-4">
        {isCenterInactive && (
          <div className="alert alert-warning border small d-flex align-items-center mb-4">
            <i className="bi bi-exclamation-triangle-fill me-2 fs-5 text-warning"></i>
            <div>
              <strong>Center Inactive (Decommissioned):</strong> This vaccination center is inactive and decommissioned from operational planning. Physical resource records are displayed in read-only mode.
            </div>
          </div>
        )}

        <ApiErrorAlert error={error} />

        {loading ? (
          <div className="py-5">
            <LoadingSpinner message="Loading center operational resources..." />
          </div>
        ) : (
          <>
            {/* Filter Pills */}
            <div className="d-flex flex-wrap gap-2 mb-4">
              {TYPE_FILTER_TABS.map((tab) => {
                const count = getCountForFilter(tab.id);
                const isSelected = activeFilter === tab.id;
                return (
                  <button
                    key={tab.id}
                    type="button"
                    className={`btn btn-sm ${
                      isSelected
                        ? 'btn-primary shadow-sm'
                        : 'btn-outline-secondary'
                    }`}
                    onClick={() => setActiveFilter(tab.id)}
                  >
                    {tab.label}{' '}
                    <span
                      className={`badge ms-1 ${
                        isSelected ? 'bg-white text-primary' : 'bg-secondary text-white'
                      }`}
                    >
                      {count}
                    </span>
                  </button>
                );
              })}
            </div>

            {/* Resources Table or Empty State */}
            {resources.length === 0 ? (
              <div className="alert alert-info border small text-dark my-3">
                <i className="bi bi-info-circle me-1 text-info"></i>
                <strong>No operational resources registered.</strong>
                <p className="mb-0 mt-1">
                  There are currently no physical rooms, stations, or equipment registered under this center.
                  {!isCenterInactive && ' Click "Add Resource" above to configure physical operational assets.'}
                </p>
              </div>
            ) : filteredResources.length === 0 ? (
              <div className="alert alert-light border small text-muted text-center py-4 my-2">
                <i className="bi bi-funnel me-1"></i>
                No physical resources found matching the selected category filter.
              </div>
            ) : (
              <div className="table-responsive">
                <table className="table table-hover align-middle border mb-0">
                  <thead className="table-light small text-uppercase">
                    <tr>
                      <th scope="col" style={{ width: '18%' }}>Resource Code</th>
                      <th scope="col" style={{ width: '22%' }}>Type</th>
                      <th scope="col" style={{ width: '35%' }}>Resource Name</th>
                      <th scope="col" style={{ width: '15%' }}>Status</th>
                      <th scope="col" className="text-end" style={{ width: '10%' }}>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredResources.map((res) => (
                      <tr key={res.id}>
                        <td>
                          <span className="font-monospace fw-semibold text-dark">
                            {res.code}
                          </span>
                        </td>
                        <td>{getResourceTypeBadge(res.resourceType)}</td>
                        <td className="fw-medium text-dark">{res.name}</td>
                        <td>{getResourceStatusBadge(res.status)}</td>
                        <td className="text-end">
                          {!isCenterInactive ? (
                            <button
                              type="button"
                              className="btn btn-sm btn-outline-primary py-1 px-2"
                              onClick={() => onEditResource(res)}
                              title="Edit resource name or operational status"
                            >
                              <i className="bi bi-pencil me-1"></i>
                              Edit
                            </button>
                          ) : (
                            <span className="text-muted small">Read-only</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
