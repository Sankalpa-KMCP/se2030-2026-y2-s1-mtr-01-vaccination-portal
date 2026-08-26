import React from 'react';
import { BrandMark } from './icons';

export default function PublicHeader({ onLoginClick }) {
  return (
    <header className="lp-header">
      <nav className="lp-container lp-header__inner" aria-label="Portal home">
        <span className="lp-brand">
          <BrandMark className="lp-brand__glyph" width="26" height="26" />
          <span className="lp-brand__name">Web-Based Vaccination Portal</span>
        </span>
        <div className="lp-header__end">
          <a className="lp-header__link" href="#capabilities">
            Capabilities
          </a>
          <button
            type="button"
            className="lp-btn lp-btn--primary lp-btn--compact"
            onClick={onLoginClick}
          >
            Sign in
          </button>
        </div>
      </nav>
    </header>
  );
}
