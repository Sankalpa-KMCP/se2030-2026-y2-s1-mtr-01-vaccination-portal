import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../../auth/context/AuthContext';
import { ADMIN_ROLE, CITIZEN_ROLE, getRoleLabel } from '../../auth/constants/authRoles';
import {
  canViewAnalytics,
  canViewCampaigns,
  canViewCenters,
  canViewSessions,
  hasRole,
} from '../../auth/utils/roleUtils';
import { getRoleHomePath } from '../../auth/utils/roleRedirect';

function navigationClassName({ isActive }) {
  return `btn btn-sm ${isActive ? 'btn-light text-primary fw-bold' : 'btn-outline-light border-0'}`;
}

export default function Header({ onLogout }) {
  const { user, logout } = useAuth();

  // Optional override so the app shell can distinguish voluntary logout
  // (e.g. to return to the public landing page) from session expiry.
  const handleLogout = onLogout || logout;

  const showCampaigns = canViewCampaigns(user);
  const showCenters = canViewCenters(user);
  const showSessions = canViewSessions(user);
  const showAnalytics = canViewAnalytics(user);
  const showAdminNavigation = hasRole(user, ADMIN_ROLE);
  const showCitizenNavigation = hasRole(user, CITIZEN_ROLE);
  const dashboardPath = getRoleHomePath(user);

  return (
    <header>
      <nav className="navbar navbar-expand-lg navbar-dark bg-primary shadow-sm" aria-label="Main Navigation">
        <div className="container">
          <span className="navbar-brand fw-bold me-4">
            Web-Based Vaccination Portal
          </span>

          <div className="d-flex align-items-center flex-grow-1">
            <ul className="navbar-nav me-auto mb-2 mb-lg-0 flex-row gap-2">
              <li className="nav-item">
                <NavLink className={navigationClassName} to={dashboardPath}>
                  Dashboard
                </NavLink>
              </li>

              {showCitizenNavigation && (
                <>
                  <li className="nav-item">
                    <NavLink className={navigationClassName} to="/app/appointments/book">
                      Book Appointment
                    </NavLink>
                  </li>
                  <li className="nav-item">
                    <NavLink className={navigationClassName} to="/app/appointments/mine">
                      My Appointments
                    </NavLink>
                  </li>
                  <li className="nav-item">
                    <NavLink className={navigationClassName} to="/app/appointments/waitlist">
                      Waitlist
                    </NavLink>
                  </li>
                </>
              )}

              {showCampaigns && (
                <li className="nav-item">
                  <NavLink className={navigationClassName} to="/app/campaigns">
                    Campaigns
                  </NavLink>
                </li>
              )}

              {showCenters && (
                <li className="nav-item">
                  <NavLink className={navigationClassName} to="/app/center-operations">
                    Center Operations
                  </NavLink>
                </li>
              )}

              {showSessions && (
                <li className="nav-item">
                  <NavLink className={navigationClassName} to="/app/sessions">
                    Sessions
                  </NavLink>
                </li>
              )}

              {showAnalytics && (
                <li className="nav-item">
                  <NavLink className={navigationClassName} to="/app/analytics">
                    Analytics
                  </NavLink>
                </li>
              )}

              {showAdminNavigation && (
                <>
                  <li className="nav-item">
                    <NavLink className={navigationClassName} to="/app/admin/staff-requests">
                      Staff Requests
                    </NavLink>
                  </li>
                  <li className="nav-item">
                    <NavLink className={navigationClassName} to="/app/screening/questions">
                      Screening Questions
                    </NavLink>
                  </li>
                  <li className="nav-item">
                    <NavLink className={navigationClassName} to="/app/screening/questionnaires">
                      Questionnaires
                    </NavLink>
                  </li>
                </>
              )}
            </ul>

            {user && (
              <div className="d-flex align-items-center gap-2 ms-auto">
                <div className="text-end d-none d-sm-block text-white me-2">
                  <div className="small fw-semibold">{user.fullName || user.email}</div>
                  {user.roles && user.roles.length > 0 && (
                    <div className="d-flex gap-1 justify-content-end">
                      {user.roles.map((role) => (
                        <span key={role} className="badge bg-light text-primary py-0 px-1" style={{ fontSize: '0.7rem' }}>
                          {getRoleLabel(role)}
                        </span>
                      ))}
                    </div>
                  )}
                </div>
                <button
                  type="button"
                  className="btn btn-outline-light btn-sm px-3"
                  onClick={handleLogout}
                  title="Sign out of your session"
                >
                  Logout
                </button>
              </div>
            )}
          </div>
        </div>
      </nav>
    </header>
  );
}
