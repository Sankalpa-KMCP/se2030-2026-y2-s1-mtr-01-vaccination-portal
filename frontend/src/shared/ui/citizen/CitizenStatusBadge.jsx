import React from 'react';

export default function CitizenStatusBadge({ children, tone = 'neutral' }) {
  if (!children) return null;
  return <span className={`citizen-ui-status citizen-ui-status--${tone}`}>{children}</span>;
}
