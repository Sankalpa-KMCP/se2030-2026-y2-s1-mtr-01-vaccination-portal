import React from 'react';

export default function PublicFooter({ onLoginClick }) {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="lp-footer" aria-label="Site Footer">
      <div className="lp-container">
        <div className="lp-footer__top">
          <div className="lp-footer__brand-col">
            <span className="lp-footer__brand">Web-Based Vaccination Portal</span>
            <p className="lp-footer__desc">
              Supporting vaccination-program operations for healthcare teams.
            </p>
          </div>

          <nav className="lp-footer__nav" aria-label="Footer navigation">
            <a className="lp-footer__link" href="#capabilities">
              Capabilities
            </a>
            <a className="lp-footer__link" href="#how-it-works">
              How it works
            </a>
            <button
              type="button"
              className="lp-footer__btn"
              onClick={onLoginClick}
            >
              Login
            </button>
          </nav>
        </div>

        <div className="lp-footer__bottom">
          <span className="lp-footer__copyright">
            &copy; {currentYear} Web-Based Vaccination Portal &mdash; SE2030 project.
          </span>
          <span className="lp-footer__status">
            Operational System
          </span>
        </div>
      </div>
    </footer>
  );
}
