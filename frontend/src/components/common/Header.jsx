import React, { useState } from 'react';
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
  return `nav-link${isActive ? ' active' : ''}`;
}

export default function Header({ onLogout }) {
  const { user, logout } = useAuth();
  const [navOpen, setNavOpen] = useState(false);

  // Optional override so the app shell can distinguish voluntary logout
  // (e.g. to return to the public landing page) from session expiry.
  const handleLogout = onLogout || logout;

  const showCampaigns = canViewCampaigns(user);
  const showCenters = canViewCenters(user);
  const showSessions = canViewSessions(user);
  const showAnalytics = canViewAnalytics(user);
  const showCheckIn = hasRole(user, ADMIN_ROLE, 'ROLE_HEALTHCARE_OFFICER', 'ROLE_CENTER_MANAGER');
  const showAdminNavigation = hasRole(user, ADMIN_ROLE);
  const showScreeningConfigurationNavigation = hasRole(
    user,
    ADMIN_ROLE,
    'ROLE_SYSTEM_ADMINISTRATOR',
  );
  const showCitizenNavigation = hasRole(user, CITIZEN_ROLE);
  const showScreeningReview = hasRole(user, 'ROLE_HEALTHCARE_OFFICER');
  const dashboardPath = getRoleHomePath(user);

  const closeNav = () => setNavOpen(false);

  return (
    <header>
      <nav className="navbar navbar-expand-lg navbar-dark bg-primary shadow-sm" aria-label="Main Navigation">
        <div className="container">
          <span className="navbar-brand fw-bold me-lg-4">
            Web-Based Vaccination Portal
          </span>

          <button
            type="button"
            className="navbar-toggler"
            aria-controls="appPrimaryNav"
            aria-expanded={navOpen}
            aria-label="Toggle navigation"
            onClick={() => setNavOpen((open) => !open)}
          >
            <span className="navbar-toggler-icon" />
          </button>

          <div
            id="appPrimaryNav"
            className={`collapse navbar-collapse${navOpen ? ' show' : ''}`}
          >
            <ul className="navbar-nav app-nav me-auto mb-2 mb-lg-0 pt-3 pt-lg-0">
              <li className="nav-item">
                <NavLink className={navigationClassName} to={dashboardPath} onClick={closeNav}>
                  Dashboard
                </NavLink>
              </li>

              {showCitizenNavigation && (
                <>
                  <li className="nav-item">
                    <NavLink className={navigationClassName} to="/app/appointments/book" onClick={closeNav}>
                      Book Appointment
                    </NavLink>
                  </li>
                  <li className="nav-item">
                    <NavLink className={navigationClassName} to="/app/appointments/mine" onClick={closeNav}>
                      My Appointments
                    </NavLink>
                  </li>
                  <li className="nav-item">
                    <NavLink className={navigationClassName} to="/app/appointments/waitlist" onClick={closeNav}>
                      Waitlist
                    </NavLink>
                  </li>
                </>
              )}

              {showScreeningReview && (
                <li className="nav-item">
                  <NavLink className={navigationClassName} to="/app/staff/screening/reviews" onClick={closeNav}>
                    Screening Review
                  </NavLink>
                </li>
              )}

              {showCampaigns && (
                <li className="nav-item">
                  <NavLink className={navigationClassName} to="/app/campaigns" onClick={closeNav}>
                    Campaigns
                  </NavLink>
                </li>
              )}

              {showCenters && (
                <li className="nav-item">
                  <NavLink className={navigationClassName} to="/app/center-operations" onClick={closeNav}>
                    Center Operations
                  </NavLink>
                </li>
              )}

              {showSessions && (
                <li className="nav-item">
                  <NavLink className={navigationClassName} to="/app/sessions" onClick={closeNav}>
                    Sessions
                  </NavLink>
                </li>
              )}

              {showCheckIn && (
                <li className="nav-item">
                  <NavLink className={navigationClassName} to="/app/checkin/queue" onClick={closeNav}>
                    Check-In Queue
                  </NavLink>
                </li>
              )}

              {showAnalytics && (
                <li className="nav-item">
                  <NavLink className={navigationClassName} to="/app/analytics" onClick={closeNav}>
                    Analytics
                  </NavLink>
                </li>
              )}

              {showAdminNavigation && (
                <li className="nav-item">
                  <NavLink className={navigationClassName} to="/app/admin/staff-requests" onClick={closeNav}>
                    Staff Requests
                  </NavLink>
                </li>
              )}

              {showScreeningConfigurationNavigation && (
                <>
                  <li className="nav-item">
                    <NavLink className={navigationClassName} to="/app/screening/questions" onClick={closeNav}>
                      Screening Questions
                    </NavLink>
                  </li>
                  <li className="nav-item">
                    <NavLink className={navigationClassName} to="/app/screening/questionnaires" onClick={closeNav}>
                      Questionnaires
                    </NavLink>
                  </li>
                </>
              )}
            </ul>

            {user && (
              <div className="d-flex align-items-center gap-2 ms-lg-auto pb-3 pb-lg-0 pt-2 pt-lg-0">
                <div className="text-lg-end text-white me-2">
                  <div className="small fw-semibold">{user.fullName || user.email}</div>
                  {user.roles && user.roles.length > 0 && (
                    <div className="d-flex gap-1 justify-content-lg-end">
                      {user.roles.map((role) => (
                        <span key={role} className="badge bg-light text-primary py-0 px-1 fs-7">
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
