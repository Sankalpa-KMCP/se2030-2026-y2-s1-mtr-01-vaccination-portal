import React from 'react';

/**
 * Shared page header: optional context eyebrow, one title scale, subtitle,
 * and a right-aligned action slot. Replaces the six hand-rolled header
 * variants across staff/admin pages.
 *
 * @param {Object} props
 * @param {string} props.title - Page title (h1).
 * @param {string} [props.eyebrow] - Small context label above the title.
 * @param {string} [props.eyebrowClass] - Badge classes for the eyebrow.
 * @param {string} [props.subtitle] - Muted supporting line under the title.
 * @param {React.ReactNode} [props.actions] - Right-aligned action buttons.
 * @param {string} [props.id] - Heading id for aria-labelledby wiring.
 */
export default function PageHeader({ title, eyebrow, eyebrowClass = 'badge bg-primary-subtle text-primary-emphasis border border-primary-subtle fw-semibold', subtitle, actions, id }) {
  const headingId = id || `page-heading-${title.toLowerCase().replace(/[^a-z0-9]+/g, '-')}`;
  return (
    <div className="d-flex flex-column flex-md-row justify-content-between align-items-md-end gap-3 mb-4">
      <div>
        {eyebrow && (
          <span className={`${eyebrowClass} mb-2 d-inline-block`}>{eyebrow}</span>
        )}
        <h1 id={headingId} className="h3 fw-bold text-primary mb-1">{title}</h1>
        {subtitle && <p className="text-secondary small mb-0">{subtitle}</p>}
      </div>
      {actions && (
        <div className="d-flex flex-wrap gap-2 flex-shrink-0">{actions}</div>
      )}
    </div>
  );
}
