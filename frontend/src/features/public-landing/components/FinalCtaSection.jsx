import React from 'react';
import { ArrowRight } from './icons';

export default function FinalCtaSection({ onLoginClick }) {
  return (
    <section className="lp-close" aria-labelledby="lp-cta-heading">
      <div className="lp-container">
        <div className="lp-close__inner">
          <div>
            <p className="lp-close__eyebrow">Operational system access</p>
            <h2 className="lp-close__title" id="lp-cta-heading">Access the Vaccination Operations Portal.</h2>
            <p className="lp-close__desc">
              Authorized officers, campaign planners and center managers sign in to coordinate programmes, verify readiness and
              review audit records.
            </p>
          </div>
          <div className="lp-close__actions">
            <button type="button" className="lp-btn lp-btn--primary" onClick={onLoginClick}>
              <span>Sign in to portal</span>
              <ArrowRight className="lp-btn__icon" width={16} height={16} aria-hidden="true" />
            </button>
            <span className="lp-close__note">Authorized access only · Session required</span>
            <a className="lp-hero__link" href="#capabilities" style={{ justifyContent: 'center', marginTop: 2 }}>
              Review system capabilities
            </a>
          </div>
        </div>
      </div>
    </section>
  );
}
