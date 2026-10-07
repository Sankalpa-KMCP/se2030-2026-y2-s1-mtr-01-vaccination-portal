import React from 'react';
import { Link } from 'react-router-dom';
import DashboardIcon from './DashboardIcon';
import {
  findActiveAppointment,
  formatRecordedDate,
  formatStatus,
} from '../utils/citizenDashboardState';

export default function AppointmentOverview({ resource }) {
  const activeAppointment = findActiveAppointment(resource.data);

  return (
    <section className="citizen-panel citizen-appointment-card" aria-labelledby="citizen-appointments-title" aria-live="polite">
      <header className="citizen-panel__header">
        <span className="citizen-panel__icon" aria-hidden="true">
          <DashboardIcon name="calendar" size={20} />
        </span>
        <div>
          <p className="citizen-dashboard__eyebrow">Appointments</p>
          <h2 id="citizen-appointments-title">Next Appointment</h2>
        </div>
        <Link to="/app/appointments/mine" className="citizen-appointment-card__header-link">
          View All <DashboardIcon name="arrow" size={13} />
        </Link>
      </header>

      {resource.loading && (
        <div className="citizen-panel__state" role="status">
          <span className="citizen-panel__pulse" aria-hidden="true" />
          <p><strong>Loading your appointments</strong><span>Checking recorded appointment statuses.</span></p>
        </div>
      )}

      {!resource.loading && resource.error && (
        <div className="citizen-panel__state citizen-panel__state--error" role="alert">
          <p><strong>Appointments could not be loaded</strong><span>Your other citizen services are still available.</span></p>
          <div className="citizen-panel__links">
            <Link to="/app/appointments/mine" className="citizen-action-link citizen-action-link--secondary">
              View My Appointments
            </Link>
          </div>
        </div>
      )}

      {!resource.loading && !resource.error && resource.data.length === 0 && (
        <div className="citizen-appointment-card__empty">
          <span className="citizen-appointment-card__empty-icon" aria-hidden="true">
            <DashboardIcon name="calendar" size={28} />
          </span>
          <p className="citizen-appointment-card__empty-title">
            <strong>No appointments recorded</strong>
          </p>
          <p className="citizen-appointment-card__empty-desc">
            Book an appointment when you are ready.
          </p>
          <div className="citizen-appointment-card__empty-actions">
            <Link to="/app/appointments/book" className="citizen-action-link citizen-action-link--primary citizen-appointment-card__cta">
              Book an appointment <DashboardIcon name="arrow" size={15} />
            </Link>
            <Link to="/app/appointments/mine" className="citizen-action-link citizen-action-link--secondary citizen-appointment-card__cta">
              View My Appointments
            </Link>
          </div>
        </div>
      )}

      {!resource.loading && !resource.error && resource.data.length > 0 && activeAppointment && (
        <article className="citizen-record citizen-appointment-card__active">
          <div className="citizen-record__heading">
            <div>
              <p className="citizen-record__label">Active appointment</p>
              <h3>Appointment #{activeAppointment.id}</h3>
            </div>
            <span className="citizen-status citizen-status--positive">
              {formatStatus(activeAppointment.status)}
            </span>
          </div>

          <div className="citizen-appointment-card__items">
            <div className="citizen-appointment-card__item">
              <span className="citizen-appointment-card__item-icon" aria-hidden="true">📅</span>
              <div>
                <small>Booked date</small>
                <strong>{formatRecordedDate(activeAppointment.bookedAt)}</strong>
              </div>
            </div>

            <div className="citizen-appointment-card__item">
              <span className="citizen-appointment-card__item-icon" aria-hidden="true">🏷️</span>
              <div>
                <small>Campaign reference</small>
                <strong>#{activeAppointment.campaignId}</strong>
              </div>
            </div>

            <div className="citizen-appointment-card__item">
              <span className="citizen-appointment-card__item-icon" aria-hidden="true">⏱️</span>
              <div>
                <small>Slot reference</small>
                <strong>#{activeAppointment.slotId}</strong>
              </div>
            </div>
          </div>

          <p className="citizen-panel__note">Visit time and center details are available only when supported by appointment records.</p>

          <div className="citizen-panel__links">
            <Link to="/app/appointments/mine" className="citizen-action-link citizen-action-link--primary citizen-appointment-card__cta">
              View Appointment Details <DashboardIcon name="arrow" size={16} />
            </Link>
          </div>
        </article>
      )}

      {!resource.loading && !resource.error && resource.data.length > 0 && !activeAppointment && (
        <div className="citizen-appointment-card__empty">
          <p><strong>No active appointment</strong><span>Your recorded appointment history is still available.</span></p>
          <div className="citizen-appointment-card__empty-actions">
            <Link to="/app/appointments/book" className="citizen-action-link citizen-action-link--primary">
              Book an appointment
            </Link>
            <Link to="/app/appointments/mine" className="citizen-action-link citizen-action-link--secondary">
              View My Appointments
            </Link>
          </div>
        </div>
      )}
    </section>
  );
}
