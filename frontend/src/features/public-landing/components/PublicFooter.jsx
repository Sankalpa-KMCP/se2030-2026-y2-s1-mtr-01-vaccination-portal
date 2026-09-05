import React from 'react';
import { Link } from 'react-router-dom';

export default function PublicFooter() {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="lp-footer" aria-label="Site Footer">
      <div className="lp-container">
        <div className="lp-footer__top">
          <div className="lp-footer__brand-col">
            <span className="lp-footer__brand">Vaccination Portal</span>
            <p className="lp-footer__desc">
              A vaccination portal supporting citizens and health
              teams with vaccination programmes.
            </p>
          </div>

          <nav className="lp-footer__nav" aria-label="Footer navigation">
            <Link to="/" className="lp-footer__link">Home</Link>
            <Link to="/how-it-works" className="lp-footer__link">How it works</Link>
            <Link to="/about" className="lp-footer__link">About</Link>
          </nav>
        </div>

        <div className="lp-footer__bottom">
          <span className="lp-footer__copyright">
            &copy; {currentYear} Vaccination Portal &mdash; SE2030 project.
          </span>
          <span className="lp-footer__status">
            Public Health Service
          </span>
        </div>
      </div>
    </footer>
  );
}
