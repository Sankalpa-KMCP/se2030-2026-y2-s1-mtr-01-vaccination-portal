import React from 'react';
import PublicHeader from '../components/PublicHeader';
import HeroSection from '../components/HeroSection';
import '../public-landing.css';

export default function LandingPage({ onLoginClick }) {
  return (
    <div className="lp-page">
      <a className="lp-skip-link" href="#lp-main">
        Skip to main content
      </a>
      <PublicHeader onLoginClick={onLoginClick} />
      <main className="lp-main" id="lp-main" tabIndex={-1}>
        <HeroSection onLoginClick={onLoginClick} />
      </main>
    </div>
  );
}
