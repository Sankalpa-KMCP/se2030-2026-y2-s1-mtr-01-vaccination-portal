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
          <span className="home-section-mark">Operational integrity</span>
          <h2 id="lp-trust-title">Trust is built into the work, not added as a claim.</h2>
          <p>
            The portal makes responsibility visible at the points where access,
            capacity, eligibility, and records are decided.
          </p>
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
