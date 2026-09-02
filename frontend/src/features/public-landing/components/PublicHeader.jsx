import React from 'react';
import { Link, NavLink } from 'react-router-dom';

function BrandGlyph() {
  return (
    <span className="lp-header__glyph" aria-hidden="true">
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
        <path
          d="M12 2.5 4.5 5.4v5.3c0 4.7 3.1 8.9 7.5 10.3 4.4-1.4 7.5-5.6 7.5-10.3V5.4L12 2.5Z"
          stroke="currentColor"
          strokeWidth="1.7"
          strokeLinejoin="round"
        />
        <path
          d="M8.6 12.1v2.6h1.9v-2.6h2.8v2.6h1.9v-2.6h1.6v-1.7H8.6v1.7Z"
          fill="currentColor"
        />
      </svg>
    </span>
  );
}

function BannerEmblem() {
  return (
    <span className="lp-service-banner__emblem">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <path
          d="M12 2.5 4.5 5.4v5.3c0 4.7 3.1 8.9 7.5 10.3 4.4-1.4 7.5-5.6 7.5-10.3V5.4L12 2.5Z"
          stroke="currentColor"
          strokeWidth="1.6"
          strokeLinejoin="round"
        />
        <path d="M9 12.2v2.2h1.6v-2.2h2.4v2.2h1.6v-2.2h1.3v-1.4H9v1.4Z" fill="currentColor" />
      </svg>
      An official public health service
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
    <>
      <div className="lp-service-banner">
        <BannerEmblem />
      </div>
      <header className="lp-header">
        <div className="lp-container lp-header__inner">
          <Link to="/" className="lp-header__brand-group" aria-label="Vaccination Portal — home">
            <BrandGlyph />
            <span className="lp-header__brand">Vaccination Portal</span>
            <span className="lp-header__badge">Public Health Service</span>
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
            </button>
          </nav>
        </div>
      </header>
    </>
  );
}
