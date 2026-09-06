import React, { useMemo } from 'react';
import { formatDateInTimezone, formatTimeInTimezone } from '../api/sessionApi';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import LoadingSpinner from '../../../components/common/LoadingSpinner';
import StatusBadge from '../../../components/common/StatusBadge';
import EmptyState from '../../../components/common/EmptyState';
import {
  sessionStatusVariants,
  sessionBookingStatusVariants,
  sessionBookingStatusTitles as BOOKING_STATUS_TITLES,
} from '../../../components/common/statusVariants';

export default function SessionListPanel({
  sessions = [],
  loading = false,
  error = null,
  filters = {},
  onFilterChange,
  onResetFilters,
  onViewDetails,
  onOpenCreateModal,
  campaignsList = [],
  centersList = [],
  campaignsMap = {},
  centersMap = {},
}) {
  // Client-side date filtering against loaded session startAt
  const filteredSessions = useMemo(() => {
    if (!filters.dateQuery) return sessions;
    return sessions.filter((s) => {
      const tz = s.scheduleTimezone || 'UTC';
      // Format startAt into YYYY-MM-DD in the session's schedule timezone for exact date comparison
      try {
        const formatter = new Intl.DateTimeFormat('en-CA', {
          timeZone: tz,
          year: 'numeric',
          month: '2-digit',
          day: '2-digit',
        });
        const localDate = formatter.format(new Date(s.startAt));
        return localDate === filters.dateQuery;
      } catch {
        return s.startAt.startsWith(filters.dateQuery);
      }
    });
  }, [sessions, filters.dateQuery]);

  // Status counts for status badges
  const counts = useMemo(() => {
    const summary = {
      TOTAL: sessions.length,
      DRAFT: 0,
      SCHEDULED: 0,
      IN_PROGRESS: 0,
      COMPLETED: 0,
      CANCELLED: 0,
    };
    sessions.forEach((s) => {
      if (summary[s.lifecycleStatus] !== undefined) {
        summary[s.lifecycleStatus]++;
      }
    });
    return summary;
  }, [sessions]);

  const getStatusBadge = (status) => (
    <StatusBadge status={status || 'UNKNOWN'} variants={sessionStatusVariants} />
  );

  const getBookingStatusBadge = (bookingStatus) => (
    <StatusBadge
      status={bookingStatus || 'CLOSED'}
      variants={sessionBookingStatusVariants}
      title={BOOKING_STATUS_TITLES[bookingStatus]}
    />
  );


  return (
    <div className="card shadow-sm border-0">
      <div className="card-body p-4">
        {/* Filters Toolbar */}
        <div className="bg-light p-3 rounded border mb-4">
          <div className="row g-3 align-items-end">
            {/* Campaign Filter */}
            <div className="col-md-3">
              <label htmlFor="filterCampaignSelect" className="form-label small fw-semibold text-muted mb-1">
                Filter by Campaign
              </label>
              <select
                id="filterCampaignSelect"
                className="form-select form-select-sm"
                value={filters.campaignId || ''}
                onChange={(e) => onFilterChange('campaignId', e.target.value)}
              >
                <option value="">All Campaigns</option>
                {campaignsList.map((c) => (
                  <option key={c.id} value={c.id}>
                    [{c.code}] {c.name}
                  </option>
                ))}
              </select>
            </div>

            {/* Center Filter */}
            <div className="col-md-3">
              <label htmlFor="filterCenterSelect" className="form-label small fw-semibold text-muted mb-1">
                Filter by Center
              </label>
              <select
                id="filterCenterSelect"
                className="form-select form-select-sm"
                value={filters.centerId || ''}
                onChange={(e) => onFilterChange('centerId', e.target.value)}
              >
                <option value="">All Centers</option>
                {centersList.map((c) => (
                  <option key={c.id} value={c.id}>
                    [{c.code}] {c.name}
                  </option>
                ))}
              </select>
            </div>

            {/* Status Filter */}
            <div className="col-md-2">
              <label htmlFor="filterStatusSelect" className="form-label small fw-semibold text-muted mb-1">
                Status
              </label>
              <select
                id="filterStatusSelect"
                className="form-select form-select-sm"
                value={filters.status || ''}
                onChange={(e) => onFilterChange('status', e.target.value)}
              >
                <option value="">All Statuses</option>
                <option value="DRAFT">DRAFT</option>
                <option value="SCHEDULED">SCHEDULED</option>
                <option value="IN_PROGRESS">IN_PROGRESS</option>
                <option value="COMPLETED">COMPLETED</option>
                <option value="CANCELLED">CANCELLED</option>
              </select>
            </div>

            {/* Date Filter (Client-side) */}
            <div className="col-md-2">
              <label htmlFor="filterDateInput" className="form-label small fw-semibold text-muted mb-1">
                Session Date
              </label>
              <input
                type="date"
                id="filterDateInput"
                className="form-control form-control-sm"
                value={filters.dateQuery || ''}
                onChange={(e) => onFilterChange('dateQuery', e.target.value)}
              />
            </div>

            {/* Reset Actions */}
            <div className="col-md-2 d-flex gap-2">
              <button
                type="button"
                className="btn btn-sm btn-outline-secondary w-100"
                onClick={onResetFilters}
                title="Reset all filters"
              >
                Reset
              </button>
            </div>
          </div>
        </div>

        {/* Status Count Badges Summary */}
        <div className="d-flex flex-wrap gap-2 mb-3 align-items-center">
          <span className="badge bg-light text-dark border p-2">
            Total Sessions: <strong className="ms-1">{counts.TOTAL}</strong>
          </span>
          <span className="badge bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle p-2">
            Draft: <strong className="ms-1">{counts.DRAFT}</strong>
          </span>
          <span className="badge bg-primary-subtle text-primary-emphasis border border-primary-subtle p-2">
            Scheduled: <strong className="ms-1">{counts.SCHEDULED}</strong>
          </span>
          <span className="badge bg-success-subtle text-success-emphasis border border-success-subtle p-2">
            In Progress: <strong className="ms-1">{counts.IN_PROGRESS}</strong>
          </span>
          <span className="badge bg-dark-subtle text-dark-emphasis border border-dark-subtle p-2">
            Completed: <strong className="ms-1">{counts.COMPLETED}</strong>
          </span>
          <span className="badge bg-danger-subtle text-danger-emphasis border border-danger-subtle p-2">
            Cancelled: <strong className="ms-1">{counts.CANCELLED}</strong>
          </span>
        </div>

        {/* Error Alert */}
        {error && <ApiErrorAlert error={error} />}

        {/* Loading Spinner */}
        {loading && <LoadingSpinner message="Loading vaccination sessions..." />}

        {/* Sessions Table */}
        {!loading && !error && (
          <div className="table-responsive">
            <table className="table table-hover align-middle border mb-0">
              <thead className="table-light">
                <tr>
                  <th scope="col" style={{ width: '80px' }}>ID</th>
                  <th scope="col">Campaign</th>
                  <th scope="col">Hosting Center</th>
                  <th scope="col">Session Schedule</th>
                  <th scope="col" style={{ width: '130px' }}>Timezone</th>
                  <th scope="col" style={{ width: '120px' }}>Status</th>
                  <th scope="col" className="text-end" style={{ width: '110px' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredSessions.length === 0 ? (
                  <tr>
                    <td colSpan="7">
                      <EmptyState
                        icon="bi-calendar-week"
                        title="No vaccination sessions found"
                        action={sessions.length === 0 && onOpenCreateModal && (
                          <button
                            type="button"
                            className="btn btn-sm btn-primary"
                            onClick={onOpenCreateModal}
                          >
                            Create First Session
                          </button>
                        )}
                      >
                        {sessions.length === 0
                          ? 'No sessions have been scheduled yet.'
                          : 'No sessions match your filter criteria.'}
                      </EmptyState>
                    </td>
                  </tr>
                ) : (
                  filteredSessions.map((s) => {
                    const campaign = campaignsMap[s.campaignId];
                    const center = centersMap[s.centerId];
                    const tz = s.scheduleTimezone || 'UTC';

                    const dateStr = formatDateInTimezone(s.startAt, tz);
                    const startTimeStr = formatTimeInTimezone(s.startAt, tz);
                    const endTimeStr = formatTimeInTimezone(s.endAt, tz);

                    return (
                      <tr key={s.id}>
                        <td>
                          <span className="font-monospace text-muted small">#{s.id}</span>
                        </td>
                        <td>
                          {campaign ? (
                            <div>
                              <strong className="text-dark d-block">{campaign.name}</strong>
                              <span className="badge bg-light text-muted border small font-monospace">
                                {campaign.code}
                              </span>
                            </div>
                          ) : (
                            <span className="text-muted small">Campaign #{s.campaignId}</span>
                          )}
                        </td>
                        <td>
                          {center ? (
                            <div>
                              <strong className="text-dark d-block">{center.name}</strong>
                              <span className="badge bg-light text-muted border small font-monospace">
                                {center.code}
                              </span>
                            </div>
                          ) : (
                            <span className="text-muted small">Center #{s.centerId}</span>
                          )}
                        </td>
                        <td>
                          <div>
                            <strong className="text-dark d-block">{dateStr}</strong>
                            <span className="small text-secondary">
                              {startTimeStr} – {endTimeStr}
                            </span>
                          </div>
                        </td>
                        <td>
                          <span className="badge bg-light text-dark border font-monospace small">
                            {tz}
                          </span>
                        </td>
                        <td>
                          <div className="d-flex flex-column gap-1">
                            <div>{getStatusBadge(s.lifecycleStatus)}</div>
                            {s.bookingStatus && (
                              <div>{getBookingStatusBadge(s.bookingStatus)}</div>
                            )}
                          </div>
                        </td>

                        <td className="text-end">
                          <button
                            type="button"
                            className="btn btn-sm btn-outline-primary"
                            onClick={() => onViewDetails(s.id)}
                            title="View session details"
                          >
                            Details
                          </button>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
