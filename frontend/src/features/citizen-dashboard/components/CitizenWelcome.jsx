import React from 'react';
import CitizenPageHeader from '../../citizen-ui/components/CitizenPageHeader';

export default function CitizenWelcome({ user }) {
  const displayName = user?.fullName || user?.email || 'Citizen';

  return (
    <CitizenPageHeader
      variant="hero"
      eyebrow="Citizen Dashboard"
      title={`Welcome, ${displayName}`}
      titleId="citizen-dashboard-title"
      subtitle="Review your screening and appointment records, then move directly to the service you need."
      badge="Secure self-service"
      meta={<><strong>Citizen account</strong>{user?.email && <small>{user.email}</small>}</>}
    />
  );
}
