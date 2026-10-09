package com.se2030.vaccinationportal.appointment.event;

/**
 * Observer Pattern: the common contract for components that react to appointment changes.
 * Audit and notification observers implement this interface, keeping their actions
 * separate from the booking, cancellation, and promotion services.
 */
public interface AppointmentObserver {
    // The subject calls this method to deliver the same event to each observer.
    // Each implementation decides how to react, for example by writing an audit log.
    void update(AppointmentEvent event);
}
