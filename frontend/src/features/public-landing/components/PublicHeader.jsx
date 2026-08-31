import React from 'react';

export default function PublicHeader({ onLoginClick }) {
  return (
    <header className="lp-header">
      <div className="lp-container lp-header__inner">
        <div className="lp-header__brand-group">
          <span className="lp-header__brand">Web-Based Vaccination Portal</span>
          <span className="lp-header__badge">Operational System</span>
        </div>

        <nav className="lp-header__nav" aria-label="Main navigation">
          <a className="lp-header__link" href="#capabilities">
            Capabilities
          </a>
          <a className="lp-header__link" href="#how-it-works">
            How it works
          </a>
          <button
            type="button"
            className="lp-btn lp-btn--header"
            onClick={onLoginClick}
          >
            Login
          </button>
        </nav>
      </div>
    </header>
  );
}
