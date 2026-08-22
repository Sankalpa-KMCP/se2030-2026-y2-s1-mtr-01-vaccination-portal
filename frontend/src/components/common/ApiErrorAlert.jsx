import React from 'react';

/**
 * Reusable alert banner for displaying API errors and backend validation failures.
 *
 * @param {Object} props
 * @param {string|Error|null} props.error - Error message or Error object
 * @param {Function} [props.onDismiss] - Optional callback to clear/dismiss the error
 */
export default function ApiErrorAlert({ error, onDismiss }) {
  if (!error) return null;

  const errorMessage = typeof error === 'string' ? error : error.message || 'An unexpected error occurred';
  const fieldErrors = error?.data?.fieldErrors || [];

  return (
    <div className="alert alert-danger alert-dismissible fade show shadow-sm mb-4" role="alert">
      <div className="d-flex align-items-center">
        <div>
          <strong className="d-block mb-1">Request Error</strong>
          <div>{errorMessage}</div>
          {fieldErrors.length > 0 && (
            <ul className="mb-0 mt-2 ps-3 small">
              {fieldErrors.map((fe, index) => (
                <li key={index}>
                  <strong>{fe.field}:</strong> {fe.message}
                </li>
              ))}
            </ul>
          )}
        </div>
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
