import React from 'react';
import '../../../shared/ui/citizen/citizen-ui.css';

export default function CitizenSection({
  eyebrow,
  title,
  titleId,
  description,
  aside,
  children,
  className = '',
}) {
  return (
    <section className={`citizen-section ${className}`.trim()} aria-labelledby={titleId}>
      <header className="citizen-section__header">
        <div>
          {eyebrow && <p className="citizen-ui-eyebrow">{eyebrow}</p>}
          <h2 id={titleId}>{title}</h2>
          {description && <p className="citizen-section__description">{description}</p>}
        </div>
        {aside && <div className="citizen-section__aside">{aside}</div>}
      </header>
      {children}
    </section>
  );
}
