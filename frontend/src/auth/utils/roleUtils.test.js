import { describe, expect, it } from 'vitest';
import {
  canManageQuestionnaireAssignments,
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
