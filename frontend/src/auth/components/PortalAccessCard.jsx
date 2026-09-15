import React from 'react';
import { Link } from 'react-router-dom';
import { ArrowIcon } from './AuthIcons';

export default function PortalAccessCard({ title, purpose, to, ctaLabel, icon }) {
  return (
    <article className="auth-access-card">
      <Link to={to} aria-label={`${ctaLabel} (${title} Portal)`}>
        <span className="auth-access-card__icon">{icon}</span>
        <span className="auth-access-card__copy"><b>{title}</b><small>{purpose}</small></span>
        <ArrowIcon />
      </Link>
    </article>
  );
}
