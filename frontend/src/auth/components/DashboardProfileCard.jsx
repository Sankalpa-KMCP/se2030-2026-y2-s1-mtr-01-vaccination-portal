import React from 'react';
import { getRoleLabel } from '../constants/authRoles';

export default function DashboardProfileCard({ user, showRoles = false }) {
  return (
    <div className="card border-0 shadow-sm h-100">
      <div className="card-body p-4">
        <h2 className="h5 mb-3">Account Summary</h2>
        <dl className="row mb-0">
          <dt className="col-sm-4 text-muted">Full Name</dt>
          <dd className="col-sm-8">{user?.fullName || 'Not provided'}</dd>
          <dt className="col-sm-4 text-muted">Email</dt>
          <dd className="col-sm-8">{user?.email || 'Not provided'}</dd>
          <dt className="col-sm-4 text-muted">Phone Number</dt>
          <dd className="col-sm-8">{user?.phoneNumber || 'Not provided'}</dd>
          {showRoles && (
            <>
              <dt className="col-sm-4 text-muted">Portal Roles</dt>
              <dd className="col-sm-8 mb-0">
                {(user?.roles || []).map((role) => (
                  <span className="badge text-bg-light border text-dark me-1" key={role}>{getRoleLabel(role)}</span>
                ))}
              </dd>
            </>
          )}
        </dl>
      </div>
    </div>
  );
}
