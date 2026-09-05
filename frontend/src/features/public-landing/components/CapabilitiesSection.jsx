import React, { useState } from 'react';
import { Link } from 'react-router-dom';

const CAPABILITIES = [
  {
    id: 'programme', domain: 'Programme', title: 'Campaigns and eligibility',
    desc: 'Published campaigns bring together programme rules, dose schedules, and campaign-specific screening. Healthcare officers review submissions and record the final eligibility decision.',
    inputs: ['Programme rules', 'Screening answers'], outcome: 'Eligibility assessment',
    detail: 'Eligibility belongs to a campaign. Creating an account is the starting point; it does not replace screening or clinical review.',
  },
  {
    id: 'place', domain: 'Place', title: 'Centers and operating calendars',
    desc: 'Operating hours, closures, rooms, stations, equipment, and staff rosters describe what each vaccination center can support.',
    inputs: ['Operating calendar', 'Staff & resources'], outcome: 'Center readiness',
    detail: 'Center managers maintain the people, spaces, and resources that session planning depends on.',
  },
  {
    id: 'time', domain: 'Time', title: 'Sessions and capacity',
    desc: 'One-off and recurring sessions connect a campaign to a center. Readiness checks consider hours, staffing, resource conflicts, and calculated capacity before scheduling.',
    inputs: ['Center readiness', 'Capacity plan'], outcome: 'Bookable time slots',
    detail: 'An appointment reserves capacity in a time slot. Rescheduling reserves the new place before releasing the previous one.',
  },
  {
    id: 'attendance', domain: 'Attendance', title: 'Appointments and clinic arrival',
    desc: 'Citizens book available time slots and manage their appointments. Clinic staff use the appointment workflow to record arrival and check-in.',
    inputs: ['Appointment', 'Clinic arrival'], outcome: 'Recorded check-in',
    detail: 'Booking and attendance remain connected so the clinic can follow the appointment through the visit.',
  },
  {
    id: 'record', domain: 'Record', title: 'Vaccination history and certificates',
    desc: 'Healthcare officers record administered doses. Finalised vaccination information supports personal history and digital certificate issuance.',
    inputs: ['Dose information', 'Finalised record'], outcome: 'History & certificate',
    detail: 'Certificates have a reference and verification token, connecting issued evidence to the vaccination record.',
  },
];

export default function CapabilitiesSection() {
  const [selected, setSelected] = useState('time');
  const active = CAPABILITIES.find((capability) => capability.id === selected);

  return (
    <section className="home-capabilities" id="capabilities" aria-labelledby="lp-capabilities-title">
      <div className="lp-container">
        <div className="home-operations__heading" data-reveal="section">
          <div className="home-capabilities__intro">
            <span className="home-section-mark">System coordination</span>
            <h2 id="lp-capabilities-title">The work behind every appointment.</h2>
          </div>
          <p>A booking is one part of a connected service. Explore how the programme, place, time, attendance, and record fit together.</p>
        </div>
        <div className="home-operations" data-reveal="section">
          <div className="home-operations__choices" role="group" aria-label="Explore portal capabilities">
            {CAPABILITIES.map((capability, index) => (
              <button
                key={capability.id}
                type="button"
                className="home-operations__choice"
                aria-pressed={selected === capability.id}
                aria-controls="home-operation-detail"
                onClick={() => setSelected(capability.id)}
              >
                <span className="home-operations__number" aria-hidden="true">
                  <span className="home-operations__indicator" />
                  0{index + 1}
                </span>
                <span>{capability.domain}{' '}<small>{capability.title}</small></span>
                <span className="home-operations__arrow" aria-hidden="true">↗</span>
              </button>
            ))}
          </div>
          <div className="home-operations__detail" id="home-operation-detail" aria-live="polite" aria-atomic="true">
            <div className="home-operations__detail-copy" key={selected}>
              <span className="home-operations__eyebrow">How the service connects / {active.domain}</span>
              <h3>{active.title}</h3>
              <p>{active.desc}</p>
              <div className="home-operations__diagram" role="img" aria-label={`${active.inputs.join(' and ')} connect to ${active.outcome}`}>
                <div className="home-operations__inputs" aria-hidden="true">
                  {active.inputs.map((input) => <span key={input}>{input}</span>)}
                </div>
                <svg viewBox="0 0 120 130" preserveAspectRatio="none" fill="none" aria-hidden="true" focusable="false">
                  <path className="home-operations__connection" pathLength="1" d="M0 25H30Q50 25 50 45V65M0 105H30Q50 105 50 85V65H120" />
                  <circle cx="50" cy="65" r="5" />
                </svg>
                <strong className="home-operations__outcome" aria-hidden="true"><span>Connected outcome</span>{active.outcome}</strong>
              </div>
              <p className="home-operations__context">{active.detail}</p>
            </div>
            <Link className="home-operations__link" to="/about">Explore roles and responsibilities <span aria-hidden="true">→</span></Link>
          </div>
        </div>
      </div>
    </section>
  );
}
