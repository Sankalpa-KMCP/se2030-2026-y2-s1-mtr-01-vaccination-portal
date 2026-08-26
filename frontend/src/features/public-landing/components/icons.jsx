/**
 * Inline SVG icon set for the public landing page.
 * 24px grid, ~1.75 stroke, currentColor, round caps/joins.
 * Stage 1 scope: brand mark, calendar, shield-check, gauge, arrow, lock.
 */

const baseProps = {
  xmlns: 'http://www.w3.org/2000/svg',
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.75,
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
  'aria-hidden': 'true',
  focusable: 'false',
};

export function BrandMark(props) {
  return (
    <svg {...baseProps} {...props}>
      <rect x="3" y="3" width="18" height="18" rx="5" />
      <path d="M12 8v8M8 12h8" />
    </svg>
  );
}

export function CalendarIcon(props) {
  return (
    <svg {...baseProps} {...props}>
      <rect x="3" y="4.5" width="18" height="16" rx="2.5" />
      <path d="M8 2.5v4M16 2.5v4M3 9.5h18" />
    </svg>
  );
}

export function ShieldCheckIcon(props) {
  return (
    <svg {...baseProps} {...props}>
      <path d="M12 3l7 3v5.2c0 4.3-2.9 7.4-7 8.8-4.1-1.4-7-4.5-7-8.8V6l7-3z" />
      <path d="M9.2 11.9l2 2 3.7-4.1" />
    </svg>
  );
}

export function GaugeIcon(props) {
  return (
    <svg {...baseProps} {...props}>
      <path d="M4.9 16.6a8 8 0 1 1 14.2 0" />
      <path d="M12 13.5l3.2-3.2" />
      <circle cx="12" cy="14" r="1.2" fill="currentColor" stroke="none" />
    </svg>
  );
}

export function ArrowRightIcon(props) {
  return (
    <svg {...baseProps} {...props}>
      <path d="M4 12h15" />
      <path d="M13 6l6 6-6 6" />
    </svg>
  );
}

export function LockIcon(props) {
  return (
    <svg {...baseProps} {...props}>
      <rect x="5" y="11" width="14" height="9" rx="2" />
      <path d="M8 11V8a4 4 0 0 1 8 0v3" />
    </svg>
  );
}

export function FlagIcon(props) {
  return (
    <svg {...baseProps} {...props}>
      <path d="M5 21V4" />
      <path d="M5 4h11l-2 3.5L16 11H5" />
    </svg>
  );
}

export function BuildingIcon(props) {
  return (
    <svg {...baseProps} {...props}>
      <rect x="4" y="3" width="12" height="18" rx="1.5" />
      <path d="M16 9h3.5a.5.5 0 0 1 .5.5V21" />
      <path d="M7.5 7h2M10.5 7h2M7.5 11h2M10.5 11h2M7.5 15h2M10.5 15h2M9 21v-3h2v3" />
      <path d="M3 21h18" />
    </svg>
  );
}

export function CalendarClockIcon(props) {
  return (
    <svg {...baseProps} {...props}>
      <path d="M15.5 4.5H5a1.5 1.5 0 0 0-1.5 1.5v13A1.5 1.5 0 0 0 5 20.5h6" />
      <path d="M14.5 2.5v4M6.5 2.5v4M3.5 9.5h12" />
      <circle cx="17.5" cy="17" r="4.5" />
      <path d="M17.5 15.2V17l1.3 1" />
    </svg>
  );
}

export function ArrowDownIcon(props) {
  return (
    <svg {...baseProps} {...props}>
      <path d="M12 4v15" />
      <path d="M6 13l6 6 6-6" />
    </svg>
  );
}
