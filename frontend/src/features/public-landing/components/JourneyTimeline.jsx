import React from 'react';

/**
 * Editorial journey timeline — the design centerpiece of /how-it-works.
 * Oversized serif numerals ride a vertical spine; icons embed in the line.
 * Spine and nodes re-animate when the journey switches (key remount).
 */
function StepIcon({ name }) {
  const paths = {
    account: (
      <>
        <circle cx="12" cy="7.5" r="3.4" stroke="currentColor" strokeWidth="1.6" />
        <path d="M5.5 19.5c.8-3.2 3.4-5 6.5-5s5.7 1.8 6.5 5" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
      </>
    ),
    screening: (
      <>
        <rect x="5" y="4" width="14" height="16" rx="2.2" stroke="currentColor" strokeWidth="1.6" />
        <path d="M8.5 8.5h7M8.5 12h7M8.5 15.5h4" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
      </>
    ),
    booking: (
      <>
        <rect x="4" y="5.5" width="16" height="15" rx="2.2" stroke="currentColor" strokeWidth="1.6" />
        <path d="M8 3v4M16 3v4M4 10.5h16" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
        <path d="M9 15l2 2 4-4" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
      </>
    ),
    clinic: (
      <>
        <path d="M4.5 20.5v-10L12 4.5l7.5 6v10" stroke="currentColor" strokeWidth="1.6" strokeLinejoin="round" />
        <path d="M12 10.5v4.5M9.75 12.75h4.5" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
      </>
    ),
    record: (
      <>
        <path d="M6 3.5h9l4 4V19a1.8 1.8 0 0 1-1.8 1.8H6A1.8 1.8 0 0 1 4.2 19V5.3A1.8 1.8 0 0 1 6 3.5Z" stroke="currentColor" strokeWidth="1.6" strokeLinejoin="round" />
        <path d="M8.5 12h7M8.5 15.5h5" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
      </>
    ),
    request: (
      <>
        <circle cx="10.5" cy="10.5" r="6.5" stroke="currentColor" strokeWidth="1.6" />
        <path d="M15.5 15.5 20 20" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
        <path d="M10.5 7.8v5.4M7.8 10.5h5.4" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
      </>
    ),
    review: (
      <>
        <path d="M12 3 5.5 5.5v4.8c0 4.2 2.7 8 6.5 9.2 3.8-1.2 6.5-5 6.5-9.2V5.5L12 3Z" stroke="currentColor" strokeWidth="1.6" strokeLinejoin="round" />
        <path d="M9 12.2l2.1 2.1L15.5 10" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
      </>
    ),
    planning: (
      <>
        <circle cx="12" cy="12" r="8.5" stroke="currentColor" strokeWidth="1.6" />
        <circle cx="12" cy="12" r="4.5" stroke="currentColor" strokeWidth="1.6" />
        <circle cx="12" cy="12" r="1.2" fill="currentColor" />
      </>
    ),
    sessions: (
      <>
        <circle cx="12" cy="12" r="8.5" stroke="currentColor" strokeWidth="1.6" />
        <path d="M12 7.5V12l3 2" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
      </>
    ),
    readiness: (
      <>
        <path d="M4.5 20.5v-10L12 4.5l7.5 6v10" stroke="currentColor" strokeWidth="1.6" strokeLinejoin="round" />
        <path d="M9 14.5l2.2 2.2 4-4.5" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
      </>
    ),
  };

  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      {paths[name]}
    </svg>
  );
}

export default function JourneyTimeline({ steps, ariaLabel }) {
  return (
    <ol className="lp-timeline" aria-label={ariaLabel}>
      {steps.map((step, index) => (
        <li
          key={step.num}
          className="lp-timeline__item"
          style={{ '--lp-step-i': index }}
        >
          <div className="lp-timeline__marker" aria-hidden="true">
            <span className="lp-timeline__node">
              <StepIcon name={step.icon} />
            </span>
          </div>

          <div className="lp-timeline__body">
            <div className="lp-timeline__num-row">
              <span className="lp-timeline__num" aria-hidden="true">{step.num}</span>
              <span className="lp-timeline__phase">{step.phase}</span>
            </div>
            <h3 className="lp-timeline__title">{step.title}</h3>
            <p className="lp-timeline__desc">{step.desc}</p>
          </div>

          <div className="lp-timeline__rail" aria-hidden="true">
            <span className="lp-timeline__rail-num">{step.num}</span>
          </div>
        </li>
      ))}
    </ol>
  );
}
