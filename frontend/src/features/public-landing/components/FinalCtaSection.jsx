import React from 'react';
import { Link } from 'react-router-dom';

export default function FinalCtaSection() {
  return (
    <section className="home-closing" id="get-started" aria-labelledby="lp-cta-title">
      <div className="lp-container home-closing__layout" data-reveal="cta">
        <div className="home-closing__copy">
          <span className="home-section-mark">Get started</span>
          <h2 id="lp-cta-title">Start with your Citizen account.</h2>
          <p>Register once, then complete screening separately for each campaign you join.</p>
        </div>
        <div className="home-closing__actions">
          <Link to="/register/citizen" className="home-action home-action--secondary">
            Open citizen registration <span aria-hidden="true">&rarr;</span>
          </Link>
          <Link className="home-closing__staff" to="/login">Already registered? Access the portal <span aria-hidden="true">&rarr;</span></Link>
        </div>
      </div>
    </section>
  );
}
