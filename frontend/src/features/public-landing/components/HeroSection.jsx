import React from 'react';
import { Link } from 'react-router-dom';

export default function HeroSection() {
  return (
    <section className="home-hero home-hero--simplified" aria-labelledby="home-hero-title">
      <div className="lp-container home-hero__frame">
        <div className="home-hero__copy">
          <span className="hospital-eyebrow">PUBLIC VACCINATION SERVICES</span>
          <h1 id="home-hero-title">
            Manage your vaccination journey <em>in one place.</em>
          </h1>
          <p className="home-hero__lead">
            Create a citizen account to complete campaign screening, view eligible
            appointments, and manage your vaccination records.
          </p>

          <div className="home-hero__actions">
            <Link to="/register/citizen" className="home-action home-action--primary">
              Create citizen account
              <span aria-hidden="true">&rarr;</span>
            </Link>
            <Link to="/login" className="home-action home-action--secondary-simple">
              Sign in
            </Link>
          </div>
        </div>

        <figure className="home-hero__visual home-hero__photo">
          <img
            src="/images/senior-care-vaccination.jpg"
            alt="Healthcare worker assisting a citizen during a vaccination appointment"
            className="home-hero__photo-image"
            loading="eager"
          />
          <figcaption className="home-hero__photo-caption">
            <i className="bi bi-shield-check" aria-hidden="true" />
            <span>
              <strong>Citizen vaccination services</strong>
              Screening, appointments, and records through one portal.
            </span>
          </figcaption>
        </figure>
      </div>
    </section>
  );
}
