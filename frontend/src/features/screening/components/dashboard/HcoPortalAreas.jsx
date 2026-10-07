import React from 'react';
import { Link } from 'react-router-dom';

const PORTAL_AREAS = [
  {
    title: 'Screening Review',
    description: 'Review citizen pre-vaccination screenings, request corrections, and record eligibility decisions.',
    to: '/app/staff/screening/reviews',
    iconClass: 'bi bi-clipboard2-pulse',
  },
  {
    title: 'Screening Questions',
    description: 'Create and maintain the clinical question and answer-option catalogue.',
    to: '/app/screening/questions',
    iconClass: 'bi bi-question-circle',
  },
  {
    title: 'Screening Questionnaires',
    description: 'Author, publish, and manage screening questionnaire versions and eligibility rules.',
    to: '/app/screening/questionnaires',
    iconClass: 'bi bi-file-earmark-medical',
  },
];

export default function HcoPortalAreas() {
  return (
    <div className="mb-3">
      <h2 className="h6 fw-bold text-uppercase text-secondary letter-spacing-sm mb-2">
        Portal Areas
      </h2>
      <div className="row g-3">
        {PORTAL_AREAS.map((area) => (
          <div key={area.title} className="col-12 col-md-4">
            <div className="card hco-portal-card">
              <div>
                <div className="d-flex align-items-center gap-2 mb-2">
                  <div className="hco-portal-icon-container" aria-hidden="true">
                    <i className={area.iconClass} />
                  </div>
                  <h3 className="hco-portal-title mb-0">{area.title}</h3>
                </div>
                <p className="hco-portal-desc">{area.description}</p>
              </div>
              <Link to={area.to} className="hco-portal-btn">
                <span>Open</span>
                <i className="bi bi-arrow-right" aria-hidden="true" />
              </Link>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
