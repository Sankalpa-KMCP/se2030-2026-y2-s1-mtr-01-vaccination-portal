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
        <div className="home-section-heading home-section-heading--split" data-reveal>
          <div>
            <span className="home-section-mark">01 / Access</span>
            <h2 id="portal-access-title">One service. Two ways in.</h2>
          </div>
          <p>
            Use the portal as a Citizen, or enter the operational workspace as
            approved health staff.
          </p>
        </div>

        <div className="home-access__ledger" data-reveal>
          <article className="home-access__citizen" data-tilt data-tilt-strength="2.5">
            <div className="home-access__number" aria-hidden="true">C</div>
            <div className="home-access__body">
              <span className="home-access__role">Citizen access</span>
              <h3>Plan and keep track of your vaccination care.</h3>
              <p>
                Create an account, complete campaign screening, manage appointments,
                and return to your own history.
              </p>
              <div className="home-access__actions">
                <Link className="home-action home-action--secondary" to="/register/citizen">
                  Register as a Citizen <span aria-hidden="true">&rarr;</span>
                </Link>
                <Link className="home-text-link" to="/login/citizen">Citizen login</Link>
              </div>
            </div>
          </article>

          <article className="home-access__staff" data-tilt data-tilt-strength="2.5">
            <div className="home-access__number" aria-hidden="true">S</div>
            <div className="home-access__body">
              <span className="home-access__role">Staff operations</span>
              <h3>Coordinate campaigns, clinics, and sessions.</h3>
              <p>
                Staff access is reviewed by an administrator before operational
                permissions are activated.
              </p>
              <div className="home-access__actions">
                <Link className="home-action home-action--outline" to="/register/staff">Request staff account</Link>
                <Link className="home-text-link home-text-link--light" to="/login/staff">
                  Staff login <span aria-hidden="true">&rarr;</span>
                </Link>
              </div>
            </div>
          </article>
        </div>
      </div>
    </section>
  );
}
