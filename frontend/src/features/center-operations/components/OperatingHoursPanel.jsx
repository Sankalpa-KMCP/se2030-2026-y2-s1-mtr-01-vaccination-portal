import React, { useState } from 'react';
import LoadingSpinner from '../../../components/common/LoadingSpinner';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';

const DAY_ORDER = {
  MONDAY: 1,
  TUESDAY: 2,
  WEDNESDAY: 3,
  THURSDAY: 4,
  FRIDAY: 5,
  SATURDAY: 6,
  SUNDAY: 7,
};

const DAY_LABELS = {
  MONDAY: 'Monday',
  TUESDAY: 'Tuesday',
  WEDNESDAY: 'Wednesday',
  THURSDAY: 'Thursday',
  FRIDAY: 'Friday',
  SATURDAY: 'Saturday',
  SUNDAY: 'Sunday',
};

function formatTime(timeStr) {
  if (!timeStr) return '—';
  // If timeStr is HH:mm:ss, show HH:mm
  return timeStr.length > 5 ? timeStr.substring(0, 5) : timeStr;
}

function formatDate(dateStr) {
  if (!dateStr) return 'Ongoing / Open-Ended';
  return dateStr;
}

function getShiftStatusBadge(shift) {
  const today = new Date().toISOString().substring(0, 10);
  if (shift.effectiveTo && shift.effectiveTo < today) {
    return <span className="badge bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle">Ended / Expired</span>;
  }
  if (shift.effectiveFrom > today) {
    return <span className="badge bg-info-subtle text-info-emphasis border border-info-subtle">Future Shift</span>;
  }
  return <span className="badge bg-success-subtle text-success-emphasis border border-success-subtle">Active / Current</span>;
}

export default function OperatingHoursPanel({
  center,
  operatingHours = [],
  loading = false,
  error = null,
  isAuthorized = false,
  onRefresh,
  onAddHours,
  onEditHours,
  onEndDateHours,
}) {
  const [dayFilter, setDayFilter] = useState('ALL');

  const isCenterInactive = center?.status === 'INACTIVE';
  const canMutate = isAuthorized && !isCenterInactive;

  const sortedHours = [...operatingHours].sort((a, b) => {
    const dayDiff = (DAY_ORDER[a.dayOfWeek] || 99) - (DAY_ORDER[b.dayOfWeek] || 99);
    if (dayDiff !== 0) return dayDiff;
    return String(a.opening).localeCompare(String(b.opening));
  });

  const filteredHours = sortedHours.filter((h) => {
    if (dayFilter === 'ALL') return true;
    return h.dayOfWeek === dayFilter;
  });

  return (
    <div className="card shadow-sm border-0">
      <div className="card-header bg-white py-3 border-bottom d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-2">
        <div>
          <h2 className="h5 mb-1 text-primary fw-bold">
            <i className="bi bi-clock-history me-2"></i>
            Weekly Operating Hours & Shifts
          </h2>
          <p className="small text-muted mb-0">
            Authoritative weekly operational schedule defining shift opening, closing, and effective dates for {center?.name}.
          </p>
        </div>

        <div className="d-flex gap-2">
          <button
            type="button"
            className="btn btn-sm btn-outline-secondary"
            onClick={onRefresh}
            disabled={loading}
            title="Refresh operating hours"
          >
            <i className="bi bi-arrow-clockwise me-1"></i>
            Refresh
          </button>

          {canMutate && (
            <button
              type="button"
              className="btn btn-sm btn-primary fw-semibold"
              onClick={onAddHours}
              disabled={loading}
            >
              <i className="bi bi-plus-lg me-1"></i>
              Add Operating Hours
            </button>
          )}
        </div>
      </div>

      <div className="card-body p-4">
        <ApiErrorAlert error={error} />

        {/* Day Filter Pills */}
        <div className="d-flex flex-wrap gap-1 mb-3">
          <button
            type="button"
            className={`btn btn-sm ${dayFilter === 'ALL' ? 'btn-secondary text-white' : 'btn-outline-secondary'}`}
            onClick={() => setDayFilter('ALL')}
          >
            All Days ({operatingHours.length})
          </button>
          {Object.keys(DAY_ORDER).map((day) => {
            const count = operatingHours.filter((h) => h.dayOfWeek === day).length;
            return (
              <button
                key={day}
                type="button"
                className={`btn btn-sm ${dayFilter === day ? 'btn-primary text-white' : 'btn-outline-secondary'}`}
                onClick={() => setDayFilter(day)}
              >
                {DAY_LABELS[day]} ({count})
              </button>
            );
          })}
        </div>

        {loading ? (
          <div className="py-5">
            <LoadingSpinner message="Loading center operating hours schedule..." />
          </div>
        ) : operatingHours.length === 0 ? (
          <div className="text-center py-5 text-secondary border rounded bg-light">
            <div className="display-6 text-muted mb-3">🕒</div>
            <h5 className="h6 text-muted mb-2">No Operating Hours Defined</h5>
            <p className="small text-muted mb-3">
              This vaccination center currently has no configured operating hours schedule shifts.
            </p>
            {canMutate && (
              <button
                type="button"
                className="btn btn-primary btn-sm fw-semibold"
                onClick={onAddHours}
              >
                <i className="bi bi-plus-lg me-1"></i>
                Define First Operating Hours Shift
              </button>
            )}
          </div>
        ) : filteredHours.length === 0 ? (
          <div className="alert alert-light border text-center py-4">
            <p className="text-muted mb-0 small">
              No operating hours shifts found for <strong>{DAY_LABELS[dayFilter] || dayFilter}</strong>.
            </p>
          </div>
        ) : (
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead className="table-light text-secondary small">
                <tr>
                  <th scope="col">Day of Week</th>
                  <th scope="col">Operating Window</th>
                  <th scope="col">Effective Period</th>
                  <th scope="col">Status</th>
                  {canMutate && <th scope="col" className="text-end pe-3">Actions</th>}
                </tr>
              </thead>
              <tbody>
                {filteredHours.map((shift) => {
                  const today = new Date().toISOString().substring(0, 10);
                  const isExpired = shift.effectiveTo && shift.effectiveTo < today;

                  return (
                    <tr key={shift.id}>
                      <td>
                        <span className="fw-bold text-dark">
                          {DAY_LABELS[shift.dayOfWeek] || shift.dayOfWeek}
                        </span>
                      </td>
                      <td>
                        <span className="font-monospace fw-semibold text-primary">
                          {formatTime(shift.opening)} — {formatTime(shift.closing)}
                        </span>
                      </td>
                      <td className="small text-muted">
                        <div>From: <span className="fw-semibold text-dark">{shift.effectiveFrom}</span></div>
                        <div>To: <span className="fw-semibold text-dark">{formatDate(shift.effectiveTo)}</span></div>
                      </td>
                      <td>
                        {getShiftStatusBadge(shift)}
                      </td>
                      {canMutate && (
                        <td className="text-end pe-3">
                          <div className="btn-group btn-group-sm" role="group">
                            <button
                              type="button"
                              className="btn btn-outline-secondary btn-sm"
                              onClick={() => onEditHours(shift)}
                              disabled={isExpired}
                              title={isExpired ? 'Expired shifts cannot be edited' : 'Edit shift schedule'}
                            >
                              <i className="bi bi-pencil me-1"></i>
                              Edit
                            </button>
                            <button
                              type="button"
                              className="btn btn-outline-warning text-dark btn-sm"
                              onClick={() => onEndDateHours(shift)}
                              disabled={isExpired}
                              title={isExpired ? 'Shift is already ended' : 'End date / shorten validity period'}
                            >
                              <i className="bi bi-calendar-x me-1"></i>
                              End Date
                            </button>
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
        <span>Total Shifts: {operatingHours.length}</span>
        <span>Center Scheduling Timezone: {center?.timezone || 'UTC'}</span>
      </div>
    </div>
  );
}
