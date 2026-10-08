import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import PublicHeader from '../components/PublicHeader';
import HeroSection from '../components/HeroSection';
import GenerationalCarePaths from '../components/GenerationalCarePaths';
import PortalAccessSection from '../components/PortalAccessSection';
import CapabilitiesSection from '../components/CapabilitiesSection';
import HowItWorksSection from '../components/HowItWorksSection';
import TrustSection from '../components/TrustSection';
import FinalCtaSection from '../components/FinalCtaSection';
import CareProgressRail from '../components/CareProgressRail';
import PublicFooter from '../components/PublicFooter';
import PublicServiceGuide from '../components/PublicServiceGuide';
import ConnectedCommunityIntro from '../components/ConnectedCommunityIntro';
import { shouldShowConnectedCommunityIntro } from '../components/connectedCommunityIntroSession';
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
import '../hospital-home.css';

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

export default function LandingPage({ onLoginClick, isInitialHomeLoad = false, onIntroAttempted }) {
  const rootRef = useRef(null);
  const [showIntro, setShowIntro] = useState(() => shouldShowConnectedCommunityIntro(isInitialHomeLoad));
  const finishIntro = useCallback(() => setShowIntro(false), []);
  useEffect(() => {
    if (isInitialHomeLoad) onIntroAttempted?.();
  }, [isInitialHomeLoad, onIntroAttempted]);
  useLandingReveal(rootRef);
  usePublicPageTop();

  return (
    <>
    <div className="lp-page lp-page--home" ref={rootRef} inert={showIntro}>
      <a className="lp-skip-link" href="#lp-main">
        Skip to main content
      </a>
      <CareProgressRail />
      <PublicHeader onLoginClick={onLoginClick} />
      <main id="lp-main" tabIndex={-1}>
        <HeroSection />
        <section className="hospital-shortcuts" aria-label="Quick access to vaccination services">
          <div className="lp-container hospital-shortcuts__grid">
            <Link to="/register/citizen" className="hospital-shortcut hospital-shortcut--register">
              <span className="hospital-shortcut__icon hospital-shortcut__icon--green" aria-hidden="true">
                <i className="bi bi-person-plus-fill" />
              </span>
              <span>
                <strong>Begin your vaccination journey</strong>
                <small>Create an account and explore campaigns</small>
              </span>
              <span className="hospital-shortcut__arrow" aria-hidden="true">↗</span>
            </Link>
            <Link to="/login" className="hospital-shortcut hospital-shortcut--booking">
              <span className="hospital-shortcut__icon hospital-shortcut__icon--blue" aria-hidden="true">
                <i className="bi bi-calendar2-check-fill" />
              </span>
              <span>
                <strong>Manage your appointments</strong>
                <small>Sign in to view or change your booking</small>
              </span>
              <span className="hospital-shortcut__arrow" aria-hidden="true">↗</span>
            </Link>
            <Link to="/how-it-works" className="hospital-shortcut hospital-shortcut--guide">
              <span className="hospital-shortcut__icon hospital-shortcut__icon--amber" aria-hidden="true">
                <i className="bi bi-heart-pulse-fill" />
              </span>
              <span>
                <strong>Know your next step</strong>
                <small>Understand screening, booking and arrival</small>
              </span>
              <span className="hospital-shortcut__arrow" aria-hidden="true">↗</span>
            </Link>
          </div>
        </section>

        {/* Tailored Pathways with Rich Photography */}
        <GenerationalCarePaths />

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
    {showIntro && <ConnectedCommunityIntro onComplete={finishIntro} />}
    </>
  );
}
