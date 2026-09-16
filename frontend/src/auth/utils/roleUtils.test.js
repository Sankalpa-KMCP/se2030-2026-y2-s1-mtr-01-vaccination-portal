import { describe, expect, it } from 'vitest';
import {
  canManageSessionOperations,
  canManageQuestionnaireAssignments,
  canOverrideSessionCapacity,
  canViewSessionOperations,
  canViewQuestionnaireAssignments,
} from './roleUtils';

function userWith(role) {
  return { roles: [role] };
}

describe('campaign questionnaire assignment authorization', () => {
  it.each(['ROLE_ADMIN', 'ROLE_CAMPAIGN_OFFICER'])(
    'allows %s to view and mutate questionnaire assignments',
    (role) => {
      const user = userWith(role);

      expect(canViewQuestionnaireAssignments(user)).toBe(true);
      expect(canManageQuestionnaireAssignments(user)).toBe(true);
    },
  );

  it('allows a management user to view history without mutation permission', () => {
    const user = userWith('ROLE_MANAGEMENT_USER');

    expect(canViewQuestionnaireAssignments(user)).toBe(true);
    expect(canManageQuestionnaireAssignments(user)).toBe(false);
  });

  it.each([
    'ROLE_HEALTHCARE_OFFICER',
    'ROLE_CENTER_MANAGER',
    'ROLE_SYSTEM_ADMINISTRATOR',
    'ROLE_CITIZEN',
  ])('does not expose questionnaire assignments to %s', (role) => {
    const user = userWith(role);

    expect(canViewQuestionnaireAssignments(user)).toBe(false);
    expect(canManageQuestionnaireAssignments(user)).toBe(false);
  });
});

describe('session operations authorization', () => {
  it.each(['ROLE_ADMIN', 'ADMIN', 'ROLE_CENTER_MANAGER', 'CENTER_MANAGER', 'ROLE_HEALTHCARE_OFFICER', 'HEALTHCARE_OFFICER'])(
    'allows %s to view session operations',
    (role) => {
      expect(canViewSessionOperations(userWith(role))).toBe(true);
    },
  );

  it.each(['ROLE_ADMIN', 'ADMIN', 'ROLE_CENTER_MANAGER', 'CENTER_MANAGER'])(
    'allows %s to manage session operations',
    (role) => {
      expect(canManageSessionOperations(userWith(role))).toBe(true);
    },
  );

  it.each(['ROLE_CAMPAIGN_OFFICER', 'ROLE_HEALTHCARE_OFFICER', 'ROLE_CITIZEN'])(
    'does not allow %s to manage session operations',
    (role) => {
      expect(canManageSessionOperations(userWith(role))).toBe(false);
    },
  );

  it.each(['ROLE_CENTER_MANAGER', 'CENTER_MANAGER'])(
    'allows %s to override session capacity',
    (role) => {
      expect(canOverrideSessionCapacity(userWith(role))).toBe(true);
    },
  );

  it.each(['ROLE_ADMIN', 'ROLE_HEALTHCARE_OFFICER', 'ROLE_CAMPAIGN_OFFICER'])(
    'does not allow %s to override session capacity',
    (role) => {
      expect(canOverrideSessionCapacity(userWith(role))).toBe(false);
    },
  );

  it('supports multi-role users while denying missing users safely', () => {
    const multiRoleUser = { roles: ['ROLE_ADMIN', 'ROLE_CENTER_MANAGER'] };

    expect(canViewSessionOperations(multiRoleUser)).toBe(true);
    expect(canManageSessionOperations(multiRoleUser)).toBe(true);
    expect(canOverrideSessionCapacity(multiRoleUser)).toBe(true);
    expect(canViewSessionOperations(null)).toBe(false);
    expect(canManageSessionOperations(undefined)).toBe(false);
    expect(canOverrideSessionCapacity(null)).toBe(false);
  });
});
