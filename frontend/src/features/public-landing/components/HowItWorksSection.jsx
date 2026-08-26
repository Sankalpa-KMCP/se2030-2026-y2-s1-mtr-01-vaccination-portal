import React from 'react';

const STEPS = [
  {
    n: '01',
    kicker: 'Step one',
    title: 'Sign in securely',
    desc: 'Authorized personnel authenticate into their assigned operational role. Permissions are isolated by assignment and enforced on every action.',
    checks: ['JWT session', 'Role boundary', 'Cross-center guard'],
  },
  {
    n: '02',
    kicker: 'Step two',
    title: 'Configure campaigns and centers',
    desc: 'Define dose programmes, target locations and lifecycle states. Set center hours, closures, stations and staffing rosters for delivery.',
    checks: ['Programme intervals', 'Operating hours', 'Staff rosters'],
  },
  {
    n: '03',
    kicker: 'Step three',
    title: 'Plan and operate sessions',
    desc: 'Generate single or recurring sessions, calculate throughput capacity, apply manager-approved overrides and pass readiness diagnostics before opening.',
    checks: ['Capacity planning', 'Recurrence', 'Readiness gate'],
  },
];

export default function HowItWorksSection() {
  return (
    <section className="lp-section lp-section--tint" id="workflow" aria-labelledby="lp-workflow-heading">
      <div className="lp-container">
        <div className="lp-flow">
          <div className="lp-flow__side">
            <p className="lp-kicker">How it works</p>
            <h2 className="lp-h2" id="lp-workflow-heading">From authentication to verified session execution.</h2>
            <p className="lp-lead" style={{ marginTop: 10 }}>
              Every session moves through the same administrative gates — so resourcing, readiness and approvals are verifiable
              rather than assumed.
            </p>
          </div>

          <div className="lp-flow__list" role="list">
            {STEPS.map((s) => (
              <div key={s.n} className="lp-flow__step" role="listitem">
                <span className="lp-flow__n" aria-hidden="true">{s.n}</span>
                <div>
                  <p className="lp-flow__kicker">{s.kicker}</p>
                  <h3 className="lp-flow__title">{s.title}</h3>
                  <p className="lp-flow__desc">{s.desc}</p>
                  <div className="lp-flow__check" aria-label={`Checks for ${s.title}`}>
                    {s.checks.map((c) => (
                      <span key={c}><span className="lp-flow__dot" aria-hidden="true" />{c}</span>
                    ))}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}
