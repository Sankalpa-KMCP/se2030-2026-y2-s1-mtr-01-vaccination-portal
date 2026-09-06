import React from 'react';

/**
 * Shared empty-state block: icon, title, explanation, and optional call to
 * action. Replaces the emoji blocks, colSpan rows, and alert-as-empty-state
 * patterns scattered across staff/admin screens.
 *
 * @param {Object} props
 * @param {string} props.icon - Bootstrap Icons class (e.g. 'bi-calendar-week').
 * @param {string} props.title - Short "nothing here" statement.
 * @param {React.ReactNode} [props.children] - Supporting explanation.
 * @param {React.ReactNode} [props.action] - Optional CTA button/link.
 * @param {boolean} [props.compact] - Tighter padding for in-table use.
 */
export default function EmptyState({ icon, title, children, action, compact = false }) {
  return (
    <div className={`text-center ${compact ? 'py-4 px-3' : 'py-5 px-4'}`}>
      {icon && (
        <div className="text-secondary mb-3">
          <i className={`bi ${icon} ${compact ? 'fs-2' : 'display-6'}`} aria-hidden="true"></i>
        </div>
      )}
      <h3 className={compact ? 'h6 fw-bold text-dark mb-2' : 'h5 fw-bold text-dark mb-2'}>{title}</h3>
      {children && <p className="text-secondary small mb-3">{children}</p>}
      {action && <div>{action}</div>}
    </div>
  );
}
