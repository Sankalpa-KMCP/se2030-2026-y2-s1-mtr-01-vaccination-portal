import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import WaitlistCard from '../components/WaitlistCard';
import WaitlistPosition from '../components/WaitlistPosition';
import { getWaitlistStatus, leaveWaitlist } from '../api/waitlistApi';
import { useAuth } from '../../../auth/context/AuthContext';

/**
 * Citizen Waitlist Page (US-05).
 * Displays active waitlist queue position, entry details, and withdrawal controls.
 */
export default function WaitlistPage() {
  const { user } = useAuth();
  const citizenId = user?.id;

  const [campaignId, setCampaignId] = useState('8001');
  const [waitlistEntry, setWaitlistEntry] = useState(null);
  const [loading, setLoading] = useState(false);
  const [leaving, setLeaving] = useState(false);
  const [feedback, setFeedback] = useState(null);

  const fetchStatus = useCallback(async (cId, campId) => {
    if (!cId || !campId) return;
    setLoading(true);
    setFeedback(null);

    try {
      const data = await getWaitlistStatus({ citizenId: cId, campaignId: campId });
      setWaitlistEntry(data);
    } catch (err) {
      if (err.status === 404) {
        setWaitlistEntry(null);
        setFeedback(null);
      } else {
        setFeedback({
          type: 'danger',
          title: 'Error Loading Waitlist Status',
          message: err.message || 'Failed to retrieve waitlist status.',
        });
        setWaitlistEntry(null);
      }
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchStatus(citizenId, campaignId);
  }, [citizenId, campaignId, fetchStatus]);

  const handleCampaignSubmit = (e) => {
    e.preventDefault();
    fetchStatus(citizenId, campaignId);
  };

  const handleLeaveWaitlist = async (entryId) => {
    if (!citizenId || !entryId) return;
    setLeaving(true);
    setFeedback(null);

    try {
      await leaveWaitlist(entryId, citizenId);
      setFeedback({
        type: 'success',
        title: 'Left Waitlist',
        message: 'You have successfully withdrawn from the waitlist.',
      });
      await fetchStatus(citizenId, campaignId);
    } catch (err) {
      setFeedback({
        type: 'danger',
        title: 'Failed to Leave Waitlist',
        message: err.message || 'An error occurred while attempting to leave the waitlist.',
      });
    } finally {
      setLeaving(false);
    }
  };

  if (!citizenId) {
    return (
      <div className="alert alert-warning border-0 shadow-sm" role="alert">
        <h4 className="h6 alert-heading fw-bold mb-1">Authentication Required</h4>
        <p className="mb-0">Unable to load waitlist status. Please sign in again.</p>
      </div>
    );
  }

  return (
    <div className="waitlist-page">
      <div className="d-flex align-items-center justify-content-between mb-4">
        <div>
          <h1 className="h3 fw-bold text-primary mb-1">My Waitlist Status</h1>
          <p className="text-muted mb-0">Track your position in line for full vaccination sessions or manage your entry.</p>
        </div>
      </div>

      <div className="card shadow-sm border-0 mb-4">
        <div className="card-body p-3">
          <form onSubmit={handleCampaignSubmit} className="row g-2 align-items-center">
            <div className="col-auto">
              <label htmlFor="campaignIdFilter" className="col-form-label fw-semibold">
                Campaign ID:
              </label>
            </div>
            <div className="col-auto">
              <input
                type="number"
                id="campaignIdFilter"
                className="form-control"
                value={campaignId}
                onChange={(e) => setCampaignId(e.target.value)}
                placeholder="Enter Campaign ID"
                required
              />
            </div>
            <div className="col-auto">
              <button type="submit" className="btn btn-primary fw-semibold" disabled={loading}>
                {loading ? 'Checking...' : 'Check Status'}
              </button>
            </div>
          </form>
        </div>
      </div>

      {feedback && (
        <div
          className={`alert alert-${feedback.type} alert-dismissible fade show border-0 shadow-sm mb-4`}
          role="alert"
        >
          <h4 className="h6 alert-heading fw-bold mb-1">{feedback.title}</h4>
          <p className="mb-0">{feedback.message}</p>
          <button
            type="button"
            className="btn-close"
            aria-label="Close"
            onClick={() => setFeedback(null)}
          ></button>
        </div>
      )}

      {loading && (
        <div className="text-center py-5">
          <div className="spinner-border text-primary" style={{ width: '3rem', height: '3rem' }} role="status">
            <span className="visually-hidden">Loading...</span>
          </div>
          <p className="text-muted mt-2 fw-semibold">Checking waitlist status...</p>
        </div>
      )}

      {!loading && waitlistEntry && (
        <div className="waitlist-content">
          {waitlistEntry.status === 'PROMOTED' && (
            <div
              className="alert alert-success border-0 shadow-sm mb-4 d-flex align-items-center justify-content-between flex-wrap gap-2"
              role="alert"
            >
              <div>
                <h4 className="h6 alert-heading fw-bold mb-1">🎉 You've been promoted!</h4>
                <p className="mb-0">
                  A slot opened up and your appointment has been automatically confirmed. Check My Appointments for your confirmed slot.
                </p>
              </div>
              <Link to="/app/appointments/mine" className="btn btn-success btn-sm fw-bold shadow-sm">
                View My Appointments
              </Link>
            </div>
          )}
          <WaitlistPosition position={waitlistEntry.position} />
          <WaitlistCard
            entry={waitlistEntry}
            onLeave={handleLeaveWaitlist}
            isLeaving={leaving}
          />
        </div>
      )}

      {!loading && !waitlistEntry && (
        <div className="card border-0 shadow-sm text-center p-5">
          <div className="mb-3 text-muted">
            <span className="display-4">📋</span>
          </div>
          <h4 className="h5 fw-bold text-dark mb-2">You're not on any waitlist</h4>
          <p className="text-muted mb-4 mx-auto" style={{ maxWidth: '480px' }}>
            You do not currently hold an active waitlist entry for Campaign #{campaignId}.
            Browse available vaccination sessions to book an open slot or join a waitlist.
          </p>
          <div>
            <Link to="/app/appointments/book" className="btn btn-primary px-4 fw-bold shadow-sm">
              Browse Available Sessions
            </Link>
          </div>
        </div>
      )}
    </div>
  );
}
