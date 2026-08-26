import React from 'react';

export default function HeroSection({ onLoginClick }) {
  return (
    <section className="lp-hero" aria-labelledby="lp-hero-title">
      <div className="lp-container">
        <p className="lp-hero__eyebrow">Vaccination Program Portal</p>
        <h1 className="lp-hero__title" id="lp-hero-title">
          Coordinated vaccination operations, in one secure portal.
        </h1>
        <p className="lp-hero__lead">
          The Web-Based Vaccination Portal gives programme teams one place to
          manage campaigns, centers and vaccination sessions — with capacity
          planning and readiness built in.
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
      </div>
    </section>
  );
}
