import React from 'react';
import { Link } from 'react-router-dom';

export default function DashboardServiceCard({ title, description, to = null, badge = null }) {
  return (
    <div className="col-md-6 col-xl-4">
      <div className={`card h-100 shadow-sm ${to ? 'border-primary-subtle' : 'border-0 bg-light'}`}>
        <div className="card-body d-flex flex-column p-4">
          <div className="d-flex justify-content-between align-items-start gap-2 mb-2">
            <h3 className="h5 mb-0">{title}</h3>
            {badge && <span className="badge text-bg-light border text-dark">{badge}</span>}
          </div>
          <p className="text-muted small flex-grow-1 mb-3">{description}</p>
          {to ? (
            <Link className="btn btn-outline-primary align-self-start" to={to}>Open</Link>
          ) : (
            <span className="badge text-bg-secondary align-self-start">Coming soon</span>
          )}
        </div>
      </div>
    </div>
  );
}
