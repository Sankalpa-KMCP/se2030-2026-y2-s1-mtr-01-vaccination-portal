import React, { useState } from 'react';
import { registerCitizen } from '../api/authApi';
import AccountRegistrationForm from '../components/AccountRegistrationForm';
import { AuthRegistrationLayout, AuthSuccessState } from '../components/AuthRegistrationLayout';

export default function CitizenRegistrationPage() {
  const [apiError, setApiError] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);

  const handleSubmit = async (registration) => {
    setApiError(null);
    setIsSubmitting(true);
    try {
      await registerCitizen(registration);
      setSubmitted(true);
    } catch (error) {
      setApiError(error);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <AuthRegistrationLayout audience="citizen">
      {submitted ? (
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
