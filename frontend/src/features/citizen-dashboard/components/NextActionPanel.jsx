import React from 'react';
import { Link } from 'react-router-dom';
import DashboardIcon from './DashboardIcon';

export default function NextActionPanel({ action }) {
  return (
    <section
      className={`citizen-next-action citizen-next-action--${action.tone}`}
      aria-labelledby="citizen-next-action-title"
      aria-live="polite"
    >
      <div className="citizen-next-action__index" aria-hidden="true">01</div>
      <div className="citizen-next-action__copy">
        <p className="citizen-dashboard__eyebrow">{action.eyebrow}</p>
        <h2 id="citizen-next-action-title">{action.title}</h2>
        <p>{action.description}</p>
      </div>
      {action.to ? (
        <Link className="citizen-action-link citizen-action-link--primary" to={action.to}>
          {action.linkLabel}
          <DashboardIcon name="arrow" size={19} />
        </Link>
      ) : (
        <span className="citizen-next-action__loading" role="status">
          <span aria-hidden="true" /> Loading records
        </span>
      )}
    </section>
  );
}
