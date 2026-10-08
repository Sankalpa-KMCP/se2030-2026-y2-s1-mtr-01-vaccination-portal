import {
  SUPER_ADMIN_ROLE,
  ADMIN_ROLE,
  CITIZEN_ROLE,
  STAFF_DASHBOARD_ROLES,
} from '../constants/authRoles';
import { hasRole } from './roleUtils';

export function getRoleHomePath(user) {
  if (hasRole(user, SUPER_ADMIN_ROLE) || hasRole(user, ADMIN_ROLE)) {
    return '/app/admin';
  }

  if (hasRole(user, ...STAFF_DASHBOARD_ROLES)) {
    return '/app/staff';
  }

  if (hasRole(user, CITIZEN_ROLE)) {
    return '/app/citizen';
  }

  return '/app/not-authorized';
}
