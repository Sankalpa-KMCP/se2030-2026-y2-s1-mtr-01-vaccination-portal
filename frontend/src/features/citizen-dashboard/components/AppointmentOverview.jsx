import React from 'react';
import { Link } from 'react-router-dom';
import DashboardIcon from './DashboardIcon';
import {
  findActiveAppointment,
  formatRecordedDate,
  formatStatus,
} from '../utils/citizenDashboardState';

function AppointmentLinks({ showBooking = false }) {
  return (
    <div className="citizen-panel__links">
      <Link to="/app/appointments/mine">View My Appointments <DashboardIcon name="arrow" size={16} /></Link>
      {showBooking && <Link to="/app/appointments/book">Book an appointment</Link>}
    </div>
  );
}

export default function AppointmentOverview({ resource }) {
  const activeAppointment = findActiveAppointment(resource.data);

  return (
    <section className="citizen-panel" aria-labelledby="citizen-appointments-title" aria-live="polite">
      <header className="citizen-panel__header">
        <span className="citizen-panel__icon"><DashboardIcon name="calendar" /></span>
        <div>
          <p className="citizen-dashboard__eyebrow">Appointments</p>
          <h2 id="citizen-appointments-title">Appointment status</h2>
        </div>
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
          <AppointmentLinks />
        </div>
      )}

      {!resource.loading && !resource.error && resource.data.length === 0 && (
        <div className="citizen-panel__state">
          <p><strong>No appointments recorded</strong><span>When you make a booking, its supported status details will appear here.</span></p>
          <AppointmentLinks showBooking />
        </div>
      )}

      {!resource.loading && !resource.error && resource.data.length > 0 && activeAppointment && (
        <article className="citizen-record">
          <div className="citizen-record__heading">
            <div>
              <p className="citizen-record__label">Active appointment</p>
              <h3>Appointment #{activeAppointment.id}</h3>
            </div>
            <span className="citizen-status citizen-status--positive">{formatStatus(activeAppointment.status)}</span>
          </div>
          <dl className="citizen-record__details">
            <div><dt>Campaign reference</dt><dd>#{activeAppointment.campaignId}</dd></div>
            <div><dt>Slot reference</dt><dd>#{activeAppointment.slotId}</dd></div>
            <div><dt>Booked</dt><dd>{formatRecordedDate(activeAppointment.bookedAt)}</dd></div>
          </dl>
          <p className="citizen-panel__note">Visit time and center details are available only when supported by appointment records.</p>
          <AppointmentLinks />
        </article>
      )}

      {!resource.loading && !resource.error && resource.data.length > 0 && !activeAppointment && (
        <div className="citizen-panel__state">
          <p><strong>No active appointment</strong><span>Your recorded appointment history is still available.</span></p>
          <AppointmentLinks showBooking />
        </div>
      )}
    </section>
  );
}
