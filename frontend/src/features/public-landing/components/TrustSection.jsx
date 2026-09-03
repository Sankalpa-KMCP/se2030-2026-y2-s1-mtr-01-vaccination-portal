import React from 'react';

const PRINCIPLES = [
  {
    num: '01',
    tag: 'Access Control',
    title: 'Role-based access',
    desc: 'Access to portal functions follows each authenticated user’s assigned role, ensuring strict boundaries across authorized operational workflows.',
  },
  {
    num: '02',
    tag: 'Operational History',
    title: 'Recorded operational changes',
    desc: 'Campaign decisions, staffing revisions, and session changes are recorded with administrative metadata so operational actions can be reviewed.',
  },
  {
    num: '03',
    tag: 'Pre-Session Gate',
    title: 'Readiness checks',
    desc: 'Sessions require explicit operational readiness checks across operating hours, active stations, and qualified staff before opening.',
  },
  {
    num: '04',
    tag: 'Capacity Management',
    title: 'Controlled capacity adjustments',
    desc: 'Capacity changes are deliberate, recorded plan revisions based on available physical stations and calculated throughput rather than silent edits.',
  },
];

export default function TrustSection() {
  return (
    <section className="lp-section lp-trust" id="accountability" aria-labelledby="lp-trust-title">
      <div className="lp-container">
        <div className="lp-trust-layout">
          {/* Left Context Column */}
          <div className="lp-trust__intro">
            <div className="lp-section__eyebrow-row">
              <span className="lp-section__eyebrow">Accountability</span>
              <span className="lp-section__eyebrow-line" aria-hidden="true" />
            </div>
            <h2 className="lp-section__title" id="lp-trust-title">
              Built for operational accountability
            </h2>
            <p className="lp-section__lead">
              Every critical action—from capacity planning to session scheduling—is governed by clear role boundaries, operational readiness checks, and traceable operational records.
            </p>
            <div className="lp-trust__badge-box">
              <span className="lp-trust__badge">Operational Accountability</span>
              <p className="lp-trust__badge-text">
                Designed around role-scoped access and recorded operational changes.
              </p>
            </div>
          </div>

          {/* Right Ledger Column */}
          <div className="lp-trust-ledger" role="list" aria-label="Accountability Principles">
            {PRINCIPLES.map((item) => (
              <article key={item.num} className="lp-trust-row" role="listitem">
                <div className="lp-trust-row__num-col">
                  <span className="lp-trust-row__num">{item.num}</span>
                </div>
                <div className="lp-trust-row__body">
                  <div className="lp-trust-row__header">
                    <h3 className="lp-trust-row__title">{item.title}</h3>
                    <span className="lp-trust-row__tag">{item.tag}</span>
                  </div>
                  <p className="lp-trust-row__desc">{item.desc}</p>
                </div>
              </article>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}
