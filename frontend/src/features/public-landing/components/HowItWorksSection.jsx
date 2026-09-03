import React from 'react';

const STAGES = [
  {
    node: '01',
    phase: 'Phase 1 — Eligibility & Planning',
    title: 'Campaign discovery & clinic planning',
    desc: 'Citizens explore campaigns eligible for their area and age. Health staff define schedules, locations, and quota targets.',
    points: ['Citizens: Find eligible campaigns & centers', 'Staff: Configure campaigns & assign rosters'],
  },
  {
    node: '02',
    phase: 'Phase 2 — Booking & Readiness',
    title: 'Session booking & clinic readiness',
    desc: 'Citizens select available slots with instant confirmation. Clinic teams verify operating hours and stations before opening.',
    points: ['Citizens: Real-time slot booking', 'Staff: Pre-opening checklist validation'],
  },
  {
    node: '03',
    phase: 'Phase 3 — Care & Documentation',
    title: 'Attendance, vaccination & verified records',
    desc: 'Citizens attend their clinic appointment. Clinical staff record dose details, automatically issuing digital records and updating counts.',
    points: ['Citizens: Instant digital records', 'Staff: Auditable clinical dose logging'],
  },
];

function PointIcon() {
  return (
    <svg width="11" height="11" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d="M5 12.5l4.2 4.2L19 7.2" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

export default function HowItWorksSection() {
  return (
    <section
      className="lp-section lp-section--subtle lp-workflow"
      id="how-it-works"
      aria-labelledby="lp-workflow-title"
    >
      <div className="lp-container">
        <div className="lp-section__header lp-section__header--centered" data-reveal>
          <div className="lp-kicker lp-kicker--centered">
            <span className="lp-kicker__index">03</span>
            <span className="lp-kicker__label">How it works</span>
            <span className="lp-kicker__rule" aria-hidden="true" />
          </div>
          <h2 className="lp-section__title" id="lp-workflow-title">
            How programme operations work
          </h2>
          <p className="lp-section__lead">
            Every vaccination appointment connects citizen convenience with clinical
            readiness and accountable record-keeping.
          </p>
        </div>

          {/* Steps with drawing spine */}
          <ol className="lp-workflow__steps" data-reveal>
            {STAGES.map((stage) => (
              <li key={stage.node} className="lp-workflow-col">
                <span className="lp-workflow-col__node" aria-hidden="true">{stage.node}</span>
                <div className="lp-workflow-col__body">
                  <span className="lp-workflow-col__phase">{stage.phase}</span>
                  <h3 className="lp-workflow-col__title">{stage.title}</h3>
                  <p className="lp-workflow-col__desc">{stage.desc}</p>
                  <ul className="lp-workflow-col__points" aria-label={`Points for ${stage.title}`}>
                    {stage.points.map((pt) => (
                      <li key={pt} className="lp-workflow-col__point">
                        <PointIcon />
                        <span>{pt}</span>
                      </li>
                    ))}
                  </ul>
                </div>
              </li>
            ))}
          </ol>
      </div>
    </section>
  );
}
