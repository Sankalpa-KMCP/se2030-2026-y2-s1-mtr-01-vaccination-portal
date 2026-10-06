import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import AvailabilitySearch from '../components/AvailabilitySearch';
import { bookAppointment } from '../api/appointmentApi';
import { joinWaitlist } from '../api/waitlistApi';
import { useAuth } from '../../../auth/context/AuthContext';
import CitizenErrorState from '../../../shared/ui/citizen/CitizenErrorState';
import CitizenPageHeader from '../../../shared/ui/citizen/CitizenPageHeader';
import CitizenStatePanel from '../../../shared/ui/citizen/CitizenStatePanel';
import CitizenWorkflowGuide from '../../../shared/ui/citizen/CitizenWorkflowGuide';
import '../citizen-appointment.css';

/**
 * Appointment Booking Page (US-03).
 * Composes AvailabilitySearch, handles booking execution, and maps backend error responses.
 * Uses the authenticated user's ID from AuthContext — no manual citizen ID entry required.
 * Offers waitlist join action when booking fails due to slot capacity exhaustion (MF03-BR-03, MF03-BR-06).
 *
 * <p><b>User Story:</b>
 * <ul>
 *   <li>US-03: Search available vaccination sessions and confirm an atomic booking.</li>
 * </ul>
 *
 * <p><b>Business Rules Handled in UI:</b>
 * <ul>
 *   <li>MF03-BR-01: Displays clear ineligibility guidance if screening assessment is not ELIGIBLE.</li>
 *   <li>MF03-BR-02: Detects and displays active booking conflict guidance.</li>
 *   <li>MF03-BR-03: Detects slot exhaustion and provides an immediate CTA to join the session waitlist (MF03-BR-06).</li>
 * </ul>
 */
export default function AppointmentBookingPage() {
  const { user } = useAuth();
  const citizenId = user?.id;
  const navigate = useNavigate();

  const [bookingSessionId, setBookingSessionId] = useState(null);
  const [joiningWaitlist, setJoiningWaitlist] = useState(false);
  const [feedback, setFeedback] = useState(null);

  const handleBookSession = async (session) => {
    const slotId = session.slotId;
    const campaignId = session.campaignId;
    const sessionId = session.sessionId;

    if (!citizenId) {
      setFeedback({
        type: 'danger',
        title: 'Authentication Error',
        message: 'Your session could not be verified. Please sign in again.',
      });
      return;
    }

    setBookingSessionId(slotId);
    setFeedback(null);

    try {
      const response = await bookAppointment({
        citizenId,
        campaignId,
        slotId,
      });

      setFeedback({
        type: 'success',
        title: 'Booking Confirmed!',
        message: `Appointment #${response.id} successfully booked at Center ${session.centerName || session.centerId}. Status: ${response.status}`,
      });
    } catch (err) {
      let userMessage = err.message || 'An unexpected error occurred during booking.';
      let isSlotFull = false;

      // Map backend error codes to clear user-facing guidance
      if (err.status === 403 || err.error === 'FORBIDDEN') {
        userMessage = 'Ineligible Citizen: You do not have a valid ELIGIBLE screening assessment for this campaign (MF03-BR-01).';
      } else if (err.status === 409 || err.error === 'CONFLICT') {
        const errorMsg = (err.message || '').toLowerCase();
        if (errorMsg.includes('already has an active appointment')) {
          userMessage = 'Duplicate Booking: You already hold an active appointment for this campaign (MF03-BR-02).';
        } else if (errorMsg.includes('slot_full') || errorMsg.includes('full')) {
          userMessage = 'Slot Exhausted: This time slot has no remaining capacity (MF03-BR-03). Consider joining the waitlist.';
          isSlotFull = true;
        } else {
          userMessage = `Booking Conflict: ${err.message}`;
        }
      }

      setFeedback({
        type: 'danger',
        title: 'Booking Failed',
        message: userMessage,
        isSlotFull,
        sessionToWaitlist: isSlotFull ? { campaignId, sessionId } : null,
      });
    } finally {
      setBookingSessionId(null);
    }
  };

  const handleJoinWaitlist = async (sessionTarget) => {
    if (!citizenId || !sessionTarget) return;

    setJoiningWaitlist(true);
    try {
      await joinWaitlist({
        citizenId,
        campaignId: sessionTarget.campaignId,
        sessionId: sessionTarget.sessionId,
      });
      navigate('/app/appointments/waitlist');
    } catch (waitlistErr) {
      setFeedback({
        type: 'danger',
        title: 'Waitlist Join Failed',
        message: waitlistErr.message || 'Unable to join the waitlist for this session.',
      });
    } finally {
      setJoiningWaitlist(false);
    }
  };

  if (!citizenId) {
    return (
      <div className="appointment-booking-page citizen-page citizen-appointment-flow">
        <CitizenPageHeader
          eyebrow="Appointment Services"
          title="Book a Vaccination Appointment"
          subtitle="Search available vaccination sessions and confirm a booking."
          backTo="/app/citizen"
          backLabel="Back to Dashboard"
          actions={<Link to="/app/appointments/mine">View My Appointments</Link>}
        />
        <CitizenStatePanel title="Authentication required" tone="warning">
          <p>Unable to load this page. Please sign in again.</p>
        </CitizenStatePanel>
      </div>
    );
  }

  return (
    <div className="appointment-booking-page citizen-page citizen-appointment-flow">
      <CitizenPageHeader
        eyebrow="Appointment Services"
        title="Book a Vaccination Appointment"
        subtitle="Search available vaccination sessions and confirm a booking."
        backTo="/app/citizen"
        backLabel="Back to Dashboard"
        actions={<Link to="/app/appointments/mine">View My Appointments</Link>}
      />

      <CitizenWorkflowGuide
        titleId="appointment-workflow-guide"
        steps={['Search sessions', 'Choose availability', 'Confirm booking']}
      />

      {feedback?.type === 'danger' ? (
        <CitizenErrorState
          error={feedback.message}
          title={feedback.title}
          actions={(
            <>
              {feedback.isSlotFull && feedback.sessionToWaitlist && (
                <button
                  type="button"
                  className="citizen-action citizen-action--secondary"
                  disabled={joiningWaitlist}
                  onClick={() => handleJoinWaitlist(feedback.sessionToWaitlist)}
                >
                  {joiningWaitlist ? 'Joining Waitlist...' : 'Join Waitlist for This Session'}
                </button>
              )}
              <button className="citizen-action citizen-action--secondary" type="button" onClick={() => setFeedback(null)}>
                Dismiss
              </button>
            </>
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

      <section aria-label="Appointment availability search">
        <AvailabilitySearch
          citizenId={citizenId}
          onBookSession={handleBookSession}
          bookingSessionId={bookingSessionId}
        />
      </section>
    </div>
  );
}
