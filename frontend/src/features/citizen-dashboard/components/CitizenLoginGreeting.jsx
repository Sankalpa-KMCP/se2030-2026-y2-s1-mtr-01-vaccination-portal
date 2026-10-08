import React, { useEffect } from 'react';
import '../styles/citizen-login-greeting.css';

export const CITIZEN_LOGIN_GREETING_DURATION_MS = 2400;

export default function CitizenLoginGreeting({ name, onComplete }) {
  useEffect(() => {
    const timeout = window.setTimeout(onComplete, CITIZEN_LOGIN_GREETING_DURATION_MS);
    return () => window.clearTimeout(timeout);
  }, [onComplete]);

  return (
    <div className="citizen-login-greeting" role="status" aria-live="polite">
      <div className="citizen-login-greeting__card">
        <span className="citizen-login-greeting__icon" aria-hidden="true">
          <i className="bi bi-person-check-fill" />
        </span>
        <div>
          <p>Login successful</p>
          <h2>Welcome, {name}</h2>
        </div>
        <button type="button" onClick={onComplete} aria-label="Close welcome message">
          <i className="bi bi-x-lg" aria-hidden="true" />
        </button>
        <span className="citizen-login-greeting__timer" aria-hidden="true" />
      </div>
    </div>
  );
}
