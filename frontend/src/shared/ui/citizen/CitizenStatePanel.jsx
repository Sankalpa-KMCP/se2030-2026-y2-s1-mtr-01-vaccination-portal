import React from 'react';
import './citizen-ui.css';

export default function CitizenStatePanel({
  eyebrow,
  title,
  children,
  actions,
  tone = 'neutral',
  status = false,
}) {
  const role = tone === 'error' ? 'alert' : status ? 'status' : undefined;

  return (
    <div className={`citizen-state citizen-state--${tone}`} role={role}>
      <span className="citizen-state__marker" aria-hidden="true" />
      <div className="citizen-state__copy">
        {eyebrow && <p className="citizen-ui-eyebrow">{eyebrow}</p>}
        <h2>{title}</h2>
        {children && <div className="citizen-state__description">{children}</div>}
      </div>
      {actions && <div className="citizen-state__actions">{actions}</div>}
    </div>
  );
}
