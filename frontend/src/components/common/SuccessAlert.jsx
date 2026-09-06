import React, { useEffect } from 'react';

/**
 * Shared success banner. Unifies the five hand-rolled alert-success variants;
 * dismissal behavior is opt-in per call site so existing auto-dismiss timers
 * keep working.
 *
 * @param {Object} props
 * @param {React.ReactNode} props.message - Success copy (may include <strong>).
 * @param {Function} [props.onDismiss] - When provided, renders a dismiss button.
 * @param {number} [props.autoDismissMs] - When set with onDismiss, auto-dismisses.
 */
export default function SuccessAlert({ message, onDismiss, autoDismissMs }) {
  useEffect(() => {
    if (!onDismiss || !autoDismissMs) return undefined;
    const timer = setTimeout(onDismiss, autoDismissMs);
    return () => clearTimeout(timer);
  }, [onDismiss, autoDismissMs]);

  if (!message) return null;

  return (
    <div
      className="alert alert-success d-flex align-items-start justify-content-between gap-2 shadow-sm border-0 mb-4"
      role="status"
    >
      <div className="d-flex align-items-start gap-2">
        <i className="bi bi-check-circle mt-1" aria-hidden="true"></i>
        <div>{message}</div>
      </div>
      {onDismiss && (
        <button
          type="button"
          className="btn-close"
          aria-label="Close"
          onClick={onDismiss}
        />
      )}
    </div>
  );
}
