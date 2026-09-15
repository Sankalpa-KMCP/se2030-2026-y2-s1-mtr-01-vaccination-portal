export const INVALID_EMAIL_MESSAGE = 'Enter a valid email address.';
export const INVALID_PHONE_NUMBER_MESSAGE = 'Phone number must contain exactly 10 digits.';

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PHONE_NUMBER_PATTERN = /^[0-9]{10}$/;

export function isValidEmail(value) {
  return EMAIL_PATTERN.test(String(value ?? '').trim());
}

export function isValidPhoneNumber(value) {
  return PHONE_NUMBER_PATTERN.test(String(value ?? '').trim());
}
