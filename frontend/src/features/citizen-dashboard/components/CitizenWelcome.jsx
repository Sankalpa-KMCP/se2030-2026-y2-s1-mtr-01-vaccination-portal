import React from 'react';
import { Link } from 'react-router-dom';
import DashboardIcon from './DashboardIcon';

function getGreeting() {
  const hour = new Date().getHours();
  if (hour < 12) return 'Good morning';
  if (hour < 18) return 'Good afternoon';
  return 'Good evening';
}

function formatStatus(status) {
  if (!status) return 'Active';
  return status.toLowerCase().replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());
}

export default function CitizenWelcome({ user }) {
  const displayName = user?.fullName || user?.email || 'Citizen User';
  const greeting = getGreeting();

  return (
    <header className="citizen-welcome-hero" aria-label="Citizen welcome banner">
      <div className="citizen-welcome-hero__copy">
        <p className="citizen-dashboard__eyebrow">Citizen Dashboard</p>
        <p className="citizen-welcome-hero__time">{greeting},</p>
        <h1 className="citizen-welcome-hero__title" id="citizen-dashboard-title">
          Welcome, {displayName} <span className="citizen-welcome-hero__wave" aria-hidden="true">👋</span>
        </h1>
        <p className="citizen-welcome-hero__subtitle">Here’s your vaccination journey.</p>
        <p className="citizen-welcome-hero__tagline">Stay protected. A healthier community starts with you.</p>

        {/* Compact Citizen Context Pills */}
        <div className="citizen-welcome-hero__meta">
          <div className="citizen-welcome-hero__pill">
            <span className="citizen-welcome-hero__pill-icon" aria-hidden="true">
              <DashboardIcon name="user" size={14} />
            </span>
            <span className="citizen-welcome-hero__pill-label">Citizen ID</span>
            <strong className="citizen-welcome-hero__pill-value">
              {user?.id != null ? `#${user.id}` : 'Not provided'}
            </strong>
          </div>

          {user?.email && (
            <div className="citizen-welcome-hero__pill">
              <span className="citizen-welcome-hero__pill-icon" aria-hidden="true">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                  <rect width="20" height="16" x="2" y="4" rx="2" />
                  <path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7" />
                </svg>
              </span>
              <span className="citizen-welcome-hero__pill-value">{user.email}</span>
            </div>
          )}

          {user?.phoneNumber && (
            <div className="citizen-welcome-hero__pill">
              <span className="citizen-welcome-hero__pill-icon" aria-hidden="true">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z" />
                </svg>
              </span>
              <span className="citizen-welcome-hero__pill-value">{user.phoneNumber}</span>
            </div>
          )}

          <span className="citizen-welcome-hero__status-badge">
            {formatStatus(user?.accountStatus)}
          </span>

          <Link to="/app/citizen/profile" className="citizen-welcome-hero__profile-link">
            View Profile <DashboardIcon name="arrow" size={13} />
          </Link>
        </div>
      </div>

      <div className="citizen-welcome-hero__media-card" aria-hidden="true">
        <div className="citizen-welcome-hero__media-wrapper">
          <img
            src="/images/citizen-vaccination-care.jpg"
            alt=""
            className="citizen-welcome-hero__image"
            loading="lazy"
          />
          <div className="citizen-welcome-hero__image-gradient" />
        </div>
        <div className="citizen-welcome-hero__caption">
          <span className="citizen-welcome-hero__caption-badge">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
            </svg>
            Vaccines · Healthier Tomorrow
          </span>
          <strong className="citizen-welcome-hero__caption-title">Stronger People</strong>
          <span className="citizen-welcome-hero__caption-sub">Healthier Sri Lanka Together</span>
        </div>
      </div>
    </header>
  );
}
