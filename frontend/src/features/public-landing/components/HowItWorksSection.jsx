import React from 'react';

const STEPS = [
  {
    num: '1',
    title: 'Sign in securely',
    desc: 'Each team member signs in with an authenticated account tied to their assigned operational role.',
  },
  {
    num: '2',
    title: 'Configure campaigns and centers',
    desc: 'Define campaign parameters, dose programmes, target locations, staffing rosters, and physical center resources.',
  },
  {
    num: '3',
    title: 'Plan and operate vaccination sessions',
    desc: 'Schedule single or recurring sessions, plan throughput capacity, and confirm operational readiness.',
  },
];

export default function HowItWorksSection() {
  return (
    <section className="lp-section lp-section--subtle" id="how-it-works" aria-labelledby="lp-workflow-title">
      <div className="lp-container">
        <p className="lp-section__eyebrow">Workflow</p>
        <h2 className="lp-section__title" id="lp-workflow-title">
          How It Works
        </h2>
        <p className="lp-section__lead">
          A structured sequence from user authentication to session readiness.
        </p>

        <ol className="lp-workflow-list">
          {STEPS.map((step) => (
            <li key={step.num} className="lp-workflow-item">
              <span className="lp-workflow-item__num">{step.num}</span>
              <h3 className="lp-workflow-item__title">{step.title}</h3>
              <p className="lp-workflow-item__desc">{step.desc}</p>
            </li>
          ))}
        </ol>
      </div>
    </section>
  );
}
