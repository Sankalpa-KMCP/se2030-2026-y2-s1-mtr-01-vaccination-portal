import React from 'react';

const ITEMS = [
  {
    title: 'Role-based access',
    desc: 'Operational access is restricted to verified accounts based on explicitly assigned roles.',
  },
  {
    title: 'Recorded operational changes',
    desc: 'Session schedule adjustments and configuration changes are recorded for administrative traceability.',
  },
  {
    title: 'Readiness checks',
    desc: 'Sessions require explicit readiness confirmation across operating hours, stations, and staffing before opening.',
  },
  {
    title: 'Controlled capacity adjustments',
    desc: 'Capacity changes are deliberate, recorded revisions based on available physical stations and throughput.',
  },
];

export default function TrustSection() {
  return (
    <section className="lp-section" id="accountability" aria-labelledby="lp-trust-title">
      <div className="lp-container">
        <p className="lp-section__eyebrow">Accountability</p>
        <h2 className="lp-section__title" id="lp-trust-title">
          Built for Operational Accountability
        </h2>
        <p className="lp-section__lead">
          Key controls and verification safeguards designed into daily portal operations.
        </p>

        <div className="lp-trust-grid" role="list">
          {ITEMS.map((item) => (
            <div key={item.title} className="lp-trust-item" role="listitem">
              <h3 className="lp-trust-item__title">{item.title}</h3>
              <p className="lp-trust-item__desc">{item.desc}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
