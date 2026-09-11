import React, { useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import PublicHeader from '../components/PublicHeader';
import PublicFooter from '../components/PublicFooter';
import PageHero from '../components/PageHero';
import JourneyTabs from '../components/JourneyTabs';
import JourneyTimeline from '../components/JourneyTimeline';
import ReadinessGate from '../components/ReadinessGate';
import FaqAccordion from '../components/FaqAccordion';
import useLandingReveal from '../hooks/useLandingReveal';
import usePremiumMotion from '../hooks/usePremiumMotion';
import usePublicPageTop from '../hooks/usePublicPageTop';
import PremiumBackdrop from '../components/PremiumBackdrop';
import PublicServiceGuide from '../components/PublicServiceGuide';
import '../public-landing.css';
import '../premium-public.css';
import '../clinical-public.css';

const JOURNEYS = [
  {
    id: 'citizen',
    label: 'For citizens',
    steps: [
      {
        num: '01',
        phase: 'Minutes',
        title: 'Create your account',
        icon: 'account',
        desc: 'Register once with your details. Citizen accounts are active immediately — no approval step.',
      },
      {
        num: '02',
        phase: 'Before booking',
        title: 'Complete pre-vaccination screening',
        icon: 'screening',
        desc: 'Answer a short eligibility questionnaire for your campaign. Your answers determine which appointments you can book.',
      },
      {
        num: '03',
        phase: 'Your choice',
        title: 'Book an appointment',
        icon: 'booking',
        desc: 'Pick a vaccination session at a center near you, from the campaigns your screening qualifies you for.',
      },
      {
        num: '04',
        phase: 'On the day',
        title: 'Attend the clinic',
        icon: 'clinic',
        desc: 'Arrive at your time slot. Clinic staff check you in and administer your vaccination.',
      },
      {
        num: '05',
        phase: 'Afterwards',
        title: 'Your record updates',
        icon: 'record',
        desc: 'Your immunisation history and certificate are recorded automatically — visible only to you.',
      },
    ],
  },
  {
    id: 'staff',
    label: 'For health staff',
    steps: [
      {
        num: '01',
        phase: 'Request',
        title: 'Request staff access',
        icon: 'request',
        desc: 'Health staff submit a registration request describing their intended role.',
      },
      {
        num: '02',
        phase: 'Review',
        title: 'Administrator review',
        icon: 'review',
        desc: 'An administrator reviews and approves the request before the account is activated.',
      },
      {
        num: '03',
        phase: 'Planning',
        title: 'Plan campaign and center',
        icon: 'planning',
        desc: 'Campaign officers define target groups, dose programmes, and locations; center managers maintain rosters, hours, rooms, and equipment.',
      },
      {
        num: '04',
        phase: 'Scheduling',
        title: 'Schedule sessions',
        icon: 'sessions',
        desc: 'Sessions are created — one-off or recurring series — with capacity planned per station and time slot.',
      },
      {
        num: '05',
        phase: 'Gate',
        title: 'Verify readiness and open',
        icon: 'readiness',
        desc: 'The readiness gate validates hours, stations, staff, and conflicts. Only then does the session open for bookings.',
      },
    ],
  },
];

const FAQ_ITEMS = [
  {
    question: 'Who can see my vaccination record?',
    answer:
      'Only you. Citizens see their own record only; health staff see operational session data, not personal histories, unless your care requires it.',
  },
  {
    question: 'How long does staff access take to approve?',
    answer:
      'Staff accounts are activated after an administrator reviews the request — usually within one working day.',
  },
  {
    question: 'Can I reschedule or cancel my appointment?',
    answer:
      'Yes. Citizens can reschedule or cancel a booked appointment from their dashboard, subject to the session\u2019s booking rules.',
  },
  {
    question: 'What happens if my screening answers change?',
    answer:
      'If your situation changes before your appointment, you can resubmit your screening — a health officer reviews any updates that affect eligibility.',
  },
  {
    question: 'How does the portal decide a session is ready?',
    answer:
      'A readiness gate checks operating hours, active stations, planned capacity, qualified staff, and schedule conflicts. Every condition must pass before bookings open.',
  },
  {
    question: 'Does the service cost anything?',
    answer:
      'No. The Vaccination Portal is a public health service — registration, booking, and records are free.',
  },
];

export default function HowItWorksPage({ onLoginClick }) {
  const rootRef = useRef(null);
  useLandingReveal(rootRef);
  usePremiumMotion(rootRef);
  usePublicPageTop();
  const [activeJourney, setActiveJourney] = useState('citizen');
  const journey = JOURNEYS.find((j) => j.id === activeJourney);

  return (
    <div className="lp-page lp-page--how" ref={rootRef}>
      <PremiumBackdrop />
      <a className="lp-skip-link" href="#lp-main">
        Skip to main content
      </a>
      <PublicHeader onLoginClick={onLoginClick} />
      <main id="lp-main" tabIndex={-1}>
        <PageHero
          variant="journey"
          index="01"
          label="How it works"
          title="One system,"
          accent="two journeys."
          lead="Citizens move from registration to a recorded vaccination. Health teams move from planning to an opened, ready clinic. Both share the same secure spine."
        />
        <PublicServiceGuide />

        {/* Journey switcher + timeline */}
        <section className="lp-section lp-section--subtle lp-journey" aria-labelledby="lp-journey-title">
          <div className="lp-container">
            <div className="lp-kicker" data-reveal>
              <span className="lp-kicker__index">02</span>
              <span className="lp-kicker__label">The journeys</span>
              <span className="lp-kicker__rule" aria-hidden="true" />
            </div>
            <h2 className="lp-section__title" id="lp-journey-title" data-reveal>
              Follow a journey from start to finish
            </h2>

            <div className="lp-journey__tabs" data-reveal>
              <JourneyTabs
                tabs={JOURNEYS.map(({ id, label }) => ({ id, label }))}
                activeId={activeJourney}
                onChange={setActiveJourney}
              />
            </div>

            <div
              key={journey.id}
              role="tabpanel"
              id={`lp-journey-panel-${journey.id}`}
              aria-labelledby={`lp-journey-tab-${journey.id}`}
              tabIndex={-1}
              className="lp-journey__panel"
            >
              <JourneyTimeline
                steps={journey.steps}
                ariaLabel={`${journey.label} journey steps`}
              />
            </div>
          </div>
        </section>

        {/* Readiness gate */}
        <section className="lp-section lp-gate-section" aria-labelledby="lp-gate-title">
          <div className="lp-container">
            <div className="lp-kicker" data-reveal>
              <span className="lp-kicker__index">03</span>
              <span className="lp-kicker__label">Readiness gate</span>
              <span className="lp-kicker__rule" aria-hidden="true" />
            </div>
            <div className="lp-section__header" data-reveal>
              <div className="lp-section__header-text">
                <h2 className="lp-section__title" id="lp-gate-title">
                  No session opens until every check{' '}
                  <span className="lp-hero__accent">passes</span>
                </h2>
              </div>
              <p className="lp-section__lead">
                The gate is how the portal keeps every clinic honest &mdash;
                five conditions, one verdict.
              </p>
            </div>

            <ReadinessGate />
          </div>
        </section>

        {/* FAQ */}
        <section className="lp-section lp-section--subtle lp-faq-section" aria-labelledby="lp-faq-title">
          <div className="lp-container">
            <div className="lp-kicker" data-reveal>
              <span className="lp-kicker__index">04</span>
              <span className="lp-kicker__label">Questions</span>
              <span className="lp-kicker__rule" aria-hidden="true" />
            </div>
            <h2 className="lp-section__title" id="lp-faq-title" data-reveal>
              Frequently asked
            </h2>
            <FaqAccordion items={FAQ_ITEMS} />
          </div>
        </section>

        {/* Closing CTA — same deep band language as Home */}
        <section className="lp-cta-section" aria-labelledby="lp-cta-title">
          <div className="lp-container lp-cta-inner">
            <div className="lp-cta-panel__content">
              <div className="lp-cta-kicker">
                <span className="lp-cta-kicker__label">Next: system overview</span>
                <span className="lp-cta-kicker__rule" aria-hidden="true" />
              </div>
              <h2 className="lp-cta-panel__title" id="lp-cta-title">
                See what keeps the service trustworthy.
              </h2>
              <p className="lp-cta-panel__text">
                Review the roles, safeguards, and operating principles behind
                the portal.
              </p>
            </div>
            <div className="lp-cta-panel__actions">
              <Link to="/about" className="lp-cta-panel__link">
                Explore system safeguards
              </Link>
            </div>
          </div>
        </section>
      </main>
      <PublicFooter />
    </div>
  );
}
