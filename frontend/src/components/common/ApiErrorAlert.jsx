import React from 'react';
import { PERMISSION_DENIED_MESSAGE } from '../../services/apiClient';

function getAlertTitle(status) {
  if (status === 0) return 'Connection Error';
  if (status === 403) return 'Permission Denied';
  if (status === 404) return 'Not Found';
  if (status === 409) return 'Request Conflict';
  return 'Request Error';
}

function normalizeFieldErrors(fieldErrors) {
  if (Array.isArray(fieldErrors)) {
    return fieldErrors.filter((item) => item?.message);
  }

  if (fieldErrors && typeof fieldErrors === 'object') {
    return Object.entries(fieldErrors).flatMap(([field, messages]) => {
      const fieldMessages = Array.isArray(messages) ? messages : [messages];
      return fieldMessages
        .filter(Boolean)
        .map((message) => ({ field, message }));
    });
  }

  return [];
}

/**
 * Reusable alert banner for displaying API errors and backend validation failures.
 *
 * @param {Object} props
 * @param {string|Error|null} props.error - Error message or Error object
 * @param {Function} [props.onDismiss] - Optional callback to clear/dismiss the error
 */
export default function ApiErrorAlert({ error, onDismiss }) {
  if (!error) return null;

  const status = typeof error === 'string' ? null : error.status;
  const errorMessage = status === 403
    ? PERMISSION_DENIED_MESSAGE
    : typeof error === 'string'
      ? error
      : error.message || 'An unexpected error occurred';
  const fieldErrors = normalizeFieldErrors(error?.data?.fieldErrors);

  return (
    <div className="alert alert-danger alert-dismissible fade show shadow-sm mb-4" role="alert">
      <div className="d-flex align-items-center">
        <div>
          <strong className="d-block mb-1">{getAlertTitle(status)}</strong>
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
