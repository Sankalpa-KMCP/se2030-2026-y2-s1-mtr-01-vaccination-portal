import React from 'react';
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
];

export default function PublicHeader({ onLoginClick }) {
  return (
    <header className="lp-header">
      <div className="lp-container lp-header__inner">
        <Link to="/" className="lp-header__brand-group" aria-label="Vaccination Portal — home">
          <BrandGlyph />
          <span className="lp-header__brand">Vaccination Portal</span>
        </Link>

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
