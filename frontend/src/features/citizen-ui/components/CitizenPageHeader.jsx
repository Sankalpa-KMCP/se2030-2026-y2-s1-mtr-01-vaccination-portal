import React from 'react';
import { Link } from 'react-router-dom';
import '../citizen-ui.css';

export default function CitizenPageHeader({
  eyebrow,
  title,
  subtitle,
  backTo,
  backLabel = 'Back',
  badge,
  actions,
  meta,
  variant = 'standard',
  titleId,
}) {
  const hasSupportingContent = badge || meta;

  return (
    <header className={`citizen-page-header citizen-page-header--${variant}`}>
      {backTo && (
        <Link className="citizen-page-header__back" to={backTo}>
          <span aria-hidden="true">←</span>
          <span>{backLabel}</span>
        </Link>
      )}

      <div className="citizen-page-header__layout">
        <div className="citizen-page-header__copy">
          {eyebrow && <p className="citizen-page-header__eyebrow">{eyebrow}</p>}
          <h1 className="citizen-page-header__title" id={titleId}>{title}</h1>
          {subtitle && <p className="citizen-page-header__subtitle">{subtitle}</p>}
        </div>

        {(hasSupportingContent || actions) && (
          <div className="citizen-page-header__aside">
            {hasSupportingContent && (
              <div className="citizen-page-header__context">
                {badge && <div className="citizen-page-header__badge">{badge}</div>}
                {meta && <div className="citizen-page-header__meta">{meta}</div>}
              </div>
            )}
            {actions && <div className="citizen-page-header__actions">{actions}</div>}
          </div>
        )}
      </div>
    </header>
  );
}
