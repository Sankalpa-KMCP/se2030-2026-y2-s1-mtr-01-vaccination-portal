import React from 'react';
import bannerImage from '../../../shared/assets/citizen-health-banner.png';

export default function BottomHealthBanner() {
  return (
    <aside className="citizen-bottom-banner citizen-panel" aria-label="Health awareness banner">
      <div className="citizen-bottom-banner__shapes" aria-hidden="true">
        <span className="citizen-bottom-banner__circle citizen-bottom-banner__circle--1" />
        <span className="citizen-bottom-banner__circle citizen-bottom-banner__circle--2" />
      </div>

      <div className="citizen-bottom-banner__content">
        <div className="citizen-bottom-banner__icon-box" aria-hidden="true">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z" />
            <path d="M12 5v6" />
            <path d="M9 8h6" />
          </svg>
        </div>

        <div className="citizen-bottom-banner__copy">
          <strong className="citizen-bottom-banner__headline">
            Vaccination today for a healthier tomorrow
          </strong>
          <p className="citizen-bottom-banner__tagline">
            Healthy individuals. Stronger communities. A brighter Sri Lanka.
          </p>
        </div>
      </div>

      <div className="citizen-bottom-banner__visual" aria-hidden="true">
        <img
          src={bannerImage}
          alt=""
          className="citizen-bottom-banner__image"
          loading="lazy"
        />
        <span className="citizen-bottom-banner__fade" aria-hidden="true" />
      </div>
    </aside>
  );
}
