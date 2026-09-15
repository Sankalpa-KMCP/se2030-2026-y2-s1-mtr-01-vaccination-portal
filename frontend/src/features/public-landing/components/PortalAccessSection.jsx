import React from 'react';
import { Link } from 'react-router-dom';

export default function PortalAccessSection() {
  return (
    <section
      className="home-access"
      id="portal-access"
      aria-labelledby="portal-access-title"
    >
      <div className="lp-container">
        <div className="home-section-heading home-section-heading--split" data-reveal="section">
          <div>
            <span className="home-section-mark">Portal access</span>
            <h2 id="portal-access-title">One service. Two ways in.</h2>
          </div>
          <p>
            Use the portal as a Citizen, or enter the operational workspace as
            approved health staff.
          </p>
        </div>

        <div className="home-access__ledger" data-reveal="access">
          <article className="home-access__citizen">
            <div className="home-access__body">
              <span className="home-access__role">Citizen access</span>
              <h3>Plan and keep track of your vaccination care.</h3>
              <p>
                Create an account, manage your profile, complete campaign screening,
                and manage appointments.
              </p>
              <ul className="home-access__features" aria-label="Citizen capabilities">
                <li>Campaign screening</li>
                <li>Appointment management</li>
                <li>Profile and screening history</li>
              </ul>
              <div className="home-access__actions">
                <Link className="home-action home-action--secondary" to="/register/citizen">
                  Register as a Citizen <span aria-hidden="true">&rarr;</span>
                </Link>
              </div>
            </div>
          </article>

          <article className="home-access__staff">
            <div className="home-access__body">
              <span className="home-access__role">Staff operations</span>
              <h3>Coordinate campaigns, clinics, and sessions.</h3>
              <p>
                Staff access is reviewed by an administrator before operational
                permissions are activated.
              </p>
              <ul className="home-access__features" aria-label="Staff capabilities">
                <li>Campaign and center planning</li>
                <li>Sessions, resources, and rosters</li>
                <li>Eligibility review and vaccination drafts</li>
              </ul>
              <div className="home-access__actions">
                <Link className="home-action home-action--outline" to="/register/staff">Request staff account</Link>
              </div>
            </div>
          </article>
        </div>
      </div>
    </section>
  );
}
