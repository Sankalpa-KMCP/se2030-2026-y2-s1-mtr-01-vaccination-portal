import React, { useState } from 'react';
import { resendVerification } from '../api/authApi';
import { ArrowIcon } from './AuthIcons';

export default function ResendVerificationForm({
  initialEmail = '',
  onSuccessMessage,
  compact = false,
}) {
  const [email, setEmail] = useState(initialEmail);
  const [clientError, setClientError] = useState('');
  const [apiError, setApiError] = useState(null);
  const [successMessage, setSuccessMessage] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setClientError('');
    setApiError(null);
    setSuccessMessage('');

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
      const response = await resendVerification(trimmedEmail);
      const message = response?.message || 'If the account exists and is pending verification, a verification email has been sent.';
      setSuccessMessage(message);
      if (onSuccessMessage) {
        onSuccessMessage(message);
      }
    } catch (error) {
      setApiError(error?.message || 'Failed to request verification email. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const activeError = clientError || apiError;

  return (
    <div className={`auth-resend-verification ${compact ? 'auth-resend-verification--compact' : ''}`}>
      {successMessage ? (
        <div className="auth-alert auth-alert--success" role="status" style={{ background: '#f0fdf4', borderColor: '#bbf7d0', color: '#166534' }}>
          {successMessage}
        </div>
      ) : (
        <form onSubmit={handleSubmit} noValidate aria-label="Resend verification email">
          {activeError && (
            <div className="auth-alert" role="alert" id="resend-error">
              {activeError}
            </div>
          )}
          <div className="auth-field">
            <label htmlFor="resend-email">Email Address</label>
            <input
              type="email"
              id="resend-email"
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
              aria-describedby={activeError ? 'resend-error' : undefined}
              required
            />
          </div>
          <button type="submit" className="auth-primary-action" disabled={isSubmitting}>
            <span>
              {isSubmitting && <span className="auth-spinner" aria-hidden="true" />}
              {isSubmitting ? 'Sending verification link…' : 'Resend verification email'}
            </span>
            {!isSubmitting && <ArrowIcon />}
          </button>
        </form>
      )}
    </div>
  );
}
