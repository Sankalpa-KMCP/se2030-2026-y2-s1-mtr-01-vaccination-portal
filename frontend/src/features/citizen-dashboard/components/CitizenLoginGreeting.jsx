import React, { useEffect, useRef } from 'react';
import '../styles/citizen-login-greeting.css';

export const CITIZEN_LOGIN_GREETING_DURATION_MS = 1500;

export default function CitizenLoginGreeting({ name, onComplete }) {
  const skipButtonRef = useRef(null);

  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    skipButtonRef.current?.focus({ preventScroll: true });

    const handleKeyDown = (event) => {
      if (event.key === 'Escape') onComplete();
    };
    window.addEventListener('keydown', handleKeyDown);
    const timeout = window.setTimeout(onComplete, CITIZEN_LOGIN_GREETING_DURATION_MS);

    return () => {
      window.clearTimeout(timeout);
      window.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = previousOverflow;
    };
  }, [onComplete]);

  return (
    <div
      className="citizen-login-greeting"
      role="dialog"
      aria-modal="true"
      aria-labelledby="citizen-login-greeting-title"
      aria-describedby="citizen-login-greeting-message"
    >
      <div className="citizen-login-greeting__halo citizen-login-greeting__halo--left" aria-hidden="true" />
      <div className="citizen-login-greeting__halo citizen-login-greeting__halo--right" aria-hidden="true" />

      <button
        ref={skipButtonRef}
        className="citizen-login-greeting__skip"
        type="button"
        onClick={onComplete}
      >
        Skip intro <i className="bi bi-arrow-right" aria-hidden="true" />
      </button>

      <div className="citizen-login-greeting__content">
        <div className="citizen-login-greeting__mark" aria-hidden="true">
          <span className="citizen-login-greeting__orbit citizen-login-greeting__orbit--outer" />
          <span className="citizen-login-greeting__orbit citizen-login-greeting__orbit--inner" />
          <span className="citizen-login-greeting__shield">
            <i className="bi bi-person-check-fill" />
          </span>
          <span className="citizen-login-greeting__check"><i className="bi bi-check-lg" /></span>
        </div>

        <p className="citizen-login-greeting__eyebrow">
          <i className="bi bi-lock-fill" aria-hidden="true" /> Citizen sign-in successful
        </p>
        <h1 id="citizen-login-greeting-title" aria-label={`Welcome, ${name}`}>
          Welcome,
          <span> {name}</span>
        </h1>
        <p id="citizen-login-greeting-message" className="citizen-login-greeting__message">
          Your secure vaccination dashboard is ready.
        </p>

        <div className="citizen-login-greeting__progress" aria-hidden="true"><span /></div>
      </div>
    </div>
  );
}
