import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import WaitlistCard from '../components/WaitlistCard';
import WaitlistPosition from '../components/WaitlistPosition';
import { getWaitlistStatus, leaveWaitlist } from '../api/waitlistApi';
import { useAuth } from '../../../auth/context/AuthContext';
import CitizenErrorState from '../../citizen-ui/components/CitizenErrorState';
import CitizenPageHeader from '../../citizen-ui/components/CitizenPageHeader';
import CitizenStatePanel from '../../citizen-ui/components/CitizenStatePanel';
import CitizenWorkflowGuide from '../../citizen-ui/components/CitizenWorkflowGuide';

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
      <div className="waitlist-page citizen-page">
        <CitizenPageHeader
          eyebrow="Appointment Services"
          title="My Waitlist Status"
          subtitle="Check or manage your waitlist entry for a vaccination campaign."
          backTo="/app/citizen"
          backLabel="Back to Dashboard"
        />
        <CitizenStatePanel title="Authentication required" tone="warning">
          <p>Unable to load waitlist status. Please sign in again.</p>
        </CitizenStatePanel>
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

      <CitizenWorkflowGuide
        titleId="waitlist-workflow-guide"
        steps={['Enter a campaign reference', 'Check your recorded entry', 'Keep or leave the waitlist']}
      />

      <div className="card shadow-sm border-0 citizen-waitlist-filter">
        <div className="card-body p-3">
          <div className="mb-3">
            <p className="citizen-ui-eyebrow">Campaign lookup</p>
            <h2 className="h5 mb-1">Find your waitlist entry</h2>
            <p className="text-muted small mb-0">Waitlist status is checked one campaign at a time.</p>
          </div>
          <form onSubmit={handleCampaignSubmit} className="citizen-waitlist-lookup">
            <label htmlFor="campaignIdFilter" className="citizen-waitlist-lookup__label">
              Campaign ID
            </label>
            <div className="citizen-waitlist-lookup__controls">
              <input
                type="number"
                id="campaignIdFilter"
                className="form-control"
                value={campaignId}
                onChange={(e) => {
                  const val = e.target.value;
                  if (val === '') {
                    setCampaignId('');
                    return;
                  }
                  const cleanVal = val.replace(/[^0-9]/g, '');
                  if (cleanVal === '') {
                    setCampaignId('');
                    return;
                  }
                  const num = parseInt(cleanVal, 10);
                  if (num > 0) {
                    setCampaignId(String(num));
                  }
                }}
                onKeyDown={(e) => {
                  if (e.key === '-' || e.key === 'e' || e.key === '+' || e.key === '.') {
                    e.preventDefault();
                  }
                }}
                placeholder="Enter Campaign ID"
                min="1"
                step="1"
                required
              />
              <button type="submit" className="btn btn-primary fw-semibold" disabled={loading}>
                {loading ? 'Checking...' : 'Check Status'}
              </button>
            </div>
          </form>
        </div>
      </div>

      {feedback?.type === 'danger' ? (
        <CitizenErrorState
          error={feedback.message}
          title={feedback.title}
          actions={(
            <button className="citizen-action citizen-action--secondary" type="button" onClick={() => setFeedback(null)}>
              Dismiss
            </button>
          )}
        />
      ) : feedback ? (
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
      ) : null}

      {loading && (
        <CitizenStatePanel title="Checking waitlist status..." tone="loading" status>
          <p>Looking for an entry linked to Campaign #{campaignId}.</p>
        </CitizenStatePanel>
      )}

      {!loading && waitlistEntry && (
        <section className="waitlist-content citizen-waitlist-content" aria-label="Current waitlist entry">
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
