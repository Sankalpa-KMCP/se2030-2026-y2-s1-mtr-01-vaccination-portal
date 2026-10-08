import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

const mocks = vi.hoisted(() => ({
  getProfile: vi.fn(),
  updateProfile: vi.fn(),
  updateCurrentUserProfile: vi.fn(),
  getVerificationStatus: vi.fn(),
  resendCode: vi.fn(),
  verifyCode: vi.fn(),
}));

vi.mock('../api/citizenProfileApi', () => ({
  getCitizenProfile: mocks.getProfile,
  updateCitizenProfile: mocks.updateProfile,
}));

vi.mock('../../../auth/context/AuthContext', () => ({
  useAuth: () => ({ updateCurrentUserProfile: mocks.updateCurrentUserProfile }),
}));

vi.mock('../../appointment-management/api/emailVerificationApi', () => ({
  getEmailVerificationStatus: mocks.getVerificationStatus,
  resendEmailOtp: mocks.resendCode,
  verifyEmailOtp: mocks.verifyCode,
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
    mocks.getProfile.mockReset();
    mocks.getProfile.mockResolvedValue({ ...completeProfile });
    mocks.getVerificationStatus.mockReset();
    mocks.getVerificationStatus.mockResolvedValue({ emailVerified: true, emailVerifiedAt: '2026-09-01T10:00:00Z' });
    mocks.updateProfile.mockReset();
    mocks.updateCurrentUserProfile.mockReset();
    mocks.resendCode.mockReset();
    mocks.verifyCode.mockReset();
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
    expect(screen.getByText('Email Verified')).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Verify Email' })).toBeNull();
    expect(screen.getByRole('link', { name: /back to citizen dashboard/i }).getAttribute('href')).toBe('/app/citizen');
  });

  it('offers verification and resend actions for a pending citizen', async () => {
    mocks.getProfile.mockResolvedValueOnce({ ...completeProfile, accountStatus: 'PENDING_VERIFICATION' });
    mocks.getVerificationStatus.mockResolvedValue({ emailVerified: false, emailVerifiedAt: null });
    renderPage();

    expect(await screen.findByRole('heading', { name: 'Email verification required' })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Verify Email' })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Resend Verification Code' })).toBeTruthy();
    expect(screen.queryByText('Email Verified')).toBeNull();

    fireEvent.click(screen.getByRole('button', { name: 'Verify Email' }));
    expect(await screen.findByRole('dialog', { name: 'Verify Your Email' })).toBeTruthy();
    expect(screen.getByLabelText('Six-digit verification code')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Verify Code' }).disabled).toBe(true);
    expect(screen.getByRole('button', { name: 'Resend code' })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeTruthy();
  });

  it('requests a resend for the authenticated citizen and opens code entry without claiming delivery', async () => {
    mocks.getProfile.mockResolvedValueOnce({ ...completeProfile, accountStatus: 'PENDING_VERIFICATION' });
    mocks.getVerificationStatus.mockResolvedValue({ emailVerified: false, emailVerifiedAt: null });
    mocks.resendCode.mockResolvedValueOnce({ emailVerified: false, emailVerifiedAt: null });
    renderPage();
    fireEvent.click(await screen.findByRole('button', { name: 'Resend Verification Code' }));

    expect(await screen.findByRole('dialog', { name: 'Verify Your Email' })).toBeTruthy();
    expect(mocks.resendCode).toHaveBeenCalledTimes(1);
    expect(mocks.resendCode.mock.calls[0]).toEqual([]);
    expect(screen.getByText('A new code was requested. Enter the latest code when available.')).toBeTruthy();
  });

  it('respects the server resend cooldown and shows safe retry feedback', async () => {
    mocks.getProfile.mockResolvedValueOnce({ ...completeProfile, accountStatus: 'PENDING_VERIFICATION' });
    mocks.getVerificationStatus.mockResolvedValue({ emailVerified: false, emailVerifiedAt: null });
    mocks.resendCode.mockRejectedValueOnce(Object.assign(new Error('server detail'), { status: 429, retryAfterSeconds: 45 }));
    renderPage();
    fireEvent.click(await screen.findByRole('button', { name: 'Resend Verification Code' }));

    expect(await screen.findByRole('button', { name: 'Resend Code in 45s' })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Resend Code in 45s' }).disabled).toBe(true);
    expect(screen.queryByText('server detail')).toBeNull();
    expect(mocks.resendCode).toHaveBeenCalledTimes(1);
  });

  it('shows a safe retry message when profile resend fails without exposing provider details', async () => {
    mocks.getProfile.mockResolvedValueOnce({ ...completeProfile, accountStatus: 'PENDING_VERIFICATION' });
    mocks.getVerificationStatus.mockResolvedValue({ emailVerified: false, emailVerifiedAt: null });
    mocks.resendCode.mockRejectedValueOnce(new Error('SMTP authentication details'));
    renderPage();
    fireEvent.click(await screen.findByRole('button', { name: 'Resend Verification Code' }));

    expect(await screen.findByText('We could not request a new code right now. Please try again shortly.')).toBeTruthy();
    expect(screen.queryByText('SMTP authentication details')).toBeNull();
    expect(screen.getByRole('button', { name: 'Resend Verification Code' }).disabled).toBe(false);
  });

  it('refreshes profile and shared auth status from the server after successful verification', async () => {
    const pending = { ...completeProfile, accountStatus: 'PENDING_VERIFICATION' };
    mocks.getProfile.mockResolvedValueOnce(pending).mockResolvedValueOnce({ ...completeProfile });
    mocks.getVerificationStatus
      .mockResolvedValueOnce({ emailVerified: false, emailVerifiedAt: null })
      .mockResolvedValueOnce({ emailVerified: false, emailVerifiedAt: null })
      .mockResolvedValueOnce({ emailVerified: true, emailVerifiedAt: '2026-10-08T10:00:00Z' });
    mocks.verifyCode.mockResolvedValueOnce({ emailVerified: true, emailVerifiedAt: '2026-10-08T10:00:00Z' });
    renderPage();
    fireEvent.click(await screen.findByRole('button', { name: 'Verify Email' }));
    fireEvent.change(await screen.findByLabelText('Six-digit verification code'), { target: { value: '004281' } });
    fireEvent.click(screen.getByRole('button', { name: 'Verify Code' }));
    expect(await screen.findByText('Email verified successfully.')).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: 'Done' }));

    expect(await screen.findByText('Email Verified')).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Verify Email' })).toBeNull();
    expect(mocks.verifyCode).toHaveBeenCalledWith('004281');
    expect(mocks.updateCurrentUserProfile).toHaveBeenCalledWith(expect.objectContaining({ accountStatus: 'ACTIVE' }));
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
