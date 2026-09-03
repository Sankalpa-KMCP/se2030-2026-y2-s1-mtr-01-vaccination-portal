import React from 'react';

const PRINCIPLES = [
  {
    num: '01',
    title: 'Access follows responsibility.',
    desc: 'Citizens, health officers, center managers, campaign officers, management users, and administrators enter role-specific areas.',
  },
  {
    num: '02',
    title: 'Sessions are checked before they open.',
    desc: 'Operating hours, staffing, resources, conflicts, and calculated capacity are part of session readiness.',
  },
  {
    num: '03',
    title: 'Operational changes leave history.',
    desc: 'Campaign, schedule, assignment, and certificate lifecycle changes are designed to remain traceable.',
  },
  {
    num: '04',
    title: 'Eligibility remains a clinical decision.',
    desc: 'Screening provides a preliminary result; an authorized healthcare officer records the final campaign-specific assessment.',
  },
];

export default function TrustSection() {
  return (
    <section
      className="home-assurance"
      id="accountability"
      aria-labelledby="lp-trust-title"
    >
      <div className="lp-container home-assurance__layout">
        <div className="home-assurance__intro" data-reveal>
          <span className="home-section-mark">04 / Operational assurance</span>
          <h2 id="lp-trust-title">Trust is built into the work, not added as a claim.</h2>
          <p>
            The portal makes responsibility visible at the points where access,
            capacity, eligibility, and records are decided.
          </p>
          <span className="home-assurance__stamp" aria-hidden="true">
            <svg width="74" height="74" viewBox="0 0 74 74" fill="none">
              <circle cx="37" cy="37" r="35" stroke="currentColor" />
              <path d="M37 17l15 6v11c0 10-6 18-15 22-9-4-15-12-15-22V23l15-6Z" stroke="currentColor" />
              <path d="M29 37l5 5 11-12" stroke="currentColor" strokeWidth="2" />
            </svg>
            <span>Control<br />Evidence<br />History</span>
          </span>
        </div>

        <ol className="home-assurance__register" data-reveal>
          {PRINCIPLES.map((item) => (
            <li key={item.num}>
              <span className="home-assurance__number" aria-hidden="true">{item.num}</span>
              <div>
                <h3>{item.title}</h3>
                <p>{item.desc}</p>
              </div>
            </li>
          ))}
        </ol>
      </div>
    </section>
  );
}
