import React from 'react';
import { Link } from 'react-router-dom';

export default function GenerationalCarePaths() {
  return (
    <section className="gen-care" id="care-pathways" aria-labelledby="gen-care-title">
      <div className="lp-container">
        <div className="gen-care__header" data-reveal="section">
          <span className="gen-care__eyebrow">
            <i className="bi bi-people-fill me-1" /> CITIZEN SERVICE PATHWAYS
          </span>
          <h2 id="gen-care-title">
            Choose the support that <em>fits your needs.</em>
          </h2>
          <p className="gen-care__subtitle">
            Find accessible guidance, secure online vaccination services, and information about
            participating vaccination centers.
          </p>
        </div>

        <div className="gen-care__grid" data-reveal="cards">
          {/* Senior & Assisted Care Card */}
          <article className="gen-card gen-card--senior">
            <div className="gen-card__media">
              <img
                src="/images/senior-care-vaccination.jpg"
                alt="Healthcare worker assisting a senior citizen at a vaccination center"
                className="gen-card__img"
                loading="lazy"
              />
              <span className="gen-card__badge gen-card__badge--senior">
                <i className="bi bi-universal-access-circle me-1" /> Accessible Support
              </span>
              <span className="gen-card__pill-tag">Guided Services</span>
            </div>
            <div className="gen-card__body">
              <div className="gen-card__audience">
                <span className="gen-card__icon-wrap gen-card__icon-wrap--senior">
                  <i className="bi bi-person-check" aria-hidden="true" />
                </span>
                <div>
                  <h3 className="gen-card__title">Senior & Assisted Services</h3>
                  <span className="gen-card__tagline">Clear guidance for citizens and caregivers</span>
                </div>
              </div>
              <ul className="gen-card__perks">
                <li>
                  <i className="bi bi-check-circle-fill text-success" />
                  <span><strong>Adjustable text sizes</strong> and guided portal steps</span>
                </li>
                <li>
                  <i className="bi bi-check-circle-fill text-success" />
                  <span><strong>Citizen registration</strong> and appointment booking access</span>
                </li>
                <li>
                  <i className="bi bi-check-circle-fill text-success" />
                  <span><strong>Public helpline</strong> for general portal support</span>
                </li>
              </ul>
              <div className="gen-card__cta">
                <Link to="/register/citizen" className="gen-card__btn gen-card__btn--senior">
                  <span>Create Citizen Account</span>
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
                alt="Citizen using a mobile phone at a vaccination service center"
                className="gen-card__img"
                loading="lazy"
              />
              <span className="gen-card__badge gen-card__badge--youth">
                <i className="bi bi-shield-lock me-1" /> Secure Online Access
              </span>
              <span className="gen-card__pill-tag gen-card__pill-tag--youth">Digital Services</span>
            </div>
            <div className="gen-card__body">
              <div className="gen-card__audience">
                <span className="gen-card__icon-wrap gen-card__icon-wrap--youth">
                  <i className="bi bi-phone" aria-hidden="true" />
                </span>
                <div>
                  <h3 className="gen-card__title">Digital Citizen Services</h3>
                  <span className="gen-card__tagline">Manage vaccination services online</span>
                </div>
              </div>
              <ul className="gen-card__perks">
                <li>
                  <i className="bi bi-check-circle-fill text-primary" />
                  <span><strong>Complete screening</strong> for available vaccination campaigns</span>
                </li>
                <li>
                  <i className="bi bi-check-circle-fill text-primary" />
                  <span><strong>View eligibility</strong> and available appointment slots</span>
                </li>
                <li>
                  <i className="bi bi-check-circle-fill text-primary" />
                  <span><strong>Access appointments</strong> and certificate services</span>
                </li>
              </ul>
              <div className="gen-card__cta">
                <Link to="/register/citizen" className="gen-card__btn gen-card__btn--youth">
                  <span>Access Online Services</span>
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
                alt="Vaccination center reception area with citizens and healthcare staff"
                className="gen-card__img"
                loading="lazy"
              />
              <span className="gen-card__badge gen-card__badge--clinic">
                <i className="bi bi-info-circle me-1" /> Center Information
              </span>
              <span className="gen-card__pill-tag gen-card__pill-tag--clinic">Service Guide</span>
            </div>
            <div className="gen-card__body">
              <div className="gen-card__audience">
                <span className="gen-card__icon-wrap gen-card__icon-wrap--clinic">
                  <i className="bi bi-hospital" aria-hidden="true" />
                </span>
                <div>
                  <h3 className="gen-card__title">Vaccination Center Services</h3>
                  <span className="gen-card__tagline">Plan your visit with clear information</span>
                </div>
              </div>
              <ul className="gen-card__perks">
                <li>
                  <i className="bi bi-check-circle-fill text-warning" />
                  <span><strong>Browse campaigns</strong> and participating center information</span>
                </li>
                <li>
                  <i className="bi bi-check-circle-fill text-warning" />
                  <span><strong>Understand appointment</strong> and arrival check-in steps</span>
                </li>
                <li>
                  <i className="bi bi-check-circle-fill text-warning" />
                  <span><strong>Learn what to bring</strong> before attending your appointment</span>
                </li>
              </ul>
              <div className="gen-card__cta">
                <Link to="/how-it-works" className="gen-card__btn gen-card__btn--clinic">
                  <span>View Citizen Service Guide</span>
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
