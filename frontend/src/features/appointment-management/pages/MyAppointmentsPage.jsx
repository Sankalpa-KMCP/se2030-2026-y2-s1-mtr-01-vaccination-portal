import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import AppointmentCard from '../components/AppointmentCard';
import RescheduleDialog from '../components/RescheduleDialog';
import { getMyAppointments, cancelAppointment } from '../api/appointmentApi';
import { useAuth } from '../../../auth/context/AuthContext';
import CitizenErrorState from '../../../shared/ui/citizen/CitizenErrorState';
import CitizenPageHeader from '../../../shared/ui/citizen/CitizenPageHeader';
import CitizenStatePanel from '../../../shared/ui/citizen/CitizenStatePanel';
import CitizenStatusBadge from '../../../shared/ui/citizen/CitizenStatusBadge';
import '../citizen-appointment.css';

/**
 * Citizen My Appointments Page (US-03).
 * Fetches and displays the authenticated citizen's appointments across all statuses,
 * enabling cancellation and rescheduling. Uses the authenticated user ID from AuthContext.
 *
 * <p><b>User Story:</b>
 * <ul>
 *   <li>US-03: View recorded appointments, cancel existing booking, or launch rescheduling modal.</li>
 * </ul>
 *
 * <p><b>Business Rules Handled in UI:</b>
 * <ul>
 *   <li>MF03-BR-04: Enforces 24-hour notice rule guidance when cancellation or rescheduling fails due to window restriction.</li>
 *   <li>MF03-BR-11: Restricts action controls to active CONFIRMED appointments, keeping completed and cancelled records read-only.</li>
 * </ul>
 */
export default function MyAppointmentsPage() {
  const { user } = useAuth();
  const citizenId = user?.id;

  const [appointments, setAppointments] = useState(null);
  const [loading, setLoading] = useState(false);
  const [feedback, setFeedback] = useState(null);
  const [rescheduleTarget, setRescheduleTarget] = useState(null);

  const fetchAppointments = useCallback(async (idToFetch) => {
    if (!idToFetch) return;
    setLoading(true);
    setFeedback(null);

    try {
      const data = await getMyAppointments(idToFetch);
      setAppointments(data || []);
    } catch (err) {
      setFeedback({
        type: 'danger',
        title: 'Error Loading Appointments',
        message: err.message || 'Failed to retrieve appointments.',
      });
      setAppointments([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchAppointments(citizenId);
  }, [citizenId, fetchAppointments]);

  const handleCancelAppointment = async (appointmentId, { isStaffOverride, staffReason }) => {
    setFeedback(null);
    try {
      await cancelAppointment(appointmentId, {
        requestingCitizenId: citizenId,
        isStaffOverride,
        staffReason,
      });

      setFeedback({
        type: 'success',
        title: 'Appointment Cancelled',
        message: `Appointment #${appointmentId} has been successfully cancelled.`,
      });
      fetchAppointments(citizenId);
    } catch (err) {
      let userMessage = err.message || 'Failed to cancel appointment.';

      if (err.status === 403 || err.error === 'FORBIDDEN') {
        userMessage = '24-Hour Notice Rule (MF03-BR-04): Cancellations within 24 hours of session start require staff override with reason.';
      }

      setFeedback({
        type: 'danger',
        title: 'Cancellation Failed',
        message: userMessage,
      });
    }
  };

  const handleRescheduleSuccess = (updatedAppointment) => {
    setRescheduleTarget(null);
    setFeedback({
      type: 'success',
      title: 'Appointment Rescheduled!',
      message: `Appointment #${updatedAppointment.id} has been rescheduled to new slot #${updatedAppointment.slotId}.`,
    });
    fetchAppointments(citizenId);
  };

  if (!citizenId) {
    return (
      <div className="my-appointments-page citizen-page citizen-appointments-page">
        <CitizenPageHeader
          eyebrow="Appointment Services"
          title="My Appointments"
          subtitle="View and manage your recorded vaccination appointments."
          backTo="/app/citizen"
          backLabel="Back to Dashboard"
          actions={<Link to="/app/appointments/book">Book Appointment</Link>}
        />
        <CitizenStatePanel title="Authentication required" tone="warning">
          <p>Unable to load appointments. Please sign in again.</p>
        </CitizenStatePanel>
      </div>
    );
  }

  return (
    <div className="my-appointments-page citizen-page citizen-appointments-page">
      <CitizenPageHeader
        eyebrow="Appointment Services"
        title="My Appointments"
        subtitle="View and manage your recorded vaccination appointments."
        backTo="/app/citizen"
        backLabel="Back to Dashboard"
        actions={appointments?.length > 0 ? <Link to="/app/appointments/book">Book Appointment</Link> : undefined}
      />

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
        <CitizenStatePanel title="Fetching your appointments..." tone="loading" status>
          <p>Checking your recorded bookings and their current statuses.</p>
        </CitizenStatePanel>
      )}

      {!loading && appointments !== null && feedback?.type !== 'danger' && (
        <div className="appointments-list-section">
          <header className="citizen-section__header mb-3">
            <div>
              <p className="citizen-ui-eyebrow">Recorded bookings</p>
              <h2>Your Appointment History</h2>
              <p className="citizen-section__description">Active and completed records remain together, with their recorded status shown on each appointment.</p>
            </div>
            <CitizenStatusBadge tone="info">{appointments.length} total</CitizenStatusBadge>
          </header>

          {appointments.length === 0 ? (
            <CitizenStatePanel
              title="No Appointments Found"
              actions={<Link className="citizen-action citizen-action--primary" to="/app/appointments/book">Book Appointment</Link>}
            >
              <p>You do not have any recorded vaccination appointments. Search available sessions when you are ready to book.</p>
            </CitizenStatePanel>
          ) : (
            <div className="appointments-list citizen-appointments-list">
              {appointments.map((appt) => (
                <AppointmentCard
                  key={appt.id}
                  appointment={appt}
                  onCancel={handleCancelAppointment}
                  onReschedule={(target) => setRescheduleTarget(target)}
                />
              ))}
            </div>
          )}
        </div>
      )}

      {rescheduleTarget && (
        <RescheduleDialog
          appointment={rescheduleTarget}
          citizenId={citizenId}
          onClose={() => setRescheduleTarget(null)}
          onSuccess={handleRescheduleSuccess}
        />
      )}
    </div>
  );
}
