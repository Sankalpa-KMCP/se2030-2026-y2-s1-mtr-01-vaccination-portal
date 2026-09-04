import React from 'react';
import { NavLink } from 'react-router-dom';

const GUIDE_ITEMS = [
  {
    number: '01',
    to: '/',
    label: 'Overview',
    title: 'Start with the service',
    description: 'See what citizens and health teams can do in the portal.',
    end: true,
  },
  {
    number: '02',
    to: '/how-it-works',
    label: 'Process',
    title: 'Follow the care journey',
    description: 'Understand each step from registration to a recorded dose.',
    end: false,
  },
  {
    number: '03',
    to: '/about',
    label: 'Governance',
    title: 'Review roles and safeguards',
    description: 'Learn how access, readiness, and records are controlled.',
    end: false,
  },
];

/**
 * Shared orientation for the complete public experience. Its fixed position
 * after each hero gives visitors a predictable map of the three-page service.
 */
export default function PublicServiceGuide() {
  return (
    <section className="lp-guide" aria-labelledby="lp-guide-title">
      <div className="lp-container lp-guide__layout">
        <div className="lp-guide__intro">
          <span className="lp-guide__eyebrow">Public service guide</span>
          <h2 id="lp-guide-title">Explore the portal with confidence.</h2>
          <p>Three short pages explain the service, the process, and the safeguards behind it.</p>
        </div>

        <nav className="lp-guide__nav" aria-label="Public service guide">
          <ol className="lp-guide__list">
            {GUIDE_ITEMS.map((item) => (
              <li key={item.to}>
                <NavLink
                  to={item.to}
                  end={item.end}
                  className={({ isActive }) => `lp-guide__link${isActive ? ' lp-is-active' : ''}`}
                >
                  {({ isActive }) => (
                    <>
                      <span className="lp-guide__number" aria-hidden="true">{item.number}</span>
                      <span className="lp-guide__copy">
                        <small>{item.label}</small>
                        <strong>{item.title}</strong>
                        <span>{item.description}</span>
                      </span>
                      <span className="lp-guide__state" aria-hidden="true">
                        {isActive ? 'Current' : 'View'}
                        <i>{isActive ? '•' : '→'}</i>
                      </span>
                    </>
                  )}
                </NavLink>
              </li>
            ))}
          </ol>
        </nav>
      </div>
    </section>
  );
}
