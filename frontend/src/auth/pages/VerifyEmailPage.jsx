import React, { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { verifyEmail } from '../api/authApi';
import { ArrowIcon, SuccessSealIcon } from '../components/AuthIcons';
import ResendVerificationForm from '../components/ResendVerificationForm';

const VERIFY_ERROR_MESSAGES = {
  INVALID_TOKEN: 'This verification link is invalid or unrecognized. Please request a new verification email.',
  TOKEN_EXPIRED: 'This verification link has expired. Please request a new verification email.',
  TOKEN_ALREADY_USED: 'This verification link has already been used. If your account is already active, please sign in.',
};

function mapVerifyError(error) {
  const code = error?.data?.code;
  if (code && VERIFY_ERROR_MESSAGES[code]) {
    return VERIFY_ERROR_MESSAGES[code];
  }
  return error?.message || 'Failed to verify email address. Please request a new verification email.';
}

export default function VerifyEmailPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') || '';

  const [isLoading, setIsLoading] = useState(Boolean(token.trim()));
  const [isSuccess, setIsSuccess] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState(
    token.trim() ? '' : 'No verification token provided. Please check the link in your email.',
  );
  const [showResendForm, setShowResendForm] = useState(false);

  const verificationAttemptedRef = useRef(false);

  const cleanUrlToken = () => {
    if (typeof window !== 'undefined' && window.history?.replaceState) {
      window.history.replaceState({}, document.title, window.location.pathname);
    }
  };

  useEffect(() => {
    if (!token.trim()) {
      setIsLoading(false);
      return;
    }

    if (verificationAttemptedRef.current) {
      return;
    }
    verificationAttemptedRef.current = true;

    async function executeVerification() {
      setIsLoading(true);
      try {
        const response = await verifyEmail(token);
        cleanUrlToken();
        setSuccessMessage(response?.message || 'Email verified successfully.');
        setIsSuccess(true);
        setErrorMessage('');
      } catch (error) {
        cleanUrlToken();
        setErrorMessage(mapVerifyError(error));
        setIsSuccess(false);
        setShowResendForm(true);
      } finally {
        setIsLoading(false);
      }
    }

    executeVerification();
  }, [token]);

  return (
    <article className="auth-panel auth-panel--verify-email">
      <header className="auth-panel__header">
        <p className="auth-panel__eyebrow">Account verification</p>
        <h1 tabIndex="-1">Email verification</h1>
        <p className="auth-panel__description">
          Confirming your email address to secure your citizen vaccination account.
        </p>
      </header>

      {isLoading ? (
        <section className="auth-loading text-center py-5" role="status" aria-live="polite">
          <div className="spinner-border text-primary mb-3" role="status">
            <span className="visually-hidden">Loading...</span>
          </div>
          <p className="text-muted">Verifying your email address, please wait…</p>
        </section>
      ) : isSuccess ? (
        <section className="auth-success" role="status">
          <div className="auth-success__seal">
            <SuccessSealIcon />
          </div>
          <p>Email verified</p>
          <h2>Email verified successfully</h2>
          <span>
            {successMessage} Your citizen account is now active and ready. You can sign in to book appointments and access records.
          </span>
          <Link className="auth-primary-action" to="/login">
            <span>Continue to Sign in</span>
            <ArrowIcon />
          </Link>
        </section>
      ) : (
        <section className="auth-error-state" role="alert">
          <div className="auth-alert mb-4">
            {errorMessage}
          </div>

          <div className="d-flex flex-column gap-3">
            {showResendForm ? (
              <div className="p-3 border rounded bg-white">
                <h2 className="h6 mb-2">Need a new verification link?</h2>
                <p className="small text-muted mb-3">
                  Enter your email address below to receive another verification link.
                </p>
                <ResendVerificationForm compact />
              </div>
            ) : (
              <button
                type="button"
                className="btn btn-outline-primary"
                onClick={() => setShowResendForm(true)}
              >
                Resend verification email
              </button>
            )}

            <Link className="auth-primary-action mt-2" to="/login">
              <span>Return to Sign in</span>
              <ArrowIcon />
            </Link>
          </div>
        </section>
      )}

      <footer className="auth-panel__footer">
        <span>Already verified?</span>
        <Link to="/login">Sign in</Link>
      </footer>
    </article>
  );
}
