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
    tag: 'Campaigns',
    title: 'Vaccination campaigns',
    desc: 'Health teams configure programmes for specific areas and priority groups with multi-dose schedule support.',
    points: ['Area & group targeting', 'Multi-dose schedules'],
  },
  {
    num: '02',
    icon: <ClinicIcon />,
    tag: 'Facilities',
    title: 'Clinic & center management',
    desc: 'Centralise center profiles, operational hours, room allocations, stations, and medical staffing in one place.',
    points: ['Staff rosters & rooms', 'Equipment & stations'],
  },
  {
    num: '03',
    icon: <SessionIcon />,
    tag: 'Scheduling',
    title: 'Session planning & booking',
    desc: 'Plan one-off or recurring clinic sessions with full audit tracking for every schedule adjustment.',
    points: ['One-off & recurring', 'Tracked adjustments'],
  },
  {
    num: '04',
    icon: <ReadinessIcon />,
    tag: 'Safety',
    title: 'Readiness before opening',
    desc: 'Confirm operating hours, station capacity, and certified staff before any session opens for bookings.',
    points: ['Pre-opening checklists', 'Verified capacity'],
  },
  {
    num: '05',
    icon: <AccessIcon />,
    tag: 'Security',
    title: 'Access you can trust',
    desc: 'Strict role-based boundaries ensure Citizens, Staff, and Administrators access only authorized capabilities.',
    points: ['Role-based access', 'Tamper-evident logs'],
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
        <div className="lp-section__header lp-section__header--centered" data-reveal>
          <div className="lp-kicker lp-kicker--centered">
            <span className="lp-kicker__index">02</span>
            <span className="lp-kicker__label">Capabilities</span>
            <span className="lp-kicker__rule" aria-hidden="true" />
          </div>
          <h2 className="lp-section__title" id="lp-capabilities-title">
            One portal for the whole <span className="lp-hero__accent">journey</span>
          </h2>
          <p className="lp-section__lead">
            From planning a local campaign to administering a dose at a clinic &mdash;
            every operational capability is coordinated in one accountable system.
          </p>
        </div>

        <div className="lp-capabilities-grid">
          {CAPABILITIES.map((cap, index) => (
            <article
              key={cap.num}
              className="lp-capability-card"
              data-reveal
              style={{ '--reveal-delay': `${index * 60}ms` }}
            >
              <div className="lp-capability-card__header">
                <span className="lp-capability-card__icon" aria-hidden="true">
                  {cap.icon}
                </span>
                <span className="lp-capability-card__tag">{cap.tag}</span>
                <span className="lp-capability-card__num" aria-hidden="true">{cap.num}</span>
              </div>
              <h3 className="lp-capability-card__title">{cap.title}</h3>
              <p className="lp-capability-card__desc">{cap.desc}</p>
              <ul className="lp-capability-card__points" aria-label={`Features for ${cap.title}`}>
                {cap.points.map((point) => (
                  <li key={point} className="lp-capability-card__point">
                    <span className="lp-capability-card__bullet" aria-hidden="true" />
                    <span>{point}</span>
                  </li>
                ))}
              </ul>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}
