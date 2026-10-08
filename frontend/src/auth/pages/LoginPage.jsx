import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getRoleHomePath } from '../utils/roleRedirect';
import { resendVerification } from '../api/authApi';
import { ArrowIcon, EyeIcon } from '../components/AuthIcons';

function LoginForm() {
  const { login, loginError, clearLoginError } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [clientError, setClientError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isResending, setIsResending] = useState(false);
  const [resendSuccess, setResendSuccess] = useState('');

  const clearErrors = () => {
    if (clientError) setClientError('');
    if (loginError) clearLoginError();
    if (resendSuccess) setResendSuccess('');
  };

  const handleResendVerification = async () => {
    const trimmedEmail = email.trim();
    if (!trimmedEmail) {
      setClientError('Please enter your email address to resend verification.');
      return;
    }
    setIsResending(true);
    try {
      const response = await resendVerification(trimmedEmail);
      setResendSuccess(
        response?.message || 'If the account exists and is pending verification, a verification email has been sent.',
      );
    } catch (err) {
      setClientError(err?.message || 'Failed to resend verification email.');
    } finally {
      setIsResending(false);
    }
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setClientError('');
    clearLoginError();
    setResendSuccess('');

    const trimmedEmail = email.trim();
    if (!trimmedEmail) return setClientError('Email address is required.');
    if (!trimmedEmail.includes('@') || trimmedEmail.startsWith('@') || trimmedEmail.endsWith('@')) return setClientError('Please enter a valid email address.');
    if (trimmedEmail.length > 255) return setClientError('Email must not exceed 255 characters.');
    if (!password) return setClientError('Password is required.');
    if (password.length > 128) return setClientError('Password must not exceed 128 characters.');

    setIsSubmitting(true);
    try {
      const result = await login(trimmedEmail, password);
      if (result.success) {
        setPassword('');
        navigate(getRoleHomePath(result.user), { replace: true });
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const activeError = clientError || loginError;
  const isPendingVerification = Boolean(
    activeError && (
      activeError === 'Your email address still needs to be verified before you can sign in.' ||
      activeError.includes('needs to be verified')
    ),
  );

  return (
    <>
      {activeError && (
        <div className="auth-alert" id="login-error" role="alert">
          <div>{activeError}</div>
          {isPendingVerification && (
            <div style={{ marginTop: '0.6rem', paddingTop: '0.5rem', borderTop: '1px solid rgba(122, 48, 52, 0.2)' }}>
              {resendSuccess ? (
                <span style={{ fontSize: '0.74rem', color: '#166534', fontWeight: 600 }}>{resendSuccess}</span>
              ) : (
                <button
                  type="button"
                  className="auth-resend-btn"
                  style={{
                    background: 'transparent',
                    border: '1px solid currentColor',
                    borderRadius: '4px',
                    color: '#7a3034',
                    fontSize: '0.72rem',
                    padding: '0.25rem 0.55rem',
                    cursor: 'pointer',
                    fontWeight: 600,
                  }}
                  onClick={handleResendVerification}
                  disabled={isResending}
                >
                  {isResending ? 'Sending…' : 'Resend verification email'}
                </button>
              )}
            </div>
          )}
        </div>
      )}
      <form className="auth-form" onSubmit={handleSubmit} noValidate aria-label="Sign in credentials">
        <div className="auth-field">
          <label htmlFor="login-email">Email Address</label>
          <input
            type="email"
            id="login-email"
            name="email"
            className="auth-input"
            placeholder="name@example.gov"
            autoComplete="email"
            inputMode="email"
            value={email}
            onChange={(event) => { setEmail(event.target.value); clearErrors(); }}
            disabled={isSubmitting}
            maxLength={255}
            aria-invalid={Boolean(activeError)}
            aria-describedby={activeError ? 'login-error' : undefined}
            required
          />
        </div>

        <div className="auth-field">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: '0.45rem' }}>
            <label htmlFor="login-password" style={{ marginBottom: 0 }}>Password</label>
            <Link
              to="/forgot-password"
              style={{ fontSize: '0.72rem', color: 'var(--auth-accent)', textDecoration: 'none', fontWeight: 600 }}
            >
              Forgot password?
            </Link>
          </div>
          <div className="auth-password">
            <input
              type={showPassword ? 'text' : 'password'}
              id="login-password"
              name="password"
              className="auth-input"
              placeholder="Enter your password"
              autoComplete="current-password"
              value={password}
              onChange={(event) => { setPassword(event.target.value); clearErrors(); }}
              disabled={isSubmitting}
              maxLength={128}
              aria-invalid={Boolean(activeError)}
              aria-describedby={activeError ? 'login-error' : undefined}
              required
            />
            <button type="button" className="auth-password__toggle" onClick={() => setShowPassword((value) => !value)} aria-label={showPassword ? 'Hide password' : 'Show password'} disabled={isSubmitting}>
              <EyeIcon hidden={showPassword} />
            </button>
          </div>
        </div>

        <button type="submit" className="auth-primary-action" disabled={isSubmitting}>
          <span>{isSubmitting && <span className="auth-spinner" aria-hidden="true" />}{isSubmitting ? 'Signing in…' : 'Sign in'}</span>
          {!isSubmitting && <ArrowIcon />}
        </button>
      </form>
    </>
  );
}

function UniversalRegistrationLinks() {
  return (
    <div className="auth-registration-section" aria-label="Registration options">
      <div className="auth-registration-links">
        <span>New to the portal?</span>
        <Link to="/register/citizen">Register as Citizen</Link>
        <Link to="/register/staff">Request Staff Access</Link>
      </div>
      <p className="auth-registration-note">Staff accounts require administrator approval before access.</p>
    </div>
  );
}

export default function LoginPage({ onBackToLanding }) {
  return (
    <article className="auth-panel auth-panel--login auth-panel--generic">
      {onBackToLanding && <button type="button" className="auth-standalone-back" onClick={onBackToLanding} aria-label="Back to portal home"><ArrowIcon direction="left" /> Back to portal home</button>}
      <header className="auth-panel__header">
        <p className="auth-panel__eyebrow">Vaccination Portal</p>
        <h1 tabIndex="-1">Welcome back</h1>
        <p className="auth-panel__description">Sign in with your registered account to continue.</p>
      </header>
      <LoginForm />
      <UniversalRegistrationLinks />
    </article>
  );
}
