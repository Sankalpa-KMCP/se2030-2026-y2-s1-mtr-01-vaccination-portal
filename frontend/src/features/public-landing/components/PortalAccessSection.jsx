import React from 'react';
import { Link } from 'react-router-dom';

function CitizenIcon() {
  return (
    <span className="lp-access-card__icon" aria-hidden="true">
      <svg width="28" height="28" viewBox="0 0 24 24" fill="none">
        <circle cx="12" cy="8" r="3.4" stroke="currentColor" strokeWidth="1.7" />
        <path
          d="M5.5 19.5c.8-3.2 3.4-5 6.5-5s5.7 1.8 6.5 5"
          stroke="currentColor"
          strokeWidth="1.7"
          strokeLinecap="round"
        />
      </svg>
    </span>
  );
}

function StaffIcon() {
  return (
    <span className="lp-access-card__icon" aria-hidden="true">
      <svg width="28" height="28" viewBox="0 0 24 24" fill="none">
        <path
          d="M9.5 12.5a4.5 4.5 0 0 0-4.5 4.5v2h9v-2a4.5 4.5 0 0 0-4.5-4.5Z"
          stroke="currentColor"
          strokeWidth="1.7"
          strokeLinejoin="round"
        />
        <circle cx="9.5" cy="7.5" r="2.8" stroke="currentColor" strokeWidth="1.7" />
        <path
          d="M16 8.5v6M13 11.5h6"
          stroke="currentColor"
          strokeWidth="1.7"
          strokeLinecap="round"
        />
      </svg>
    </span>
  );
}

function ClockIcon() {
  return (
    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <circle cx="12" cy="12" r="8.5" stroke="currentColor" strokeWidth="1.9" />
      <path d="M12 7.5V12l3 2" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function ReviewIcon() {
  return (
    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <circle cx="10.5" cy="10.5" r="6.5" stroke="currentColor" strokeWidth="1.9" />
      <path d="M15.5 15.5 20 20" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" />
    </svg>
  );
}

export default function PortalAccessSection() {
  return (
    <section
      className="lp-section lp-section--subtle"
      id="portal-access"
      aria-labelledby="portal-access-title"
    >
      <div className="lp-container">
        <div className="lp-kicker" data-reveal>
          <span className="lp-kicker__index">01</span>
          <span className="lp-kicker__label">Account Access</span>
          <span className="lp-kicker__rule" aria-hidden="true" />
        </div>

        <div className="lp-section__header" data-reveal>
          <div className="lp-section__header-text">
            <h2 className="lp-section__title" id="portal-access-title">
              Choose your portal
            </h2>
          </div>
          <p className="lp-section__lead">
            Citizens start straight away. Staff accounts are activated after a
            quick administrator review.
          </p>
        </div>

        <div className="lp-access-grid">
          <article className="lp-access-card lp-access-card--lead" data-reveal>
            <CitizenIcon />
            <span className="lp-access-card__tag">For citizens</span>
            <h3 className="lp-access-card__title">Your vaccinations, in one place</h3>
            <p className="lp-access-card__text">
              Book a session at a clinic near you and view your immunisation
              record whenever you need it.
            </p>
            <span className="lp-access-card__fact">
              <ClockIcon />
              Ready in minutes &mdash; no approval needed
            </span>
            <div className="lp-access-card__actions">
              <Link className="lp-btn lp-btn--primary" to="/register/citizen">Create citizen account</Link>
              <Link className="lp-btn lp-btn--secondary" to="/login/citizen">Citizen login</Link>
            </div>
          </article>

          <article className="lp-access-card" data-reveal style={{ '--reveal-delay': '120ms' }}>
            <StaffIcon />
            <span className="lp-access-card__tag">For health staff</span>
            <h3 className="lp-access-card__title">Run clinics and campaigns</h3>
            <p className="lp-access-card__text">
              Plan campaigns, manage clinic sessions, and record vaccinations
              for your health team.
            </p>
            <span className="lp-access-card__fact">
              <ReviewIcon />
              Activated after administrator review
            </span>
            <div className="lp-access-card__actions">
              <Link className="lp-btn lp-btn--secondary" to="/register/staff">Request staff account</Link>
              <Link className="lp-btn lp-btn--quiet" to="/login/staff">
                Staff login <span aria-hidden="true">&rarr;</span>
              </Link>
            </div>
          </article>
        </div>
      </div>
    </section>
  );
}
