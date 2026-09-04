import React from 'react';

/**
 * A softer service trail connecting public actions with the operational
 * checks behind them. Decorative only; the same journey is described in HTML.
 */
export default function HeroArtwork() {
  return (
    <svg
      className="home-pathway"
      viewBox="0 0 720 430"
      fill="none"
      role="presentation"
      focusable="false"
    >
      <defs>
        <linearGradient id="home-trail-gradient" x1="58" y1="300" x2="660" y2="165">
          <stop stopColor="#13879A" />
          <stop offset="0.58" stopColor="#187BB8" />
          <stop offset="1" stopColor="#6E7FC7" />
        </linearGradient>
        <radialGradient id="home-node-glow" cx="35%" cy="28%" r="75%">
          <stop stopColor="#FFFFFF" />
          <stop offset="1" stopColor="#E8F4FA" />
        </radialGradient>
        <filter id="home-soft-shadow" x="-50%" y="-50%" width="200%" height="200%">
          <feDropShadow dx="0" dy="10" stdDeviation="11" floodColor="#183B59" floodOpacity="0.12" />
        </filter>
      </defs>

      <g className="home-pathway__atmosphere" aria-hidden="true">
        <circle cx="92" cy="286" r="94" />
        <circle cx="522" cy="164" r="124" />
        <path d="M20 370C180 318 300 408 470 350S632 292 704 320" />
      </g>

      <g className="home-pathway__labels">
        <text x="42" y="42">A CONNECTED CARE JOURNEY</text>
        <text x="678" y="42" textAnchor="end">PUBLIC ACTION + CLINIC READINESS</text>
      </g>

      <path className="home-pathway__route-shadow" d="M76 292C144 292 150 180 225 180S316 306 375 270 438 140 510 157 574 252 642 226" />
      <path className="home-pathway__route" d="M76 292C144 292 150 180 225 180S316 306 375 270 438 140 510 157 574 252 642 226" />

      <g className="home-pathway__staff-pill home-pathway__staff-pill--campaign">
        <rect x="44" y="82" width="190" height="56" rx="28" />
        <circle cx="76" cy="110" r="11" />
        <path d="M71 110h10M76 105v10" />
        <text x="98" y="106">CAMPAIGN PUBLISHED</text>
        <text className="home-pathway__pill-note" x="98" y="122">Rules and screening open</text>
      </g>
      <path className="home-pathway__staff-connector" d="M186 138C198 151 210 158 225 161" />

      <g className="home-pathway__staff-pill home-pathway__staff-pill--ready">
        <rect x="442" y="338" width="228" height="56" rx="28" />
        <circle cx="474" cy="366" r="11" />
        <path d="m468 366 4 4 8-9" />
        <text x="496" y="362">CLINIC READY</text>
        <text className="home-pathway__pill-note" x="496" y="378">Capacity and staff confirmed</text>
      </g>
      <path className="home-pathway__staff-connector" d="M510 191C519 246 512 298 488 338" />

      <g className="home-pathway__journey-node" transform="translate(76 292)">
        <circle className="home-pathway__node-halo" r="43" />
        <circle className="home-pathway__node-face" r="31" />
        <circle cy="-6" r="6" />
        <path d="M-13 15c3-10 9-15 13-15s10 5 13 15" />
        <text className="home-pathway__node-number" x="-30" y="-48">01</text>
        <text className="home-pathway__node-label" y="61" textAnchor="middle">ACCOUNT</text>
      </g>
      <g className="home-pathway__journey-node" transform="translate(225 180)">
        <circle className="home-pathway__node-halo" r="43" />
        <circle className="home-pathway__node-face" r="31" />
        <path d="m-13 0 9 9 18-21" />
        <text className="home-pathway__node-number" x="-30" y="-48">02</text>
        <text className="home-pathway__node-label" y="61" textAnchor="middle">ELIGIBILITY</text>
      </g>
      <g className="home-pathway__journey-node" transform="translate(375 270)">
        <circle className="home-pathway__node-halo" r="43" />
        <circle className="home-pathway__node-face" r="31" />
        <rect x="-13" y="-10" width="26" height="22" rx="4" />
        <path d="M-13-2h26M-7-15v9M7-15v9" />
        <text className="home-pathway__node-number" x="-30" y="-48">03</text>
        <text className="home-pathway__node-label" y="61" textAnchor="middle">APPOINTMENT</text>
      </g>
      <g className="home-pathway__journey-node home-pathway__journey-node--active" transform="translate(510 157)">
        <circle className="home-pathway__node-halo" r="46" />
        <circle className="home-pathway__node-face" r="34" />
        <path d="M-5-14h10M-4-14v7h8v-7M-6-7H6v22a6 6 0 0 1-12 0zM-6 3H6" />
        <text className="home-pathway__node-number" x="-32" y="-51">04</text>
        <text className="home-pathway__node-label" y="65" textAnchor="middle">DOSE</text>
      </g>
      <g className="home-pathway__journey-node" transform="translate(642 226)">
        <circle className="home-pathway__node-halo" r="43" />
        <circle className="home-pathway__node-face" r="31" />
        <path d="M-11-15H6l7 7v24h-24zM6-15v8h7M-5 2H7M-5 8H7" />
        <text className="home-pathway__node-number" x="-30" y="-48">05</text>
        <text className="home-pathway__node-label" y="61" textAnchor="middle">RECORD</text>
      </g>
    </svg>
  );
}
