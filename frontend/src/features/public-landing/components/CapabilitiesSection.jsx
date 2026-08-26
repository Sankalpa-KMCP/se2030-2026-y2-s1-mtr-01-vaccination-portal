import React from 'react';

const CAPABILITIES = [
  {
    num: '01',
    title: 'Campaign Management',
    desc: 'Establish multi-dose programmes with sequence constraints, interval windows, target demographic groups and assigned delivery centers. Campaigns move through a controlled lifecycle from Draft to Active.',
    meta: 'Programme policy · Lifecycle state machine',
    note: 'Interval rules and state transitions audited per actor.',
    tags: ['Dose programme intervals', 'Target locations', 'Lifecycle control'],
  },
  {
    num: '02',
    title: 'Center Operations',
    desc: 'Maintain facilities with weekly operating hours, blackout closure dates and physical resource tracking — stations, rooms and cold-chain units. Rosters assign qualified personnel by operational role.',
    meta: 'Facilities & equipment · Staffing',
    note: 'Hours, blackouts and rosters versioned with closure checks.',
    tags: ['Operating hours matrix', 'Station tracking', 'Staff allocation rosters'],
  },
  {
    num: '03',
    title: 'Session Planning & Scheduling',
    desc: 'Create single sessions or recurring series (Daily, Weekly, Monthly) inside center-aligned time windows. Reschedules, transfers and cancellations are recorded under formal change control with mandatory justification.',
    meta: 'Single & recurring series · Change control',
    note: 'Recurrence expansion with justification on every change.',
    tags: ['Recurrence generator', 'Change control', 'Immutable change logs'],
  },
  {
    num: '04',
    title: 'Capacity & Readiness',
    desc: 'Calculate baseline throughput from active stations and appointment dwell times. A 6-metric pre-session check — hours, stations, staff, closures, campaign link — must pass before a session can open.',
    meta: 'Throughput formulation · Pre-flight diagnostics',
    note: 'Calculated baseline; overrides require manager sign-off.',
    tags: ['Throughput formula', 'Manager override', '6-point diagnostic gate'],
  },
  {
    num: '05',
    title: 'Secure Role-Based Access',
    desc: 'Cryptographic JWT sessions and explicit role boundaries. Permissions are isolated by assignment — no cross-domain privilege bleed, with cross-center safeguards on scheduling and approvals.',
    meta: 'Authentication · Authorization · Isolation',
    note: 'JWT-bound sessions with isolated role enforcement.',
    tags: ['Role boundaries', 'Conflict checks', 'Cryptographic sessions'],
  },
];

export default function CapabilitiesSection() {
  return (
    <section className="lp-section lp-section--paper" id="capabilities" aria-labelledby="lp-capabilities-heading">
      <div className="lp-container">
        <div className="lp-index-intro">
          <div className="lp-index-intro__copy">
            <p className="lp-kicker">System capabilities</p>
            <h2 className="lp-h2" id="lp-capabilities-heading">An operational specification, not a feature list.</h2>
            <p className="lp-lead lp-lead--narrow">
              Five interconnected modules govern how immunization delivery is planned, staffed, verified and recorded.
            </p>
          </div>
          <p className="lp-index-intro__note">
            The portal is an operations instrument — closer to a programme ledger than a marketing site. Each capability is a
            controlled administrative surface with traceability built in.
          </p>
        </div>

        <div className="lp-index" role="list">
          {CAPABILITIES.map((cap) => (
            <article key={cap.num} className="lp-index__row" role="listitem">
              <span className="lp-index__num">{cap.num}</span>
              <div>
                <h3 className="lp-index__title">{cap.title}</h3>
                <p className="lp-index__desc">{cap.desc}</p>
                <div className="lp-index__taglist" aria-label={`Aspects of ${cap.title}`}>
                  {cap.tags.map((t) => (
                    <span key={t} className="lp-index__tag">{t}</span>
                  ))}
                </div>
              </div>
              <div className="lp-index__meta">
                <b>{cap.meta}</b>
                <span className="lp-index__meta-line">{cap.note}</span>
              </div>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}
