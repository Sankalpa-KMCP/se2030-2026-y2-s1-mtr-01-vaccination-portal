import React from 'react';

export function ShieldMark({ size = 24 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path
        d="M12 2.5 4.5 5.4v5.3c0 4.7 3.1 8.9 7.5 10.3 4.4-1.4 7.5-5.6 7.5-10.3V5.4L12 2.5Z"
        stroke="currentColor"
        strokeWidth="1.7"
        strokeLinejoin="round"
      />
      <path d="M8.6 12.1v2.6h1.9v-2.6h2.8v2.6h1.9v-2.6h1.6v-1.7H8.6v1.7Z" fill="currentColor" />
    </svg>
  );
}

export function CitizenIcon({ size = 22 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.65" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <circle cx="12" cy="8" r="3.25" />
      <path d="M5.5 20c.35-4.1 2.5-6.15 6.5-6.15S18.15 15.9 18.5 20" />
      <path d="M8.2 4.6A6.6 6.6 0 0 1 12 3.4a6.6 6.6 0 0 1 3.8 1.2" opacity=".55" />
    </svg>
  );
}

export function StaffIcon({ size = 22 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.65" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M8.25 6V4.75c0-.95.8-1.75 1.75-1.75h4c.95 0 1.75.8 1.75 1.75V6" />
      <rect x="3" y="6" width="18" height="14" rx="1.5" />
      <path d="M3 10.5c2.8 1.55 5.8 2.3 9 2.3s6.2-.75 9-2.3M12 10.8v4" />
    </svg>
  );
}

export function EyeIcon({ hidden = false }) {
  if (hidden) {
    return (
      <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M9.9 9.9a3 3 0 0 0 4.2 4.2M10.7 5.1A10.4 10.4 0 0 1 12 5c7 0 10 7 10 7a13 13 0 0 1-1.7 2.7M6.6 6.6A13.5 13.5 0 0 0 2 12s3 7 10 7a9.7 9.7 0 0 0 5.4-1.6M2 2l20 20" />
      </svg>
    );
  }

  return (
    <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z" />
      <circle cx="12" cy="12" r="3" />
    </svg>
  );
}

export function ArrowIcon({ direction = 'right' }) {
  const transform = direction === 'left' ? 'rotate(180 12 12)' : undefined;
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" style={{ transform }}>
      <path d="M5 12h14M14 7l5 5-5 5" />
    </svg>
  );
}

export function SuccessSealIcon() {
  return (
    <svg width="30" height="30" viewBox="0 0 30 30" fill="none" aria-hidden="true">
      <circle cx="15" cy="15" r="13.5" stroke="currentColor" />
      <path d="m9.5 15.1 3.5 3.5 7.7-8" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}
