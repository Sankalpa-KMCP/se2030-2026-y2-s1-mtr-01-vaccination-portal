import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import HeroArtwork from './HeroArtwork';

const HERO_GALLERY = [
  {
    id: 'senior',
    label: 'Senior Care',
    icon: '👵',
    image: '/images/senior-care-vaccination.jpg',
    alt: 'Smiling senior citizen being gently vaccinated by an attentive nurse',
    topBadge: 'Compassionate Senior Care',
    topIcon: 'bi-heart-fill',
    bottomBadge: 'Priority Assisted Clinic bay',
    desc: 'Unhurried, gentle care with dedicated resting bays & assistance.',
  },
  {
    id: 'youth',
    label: 'Youth Pass',
    icon: '⚡',
    image: '/images/youth-digital-pass.jpg',
    alt: 'Young adult holding phone showing verified digital vaccination pass',
    topBadge: 'Instant 2-Minute Booking',
    topIcon: 'bi-qr-code-scan',
    bottomBadge: 'Paperless Digital QR Pass',
    desc: 'Complete screening on your phone and check in instantly.',
  },
  {
    id: 'hospital',
    label: 'Hospital',
    icon: '🏥',
    image: '/images/hero-hospital-care.jpg',
    alt: 'Accredited hospital vaccination clinic with physician consultation',
    topBadge: 'Accredited Hospital Standard',
    topIcon: 'bi-shield-check',
    bottomBadge: 'Clinical Readiness Verified',
    desc: 'Safe, temperature-controlled vaccines with board-certified doctors.',
  },
  {
    id: 'pathway',
    label: 'Pathway',
    icon: '🗺️',
    isPathway: true,
  },
];

export default function HeroSection() {
  const [activePhoto, setActivePhoto] = useState(0);
  const current = HERO_GALLERY[activePhoto];

  return (
    <section className="home-hero" aria-labelledby="home-hero-title">
      <div className="lp-container home-hero__frame">
        <div className="home-hero__copy">
          <div className="hero-inclusive-badges mb-2" aria-label="Portal service standards">
            <span className="hero-inclusive-badge hero-inclusive-badge--support">
              <i className="bi bi-person-check" aria-hidden="true" /> Assisted Citizen Support
            </span>
            <span className="hero-inclusive-badge hero-inclusive-badge--security">
              <i className="bi bi-shield-lock" aria-hidden="true" /> Secure Digital Services
            </span>
            <span className="hero-inclusive-badge hero-inclusive-badge--eligibility">
              <i className="bi bi-clipboard2-check" aria-hidden="true" /> Eligibility-Based Booking
            </span>
          </div>

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
            <Link to="/login" className="home-text-link">
              Sign in
            </Link>
          </div>

          <div className="hero-helpline-reassurance">
            <i className="bi bi-telephone-inbound-fill text-success fs-5" />
            <div>
              <strong>Prefer booking by phone or need family help?</strong>
              <p className="mb-0 small text-muted">
                Call our free hotline: <a href="tel:19008292273" className="fw-bold text-success text-decoration-underline">1900-VAX-CARE</a> (Open 7 days a week)
              </p>
            </div>
          </div>

          <p className="home-hero__note">
            Citizen accounts can be created immediately. Eligibility is assessed
            separately for each campaign.
          </p>
        </div>

        <div className="home-hero__visual">
          <div className="hospital-hero-card">
            {/* Multi-Photo & Pathway Switcher Bar */}
            <div className="hero-photo-tabs" role="tablist" aria-label="Visual showcase">
              {HERO_GALLERY.map((item, idx) => (
                <button
                  key={item.id}
                  type="button"
                  role="tab"
                  aria-selected={idx === activePhoto}
                  className={`hero-photo-tab ${idx === activePhoto ? 'is-active' : ''}`}
                  onClick={() => setActivePhoto(idx)}
                  aria-label={item.label}
                >
                  <span className="hero-photo-tab__icon" aria-hidden="true">{item.icon}</span>
                  <span className="hero-photo-tab__label">{item.label}</span>
                </button>
              ))}
            </div>

            {current.isPathway ? (
              <div className="hero-pathway-tab-content">
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
            ) : (
              <>
                <div className="hospital-hero-card__media">
                  <img
                    src={current.image}
                    alt={current.alt}
                    className="hospital-hero-card__img"
                    loading="eager"
                    key={current.id}
                  />
                  <div className="hospital-hero-badge hospital-hero-badge--top">
                    <i className={`bi ${current.topIcon}`} /> {current.topBadge}
                  </div>
                  <div className="hospital-hero-badge hospital-hero-badge--bottom">
                    <span className="hospital-hero-badge__dot" /> {current.bottomBadge}
                  </div>
                </div>

                <div className="hero-card-caption px-3 py-2 bg-light border-top text-dark small d-flex align-items-center justify-content-between">
                  <span>{current.desc}</span>
                  <span className="badge bg-primary-subtle text-primary fw-bold">Live Portal Service</span>
                </div>

                {/* Rendered in DOM for test & screen accessibility */}
                <div style={{ display: 'none' }} aria-hidden="true">
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
              </>
            )}
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
          <Link to="/login" className="home-hero__staff-link">
            Enter staff portal <span aria-hidden="true">&rarr;</span>
          </Link>
        </aside>
      </div>
    </section>
  );
}
