import React, { useState, useEffect } from 'react';
import { getSessionById, formatInTimezone } from '../api/sessionApi';
import SessionAssignmentsTab from './SessionAssignmentsTab';
import SessionCapacityPlanTab from './SessionCapacityPlanTab';
import ApiErrorAlert from '../../../components/common/ApiErrorAlert';
import LoadingSpinner from '../../../components/common/LoadingSpinner';

export default function SessionDetailsModal({
  isOpen,
  onClose,
  sessionId,
  campaignsMap = {},
  centersMap = {},
}) {
  const [session, setSession] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState('overview');

  useEffect(() => {
    if (!isOpen || !sessionId) {
      setSession(null);
      setLoading(false);
      setError(null);
      setActiveTab('overview');
      return;
    }

    setLoading(true);
    setError(null);
    setActiveTab('overview');

    getSessionById(sessionId)
      .then((data) => {
        setSession(data);
        setLoading(false);
      })
      .catch((err) => {
        setError(err);
        setLoading(false);
      });
  }, [isOpen, sessionId]);

  if (!isOpen) return null;

  const campaign = session ? campaignsMap[session.campaignId] : null;
  const center = session ? centersMap[session.centerId] : null;
  const tz = session?.scheduleTimezone || 'UTC';

  const getStatusBadge = (status) => {
    switch (status) {
      case 'DRAFT':
        return <span className="badge bg-secondary text-white">DRAFT</span>;
      case 'SCHEDULED':
        return <span className="badge bg-primary text-white">SCHEDULED</span>;
      case 'IN_PROGRESS':
        return <span className="badge bg-success text-white">IN_PROGRESS</span>;
      case 'COMPLETED':
        return <span className="badge bg-dark text-white">COMPLETED</span>;
      case 'CANCELLED':
        return <span className="badge bg-danger text-white">CANCELLED</span>;
      default:
        return <span className="badge bg-light text-dark border">{status || 'UNKNOWN'}</span>;
    }
  };

  return (
    <div
      className="modal fade show d-block"
      tabIndex="-1"
      role="dialog"
      aria-labelledby="sessionDetailsModalTitle"
      style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}
    >
      <div className="modal-dialog modal-lg modal-dialog-centered" role="document">
        <div className="modal-content border-0 shadow">
          <div className="modal-header bg-light border-bottom">
            <div className="d-flex align-items-center gap-2">
              <h5 className="modal-title h5 mb-0 text-dark fw-bold" id="sessionDetailsModalTitle">
                Vaccination Session #{sessionId}
              </h5>
              {session && getStatusBadge(session.lifecycleStatus)}
            </div>
            <button
              type="button"
              className="btn-close"
              aria-label="Close"
              onClick={onClose}
            />
          </div>

          {/* Navigation Tabs */}
          {session && (
            <div className="px-4 pt-3 border-bottom bg-light">
              <ul className="nav nav-tabs card-header-tabs" role="tablist">
                <li className="nav-item">
                  <button
                    type="button"
                    className={`nav-link ${activeTab === 'overview' ? 'active fw-bold text-primary' : 'text-secondary'}`}
                    onClick={() => setActiveTab('overview')}
                  >
                    Overview
                  </button>
                </li>
                <li className="nav-item">
                  <button
                    type="button"
                    className={`nav-link ${activeTab === 'assignments' ? 'active fw-bold text-primary' : 'text-secondary'}`}
                    onClick={() => setActiveTab('assignments')}
                  >
                    Operational Assignments
                  </button>
                </li>
                <li className="nav-item">
                  <button
                    type="button"
                    className={`nav-link ${activeTab === 'capacity' ? 'active fw-bold text-primary' : 'text-secondary'}`}
                    onClick={() => setActiveTab('capacity')}
                  >
                    Capacity Plan
                  </button>
                </li>
              </ul>
            </div>
          )}

          <div className="modal-body p-4">
            {error && <ApiErrorAlert error={error} />}

            {loading && <LoadingSpinner message="Loading session details..." />}

            {!loading && session && activeTab === 'overview' && (
              <div className="d-flex flex-column gap-3">
                {/* Campaign & Center Overview Card */}
                <div className="card bg-light border-0 p-3">
                  <div className="row g-3">
                    <div className="col-md-6">
                      <span className="text-muted small d-block">Campaign</span>
                      <strong className="text-dark">
                        {campaign ? `[${campaign.code}] ${campaign.name}` : `Campaign #${session.campaignId}`}
                      </strong>
                    </div>
                    <div className="col-md-6">
                      <span className="text-muted small d-block">Hosting Center</span>
                      <strong className="text-dark">
                        {center ? `[${center.code}] ${center.name}` : `Center #${session.centerId}`}
                      </strong>
                    </div>
                  </div>
                </div>

                {/* Schedule Interval Information */}
                <div className="card border p-3">
                  <h6 className="fw-bold text-primary mb-3">Schedule & Timing</h6>
                  <div className="row g-3">
                    <div className="col-md-4">
                      <span className="text-muted small d-block">Schedule Timezone</span>
                      <code>{session.scheduleTimezone}</code>
                    </div>

                    <div className="col-md-4">
                      <span className="text-muted small d-block">Start Time ({tz})</span>
                      <strong className="text-dark">
                        {formatInTimezone(session.startAt, tz)}
                      </strong>
                      <div className="text-muted small font-monospace" style={{ fontSize: '0.75rem' }}>
                        UTC: {session.startAt}
                      </div>
                    </div>

                    <div className="col-md-4">
                      <span className="text-muted small d-block">End Time ({tz})</span>
                      <strong className="text-dark">
                        {formatInTimezone(session.endAt, tz)}
                      </strong>
                      <div className="text-muted small font-monospace" style={{ fontSize: '0.75rem' }}>
                        UTC: {session.endAt}
                      </div>
                    </div>

                    <div className="col-md-12">
                      <span className="text-muted small d-block">Booking Intake Close Time ({tz})</span>
                      <strong className="text-dark">
                        {formatInTimezone(session.bookingCloseAt, tz)}
                      </strong>
                      <div className="text-muted small font-monospace" style={{ fontSize: '0.75rem' }}>
                        UTC: {session.bookingCloseAt}
                      </div>
                    </div>
                  </div>
                </div>

                {/* Provenance & Recurrence */}
                <div className="card border p-3 bg-light">
                  <h6 className="fw-bold text-secondary mb-2">Provenance & Series Metadata</h6>
                  <div className="row g-2 small">
                    <div className="col-md-4">
                      <span className="text-muted d-block">Recurrence Rule</span>
                      <span>
                        {session.recurrenceRuleId ? `Rule #${session.recurrenceRuleId}` : 'One-Off Session (Manual)'}
                      </span>
                    </div>
                    <div className="col-md-4">
                      <span className="text-muted d-block">Occurrence Key</span>
                      <span className="font-monospace text-dark">
                        {session.occurrenceKey || '—'}
                      </span>
                    </div>
                    <div className="col-md-4">
                      <span className="text-muted d-block">Origin Occurrence Date</span>
                      <span>{session.originOccurrenceDate || '—'}</span>
                    </div>
                  </div>
                </div>

                {/* Audit Timestamps */}
                <div className="d-flex justify-content-between text-muted small px-1">
                  <span>
                    Created: <code>{session.createdAt ? new Date(session.createdAt).toLocaleString() : '—'}</code>
                  </span>
                  <span>
                    Updated: <code>{session.updatedAt ? new Date(session.updatedAt).toLocaleString() : '—'}</code>
                  </span>
                </div>
              </div>
            )}

            {!loading && session && activeTab === 'assignments' && (
              <SessionAssignmentsTab session={session} />
            )}

            {!loading && session && activeTab === 'capacity' && (
              <SessionCapacityPlanTab session={session} />
            )}
          </div>


          <div className="modal-footer bg-light">
            <button type="button" className="btn btn-secondary" onClick={onClose}>
              Close
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
