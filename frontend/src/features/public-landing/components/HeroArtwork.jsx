import React from 'react';

/**
 * Hero line artwork: a connected care network drawn as fine line-art —
 * a central verified shield, orbiting citizen / clinic / record nodes,
 * linked by routes that draw in on load. Decorative only (aria-hidden).
 */
export default function HeroArtwork() {
  return (
    <svg
      className="lp-hero__artwork"
      viewBox="0 0 460 400"
      fill="none"
      role="presentation"
      focusable="false"
    >
      {/* — Connecting routes (draw in, staggered) — */}
      <g stroke="#0b5d47" strokeWidth="1.5" strokeLinecap="round" opacity="0.55">
        <path
          className="lp-art-stroke"
          style={{ '--lp-art-i': 0 }}
          pathLength="1"
          d="M230 118 L156 84"
        />
        <path
          className="lp-art-stroke"
          style={{ '--lp-art-i': 1 }}
          pathLength="1"
          d="M230 118 L304 84"
        />
        <path
          className="lp-art-stroke"
          style={{ '--lp-art-i': 2 }}
          pathLength="1"
          d="M230 118 L118 190"
        />
        <path
          className="lp-art-stroke"
          style={{ '--lp-art-i': 3 }}
          pathLength="1"
          d="M230 118 L342 190"
        />
        <path
          className="lp-art-stroke"
          style={{ '--lp-art-i': 4 }}
          pathLength="1"
          d="M230 118 L230 286"
        />
        <path
          className="lp-art-stroke"
          style={{ '--lp-art-i': 5 }}
          pathLength="1"
          d="M118 190 L156 286"
        />
        <path
          className="lp-art-stroke"
          style={{ '--lp-art-i': 6 }}
          pathLength="1"
          d="M342 190 L304 286"
        />
        <path
          className="lp-art-stroke"
          style={{ '--lp-art-i': 7 }}
          pathLength="1"
          d="M156 84 C 100 96, 84 140, 118 190"
        />
        <path
          className="lp-art-stroke"
          style={{ '--lp-art-i': 8 }}
          pathLength="1"
          d="M304 84 C 360 96, 376 140, 342 190"
        />
      </g>

      {/* — Fine construction arcs framing the composition — */}
      <g stroke="#c3cfc7" strokeWidth="1" opacity="0.7">
        <path className="lp-art-stroke" style={{ '--lp-art-i': 9 }} pathLength="1" d="M70 200 A 160 160 0 0 1 230 40" />
        <path className="lp-art-stroke" style={{ '--lp-art-i': 10 }} pathLength="1" d="M390 200 A 160 160 0 0 0 230 40" />
        <path className="lp-art-stroke" style={{ '--lp-art-i': 11 }} pathLength="1" d="M110 300 A 138 138 0 0 1 94 120" />
        <path className="lp-art-stroke" style={{ '--lp-art-i': 12 }} pathLength="1" d="M350 300 A 138 138 0 0 0 366 120" />
      </g>

      {/* — Central verified shield with heartbeat check — */}
      <g className="lp-art-node" style={{ '--lp-art-i': 0 }}>
        <circle cx="230" cy="118" r="52" fill="#ffffff" stroke="#0b5d47" strokeWidth="1.5" />
        <circle cx="230" cy="118" r="45" fill="#e1efe8" opacity="0.55" />
        <path
          d="M230 92 L210 99.5 V113 C210 125.5 218.4 136.6 230 140 C241.6 136.6 250 125.5 250 113 V99.5 L230 92 Z"
          fill="#0b5d47"
        />
        <g className="lp-art-heartbeat">
          <path
            d="M220 116 h6 l3.4 -8 4.6 14 3.6 -10 h5.4"
            stroke="#ffffff"
            strokeWidth="2.2"
            strokeLinecap="round"
            strokeLinejoin="round"
            fill="none"
          />
        </g>
      </g>

      {/* — Orbit nodes — */}
      <g className="lp-art-node" style={{ '--lp-art-i': 1 }}>
        <circle cx="156" cy="84" r="26" fill="#ffffff" stroke="#0b5d47" strokeWidth="1.5" />
        {/* Citizen */}
        <circle cx="156" cy="77" r="5.4" stroke="#0b5d47" strokeWidth="1.7" />
        <path d="M146.5 96.5 c1.8 -6.4 6.8 -9.5 9.5 -9.5 s7.7 3.1 9.5 9.5" stroke="#0b5d47" strokeWidth="1.7" strokeLinecap="round" />
      </g>

      <g className="lp-art-node" style={{ '--lp-art-i': 2 }}>
        <circle cx="304" cy="84" r="26" fill="#ffffff" stroke="#0b5d47" strokeWidth="1.5" />
        {/* Clinic / cross */}
        <path d="M304 73 v6.5 M300.75 76.25 h6.5" stroke="#0b5d47" strokeWidth="2.6" strokeLinecap="round" />
        <rect x="296" y="73" width="16" height="16" rx="2.5" stroke="#0b5d47" strokeWidth="1.7" />
      </g>

      <g className="lp-art-node" style={{ '--lp-art-i': 3 }}>
        <circle cx="118" cy="190" r="26" fill="#ffffff" stroke="#0b5d47" strokeWidth="1.5" />
        {/* Calendar / appointment */}
        <rect x="107" y="182" width="22" height="18" rx="2.5" stroke="#0b5d47" strokeWidth="1.7" />
        <path d="M107 188 h22 M112.5 179 v4 M123.5 179 v4" stroke="#0b5d47" strokeWidth="1.7" strokeLinecap="round" />
        <circle cx="112.5" cy="193.5" r="1.4" fill="#0b5d47" />
        <circle cx="118" cy="193.5" r="1.4" fill="#0b5d47" />
        <circle cx="123.5" cy="193.5" r="1.4" fill="#0b5d47" />
      </g>

      <g className="lp-art-node" style={{ '--lp-art-i': 4 }}>
        <circle cx="342" cy="190" r="26" fill="#ffffff" stroke="#0b5d47" strokeWidth="1.5" />
        {/* Record / document */}
        <path
          d="M333 180 h12.5 l4.5 4.5 V200 a2 2 0 0 1 -2 2 H333 a2 2 0 0 1 -2 -2 V182 a2 2 0 0 1 2 -2 Z"
          stroke="#0b5d47"
          strokeWidth="1.7"
          strokeLinejoin="round"
        />
        <path d="M336 190 h9 M336 194.5 h9" stroke="#0b5d47" strokeWidth="1.6" strokeLinecap="round" />
      </g>

      <g className="lp-art-node" style={{ '--lp-art-i': 5 }}>
        <circle cx="230" cy="286" r="26" fill="#ffffff" stroke="#0b5d47" strokeWidth="1.5" />
        {/* Vial */}
        <path d="M225.5 275 h9 M227 275 v4 h6 v-4" stroke="#0b5d47" strokeWidth="1.7" strokeLinecap="round" />
        <path d="M226 279 h8 v11 a4 4 0 0 1 -8 0 Z" stroke="#0b5d47" strokeWidth="1.7" strokeLinejoin="round" />
        <path d="M226.5 286 h7" stroke="#0b5d47" strokeWidth="1.4" />
      </g>

      {/* — Grounding baseline ticks — */}
      <g className="lp-art-node" style={{ '--lp-art-i': 6 }}>
        <path d="M150 330 h160" stroke="#c3cfc7" strokeWidth="1.5" strokeLinecap="round" />
        <path d="M170 338 h120" stroke="#c3cfc7" strokeWidth="1" strokeLinecap="round" />
      </g>
    </svg>
  );
}
