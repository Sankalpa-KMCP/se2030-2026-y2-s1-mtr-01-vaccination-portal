import React from 'react';

const PRINCIPLES = [
  {
    num: '01',
    tag: 'Privacy',
    title: 'Role-isolated data access',
    desc: 'Citizens access only their personal bookings and immunization records. Health staff access only their assigned center operations.',
  },
  {
    num: '02',
    tag: 'Accountability',
    title: 'Auditable operational history',
    desc: 'Campaign configurations, session schedule changes, and staff assignments preserve an immutable log of who made each change.',
  },
  {
    num: '03',
    tag: 'Safety',
    title: 'Pre-session readiness checks',
    desc: 'A clinic session unlocks for booking only after operating hours, station capacity, and certified staff are confirmed ready.',
  },
  {
    num: '04',
    tag: 'Fairness',
    title: 'Transparent capacity planning',
    desc: 'Appointment limits mirror physical rooms and confirmed staffing allocations — preventing overbooking and clinic delays.',
  },
];

export default function TrustSection() {
  return (
    <section
      className="lp-section lp-trust"
      id="accountability"
      aria-labelledby="lp-trust-title"
    >
      <div className="lp-container">
        <div className="lp-trust-layout">
          {/* Commitment statement */}
          <div className="lp-trust__intro" data-reveal>
            <div className="lp-kicker">
              <span className="lp-kicker__index">04</span>
              <span className="lp-kicker__label">Privacy &amp; accountability</span>
              <span className="lp-kicker__rule" aria-hidden="true" />
            </div>
            <h2 className="lp-trust__title" id="lp-trust-title">
              Built for operational <span className="lp-hero__accent">trust</span>
            </h2>

            <blockquote className="lp-trust__quote">
              Health information deserves careful handling &mdash; every part of
              this portal is designed to keep records accurate, private, and
              accountable.
            </blockquote>

            <div className="lp-trust__badge-box">
              <span className="lp-trust__badge">Our commitment</span>
              <p className="lp-trust__badge-text">
                Role-based access and a full record of every operational change,
                by design.
              </p>
            </div>
          </div>

          {/* Principles ledger */}
          <div className="lp-trust-ledger" data-reveal>
            {PRINCIPLES.map((item, index) => (
              <article
                key={item.num}
                className="lp-trust-row"
                style={{ '--reveal-delay': `${index * 80}ms` }}
                data-reveal
              >
                <span className="lp-trust-row__num" aria-hidden="true">{item.num}</span>
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
