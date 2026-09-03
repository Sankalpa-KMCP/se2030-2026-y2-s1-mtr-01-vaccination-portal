import React from 'react';

const PRINCIPLES = [
  {
    num: '01',
    tag: 'Privacy',
    title: 'Your data is protected',
    desc: 'Citizens see only their own record. Personal information is stored securely.',
  },
  {
    num: '02',
    tag: 'Records',
    title: 'Every change is recorded',
    desc: 'Campaign, staffing, and session changes keep a record of what changed and who made the change.',
  },
  {
    num: '03',
    tag: 'Safety',
    title: 'Clinics are checked before opening',
    desc: 'A session opens only once its hours, stations, and qualified staff are confirmed.',
  },
  {
    num: '04',
    tag: 'Fairness',
    title: 'Capacity is planned openly',
    desc: 'Capacity follows the rooms, stations, and staff actually available — never guessed.',
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
