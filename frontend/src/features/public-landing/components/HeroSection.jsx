import React from 'react';
import { Link } from 'react-router-dom';
import HeroArtwork from './HeroArtwork';

export default function HeroSection() {
  return (
    <section className="home-hero" aria-labelledby="home-hero-title">
      <div className="lp-container home-hero__frame">
        <div className="home-hero__copy">
          <span className="hospital-eyebrow">CONNECTED CARE · VACCINATION SERVICES</span>
          <h1 id="home-hero-title">
            Your next step to a <em>healthier tomorrow.</em>
          </h1>
          <p className="home-hero__lead">
            Vaccination planning, made simpler. Explore campaigns, complete your
            screening and book an available clinic appointment once eligible.
          </p>
          <div className="home-hero__actions">
            <Link to="/register/citizen" className="home-action home-action--primary">
              Create citizen account
              <span aria-hidden="true">&rarr;</span>
            </Link>
            <Link to="/login/citizen" className="home-text-link">
              Sign in as a Citizen
            </Link>
          </div>
          <p className="home-hero__note">
            Citizen accounts can be created immediately. Eligibility is assessed
            separately for each campaign.
          </p>
        </div>

        <div className="home-hero__visual" aria-hidden="true">
          <div className="home-hero__visual-head">
            <span>Vaccination pathway</span>
            <span className="home-hero__visual-context">Citizen services</span>
          </div>
          <div className="home-hero__visual-title">
            <span>Care that moves with you.</span>
            <p>From campaign screening to a booked clinic visit.</p>
          </div>
          <div className="home-hero__visual-screen">
            <HeroArtwork />
          </div>
          <div className="home-hero__visual-foot">
            <p>Clinical review and session readiness support each appointment.</p>
          </div>
        </div>

        <aside className="home-hero__rail" aria-label="Portal overview">
          <div>
            <span className="home-hero__rail-index">01</span>
            <strong>For Citizens</strong>
            <span>Profile details, screening, and appointments.</span>
          </div>
          <div>
            <span className="home-hero__rail-index">02</span>
            <strong>For health teams</strong>
            <span>Campaigns, centers, sessions, and readiness.</span>
          </div>
          <Link to="/login/staff" className="home-hero__staff-link">
            Enter staff portal <span aria-hidden="true">&rarr;</span>
          </Link>
        </aside>
      </div>
    </section>
  );
}
