import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import WaitlistCard from '../components/WaitlistCard';
import WaitlistPosition from '../components/WaitlistPosition';
import { getWaitlistStatus, leaveWaitlist } from '../api/waitlistApi';
import { useAuth } from '../../../auth/context/AuthContext';
import CitizenPageHeader from '../../citizen-ui/components/CitizenPageHeader';
import CitizenStatePanel from '../../citizen-ui/components/CitizenStatePanel';

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
      setFeedback({
        type: 'danger',
        title: 'Error Loading Waitlist Status',
        message: err.message || 'Failed to retrieve waitlist status.',
      });
      setWaitlistEntry(null);
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
      <div className="waitlist-page citizen-page">
        <CitizenPageHeader
          eyebrow="Appointment Services"
          title="My Waitlist Status"
          subtitle="Check or manage your waitlist entry for a vaccination campaign."
          backTo="/app/citizen"
          backLabel="Back to Dashboard"
        />
        <div className="alert alert-warning border-0 shadow-sm citizen-page__notice" role="alert">
          <h2 className="h6 alert-heading fw-bold mb-1">Authentication Required</h2>
          <p className="mb-0">Unable to load waitlist status. Please sign in again.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="waitlist-page citizen-page">
      <CitizenPageHeader
        eyebrow="Appointment Services"
        title="My Waitlist Status"
        subtitle="Check or manage your waitlist entry for a vaccination campaign."
        backTo="/app/citizen"
        backLabel="Back to Dashboard"
      />

      <ol className="citizen-workflow-steps" aria-label="Waitlist steps">
        <li><span>1</span><strong>Enter a campaign reference</strong></li>
        <li><span>2</span><strong>Check your recorded entry</strong></li>
        <li><span>3</span><strong>Keep or leave the waitlist</strong></li>
      </ol>

      <div className="card shadow-sm border-0 citizen-waitlist-filter">
        <div className="card-body p-3">
          <div className="mb-3">
            <p className="citizen-ui-eyebrow">Campaign lookup</p>
            <h2 className="h5 mb-1">Find your waitlist entry</h2>
            <p className="text-muted small mb-0">Waitlist status is checked one campaign at a time.</p>
          </div>
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
        <div className="citizen-page__notice">
          <div
            className={`alert alert-${feedback.type} alert-dismissible fade show border-0 shadow-sm`}
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
        </div>
      )}

      {loading && (
        <CitizenStatePanel title="Checking waitlist status..." tone="loading" status>
          <p>Looking for an entry linked to Campaign #{campaignId}.</p>
        </CitizenStatePanel>
      )}

      {!loading && waitlistEntry && (
        <section className="waitlist-content citizen-waitlist-content" aria-label="Current waitlist entry">
          <WaitlistPosition position={waitlistEntry.position} />
          <WaitlistCard
            entry={waitlistEntry}
            onLeave={handleLeaveWaitlist}
            isLeaving={leaving}
          />
        </section>
      )}

      {!loading && !waitlistEntry && (
        <CitizenStatePanel
          eyebrow="Campaign status"
          title="You're not on any waitlist"
          actions={<Link to="/app/appointments/book" className="citizen-action citizen-action--primary">Browse Available Sessions</Link>}
        >
          <p>
            You do not currently hold an active waitlist entry for Campaign #{campaignId}.
            Browse available vaccination sessions to book an open slot or join a waitlist.
          </p>
        </CitizenStatePanel>
      )}
    </div>
  );
}
