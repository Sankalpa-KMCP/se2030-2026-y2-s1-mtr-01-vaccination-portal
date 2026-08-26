import React from 'react';
import { LockShield } from './icons';

export default function PublicFooter({ onLoginClick }) {
  const year = new Date().getFullYear();
  return (
    <footer className="lp-footer">
      <div className="lp-container lp-footer__inner">
        <div className="lp-footer__left">
          <span className="lp-footer__brand">Web-Based Vaccination Portal</span>
          <span className="lp-footer__rule" aria-hidden="true" />
          <span>© {year} SE2030 Project Baseline</span>
        </div>
        <nav className="lp-footer__nav" aria-label="Footer">
          <a className="lp-footer__link" href="#capabilities">Capabilities</a>
          <a className="lp-footer__link" href="#workflow">Workflow</a>
          <a className="lp-footer__link" href="#governance">Governance</a>
          <button type="button" className="lp-footer__link lp-footer__link--btn" onClick={onLoginClick}>
            Sign in
          </button>
          <span className="lp-footer__lock" aria-hidden="true">
            <LockShield width={12} height={12} />
            Authorized access only
          </span>
        </nav>
      </div>
    </footer>
  );
}
