import React from 'react';
import { Link } from 'react-router-dom';

/**
 * PortalAccessCard
 * Reusable, accessible access selection card for Citizen and Staff portal paths.
 */
export default function PortalAccessCard({
  title,
  purpose,
  to,
  ctaLabel,
  icon,
  badgeText,
  badgeVariant = 'teal',
}) {
  return (
    <article className="access-choice-card">
      <div className="access-choice-card__top">
        <div className="access-choice-card__icon-wrap" aria-hidden="true">
          {icon}
        </div>
        <div className="access-choice-card__meta">
          <div className="d-flex align-items-center gap-2 mb-1">
            <h3 className="access-choice-card__title">{title}</h3>
            {badgeText && (
              <span className={`access-choice-badge access-choice-badge--${badgeVariant}`}>
                {badgeText}
              </span>
            )}
          </div>
          <p className="access-choice-card__purpose">{purpose}</p>
        </div>
      </div>
      <div className="access-choice-card__footer">
        <Link
          to={to}
          className="btn btn-outline-primary btn-sm access-choice-card__cta"
          aria-label={`${ctaLabel} (${title})`}
        >
          <span>{ctaLabel}</span>
          <span className="access-choice-card__arrow" aria-hidden="true">&rarr;</span>
        </Link>
      </div>
    </article>
  );
}
