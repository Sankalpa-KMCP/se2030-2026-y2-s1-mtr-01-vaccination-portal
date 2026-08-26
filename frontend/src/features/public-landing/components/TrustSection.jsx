import React from 'react';
import { ClipboardCheckIcon, GaugeIcon, HistoryIcon, LockIcon } from './icons';

const TRUST_ITEMS = [
  {
    icon: LockIcon,
    title: 'Role-based access',
    copy: 'Access to portal features follows each user\u2019s assigned role.',
  },
  {
    icon: HistoryIcon,
    title: 'Auditable operational changes',
    copy: 'Campaign decisions and session changes are recorded so teams can review what happened and when.',
  },
  {
    icon: ClipboardCheckIcon,
    title: 'Readiness checks',
    copy: 'Sessions include explicit readiness confirmation before they go live.',
  },
  {
    icon: GaugeIcon,
    title: 'Controlled capacity adjustments',
    copy: 'Capacity changes are deliberate, recorded revisions \u2014 not silent edits.',
  },
];

export default function TrustSection() {
  return (
    <section className="lp-section lp-section--tint lp-trust" id="trust" aria-labelledby="lp-trust-title">
      <div className="lp-container">
        <h2 className="lp-trust__title lp-reveal" id="lp-trust-title">
          Built for accountable day-to-day operations
        </h2>
        <div className="lp-trust__grid">
          {TRUST_ITEMS.map((item) => {
            const Icon = item.icon;
            return (
              <div key={item.title} className="lp-trust__item lp-reveal">
                <span className="lp-trust__icon">
                  <Icon width="19" height="19" />
                </span>
                <div>
                  <h3 className="lp-trust__heading">{item.title}</h3>
                  <p className="lp-trust__copy">{item.copy}</p>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}
