import React from 'react';
import { Link } from 'react-router-dom';

export default function GenerationalCarePaths() {
  return (
    <section className="gen-care" id="care-pathways" aria-labelledby="gen-care-title">
      <div className="lp-container">
        <div className="gen-care__header" data-reveal="section">
          <span className="gen-care__eyebrow">
            <i className="bi bi-people-fill me-1" /> HEALTHCARE FOR EVERY GENERATION
          </span>
          <h2 id="gen-care-title">
            Tailored care whether you are <em>eighteen or eighty.</em>
          </h2>
          <p className="gen-care__subtitle">
            We designed every step to be crystal clear. Choose your pathway below for simple booking,
            reassuring nurse support, or instant mobile passes.
          </p>
        </div>

        <div className="gen-care__grid" data-reveal="cards">
          {/* Senior & Assisted Care Card */}
          <article className="gen-card gen-card--senior">
            <div className="gen-card__media">
              <img
                src="/images/senior-care-vaccination.jpg"
                alt="Compassionate nurse gently assisting a smiling senior citizen"
                className="gen-card__img"
                loading="lazy"
              />
              <span className="gen-card__badge gen-card__badge--senior">
                <i className="bi bi-heart-fill me-1" /> Gentle Senior Care
              </span>
              <span className="gen-card__pill-tag">Priority Support</span>
            </div>
            <div className="gen-card__body">
              <div className="gen-card__audience">
                <span className="gen-card__icon-wrap gen-card__icon-wrap--senior">
                  👵
                </span>
                <div>
                  <h3 className="gen-card__title">For Seniors & Elders</h3>
                  <span className="gen-card__tagline">Comfortable, clear & unhurried</span>
                </div>
              </div>
              <ul className="gen-card__perks">
                <li>
                  <i className="bi bi-check-circle-fill text-success" />
                  <span><strong>Big, clear text</strong> & step-by-step guidance</span>
                </li>
                <li>
                  <i className="bi bi-check-circle-fill text-success" />
                  <span><strong>Priority arrival seating</strong> with gentle nurse assistance</span>
                </li>
                <li>
                  <i className="bi bi-check-circle-fill text-success" />
                  <span><strong>Phone booking hotline</strong> (1900-VAX) if you prefer to call</span>
                </li>
              </ul>
              <div className="gen-card__cta">
                <Link to="/register/citizen" className="gen-card__btn gen-card__btn--senior">
                  <span>Start Senior Booking</span>
                  <i className="bi bi-arrow-right" />
                </Link>
              </div>
            </div>
          </article>

          {/* Youth & Young Adult Card */}
          <article className="gen-card gen-card--youth">
            <div className="gen-card__media">
              <img
                src="/images/youth-digital-pass.jpg"
                alt="Smiling young adult holding mobile phone with digital vaccination pass"
                className="gen-card__img"
                loading="lazy"
              />
              <span className="gen-card__badge gen-card__badge--youth">
                <i className="bi bi-qr-code-scan me-1" /> 2-Min Instant Pass
              </span>
              <span className="gen-card__pill-tag gen-card__pill-tag--youth">Fast Track</span>
            </div>
            <div className="gen-card__body">
              <div className="gen-card__audience">
                <span className="gen-card__icon-wrap gen-card__icon-wrap--youth">
                  ⚡
                </span>
                <div>
                  <h3 className="gen-card__title">For Youth & Families</h3>
                  <span className="gen-card__tagline">Fast, paperless & mobile-ready</span>
                </div>
              </div>
              <ul className="gen-card__perks">
                <li>
                  <i className="bi bi-check-circle-fill text-primary" />
                  <span><strong>Quick online screening</strong> in under 2 minutes</span>
                </li>
                <li>
                  <i className="bi bi-check-circle-fill text-primary" />
                  <span><strong>Flexible slot selection</strong> near your home or work</span>
                </li>
                <li>
                  <i className="bi bi-check-circle-fill text-primary" />
                  <span><strong>Instant digital QR code</strong> for express clinic check-in</span>
                </li>
              </ul>
              <div className="gen-card__cta">
                <Link to="/register/citizen" className="gen-card__btn gen-card__btn--youth">
                  <span>Get Fast Digital Pass</span>
                  <i className="bi bi-arrow-right" />
                </Link>
              </div>
            </div>
          </article>

          {/* Community Center & Staff Safety Card */}
          <article className="gen-card gen-card--clinic">
            <div className="gen-card__media">
              <img
                src="/images/community-vax-clinic.jpg"
                alt="Bright, welcoming community healthcare center with friendly greeting staff"
                className="gen-card__img"
                loading="lazy"
              />
              <span className="gen-card__badge gen-card__badge--clinic">
                <i className="bi bi-shield-check me-1" /> Certified Safety
              </span>
              <span className="gen-card__pill-tag gen-card__pill-tag--clinic">All Centers</span>
            </div>
            <div className="gen-card__body">
              <div className="gen-card__audience">
                <span className="gen-card__icon-wrap gen-card__icon-wrap--clinic">
                  🏥
                </span>
                <div>
                  <h3 className="gen-card__title">Accredited Centers</h3>
                  <span className="gen-card__tagline">Warm welcoming clinic spaces</span>
                </div>
              </div>
              <ul className="gen-card__perks">
                <li>
                  <i className="bi bi-check-circle-fill text-warning" />
                  <span><strong>Continuous cold chain telemetry</strong> ensuring vaccine purity</span>
                </li>
                <li>
                  <i className="bi bi-check-circle-fill text-warning" />
                  <span><strong>Wheelchair accessible</strong> with dedicated resting bays</span>
                </li>
                <li>
                  <i className="bi bi-check-circle-fill text-warning" />
                  <span><strong>Staffed by certified physicians</strong> and registered nurses</span>
                </li>
              </ul>
              <div className="gen-card__cta">
                <Link to="/how-it-works" className="gen-card__btn gen-card__btn--clinic">
                  <span>See How Centers Operate</span>
                  <i className="bi bi-arrow-right" />
                </Link>
              </div>
            </div>
          </article>
        </div>
      </div>
    </section>
  );
}
