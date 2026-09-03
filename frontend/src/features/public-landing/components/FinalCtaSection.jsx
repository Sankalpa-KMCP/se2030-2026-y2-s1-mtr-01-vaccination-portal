import React from 'react';

export default function FinalCtaSection({ onLoginClick }) {
  return (
    <section className="lp-cta-section" aria-labelledby="lp-cta-title">
      <div className="lp-container lp-cta-inner">
        <div className="lp-cta-panel__content">
          <div className="lp-cta-kicker">
            <span className="lp-cta-kicker__label">Portal access</span>
            <span className="lp-cta-kicker__rule" aria-hidden="true" />
          </div>
          <h2 className="lp-cta-panel__title" id="lp-cta-title">
            Ready to get started?
          </h2>
          <p className="lp-cta-panel__text">
            Sign in to book a vaccination session, or to manage campaigns,
            clinics, and sessions for your health team.
          </p>
        </div>

        <div className="lp-cta-panel__actions">
          <button
            type="button"
            className="lp-btn lp-btn--inverted"
            onClick={onLoginClick}
          >
            Sign in to the portal
          </button>
          <a className="lp-cta-panel__link" href="#capabilities">
            See what the portal does
          </a>
        </div>
      </div>
    </section>
  );
}
