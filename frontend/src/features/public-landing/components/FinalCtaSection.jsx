import React from 'react';

export default function FinalCtaSection({ onLoginClick }) {
  return (
    <section className="lp-cta-section" aria-labelledby="lp-cta-title">
      <div className="lp-container">
        <h2 className="lp-cta-section__title" id="lp-cta-title">
          Access the portal
        </h2>
        <p className="lp-cta-section__text">
          Sign in with your authorized account to manage campaigns, centers and sessions.
        </p>
        <button
          type="button"
          className="lp-btn lp-btn--primary"
          onClick={onLoginClick}
        >
          Sign in
        </button>
      </div>
    </section>
  );
}
