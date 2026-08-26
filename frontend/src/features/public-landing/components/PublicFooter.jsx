import React from 'react';
import { BrandMark } from './icons';

export default function PublicFooter({ onLoginClick }) {
  const year = new Date().getFullYear();

  return (
    <footer className="lp-footer">
      <div className="lp-container lp-footer__inner">
        <div className="lp-footer__brand">
          <span className="lp-brand">
            <BrandMark className="lp-brand__glyph" width="22" height="22" />
            <span className="lp-footer__name">Web-Based Vaccination Portal</span>
          </span>
          <p className="lp-footer__descriptor">
            Supporting vaccination-program operations for healthcare teams.
          </p>
        </div>

        <nav className="lp-footer__links" aria-label="Footer">
          <a className="lp-footer__link" href="#capabilities">
            Capabilities
          </a>
          <a className="lp-footer__link" href="#how-it-works">
            How it works
          </a>
          <button type="button" className="lp-footer__link lp-footer__link--action" onClick={onLoginClick}>
            Sign in
          </button>
        </nav>
      </div>

      <div className="lp-container">
        <p className="lp-footer__project">&copy; {year} Web-Based Vaccination Portal &mdash; SE2030 project.</p>
      </div>
    </footer>
  );
}
