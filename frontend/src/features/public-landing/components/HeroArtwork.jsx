import React from 'react';

export default function HeroArtwork() {
  return (
    <svg
      className="home-pathway"
      viewBox="0 0 680 520"
      fill="none"
      role="presentation"
      focusable="false"
    >
      <defs>
        <radialGradient id="care-core" cx="35%" cy="28%" r="76%">
          <stop offset="0" stopColor="#fff" stopOpacity=".95" />
          <stop offset=".58" stopColor="#e8e2f0" stopOpacity=".86" />
          <stop offset="1" stopColor="#8b80a8" stopOpacity=".42" />
        </radialGradient>
        <filter id="care-shadow" x="-40%" y="-40%" width="180%" height="180%">
          <feGaussianBlur stdDeviation="16" />
        </filter>
      </defs>
      <ellipse className="home-pathway__shadow" cx="340" cy="422" rx="208" ry="32" filter="url(#care-shadow)" />
      <g className="home-pathway__orbits">
        <ellipse cx="340" cy="260" rx="235" ry="152" transform="rotate(-8 340 260)" />
        <ellipse cx="340" cy="260" rx="168" ry="232" transform="rotate(58 340 260)" />
      </g>
      <circle className="home-pathway__core" cx="340" cy="260" r="84" fill="url(#care-core)" />
      <circle className="home-pathway__halo" cx="340" cy="260" r="106" />
      <g className="home-pathway__forms">
        <rect x="97" y="131" width="82" height="82" rx="34" transform="rotate(-18 138 172)" />
        <circle cx="171" cy="369" r="43" />
        <rect x="302" y="424" width="82" height="60" rx="22" />
        <rect x="499" y="326" width="76" height="82" rx="38" transform="rotate(19 537 367)" />
        <circle cx="527" cy="151" r="44" />
      </g>
      <g className="home-pathway__glints">
        <circle cx="307" cy="221" r="7" />
        <circle cx="377" cy="291" r="4" />
        <path d="M340 193v18M331 202h18" />
      </g>
    </svg>
  );
}
