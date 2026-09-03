import React from 'react';

/**
 * Reusable centered loading spinner component.
 *
 * @param {Object} props
 * @param {string} [props.message='Loading...'] - Text description beneath spinner
 */
export default function LoadingSpinner({ message = 'Loading...' }) {
  return (
    <div className="d-flex flex-column justify-content-center align-items-center py-5">
      <div className="spinner-border text-primary mb-3" role="status" style={{ width: '3rem', height: '3rem' }}>
        <span className="visually-hidden">{message}</span>
      </div>
      <p className="text-secondary fw-semibold mb-0">{message}</p>
    </div>
  );
}
