import React from 'react';
import { ArrowDownIcon, ArrowRightIcon, CalendarIcon, GaugeIcon, LockIcon, ShieldCheckIcon } from './icons';

function SessionRow({ time, bars, status, tone }) {
  return (
    <div className="lp-session-row">
      <span className="lp-session-row__time">{time}</span>
      <span className="lp-session-row__bars">
        {bars.map((width, index) => (
          <span key={index} className="lp-session-row__bar" style={{ width }} />
        ))}
      </span>
      <span className={`lp-pill lp-pill--${tone}`}>{status}</span>
    </div>
  );
}

export default function HeroSection({ onLoginClick }) {
  return (
    <section className="lp-hero" aria-labelledby="lp-hero-title">
      <div className="lp-container lp-hero__inner">
        <div className="lp-hero__content">
          <p className="lp-eyebrow">Vaccination Program Portal</p>
          <h1 className="lp-hero__title" id="lp-hero-title">
            Coordinated vaccination operations, in one secure portal
          </h1>
          <p className="lp-hero__lead">
            The Web-Based Vaccination Portal gives programme teams one place to
            manage campaigns, centers and vaccination sessions — with planned
            capacity and readiness built in.
          </p>
          <div className="lp-hero__actions">
            <button type="button" className="lp-btn lp-btn--primary lp-btn--lg" onClick={onLoginClick}>
              Sign in to the portal
              <ArrowRightIcon className="lp-btn__icon" width="17" height="17" strokeWidth={2} />
            </button>
            <a className="lp-btn lp-btn--ghost lp-btn--lg" href="#capabilities">
              Explore the portal
              <ArrowDownIcon className="lp-btn__icon" width="16" height="16" strokeWidth={2} />
            </a>
          </div>
          <p className="lp-hero__note">
            <LockIcon width="15" height="15" />
            Secure sign-in · Access based on your assigned role
          </p>
        </div>

        <div className="lp-hero__visual" aria-hidden="true">
          <div className="lp-visual-frame">
            <div className="lp-session-panel">
              <div className="lp-session-panel__head">
                <CalendarIcon width="16" height="16" />
                <span>Session schedule</span>
              </div>
              <SessionRow time="09:00" bars={['72%', '46%']} status="SCHEDULED" tone="info" />
              <SessionRow time="11:00" bars={['64%', '38%']} status="IN_PROGRESS" tone="success" />
              <SessionRow time="14:00" bars={['58%', '42%']} status="DRAFT" tone="neutral" />
            </div>

            <div className="lp-chip lp-chip--ready">
              <span className="lp-chip__icon">
                <ShieldCheckIcon width="18" height="18" />
              </span>
              <span className="lp-chip__meta">
                <span className="lp-chip__label">Readiness</span>
                <span className="lp-pill lp-pill--success">READY</span>
              </span>
            </div>

            <div className="lp-capacity">
              <span className="lp-capacity__head">
                <GaugeIcon width="15" height="15" />
                Capacity Plan
              </span>
              <span className="lp-meter">
                <span className="lp-meter__fill" />
              </span>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
