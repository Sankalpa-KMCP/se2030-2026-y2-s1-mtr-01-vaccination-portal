import { describe, expect, it } from 'vitest';
import { isValidEmail, isValidPhoneNumber } from './contactValidation';

describe('contactValidation', () => {
  it.each([
    'student@gmail.com',
    'student@university.edu',
    'admin@email.com',
    '  student@gmail.com  ',
  ])('accepts valid email %s', (email) => {
    expect(isValidEmail(email)).toBe(true);
  });

  it.each([
    'student',
    'student@',
    '@gmail.com',
    'student gmail.com',
    'student@domain',
    '',
  ])('rejects invalid email %s', (email) => {
    expect(isValidEmail(email)).toBe(false);
  });

  it.each([
    '0771234567',
    '  0771234567  ',
  ])('accepts valid phone number %s', (phoneNumber) => {
    expect(isValidPhoneNumber(phoneNumber)).toBe(true);
  });

  it.each([
    '771234567',
    '07712345678',
    '07712ABC67',
    '077-123-4567',
    '+94771234567',
    '077 1234567',
    '',
  ])('rejects invalid phone number %s', (phoneNumber) => {
    expect(isValidPhoneNumber(phoneNumber)).toBe(false);
  });
});
