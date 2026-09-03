import React from 'react';

const STAGES = [
  {
    node: '1',
    phase: 'For citizens',
    title: 'Create your account',
    desc: 'Register with your details once — your account is ready immediately.',
    points: ['Register in minutes', 'Secure and private'],
  },
  {
    node: '2',
    phase: 'For everyone',
    title: 'Book or schedule',
    desc: 'Citizens book a session at a nearby clinic. Staff plan one-off or repeating clinic schedules.',
    points: ['Choose clinic & time', 'Repeating schedules'],
  },
  {
    node: '3',
    phase: 'For everyone',
    title: 'Attend and get your record',
    desc: 'Visit the clinic for your vaccination — records update automatically for citizens and campaigns alike.',
    points: ['Automatic records', 'Campaign progress'],
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
        <div className="lp-workflow-layout">
          {/* Sticky aside */}
          <aside className="lp-workflow-aside">
            <div data-reveal>
              <div className="lp-kicker">
                <span className="lp-kicker__index">03</span>
                <span className="lp-kicker__label">How it works</span>
                <span className="lp-kicker__rule" aria-hidden="true" />
              </div>
              <h2 className="lp-section__title" id="lp-workflow-title">
                How programme operations work
              </h2>
              <p className="lp-section__lead">
                Getting vaccinated &mdash; or running a programme &mdash; takes
                three simple steps.
              </p>
            </div>
          </aside>

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
      </div>
    </section>
  );
}
