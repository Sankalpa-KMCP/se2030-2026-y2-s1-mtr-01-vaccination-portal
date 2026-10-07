import React from 'react';
import { Link } from 'react-router-dom';
import DashboardIcon from './DashboardIcon';

const ACTIONS = [
  {
    title: 'Start Screening',
    accessibleName: 'Start or view screening',
    description: 'Check your eligibility and start questionnaire.',
    to: '/app/screening',
    icon: 'clipboard',
  },
  {
    title: 'My Eligibility',
    accessibleName: 'My Eligibility',
    description: 'View clinical assessment and campaign eligibility.',
    to: '/app/screening/eligibility',
    icon: 'check',
  },
  {
    title: 'Book Appointment',
    accessibleName: 'Book Appointment',
    description: 'Find and book an available vaccination slot.',
    to: '/app/appointments/book',
    icon: 'calendar',
  },
  {
    title: 'My Appointments',
    accessibleName: 'My Appointments',
    description: 'View, reschedule, or cancel recorded bookings.',
    to: '/app/appointments/mine',
    icon: 'ticket',
  },
  {
    title: 'Screening History',
    accessibleName: 'Screening History',
    description: 'Review your previously submitted questionnaires.',
    to: '/app/screening/history',
    icon: 'history',
  },
  {
    title: 'Waitlist',
    accessibleName: 'Waitlist',
    description: 'Manage campaign session waitlist requests.',
    to: '/app/appointments/waitlist',
    icon: 'user',
  },
];

export default function QuickActions() {
  return (
    <section className="citizen-services citizen-panel" aria-labelledby="citizen-services-title">
      <header className="citizen-section-heading">
        <div className="citizen-section-heading__title-group">
          <span className="citizen-section-heading__icon" aria-hidden="true">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
              <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />
            </svg>
          </span>
          <div>
            <h2 id="citizen-services-title">Quick Actions</h2>
            <p className="citizen-section-heading__subtitle">Everything you need, in one place.</p>
          </div>
        </div>
      </header>

      <nav aria-label="Citizen service shortcuts">
        <ul className="citizen-services__grid">
          {ACTIONS.map((action) => (
            <li key={action.to}>
              <Link
                className="citizen-service-card"
                to={action.to}
                aria-label={action.accessibleName}
              >
                <span className="citizen-service-card__icon" aria-hidden="true">
                  <DashboardIcon name={action.icon} size={18} />
                </span>
                <span className="citizen-service-card__copy">
                  <strong>{action.title}</strong>
                  <small>{action.description}</small>
                </span>
                <span className="citizen-service-card__arrow" aria-hidden="true">
                  <DashboardIcon name="arrow" size={14} />
                </span>
              </Link>
            </li>
          ))}
        </ul>
      </nav>
    </section>
  );
}
