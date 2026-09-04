import React from 'react';

/**
 * Hero line artwork: a two-track service schematic connecting staff
 * operations with the citizen journey from account to vaccination record.
 * Decorative only (aria-hidden).
 */
export default function HeroArtwork() {
  return (
    <svg
      className="home-pathway"
      viewBox="0 0 680 390"
      fill="none"
      role="presentation"
      focusable="false"
    >
      <g className="home-pathway__grid" stroke="currentColor" strokeWidth="1">
        <path d="M24 70H656M24 150H656M24 230H656M24 310H656" />
        <path d="M100 34V352M226 34V352M352 34V352M478 34V352M604 34V352" />
      </g>
      <g className="home-pathway__labels">
        <text x="24" y="48">STAFF OPERATIONS</text>
        <text x="24" y="336">CITIZEN JOURNEY</text>
      </g>
      <g className="home-pathway__route" stroke="currentColor" strokeWidth="2" pathLength="1">
        <path pathLength="1" d="M75 270H604" />
        <path pathLength="1" d="M100 115H478" />
        <path pathLength="1" d="M100 115V270M226 115V270M352 115V270M478 115V270" />
      </g>
      <g className="home-pathway__staff-node home-pathway__node--1">
        <rect x="60" y="86" width="80" height="58" />
        <path d="M76 101h22M76 108h36" />
        <text x="76" y="129">CAMPAIGN</text>
      </g>
      <g className="home-pathway__staff-node home-pathway__node--2">
        <rect x="186" y="86" width="80" height="58" />
        <path d="M208 104h36M226 95v18" />
        <text x="203" y="129">CENTER</text>
      </g>
      <g className="home-pathway__staff-node home-pathway__node--3">
        <rect x="312" y="86" width="80" height="58" />
        <path d="M328 103h48M328 112h32" />
        <text x="329" y="129">SESSION</text>
      </g>
      <g className="home-pathway__staff-node home-pathway__node--4">
        <rect x="438" y="86" width="80" height="58" />
        <path d="M462 101h32M454 110h48" />
        <text x="454" y="129">READINESS</text>
      </g>
      <g className="home-pathway__citizen-node home-pathway__node--5">
        <circle cx="75" cy="270" r="22" />
        <circle cx="75" cy="263" r="5" />
        <path d="M64 281c2-7 7-10 11-10s9 3 11 10" />
        <text x="54" y="309">ACCOUNT</text>
      </g>
      <g className="home-pathway__citizen-node home-pathway__node--6">
        <circle cx="226" cy="270" r="22" />
        <path d="M216 270l7 7 14-16" />
        <text x="194" y="309">ELIGIBILITY</text>
      </g>
      <g className="home-pathway__citizen-node home-pathway__node--7">
        <circle cx="352" cy="270" r="22" />
        <rect x="342" y="262" width="20" height="17" />
        <path d="M342 268h20M347 258v7M357 258v7" />
        <text x="316" y="309">APPOINTMENT</text>
      </g>
      <g className="home-pathway__dose-node home-pathway__node--8">
        <circle cx="478" cy="270" r="25" />
        <path d="M474 258h8M475 258v5h6v-5M474 263h8v13a4 4 0 0 1-8 0zM474 270h8" />
        <text x="464" y="309">DOSE</text>
      </g>
      <g className="home-pathway__citizen-node home-pathway__node--9">
        <circle cx="604" cy="270" r="22" />
        <path d="M596 258h13l5 5v18h-18zM600 269h10M600 274h10" />
        <text x="584" y="309">RECORD</text>
      </g>
      <path className="home-pathway__completion" pathLength="1" d="M604 248V188H540" />
      <text className="home-pathway__annotation" x="538" y="177">CARE RECORDED</text>
    </svg>
  );
}
