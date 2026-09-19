import React from 'react';
import { Link } from 'react-router-dom';
import DashboardIcon from './DashboardIcon';
import { formatRecordedDate, formatStatus } from '../utils/citizenDashboardState';

export default function RecentActivity({ dashboardData }) {
  const { appointments, screening } = dashboardData || {};
  const submissions = Array.isArray(screening?.submissions) ? screening.submissions : [];
  const assessments = Array.isArray(screening?.assessments) ? screening.assessments : [];
  const appointmentList = Array.isArray(appointments?.data) ? appointments.data : [];

  // Build real activity events from existing data
  const events = [];

  submissions.forEach((sub) => {
    events.push({
      id: `sub-${sub.id}`,
      title: 'Screening submitted',
      detail: `Campaign #${sub.campaignId}`,
      timestamp: sub.submittedAt || sub.createdAt,
      icon: 'clipboard',
      badgeTone: 'positive',
    });
  });

  assessments.forEach((ass) => {
    events.push({
      id: `ass-${ass.id || ass.submissionId || Math.random()}`,
      title: `Eligibility recorded: ${formatStatus(ass.status)}`,
      detail: ass.validUntil ? `Valid until ${formatRecordedDate(ass.validUntil)}` : 'Assessment complete',
      timestamp: ass.reviewedAt || ass.createdAt,
      icon: 'check',
      badgeTone: ass.status === 'ELIGIBLE' ? 'positive' : 'attention',
    });
  });

  appointmentList.forEach((apt) => {
    events.push({
      id: `apt-${apt.id}`,
      title: `Appointment booked #${apt.id}`,
      detail: `Status: ${formatStatus(apt.status)}`,
      timestamp: apt.bookedAt,
      icon: 'calendar',
      badgeTone: apt.status === 'CONFIRMED' ? 'positive' : 'neutral',
    });
  });

  // Sort by timestamp descending
  events.sort((a, b) => {
    const timeA = Date.parse(a.timestamp) || 0;
    const timeB = Date.parse(b.timestamp) || 0;
    return timeB - timeA;
  });

  const displayEvents = events.slice(0, 3);

  return (
    <section className="citizen-recent-activity citizen-panel" aria-labelledby="recent-activity-title">
      <header className="citizen-panel__header">
        <span className="citizen-panel__icon">
          <DashboardIcon name="history" />
        </span>
        <div>
          <p className="citizen-dashboard__eyebrow">Recent Activity</p>
          <h2 id="recent-activity-title">Your latest updates</h2>
        </div>
        <Link to="/app/screening/history" className="citizen-recent-activity__header-link">
          View History <DashboardIcon name="arrow" size={14} />
        </Link>
      </header>

      <div className="citizen-recent-activity__body">
        {displayEvents.length === 0 ? (
          <div className="citizen-panel__state">
            <p>
              <strong>No recent activity recorded</strong>
              <span>Your completed screening and appointment actions will appear here.</span>
            </p>
          </div>
        ) : (
          <ul className="citizen-recent-activity__list">
            {displayEvents.map((evt) => (
              <li key={evt.id} className="citizen-recent-activity__item">
                <span className={`citizen-recent-activity__marker citizen-recent-activity__marker--${evt.badgeTone}`} aria-hidden="true">
                  <DashboardIcon name={evt.icon} size={15} />
                </span>
                <div className="citizen-recent-activity__content">
                  <strong>{evt.title}</strong>
                  <small>{evt.detail}</small>
                </div>
                {evt.timestamp && (
                  <time className="citizen-recent-activity__date">
                    {formatRecordedDate(evt.timestamp)}
                  </time>
                )}
              </li>
            ))}
          </ul>
        )}
      </div>
    </section>
  );
}
