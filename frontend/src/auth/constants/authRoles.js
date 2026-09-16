export const ADMIN_ROLE = 'ROLE_ADMIN';
export const CITIZEN_ROLE = 'ROLE_CITIZEN';
export const VACCINATOR_ROLE = 'ROLE_VACCINATOR';

export const REQUESTABLE_STAFF_ROLES = Object.freeze([
  { code: 'ROLE_HEALTHCARE_OFFICER', label: 'Healthcare Officer' },
  { code: 'ROLE_CENTER_MANAGER', label: 'Center Manager' },
  { code: 'ROLE_CAMPAIGN_OFFICER', label: 'Campaign Officer' },
  { code: 'ROLE_INVENTORY_OFFICER', label: 'Inventory Officer' },
  { code: VACCINATOR_ROLE, label: 'Vaccinator' },
]);

export const REQUESTABLE_STAFF_ROLE_CODES = Object.freeze(
  REQUESTABLE_STAFF_ROLES.map(({ code }) => code),
);

export const STAFF_DASHBOARD_ROLES = Object.freeze([
  ...REQUESTABLE_STAFF_ROLE_CODES,
  'ROLE_MANAGEMENT_USER',
]);

const ROLE_LABELS = Object.freeze({
  [ADMIN_ROLE]: 'Administrator',
  [CITIZEN_ROLE]: 'Citizen',
  ROLE_HEALTHCARE_OFFICER: 'Healthcare Officer',
  ROLE_CENTER_MANAGER: 'Center Manager',
  ROLE_CAMPAIGN_OFFICER: 'Campaign Officer',
  ROLE_INVENTORY_OFFICER: 'Inventory Officer',
  ROLE_MANAGEMENT_USER: 'Management User',
  [VACCINATOR_ROLE]: 'Vaccinator',
});

export function getRoleLabel(roleCode) {
  if (!roleCode) return 'Not assigned';
  return ROLE_LABELS[roleCode]
    || roleCode.replace(/^ROLE_/, '').replace(/_/g, ' ').toLowerCase()
      .replace(/\b\w/g, (character) => character.toUpperCase());
}
