import React, { useState } from 'react';
import AvailabilitySearch from '../components/AvailabilitySearch';
import { bookAppointment } from '../api/appointmentApi';
import { useAuth } from '../../../auth/context/AuthContext';

/**
 * Appointment Booking Page (US-03).
 * Composes AvailabilitySearch, handles booking execution, and maps backend error responses.
 * Uses the authenticated user's ID from AuthContext — no manual citizen ID entry required.
 */
export default function AppointmentBookingPage() {
  const { user } = useAuth();
  const citizenId = user?.id;

  const [bookingSessionId, setBookingSessionId] = useState(null);
  const [feedback, setFeedback] = useState(null);

  const handleBookSession = async (session) => {
    const slotId = session.sessionId || session.slotId;
    const campaignId = session.campaignId;

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

      // Map backend error codes to clear user-facing guidance
      if (err.status === 403 || err.error === 'FORBIDDEN') {
        userMessage = 'Ineligible Citizen: You do not have a valid ELIGIBLE screening assessment for this campaign (MF03-BR-01).';
      } else if (err.status === 409 || err.error === 'CONFLICT') {
        if (err.message && err.message.includes('already has an active appointment')) {
          userMessage = 'Duplicate Booking: You already hold an active appointment for this campaign (MF03-BR-02).';
        } else if (err.message && err.message.includes('full')) {
          userMessage = 'Slot Exhausted: This time slot has no remaining capacity (MF03-BR-03). Consider joining the waitlist.';
        } else {
          userMessage = `Booking Conflict: ${err.message}`;
        }
      }

      setFeedback({
        type: 'danger',
        title: 'Booking Failed',
        message: userMessage,
      });
    } finally {
      setBookingSessionId(null);
    }
  };

  if (!citizenId) {
    return (
      <div className="alert alert-warning border-0 shadow-sm" role="alert">
        <h4 className="h6 alert-heading fw-bold mb-1">Authentication Required</h4>
        <p className="mb-0">Unable to load this page. Please sign in again.</p>
      </div>
    );
  }

  return (
    <div className="appointment-booking-page">
      <div className="d-flex align-items-center justify-content-between mb-4">
        <div>
          <h1 className="h3 fw-bold text-primary mb-1">Book a Vaccination Appointment</h1>
          <p className="text-muted mb-0">Search available slots at nearby centers and confirm your booking.</p>
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

      <AvailabilitySearch
        citizenId={citizenId}
        onBookSession={handleBookSession}
        bookingSessionId={bookingSessionId}
      />
    </div>
  );
}
