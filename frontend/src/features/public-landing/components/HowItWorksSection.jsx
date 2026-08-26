import React from 'react';
import { BuildingIcon, CalendarClockIcon, LockIcon } from './icons';

const STEPS = [
  {
    icon: LockIcon,
    title: 'Sign in securely',
    copy: 'Each team member signs in with an authenticated account tied to their role.',
  },
  {
    icon: BuildingIcon,
    title: 'Configure campaigns and centers',
    copy: 'Define campaign settings, target locations, dose programmes, staffing rosters and center resources.',
  },
  {
    icon: CalendarClockIcon,
    title: 'Plan and operate vaccination sessions',
    copy: 'Schedule recurring sessions, set capacity, confirm readiness and record schedule changes responsibly.',
  },
];

export default function HowItWorksSection() {
  return (
    <section className="lp-section" id="how-it-works" aria-labelledby="lp-workflow-title">
      <div className="lp-container">
        <div className="lp-section-head lp-reveal">
          <p className="lp-eyebrow lp-eyebrow--center">Workflow</p>
          <h2 className="lp-section-title" id="lp-workflow-title">
            How programme operations work
          </h2>
          <p className="lp-section-sub">
            A straightforward path from sign-in to running vaccination sessions.
          </p>
        </div>

        <ol className="lp-steps">
          {STEPS.map((step, index) => {
            const Icon = step.icon;
            return (
              <li key={step.title} className="lp-step lp-reveal">
                <span className="lp-step__marker">{index + 1}</span>
                <span className="lp-step__icon">
                  <Icon width="19" height="19" />
                </span>
                <h3 className="lp-step__title">{step.title}</h3>
                <p className="lp-step__copy">{step.copy}</p>
              </li>
            );
          })}
        </ol>
      </div>
    </section>
  );
}
