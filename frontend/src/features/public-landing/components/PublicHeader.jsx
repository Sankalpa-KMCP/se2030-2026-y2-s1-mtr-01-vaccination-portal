import React from 'react';
import { ArrowRight, BrandCross } from './icons';

export default function PublicHeader({ onLoginClick }) {
  return (
    <header className="lp-header">
      <div className="lp-container lp-header__inner">
        <a className="lp-header__brand" href="#" onClick={(e) => e.preventDefault()} aria-label="Web-Based Vaccination Portal — home">
          <span className="lp-header__mark" aria-hidden="true">
            <BrandCross width={14} height={14} />
          </span>
          <span className="lp-header__title">Web-Based Vaccination Portal</span>
          <span className="lp-header__meta" aria-hidden="true">
            <span className="lp-header__meta-dot" />
            Operational system
          </span>
        </a>

        <nav className="lp-header__nav" aria-label="Primary">
          <a className="lp-header__link" href="#capabilities">Capabilities</a>
          <a className="lp-header__link" href="#workflow">Workflow</a>
          <a className="lp-header__link" href="#governance">Governance</a>
        </nav>

        <div className="lp-header__actions">
          <button type="button" className="lp-btn lp-btn--primary lp-btn--small" onClick={onLoginClick}>
            <span>Sign in</span>
            <ArrowRight className="lp-btn__icon" width={14} height={14} aria-hidden="true" />
          </button>
        </div>
      </div>
    </header>
  );
}
