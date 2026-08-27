import React from 'react';

export default function HeroSection({ onLoginClick }) {
  return (
    <section className="lp-hero" aria-labelledby="lp-hero-title">
      <div className="lp-container lp-hero__grid">
        <div className="lp-hero__content">
          <div className="lp-hero__eyebrow-row">
            <span className="lp-hero__eyebrow">Vaccination Program Portal</span>
            <span className="lp-hero__eyebrow-line" aria-hidden="true" />
          </div>

          <h1 className="lp-hero__title" id="lp-hero-title">
            Coordinated vaccination operations, in one secure portal.
          </h1>

          <p className="lp-hero__lead">
            Programme teams can manage campaigns, centers, and vaccination sessions
            — with capacity planning and readiness built into operations.
          </p>

          <div className="lp-hero__actions">
            <button
              type="button"
              className="lp-btn lp-btn--primary"
              onClick={onLoginClick}
            >
              Sign in
            </button>
            <a className="lp-btn lp-btn--secondary" href="#capabilities">
              Explore capabilities
            </a>
          </div>

          <div className="lp-hero__meta">
            <span className="lp-hero__meta-item">Role-based access control</span>
            <span className="lp-hero__meta-dot" aria-hidden="true">·</span>
            <span className="lp-hero__meta-item">Pre-session readiness diagnostics</span>
            <span className="lp-hero__meta-dot" aria-hidden="true">·</span>
            <span className="lp-hero__meta-item">Change control logging</span>
          </div>
        </div>

        <aside className="lp-hero__sidebar" aria-label="System operational scope">
          <div className="lp-hero__sidebar-header">
            <span className="lp-hero__sidebar-tag">SYSTEM SCOPE</span>
            <span className="lp-hero__sidebar-badge">MF-02 Core</span>
          </div>

          <div className="lp-hero__sidebar-list">
            <div className="lp-hero__sidebar-row">
              <span className="lp-hero__sidebar-num">01</span>
              <div className="lp-hero__sidebar-body">
                <span className="lp-hero__sidebar-title">Campaign Policy</span>
                <span className="lp-hero__sidebar-desc">Dose schedules, target locations &amp; lifecycle</span>
              </div>
            </div>

            <div className="lp-hero__sidebar-row">
              <span className="lp-hero__sidebar-num">02</span>
              <div className="lp-hero__sidebar-body">
                <span className="lp-hero__sidebar-title">Center Facilities</span>
                <span className="lp-hero__sidebar-desc">Stations, equipment &amp; staff rosters</span>
              </div>
            </div>

            <div className="lp-hero__sidebar-row">
              <span className="lp-hero__sidebar-num">03</span>
              <div className="lp-hero__sidebar-body">
                <span className="lp-hero__sidebar-title">Session Operations</span>
                <span className="lp-hero__sidebar-desc">Single/recurring series &amp; change records</span>
              </div>
            </div>

            <div className="lp-hero__sidebar-row">
              <span className="lp-hero__sidebar-num">04</span>
              <div className="lp-hero__sidebar-body">
                <span className="lp-hero__sidebar-title">Readiness Gate</span>
                <span className="lp-hero__sidebar-desc">Pre-opening operational readiness checks</span>
              </div>
            </div>
          </div>

          <div className="lp-hero__sidebar-footer">
            <span>Authorized operational portal accounts only</span>
          </div>
        </aside>
      </div>
    </section>
  );
}
