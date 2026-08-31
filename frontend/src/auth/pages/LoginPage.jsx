import React, { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getRoleHomePath } from '../utils/roleRedirect';

const LOGIN_CONTENT = {
  citizen: {
    heading: 'Citizen Login',
    description: 'Log in with your citizen account.',
  },
  staff: {
    heading: 'Staff Login',
    description: 'Log in with your approved staff or administrator account.',
  },
  generic: {
    heading: 'Login to Vaccination Portal',
    description: 'Use your citizen or authorized staff account.',
  },
};

export default function LoginPage({ audience = 'generic', onBackToLanding }) {
  const { login, loginError, clearLoginError } = useAuth();
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const routeAudience = pathname === '/login/citizen'
    ? 'citizen'
    : pathname === '/login/staff'
      ? 'staff'
      : audience;
  const content = LOGIN_CONTENT[routeAudience] || LOGIN_CONTENT.generic;

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
        navigate(getRoleHomePath(result.user), { replace: true });
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
                <h1 className="h3 text-primary fw-bold mb-2">{content.heading}</h1>
                <p className="text-muted small">{content.description}</p>
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
                        Logging in...
                      </>
                    ) : (
                      'Login'
                    )}
                  </button>
                </div>
              </form>

              <div className="text-center mt-3">
                {routeAudience === 'citizen' && (
                  <small className="text-muted">Need a citizen account? <Link to="/register/citizen">Create Citizen Account</Link></small>
                )}
                {routeAudience === 'staff' && (
                  <small className="text-muted">Need staff access? <Link to="/register/staff">Request a Staff Account</Link></small>
                )}
                {routeAudience === 'generic' && (
                  <small className="text-muted">Choose citizen or staff access from the portal home page.</small>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
