import React, { useState, useEffect } from 'react';
import { Link, NavLink } from 'react-router-dom';
import portalCareMark from '../assets/portal-care-mark.svg';

function BrandGlyph() {
  return (
    <span className="lp-header__glyph" aria-hidden="true">
      <img src={portalCareMark} alt="" width="50" height="50" />
    </span>
  );
}

const NAV_ITEMS = [
  { to: '/', label: 'Home', end: true },
  { to: '/how-it-works', label: 'How it works', end: false },
  { to: '/about', label: 'About', end: false },
  { to: '/verify-certificate', label: 'Verify Certificate', end: false },
];

export default function PublicHeader({ onLoginClick }) {
  const [textSize, setTextSize] = useState(() => {
    return localStorage.getItem('vax_text_size') || 'normal';
  });

  useEffect(() => {
    document.documentElement.classList.remove('vax-text-normal', 'vax-text-large', 'vax-text-xl');
    if (textSize === 'large') {
      document.documentElement.classList.add('vax-text-large');
    } else if (textSize === 'xl') {
      document.documentElement.classList.add('vax-text-xl');
    } else {
      document.documentElement.classList.add('vax-text-normal');
    }
    localStorage.setItem('vax_text_size', textSize);
  }, [textSize]);

  return (
    <header className="lp-header">
      <div className="lp-container lp-header__inner">
        <Link to="/" className="lp-header__brand-group" aria-label="Vaccination Portal — home">
          <BrandGlyph />
          <span className="lp-header__brand-copy">
            <span className="lp-header__brand">Vaccination Portal</span>
            <span className="lp-header__brand-subtitle">Public Health Services</span>
          </span>
        </Link>

        {/* Accessibility & Hotline Bar */}
        <div className="lp-header__utility" aria-label="Accessibility and Support">
          <div className="lp-header__access-ctrl" title="Adjust text size">
            <span className="lp-header__access-label">Text size</span>
            <button
              type="button"
              className={`lp-header__font-btn ${textSize === 'normal' ? 'is-active' : ''}`}
              onClick={() => setTextSize('normal')}
              aria-label="Standard text size"
              title="Standard text size"
            >
              A
            </button>
            <button
              type="button"
              className={`lp-header__font-btn ${textSize === 'large' ? 'is-active' : ''}`}
              onClick={() => setTextSize('large')}
              aria-label="Large text size for senior citizens"
              title="Large text size"
            >
              A+
            </button>
            <button
              type="button"
              className={`lp-header__font-btn ${textSize === 'xl' ? 'is-active' : ''}`}
              onClick={() => setTextSize('xl')}
              aria-label="Extra large text size for high visibility"
              title="Extra large text size"
            >
              A++
            </button>
          </div>

          <a
            href="tel:19008292273"
            className="lp-header__helpline"
            aria-label="Public vaccination helpline 1900-VAX-CARE"
            title="Public vaccination helpline"
          >
            <i className="bi bi-telephone-fill" aria-hidden="true" />
            <span className="lp-header__helpline-copy">
              <small>Public helpline</small>
              <strong>1900-VAX-CARE</strong>
            </span>
          </a>
        </div>

        <nav className="lp-header__nav" aria-label="Main navigation">
          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) =>
                `lp-header__link${isActive ? ' lp-is-active' : ''}`
              }
            >
              {item.label}
            </NavLink>
          ))}
          <button
            type="button"
            className="lp-btn lp-btn--header"
            onClick={onLoginClick}
          >
            Login
            <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
              <path d="M3 8h9m-3.5-3.5L12 8l-3.5 3.5" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
          </button>
        </nav>
      </div>
    </header>
  );
}
