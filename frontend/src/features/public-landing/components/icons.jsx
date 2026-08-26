import React from 'react';

const base = {
  xmlns: 'http://www.w3.org/2000/svg',
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.7,
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
  'aria-hidden': 'true',
  focusable: 'false',
};

export function BrandCross(props) {
  return (
    <svg {...base} {...props}>
      <path d="M12 5v14M5 12h14" strokeWidth={2.2} />
    </svg>
  );
}
export function ArrowRight(props) {
  return (
    <svg {...base} {...props}>
      <path d="M5 12h14" />
      <path d="M13 5l7 7-7 7" />
    </svg>
  );
}
export function ArrowDown(props) {
  return (
    <svg {...base} {...props}>
      <path d="M12 5v14" />
      <path d="M5 13l7 7 7-7" />
    </svg>
  );
}
export function LockShield(props) {
  return (
    <svg {...base} {...props}>
      <path d="M12 22s7-3.2 7-9V6l-7-2.8L5 6v7c0 5.8 7 9 7 9z" />
      <path d="M12 11.5a1.15 1.15 0 100 .1" strokeWidth={2} />
      <path d="M12 12.8V15" />
    </svg>
  );
}
export function CheckMini(props) {
  return (
    <svg {...base} {...props}>
      <path d="M5 13l4 4 9-9" strokeWidth={2} />
    </svg>
  );
}
export function DotIcon(props) {
  return (
    <svg viewBox="0 0 8 8" width="8" height="8" aria-hidden="true" focusable="false" {...props}>
      <circle cx="4" cy="4" r="2.8" fill="currentColor" />
    </svg>
  );
}
