import React from 'react';

const ICON_PATHS = {
  arrow: 'M5 12h14M14 6l6 6-6 6',
  calendar: 'M7 3v3m10-3v3M4 9h16M5 5h14a1 1 0 0 1 1 1v14H4V6a1 1 0 0 1 1-1Zm3 8h3v3H8v-3Z',
  check: 'm5 12 4 4L19 6',
  clipboard: 'M9 5h6M9 3h6v4H9V3ZM6 5H4v16h16V5h-2M8 12h8M8 16h6',
  history: 'M4 12a8 8 0 1 0 2.3-5.7L4 8.6M4 4v4.6h4.6M12 8v5l3 2',
  ticket: 'M4 6h16v4a2 2 0 0 0 0 4v4H4v-4a2 2 0 0 0 0-4V6Zm7 2v2m0 4v2',
  user: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm-7 9a7 7 0 0 1 14 0',
};

export default function DashboardIcon({ name, size = 22 }) {
  return (
    <svg
      aria-hidden="true"
      focusable="false"
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d={ICON_PATHS[name] || ICON_PATHS.arrow} />
    </svg>
  );
}
