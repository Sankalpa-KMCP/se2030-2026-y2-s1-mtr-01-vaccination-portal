import React from 'react';
import { Link } from 'react-router-dom';
import DashboardIcon from './DashboardIcon';

const ACTIONS = [
  { title: 'Start or view screening', description: 'Choose a campaign and open its screening flow.', to: '/app/screening', icon: 'clipboard' },
  { title: 'My Eligibility', description: 'Review screening progress and recorded decisions.', to: '/app/screening/eligibility', icon: 'check' },
  { title: 'Book Appointment', description: 'Search currently available vaccination sessions.', to: '/app/appointments/book', icon: 'calendar' },
  { title: 'My Appointments', description: 'View and manage your recorded appointments.', to: '/app/appointments/mine', icon: 'ticket' },
  { title: 'Screening History', description: 'Return to previously submitted screening records.', to: '/app/screening/history', icon: 'history' },
  { title: 'Waitlist', description: 'Check or manage a campaign-specific waitlist entry.', to: '/app/appointments/waitlist', icon: 'user' },
];

export default function QuickActions() {
  return (
    <section className="citizen-services" aria-labelledby="citizen-services-title">
      <div className="citizen-section-heading">
        <div>
          <p className="citizen-dashboard__eyebrow">Citizen services</p>
          <h2 id="citizen-services-title">Go directly to a service</h2>
        </div>
        <p>Every action below opens an implemented citizen route.</p>
      </div>
      <nav aria-label="Citizen service shortcuts">
        <ul className="citizen-services__grid">
          {ACTIONS.map((action) => (
            <li key={action.to}>
              <Link className="citizen-service-card" to={action.to}>
                <span className="citizen-service-card__icon"><DashboardIcon name={action.icon} /></span>
                <span className="citizen-service-card__copy">
                  <strong>{action.title}</strong>
                  <small>{action.description}</small>
                </span>
                <span className="citizen-service-card__arrow"><DashboardIcon name="arrow" size={18} /></span>
              </Link>
            </li>
          ))}
        </ul>
      </nav>
    </section>
  );
}
