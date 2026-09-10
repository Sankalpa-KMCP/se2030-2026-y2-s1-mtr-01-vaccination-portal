import React, { useState, useEffect, useCallback } from 'react';
import { getOperationalAnalytics } from '../api/analyticsApi';
import { listCenters } from '../../center-operations/api/centerOperationsApi';
import { listCampaigns } from '../../campaign-management/api/campaignApi';
import PageHeader from '../../../components/common/PageHeader';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import LoadingSpinner from '../../../components/common/LoadingSpinner';
import EmptyState from '../../../components/common/EmptyState';
import StatusBadge from '../../../components/common/StatusBadge';
import {
  sessionStatusVariants,
  campaignStatusVariants,
} from '../../../components/common/statusVariants';

export default function OperationalAnalyticsView() {
  const [analytics, setAnalytics] = useState(null);
  const [centers, setCenters] = useState([]);
  const [campaigns, setCampaigns] = useState([]);
  const [selectedCenterId, setSelectedCenterId] = useState('');
  const [selectedCampaignId, setSelectedCampaignId] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Load filter options on mount
  useEffect(() => {
    async function loadFilterOptions() {
      try {
        const [centerList, campaignList] = await Promise.all([
          listCenters().catch(() => []),
          listCampaigns().catch(() => []),
        ]);
        setCenters(Array.isArray(centerList) ? centerList : []);
        setCampaigns(Array.isArray(campaignList) ? campaignList : []);
      } catch {
        // Option load failure is non-blocking
      }
    }
    loadFilterOptions();
  }, []);

  const fetchAnalytics = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const filters = {};
      if (selectedCenterId) {
        filters.centerId = selectedCenterId;
      }
      if (selectedCampaignId) {
        filters.campaignId = selectedCampaignId;
      }
      const data = await getOperationalAnalytics(filters);
      setAnalytics(data);
    } catch (err) {
      setError(err.message || 'Failed to load operational analytics');
    } finally {
      setLoading(false);
    }
  }, [selectedCenterId, selectedCampaignId]);

  useEffect(() => {
    fetchAnalytics();
  }, [fetchAnalytics]);

  const handleResetFilters = () => {
    setSelectedCenterId('');
    setSelectedCampaignId('');
  };

  return (
    <div>
      <PageHeader
        title="Operational Analytics"
        subtitle="Reconciled descriptive capacity, slot utilization, and resource allocation"
        actions={(
          <button
            type="button"
            className="btn btn-outline-primary btn-sm d-flex align-items-center gap-1"
            onClick={fetchAnalytics}
            disabled={loading}
          >
            {loading ? (
              <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true" />
            ) : (
              <i className="bi bi-arrow-clockwise" aria-hidden="true" />
            )}
            Refresh
          </button>
        )}
      />

      {/* Filter Control Bar */}
      <div className="card border-0 shadow-sm mb-4">
        <div className="card-body py-3">
          <div className="row g-3 align-items-center">
            <div className="col-12 col-md-5">
              <label htmlFor="analyticsCenterFilter" className="form-label small fw-semibold text-muted mb-1">
                Filter by Center
              </label>
              <select
                id="analyticsCenterFilter"
                className="form-select form-select-sm"
                value={selectedCenterId}
                onChange={(e) => setSelectedCenterId(e.target.value)}
              >
                <option value="">All Centers</option>
                {centers.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name} ({c.code})
                  </option>
                ))}
              </select>
            </div>
            <div className="col-12 col-md-5">
              <label htmlFor="analyticsCampaignFilter" className="form-label small fw-semibold text-muted mb-1">
                Filter by Campaign
              </label>
              <select
                id="analyticsCampaignFilter"
                className="form-select form-select-sm"
                value={selectedCampaignId}
                onChange={(e) => setSelectedCampaignId(e.target.value)}
              >
                <option value="">All Campaigns</option>
                {campaigns.map((camp) => (
                  <option key={camp.id} value={camp.id}>
                    {camp.name} ({camp.code})
                  </option>
                ))}
              </select>
            </div>
            <div className="col-12 col-md-2 d-flex align-items-end">
              <button
                type="button"
                className="btn btn-outline-secondary btn-sm w-100"
                onClick={handleResetFilters}
                disabled={!selectedCenterId && !selectedCampaignId}
              >
                Clear Filters
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Error Alert */}
      {error && (
        <ApiErrorAlert error={error} onDismiss={() => setError(null)} />
      )}

      {/* Loading Skeleton / State */}
      {loading && !analytics && (
        <LoadingSpinner message="Computing reconciled operational metrics..." />
      )}

      {/* Main KPI Grid */}
      {analytics && (
        <>
          {/* Top KPI Cards Row */}
          <div className="row g-3 mb-4">
            {/* Slot Utilization */}
            <div className="col-12 col-sm-6 col-lg-3">
              <div className="card h-100 border-0 shadow-sm">
                <div className="card-body">
                  <div className="text-muted small text-uppercase fw-semibold mb-1">Slot Utilization</div>
                  <div className="h3 fw-bold mb-2 text-dark">
                    {analytics.slotUtilizationPercentage.toFixed(1)}%
                  </div>
                  <div className="progress" style={{ height: '8px' }}>
                    <div
                      className={`progress-bar ${
                        analytics.slotUtilizationPercentage > 90
                          ? 'bg-danger'
                          : analytics.slotUtilizationPercentage > 75
                          ? 'bg-warning'
                          : 'bg-primary'
                      }`}
                      role="progressbar"
                      style={{ width: `${Math.min(100, analytics.slotUtilizationPercentage)}%` }}
                      aria-valuenow={analytics.slotUtilizationPercentage}
                      aria-valuemin="0"
                      aria-valuemax="100"
                    />
                  </div>
                  <div className="text-muted small mt-2">
                    {analytics.totalReservedSlots.toLocaleString()} / {analytics.totalConfiguredSlotCapacity.toLocaleString()} reserved
                  </div>
                </div>
              </div>
            </div>

            {/* Approved Capacity vs Configured Slots */}
            <div className="col-12 col-sm-6 col-lg-3">
              <div className="card h-100 border-0 shadow-sm">
                <div className="card-body">
                  <div className="text-muted small text-uppercase fw-semibold mb-1">Approved Capacity</div>
                  <div className="h3 fw-bold mb-2 text-primary">
                    {analytics.totalApprovedCapacity.toLocaleString()}
                  </div>
                  <div className="text-muted small">
                    Recommended Throughput: <strong className="text-dark">{analytics.totalRecommendedThroughput.toLocaleString()}</strong>
                  </div>
                  <div className="text-muted small mt-1">
                    Remaining Slot Cap: <strong className="text-dark">{analytics.totalRemainingSlotCapacity.toLocaleString()}</strong>
                  </div>
                </div>
              </div>
            </div>

            {/* Assigned Active Resources & Staff */}
            <div className="col-12 col-sm-6 col-lg-3">
              <div className="card h-100 border-0 shadow-sm">
                <div className="card-body">
                  <div className="text-muted small text-uppercase fw-semibold mb-1">Operational Load</div>
                  <div className="d-flex justify-content-between align-items-baseline mb-2">
                    <span className="text-muted small">Assigned Stations:</span>
                    <span className="h4 fw-bold text-dark mb-0">{analytics.assignedStationCount}</span>
                  </div>
                  <div className="d-flex justify-content-between align-items-baseline">
                    <span className="text-muted small">Assigned Vaccinators:</span>
                    <span className="h4 fw-bold text-dark mb-0">{analytics.assignedVaccinatorCount}</span>
                  </div>
                </div>
              </div>
            </div>

            {/* Operational Cancellations */}
            <div className="col-12 col-sm-6 col-lg-3">
              <div className="card h-100 border-0 shadow-sm">
                <div className="card-body">
                  <div className="text-muted small text-uppercase fw-semibold mb-1">Cancellations</div>
                  <div className="d-flex justify-content-between align-items-baseline mb-2">
                    <span className="text-muted small">Cancelled Sessions:</span>
                    <span className={`h4 fw-bold mb-0 ${analytics.cancelledSessionCount > 0 ? 'text-danger' : 'text-dark'}`}>
                      {analytics.cancelledSessionCount}
                    </span>
                  </div>
                  <div className="d-flex justify-content-between align-items-baseline">
                    <span className="text-muted small">Cancelled Campaigns:</span>
                    <span className={`h4 fw-bold mb-0 ${analytics.cancelledCampaignCount > 0 ? 'text-danger' : 'text-dark'}`}>
                      {analytics.cancelledCampaignCount}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Breakdown Tables Row */}
          <div className="row g-4 mb-4">
            {/* Session Lifecycle Distribution */}
            <div className="col-12 col-lg-6">
              <div className="card border-0 shadow-sm h-100">
                <div className="card-header bg-white border-bottom py-3">
                  <h5 className="card-title h6 mb-0 fw-bold text-dark">Session Lifecycle Status</h5>
                </div>
                <div className="card-body p-0">
                  <div className="table-responsive">
                    <table className="table table-hover align-middle mb-0">
                      <thead className="table-light">
                        <tr>
                          <th className="ps-3">Lifecycle Status</th>
                          <th className="text-end pe-3">Session Count</th>
                        </tr>
                      </thead>
                      <tbody>
                        {Object.entries(analytics.sessionCountByLifecycleStatus || {}).length === 0 ? (
                          <tr><td colSpan="2"><EmptyState compact icon="bi-graph-up" title="No session data for the selected scope" /></td></tr>
                        ) : Object.entries(analytics.sessionCountByLifecycleStatus || {}).map(([status, count]) => (
                          <tr key={status}>
                            <td className="ps-3 fw-medium">
                              <StatusBadge status={status} variants={sessionStatusVariants} className="me-2" />
                            </td>
                            <td className="text-end pe-3 fw-bold">{count}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            </div>

            {/* Campaign Status Distribution */}
            <div className="col-12 col-lg-6">
              <div className="card border-0 shadow-sm h-100">
                <div className="card-header bg-white border-bottom py-3">
                  <h5 className="card-title h6 mb-0 fw-bold text-dark">Campaign Status Distribution</h5>
                </div>
                <div className="card-body p-0">
                  <div className="table-responsive">
                    <table className="table table-hover align-middle mb-0">
                      <thead className="table-light">
                        <tr>
                          <th className="ps-3">Campaign Status</th>
                          <th className="text-end pe-3">Count</th>
                        </tr>
                      </thead>
                      <tbody>
                        {Object.entries(analytics.campaignCountByStatus || {}).length === 0 ? (
                          <tr><td colSpan="2"><EmptyState compact icon="bi-clipboard-data" title="No campaign data for the selected scope" /></td></tr>
                        ) : Object.entries(analytics.campaignCountByStatus || {}).map(([status, count]) => (
                          <tr key={status}>
                            <td className="ps-3 fw-medium">
                              <StatusBadge status={status} variants={campaignStatusVariants} className="me-2" />
                            </td>
                            <td className="text-end pe-3 fw-bold">{count}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Assigned Resource Type Breakdown */}
          <div className="card border-0 shadow-sm mb-4">
            <div className="card-header bg-white border-bottom py-3">
              <h5 className="card-title h6 mb-0 fw-bold text-dark">Active Resource Allocation by Type</h5>
            </div>
            <div className="card-body p-0">
              <div className="table-responsive">
                <table className="table table-hover align-middle mb-0">
                  <thead className="table-light">
                    <tr>
                      <th className="ps-3">Resource Type</th>
                      <th className="text-end pe-3">Distinct Assigned Resources</th>
                    </tr>
                  </thead>
                  <tbody>
                    {Object.entries(analytics.assignedResourceCountsByType || {}).length === 0 ? (
                      <tr><td colSpan="2"><EmptyState compact icon="bi-box-seam" title="No active resource assignments for the selected scope" /></td></tr>
                    ) : Object.entries(analytics.assignedResourceCountsByType || {}).map(([type, count]) => (
                      <tr key={type}>
                        <td className="ps-3 fw-medium">{formatResourceType(type)}</td>
                        <td className="text-end pe-3 fw-bold">{count}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          {/* Footer Metadata */}
          <div className="text-end text-muted small">
            Calculated at: {new Date(analytics.calculatedAt).toLocaleString()}
          </div>
        </>
      )}
    </div>
  );
}

function formatResourceType(type) {
  switch (type) {
    case 'VACCINATION_STATION':
      return 'Vaccination Stations';
    case 'EQUIPMENT':
      return 'Operational Equipment';
    case 'ROOM':
      return 'Rooms & Facility Spaces';
    default:
      return type;
  }
}
