import React from 'react';

const STAGES = [
  {
    moment: 'Programme',
    citizen: 'Find a campaign and complete its screening.',
    staff: 'Publish programme rules and review eligibility.',
  },
  {
    moment: 'Appointment',
    citizen: 'Choose from valid sessions and reserve a place.',
    staff: 'Plan the session around center resources and capacity.',
  },
  {
    moment: 'Clinic',
    citizen: 'Attend the booked center and check in.',
    staff: 'Confirm readiness, arrival, and the administered dose.',
  },
  {
    moment: 'Record',
    citizen: 'Return to a personal vaccination history.',
    staff: 'Finalise the record and certificate issue.',
  },
];

export default function HowItWorksSection() {
  return (
    <section
      className="home-workflow"
      id="how-it-works"
      aria-labelledby="lp-workflow-title"
    >
      <div className="lp-container">
        <div className="home-section-heading home-section-heading--workflow" data-reveal>
          <div>
            <span className="home-section-mark">Service delivery</span>
            <h2 id="lp-workflow-title">Citizen steps and staff operations move together.</h2>
          </div>
          <p>
            Each public action has an operational counterpart. That relationship
            keeps availability, attendance, and records aligned.
          </p>
        </div>

        <div className="home-workflow__table" data-reveal role="table" aria-label="Citizen and Staff vaccination workflow">
          <div className="home-workflow__head" role="row">
            <span role="columnheader">Service point</span>
            <span role="columnheader">Citizen</span>
            <span role="columnheader">Health staff</span>
          </div>
          {STAGES.map((stage, index) => (
            <div className="home-workflow__row" role="row" key={stage.moment}>
              <div className="home-workflow__moment" role="cell">
                <span aria-hidden="true">0{index + 1}</span>
                <h3>{stage.moment}</h3>
              </div>
              <p role="cell">{stage.citizen}</p>
              <p role="cell">{stage.staff}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
