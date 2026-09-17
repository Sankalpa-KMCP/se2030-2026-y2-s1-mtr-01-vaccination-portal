import React, { useRef } from 'react';
import { Link } from 'react-router-dom';
import PublicHeader from '../components/PublicHeader';
import HeroSection from '../components/HeroSection';
import PortalAccessSection from '../components/PortalAccessSection';
import CapabilitiesSection from '../components/CapabilitiesSection';
import HowItWorksSection from '../components/HowItWorksSection';
import TrustSection from '../components/TrustSection';
import FinalCtaSection from '../components/FinalCtaSection';
import CareProgressRail from '../components/CareProgressRail';
import PublicFooter from '../components/PublicFooter';
import PublicServiceGuide from '../components/PublicServiceGuide';
import useLandingReveal from '../hooks/useLandingReveal';
import usePublicPageTop from '../hooks/usePublicPageTop';
import '../public-landing.css';
import '../landing-page.css';
import '../premium-public.css';
import '../clinical-public.css';
import '../landing-motion.css';
import '../care-experience.css';
import '../glass-header.css';
import '../hero-identity.css';

/**
 * "Continue exploring" band — connects Home to the two interior pages
 * with the same ledger language, making the three pages feel like one site.
 */
function ContinueBand() {
  return (
    <section className="home-index" aria-labelledby="home-index-title">
      <div className="lp-container home-index__layout" data-reveal="links">
        <div className="home-index__intro">
          <span className="home-section-mark">Public information</span>
          <h2 id="home-index-title">Understand the service before you enter.</h2>
        </div>
        <nav className="home-index__links" aria-label="Explore public information">
          <Link to="/how-it-works" className="home-index__link">
            <span className="home-index__number" aria-hidden="true">01</span>
            <span>
              <strong>How it works</strong>
              <small>Follow the Citizen and Staff journeys.</small>
            </span>
            <span className="home-index__arrow" aria-hidden="true">&rarr;</span>
          </Link>
          <Link to="/about" className="home-index__link">
            <span className="home-index__number" aria-hidden="true">02</span>
            <span>
              <strong>About the system</strong>
              <small>See the roles and operational domains.</small>
            </span>
            <span className="home-index__arrow" aria-hidden="true">&rarr;</span>
          </Link>
        </nav>
      </div>
    </section>
  );
}

export default function LandingPage({ onLoginClick }) {
  const rootRef = useRef(null);
  useLandingReveal(rootRef);
  usePublicPageTop();

  return (
    <div className="lp-page lp-page--home" ref={rootRef}>
      <a className="lp-skip-link" href="#lp-main">
        Skip to main content
      </a>
      <CareProgressRail />
      <PublicHeader onLoginClick={onLoginClick} />
      <main id="lp-main" tabIndex={-1}>
        <HeroSection />
        <PublicServiceGuide />
        <PortalAccessSection />
        <CapabilitiesSection />
        <HowItWorksSection />
        <TrustSection />
        <ContinueBand />
        <FinalCtaSection />
      </main>
      <PublicFooter />
    </div>
  );
}
