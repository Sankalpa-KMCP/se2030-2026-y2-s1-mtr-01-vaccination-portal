import React from 'react';
import { FALLBACK_BADGE_CLASS } from './statusVariants';

/**
 * Renders a status label with the badge class looked up in a domain-specific
 * variant map (see statusVariants.js). Unknown statuses fall back to a neutral
 * chip so new backend values never render unstyled.
 *
 * @param {Object} props
 * @param {string} props.status - Raw status value (e.g. 'SCHEDULED').
 * @param {Record<string, string>} props.variants - Domain status -> class map.
 * @param {string} [props.className] - Extra classes for spacing utilities.
 */
export default function StatusBadge({ status, variants, className = '' }) {
  const badgeClass = variants?.[status] ?? FALLBACK_BADGE_CLASS;
  return <span className={`${badgeClass}${className ? ` ${className}` : ''}`}>{status}</span>;
}
