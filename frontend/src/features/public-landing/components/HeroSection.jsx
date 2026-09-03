import React from 'react';
import { Link } from 'react-router-dom';
import HeroArtwork from './HeroArtwork';

const HERO_ASSURANCES = [
  { label: 'Official service', icon: 'shield' },
  { label: 'Secure and private', icon: 'lock' },
  { label: 'Free to use', icon: 'check' },
];

const HERO_STEPS = [
  { num: '01', label: 'Create your account' },
  { num: '02', label: 'Book a session near you' },
  { num: '03', label: 'Attend and get your record' },
];

function AssuricIcon({ name }) {
  if (name === 'shield') {
    return (
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <path d="M12 3 5.5 5.5v4.8c0 4.2 2.7 8 6.5 9.2 3.8-1.2 6.5-5 6.5-9.2V5.5L12 3Z" stroke="currentColor" strokeWidth="1.9" strokeLinejoin="round" />
      </svg>
    );
  }
  if (name === 'lock') {
    return (
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <rect x="5.5" y="10.5" width="13" height="9.5" rx="2" stroke="currentColor" strokeWidth="1.9" />
        <path d="M8.5 10.5V8a3.5 3.5 0 0 1 7 0v2.5" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" />
      </svg>
    );
  }
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d="M5 12.5l4.2 4.2L19 7.2" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

export default function HeroSection({ onLoginClick }) {
  return (
    <section className="lp-hero" aria-labelledby="lp-hero-title">
      <div className="lp-container">
        <div className="lp-hero__grid">
          <div className="lp-hero__content">
            <div className="lp-hero__eyebrow-row">
              <span className="lp-hero__eyebrow">
                Public Health Service
              </span>
            </div>

            <h1 className="lp-hero__title" id="lp-hero-title">
              Vaccination care, <span className="lp-hero__accent">coordinated</span>{' '}
              for your community.
            </h1>

            <p className="lp-hero__lead">
              Citizens, clinics, and health teams in one secure place &mdash;
              book sessions, run campaigns, and keep immunisation records.
            </p>

            <div className="lp-hero__actions">
              <Link to="/register/citizen" className="lp-btn lp-btn--primary">
                Get started as citizen
                <span className="lp-btn__arrow" aria-hidden="true">&rarr;</span>
              </Link>
              <button type="button" className="lp-btn lp-btn--secondary" onClick={onLoginClick}>
                Sign in
              </button>
              <Link to="/how-it-works" className="lp-btn lp-btn--quiet">
                How it works
              </Link>
            </div>

            <div className="lp-hero__meta">
              {HERO_ASSURANCES.map((item) => (
                <span key={item.label} className="lp-hero__meta-item">
                  <AssuricIcon name={item.icon} />
                  {item.label}
                </span>
              ))}
            </div>
          </div>

          <div className="lp-hero__art" aria-hidden="true">
            <HeroArtwork />
          </div>
        </div>

        {/* Numbered micro-steps strip — replaces the old steps card. */}
        <div className="lp-hero__strip">
          {HERO_STEPS.map((step) => (
            <div key={step.num} className="lp-hero__strip-item">
              <span className="lp-hero__strip-num">{step.num}</span>
              <span className="lp-hero__strip-label">{step.label}</span>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
