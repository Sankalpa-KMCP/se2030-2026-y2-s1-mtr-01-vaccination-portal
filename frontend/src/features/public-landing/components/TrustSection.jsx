import React from 'react';

const ITEMS = [
  {
    tag: 'Access control',
    title: 'Role-based operational boundaries',
    desc: 'Campaign Officers configure clinical parameters, Center Managers oversee facilities and rosters, and clinical staff execute sessions. Capabilities are bounded by assignment — no privilege bleed across domains.',
  },
  {
    tag: 'Audit trail',
    title: 'Immutable operational logging',
    desc: 'Reschedules, transfers, cancellations and capacity overrides are permanently recorded with actor, timestamp and justification for post-incident review.',
  },
  {
    tag: 'Verification',
    title: 'Point-in-time readiness diagnostics',
    desc: 'Sessions are blocked from opening until operating hours, station minimums, vaccinator assignments, closure conflicts and campaign linkage all verify at that moment.',
  },
  {
    tag: 'Capacity control',
    title: 'Formalized throughput governance',
    desc: 'Capacity is derived from physical stations and dwell times. Any increase above recommended throughput requires explicit manager approval with recorded rationale.',
  },
];

export default function TrustSection() {
  return (
    <section className="lp-section lp-section--paper" id="governance" aria-labelledby="lp-governance-heading">
      <div className="lp-container">
        <div className="lp-gov-intro">
          <p className="lp-kicker">Governance &amp; accountability</p>
          <h2 className="lp-h2" id="lp-governance-heading">Safeguards for clinical accountability.</h2>
          <p className="lp-lead">Operational control is verifiable — every gate, adjustment and override leaves a trace.</p>
        </div>

        <div className="lp-gov-grid" role="list">
          {ITEMS.map((it) => (
            <div key={it.title} className="lp-gov-item" role="listitem">
              <span className="lp-gov-item__tag">{it.tag}</span>
              <h3 className="lp-gov-item__title">{it.title}</h3>
              <p className="lp-gov-item__desc">{it.desc}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
