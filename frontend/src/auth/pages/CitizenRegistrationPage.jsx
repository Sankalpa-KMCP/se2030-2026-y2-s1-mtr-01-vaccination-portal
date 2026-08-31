import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { registerCitizen } from '../api/authApi';
import AccountRegistrationForm from '../components/AccountRegistrationForm';
import { useAuth } from '../context/AuthContext';
import { getRoleHomePath } from '../utils/roleRedirect';

export default function CitizenRegistrationPage() {
  const { login, clearLoginError } = useAuth();
  const navigate = useNavigate();
  const [apiError, setApiError] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [createdWithoutLogin, setCreatedWithoutLogin] = useState(false);

  const handleSubmit = async (registration) => {
    setApiError(null);
    setIsSubmitting(true);
    clearLoginError();
    try {
      await registerCitizen(registration);
      const result = await login(registration.email, registration.password);
      if (result.success) {
        navigate(getRoleHomePath(result.user), { replace: true });
        return;
      }
      setCreatedWithoutLogin(true);
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
                <span className="badge text-bg-primary mb-2">Citizen Portal</span>
                <h1 className="h2 mb-2">Create Citizen Account</h1>
                <p className="text-muted mb-0">Create your account to access citizen vaccination services.</p>
              </div>

              {createdWithoutLogin ? (
                <div className="alert alert-success mb-0" role="status">
                  <h2 className="h5 alert-heading">Account created successfully</h2>
                  <p>Please log in to continue to your citizen dashboard.</p>
                  <Link className="btn btn-success" to="/login/citizen">Go to Citizen Login</Link>
                </div>
              ) : (
                <AccountRegistrationForm
                  accountType="citizen"
                  apiError={apiError}
                  isSubmitting={isSubmitting}
                  onDismissError={() => setApiError(null)}
                  onSubmit={handleSubmit}
                />
              )}

              {!createdWithoutLogin && (
                <p className="text-center text-muted small mt-4 mb-0">
                  Already have an account? <Link to="/login/citizen">Citizen Login</Link>
                </p>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
