import React from 'react';
import DashboardIcon from './DashboardIcon';

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
        <div><dt>Full name</dt><dd>{user?.fullName || 'Not provided'}</dd></div>
        <div><dt>Email address</dt><dd>{user?.email || 'Not provided'}</dd></div>
        <div><dt>Phone number</dt><dd>{user?.phoneNumber || 'Not provided'}</dd></div>
      </dl>
      <p>These details come from your authenticated citizen account. Profile editing is not available here.</p>
    </section>
  );
}
