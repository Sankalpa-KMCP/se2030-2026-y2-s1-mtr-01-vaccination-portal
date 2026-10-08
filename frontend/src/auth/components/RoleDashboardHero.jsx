import React from 'react';
import { Link } from 'react-router-dom';
import approvedHeroIllustration from '../assets/admin-dashboard-hero-approved.png';

export default function RoleDashboardHero({
  user,
  titleId,
  roleLabel,
  description,
  profileDetails = [],
  profileLink = null,
}) {
  const displayName = user?.fullName || user?.email || roleLabel;

  return (
    <section className="admin-hero role-dashboard-hero" aria-labelledby={titleId}>
      <div className="admin-hero__copy">
        <span className="admin-role-pill badge">{roleLabel}</span>
        <h1 id={titleId}>Welcome, <span className="d-block">{displayName}</span></h1>
        <p>{description}</p>
      </div>

      <img
        className="admin-hero__illustration"
        src={approvedHeroIllustration}
        alt=""
        aria-hidden="true"
      />

      <section className="admin-profile card" aria-labelledby={`${titleId}-profile`}>
        <div className="admin-profile__heading">
          <span className="admin-icon-tile" aria-hidden="true"><i className="bi bi-person-fill" /></span>
          <h2 id={`${titleId}-profile`}>Account Profile</h2>
        </div>
        <dl>
          <div><dt><i className="bi bi-person" aria-hidden="true" />Full Name</dt><dd>{user?.fullName || 'Not provided'}</dd></div>
          <div><dt><i className="bi bi-envelope" aria-hidden="true" />Email</dt><dd>{user?.email || 'Not provided'}</dd></div>
          <div><dt><i className="bi bi-telephone" aria-hidden="true" />Phone Number</dt><dd>{user?.phoneNumber || 'Not provided'}</dd></div>
          <div><dt><i className="bi bi-shield-check" aria-hidden="true" />Portal Role</dt><dd><span className="admin-profile__role">{roleLabel}</span></dd></div>
          {profileDetails.map((detail) => (
            <div key={detail.label}>
              <dt><i className={`bi ${detail.icon || 'bi-info-circle'}`} aria-hidden="true" />{detail.label}</dt>
              <dd>{detail.value}</dd>
            </div>
          ))}
        </dl>
        {profileLink && (
          <Link className="admin-profile__link" to={profileLink.to}>
            {profileLink.label}<i className="bi bi-arrow-right" aria-hidden="true" />
          </Link>
        )}
      </section>
    </section>
  );
}
