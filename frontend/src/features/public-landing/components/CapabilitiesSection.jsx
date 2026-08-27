import React from 'react';

const PRIMARY_CAPABILITY = {
  num: '01',
  tag: 'Campaign Operations',
  title: 'Vaccination Campaign Management',
  desc: 'Define target populations, configure multi-dose vaccination programmes, set geographic locations, and guide campaigns through their formal operational lifecycle from draft to completion.',
  highlights: ['Dose interval rules', 'Geographic targeting', 'Lifecycle status transitions'],
};

const SECONDARY_CAPABILITIES = [
  {
    num: '02',
    tag: 'Facility Management',
    title: 'Vaccination Center Operations',
    desc: 'Maintain center staffing rosters, operating hours, and operational resources including physical stations, rooms, and equipment.',
    highlights: ['Staff rosters & shifts', 'Physical station allocation'],
  },
  {
    num: '03',
    tag: 'Operational Scheduling',
    title: 'Session Planning & Scheduling',
    desc: 'Create single or recurring vaccination sessions, manage schedule adjustments, and record operational change rationales.',
    highlights: ['Recurrence pattern rules', 'Schedule change records'],
  },
  {
    num: '04',
    tag: 'Capacity & Readiness',
    title: 'Operational Capacity & Readiness',
    desc: 'Plan daily session throughput capacity in advance and verify operational readiness through pre-session checks.',
    highlights: ['Throughput calculation', 'Operational readiness checks'],
  },
  {
    num: '05',
    tag: 'Security & Access',
    title: 'Secure Role-Based Access',
    desc: 'Restrict portal operations to authenticated accounts with functional access shaped by assigned operational roles.',
    highlights: ['Role-scoped permissions', 'Session change records'],
  },
];

export default function CapabilitiesSection() {
  return (
    <section className="lp-section lp-capabilities" id="capabilities" aria-labelledby="lp-capabilities-title">
      <div className="lp-container">
        <div className="lp-section__header">
          <div className="lp-section__eyebrow-row">
            <span className="lp-section__eyebrow">Portal Capabilities</span>
            <span className="lp-section__eyebrow-line" aria-hidden="true" />
          </div>
          <h2 className="lp-section__title" id="lp-capabilities-title">
            One portal for coordinated vaccination programmes
          </h2>
          <p className="lp-section__lead">
            Structured operational tools for planning campaigns, managing center resources, and operating daily vaccination sessions.
          </p>
        </div>

        <div className="lp-cap-matrix" role="list">
          {/* Primary Lead Capability (01) */}
          <article className="lp-cap-card lp-cap-card--primary" role="listitem">
            <div className="lp-cap-card__header">
              <span className="lp-cap-card__num">{PRIMARY_CAPABILITY.num}</span>
              <span className="lp-cap-card__tag">{PRIMARY_CAPABILITY.tag}</span>
            </div>
            <div className="lp-cap-card__content">
              <h3 className="lp-cap-card__title">{PRIMARY_CAPABILITY.title}</h3>
              <p className="lp-cap-card__desc">{PRIMARY_CAPABILITY.desc}</p>
              <div className="lp-cap-card__highlights">
                {PRIMARY_CAPABILITY.highlights.map((h) => (
                  <span key={h} className="lp-cap-card__pill">
                    {h}
                  </span>
                ))}
              </div>
            </div>
          </article>

          {/* Secondary 2x2 Grid (02 - 05) */}
          <div className="lp-cap-grid">
            {SECONDARY_CAPABILITIES.map((cap) => (
              <article key={cap.num} className="lp-cap-card" role="listitem">
                <div className="lp-cap-card__header">
                  <span className="lp-cap-card__num">{cap.num}</span>
                  <span className="lp-cap-card__tag">{cap.tag}</span>
                </div>
                <div className="lp-cap-card__content">
                  <h3 className="lp-cap-card__title">{cap.title}</h3>
                  <p className="lp-cap-card__desc">{cap.desc}</p>
                  <div className="lp-cap-card__highlights">
                    {cap.highlights.map((h) => (
                      <span key={h} className="lp-cap-card__pill">
                        {h}
                      </span>
                    ))}
                  </div>
                </div>
              </article>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}
