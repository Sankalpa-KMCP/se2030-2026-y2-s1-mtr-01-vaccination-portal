import React from 'react';

/** Shared dismissible notice for successful appointment and waitlist actions. */
export default function AppointmentNotice({ feedback, onDismiss }) {
  return (
    <div className="citizen-page__notice">
      <div
        className={`alert alert-${feedback.type} alert-dismissible fade show border-0 shadow-sm`}
        role="alert"
      >
        <h4 className="h6 alert-heading fw-bold mb-1">{feedback.title}</h4>
        <p className="mb-0">{feedback.message}</p>
        <button type="button" className="btn-close" aria-label="Close" onClick={onDismiss}></button>
      </div>
    </div>
  );
}
