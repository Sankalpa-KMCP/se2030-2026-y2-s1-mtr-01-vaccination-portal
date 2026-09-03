import React from 'react';

const CAPABILITIES = [
  {
    num: '01',
    domain: 'Programme',
    title: 'Campaigns and eligibility',
    desc: 'Campaign-specific screening connects published programmes with the citizens they are designed to serve.',
    signal: 'Screening / review',
  },
  {
    num: '02',
    domain: 'Place',
    title: 'Centers and operating calendars',
    desc: 'Hours, closures, stations, resources, and staff assignments describe how each center can operate.',
    signal: 'Center / resources',
  },
  {
    num: '03',
    domain: 'Time',
    title: 'Sessions and capacity',
    desc: 'One-off or recurring sessions turn confirmed staffing and physical resources into bookable capacity.',
    signal: 'Schedule / slots',
  },
  {
    num: '04',
    domain: 'Attendance',
    title: 'Appointments and clinic arrival',
    desc: 'Citizens reserve available time, manage appointments, and move into the clinic check-in process.',
    signal: 'Booking / check-in',
  },
  {
    num: '05',
    domain: 'Record',
    title: 'Vaccination history and certificates',
    desc: 'Finalised dose information creates a lasting vaccination record and a verifiable certificate issue.',
    signal: 'Dose / evidence',
  },
];

export default function CapabilitiesSection() {
  return (
    <section
      className="home-capabilities"
      id="capabilities"
      aria-labelledby="lp-capabilities-title"
    >
      <div className="lp-container home-capabilities__layout">
        <div className="home-capabilities__intro" data-reveal>
          <span className="home-section-mark">02 / What the system coordinates</span>
          <h2 id="lp-capabilities-title">
            The work behind every appointment.
          </h2>
          <p>
            A booking is only one point in a larger public-health operation.
            The portal keeps the programme, place, time, attendance, and record connected.
          </p>
          <div className="home-capabilities__key" aria-hidden="true">
            <span>INPUT</span><i /><span>CARE EVENT</span><i /><span>RECORD</span>
          </div>
        </div>

        <ol className="home-capabilities__index" data-reveal>
          {CAPABILITIES.map((cap) => (
            <li key={cap.num} className="home-capability">
              <span className="home-capability__number" aria-hidden="true">{cap.num}</span>
              <span className="home-capability__domain">{cap.domain}</span>
              <div>
                <h3>{cap.title}</h3>
                <p>{cap.desc}</p>
              </div>
              <span className="home-capability__signal">{cap.signal}</span>
            </li>
          ))}
        </ol>
      </div>
    </section>
  );
}
