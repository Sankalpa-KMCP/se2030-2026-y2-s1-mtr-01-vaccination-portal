import React, { useState, useEffect, useCallback } from 'react';
import AppointmentCard from '../components/AppointmentCard';
import RescheduleDialog from '../components/RescheduleDialog';
import { getMyAppointments, cancelAppointment } from '../api/appointmentApi';

/**
 * Citizen My Appointments Page (US-03).
 * Fetches and displays a citizen's appointments across all statuses, enabling cancellation and rescheduling.
 */
export default function MyAppointmentsPage() {
  const [citizenId, setCitizenId] = useState('7001');
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

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    fetchAppointments(citizenId);
  };

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

  return (
    <div className="my-appointments-page">
      <div className="d-flex align-items-center justify-content-between mb-4">
        <div>
          <h1 className="h3 fw-bold text-primary mb-1">My Appointments</h1>
          <p className="text-muted mb-0">View, track, reschedule, or cancel your vaccination appointments.</p>
        </div>
      </div>

      <div className="card shadow-sm border-0 mb-4">
        <div className="card-body p-3">
          <form onSubmit={handleSearchSubmit} className="row g-2 align-items-center">
            <div className="col-auto">
              <label htmlFor="citizenIdFilter" className="col-form-label fw-semibold">
                Citizen ID:
              </label>
            </div>
            <div className="col-auto">
              <input
                type="number"
                id="citizenIdFilter"
                className="form-control"
                value={citizenId}
                onChange={(e) => setCitizenId(e.target.value)}
                placeholder="Enter Citizen ID"
                required
              />
            </div>
            <div className="col-auto">
              <button type="submit" className="btn btn-primary fw-semibold" disabled={loading}>
                {loading ? 'Loading...' : 'Fetch Appointments'}
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
          <p className="text-muted mt-2 fw-semibold">Fetching your appointments...</p>
        </div>
      )}

      {!loading && appointments !== null && (
        <div className="appointments-list-section">
          <div className="d-flex justify-content-between align-items-center mb-3">
            <h3 className="h5 fw-bold mb-0">Your Appointment History</h3>
            <span className="badge bg-secondary">{appointments.length} Total</span>
          </div>

          {appointments.length === 0 ? (
            <div className="alert alert-info border-0 shadow-sm p-4 text-center">
              <h4 className="h6 alert-heading fw-bold mb-1">No Appointments Found</h4>
              <p className="mb-0 text-muted small">
                You do not have any recorded vaccination appointments for Citizen ID #{citizenId}.
              </p>
            </div>
          ) : (
            <div className="appointments-list">
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
