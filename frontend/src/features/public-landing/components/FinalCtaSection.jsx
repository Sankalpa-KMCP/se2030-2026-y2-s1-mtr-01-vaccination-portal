import React from 'react';

export default function FinalCtaSection({ onLoginClick }) {
  return (
    <section className="lp-section lp-section--subtle lp-cta-section" aria-labelledby="lp-cta-title">
      <div className="lp-container">
        <div className="lp-cta-panel">
          <div className="lp-cta-panel__content">
            <div className="lp-section__eyebrow-row">
              <span className="lp-section__eyebrow">Portal Access</span>
              <span className="lp-section__eyebrow-line" aria-hidden="true" />
            </div>
            <h2 className="lp-cta-panel__title" id="lp-cta-title">
              Access the portal
            </h2>
            <p className="lp-cta-panel__text">
              Sign in with your authorized account to manage campaigns, centers, and vaccination sessions.
            </p>
            <span className="lp-cta-panel__note">
              Access is based on your assigned portal role.
            </span>
          </div>

          <div className="lp-cta-panel__actions">
            <button
              type="button"
              className="lp-btn lp-btn--primary lp-cta-panel__btn"
              onClick={onLoginClick}
            >
              Sign in to portal
            </button>
            <a className="lp-cta-panel__link" href="#capabilities">
              Review capabilities
            </a>
          </div>
        </div>
      </div>
    </section>
  );
}
