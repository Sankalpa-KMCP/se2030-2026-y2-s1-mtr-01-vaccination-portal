import React from 'react';

const STAGES = [
  {
    stageNum: 'STAGE 01',
    phase: 'Identity & Access',
    title: 'Sign in securely',
    desc: 'Each team member signs in through an authenticated account associated with their assigned clinical, administrative, or operational role.',
    points: ['Role-scoped permissions', 'Verified session context'],
  },
  {
    stageNum: 'STAGE 02',
    phase: 'Programme & Facility Setup',
    title: 'Configure campaigns and centers',
    desc: 'Define campaign parameters, target locations, and dose programmes, alongside center staffing rosters, operating hours, and physical stations.',
    points: ['Dose schedules & target areas', 'Staffing rosters & station allocation'],
  },
  {
    stageNum: 'STAGE 03',
    phase: 'Planning & Execution',
    title: 'Plan and operate vaccination sessions',
    desc: 'Schedule single or recurring sessions, plan throughput capacity, execute pre-session readiness verification, and manage recorded schedule revisions.',
    points: ['Throughput & recurrence rules', 'Operational readiness checks'],
  },
];

export default function HowItWorksSection() {
  return (
    <section className="lp-section lp-section--subtle lp-workflow" id="how-it-works" aria-labelledby="lp-workflow-title">
      <div className="lp-container">
        <div className="lp-section__header">
          <div className="lp-section__eyebrow-row">
            <span className="lp-section__eyebrow">Workflow</span>
            <span className="lp-section__eyebrow-line" aria-hidden="true" />
          </div>
          <h2 className="lp-section__title" id="lp-workflow-title">
            How programme operations work
          </h2>
          <p className="lp-section__lead">
            A structured operational sequence from user authentication to verified session readiness.
          </p>
        </div>

        <ol className="lp-workflow-grid">
          {STAGES.map((stage) => (
            <li key={stage.stageNum} className="lp-workflow-col">
              <div className="lp-workflow-col__top">
                <span className="lp-workflow-col__stage">{stage.stageNum}</span>
                <span className="lp-workflow-col__phase">{stage.phase}</span>
              </div>
              <h3 className="lp-workflow-col__title">{stage.title}</h3>
              <p className="lp-workflow-col__desc">{stage.desc}</p>
              <ul className="lp-workflow-col__points" aria-label={`Deliverables for ${stage.title}`}>
                {stage.points.map((pt) => (
                  <li key={pt} className="lp-workflow-col__point">
                    <span className="lp-workflow-col__bullet" aria-hidden="true" />
                    <span>{pt}</span>
                  </li>
                ))}
              </ul>
            </li>
          ))}
        </ol>
      </div>
    </section>
  );
}
