import React from 'react';

function CampaignIcon() {
  return (
    <svg width="26" height="26" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <circle cx="12" cy="12" r="8.5" stroke="currentColor" strokeWidth="1.6" />
      <circle cx="12" cy="12" r="4.5" stroke="currentColor" strokeWidth="1.6" />
      <circle cx="12" cy="12" r="1.2" fill="currentColor" />
    </svg>
  );
}

function ClinicIcon() {
  return (
    <svg width="26" height="26" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d="M4.5 20.5v-10L12 4.5l7.5 6v10" stroke="currentColor" strokeWidth="1.6" strokeLinejoin="round" />
      <path d="M12 10.5v4.5M9.75 12.75h4.5" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
    </svg>
  );
}

function SessionIcon() {
  return (
    <svg width="26" height="26" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <rect x="4" y="5.5" width="16" height="15" rx="2.2" stroke="currentColor" strokeWidth="1.6" />
      <path d="M8 3v4M16 3v4M4 10.5h16" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
    </svg>
  );
}

function ReadinessIcon() {
  return (
    <svg width="26" height="26" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d="M12 3 5.5 5.5v4.8c0 4.2 2.7 8 6.5 9.2 3.8-1.2 6.5-5 6.5-9.2V5.5L12 3Z" stroke="currentColor" strokeWidth="1.6" strokeLinejoin="round" />
      <path d="M9 12.2l2.1 2.1L15.5 10" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function AccessIcon() {
  return (
    <svg width="26" height="26" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <rect x="5.5" y="10.5" width="13" height="9.5" rx="2" stroke="currentColor" strokeWidth="1.6" />
      <path d="M8.5 10.5V8a3.5 3.5 0 0 1 7 0v2.5" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
      <circle cx="12" cy="15.2" r="1.3" fill="currentColor" />
    </svg>
  );
}

const CAPABILITIES = [
  {
    num: '01',
    icon: <CampaignIcon />,
    title: 'Vaccination campaigns',
    desc: 'Health teams set up programmes for specific areas and groups — from planning through to completion.',
    points: ['Area & group targeting', 'Multi-dose schedules'],
  },
  {
    num: '02',
    icon: <ClinicIcon />,
    title: 'Clinic & center management',
    desc: 'Each center keeps staffing, opening hours, rooms, and equipment in one place.',
    points: ['Staff rosters', 'Rooms & stations'],
  },
  {
    num: '03',
    icon: <SessionIcon />,
    title: 'Session planning & booking',
    desc: 'One-off or repeating sessions, with every schedule change recorded and explained.',
    points: ['One-off & recurring', 'Recorded changes'],
  },
  {
    num: '04',
    icon: <ReadinessIcon />,
    title: 'Readiness before opening',
    desc: 'Hours, stations, and qualified staff are all confirmed before a session opens.',
    points: ['Readiness checks', 'Planned capacity'],
  },
  {
    num: '05',
    icon: <AccessIcon />,
    title: 'Access you can trust',
    desc: 'Secure sign-in, with every account seeing only what its role allows.',
    points: ['Secure sign-in', 'Role-based access'],
  },
];

export default function CapabilitiesSection() {
  return (
    <section
      className="lp-section lp-capabilities"
      id="capabilities"
      aria-labelledby="lp-capabilities-title"
    >
      <div className="lp-container">
        <div className="lp-cap-layout">
          {/* Sticky editorial aside */}
          <aside className="lp-cap-aside">
            <div className="lp-cap-aside__inner" data-reveal>
              <div className="lp-kicker">
                <span className="lp-kicker__index">02</span>
                <span className="lp-kicker__label">Capabilities</span>
                <span className="lp-kicker__rule" aria-hidden="true" />
              </div>
              <h2 className="lp-cap-aside__title" id="lp-capabilities-title">
                One portal for the whole{' '}
                <span className="lp-hero__accent">journey</span>
              </h2>
              <p className="lp-cap-aside__text">
                From planning a local campaign to giving a vaccine at a clinic
                &mdash; every part of the process happens here.
              </p>

              <ul className="lp-cap-index" aria-label="Capability index">
                {CAPABILITIES.map((cap) => (
                  <li key={cap.num}>
                    <span>{cap.title}</span>
                    <span aria-hidden="true">{cap.num}</span>
                  </li>
                ))}
              </ul>
            </div>
          </aside>

          {/* Ledger rows */}
          <div className="lp-cap-ledger">
            {CAPABILITIES.map((cap, index) => (
              <article
                key={cap.num}
                className="lp-cap-row"
                data-reveal
                style={{ '--reveal-delay': `${index * 80}ms` }}
              >
                <span className="lp-cap-row__num" aria-hidden="true">{cap.num}</span>
                <div className="lp-cap-row__body">
                  <h3 className="lp-cap-row__title">{cap.title}</h3>
                  <p className="lp-cap-row__desc">{cap.desc}</p>
                </div>
                <span className="lp-cap-row__icon" aria-hidden="true">{cap.icon}</span>
              </article>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}
