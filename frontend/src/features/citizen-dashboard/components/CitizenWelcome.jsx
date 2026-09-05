import React from 'react';

export default function CitizenWelcome({ user }) {
  const displayName = user?.fullName || user?.email || 'Citizen';

  return (
    <header className="citizen-dashboard__welcome">
      <div className="citizen-dashboard__welcome-copy">
        <p className="citizen-dashboard__eyebrow">Citizen Dashboard</p>
        <h1 id="citizen-dashboard-title">Welcome, {displayName}</h1>
        <p className="citizen-dashboard__welcome-description">
          Review your screening and appointment records, then move directly to the service you need.
        </p>
      </div>
      <div className="citizen-dashboard__welcome-meta">
        <span>Secure self-service</span>
        <strong>Citizen account</strong>
        {user?.email && <small>{user.email}</small>}
      </div>
    </header>
  );
}
