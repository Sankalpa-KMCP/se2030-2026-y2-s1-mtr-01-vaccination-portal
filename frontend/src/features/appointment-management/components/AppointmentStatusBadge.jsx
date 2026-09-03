import React from 'react';

/**
 * Reusable badge component mapping AppointmentStatus enum values to semantic Bootstrap colors.
 */
export default function AppointmentStatusBadge({ status }) {
  if (!status) return null;

  const config = {
    CONFIRMED: { label: 'Confirmed', bgClass: 'bg-success' },
    CHECKED_IN: { label: 'Checked In', bgClass: 'bg-info text-dark' },
    COMPLETED: { label: 'Completed', bgClass: 'bg-primary' },
    CANCELLED: { label: 'Cancelled', bgClass: 'bg-secondary' },
    NO_SHOW: { label: 'No Show', bgClass: 'bg-danger' },
  };

  const badgeInfo = config[status] || { label: status, bgClass: 'bg-dark' };

  return (
    <span className={`badge ${badgeInfo.bgClass} px-3 py-2 fs-7 fw-bold shadow-sm`}>
      {badgeInfo.label}
    </span>
  );
}
