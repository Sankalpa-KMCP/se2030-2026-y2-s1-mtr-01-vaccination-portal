import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { registerStaff } from '../api/authApi';
import AccountRegistrationForm from '../components/AccountRegistrationForm';

export default function StaffRegistrationRequestPage() {
  const [apiError, setApiError] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);

  const handleSubmit = async (registration) => {
    setApiError(null);
    setIsSubmitting(true);
    try {
      await registerStaff(registration);
      setSubmitted(true);
    } catch (error) {
      setApiError(error);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="container py-5">
      <div className="row justify-content-center">
        <div className="col-12 col-lg-8 col-xl-7">
          <div className="mb-3">
            <Link className="btn btn-link btn-sm text-decoration-none p-0" to="/">&larr; Back to portal home</Link>
          </div>
          <div className="card border-0 shadow-sm">
            <div className="card-body p-4 p-md-5">
              <div className="mb-4">
                <span className="badge text-bg-secondary mb-2">Staff Portal</span>
                <h1 className="h2 mb-2">Request a Staff Account</h1>
                <p className="text-muted mb-0">Submit your details for administrator review and role approval.</p>
              </div>

              {submitted ? (
                <div className="alert alert-success mb-0" role="status">
                  <h2 className="h5 alert-heading">Request submitted</h2>
                  <p>Your staff account request has been submitted and is waiting for administrator approval.</p>
                  <Link className="btn btn-success" to="/login/staff">Go to Staff Login</Link>
                </div>
              ) : (
                <AccountRegistrationForm
                  accountType="staff"
                  apiError={apiError}
                  isSubmitting={isSubmitting}
                  onDismissError={() => setApiError(null)}
                  onSubmit={handleSubmit}
                />
              )}

              {!submitted && (
                <p className="text-center text-muted small mt-4 mb-0">
                  Already approved? <Link to="/login/staff">Staff Login</Link>
                </p>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
