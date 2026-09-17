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
import '../glass-header.css';

const JOURNEYS = [
  {
    id: 'citizen',
    label: 'For citizens',
    steps: [
      {
        num: '01',
        phase: 'Minutes',
        title: 'Create your account and sign in',
        icon: 'account',
        desc: 'Register with your full name, email, phone number, and password. Your account is active immediately, and the portal attempts to sign you in.',
      },
      {
        num: '02',
        phase: 'Before booking',
        title: 'Choose a campaign and complete screening',
        icon: 'screening',
        desc: 'Complete the campaign questionnaire and review its preliminary result. A Healthcare Officer records the final campaign-specific eligibility decision.',
      },
      {
        num: '03',
        phase: 'Your choice',
        title: 'Search and book an appointment',
        icon: 'booking',
        desc: 'After final eligibility is recorded, search future sessions by campaign, date, and optional center, then book an available slot.',
      },
      {
        num: '04',
        phase: 'Before your visit',
        title: 'Manage appointments or waitlists',
        icon: 'clinic',
        desc: 'View appointments, cancel or reschedule under the 24-hour rule, or join and manage a full session’s waitlist.',
      },
      {
        num: '05',
        phase: 'At the clinic',
        title: 'Attend and check in',
        icon: 'record',
        desc: 'Attend the booked clinic, where authorized staff record your arrival and check-in.',
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
        desc: 'Staff submit a registration request describing their intended role.',
      },
      {
        num: '02',
        phase: 'Review',
        title: 'Administrator review',
        icon: 'review',
        desc: 'An Administrator reviews and approves the request before the account is activated.',
      },
      {
        num: '03',
        phase: 'Planning',
        title: 'Plan campaign and center',
        icon: 'planning',
        desc: 'Campaign Officers define target groups, dose programmes, and locations; Center Managers maintain rosters, hours, rooms, and equipment.',
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
        desc: 'The readiness gate validates hours, stations, staff, capacity, and conflicts. Only then does the session open for bookings.',
      },
    ],
  },
];

const FAQ_ITEMS = [
  {
    question: 'Which profile details can I update?',
    answer:
      'Signed-in citizens can update their full name, phone number, date of birth, gender, and address. Email and password changes are not available on the profile page.',
  },
  {
    question: 'How long does staff access take to approve?',
    answer:
      'Staff accounts remain pending until an Administrator approves or rejects the request. The portal does not provide a guaranteed approval time.',
  },
  {
    question: 'Can I reschedule or cancel my appointment?',
    answer:
      'Yes. Citizens can cancel or reschedule from their dashboard at least 24 hours before the session. Authorized staff can override the 24-hour restriction with a recorded reason.',
  },
  {
    question: 'What happens if my screening answers change?',
    answer:
      'Citizens can resubmit answers when corrections are requested. A Healthcare Officer reviews submissions and records the final campaign-specific eligibility decision.',
  },
  {
    question: 'How does the portal decide a session is ready?',
    answer:
      'A readiness gate checks operating hours, active stations, planned capacity, qualified staff, and schedule conflicts. Every condition must pass before bookings open.',
  },
  {
    question: 'What happens when a session is full?',
    answer:
      'An eligible citizen can join the session waitlist, see its status and position, and leave it. When capacity becomes available, the portal promotes the next eligible entry in FIFO order and shows the promoted state in the citizen portal.',
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
          lead="Citizens move from account creation and screening to appointment booking and clinic check-in. Health teams move from planning to an opened, ready clinic."
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
              Follow each journey step by step
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
