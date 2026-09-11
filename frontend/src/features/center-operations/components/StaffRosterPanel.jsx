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
            <i className="bi bi-arrow-clockwise" aria-hidden="true"></i>
            <span>Refresh</span>
          </button>

          <button
            type="button"
            className="btn btn-primary btn-sm px-3 fw-semibold d-flex align-items-center gap-1"
            onClick={onRegisterClick}
            disabled={loading || isCenterInactive}
            title={isCenterInactive ? 'Cannot register staff in an inactive center' : 'Register a new staff member on this center roster'}
          >
            <i className="bi bi-plus-lg" aria-hidden="true"></i>
            <span>Register Staff</span>
          </button>
        </div>
      </div>

      <div className="card-body p-4">
        {/* Inactive Center Notice */}
        {isCenterInactive && (
          <div className="alert alert-warning d-flex align-items-center gap-2 mb-3 py-2 px-3 small border-0 shadow-sm" role="alert">
            <i className="bi bi-exclamation-triangle-fill flex-shrink-0" aria-hidden="true"></i>
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
              <i className="bi bi-people" style={{ fontSize: '3rem' }} aria-hidden="true"></i>
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
