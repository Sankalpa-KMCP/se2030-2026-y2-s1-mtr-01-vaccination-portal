import React, { useEffect, useRef } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../../auth/context/AuthContext';
import '../styles/citizen-login-welcome.css';

export const CITIZEN_WELCOME_DURATION_MS = 2600;
export const CITIZEN_WELCOME_REDUCED_MOTION_DURATION_MS = 900;

export default function CitizenLoginWelcome() {
  const { user } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const titleRef = useRef(null);
  const displayName = user?.fullName?.trim() || user?.email || 'Citizen';

  useEffect(() => {
    if (!location.state?.fromCitizenLogin) {
      navigate('/app/citizen', { replace: true });
      return undefined;
    }

    const prefersReducedMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
    const duration = prefersReducedMotion
      ? CITIZEN_WELCOME_REDUCED_MOTION_DURATION_MS
      : CITIZEN_WELCOME_DURATION_MS;
    const timeout = window.setTimeout(() => {
      navigate('/app/citizen', { replace: true });
    }, duration);

    titleRef.current?.focus({ preventScroll: true });
    return () => window.clearTimeout(timeout);
  }, [location.state, navigate]);

  const continueToDashboard = () => navigate('/app/citizen', { replace: true });

  return (
    <section
      className="citizen-login-welcome"
      aria-labelledby="citizen-login-welcome-title"
      aria-describedby="citizen-login-welcome-message"
    >
      <div className="citizen-login-welcome__glow citizen-login-welcome__glow--one" aria-hidden="true" />
      <div className="citizen-login-welcome__glow citizen-login-welcome__glow--two" aria-hidden="true" />

      <div className="citizen-login-welcome__card">
        <div className="citizen-login-welcome__mark" aria-hidden="true">
          <span className="citizen-login-welcome__orbit" />
          <span className="citizen-login-welcome__shield">
            <i className="bi bi-shield-check" />
          </span>
          <span className="citizen-login-welcome__success"><i className="bi bi-check-lg" /></span>
        </div>

        <p className="citizen-login-welcome__eyebrow">
          <i className="bi bi-lock-fill" aria-hidden="true" /> Secure sign-in successful
        </p>
        <h1 ref={titleRef} id="citizen-login-welcome-title" tabIndex="-1">
          Welcome, <span>{displayName}</span>
        </h1>
        <p id="citizen-login-welcome-message">
          Your citizen account is ready. We are preparing your personal vaccination dashboard.
        </p>

        <div className="citizen-login-welcome__progress" aria-hidden="true">
          <span />
        </div>

        <button
          className="citizen-login-welcome__continue"
          type="button"
          onClick={continueToDashboard}
        >
          Continue to dashboard <i className="bi bi-arrow-right" aria-hidden="true" />
        </button>
      </div>
    </section>
  );
}
