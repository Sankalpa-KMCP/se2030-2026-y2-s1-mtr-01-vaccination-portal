import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

const mocks = vi.hoisted(() => ({
  getProfile: vi.fn(),
  updateProfile: vi.fn(),
  updateCurrentUserProfile: vi.fn(),
}));

vi.mock('../api/citizenProfileApi', () => ({
  getCitizenProfile: mocks.getProfile,
  updateCitizenProfile: mocks.updateProfile,
}));

vi.mock('../../../auth/context/AuthContext', () => ({
  useAuth: () => ({ updateCurrentUserProfile: mocks.updateCurrentUserProfile }),
}));

import CitizenProfilePage from './CitizenProfilePage';

const completeProfile = {
  id: 17,
  fullName: 'Arusha Citizen',
  email: 'arusha@example.com',
  phoneNumber: '0771234567',
  accountStatus: 'ACTIVE',
  roles: ['ROLE_CITIZEN'],
  createdAt: '2026-08-01T10:00:00Z',
  dateOfBirth: '1994-06-15',
  gender: 'FEMALE',
  address: '10 Green Road, Colombo',
  updatedAt: '2026-09-01T10:00:00Z',
};

function renderPage() {
  return render(<MemoryRouter><CitizenProfilePage /></MemoryRouter>);
}

async function openEditor() {
  renderPage();
  await screen.findByRole('heading', { name: 'My Profile' });
  await screen.findByText('Arusha Citizen');
  fireEvent.click(screen.getByRole('button', { name: 'Edit Profile' }));
}

describe('CitizenProfilePage', () => {
  beforeEach(() => {
    mocks.getProfile.mockResolvedValue({ ...completeProfile });
    mocks.updateProfile.mockReset();
    mocks.updateCurrentUserProfile.mockReset();
  });

  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  it('loads and presents account, personal, and contact information', async () => {
    renderPage();
    expect(screen.getAllByText('Loading your profile...')).toHaveLength(2);

    expect(await screen.findByText('Arusha Citizen')).toBeTruthy();
    expect(screen.getByText('Citizen ID')).toBeTruthy();
    expect(screen.getByText('#17')).toBeTruthy();
    expect(screen.getByText('arusha@example.com')).toBeTruthy();
    expect(screen.getByText('0771234567')).toBeTruthy();
    expect(screen.getAllByText('Active')).toHaveLength(2);
    expect(screen.getByText('Citizen')).toBeTruthy();
    expect(screen.getByText(/Aug 1, 2026/)).toBeTruthy();
    expect(screen.getByText(/Sep 1, 2026/)).toBeTruthy();
    expect(screen.getByText(/Jun 15, 1994/)).toBeTruthy();
    expect(screen.getByText('Female')).toBeTruthy();
    expect(screen.getByText('10 Green Road, Colombo')).toBeTruthy();
    expect(screen.getByRole('link', { name: /back to citizen dashboard/i }).getAttribute('href')).toBe('/app/citizen');
  });

  it('renders legacy nullable values as Not provided', async () => {
    mocks.getProfile.mockResolvedValueOnce({
      ...completeProfile,
      dateOfBirth: null,
      gender: null,
      address: null,
    });
    renderPage();

    await screen.findByText('Arusha Citizen');
    expect(screen.getAllByText('Not provided')).toHaveLength(3);
  });

  it('shows a retryable initial-load failure', async () => {
    mocks.getProfile
      .mockRejectedValueOnce(Object.assign(new Error('Profile service unavailable'), { status: 500 }))
      .mockResolvedValueOnce({ ...completeProfile });
    renderPage();

    expect(await screen.findByText('Profile service unavailable')).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: 'Retry' }));
    expect(await screen.findByText('Arusha Citizen')).toBeTruthy();
    expect(mocks.getProfile).toHaveBeenCalledTimes(2);
  });

  it('limits edit mode to the five supported profile fields', async () => {
    await openEditor();

    expect(screen.getByLabelText('Full name')).toBeTruthy();
    expect(screen.getByLabelText('Phone number')).toBeTruthy();
    expect(screen.getByLabelText('Phone number').maxLength).toBe(10);
    expect(screen.getByLabelText('Phone number').inputMode).toBe('numeric');
    expect(screen.getByLabelText(/Date of birth/)).toBeTruthy();
    expect(screen.getByLabelText(/Gender/)).toBeTruthy();
    expect(screen.getByLabelText(/Address/)).toBeTruthy();
    expect(screen.queryByLabelText('Email address')).toBeNull();
    expect(screen.queryByLabelText(/role/i)).toBeNull();
    expect(screen.queryByLabelText(/account status/i)).toBeNull();
    expect(screen.queryByLabelText(/citizen id/i)).toBeNull();
    expect(screen.getByText('arusha@example.com')).toBeTruthy();
    expect(screen.getByText('#17')).toBeTruthy();
    expect(screen.getAllByText('Active')).toHaveLength(2);
  });

  it('blocks blank required fields and a future date without calling the API', async () => {
    await openEditor();
    fireEvent.change(screen.getByLabelText('Full name'), { target: { value: '   ' } });
    fireEvent.change(screen.getByLabelText('Phone number'), { target: { value: '' } });
    fireEvent.change(screen.getByLabelText(/Date of birth/), { target: { value: '2999-01-01' } });
    fireEvent.click(screen.getByRole('button', { name: 'Save Changes' }));

    expect(await screen.findByText('Full name is required.')).toBeTruthy();
    expect(screen.getByText('Phone number is required.')).toBeTruthy();
    expect(screen.getByText('Date of birth must not be in the future.')).toBeTruthy();
    expect(mocks.updateProfile).not.toHaveBeenCalled();
  });

  it('shows length validation and keeps the editor open', async () => {
    await openEditor();
    fireEvent.change(screen.getByLabelText('Full name'), { target: { value: 'A'.repeat(256) } });
    fireEvent.change(screen.getByLabelText(/Address/), { target: { value: 'B'.repeat(501) } });
    fireEvent.submit(screen.getByRole('button', { name: 'Save Changes' }).closest('form'));

    expect(await screen.findByText('Full name must not exceed 255 characters.')).toBeTruthy();
    expect(screen.getByText('Address must not exceed 500 characters.')).toBeTruthy();
    expect(screen.getByRole('heading', { name: 'Edit your profile' })).toBeTruthy();
  });

  it.each(['771234567', '07712345678', '07712ABC67', '077-123-4567', '+94771234567', '077 1234567'])(
    'blocks invalid phone number %s and preserves it for correction',
    async (phoneNumber) => {
      await openEditor();
      fireEvent.change(screen.getByLabelText('Phone number'), { target: { value: phoneNumber } });
      fireEvent.click(screen.getByRole('button', { name: 'Save Changes' }));

      expect(await screen.findByText('Phone number must contain exactly 10 digits.')).toBeTruthy();
      expect(screen.getByLabelText('Phone number').value).toBe(phoneNumber);
      expect(mocks.updateProfile).not.toHaveBeenCalled();
    },
  );

  it('shows a legacy phone in view mode and accepts a corrected 10-digit phone', async () => {
    mocks.getProfile.mockResolvedValueOnce({ ...completeProfile, phoneNumber: '+94 77 123 4567' });
    mocks.updateProfile.mockResolvedValueOnce({ ...completeProfile, phoneNumber: '0777654321' });
    await openEditor();

    expect(screen.getByLabelText('Phone number').value).toBe('+94 77 123 4567');
    fireEvent.change(screen.getByLabelText('Phone number'), { target: { value: '0777654321' } });
    fireEvent.click(screen.getByRole('button', { name: 'Save Changes' }));

    expect(await screen.findByText('Profile updated successfully.')).toBeTruthy();
    expect(mocks.updateProfile).toHaveBeenCalledWith(expect.objectContaining({ phoneNumber: '0777654321' }));
  });

  it('accepts a valid date and allows every optional field to be cleared', async () => {
    mocks.updateProfile.mockResolvedValueOnce({
      ...completeProfile,
      dateOfBirth: null,
      gender: null,
      address: null,
    });
    await openEditor();
    fireEvent.change(screen.getByLabelText(/Date of birth/), { target: { value: '' } });
    fireEvent.change(screen.getByLabelText(/Gender/), { target: { value: '' } });
    fireEvent.change(screen.getByLabelText(/Address/), { target: { value: '' } });
    fireEvent.click(screen.getByRole('button', { name: 'Save Changes' }));

    expect(await screen.findByText('Profile updated successfully.')).toBeTruthy();
    expect(mocks.updateProfile).toHaveBeenCalledWith(expect.objectContaining({
      dateOfBirth: '',
      gender: '',
      address: '',
    }));
    expect(screen.getAllByText('Not provided')).toHaveLength(3);
  });

  it('accepts a valid YYYY-MM-DD date and controlled gender value', async () => {
    mocks.updateProfile.mockResolvedValueOnce({ ...completeProfile, dateOfBirth: '2000-01-02', gender: 'OTHER' });
    await openEditor();
    fireEvent.change(screen.getByLabelText(/Date of birth/), { target: { value: '2000-01-02' } });
    fireEvent.change(screen.getByLabelText(/Gender/), { target: { value: 'OTHER' } });
    fireEvent.click(screen.getByRole('button', { name: 'Save Changes' }));

    await screen.findByText('Profile updated successfully.');
    expect(mocks.updateProfile).toHaveBeenCalledWith(expect.objectContaining({
      dateOfBirth: '2000-01-02',
      gender: 'OTHER',
    }));
    expect(screen.getByText('Other')).toBeTruthy();
  });

  it('uses the returned profile as authoritative and updates shared auth state', async () => {
    const returnedProfile = {
      ...completeProfile,
      fullName: 'Updated Citizen',
      phoneNumber: '0112223333',
      gender: 'PREFER_NOT_TO_SAY',
      updatedAt: '2026-09-12T08:00:00Z',
    };
    mocks.updateProfile.mockResolvedValueOnce(returnedProfile);
    await openEditor();
    fireEvent.change(screen.getByLabelText('Full name'), { target: { value: '  Client Entry  ' } });
    fireEvent.change(screen.getByLabelText('Phone number'), { target: { value: '0112223333' } });
    fireEvent.click(screen.getByRole('button', { name: 'Save Changes' }));

    expect(await screen.findByText('Profile updated successfully.')).toBeTruthy();
    expect(mocks.updateProfile).toHaveBeenCalledWith({
      fullName: '  Client Entry  ',
      phoneNumber: '0112223333',
      dateOfBirth: '1994-06-15',
      gender: 'FEMALE',
      address: '10 Green Road, Colombo',
    });
    expect(screen.getByText('Updated Citizen')).toBeTruthy();
    expect(screen.queryByText('Client Entry')).toBeNull();
    expect(mocks.updateCurrentUserProfile).toHaveBeenCalledWith(returnedProfile);
    expect(mocks.updateProfile.mock.calls[0][0]).not.toHaveProperty('id');
    expect(mocks.updateProfile.mock.calls[0][0]).not.toHaveProperty('userId');
  });

  it('prevents duplicate submissions while a save is pending', async () => {
    let resolveSave;
    mocks.updateProfile.mockReturnValueOnce(new Promise((resolve) => { resolveSave = resolve; }));
    await openEditor();
    const form = screen.getByRole('button', { name: 'Save Changes' }).closest('form');

    fireEvent.submit(form);
    fireEvent.submit(form);
    expect(mocks.updateProfile).toHaveBeenCalledTimes(1);
    expect(screen.getByRole('button', { name: 'Saving...' }).disabled).toBe(true);

    await act(async () => resolveSave({ ...completeProfile }));
    await screen.findByText('Profile updated successfully.');
  });

  it.each([
    [400, 'Validation failed'],
    [500, 'Profile service unavailable'],
  ])('preserves entered values without automatic retry after a %s save error', async (status, message) => {
    mocks.updateProfile.mockRejectedValueOnce(Object.assign(new Error(message), { status }));
    await openEditor();
    fireEvent.change(screen.getByLabelText('Full name'), { target: { value: 'Entered Citizen' } });
    fireEvent.click(screen.getByRole('button', { name: 'Save Changes' }));

    expect(await screen.findByText(message)).toBeTruthy();
    expect(screen.getByLabelText('Full name').value).toBe('Entered Citizen');
    await waitFor(() => expect(mocks.updateProfile).toHaveBeenCalledTimes(1));
  });

  it('restores the latest server profile when editing is cancelled', async () => {
    await openEditor();
    fireEvent.change(screen.getByLabelText('Full name'), { target: { value: 'Discard this' } });
    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(screen.getByText('Arusha Citizen')).toBeTruthy();
    expect(screen.queryByText('Discard this')).toBeNull();
    expect(mocks.updateProfile).not.toHaveBeenCalled();
  });

  it('lets a legacy citizen populate previously missing profile details', async () => {
    mocks.getProfile.mockResolvedValueOnce({
      ...completeProfile,
      dateOfBirth: null,
      gender: null,
      address: null,
    });
    mocks.updateProfile.mockResolvedValueOnce({
      ...completeProfile,
      dateOfBirth: '1998-03-04',
      gender: 'PREFER_NOT_TO_SAY',
      address: 'New address',
    });
    await openEditor();
    fireEvent.change(screen.getByLabelText(/Date of birth/), { target: { value: '1998-03-04' } });
    fireEvent.change(screen.getByLabelText(/Gender/), { target: { value: 'PREFER_NOT_TO_SAY' } });
    fireEvent.change(screen.getByLabelText(/Address/), { target: { value: 'New address' } });
    fireEvent.click(screen.getByRole('button', { name: 'Save Changes' }));

    expect(await screen.findByText('Profile updated successfully.')).toBeTruthy();
    expect(screen.getByText('Prefer not to say')).toBeTruthy();
    expect(screen.getByText('New address')).toBeTruthy();
  });
});
