import React from 'react';
import { ArrowRight } from './icons';

export default function HeroSection({ onLoginClick }) {
  return (
    <section className="lp-hero" aria-labelledby="lp-hero-headline">
      <div className="lp-container">
        <div className="lp-hero__grid">
          <div className="lp-hero__main">
            <div className="lp-hero__eyebrow">
              <span>Operational infrastructure</span>
              <i aria-hidden="true" />
              <span style={{ color: 'var(--lp-ink-4)', fontWeight: 500 }}>SE 2030 — Programme operations</span>
            </div>

            <h1 className="lp-hero__title" id="lp-hero-headline">
              Coordinated vaccination operations, <em>in one secure portal.</em>
            </h1>

            <p className="lp-hero__desc">
              The portal gives programme teams one place to manage campaigns, centers and vaccination sessions — with capacity
              planning and readiness built into everyday operations.
            </p>

            <div className="lp-hero__actions">
              <button type="button" className="lp-btn lp-btn--primary" onClick={onLoginClick}>
                <span>Sign in with authorized account</span>
                <ArrowRight className="lp-btn__icon" width={16} height={16} aria-hidden="true" />
              </button>
              <a className="lp-hero__link" href="#capabilities">
                Browse capabilities
                <span aria-hidden="true"> ↓</span>
              </a>
            </div>

            <div className="lp-hero__assurance" aria-label="Assurances">
              <span><b>Role-governed access</b> · Officers &amp; Managers</span>
              <span>Pre-session readiness checks</span>
              <span>Audit history on every change</span>
            </div>
          </div>

          <aside className="lp-ledger" aria-label="System registry">
            <div className="lp-ledger__head">
              <span className="lp-ledger__head-title">System registry</span>
              <span className="lp-ledger__head-note">Authorized access only</span>
            </div>
            <div className="lp-ledger__rows" role="list">
              <div className="lp-ledger__row" role="listitem">
                <span className="lp-ledger__label">01 — Campaigns</span>
                <div className="lp-ledger__value">
                  <p className="lp-ledger__value-title">Programme policy</p>
                  <p className="lp-ledger__value-desc">Dose programmes, intervals and target locations with lifecycle control.</p>
                </div>
              </div>
              <div className="lp-ledger__row" role="listitem">
                <span className="lp-ledger__label">02 — Facilities</span>
                <div className="lp-ledger__value">
                  <p className="lp-ledger__value-title">Center operations</p>
                  <p className="lp-ledger__value-desc">Hours, closures, stations, rooms and equipment inventory.</p>
                </div>
              </div>
              <div className="lp-ledger__row" role="listitem">
                <span className="lp-ledger__label">03 — Personnel</span>
                <div className="lp-ledger__value">
                  <p className="lp-ledger__value-title">Staff rosters</p>
                  <p className="lp-ledger__value-desc">Role allocations with cross-center overlap prevention.</p>
                </div>
              </div>
              <div className="lp-ledger__row" role="listitem">
                <span className="lp-ledger__label">04 — Sessions</span>
                <div className="lp-ledger__value">
                  <p className="lp-ledger__value-title">Readiness &amp; delivery</p>
                  <p className="lp-ledger__value-desc">Throughput from stations, 6-point diagnostics, immutable change logs.</p>
                </div>
              </div>
            </div>
            <div className="lp-ledger__foot">
              <b>Access is role-based.</b> Campaign Officers, Center Managers and clinical staff operate in isolated permissions.
            </div>
          </aside>
        </div>

        <div className="lp-hero__rule" aria-hidden="true">
          <span><b>Operational scope</b> — Campaigns · Centers · Sessions · Capacity · Readiness · Access control</span>
          <span>Recorded changes · Manager approvals · Readiness gate</span>
        </div>
      </div>
    </section>
  );
}
