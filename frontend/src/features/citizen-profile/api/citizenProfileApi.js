import { apiRequestData } from '../../../services/apiClient';

const PROFILE_PATH = '/v1/profile/me';

function requiredText(value) {
  return typeof value === 'string' ? value.trim() : '';
}

function optionalText(value) {
  const trimmed = requiredText(value);
  return trimmed || null;
}

export function buildCitizenProfileUpdate(input = {}) {
  return {
    fullName: requiredText(input.fullName),
    phoneNumber: requiredText(input.phoneNumber),
    dateOfBirth: input.dateOfBirth || null,
    gender: input.gender || null,
    address: optionalText(input.address),
  };
}

export function getCitizenProfile() {
  return apiRequestData(
    PROFILE_PATH,
    { method: 'GET' },
    { defaultMessage: 'Unable to load your profile.' },
  );
}

export function updateCitizenProfile(input) {
  return apiRequestData(
    PROFILE_PATH,
    {
      method: 'PUT',
      body: JSON.stringify(buildCitizenProfileUpdate(input)),
    },
    { defaultMessage: 'Unable to update your profile.' },
  );
}
