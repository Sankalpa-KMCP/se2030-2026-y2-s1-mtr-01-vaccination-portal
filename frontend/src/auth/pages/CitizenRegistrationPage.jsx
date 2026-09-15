import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { registerCitizen } from '../api/authApi';
import AccountRegistrationForm from '../components/AccountRegistrationForm';
import { AuthRegistrationLayout, AuthSuccessState } from '../components/AuthRegistrationLayout';
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
    <AuthRegistrationLayout audience="citizen">
      {createdWithoutLogin ? (
        <AuthSuccessState audience="citizen" />
      ) : (
        <AccountRegistrationForm
          accountType="citizen"
          apiError={apiError}
          isSubmitting={isSubmitting}
          onDismissError={() => setApiError(null)}
          onSubmit={handleSubmit}
        />
      )}
    </AuthRegistrationLayout>
  );
}
