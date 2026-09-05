import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import AvailabilitySearch from '../components/AvailabilitySearch';
import { bookAppointment } from '../api/appointmentApi';
import { joinWaitlist } from '../api/waitlistApi';
import { useAuth } from '../../../auth/context/AuthContext';
import CitizenPageHeader from '../../citizen-ui/components/CitizenPageHeader';

/**
 * Appointment Booking Page (US-03).
 * Composes AvailabilitySearch, handles booking execution, and maps backend error responses.
 * Uses the authenticated user's ID from AuthContext — no manual citizen ID entry required.
 * Offers waitlist join action when booking fails due to slot capacity exhaustion (MF03-BR-03, MF03-BR-06).
 */
export default function AppointmentBookingPage() {
  const { user } = useAuth();
  const citizenId = user?.id;
  const navigate = useNavigate();

  const [bookingSessionId, setBookingSessionId] = useState(null);
  const [joiningWaitlist, setJoiningWaitlist] = useState(false);
  const [feedback, setFeedback] = useState(null);

  const handleBookSession = async (session) => {
    const slotId = session.sessionId || session.slotId;
    const campaignId = session.campaignId;
    const sessionId = session.sessionId || session.slotId;

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
        if (err.message && err.message.includes('already has an active appointment')) {
          userMessage = 'Duplicate Booking: You already hold an active appointment for this campaign (MF03-BR-02).';
        } else if (err.message && err.message.includes('full')) {
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
      <div className="appointment-booking-page">
        <CitizenPageHeader
          eyebrow="Appointment Services"
          title="Book a Vaccination Appointment"
          subtitle="Search available vaccination sessions and confirm a booking."
          backTo="/app/citizen"
          backLabel="Back to Dashboard"
          actions={<Link to="/app/appointments/mine">View My Appointments</Link>}
        />
        <div className="alert alert-warning border-0 shadow-sm" role="alert">
          <h2 className="h6 alert-heading fw-bold mb-1">Authentication Required</h2>
          <p className="mb-0">Unable to load this page. Please sign in again.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="appointment-booking-page">
      <CitizenPageHeader
        eyebrow="Appointment Services"
        title="Book a Vaccination Appointment"
        subtitle="Search available vaccination sessions and confirm a booking."
        backTo="/app/citizen"
        backLabel="Back to Dashboard"
        actions={<Link to="/app/appointments/mine">View My Appointments</Link>}
      />

      {feedback && (
        <div
          className={`alert alert-${feedback.type} alert-dismissible fade show border-0 shadow-sm mb-4`}
          role="alert"
        >
          <h4 className="h6 alert-heading fw-bold mb-1">{feedback.title}</h4>
          <p className="mb-0">{feedback.message}</p>
          {feedback.isSlotFull && feedback.sessionToWaitlist && (
            <div className="mt-3 pt-2 border-top">
              <button
                type="button"
                className="btn btn-warning btn-sm fw-bold shadow-sm"
                disabled={joiningWaitlist}
                onClick={() => handleJoinWaitlist(feedback.sessionToWaitlist)}
              >
                {joiningWaitlist ? (
                  <>
                    <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                    Joining Waitlist...
                  </>
                ) : (
                  'Join Waitlist for This Session'
                )}
              </button>
            </div>
          )}
          <button
            type="button"
            className="btn-close"
            aria-label="Close"
            onClick={() => setFeedback(null)}
          ></button>
        </div>
      )}

      <AvailabilitySearch
        citizenId={citizenId}
        onBookSession={handleBookSession}
        bookingSessionId={bookingSessionId}
      />
    </div>
  );
}
