import React from 'react';
import { Link } from 'react-router-dom';

const STAGES = [
  { moment: 'Programme', phase: 'Before you book', citizen: 'Find a campaign and complete its screening.', staff: 'Publish programme rules and review eligibility.', icon: 'M6 3h12v18H6zM9 8h6M9 12h6M9 16h4' },
  { moment: 'Appointment', phase: 'Choose your time', citizen: 'Choose from valid sessions and reserve a place.', staff: 'Plan the session around center resources and capacity.', icon: 'M4 6h16v15H4zM4 11h16M8 3v5M16 3v5M8 15h3' },
  { moment: 'Clinic', phase: 'On the day', citizen: 'Attend the booked center and check in.', staff: 'Confirm arrival and record the administered dose.', icon: 'M3 21V9l9-6 9 6v12M9 21v-6h6v6M12 7v5M9.5 9.5h5' },
  { moment: 'Record', phase: 'After your visit', citizen: 'Return to your personal vaccination history.', staff: 'Finalise dose information and issue a certificate.', icon: 'M6 3h9l4 4v14H6zM14 3v5h5M9 13l2 2 5-5' },
];

export default function HowItWorksSection() {
  return (
    <section className="home-workflow" id="how-it-works" aria-labelledby="lp-workflow-title">
      <div className="lp-container">
        <div className="home-section-heading home-section-heading--workflow" data-reveal="section">
          <div>
            <span className="home-section-mark">The connected care journey</span>
            <h2 id="lp-workflow-title">Citizen steps and staff operations move together.</h2>
          </div>
          <p>One continuous journey, with a clear next step for you and a coordinated response from the clinic.</p>
        </div>
        <ol className="home-journey" data-reveal="sequence" aria-label="Citizen and staff vaccination journey">
          {STAGES.map((stage, index) => (
            <li className="home-journey__stage" key={stage.moment}>
              <div className="home-journey__station" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none" focusable="false"><path d={stage.icon} /></svg>
                <span>0{index + 1}</span>
              </div>
              <span className="home-journey__phase">{stage.phase}</span>
              <h3>{stage.moment}</h3>
              <p>{stage.citizen}</p>
              <div className="home-journey__staff"><span>Behind the scenes</span><p>{stage.staff}</p></div>
            </li>
          ))}
        </ol>
        <div className="home-journey__foot">
          <p>New to the portal? Your journey begins with a Citizen account.</p>
          <Link to="/how-it-works">Walk through the full process <span aria-hidden="true">→</span></Link>
        </div>
      </div>
    </section>
  );
}
