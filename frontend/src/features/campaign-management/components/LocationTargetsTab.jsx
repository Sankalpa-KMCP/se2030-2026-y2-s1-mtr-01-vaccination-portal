import React, { useState } from 'react';
import LoadingSpinner from '../../../components/common/LoadingSpinner';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';

const TYPE_CONFIG = {
  PROVINCE: {
    label: 'Province',
    badgeClass: 'bg-info-subtle text-info-emphasis border border-info-subtle',
    scopeDesc: 'Administrative Province',
  },
  DISTRICT: {
    label: 'District',
    badgeClass: 'bg-primary-subtle text-primary border border-primary-subtle',
    scopeDesc: 'Administrative District',
  },
  HEALTH_REGION: {
    label: 'Health Region',
    badgeClass: 'bg-success-subtle text-success border border-success-subtle',
    scopeDesc: 'Health Directorate Division',
  },
  CENTER: {
    label: 'Center',
    badgeClass: 'bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle',
    scopeDesc: 'Specific Center Location',
  },
};

/**
 * Tab component for viewing and managing administrative location targets.
 */
export default function LocationTargetsTab({
  campaign,
  targets = [],
  loading = false,
  error = null,
  onOpenAddModal,
  onRefresh,
}) {
  const [selectedTypeFilter, setSelectedTypeFilter] = useState('ALL');

  const isTerminal = ['CANCELLED', 'COMPLETED', 'ARCHIVED'].includes(campaign?.status);

  const filteredTargets = selectedTypeFilter === 'ALL'
    ? targets
    : targets.filter((t) => t.targetType === selectedTypeFilter);

  const countForType = (type) => targets.filter((t) => t.targetType === type).length;

  return (
    <div className="location-targets-tab">
      {/* Header Actions */}
      <div className="d-flex justify-content-between align-items-center mb-3">
        <div>
          <h6 className="fw-bold mb-0 text-dark">
            <i className="bi bi-geo-alt me-1 text-primary"></i>
            Administrative Location Targets
          </h6>
          <p className="text-muted small mb-0">
            Define citizen portal visibility and administrative service-area scoping. Location targets are optional.
          </p>
        </div>

        <div className="d-flex gap-2">
          <button
            type="button"
            className="btn btn-sm btn-outline-secondary"
            onClick={onRefresh}
            disabled={loading}
            title="Refresh location targets"
          >
            <i className="bi bi-arrow-clockwise me-1"></i>
            Refresh
          </button>

          {!isTerminal && (
            <button
              type="button"
              className="btn btn-sm btn-primary"
              onClick={onOpenAddModal}
              disabled={loading}
            >
              <i className="bi bi-plus-lg me-1"></i>
              Add Location Target
            </button>
          )}
        </div>
      </div>

      <ApiErrorAlert error={error} />

      {loading ? (
        <div className="text-center py-4">
          <LoadingSpinner />
          <p className="text-muted small mt-2">Loading location targets...</p>
        </div>
      ) : (
        <>
          {/* Filter Pills */}
          <div className="d-flex flex-wrap gap-2 mb-3 pb-2 border-bottom">
            <button
              type="button"
              className={`btn btn-sm rounded-pill ${selectedTypeFilter === 'ALL' ? 'btn-dark' : 'btn-outline-secondary'}`}
              onClick={() => setSelectedTypeFilter('ALL')}
            >
              All ({targets.length})
            </button>
            <button
              type="button"
              className={`btn btn-sm rounded-pill ${selectedTypeFilter === 'PROVINCE' ? 'btn-info' : 'btn-outline-info'}`}
              onClick={() => setSelectedTypeFilter('PROVINCE')}
            >
              Province ({countForType('PROVINCE')})
            </button>
            <button
              type="button"
              className={`btn btn-sm rounded-pill ${selectedTypeFilter === 'DISTRICT' ? 'btn-primary' : 'btn-outline-primary'}`}
              onClick={() => setSelectedTypeFilter('DISTRICT')}
            >
              District ({countForType('DISTRICT')})
            </button>
            <button
              type="button"
              className={`btn btn-sm rounded-pill ${selectedTypeFilter === 'HEALTH_REGION' ? 'btn-success' : 'btn-outline-success'}`}
              onClick={() => setSelectedTypeFilter('HEALTH_REGION')}
            >
              Health Region ({countForType('HEALTH_REGION')})
            </button>
            <button
              type="button"
              className={`btn btn-sm rounded-pill ${selectedTypeFilter === 'CENTER' ? 'btn-secondary' : 'btn-outline-secondary'}`}
              onClick={() => setSelectedTypeFilter('CENTER')}
            >
              Center ({countForType('CENTER')})
            </button>
          </div>

          {targets.length === 0 ? (
            <div className="alert alert-info border small text-dark my-3">
              <i className="bi bi-info-circle me-1 text-info"></i>
              <strong>No location targets defined.</strong>
              <p className="mb-0 mt-1">
                This campaign currently has no administrative service-area restriction. Citizens from all regions will be able to discover and access this campaign in the portal.
              </p>
            </div>
          ) : filteredTargets.length === 0 ? (
            <div className="text-center py-4 text-muted small bg-light rounded border">
              <i className="bi bi-funnel me-1"></i>
              No location targets found for the selected type.
            </div>
          ) : (
            <div className="table-responsive border rounded bg-white">
              <table className="table table-sm table-hover align-middle mb-0">
                <thead className="table-light">
                  <tr className="small text-muted text-uppercase">
                    <th style={{ width: '25%' }}>Target Type</th>
                    <th style={{ width: '40%' }}>Target Code</th>
                    <th style={{ width: '35%' }}>Administrative Scope</th>
                  </tr>
                </thead>
                <tbody className="small">
                  {filteredTargets.map((target) => {
                    const config = TYPE_CONFIG[target.targetType] || {
                      label: target.targetType,
                      badgeClass: 'bg-secondary-subtle text-secondary',
                      scopeDesc: 'Custom Target',
                    };

                    return (
                      <tr key={target.id}>
                        <td>
                          <span className={`badge ${config.badgeClass}`}>
                            {config.label}
                          </span>
                        </td>
                        <td>
                          <span className="font-monospace fw-bold text-dark">
                            {target.targetCode}
                          </span>
                        </td>
                        <td className="text-muted">
                          {config.scopeDesc}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </div>
  );
}
