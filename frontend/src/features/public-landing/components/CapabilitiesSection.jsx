import React from 'react';

const CAPABILITIES = [
  {
    num: '01',
    title: 'Vaccination Campaign Management',
    desc: 'Set up campaigns, configure dose programmes and target locations, and guide each campaign through its operational lifecycle.',
  },
  {
    num: '02',
    title: 'Vaccination Center Operations',
    desc: 'Maintain center staffing rosters, operating hours, and operational resources including stations and physical equipment.',
  },
  {
    num: '03',
    title: 'Session Planning & Scheduling',
    desc: 'Create single or recurring vaccination sessions and manage schedule adjustments with formal records.',
  },
  {
    num: '04',
    title: 'Operational Capacity & Readiness',
    desc: 'Plan session throughput capacity in advance and verify operational readiness before sessions open.',
  },
  {
    num: '05',
    title: 'Secure Role-Based Access',
    desc: 'Ensure team members sign in through authenticated accounts with permissions shaped by assigned roles.',
  },
];

export default function CapabilitiesSection() {
  return (
    <section className="lp-section" id="capabilities" aria-labelledby="lp-capabilities-title">
      <div className="lp-container">
        <p className="lp-section__eyebrow">Capabilities</p>
        <h2 className="lp-section__title" id="lp-capabilities-title">
          Portal Capabilities
        </h2>
        <p className="lp-section__lead">
          Core tools for coordinating vaccination campaigns, facilities, and daily sessions.
        </p>

        <div className="lp-capabilities-list" role="list">
          {CAPABILITIES.map((item) => (
            <div key={item.num} className="lp-capability-item" role="listitem">
              <span className="lp-capability-item__num">{item.num}</span>
              <div>
                <h3 className="lp-capability-item__title">{item.title}</h3>
                <p className="lp-capability-item__desc">{item.desc}</p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
