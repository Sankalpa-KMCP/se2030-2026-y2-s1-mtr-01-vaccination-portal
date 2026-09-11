import React, { useRef } from 'react';
import { Link } from 'react-router-dom';
import PublicHeader from '../components/PublicHeader';
import PublicFooter from '../components/PublicFooter';
import PageHero from '../components/PageHero';
import useLandingReveal from '../hooks/useLandingReveal';
import usePremiumMotion from '../hooks/usePremiumMotion';
import usePublicPageTop from '../hooks/usePublicPageTop';
import PremiumBackdrop from '../components/PremiumBackdrop';
import PublicServiceGuide from '../components/PublicServiceGuide';
import '../public-landing.css';
import '../premium-public.css';
import '../clinical-public.css';

const ROLES = [
  {
    num: '01',
    role: 'Citizen',
    scope: 'Self-service',
    desc: 'Registers, completes screening, books appointments, and views a personal immunisation record.',
    icon: (
      <>
        <circle cx="12" cy="7.5" r="3.4" stroke="currentColor" strokeWidth="1.6" />
        <path d="M5.5 19.5c.8-3.2 3.4-5 6.5-5s5.7 1.8 6.5 5" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
      </>
    ),
  },
  {
    num: '02',
    role: 'Healthcare Officer',
    scope: 'Clinical operations',
    desc: 'Runs vaccination administration, reviews screening outcomes, and manages clinic sessions on the day.',
    icon: (
      <>
        <path d="M9.5 12.5a4.5 4.5 0 0 0-4.5 4.5v2h9v-2a4.5 4.5 0 0 0-4.5-4.5Z" stroke="currentColor" strokeWidth="1.6" strokeLinejoin="round" />
        <circle cx="9.5" cy="7.5" r="2.8" stroke="currentColor" strokeWidth="1.6" />
        <path d="M16 8.5v6M13 11.5h6" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
      </>
    ),
  },
  {
    num: '03',
    role: 'Center Manager',
    scope: 'Center readiness',
    desc: 'Maintains each center\u2019s operating hours, rooms, equipment, and staff rosters.',
    icon: (
      <>
        <path d="M4.5 20.5v-10L12 4.5l7.5 6v10" stroke="currentColor" strokeWidth="1.6" strokeLinejoin="round" />
        <path d="M12 10.5v4.5M9.75 12.75h4.5" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
      </>
    ),
  },
  {
    num: '04',
    role: 'Campaign Officer',
    scope: 'Programme design',
    desc: 'Defines campaigns — target groups, dose programmes, and locations — and guides their lifecycle.',
    icon: (
      <>
        <circle cx="12" cy="12" r="8.5" stroke="currentColor" strokeWidth="1.6" />
        <circle cx="12" cy="12" r="4.5" stroke="currentColor" strokeWidth="1.6" />
        <circle cx="12" cy="12" r="1.2" fill="currentColor" />
      </>
    ),
  },
  {
    num: '05',
    role: 'Management User',
    scope: 'Oversight',
    desc: 'Monitors campaign and center operations across the programme through operational analytics.',
    icon: (
      <>
        <path d="M4 20.5h16" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
        <path d="M7 17V10M12 17V5.5M17 17v-9" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
      </>
    ),
  },
  {
    num: '06',
    role: 'Admin',
    scope: 'System authority',
    desc: 'Approves staff access, configures screening questionnaires, and holds system-wide authority.',
    icon: (
      <>
        <path d="M12 3 5.5 5.5v4.8c0 4.2 2.7 8 6.5 9.2 3.8-1.2 6.5-5 6.5-9.2V5.5L12 3Z" stroke="currentColor" strokeWidth="1.6" strokeLinejoin="round" />
        <path d="M9 12.2l2.1 2.1L15.5 10" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
      </>
    ),
  },
];

const LEDGER = [
  {
    num: '01',
    title: 'Campaign lifecycle',
    desc: 'Campaigns move through controlled stages — draft, configured, active, suspended, completed — each transition recorded with who made it and why.',
  },
  {
    num: '02',
    title: 'Center operations',
    desc: 'Every center keeps a single authoritative record of hours, rooms, stations, equipment, and staff rosters.',
  },
  {
    num: '03',
    title: 'Session scheduling',
    desc: 'One-off sessions and recurring series are planned with capacity per station and time slot; changes are previewed before they are confirmed.',
  },
  {
    num: '04',
    title: 'Readiness validation',
    desc: 'Hours, stations, staff, capacity, and conflicts are validated as a gate before any session opens for bookings.',
  },
  {
    num: '05',
    title: 'Records & certificates',
    desc: 'Vaccination records and digital certificates are created when clinics administer doses — tied to citizens, campaigns, and sessions.',
  },
];

const COMMITMENTS = [
  {
    num: '01',
    title: 'Controlled access',
    desc: 'Every account sees exactly what its role permits — nothing more.',
  },
  {
    num: '02',
    title: 'Recorded changes',
    desc: 'Operational changes carry their rationale and author, permanently.',
  },
  {
    num: '03',
    title: 'Validated openings',
    desc: 'Sessions accept bookings only after the five-check gate passes.',
  },
  {
    num: '04',
    title: 'Operational safety',
    desc: 'Capacity follows real stations and rosters — never guesswork.',
  },
];

const DATA_STATEMENTS = [
  {
    title: 'Privacy',
    statement: 'Your record is yours.',
    detail: 'Citizens see their own immunisation data only. Personal information is stored securely and access is role-scoped end to end.',
    icon: (
      <>
        <rect x="5.5" y="10.5" width="13" height="9.5" rx="2" stroke="currentColor" strokeWidth="1.5" />
        <path d="M8.5 10.5V8a3.5 3.5 0 0 1 7 0v2.5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
        <circle cx="12" cy="15.2" r="1.3" fill="currentColor" />
      </>
    ),
  },
  {
    title: 'Traceability',
    statement: 'Every change leaves a mark.',
    detail: 'Campaign decisions, staffing revisions, and session changes are recorded with administrative metadata — reviewable at any time.',
    icon: (
      <>
        <path d="M5 19.5h14" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
        <path d="M8 16l1.5-4.5L12 4l2.5 7.5L16 16" stroke="currentColor" strokeWidth="1.5" strokeLinejoin="round" />
        <circle cx="12" cy="4" r="1.6" stroke="currentColor" strokeWidth="1.5" />
      </>
    ),
  },
  {
    title: 'Readiness',
    statement: 'Clinics open prepared.',
    detail: 'The readiness gate validates operating hours, stations, staff, capacity, and schedule conflicts before any session accepts bookings.',
    icon: (
      <>
        <path d="M4.5 20.5v-10L12 4.5l7.5 6v10" stroke="currentColor" strokeWidth="1.5" strokeLinejoin="round" />
        <path d="M9 14.5l2.2 2.2 4-4.5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
      </>
    ),
  },
];

export default function AboutPage({ onLoginClick }) {
  const rootRef = useRef(null);
  useLandingReveal(rootRef);
  usePremiumMotion(rootRef);
  usePublicPageTop();

  return (
    <div className="lp-page lp-page--about" ref={rootRef}>
      <PremiumBackdrop />
      <a className="lp-skip-link" href="#lp-main">
        Skip to main content
      </a>
      <PublicHeader onLoginClick={onLoginClick} />
      <main id="lp-main" tabIndex={-1}>
        <PageHero
          variant="about"
          index="01"
          label="About the portal"
          title="A system built on"
          accent="responsibility."
          lead="Six roles, five operational domains, one auditable record of every decision. This is the structure that keeps a vaccination programme trustworthy."
        />
        <PublicServiceGuide />

        {/* Roles registry */}
        <section className="lp-section lp-section--subtle lp-roles" aria-labelledby="lp-roles-title">
          <div className="lp-container">
            <div className="lp-kicker" data-reveal>
              <span className="lp-kicker__index">02</span>
              <span className="lp-kicker__label">The registry</span>
              <span className="lp-kicker__rule" aria-hidden="true" />
            </div>
            <div className="lp-section__header" data-reveal>
              <div className="lp-section__header-text">
                <h2 className="lp-section__title" id="lp-roles-title">
                  Six roles, one{' '}
                  <span className="lp-hero__accent">registry</span>
                </h2>
              </div>
              <p className="lp-section__lead">
                Access in the portal follows role boundaries. Each role carries
                its own scope and responsibility.
              </p>
            </div>

            <div className="lp-roles-registry" aria-label="Portal roles">
              {ROLES.map((entry, index) => (
                <article
                  key={entry.num}
                  className="lp-role"
                  data-reveal
                  data-tilt
                  data-tilt-strength="1.5"
                  style={{ '--reveal-delay': `${(index % 3) * 90}ms` }}
                >
                  <div className="lp-role__num-row">
                    <span className="lp-role__num" aria-hidden="true">{entry.num}</span>
                    <span className="lp-role__icon" aria-hidden="true">
                      <svg width="24" height="24" viewBox="0 0 24 24" fill="none">{entry.icon}</svg>
                    </span>
                  </div>
                  <div className="lp-role__body">
                    <div className="lp-role__head">
                      <h3 className="lp-role__title">{entry.role}</h3>
                      <span className="lp-role__scope">{entry.scope}</span>
                    </div>
                    <p className="lp-role__desc">{entry.desc}</p>
                  </div>
                </article>
              ))}
            </div>
          </div>
        </section>

        {/* Operational ledger */}
        <section className="lp-section lp-opledger" aria-labelledby="lp-opledger-title">
          <div className="lp-container">
            <div className="lp-kicker" data-reveal>
              <span className="lp-kicker__index">03</span>
              <span className="lp-kicker__label">Operational ledger</span>
              <span className="lp-kicker__rule" aria-hidden="true" />
            </div>
            <div className="lp-section__header" data-reveal>
              <div className="lp-section__header-text">
                <h2 className="lp-section__title" id="lp-opledger-title">
                  What the system{' '}
                  <span className="lp-hero__accent">governs</span>
                </h2>
              </div>
              <p className="lp-section__lead">
                Five operational domains, each with its own rules and record.
              </p>
            </div>

            <div className="lp-opledger-list">
              {LEDGER.map((item, index) => (
                <article
                  key={item.num}
                  className="lp-opledger-row"
                  data-reveal
                  style={{ '--reveal-delay': `${index * 80}ms` }}
                >
                  <span className="lp-opledger-row__num" aria-hidden="true">{item.num}</span>
                  <h3 className="lp-opledger-row__title">{item.title}</h3>
                  <p className="lp-opledger-row__desc">{item.desc}</p>
                </article>
              ))}
            </div>
          </div>
        </section>

        {/* Trust commitments */}
        <section className="lp-section lp-section--subtle lp-commitments" aria-labelledby="lp-commitments-title">
          <div className="lp-container">
            <div className="lp-kicker" data-reveal>
              <span className="lp-kicker__index">04</span>
              <span className="lp-kicker__label">Trust &amp; accountability</span>
              <span className="lp-kicker__rule" aria-hidden="true" />
            </div>
            <h2 className="lp-section__title" id="lp-commitments-title" data-reveal>
              Accountable by design
            </h2>

            <div className="lp-commitments-grid">
              {COMMITMENTS.map((item, index) => (
                <div
                  key={item.num}
                  className="lp-commitment"
                  data-reveal
                  style={{ '--reveal-delay': `${(index % 2) * 100}ms` }}
                >
                  <span className="lp-commitment__num" aria-hidden="true">{item.num}</span>
                  <h3 className="lp-commitment__title">{item.title}</h3>
                  <p className="lp-commitment__desc">{item.desc}</p>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* Data statements */}
        <section className="lp-section lp-statements" aria-labelledby="lp-statements-title">
          <div className="lp-container">
            <div className="lp-kicker" data-reveal>
              <span className="lp-kicker__index">05</span>
              <span className="lp-kicker__label">Data commitments</span>
              <span className="lp-kicker__rule" aria-hidden="true" />
            </div>
            <h2 className="lp-section__title" id="lp-statements-title" data-reveal>
              Three commitments we hold
            </h2>

            <div className="lp-statements-rail">
              {DATA_STATEMENTS.map((item, index) => (
                <article
                  key={item.title}
                  className="lp-statement"
                  data-reveal
                  data-tilt
                  data-tilt-strength="1.5"
                  style={{ '--reveal-delay': `${index * 110}ms` }}
                >
                  <span className="lp-statement__icon" aria-hidden="true">
                    <svg width="34" height="34" viewBox="0 0 24 24" fill="none">{item.icon}</svg>
                  </span>
                  <span className="lp-statement__title">{item.title}</span>
                  <h3 className="lp-statement__headline">{item.statement}</h3>
                  <p className="lp-statement__detail">{item.detail}</p>
                </article>
              ))}
            </div>
          </div>
        </section>

        {/* Closing CTA */}
        <section className="lp-cta-section" aria-labelledby="lp-cta-title">
          <div className="lp-container lp-cta-inner">
            <div className="lp-cta-panel__content">
              <div className="lp-cta-kicker">
                <span className="lp-cta-kicker__label">Next: care journey</span>
                <span className="lp-cta-kicker__rule" aria-hidden="true" />
              </div>
              <h2 className="lp-cta-panel__title" id="lp-cta-title">
                See the service from start to record.
              </h2>
              <p className="lp-cta-panel__text">
                Follow the connected steps for citizens and health teams, from
                registration to a recorded vaccination.
              </p>
            </div>
            <div className="lp-cta-panel__actions">
              <Link to="/how-it-works" className="lp-cta-panel__link">
                Explore how it works
              </Link>
            </div>
          </div>
        </section>
      </main>
      <PublicFooter />
    </div>
  );
}
