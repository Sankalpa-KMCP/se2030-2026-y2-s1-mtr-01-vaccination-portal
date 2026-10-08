import React, { useEffect, useState } from 'react';
import { usePasswordPrivacy } from '../components/authCharacterContext';
import { Link, useSearchParams } from 'react-router-dom';
import { resetPassword } from '../api/authApi';
import { ArrowIcon, EyeIcon, SuccessSealIcon } from '../components/AuthIcons';

const RESET_ERROR_MESSAGES = {
  INVALID_TOKEN: 'This password reset token is invalid or unrecognized. Please request a new link.',
  TOKEN_EXPIRED: 'This password reset token has expired. Please request a new reset link.',
  TOKEN_ALREADY_USED: 'This password reset token has already been used. Please request a new reset link if needed.',
  VALIDATION_FAILED: 'Please ensure your new password meets all requirements (8 to 128 characters).',
};

function mapResetError(error) {
  const code = error?.data?.code;
  if (code && RESET_ERROR_MESSAGES[code]) {
    return RESET_ERROR_MESSAGES[code];
  }
  return error?.message || 'Failed to reset password. Please try again.';
}

export default function ResetPasswordPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [token] = useState(() => searchParams.get('token') || '');

  useEffect(() => {
    if (searchParams.has('token')) {
      const nextParams = new URLSearchParams(searchParams);
      nextParams.delete('token');
      setSearchParams(nextParams, { replace: true });
      if (typeof window !== 'undefined' && window.history?.replaceState) {
        const nextSearch = nextParams.toString() ? `?${nextParams.toString()}` : '';
        window.history.replaceState({}, document.title, window.location.pathname + nextSearch);
      }
    }
  }, [searchParams, setSearchParams]);

  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const newPasswordPrivacy = usePasswordPrivacy(showPassword);
  const confirmPasswordPrivacy = usePasswordPrivacy(showConfirmPassword);
  const [clientError, setClientError] = useState('');
  const [apiError, setApiError] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');

  const handleSubmit = async (event) => {
    event.preventDefault();
    setClientError('');
    setApiError(null);

    if (!token.trim()) {
      setClientError('A valid password reset token is required. Please check the link from your email.');
      return;
    }

    if (!newPassword) {
      setClientError('New password is required.');
      return;
    }
    if (newPassword.length < 8 || newPassword.length > 128) {
      setClientError('Password must be between 8 and 128 characters.');
      return;
    }
    if (!confirmPassword) {
      setClientError('Please confirm your new password.');
      return;
    }
    if (newPassword !== confirmPassword) {
      setClientError('Passwords do not match.');
      return;
    }

    setIsSubmitting(true);
    try {
      const response = await resetPassword(token, newPassword);
      setSuccessMessage(response?.message || 'Password has been reset successfully.');
      setIsSuccess(true);
    } catch (error) {
      setApiError(mapResetError(error));
    } finally {
      setIsSubmitting(false);
    }
  };

  const activeError = clientError || apiError;
  const isMissingToken = !token.trim();

  return (
    <article className="auth-panel auth-panel--reset-password">
      <header className="auth-panel__header">
        <p className="auth-panel__eyebrow">Password recovery</p>
        <h1 tabIndex="-1">Set new password</h1>
        <p className="auth-panel__description">
          Create a new, secure password to regain access to your account.
        </p>
      </header>

      {isSuccess ? (
        <section className="auth-success" role="status">
          <div className="auth-success__seal">
            <SuccessSealIcon />
          </div>
          <p>Password updated</p>
          <h2>Password reset successfully</h2>
          <span>{successMessage} You may now sign in using your new password.</span>
          <Link className="auth-primary-action" to="/login">
            <span>Continue to Sign in</span>
            <ArrowIcon />
          </Link>
        </section>
      ) : isMissingToken ? (
        <div className="auth-missing-token">
          <div className="auth-alert" role="alert">
            A valid password reset token is required. Please open the link received in your email or request a new reset link.
          </div>
          <div className="d-flex flex-column gap-2 mt-3">
            <Link className="auth-primary-action" to="/forgot-password">
              <span>Request New Reset Link</span>
              <ArrowIcon />
            </Link>
          </div>
        </div>
      ) : (
        <>
          {activeError && (
            <div className="auth-alert" id="reset-password-error" role="alert">
              {activeError}
            </div>
          )}
          <form
            className="auth-form"
            onSubmit={handleSubmit}
            noValidate
            aria-label="Set new password"
          >
            <div className="auth-field">
              <label htmlFor="reset-new-password">New Password</label>
              <div className="auth-password" {...newPasswordPrivacy}>
                <input
                  type={showPassword ? 'text' : 'password'}
                  id="reset-new-password"
                  name="newPassword"
                  className="auth-input"
                  placeholder="Enter at least 8 characters"
                  autoComplete="new-password"
                  value={newPassword}
                  onChange={(event) => {
                    setNewPassword(event.target.value);
                    if (clientError) setClientError('');
                    if (apiError) setApiError(null);
                  }}
                  disabled={isSubmitting}
                  maxLength={128}
                  aria-invalid={Boolean(activeError)}
                  aria-describedby={activeError ? 'reset-password-error' : undefined}
                  required
                />
                <button
                  type="button"
                  className="auth-password__toggle"
                  onClick={() => setShowPassword((prev) => !prev)}
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                  disabled={isSubmitting}
                >
                  <EyeIcon hidden={showPassword} />
                </button>
              </div>
            </div>

            <div className="auth-field">
              <label htmlFor="reset-confirm-password">Confirm New Password</label>
              <div className="auth-password" {...confirmPasswordPrivacy}>
                <input
                  type={showConfirmPassword ? 'text' : 'password'}
                  id="reset-confirm-password"
                  name="confirmPassword"
                  className="auth-input"
                  placeholder="Re-enter your new password"
                  autoComplete="new-password"
                  value={confirmPassword}
                  onChange={(event) => {
                    setConfirmPassword(event.target.value);
                    if (clientError) setClientError('');
                    if (apiError) setApiError(null);
                  }}
                  disabled={isSubmitting}
                  maxLength={128}
                  aria-invalid={Boolean(activeError)}
                  aria-describedby={activeError ? 'reset-password-error' : undefined}
                  required
                />
                <button
                  type="button"
                  className="auth-password__toggle"
                  onClick={() => setShowConfirmPassword((prev) => !prev)}
                  aria-label={showConfirmPassword ? 'Hide password' : 'Show password'}
                  disabled={isSubmitting}
                >
                  <EyeIcon hidden={showConfirmPassword} />
                </button>
              </div>
            </div>

            <button
              type="submit"
              className="auth-primary-action"
              disabled={isSubmitting}
            >
              <span>
                {isSubmitting && <span className="auth-spinner" aria-hidden="true" />}
                {isSubmitting ? 'Updating password…' : 'Reset password'}
              </span>
              {!isSubmitting && <ArrowIcon />}
            </button>
          </form>
        </>
      )}

      <footer className="auth-panel__footer">
        <span>Need a new link?</span>
        <Link to="/forgot-password">Request password reset</Link>
        <Link to="/login">Sign in</Link>
      </footer>
    </article>
  );
}
