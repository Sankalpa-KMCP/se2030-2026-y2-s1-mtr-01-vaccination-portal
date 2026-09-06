/**
 * Domain-specific status -> badge-class maps.
 *
 * Rendering is centralized in <StatusBadge>, but each domain owns its own
 * mapping so appointment/session/campaign/screening semantics never get
 * forced into one universal color scheme. Recipes use Bootstrap soft chips
 * (bg-*-subtle + text-*-emphasis + border) for a quiet status language;
 * solid variants are reserved for states that must stand out.
 */

function soft(hue) {
  return `badge bg-${hue}-subtle text-${hue}-emphasis border border-${hue}-subtle fw-semibold`;
}

/* Campaign lifecycle (MF-02 campaign management). */
export const campaignStatusVariants = {
  DRAFT: soft('secondary'),
  SCHEDULED: soft('info'),
  PUBLISHED: soft('primary'),
  ACTIVE: soft('success'),
  SUSPENDED: soft('warning'),
  CANCELLED: soft('danger'),
  COMPLETED: soft('dark'),
  ARCHIVED: soft('secondary'),
};

/* Vaccination session lifecycle (session management). */
export const sessionStatusVariants = {
  DRAFT: soft('secondary'),
  SCHEDULED: soft('info'),
  IN_PROGRESS: soft('success'),
  COMPLETED: soft('dark'),
  CANCELLED: soft('danger'),
};

/* Booking-window status of a session (session management). */
export const sessionBookingStatusVariants = {
  OPEN: soft('success'),
  FULL: soft('warning'),
  SUSPENDED: soft('danger'),
  NOT_OPEN: soft('info'),
  CLOSED: soft('secondary'),
};

/* Tooltip copy for booking-window chips (session management). */
export const sessionBookingStatusTitles = {
  OPEN: 'Booking Intake Open',
  FULL: 'Fully Booked (Waitlist Available)',
  SUSPENDED: 'Booking Intake Suspended',
  NOT_OPEN: 'Booking Window Not Open Yet',
  CLOSED: 'Booking Closed',
};

/* Citizen appointment lifecycle (appointment management). */
export const appointmentStatusVariants = {
  CONFIRMED: soft('success'),
  CHECKED_IN: soft('info'),
  COMPLETED: soft('primary'),
  CANCELLED: soft('secondary'),
  NO_SHOW: soft('danger'),
};

/* Waitlist entry lifecycle (appointment management). */
export const waitlistStatusVariants = {
  WAITING: soft('warning'),
  PROMOTED: soft('success'),
  SKIPPED_INELIGIBLE: soft('secondary'),
  SKIPPED_DUPLICATE_APPOINTMENT: soft('secondary'),
  CANCELLED: soft('secondary'),
  SESSION_CANCELLED: soft('danger'),
  EXPIRED: soft('dark'),
};

/* Screening questionnaire lifecycle (screening administration). */
export const screeningStatusVariants = {
  DRAFT: soft('secondary'),
  PUBLISHED: soft('primary'),
  ACTIVE: soft('success'),
  RETIRED: soft('dark'),
  ARCHIVED: soft('secondary'),
};

export const FALLBACK_BADGE_CLASS = soft('secondary');
