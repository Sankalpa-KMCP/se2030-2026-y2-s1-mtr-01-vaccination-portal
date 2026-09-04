import React, { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getRoleHomePath } from '../utils/roleRedirect';
import PortalAccessCard from '../components/PortalAccessCard';
import './LoginPage.css';

const LOGIN_CONTENT = {
  citizen: {
    heading: 'Citizen Login',
    badge: 'Citizen Access',
    description: 'Log in with your citizen account.',
    subtext: 'Manage your vaccination appointments, check campaign eligibility, and access immunization records.',
  },
  staff: {
    heading: 'Staff Login',
    badge: 'Staff Operations',
    description: 'Log in with your approved staff or administrator account.',
    subtext: 'Authorized workspace for clinic sessions, campaign coordination, inventory management, and oversight.',
  },
  generic: {
    heading: 'Login to Vaccination Portal',
    badge: 'Universal Access Hub',
    description: 'Use your citizen or authorized staff account.',
    subtext: 'Sign in with your email and password to access citizen services or your operational workspace.',
  },
};

function BrandShieldIcon({ width = 24, height = 24 }) {
  return (
    <svg width={width} height={height} viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path
        d="M12 2.5 4.5 5.4v5.3c0 4.7 3.1 8.9 7.5 10.3 4.4-1.4 7.5-5.6 7.5-10.3V5.4L12 2.5Z"
        stroke="currentColor"
        strokeWidth="1.7"
        strokeLinejoin="round"
      />
      <path
        d="M8.6 12.1v2.6h1.9v-2.6h2.8v2.6h1.9v-2.6h1.6v-1.7H8.6v1.7Z"
        fill="currentColor"
      />
    </svg>
  );
}

function CitizenIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2" />
      <circle cx="12" cy="7" r="4" />
    </svg>
  );
}

function StaffIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <rect width="20" height="14" x="2" y="7" rx="2" ry="2" />
      <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16" />
      <path d="M12 11v4" />
      <path d="M10 13h4" />
    </svg>
  );
}

function CheckCircleIcon() {
  return (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M20 6 9 17l-5-5" />
    </svg>
  );
}

function EyeIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z" />
      <circle cx="12" cy="12" r="3" />
    </svg>
  );
}

function EyeOffIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M9.88 9.88a3 3 0 1 0 4.24 4.24" />
      <path d="M10.73 5.08A10.43 10.43 0 0 1 12 5c7 0 10 7 10 7a13.16 13.16 0 0 1-1.67 2.68" />
      <path d="M6.61 6.61A13.526 13.526 0 0 0 2 12s3 7 10 7a9.74 9.74 0 0 0 5.39-1.61" />
      <line x1="2" x2="22" y1="2" y2="22" />
    </svg>
  );
}

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
  const isGenericHub = routeAudience === 'generic';

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [clientError, setClientError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleBack = () => {
    if (onBackToLanding) {
      onBackToLanding();
    } else {
      navigate('/');
    }
  };

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
    <div className="login-page-wrap">
      <div className="login-access-hub">
        {/* Left Contextual Brand Panel */}
        <section className="login-access-hub__brand" aria-label="Portal Context">
          <div className="login-access-hub__brand-header">
            <div className="login-access-hub__emblem-tag">
              <BrandShieldIcon width={16} height={16} />
              <span>Official Public Health Service</span>
            </div>

            <div className="login-access-hub__brand-identity">
              <div className="login-access-hub__brand-glyph">
                <BrandShieldIcon width={32} height={32} />
              </div>
              <h2 className="login-access-hub__portal-name">Vaccination Portal</h2>
            </div>

            <p className="login-access-hub__tagline">
              National Immunization Gateway &amp; Clinical Operations Management
            </p>

            <ul className="login-access-hub__assurances">
              <li className="login-access-hub__assurance-item">
                <div className="login-access-hub__assurance-icon">
                  <CheckCircleIcon />
                </div>
                <div>
                  <div className="login-access-hub__assurance-title">Unified Universal Access</div>
                  <p className="login-access-hub__assurance-desc">
                    Citizens and authorized staff authenticate through this secure access gateway.
                  </p>
                </div>
              </li>

              <li className="login-access-hub__assurance-item">
                <div className="login-access-hub__assurance-icon">
                  <CheckCircleIcon />
                </div>
                <div>
                  <div className="login-access-hub__assurance-title">Citizen Self-Service</div>
                  <p className="login-access-hub__assurance-desc">
                    Book appointments, complete campaign screening questionnaires, and access history.
                  </p>
                </div>
              </li>

              <li className="login-access-hub__assurance-item">
                <div className="login-access-hub__assurance-icon">
                  <CheckCircleIcon />
                </div>
                <div>
                  <div className="login-access-hub__assurance-title">Clinical &amp; Operational Governance</div>
                  <p className="login-access-hub__assurance-desc">
                    Role-governed workflows for health officers, campaign officers, and administrators.
                  </p>
                </div>
              </li>
            </ul>
          </div>

          <div className="login-access-hub__brand-footer">
            <span>Protected public health platform. All access events are audited and verified.</span>
          </div>

          <svg
            className="login-access-hub__brand-watermark"
            viewBox="0 0 200 200"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
            aria-hidden="true"
          >
            <circle cx="100" cy="100" r="90" stroke="currentColor" strokeWidth="2" strokeDasharray="6 6" />
            <circle cx="100" cy="100" r="65" stroke="currentColor" strokeWidth="1.5" />
            <path d="M100 35v130M35 100h130" stroke="currentColor" strokeWidth="2" />
          </svg>
        </section>

        {/* Right Authentication & Access Section */}
        <main className="login-access-hub__main" id="main-content">
          <div className="login-access-hub__top-nav">
            <button
              type="button"
              className="login-back-btn"
              onClick={handleBack}
              aria-label="Back to portal home"
            >
              <span aria-hidden="true">&larr;</span>
              <span>Back to portal home</span>
            </button>
            <span className="login-hub-badge">{content.badge}</span>
          </div>

          <header className="mb-3">
            <h1 className="login-hub-title">{content.heading}</h1>
            <p className="login-hub-desc">{content.description}</p>
          </header>

          {activeError && (
            <div
              className="alert alert-danger py-2 px-3 small border-0 shadow-sm mb-4"
              role="alert"
            >
              {activeError}
            </div>
          )}

          {/* Universal Login Form */}
          <form onSubmit={handleSubmit} noValidate aria-label="Sign in credentials">
            <div className="login-form-group">
              <label htmlFor="login-email" className="login-form-label">
                Email Address
              </label>
              <input
                type="email"
                id="login-email"
                name="email"
                className="login-input"
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

            <div className="login-form-group">
              <label htmlFor="login-password" className="login-form-label">
                Password
              </label>
              <div className="login-password-wrapper">
                <input
                  type={showPassword ? 'text' : 'password'}
                  id="login-password"
                  name="password"
                  className="login-input"
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
                <button
                  type="button"
                  className="login-password-toggle"
                  onClick={() => setShowPassword(!showPassword)}
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                  title={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? <EyeOffIcon /> : <EyeIcon />}
                </button>
              </div>
            </div>

            <button
              type="submit"
              className="btn btn-primary w-100 login-submit-btn mb-2"
              disabled={isSubmitting}
            >
              {isSubmitting ? (
                <>
                  <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true" />
                  <span>Logging in...</span>
                </>
              ) : (
                'Login'
              )}
            </button>
          </form>

          {/* Guided Access Cards on Universal / Generic Route */}
          {isGenericHub && (
            <>
              <div className="login-hub-divider" role="separator">
                <span>Prefer a guided portal?</span>
              </div>

              <section className="access-choices-grid" aria-label="Guided portal options">
                <PortalAccessCard
                  title="Citizen Portal"
                  purpose="Appointments, eligibility, vaccination services, personal records."
                  to="/login/citizen"
                  ctaLabel="Continue as Citizen"
                  badgeText="Public"
                  badgeVariant="teal"
                  icon={<CitizenIcon />}
                />

                <PortalAccessCard
                  title="Staff Portal"
                  purpose="Authorized operational, campaign, clinical, inventory, administrative, or management access."
                  to="/login/staff"
                  ctaLabel="Continue as Staff"
                  badgeText="Staff Only"
                  badgeVariant="slate"
                  icon={<StaffIcon />}
                />
              </section>

              {/* Registration Section */}
              <section className="login-registration-section" aria-labelledby="reg-heading">
                <h2 id="reg-heading" className="login-registration-heading">
                  <span>New to the portal?</span>
                </h2>
                <div className="login-registration-actions">
                  <Link
                    to="/register/citizen"
                    className="login-reg-link"
                    aria-label="Register as Citizen (immediate activation)"
                  >
                    <span>Register as Citizen</span>
                    <span aria-hidden="true">&rarr;</span>
                  </Link>

                  <Link
                    to="/register/staff"
                    className="login-reg-link"
                    aria-label="Register as Staff (requires administrator approval)"
                  >
                    <span>Register as Staff</span>
                    <span aria-hidden="true">&rarr;</span>
                  </Link>
                </div>
                <p className="login-registration-notice">
                  Citizens can create an account immediately. Staff registration may require administrator approval before account access.
                </p>
              </section>
            </>
          )}

          {/* Sub-route Navigation & Registrations (/login/citizen and /login/staff) */}
          {!isGenericHub && (
            <div className="login-subroute-nav">
              {routeAudience === 'citizen' && (
                <>
                  <div className="login-subroute-switch">
                    <span>Need a citizen account? </span>
                    <Link to="/register/citizen">Create Citizen Account</Link>
                  </div>
                  <div className="login-subroute-switch text-muted small">
                    <span>Need staff access? </span>
                    <Link to="/login/staff">Staff Login</Link>
                    <span> &bull; </span>
                    <Link to="/login">Universal Login</Link>
                  </div>
                </>
              )}

              {routeAudience === 'staff' && (
                <>
                  <div className="login-subroute-switch">
                    <span>Need staff access? </span>
                    <Link to="/register/staff">Request a Staff Account</Link>
                  </div>
                  <p className="login-registration-notice mt-1">
                    Staff registration may require administrator approval before account access.
                  </p>
                  <div className="login-subroute-switch text-muted small">
                    <span>Need citizen access? </span>
                    <Link to="/login/citizen">Citizen Login</Link>
                    <span> &bull; </span>
                    <Link to="/login">Universal Login</Link>
                  </div>
                </>
              )}
            </div>
          )}
        </main>
      </div>
    </div>
  );
}
