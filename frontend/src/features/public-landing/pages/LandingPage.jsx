import React from 'react';
import PublicHeader from '../components/PublicHeader';
import HeroSection from '../components/HeroSection';
import CapabilitiesSection from '../components/CapabilitiesSection';
import HowItWorksSection from '../components/HowItWorksSection';
import TrustSection from '../components/TrustSection';
import FinalCtaSection from '../components/FinalCtaSection';
import PublicFooter from '../components/PublicFooter';
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
        <CapabilitiesSection />
        <HowItWorksSection />
        <TrustSection />
        <FinalCtaSection onLoginClick={onLoginClick} />
      </main>
      <PublicFooter onLoginClick={onLoginClick} />
    </div>
  );
}
