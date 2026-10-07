import React from 'react';

function getInitials(fullName, email) {
  if (fullName && typeof fullName === 'string') {
    const parts = fullName.trim().split(/\s+/);
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
  return 'HO';
}

export default function HcoDashboardHeader({ user }) {
  const displayName = user?.fullName || user?.email || 'Healthcare Officer';
  const initials = getInitials(user?.fullName, user?.email);
  const contactParts = [user?.email, user?.phoneNumber].filter(Boolean);
  const isActive = user?.accountStatus === 'ACTIVE';

  return (
    <div className="row g-3 align-items-center mb-3">
      {/* Welcome & Eyebrow */}
      <div className="col-12 col-lg-8">
        <span className="hco-header-badge">Healthcare Officer Dashboard</span>
        <h1 className="hco-welcome-title" id="hco-dashboard-title">
          Welcome, {displayName}
        </h1>
        <p className="hco-welcome-subtitle">
          Support safe and effective vaccination by reviewing screenings, managing clinical questions, and maintaining questionnaires.
        </p>
      </div>

      {/* Compact Account Card */}
      <div className="col-12 col-lg-4">
        <div className="hco-account-card">
          <div className="hco-account-card-title">Your Account</div>
          <div className="d-flex align-items-center gap-3">
            <div className="hco-account-avatar" aria-hidden="true">
              {initials}
            </div>
            <div className="flex-grow-1 min-w-0">
              <div className="hco-account-name text-truncate">
                {displayName}
              </div>
              {contactParts.length > 0 && (
                <div className="hco-account-contact text-truncate">
                  {contactParts.join(' | ')}
                </div>
              )}
              <div className="d-flex align-items-center gap-2 mt-1">
                <span className="hco-account-role-badge">Healthcare Officer</span>
                {isActive && (
                  <span className="hco-account-status-active">
                    <span className="hco-account-status-dot" aria-hidden="true" />
                    Active
                  </span>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
