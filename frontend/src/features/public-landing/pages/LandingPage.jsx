import React from 'react';
import PublicHeader from '../components/PublicHeader';
import HeroSection from '../components/HeroSection';
import CapabilitiesSection from '../components/CapabilitiesSection';
import HowItWorksSection from '../components/HowItWorksSection';
import TrustSection from '../components/TrustSection';
import PublicFooter from '../components/PublicFooter';
import { ArrowRightIcon } from '../components/icons';
import { useRevealOnScroll } from '../hooks/useRevealOnScroll';
import '../public-landing.css';

export default function LandingPage({ onLoginClick }) {
  useRevealOnScroll();

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

        <section className="lp-cta" aria-labelledby="lp-cta-title">
          <div className="lp-container lp-cta__inner lp-reveal">
            <h2 className="lp-cta__title" id="lp-cta-title">Access the portal</h2>
            <p className="lp-cta__copy">
              Sign in with your authorized account to manage campaigns, centers and sessions.
            </p>
            <button type="button" className="lp-btn lp-btn--primary lp-btn--lg" onClick={onLoginClick}>
              Go to sign in
              <ArrowRightIcon className="lp-btn__icon" width="17" height="17" strokeWidth={2} />
            </button>
          </div>
        </section>
      </main>
      <PublicFooter onLoginClick={onLoginClick} />
    </div>
  );
}
