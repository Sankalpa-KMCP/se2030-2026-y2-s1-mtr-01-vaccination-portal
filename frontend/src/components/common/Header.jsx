import React, { useEffect, useRef, useState } from 'react';
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
  const [profileMenuOpen, setProfileMenuOpen] = useState(false);
  const profileMenuRef = useRef(null);

  useEffect(() => {
    function handleDocumentClick(event) {
      if (profileMenuRef.current && !profileMenuRef.current.contains(event.target)) {
        setProfileMenuOpen(false);
      }
    }
    function handleKeyDown(event) {
      if (event.key === 'Escape') {
        setProfileMenuOpen(false);
      }
    }
    if (profileMenuOpen) {
      document.addEventListener('mousedown', handleDocumentClick);
      document.addEventListener('keydown', handleKeyDown);
    }
    return () => {
      document.removeEventListener('mousedown', handleDocumentClick);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [profileMenuOpen]);

  // Optional override so the app shell can distinguish voluntary logout
  // (e.g. to return to the public landing page) from session expiry.
  const handleLogout = onLogout || logout;

  const showCampaigns = canViewCampaigns(user);
  const showCenters = canViewCenters(user);
  const showSessions = canViewSessions(user);
  const showAnalytics = canViewAnalytics(user);
  const showCheckIn = hasRole(
    user,
    ADMIN_ROLE,
    'ROLE_CENTER_MANAGER',
    'ROLE_VACCINATOR',
  );
  const showInventory = hasRole(user, ADMIN_ROLE, 'ROLE_INVENTORY_OFFICER');
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
              <img src="/brand/vaccination-portal-icon.svg" alt="" width="40" height="40" />
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
                  <li className="nav-item">
                    <NavLink className={navigationClassName} to="/app/citizen/vaccinations" onClick={closeNav}>
                      My Vaccinations
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

              {hasRole(user, 'ROLE_VACCINATOR') && (
                <>
                  <li className="nav-item">
                    <NavLink className={navigationClassName} to="/app/staff/vaccination-drafts" onClick={closeNav}>
                      Vaccination Drafts
                    </NavLink>
                  </li>
                  <li className="nav-item">
                    <NavLink className={navigationClassName} to="/app/staff/vaccination-records" onClick={closeNav}>
                      Vaccination Records
                    </NavLink>
                  </li>
                  <li className="nav-item">
                    <NavLink className={navigationClassName} to="/app/staff/correction-requests" onClick={closeNav}>
                      Correction Requests
                    </NavLink>
                  </li>
                </>
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

              {showInventory && (
                <li className="nav-item">
                  <NavLink className={navigationClassName} to="/app/inventory" onClick={closeNav}>
                    Inventory
                  </NavLink>
                </li>
              )}

              {showAdminNavigation && (
                <>
                  <li className="nav-item">
                    <NavLink className={navigationClassName} to="/app/admin/staff-requests" onClick={closeNav}>
                      Staff Requests
                    </NavLink>
                  </li>
                  <li className="nav-item">
                    <NavLink className={navigationClassName} to="/app/admin/correction-requests" onClick={closeNav}>
                      Correction Requests
                    </NavLink>
                  </li>
                </>
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
                {hasRole(user, 'ROLE_VACCINATOR') ? (
                  <div className="dropdown position-relative" ref={profileMenuRef}>
                    <button
                      type="button"
                      className="btn p-0 border-0 text-white d-flex align-items-center gap-2 text-decoration-none shadow-none"
                      id="vaccinatorProfileDropdown"
                      aria-expanded={profileMenuOpen}
                      aria-haspopup="true"
                      aria-label="User profile and account menu"
                      onClick={() => setProfileMenuOpen((open) => !open)}
                      style={{ background: 'transparent' }}
                    >
                      <div className="app-header__user-avatar" aria-hidden="true">
                        {getUserInitials(user.fullName, user.email)}
                      </div>
                      <div className="text-start me-1">
                        <div className="app-header__user-name small fw-semibold" title={user.fullName || user.email}>
                          {user.fullName || user.email}
                        </div>
                        <div className="text-white-50 small" style={{ fontSize: '0.72rem', lineHeight: 1.1 }}>
                          Vaccinator
                        </div>
                      </div>
                      <span className="small text-white-50" aria-hidden="true" style={{ fontSize: '0.7rem' }}>
                        ▼
                      </span>
                    </button>

                    {profileMenuOpen && (
                      <div
                        className="dropdown-menu dropdown-menu-end shadow-lg show p-3 border-0 mt-2"
                        aria-labelledby="vaccinatorProfileDropdown"
                        style={{
                          minWidth: '240px',
                          maxWidth: '300px',
                          zIndex: 1050,
                          borderRadius: '0.75rem',
                        }}
                      >
                        <div className="d-flex align-items-center gap-2 mb-2 pb-2 border-bottom">
                          <div
                            className="app-header__user-avatar bg-primary text-white"
                            aria-hidden="true"
                            style={{ width: '38px', height: '38px', fontSize: '0.85rem' }}
                          >
                            {getUserInitials(user.fullName, user.email)}
                          </div>
                          <div className="overflow-hidden">
                            <div className="fw-bold text-dark text-truncate small" title={user.fullName || user.email}>
                              {user.fullName || user.email}
                            </div>
                            <span className="badge bg-primary-subtle text-primary border border-primary-subtle" style={{ fontSize: '0.68rem' }}>
                              Vaccinator
                            </span>
                          </div>
                        </div>

                        <div className="small mb-3">
                          {user.email && (
                            <div className="text-muted text-truncate mb-1" title={user.email}>
                              <span className="text-secondary fw-semibold">Email: </span>
                              {user.email}
                            </div>
                          )}
                          {user.phoneNumber && (
                            <div className="text-muted text-truncate" title={user.phoneNumber}>
                              <span className="text-secondary fw-semibold">Phone: </span>
                              {user.phoneNumber}
                            </div>
                          )}
                        </div>

                        <div className="pt-2 border-top">
                          <button
                            type="button"
                            className="btn btn-outline-danger btn-sm w-100 fw-semibold"
                            onClick={() => {
                              setProfileMenuOpen(false);
                              handleLogout();
                            }}
                          >
                            Sign Out
                          </button>
                        </div>
                      </div>
                    )}
                  </div>
                ) : (
                  <>
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
                  </>
                )}
              </div>
            )}
          </div>
        </div>
      </nav>
    </header>
  );
}
