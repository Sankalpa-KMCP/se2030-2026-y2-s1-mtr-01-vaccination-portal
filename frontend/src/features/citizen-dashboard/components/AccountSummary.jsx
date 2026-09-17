import React from 'react';
import { Link } from 'react-router-dom';
import DashboardIcon from './DashboardIcon';

function formatStatus(status) {
  if (!status) return 'Not provided';
  return status.toLowerCase().split('_').map((part) => part.charAt(0).toUpperCase() + part.slice(1)).join(' ');
}

export default function AccountSummary({ user }) {
  return (
    <section className="citizen-account" aria-labelledby="citizen-account-title">
      <header>
        <span className="citizen-panel__icon"><DashboardIcon name="user" /></span>
        <div>
          <p className="citizen-dashboard__eyebrow">Account</p>
          <h2 id="citizen-account-title">Your contact details</h2>
        </div>
      </header>
      <dl>
        <div><dt>Citizen ID</dt><dd>{user?.id != null ? `#${user.id}` : 'Not provided'}</dd></div>
        <div><dt>Full name</dt><dd>{user?.fullName || 'Not provided'}</dd></div>
        <div><dt>Email address</dt><dd>{user?.email || 'Not provided'}</dd></div>
        <div><dt>Phone number</dt><dd>{user?.phoneNumber || 'Not provided'}</dd></div>
        <div><dt>Account status</dt><dd>{formatStatus(user?.accountStatus)}</dd></div>
      </dl>
      <nav className="citizen-panel__links" aria-label="Citizen account links">
        <Link to="/app/citizen/profile">View Profile <DashboardIcon name="arrow" size={16} /></Link>
      </nav>
    </section>
  );
}
