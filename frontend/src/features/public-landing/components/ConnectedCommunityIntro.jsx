import React, { useEffect, useRef } from 'react';
import '../connected-community-intro.css';

const INTRO_DURATION_MS = 3600;
const ANIMATION_FALLBACK_MS = 100;

const CITIZENS = [
  { x: 142, y: 98 },
  { x: 105, y: 218 },
  { x: 153, y: 338 },
  { x: 618, y: 98 },
  { x: 655, y: 218 },
  { x: 607, y: 338 },
];

function CommunityArtwork() {
  return (
    <svg className="cc-intro__art" viewBox="0 0 760 440" fill="none" aria-hidden="true">
      <defs>
        <linearGradient id="cc-intro-shield-fill" x1="317" y1="133" x2="447" y2="300" gradientUnits="userSpaceOnUse">
          <stop stopColor="#F2FFFB" />
          <stop offset="1" stopColor="#BDEBDD" />
        </linearGradient>
        <linearGradient id="cc-intro-line" x1="105" y1="218" x2="655" y2="218" gradientUnits="userSpaceOnUse">
          <stop stopColor="#8CD5C8" />
          <stop offset="0.5" stopColor="#0D776F" />
          <stop offset="1" stopColor="#8CD5C8" />
        </linearGradient>
      </defs>

      <circle className="cc-intro__halo" cx="380" cy="218" r="135" fill="#D9F4EA" />
      <circle className="cc-intro__halo-ring" cx="380" cy="218" r="148" stroke="#A4DDD0" />

      {CITIZENS.map(({ x, y }, index) => (
        <line
          key={`connection-${x}-${y}`}
          className="cc-intro__connection"
          style={{ '--cc-index': index }}
          x1={x}
          y1={y}
          x2="380"
          y2="218"
          pathLength="1"
          stroke="url(#cc-intro-line)"
          strokeWidth="2"
        />
      ))}

      {CITIZENS.map(({ x, y }, index) => (
        <g
          key={`citizen-${x}-${y}`}
          transform={`translate(${x} ${y})`}
        >
          <g className="cc-intro__citizen" style={{ '--cc-index': index }}>
            <circle r="32" fill="#F7FEFB" stroke="#A9DACC" strokeWidth="2" />
            <circle cy="-8" r="8" fill="#188477" />
            <path d="M-16 16c1-10 7-15 16-15s15 5 16 15" stroke="#188477" strokeWidth="6" strokeLinecap="round" />
          </g>
        </g>
      ))}

      <g className="cc-intro__shield">
        <path
          d="M380 125c-21 17-48 25-67 29v61c0 49 25 83 67 108 42-25 67-59 67-108v-61c-19-4-46-12-67-29Z"
          fill="url(#cc-intro-shield-fill)"
          stroke="#0D776F"
          strokeWidth="5"
          strokeLinejoin="round"
        />
        <path d="M380 145c-17 12-36 19-47 22v47c0 37 17 64 47 85 30-21 47-48 47-85v-47c-11-3-30-10-47-22Z" stroke="#8CCDBD" strokeWidth="2" />
        <g className="cc-intro__syringe" transform="translate(377 221) rotate(-36) scale(0.8)">
          <path d="M-48 0h18m-13-10v20" stroke="#0B706A" strokeWidth="5" strokeLinecap="round" />
          <rect x="-30" y="-12" width="51" height="24" rx="5" fill="#FFFFFF" stroke="#0B706A" strokeWidth="4" />
          <path d="M-12-5v10M1-5v10" stroke="#69B9AB" strokeWidth="3" strokeLinecap="round" />
          <path d="M21 0h27" stroke="#0B706A" strokeWidth="4" strokeLinecap="round" />
          <path d="m48 0 10-4" stroke="#0B706A" strokeWidth="2.5" strokeLinecap="round" />
        </g>
      </g>
      <circle className="cc-intro__pulse" cx="380" cy="218" r="90" stroke="#52C6A8" strokeWidth="3" />
    </svg>
  );
}

export default function ConnectedCommunityIntro({ onComplete }) {
  const skipRef = useRef(null);

  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    skipRef.current?.focus({ preventScroll: true });

    const handleKeyDown = (event) => {
      if (event.key === 'Escape') onComplete();
    };
    window.addEventListener('keydown', handleKeyDown);
    const timeout = window.setTimeout(onComplete, INTRO_DURATION_MS + ANIMATION_FALLBACK_MS);

    return () => {
      window.clearTimeout(timeout);
      window.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = previousOverflow;
    };
  }, [onComplete]);

  const handleAnimationEnd = (event) => {
    if (event.target === event.currentTarget) {
      onComplete();
    }
  };

  return (
    <div className="cc-intro" role="dialog" aria-modal="true" aria-labelledby="cc-intro-title" aria-describedby="cc-intro-subtitle" onAnimationEnd={handleAnimationEnd}>
      <button ref={skipRef} className="cc-intro__skip" type="button" onClick={onComplete}>
        Skip Intro <span aria-hidden="true">→</span>
      </button>
      <div className="cc-intro__content">
        <p className="cc-intro__eyebrow">A connected community</p>
        <CommunityArtwork />
        <div className="cc-intro__message">
          <h1 id="cc-intro-title">Vaccination Portal</h1>
          <p id="cc-intro-subtitle">Connected for a healthier community</p>
        </div>
      </div>
    </div>
  );
}
