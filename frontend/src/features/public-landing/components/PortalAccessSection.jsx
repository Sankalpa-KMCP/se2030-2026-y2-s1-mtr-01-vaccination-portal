import React from 'react';
import { Link } from 'react-router-dom';

export default function PortalAccessSection() {
  return (
    <section className="lp-section lp-section--subtle" id="portal-access" aria-labelledby="portal-access-title">
      <div className="lp-container">
        <div className="lp-section__header">
          <div className="lp-section__eyebrow-row">
            <span className="lp-section__eyebrow">Account Access</span>
            <span className="lp-section__eyebrow-line" aria-hidden="true" />
          </div>
          <h2 className="lp-section__title" id="portal-access-title">Choose your portal</h2>
          <p className="lp-section__lead">Citizen accounts are active immediately. Staff access requires administrator approval.</p>
        </div>

        <div className="lp-access-grid">
          <article className="lp-access-card">
            <span className="lp-access-card__tag">Citizen Portal</span>
            <h3 className="lp-access-card__title">Vaccination services for citizens</h3>
            <p className="lp-access-card__text">Create an account or log in to reach your personal vaccination portal.</p>
            <div className="lp-access-card__actions">
              <Link className="lp-btn lp-btn--primary" to="/register/citizen">Create Citizen Account</Link>
              <Link className="lp-btn lp-btn--secondary" to="/login/citizen">Citizen Login</Link>
            </div>
          </article>

          <article className="lp-access-card">
            <span className="lp-access-card__tag">Staff Portal</span>
            <h3 className="lp-access-card__title">Approved access for portal staff</h3>
            <p className="lp-access-card__text">Request a staff account for administrator review, or log in after approval.</p>
            <div className="lp-access-card__actions">
              <Link className="lp-btn lp-btn--primary" to="/register/staff">Request Staff Account</Link>
              <Link className="lp-btn lp-btn--secondary" to="/login/staff">Staff Login</Link>
            </div>
          </article>
        </div>
      </div>
    </section>
  );
}
