import React from 'react';
import {
  BuildingIcon,
  CalendarClockIcon,
  FlagIcon,
  GaugeIcon,
  LockIcon,
} from './icons';

const CAPABILITIES = [
  {
    icon: FlagIcon,
    title: 'Vaccination Campaign Management',
    copy: 'Set up campaigns, configure target locations and dose programmes, and guide each campaign through its lifecycle.',
    wide: false,
  },
  {
    icon: BuildingIcon,
    title: 'Vaccination Center Operations',
    copy: 'Maintain center staffing rosters and operational resources such as rooms, stations and equipment.',
    wide: false,
  },
  {
    icon: CalendarClockIcon,
    title: 'Session Planning & Scheduling',
    copy: 'Create single or recurring vaccination sessions and handle schedule changes with clear records.',
    wide: false,
  },
  {
    icon: GaugeIcon,
    title: 'Operational Capacity & Readiness',
    copy: 'Plan session capacity in advance and confirm readiness before a session opens.',
    wide: true,
  },
  {
    icon: LockIcon,
    title: 'Secure Role-Based Access',
    copy: 'Every user works through an authenticated account, with access shaped by an assigned role.',
    wide: true,
  },
];

export default function CapabilitiesSection() {
  return (
    <section className="lp-section lp-section--tint" id="capabilities" aria-labelledby="lp-capabilities-title">
      <div className="lp-container">
        <div className="lp-section-head lp-reveal">
          <p className="lp-eyebrow lp-eyebrow--center">Portal Capabilities</p>
          <h2 className="lp-section-title" id="lp-capabilities-title">
            One portal for coordinated vaccination programmes
          </h2>
          <p className="lp-section-sub">Tools for planning and running vaccination services.</p>
        </div>

        <div className="lp-capabilities">
          {CAPABILITIES.map((capability) => {
            const Icon = capability.icon;
            return (
              <article
                key={capability.title}
                className={`lp-capability${capability.wide ? ' lp-capability--wide' : ''} lp-reveal`}
              >
                <span className="lp-capability__icon">
                  <Icon width="20" height="20" />
                </span>
                <h3 className="lp-capability__title">{capability.title}</h3>
                <p className="lp-capability__copy">{capability.copy}</p>
              </article>
            );
          })}
        </div>
      </div>
    </section>
  );
}
