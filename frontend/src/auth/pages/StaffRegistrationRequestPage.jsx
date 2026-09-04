import React, { useState } from 'react';
import { registerStaff } from '../api/authApi';
import AccountRegistrationForm from '../components/AccountRegistrationForm';
import { AuthRegistrationLayout, AuthSuccessState } from '../components/AuthRegistrationLayout';

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
    <AuthRegistrationLayout audience="staff">
      {submitted ? (
        <AuthSuccessState audience="staff" />
      ) : (
        <AccountRegistrationForm
          accountType="staff"
          apiError={apiError}
          isSubmitting={isSubmitting}
          onDismissError={() => setApiError(null)}
          onSubmit={handleSubmit}
        />
      )}
    </AuthRegistrationLayout>
  );
}
