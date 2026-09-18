export const SUPER_ADMIN_ROLE = 'ROLE_SUPER_ADMIN';
export const ADMIN_ROLE = 'ROLE_ADMIN';
export const CITIZEN_ROLE = 'ROLE_CITIZEN';

export const REQUESTABLE_STAFF_ROLES = Object.freeze([
  { code: 'ROLE_HEALTHCARE_OFFICER', label: 'Healthcare Officer' },
  { code: 'ROLE_CENTER_MANAGER', label: 'Center Manager' },
  { code: 'ROLE_CAMPAIGN_OFFICER', label: 'Campaign Officer' },
  { code: 'ROLE_INVENTORY_OFFICER', label: 'Inventory Officer' },
]);

export const REQUESTABLE_STAFF_ROLE_CODES = Object.freeze(
  REQUESTABLE_STAFF_ROLES.map(({ code }) => code),
);

export const APPROVABLE_STAFF_ROLES = Object.freeze([
  ...REQUESTABLE_STAFF_ROLES,
  { code: 'ROLE_VACCINATOR', label: 'Vaccinator' },
]);

export const STAFF_DASHBOARD_ROLES = Object.freeze([
  ...REQUESTABLE_STAFF_ROLE_CODES,
  'ROLE_MANAGEMENT_USER',
  'ROLE_VACCINATOR',
  SUPER_ADMIN_ROLE,
]);

const ROLE_LABELS = Object.freeze({
  [SUPER_ADMIN_ROLE]: 'Super Administrator',
  [ADMIN_ROLE]: 'Administrator',
  [CITIZEN_ROLE]: 'Citizen',
  ROLE_HEALTHCARE_OFFICER: 'Healthcare Officer',
  ROLE_CENTER_MANAGER: 'Center Manager',
  ROLE_CAMPAIGN_OFFICER: 'Campaign Officer',
  ROLE_INVENTORY_OFFICER: 'Inventory Officer',
  ROLE_MANAGEMENT_USER: 'Management User',
  ROLE_VACCINATOR: 'Vaccinator',
});

export function getRoleLabel(roleCode) {
  if (!roleCode) return 'Not assigned';
  return ROLE_LABELS[roleCode]
    || roleCode.replace(/^ROLE_/, '').replace(/_/g, ' ').toLowerCase()
      .replace(/\b\w/g, (character) => character.toUpperCase());
}
