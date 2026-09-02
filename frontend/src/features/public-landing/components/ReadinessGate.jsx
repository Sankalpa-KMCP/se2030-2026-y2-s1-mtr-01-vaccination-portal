import React from 'react';

/**
 * Readiness Gate — a system-validation mechanism in visual form.
 * Five real domain checks converge, via converging rules, into a single
 * verdict: READY TO OPEN. Purely presentational (the portal computes
 * readiness server-side); animated as a sequence when revealed.
 */
const CHECKS = [
  {
    id: 'hours',
    label: 'Operating hours',
    detail: 'The session falls inside the center\u2019s published operating hours.',
    icon: (
      <>
        <circle cx="12" cy="12" r="8.5" stroke="currentColor" strokeWidth="1.6" />
        <path d="M12 7.5V12l3 2" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
      </>
    ),
  },
  {
    id: 'stations',
    label: 'Active stations',
    detail: 'Vaccination stations and rooms are assigned and operational.',
    icon: (
      <>
        <path d="M4.5 20.5v-10L12 4.5l7.5 6v10" stroke="currentColor" strokeWidth="1.6" strokeLinejoin="round" />
        <path d="M12 10.5v4.5M9.75 12.75h4.5" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
      </>
    ),
  },
  {
    id: 'capacity',
    label: 'Planned capacity',
    detail: 'Daily throughput is planned from the stations actually available.',
    icon: (
      <>
        <rect x="4" y="5.5" width="16" height="15" rx="2.2" stroke="currentColor" strokeWidth="1.6" />
        <path d="M8 3v4M16 3v4M4 10.5h16" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
        <path d="M9 15.5h6" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
      </>
    ),
  },
  {
    id: 'staff',
    label: 'Qualified staff',
    detail: 'A qualified staff roster is confirmed for the session.',
    icon: (
      <>
        <circle cx="9.5" cy="7.5" r="2.8" stroke="currentColor" strokeWidth="1.6" />
        <path d="M4.5 19.5v-2a4.5 4.5 0 0 1 4.5-4.5h1" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
        <circle cx="16.5" cy="8.5" r="2.4" stroke="currentColor" strokeWidth="1.6" />
        <path d="M14.5 13.4a4.4 4.4 0 0 1 6 4.1v2" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
      </>
    ),
  },
  {
    id: 'conflicts',
    label: 'No schedule conflicts',
    detail: 'No overlapping closure or conflicting session blocks the schedule.',
    icon: (
      <>
        <rect x="4.5" y="4" width="12" height="16" rx="2" stroke="currentColor" strokeWidth="1.6" />
        <path d="M12 8.5l4.5 7h-9L12 8.5Z" stroke="currentColor" strokeWidth="1.6" strokeLinejoin="round" />
      </>
    ),
  },
];

function GateCheckRow({ check, index }) {
  return (
    <div
      className="lp-gate__check"
      data-reveal
      style={{ '--reveal-delay': `${index * 110}ms` }}
    >
      <span className="lp-gate__check-index" aria-hidden="true">
        {String(index + 1).padStart(2, '0')}
      </span>
      <span className="lp-gate__check-icon" aria-hidden="true">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
          {check.icon}
        </svg>
      </span>
      <div className="lp-gate__check-body">
        <span className="lp-gate__check-label">{check.label}</span>
        <span className="lp-gate__check-detail">{check.detail}</span>
      </div>
      <span className="lp-gate__check-state" aria-hidden="true">OK</span>
    </div>
  );
}

export default function ReadinessGate() {
  return (
    <div className="lp-gate" data-reveal>
      <div className="lp-gate__checks">
        <div className="lp-gate__checks-head">
          <span className="lp-gate__head-label">Pre-open validation</span>
          <span className="lp-gate__head-note">All five conditions must pass</span>
        </div>
        {CHECKS.map((check, index) => (
          <GateCheckRow key={check.id} check={check} index={index} />
        ))}
      </div>

      {/* Convergence — a fan of wires merging into one point. */}
      <div className="lp-gate__convergence" aria-hidden="true">
        <span className="lp-gate__wire" />
        <span className="lp-gate__merge-point" />
      </div>

      <div className="lp-gate__verdict">
        <span className="lp-gate__verdict-kicker">Session status</span>
        <span className="lp-gate__verdict-label">Ready to open</span>
        <span className="lp-gate__verdict-note">
          The portal computes every check before a session can accept bookings.
        </span>
      </div>
    </div>
  );
}
