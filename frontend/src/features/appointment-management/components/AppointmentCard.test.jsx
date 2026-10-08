import React from 'react';
import { afterEach, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import AppointmentCard from './AppointmentCard';

afterEach(cleanup);

it('keeps citizen cancellation self-service and directs late changes to center staff', () => {
  const onCancel = vi.fn();
  render(<AppointmentCard appointment={{ id: 8, citizenId: 2, campaignId: 1, slotId: 401, status: 'CONFIRMED' }} onCancel={onCancel} onReschedule={() => {}} />);
  fireEvent.click(screen.getByRole('button', { name: 'Cancel Appointment' }));
  expect(screen.queryByRole('checkbox')).toBeNull();
  expect(screen.getByText(/contact your vaccination center staff/i)).toBeTruthy();
  fireEvent.click(screen.getByRole('button', { name: 'Yes, Cancel Appointment' }));
  expect(onCancel).toHaveBeenCalledWith(8, { isStaffOverride: false, staffReason: null });
});
