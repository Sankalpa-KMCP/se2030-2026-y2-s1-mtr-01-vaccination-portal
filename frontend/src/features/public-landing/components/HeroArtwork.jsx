import React from 'react';

const STAGES = [
  { label: 'Account', detail: 'Create your account', icon: 'M0-12a6 6 0 1 0 0 12 6 6 0 0 0 0-12ZM-12 14c2-8 7-12 12-12s10 4 12 12' },
  { label: 'Screening', detail: 'Complete the campaign questionnaire', icon: 'm-12 0 8 8 17-19' },
  { label: 'Appointment', detail: 'Reserve an available time slot', icon: 'M-12-9h24v21h-24zM-12-2h24M-6-14v9M6-14v9' },
  { label: 'Clinic arrival', detail: 'Attend your clinic appointment', icon: 'M-5-14h10M-4-14v7h8v-7M-6-7H6v22a6 6 0 0 1-12 0zM-6 3H6' },
  { label: 'Check-in', detail: 'Clinic staff confirm your arrival', icon: 'M-10-14H5l7 7v22h-22zM5-14v8h7M-4 1H6M-4 7H6' },
];

/** Decorative service diagrams; the full citizen/staff journey is also in HTML. */
export default function HeroArtwork() {
  return (
    <>
      <svg className="home-pathway home-pathway--desktop" viewBox="0 0 720 340" fill="none" role="presentation" focusable="false">
        <g className="home-pathway__staff-pill home-pathway__staff-pill--campaign">
          <rect x="74" y="36" width="248" height="62" rx="7" />
          <text x="92" y="61">Campaign screening</text>
          <text className="home-pathway__pill-note" x="92" y="81">Programme rules and clinical review</text>
        </g>
        <g className="home-pathway__staff-pill home-pathway__staff-pill--ready">
          <rect x="366" y="36" width="280" height="62" rx="7" />
          <text x="384" y="61">Session readiness</text>
          <text className="home-pathway__pill-note" x="384" y="81">Center, capacity and staff confirmed</text>
        </g>
        <path className="home-pathway__staff-connector home-pathway__staff-connector--campaign" pathLength="1" d="M216 98V192" />
        <path className="home-pathway__staff-connector home-pathway__staff-connector--ready" pathLength="1" d="M506 98V126Q506 138 494 138H372Q360 138 360 150V192" />
        <path className="home-pathway__route" pathLength="1" d="M72 228H648" />
        <path className="home-pathway__route-progress" pathLength="1" d="M72 228H648" />
        {STAGES.map((stage, index) => (
          <g className={`home-pathway__journey-node home-pathway__journey-node--${index + 1}`} transform={`translate(${72 + index * 144} 228)`} key={stage.label}>
            <g className="home-pathway__node-core">
              <rect className="home-pathway__node-face" x="-28" y="-28" width="56" height="56" rx="10" />
              <path d={stage.icon} />
              <text className="home-pathway__node-number" x="-24" y="-44">0{index + 1}</text>
              <text className="home-pathway__node-label" y="62" textAnchor="middle">{stage.label}</text>
            </g>
          </g>
        ))}
      </svg>
      <svg className="home-pathway home-pathway--mobile" viewBox="0 0 360 350" fill="none" role="presentation" focusable="false">
        <path className="home-pathway__route" pathLength="1" d="M36 38V302" />
        {STAGES.map((stage, index) => (
          <g className={`home-pathway__journey-node home-pathway__journey-node--${index + 1}`} transform={`translate(36 ${38 + index * 66})`} key={stage.label}>
            <g className="home-pathway__node-core">
              <rect className="home-pathway__node-face" x="-22" y="-22" width="44" height="44" rx="8" />
              <path d={stage.icon} />
              <text className="home-pathway__node-label" x="66" y="-4">{stage.label}</text>
              <text className="home-pathway__node-number" x="48" y="-4">{index + 1}</text>
              <text className="home-pathway__mobile-note" x="66" y="16">{stage.detail}</text>
            </g>
          </g>
        ))}
      </svg>
    </>
  );
}
