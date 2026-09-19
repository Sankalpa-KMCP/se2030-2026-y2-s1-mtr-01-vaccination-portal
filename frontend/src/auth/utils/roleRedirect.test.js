import { describe, expect, it } from 'vitest';
import { getRoleHomePath } from './roleRedirect';

describe('role-aware home redirect', () => {
  it('sends an administrator to the admin dashboard', () => {
    expect(getRoleHomePath({ roles: ['ROLE_ADMIN'] })).toBe('/app/admin');
  });

  it('sends a super administrator to the admin dashboard', () => {
    expect(getRoleHomePath({ roles: ['ROLE_SUPER_ADMIN'] })).toBe('/app/admin');
  });

  it('sends canonical and management staff roles to the staff dashboard', () => {
    expect(getRoleHomePath({ roles: ['ROLE_HEALTHCARE_OFFICER'] })).toBe('/app/staff');
    expect(getRoleHomePath({ roles: ['ROLE_MANAGEMENT_USER'] })).toBe('/app/staff');
  });

  it('sends a citizen to the citizen dashboard', () => {
    expect(getRoleHomePath({ roles: ['ROLE_CITIZEN'] })).toBe('/app/citizen');
  });

  it('recognizes a Vaccinator as staff, without administrator privileges', () => {
    expect(getRoleHomePath({ roles: ['ROLE_VACCINATOR'] })).toBe('/app/staff');
    expect(getRoleHomePath({ roles: ['VACCINATOR'] })).toBe('/app/staff');
  });

  it('gives administrator routing priority for multi-role users', () => {
    expect(getRoleHomePath({ roles: ['ROLE_CITIZEN', 'ROLE_ADMIN'] })).toBe('/app/admin');
  });

  it('sends unknown or missing roles to not authorized', () => {
    expect(getRoleHomePath({ roles: ['ROLE_UNKNOWN'] })).toBe('/app/not-authorized');
    expect(getRoleHomePath(null)).toBe('/app/not-authorized');
  });
});
