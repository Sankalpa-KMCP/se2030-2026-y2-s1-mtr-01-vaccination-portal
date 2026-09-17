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

function getUserInitials(name, email) {
  if (name && typeof name === 'string') {
    const parts = name.trim().split(/\s+/);
    if (parts.length >= 2) {
      return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    }
    if (parts.length === 1 && parts[0].length > 0) {
      return parts[0].slice(0, 2).toUpperCase();
    }
  }
  if (email && typeof email === 'string') {
    return email.slice(0, 2).toUpperCase();
  }
  return 'U';
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
  const showCheckIn = hasRole(user, ADMIN_ROLE, 'ROLE_CENTER_MANAGER');
  const showAdminNavigation = hasRole(user, ADMIN_ROLE);
  const showScreeningConfigurationNavigation = hasRole(
    user,
    'ROLE_HEALTHCARE_OFFICER',
  );
  const showCitizenNavigation = hasRole(user, CITIZEN_ROLE);
  const showScreeningReview = hasRole(user, 'ROLE_HEALTHCARE_OFFICER');
  const dashboardPath = getRoleHomePath(user);

  const closeNav = () => setNavOpen(false);

  return (
    <header className="app-header">
      <nav className="navbar navbar-expand-lg navbar-dark app-navbar shadow-sm" aria-label="Main Navigation">
        <div className="container">
          <span className="navbar-brand app-header__brand fw-bold me-lg-4">
            <span className="app-header__brand-mark" aria-hidden="true">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                <path d="m9 12 2 2 4-4" />
              </svg>
            </span>
            <span className="app-header__brand-title">Web-Based Vaccination Portal</span>
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
              <div className="app-header__account d-flex align-items-center gap-3 ms-lg-auto pb-3 pb-lg-0 pt-2 pt-lg-0">
                <div className="d-flex align-items-center gap-2 text-white">
                  <div className="app-header__user-avatar" aria-hidden="true">
                    {getUserInitials(user.fullName, user.email)}
                  </div>
                  <div className="text-start text-lg-end me-lg-1">
                    <div className="app-header__user-name small fw-semibold" title={user.fullName || user.email}>
                      {user.fullName || user.email}
                    </div>
                    {user.roles && user.roles.length > 0 && (
                      <div className="app-header__user-roles d-flex gap-1 justify-content-start justify-content-lg-end mt-1">
                        {user.roles.map((role) => (
                          <span key={role} className="app-header__role-badge">
                            {getRoleLabel(role)}
                          </span>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
                <button
                  type="button"
                  className="app-header__logout-btn btn btn-sm"
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
