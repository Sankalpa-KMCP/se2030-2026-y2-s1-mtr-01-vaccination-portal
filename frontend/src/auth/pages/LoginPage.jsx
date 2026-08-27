import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';

export default function LoginPage({ onBackToLanding }) {
  const { login, loginError, clearLoginError } = useAuth();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [clientError, setClientError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setClientError('');
    clearLoginError();

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

    if (!password) {
      setClientError('Password is required.');
      return;
    }

    if (password.length > 128) {
      setClientError('Password must not exceed 128 characters.');
      return;
    }

    setIsSubmitting(true);
    try {
      const result = await login(trimmedEmail, password);
      if (result.success) {
        setPassword('');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const activeError = clientError || loginError;

  return (
    <div className="container py-5">
      <div className="row justify-content-center">
        <div className="col-12 col-sm-10 col-md-8 col-lg-5">
          {onBackToLanding && (
            <div className="mb-3">
              <button
                type="button"
                className="btn btn-link btn-sm text-decoration-none p-0"
                onClick={onBackToLanding}
              >
                &larr; Back to portal home
              </button>
            </div>
          )}
          <div className="card shadow-sm border-0">
            <div className="card-body p-4 p-md-5">
              <div className="text-center mb-4">
                <h1 className="h3 text-primary fw-bold mb-2">Vaccination Portal</h1>
                <p className="text-muted small">Sign in to access your operational account</p>
              </div>

              {activeError && (
                <div className="alert alert-danger py-2 px-3 small border-0 shadow-sm mb-4" role="alert">
                  {activeError}
                </div>
              )}

              <form onSubmit={handleSubmit} noValidate>
                <div className="mb-3">
                  <label htmlFor="login-email" className="form-label small fw-semibold text-secondary">
                    Email Address
                  </label>
                  <input
                    type="email"
                    id="login-email"
                    name="email"
                    className="form-control"
                    placeholder="name@example.gov"
                    autoComplete="email"
                    value={email}
                    onChange={(e) => {
                      setEmail(e.target.value);
                      if (clientError) setClientError('');
                      if (loginError) clearLoginError();
                    }}
                    disabled={isSubmitting}
                    maxLength={255}
                    required
                  />
                </div>

                <div className="mb-4">
                  <label htmlFor="login-password" className="form-label small fw-semibold text-secondary">
                    Password
                  </label>
                  <input
                    type="password"
                    id="login-password"
                    name="password"
                    className="form-control"
                    placeholder="••••••••"
                    autoComplete="current-password"
                    value={password}
                    onChange={(e) => {
                      setPassword(e.target.value);
                      if (clientError) setClientError('');
                      if (loginError) clearLoginError();
                    }}
                    disabled={isSubmitting}
                    maxLength={128}
                    required
                  />
                </div>

                <div className="d-grid mb-3">
                  <button
                    type="submit"
                    className="btn btn-primary fw-semibold py-2"
                    disabled={isSubmitting}
                  >
                    {isSubmitting ? (
                      <>
                        <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true" />
                        Signing in...
                      </>
                    ) : (
                      'Sign In'
                    )}
                  </button>
                </div>
              </form>

              <div className="text-center mt-3">
                <small className="text-muted">
                  Authorized healthcare & operations personnel only.
                </small>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
