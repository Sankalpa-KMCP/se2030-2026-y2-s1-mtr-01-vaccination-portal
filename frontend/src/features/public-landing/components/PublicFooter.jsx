import React from 'react';

export default function PublicFooter({ onLoginClick }) {
  const year = new Date().getFullYear();

  return (
    <footer className="lp-footer">
      <div className="lp-container lp-footer__inner">
        <span className="lp-footer__brand">Web-Based Vaccination Portal</span>

        <nav className="lp-footer__nav" aria-label="Footer navigation">
          <a className="lp-footer__link" href="#capabilities">
            Capabilities
          </a>
          <a className="lp-footer__link" href="#how-it-works">
            How it works
          </a>
          <button
            type="button"
            className="lp-footer__link"
            onClick={onLoginClick}
          >
            Sign in
          </button>
        </nav>

        <span>&copy; {year} Web-Based Vaccination Portal &mdash; SE2030 project.</span>
      </div>
    </footer>
  );
}
