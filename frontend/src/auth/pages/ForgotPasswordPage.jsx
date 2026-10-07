import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { forgotPassword } from '../api/authApi';
import { ArrowIcon, SuccessSealIcon } from '../components/AuthIcons';

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [clientError, setClientError] = useState('');
  const [apiError, setApiError] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');

  const handleSubmit = async (event) => {
    event.preventDefault();
    setClientError('');
    setApiError(null);

    const trimmedEmail = email.trim();
    if (!trimmedEmail) {
      setClientError('Email address is required.');
      return;
    }
    if (!trimmedEmail.includes('@') || trimmedEmail.startsWith('@') || trimmedEmail.endsWith('@')) {
      setClientError('Please enter a valid email address.');
      return;
    }
    if (trimmedEmail.length > 255) {
      setClientError('Email must not exceed 255 characters.');
      return;
    }

    setIsSubmitting(true);
    try {
      const response = await forgotPassword(trimmedEmail);
      setSuccessMessage(
        response?.message || 'If an eligible account is associated with this email address, password reset instructions have been sent.',
      );
    } catch (error) {
      setApiError(error?.message || 'Failed to submit password reset request. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const activeError = clientError || apiError;

  return (
    <article className="auth-panel auth-panel--forgot-password">
      <header className="auth-panel__header">
        <p className="auth-panel__eyebrow">Password recovery</p>
        <h1 tabIndex="-1">Forgot your password?</h1>
        <p className="auth-panel__description">
          Enter your registered email address to receive password reset instructions.
        </p>
      </header>

      {successMessage ? (
        <section className="auth-success" role="status">
          <div className="auth-success__seal">
            <SuccessSealIcon />
          </div>
          <p>Request submitted</p>
          <h2>Check your email</h2>
          <span>{successMessage}</span>
          <Link className="auth-primary-action" to="/login">
            <span>Return to Sign in</span>
            <ArrowIcon />
          </Link>
        </section>
      ) : (
        <>
          {activeError && (
            <div className="auth-alert" id="forgot-password-error" role="alert">
              {activeError}
            </div>
          )}
          <form
            className="auth-form"
            onSubmit={handleSubmit}
            noValidate
            aria-label="Request password reset"
          >
            <div className="auth-field">
              <label htmlFor="forgot-email">Email Address</label>
              <input
                type="email"
                id="forgot-email"
                name="email"
                className="auth-input"
                placeholder="name@example.gov"
                autoComplete="email"
                inputMode="email"
                value={email}
                onChange={(event) => {
                  setEmail(event.target.value);
                  if (clientError) setClientError('');
                  if (apiError) setApiError(null);
                }}
                disabled={isSubmitting}
                maxLength={255}
                aria-invalid={Boolean(activeError)}
                aria-describedby={activeError ? 'forgot-password-error' : undefined}
                required
              />
            </div>

            <button
              type="submit"
              className="auth-primary-action"
              disabled={isSubmitting}
            >
              <span>
                {isSubmitting && <span className="auth-spinner" aria-hidden="true" />}
                {isSubmitting ? 'Sending instructions…' : 'Send reset instructions'}
              </span>
              {!isSubmitting && <ArrowIcon />}
            </button>
          </form>
        </>
      )}

      <footer className="auth-panel__footer">
        <span>Remembered your password?</span>
        <Link to="/login">Sign in</Link>
      </footer>
    </article>
  );
}
