/**
 * Role-based authorization utility functions.
 * Aligns frontend UI visibility and actions strictly with backend @PreAuthorize policies.
 */

/**
 * Checks if the user has any of the specified roles (with or without 'ROLE_' prefix).
 *
 * @param {Object|null} user - User object containing `roles` array
 * @param {...string} roles - Role names to check against (e.g. 'ROLE_ADMIN' or 'ADMIN')
 * @returns {boolean} True if user possesses at least one matching role
 */
export function hasRole(user, ...roles) {
  if (!user || !Array.isArray(user.roles)) {
    return false;
  }

  const normalizedUserRoles = new Set(
    user.roles.map((r) => (r.startsWith('ROLE_') ? r : `ROLE_${r}`))
  );

  return roles.some((role) => {
    const normalizedTarget = role.startsWith('ROLE_') ? role : `ROLE_${role}`;
    return normalizedUserRoles.has(normalizedTarget);
  });
}

/**
 * Whether user can view Campaign Management navigation and read-only views.
 * Authorized: ADMIN, CAMPAIGN_OFFICER, CENTER_MANAGER, HEALTHCARE_OFFICER, MANAGEMENT_USER
 */
export function canViewCampaigns(user) {
  return hasRole(
    user,
    'ROLE_ADMIN',
    'ROLE_CAMPAIGN_OFFICER',
    'ROLE_CENTER_MANAGER',
    'ROLE_HEALTHCARE_OFFICER',
    'ROLE_MANAGEMENT_USER'
  );
}

/**
 * Whether user can create, configure, or transition campaign lifecycle statuses.
 * Authorized: ADMIN, CAMPAIGN_OFFICER
 */
export function canManageCampaigns(user) {
  return hasRole(user, 'ROLE_ADMIN', 'ROLE_CAMPAIGN_OFFICER');
}

/**
 * Whether user can view Center Operations navigation and center details.
 * Authorized: ADMIN, CAMPAIGN_OFFICER, CENTER_MANAGER, HEALTHCARE_OFFICER, MANAGEMENT_USER
 */
export function canViewCenters(user) {
  return hasRole(
    user,
    'ROLE_ADMIN',
    'ROLE_CAMPAIGN_OFFICER',
    'ROLE_CENTER_MANAGER',
    'ROLE_HEALTHCARE_OFFICER',
    'ROLE_MANAGEMENT_USER'
  );
}

/**
 * Whether user can create/update centers, operating hours, closures, rosters, and resources.
 * Authorized: ADMIN, CENTER_MANAGER
 */
export function canManageCenters(user) {
  return hasRole(user, 'ROLE_ADMIN', 'ROLE_CENTER_MANAGER');
}

/**
 * Whether user can view Session Management navigation and list sessions.
 * Authorized: ADMIN, CENTER_MANAGER, CAMPAIGN_OFFICER, HEALTHCARE_OFFICER
 * Note: MANAGEMENT_USER is excluded as GET /api/v1/sessions rejects this role.
 */
export function canViewSessions(user) {
  return hasRole(
    user,
    'ROLE_ADMIN',
    'ROLE_CENTER_MANAGER',
    'ROLE_CAMPAIGN_OFFICER',
    'ROLE_HEALTHCARE_OFFICER'
  );
}

/**
 * Whether user can plan, create individual/series sessions, update sessions, schedule/cancel/complete sessions, and manage recurrence rules.
 * Authorized: ADMIN, CENTER_MANAGER, CAMPAIGN_OFFICER
 */
export function canPlanSessions(user) {
  return hasRole(
    user,
    'ROLE_ADMIN',
    'ROLE_CENTER_MANAGER',
    'ROLE_CAMPAIGN_OFFICER'
  );
}

/**
 * Whether user can manage day-of-operations (capacity plans, operational assignments).
 * Authorized: ADMIN, CENTER_MANAGER
 */
export function canManageSessionOperations(user) {
  return hasRole(user, 'ROLE_ADMIN', 'ROLE_CENTER_MANAGER');
}

/**
 * Whether user can list time slots for a session.
 * Authorized: ADMIN, CENTER_MANAGER, HEALTHCARE_OFFICER
 */
export function canViewTimeSlots(user) {
  return hasRole(
    user,
    'ROLE_ADMIN',
    'ROLE_CENTER_MANAGER',
    'ROLE_HEALTHCARE_OFFICER'
  );
}

/**
 * Whether user can create time slots or modify time slot capacities.
 * Authorized: ADMIN, CENTER_MANAGER
 */
export function canManageTimeSlots(user) {
  return hasRole(user, 'ROLE_ADMIN', 'ROLE_CENTER_MANAGER');
}
