import React, { useEffect, useRef } from 'react';
import '../styles/post-login-greeting.css';

export const POST_LOGIN_GREETING_DURATION_MS = 1500;

const CONTENT = {
  citizen: {
    eyebrow: 'Citizen sign-in successful',
    message: 'Your secure vaccination dashboard is ready.',
    icon: 'bi-person-check-fill',
  },
  staff: {
    eyebrow: 'Staff sign-in successful',
    message: 'Your secure operational workspace is ready.',
    icon: 'bi-person-badge-fill',
  },
  admin: {
    eyebrow: 'Administrator sign-in successful',
    message: 'Your secure administration workspace is ready.',
    icon: 'bi-shield-lock-fill',
  },
};

export default function PostLoginGreeting({ audience = 'citizen', name, onComplete }) {
  const skipButtonRef = useRef(null);
  const content = CONTENT[audience] || CONTENT.citizen;

  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    skipButtonRef.current?.focus({ preventScroll: true });

    const handleKeyDown = (event) => {
      if (event.key === 'Escape') onComplete();
    };
    window.addEventListener('keydown', handleKeyDown);
    const timeout = window.setTimeout(onComplete, POST_LOGIN_GREETING_DURATION_MS);

    return () => {
      window.clearTimeout(timeout);
      window.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = previousOverflow;
    };
  }, [onComplete]);

  return (
    <div
      className={`post-login-greeting post-login-greeting--${audience}`}
      role="dialog"
      aria-modal="true"
      aria-labelledby="post-login-greeting-title"
      aria-describedby="post-login-greeting-message"
    >
      <div className="post-login-greeting__halo post-login-greeting__halo--left" aria-hidden="true" />
      <div className="post-login-greeting__halo post-login-greeting__halo--right" aria-hidden="true" />

      <button
        ref={skipButtonRef}
        className="post-login-greeting__skip"
        type="button"
        onClick={onComplete}
      >
        Skip intro <i className="bi bi-arrow-right" aria-hidden="true" />
      </button>

      <div className="post-login-greeting__content">
        <div className="post-login-greeting__mark" aria-hidden="true">
          <span className="post-login-greeting__orbit post-login-greeting__orbit--outer" />
          <span className="post-login-greeting__orbit post-login-greeting__orbit--inner" />
          <span className="post-login-greeting__shield">
            <i className={`bi ${content.icon}`} />
          </span>
          <span className="post-login-greeting__check"><i className="bi bi-check-lg" /></span>
        </div>

        <p className="post-login-greeting__eyebrow">
          <i className="bi bi-lock-fill" aria-hidden="true" /> {content.eyebrow}
        </p>
        <h1 id="post-login-greeting-title" aria-label={`Welcome, ${name}`}>
          Welcome,
          <span> {name}</span>
        </h1>
        <p id="post-login-greeting-message" className="post-login-greeting__message">
          {content.message}
        </p>

        <div className="post-login-greeting__progress" aria-hidden="true"><span /></div>
      </div>
    </div>
  );
}
