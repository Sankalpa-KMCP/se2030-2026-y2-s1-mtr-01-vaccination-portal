import React from 'react';
import { Link } from 'react-router-dom';

export default function FinalCtaSection({ onLoginClick }) {
  return (
    <section className="home-closing" aria-labelledby="lp-cta-title">
      <div className="lp-container home-closing__layout" data-reveal>
        <div className="home-closing__index" aria-hidden="true">05</div>
        <div className="home-closing__copy">
          <span className="home-section-mark">Begin</span>
          <h2 id="lp-cta-title">Start with your Citizen account.</h2>
          <p>Register once, then complete screening separately for each campaign you join.</p>
        </div>
        <div className="home-closing__actions">
          <Link to="/register/citizen" className="home-action home-action--secondary">
            Open citizen registration <span aria-hidden="true">&rarr;</span>
          </Link>
          <button
            type="button"
            className="home-text-button"
            onClick={onLoginClick}
          >
            Sign in to the portal
          </button>
          <Link className="home-closing__staff" to="/register/staff">Health staff access</Link>
        </div>
      </div>
    </section>
  );
}
