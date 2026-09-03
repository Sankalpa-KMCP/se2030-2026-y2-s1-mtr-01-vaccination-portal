import React, { useRef } from 'react';
import { Link } from 'react-router-dom';
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

/**
 * "Continue exploring" band — connects Home to the two interior pages
 * with the same ledger language, making the three pages feel like one site.
 */
function ContinueBand() {
  return (
    <section className="lp-section lp-continue" aria-label="Explore the portal">
      <div className="lp-container">
        <div className="lp-continue__grid">
          <Link to="/how-it-works" className="lp-continue__link" data-reveal>
            <span className="lp-continue__index" aria-hidden="true">01</span>
            <span className="lp-continue__body">
              <span className="lp-continue__label">The journeys</span>
              <span className="lp-continue__title">How it works</span>
              <span className="lp-continue__desc">
                Follow a citizen or a staff journey end to end.
              </span>
            </span>
            <span className="lp-continue__arrow" aria-hidden="true">&rarr;</span>
          </Link>

          <Link to="/about" className="lp-continue__link" data-reveal style={{ '--reveal-delay': '100ms' }}>
            <span className="lp-continue__index" aria-hidden="true">02</span>
            <span className="lp-continue__body">
              <span className="lp-continue__label">The structure</span>
              <span className="lp-continue__title">About the system</span>
              <span className="lp-continue__desc">
                Six roles, five domains, one accountable record.
              </span>
            </span>
            <span className="lp-continue__arrow" aria-hidden="true">&rarr;</span>
          </Link>
        </div>
      </div>
    </section>
  );
}

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
        <ContinueBand />
        <FinalCtaSection onLoginClick={onLoginClick} />
      </main>
      <PublicFooter onLoginClick={onLoginClick} />
    </div>
  );
}
