import React, { useState, useMemo } from 'react';
import LoadingSpinner from '../../../components/common/LoadingSpinner';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';

const ROLE_CONFIG = {
  VACCINATOR: {
    label: 'Vaccinator',
    badgeClass: 'bg-primary-subtle text-primary border border-primary-subtle',
  },
  CHECK_IN_STAFF: {
    label: 'Check-In Staff',
    badgeClass: 'bg-info-subtle text-info-emphasis border border-info-subtle',
  },
  SUPERVISOR: {
    label: 'Supervisor',
    badgeClass: 'bg-warning-subtle text-warning-emphasis border border-warning-subtle',
  },
};

const STATUS_CONFIG = {
  ACTIVE: {
    label: 'Active',
    badgeClass: 'bg-success',
  },
  INACTIVE: {
    label: 'Inactive',
    badgeClass: 'bg-secondary',
  },
};

/**
 * Panel component to display and manage operational staff roster for a selected vaccination center.
 */
export default function StaffRosterPanel({
  center,
  staffRoster = [],
  loading = false,
  error = null,
  onRefresh,
  onRegisterClick,
  onEditClick,
}) {
  const [selectedRole, setSelectedRole] = useState('ALL');
  const [selectedStatus, setSelectedStatus] = useState('ALL');

  const isCenterInactive = center?.status === 'INACTIVE';

  // Dynamic counts per role
  const counts = useMemo(() => {
    const roleCounts = {
      ALL: staffRoster.length,
      VACCINATOR: 0,
      CHECK_IN_STAFF: 0,
      SUPERVISOR: 0,
    };

    staffRoster.forEach((member) => {
      if (member.role && roleCounts[member.role] !== undefined) {
        roleCounts[member.role] += 1;
      }
    });

    return roleCounts;
  }, [staffRoster]);

  // Filtered staff list
  const filteredRoster = useMemo(() => {
    return staffRoster.filter((member) => {
      const matchRole = selectedRole === 'ALL' || member.role === selectedRole;
      const matchStatus = selectedStatus === 'ALL' || member.status === selectedStatus;
      return matchRole && matchStatus;
    });
  }, [staffRoster, selectedRole, selectedStatus]);

  return (
    <div className="card shadow-sm border-0 mb-4">
      {/* Header & Actions */}
      <div className="card-header bg-white py-3 border-bottom d-flex flex-wrap justify-content-between align-items-center gap-2">
        <div>
          <h5 className="card-title fw-bold mb-1">Center Staff Roster</h5>
          <p className="card-subtitle text-muted small mb-0">
            Operational workforce roster for {center ? `[${center.code}] ${center.name}` : 'the selected center'}
          </p>
        </div>

        <div className="d-flex align-items-center gap-2">
          <button
            type="button"
            className="btn btn-outline-secondary btn-sm d-flex align-items-center gap-1"
            onClick={onRefresh}
            disabled={loading}
            title="Refresh staff roster"
          >
            <svg width="14" height="14" fill="currentColor" viewBox="0 0 16 16">
              <path fillRule="evenodd" d="M8 3a5 5 0 1 0 4.546 2.914.5.5 0 0 1 .908-.417A6 6 0 1 1 8 2v1z" />
              <path d="M8 4.466V.534a.25.25 0 0 1 .41-.192l2.36 1.966c.12.1.12.284 0 .384L8.41 4.658A.25.25 0 0 1 8 4.466z" />
            </svg>
            <span>Refresh</span>
          </button>

          <button
            type="button"
            className="btn btn-primary btn-sm px-3 fw-semibold d-flex align-items-center gap-1"
            onClick={onRegisterClick}
            disabled={loading || isCenterInactive}
            title={isCenterInactive ? 'Cannot register staff in an inactive center' : 'Register a new staff member on this center roster'}
          >
            <svg width="14" height="14" fill="currentColor" viewBox="0 0 16 16">
              <path d="M8 4a.5.5 0 0 1 .5.5v3h3a.5.5 0 0 1 0 1h-3v3a.5.5 0 0 1-1 0v-3h-3a.5.5 0 0 1 0-1h3v-3A.5.5 0 0 1 8 4z" />
            </svg>
            <span>Register Staff</span>
          </button>
        </div>
      </div>

      <div className="card-body p-4">
        {/* Inactive Center Notice */}
        {isCenterInactive && (
          <div className="alert alert-warning d-flex align-items-center gap-2 mb-3 py-2 px-3 small border-0 shadow-sm" role="alert">
            <svg width="16" height="16" fill="currentColor" className="bi bi-exclamation-triangle-fill flex-shrink-0" viewBox="0 0 16 16">
              <path d="M8.982 1.566a1.13 1.13 0 0 0-1.96 0L.165 13.233c-.457.778.091 1.767.98 1.767h13.713c.889 0 1.438-.99.98-1.767L8.982 1.566zM8 5c.535 0 .954.462.9.995l-.35 3.507a.552.552 0 0 1-1.1 0L7.1 5.995A.905.905 0 0 1 8 5zm.002 6a1 1 0 1 1 0 2 1 1 0 0 1 0-2z"/>
            </svg>
            <div>
              This vaccination center is currently <strong>INACTIVE (decommissioned)</strong>. Staff roster records are displayed in read-only mode.
            </div>
          </div>
        )}

        {/* Filters and Search Toolbar */}
        <div className="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4 pb-2 border-bottom">
          {/* Role Filter Pills */}
          <div className="d-flex flex-wrap gap-2 align-items-center">
            <span className="text-muted small fw-semibold me-1">Role:</span>
            <button
              type="button"
              className={`btn btn-sm ${selectedRole === 'ALL' ? 'btn-primary' : 'btn-outline-secondary'}`}
              onClick={() => setSelectedRole('ALL')}
            >
              All Roles <span className="badge bg-white text-dark ms-1">{counts.ALL}</span>
            </button>
            <button
              type="button"
              className={`btn btn-sm ${selectedRole === 'VACCINATOR' ? 'btn-primary' : 'btn-outline-secondary'}`}
              onClick={() => setSelectedRole('VACCINATOR')}
            >
              Vaccinators <span className="badge bg-white text-dark ms-1">{counts.VACCINATOR}</span>
            </button>
            <button
              type="button"
              className={`btn btn-sm ${selectedRole === 'CHECK_IN_STAFF' ? 'btn-primary' : 'btn-outline-secondary'}`}
              onClick={() => setSelectedRole('CHECK_IN_STAFF')}
            >
              Check-In Staff <span className="badge bg-white text-dark ms-1">{counts.CHECK_IN_STAFF}</span>
            </button>
            <button
              type="button"
              className={`btn btn-sm ${selectedRole === 'SUPERVISOR' ? 'btn-primary' : 'btn-outline-secondary'}`}
              onClick={() => setSelectedRole('SUPERVISOR')}
            >
              Supervisors <span className="badge bg-white text-dark ms-1">{counts.SUPERVISOR}</span>
            </button>
          </div>

          {/* Status Filter */}
          <div className="d-flex align-items-center gap-2">
            <label htmlFor="staff-status-filter" className="text-muted small fw-semibold text-nowrap mb-0">
              Status:
            </label>
            <select
              id="staff-status-filter"
              className="form-select form-select-sm"
              style={{ width: '130px' }}
              value={selectedStatus}
              onChange={(e) => setSelectedStatus(e.target.value)}
            >
              <option value="ALL">All Statuses</option>
              <option value="ACTIVE">Active</option>
              <option value="INACTIVE">Inactive</option>
            </select>
          </div>
        </div>

        {/* Error Alert */}
        {error && <ApiErrorAlert error={error} />}

        {/* Loading State */}
        {loading ? (
          <div className="py-5 text-center">
            <LoadingSpinner message="Loading center staff roster..." />
          </div>
        ) : staffRoster.length === 0 ? (
          /* Empty Initial Roster */
          <div className="text-center py-5 border rounded-3 bg-light">
            <div className="mb-3 text-muted">
              <svg width="48" height="48" fill="currentColor" className="bi bi-people" viewBox="0 0 16 16">
                <path d="M15 14s1 0 1-1-1-4-5-4-5 3-5 4 1 1 1 1h8Zm-7.978-1A.261.261 0 0 1 7 12.996c.001-.264.167-1.03.76-1.72C8.312 10.629 9.282 10 11 10c1.717 0 2.687.63 3.24 1.276.593.69.758 1.457.76 1.72l-.008.002a.274.274 0 0 1-.014.002H7.022ZM11 7a2 2 0 1 0 0-4 2 2 0 0 0 0 4Zm3-2a3 3 0 1 1-6 0 3 3 0 0 1 6 0ZM6.736 9.48c.37-.301.796-.536 1.264-.69A4.012 4.012 0 0 0 5 8a4 4 0 0 0-4 4v1h7.022q.007-.067.023-.138c.074-.35.215-.718.423-1.07-.408-.182-.823-.338-1.232-.472V9.48ZM5 7a3 3 0 1 1 0-6 3 3 0 0 1 0 6Zm0 1a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z"/>
              </svg>
            </div>
            <h6 className="fw-bold text-dark">No Operational Staff Rostered</h6>
            <p className="text-muted small max-w-md mx-auto mb-3" style={{ maxWidth: '420px' }}>
              No operational staff members are currently registered on this center's roster. Click "Register Staff" above to roster clinical vaccinators, check-in staff, or supervisors.
            </p>
            {!isCenterInactive && (
              <button
                type="button"
                className="btn btn-outline-primary btn-sm"
                onClick={onRegisterClick}
              >
                + Register First Staff Member
              </button>
            )}
          </div>
        ) : filteredRoster.length === 0 ? (
          /* Filtered Empty State */
          <div className="text-center py-4 border rounded-3 bg-light">
            <p className="text-muted mb-0 small">
              No staff members match the selected role or status filters.
            </p>
            <button
              type="button"
              className="btn btn-link btn-sm text-decoration-none mt-1"
              onClick={() => {
                setSelectedRole('ALL');
                setSelectedStatus('ALL');
              }}
            >
              Reset Filters
            </button>
          </div>
        ) : (
          /* Staff Roster Table */
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead className="table-light">
                <tr>
                  <th scope="col" style={{ width: '22%' }}>User Identifier</th>
                  <th scope="col" style={{ width: '28%' }}>Assigned Role</th>
                  <th scope="col" style={{ width: '20%' }}>Operational Status</th>
                  <th scope="col" style={{ width: '15%' }}>Roster ID</th>
                  <th scope="col" className="text-end" style={{ width: '15%' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredRoster.map((member) => {
                  const roleInfo = ROLE_CONFIG[member.role] || {
                    label: member.role,
                    badgeClass: 'bg-secondary-subtle text-secondary',
                  };
                  const statusInfo = STATUS_CONFIG[member.status] || {
                    label: member.status,
                    badgeClass: 'bg-secondary',
                  };

                  return (
                    <tr key={member.id}>
                      <td>
                        <div className="d-flex align-items-center gap-2">
                          <span className="badge bg-light text-dark border font-monospace px-2 py-1 fs-7">
                            User #{member.userId}
                          </span>
                        </div>
                      </td>
                      <td>
                        <span className={`badge ${roleInfo.badgeClass} px-2 py-1`}>
                          {roleInfo.label}
                        </span>
                      </td>
                      <td>
                        <span className={`badge ${statusInfo.badgeClass}`}>
                          {statusInfo.label}
                        </span>
                      </td>
                      <td>
                        <span className="text-muted small font-monospace">
                          #{member.id}
                        </span>
                      </td>
                      <td className="text-end">
                        {isCenterInactive ? (
                          <span className="text-muted small fst-italic">Read-only</span>
                        ) : (
                          <button
                            type="button"
                            className="btn btn-outline-primary btn-sm py-0 px-2 fw-semibold"
                            onClick={() => onEditClick(member)}
                            title="Edit staff role or operational status"
                          >
                            Edit
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
