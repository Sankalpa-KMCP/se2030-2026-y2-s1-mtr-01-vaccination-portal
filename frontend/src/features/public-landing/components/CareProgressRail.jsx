import React from 'react';

const CARE_STEPS = [
  { number: '01', label: 'Access', href: '#portal-access' },
  { number: '02', label: 'System', href: '#capabilities' },
  { number: '03', label: 'Journey', href: '#how-it-works' },
  { number: '04', label: 'Assurance', href: '#accountability' },
  { number: '05', label: 'Begin', href: '#get-started' },
];

export default function CareProgressRail() {
  return (
    <nav className="home-care-rail" aria-label="Care path progress">
      <span className="home-care-rail__label" aria-hidden="true">
        Care path
      </span>
      <div className="home-care-rail__track" aria-hidden="true">
        <span />
      </div>
      <ol>
        {CARE_STEPS.map((step, index) => (
          <li key={step.number}>
            <a
              href={step.href}
              data-care-marker={index + 1}
              aria-label={`${step.number} ${step.label}`}
            >
              <span aria-hidden="true">{step.number}</span>
            </a>
          </li>
        ))}
      </ol>
    </nav>
  );
}
