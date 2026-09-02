import React, { useRef } from 'react';
import PublicHeader from '../components/PublicHeader';
import HeroSection from '../components/HeroSection';
import PortalAccessSection from '../components/PortalAccessSection';
import CapabilitiesSection from '../components/CapabilitiesSection';
import HowItWorksSection from '../components/HowItWorksSection';
import TrustSection from '../components/TrustSection';
import FinalCtaSection from '../components/FinalCtaSection';
import PublicFooter from '../components/PublicFooter';
import useLandingReveal from '../hooks/useLandingReveal';
import '../public-landing.css';

export default function LandingPage({ onLoginClick }) {
  const rootRef = useRef(null);
  useLandingReveal(rootRef);

  return (
    <div className="lp-page" ref={rootRef}>
      <a className="lp-skip-link" href="#lp-main">
        Skip to main content
      </a>
      <PublicHeader onLoginClick={onLoginClick} />
      <main id="lp-main" tabIndex={-1}>
        <HeroSection onLoginClick={onLoginClick} />
        <PortalAccessSection />
        <CapabilitiesSection />
        <HowItWorksSection />
        <TrustSection />
        <FinalCtaSection onLoginClick={onLoginClick} />
      </main>
      <PublicFooter onLoginClick={onLoginClick} />
    </div>
  );
}
