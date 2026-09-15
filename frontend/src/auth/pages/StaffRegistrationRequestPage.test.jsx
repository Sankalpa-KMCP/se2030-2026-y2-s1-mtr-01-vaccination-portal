import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { registerStaff } from '../api/authApi';
import StaffRegistrationRequestPage from './StaffRegistrationRequestPage';

vi.mock('../api/authApi', () => ({ registerStaff: vi.fn() }));

function renderPage() {
  return render(<MemoryRouter><StaffRegistrationRequestPage /></MemoryRouter>);
}

function fillStaffForm({
  confirmPassword = 'Secret123!',
  email = 'staff@example.com',
  phoneNumber = '0711002000',
} = {}) {
  fireEvent.change(screen.getByLabelText('Full Name'), { target: { value: 'Staff User' } });
  fireEvent.change(screen.getByLabelText('Email Address'), { target: { value: email } });
  fireEvent.change(screen.getByLabelText('Phone Number'), { target: { value: phoneNumber } });
  fireEvent.change(screen.getByLabelText('Requested Staff Role'), { target: { value: 'ROLE_CAMPAIGN_OFFICER' } });
  fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'Secret123!' } });
  fireEvent.change(screen.getByLabelText('Confirm Password'), { target: { value: confirmPassword } });
}

describe('StaffRegistrationRequestPage', () => {
  beforeEach(() => {
    registerStaff.mockReset();
  });
  afterEach(cleanup);

  it('renders only the four publicly requestable staff roles', () => {
    renderPage();
    expect(screen.getByLabelText('Phone Number').maxLength).toBe(10);
    expect(screen.getByLabelText('Phone Number').inputMode).toBe('numeric');
    const options = [...screen.getByLabelText('Requested Staff Role').options];
    expect(options.map((option) => option.textContent)).toEqual([
      'Healthcare Officer',
      'Center Manager',
      'Campaign Officer',
      'Inventory Officer',
    ]);
    expect(options.map((option) => option.value)).not.toContain('ROLE_ADMIN');
  });

  it('blocks mismatched passwords', () => {
    renderPage();
    fillStaffForm({ confirmPassword: 'Mismatch123!' });
    fireEvent.click(screen.getByRole('button', { name: 'Submit Staff Account Request' }));
    expect(screen.getByRole('alert').textContent).toContain('Passwords do not match');
    expect(registerStaff).not.toHaveBeenCalled();
  });

  it.each(['student', 'student@', '@gmail.com', 'student gmail.com', 'student@domain'])(
    'blocks malformed email %s',
    (email) => {
      renderPage();
      fillStaffForm({ email });
      fireEvent.click(screen.getByRole('button', { name: 'Submit Staff Account Request' }));

      expect(screen.getByRole('alert').textContent).toContain('Enter a valid email address.');
      expect(registerStaff).not.toHaveBeenCalled();
    },
  );

  it.each(['771234567', '07712345678', '07712ABC67', '077-123-4567', '+94771234567', '077 1234567'])(
    'blocks invalid phone number %s',
    (phoneNumber) => {
      renderPage();
      fillStaffForm({ phoneNumber });
      fireEvent.click(screen.getByRole('button', { name: 'Submit Staff Account Request' }));

      expect(screen.getByRole('alert').textContent).toContain('Phone number must contain exactly 10 digits.');
      expect(registerStaff).not.toHaveBeenCalled();
    },
  );

  it('shows the pending-approval success state without logging in', async () => {
    registerStaff.mockResolvedValue({ id: 2, accountStatus: 'PENDING_APPROVAL' });
    renderPage();
    fillStaffForm();
    fireEvent.click(screen.getByRole('button', { name: 'Submit Staff Account Request' }));

    expect(await screen.findByRole('heading', { name: /awaiting approval/i })).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Go to Staff Login' })).toBeTruthy();
    expect(registerStaff).toHaveBeenCalledWith(expect.objectContaining({
      email: 'staff@example.com',
      phoneNumber: '0711002000',
      requestedRoleCode: 'ROLE_CAMPAIGN_OFFICER',
    }));
  });

  it('shows a backend request error', async () => {
    registerStaff.mockRejectedValue(new Error('Email is already registered'));
    renderPage();
    fillStaffForm();
    fireEvent.click(screen.getByRole('button', { name: 'Submit Staff Account Request' }));
    await waitFor(() => expect(screen.getByRole('alert').textContent).toContain('Email is already registered'));
  });
});
