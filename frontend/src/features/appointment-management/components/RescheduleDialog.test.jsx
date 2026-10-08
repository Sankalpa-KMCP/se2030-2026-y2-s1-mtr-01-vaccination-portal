import React from 'react';
import { afterEach, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import RescheduleDialog from './RescheduleDialog';
import { searchAvailableAppointments } from '../api/availabilityApi';
import { rescheduleAppointment } from '../api/appointmentApi';

vi.mock('../api/availabilityApi', () => ({ searchAvailableAppointments: vi.fn() }));
vi.mock('../api/appointmentApi', () => ({ rescheduleAppointment: vi.fn() }));
afterEach(() => { cleanup(); vi.clearAllMocks(); });

it('reschedules to the selected time-slot ID when two slots belong to the same session', async () => {
  searchAvailableAppointments.mockResolvedValue([
    { sessionId: 101, slotId: 501, centerName: 'Early slot', startAt: '2030-01-02T08:00:00Z', remainingCapacity: 2 },
    { sessionId: 101, slotId: 502, centerName: 'Later slot', startAt: '2030-01-02T09:00:00Z', remainingCapacity: 3 },
  ]);
  rescheduleAppointment.mockResolvedValue({ id: 8, slotId: 502 });
  const onSuccess = vi.fn();
  render(<RescheduleDialog appointment={{ id: 8, slotId: 401, campaignId: 1 }} citizenId={2} onSuccess={onSuccess} onClose={() => {}} />);
  fireEvent.click(screen.getByRole('button', { name: /search/i }));
  fireEvent.click(await screen.findByRole('button', { name: /Later slot/i }));
  fireEvent.click(screen.getByRole('button', { name: /Confirm Reschedule/i }));
  await waitFor(() => expect(rescheduleAppointment).toHaveBeenCalledWith(8, {
    requestingCitizenId: 2, newSlotId: 502, isStaffOverride: false, staffReason: null,
  }));
  expect(onSuccess).toHaveBeenCalledWith({ id: 8, slotId: 502 });
});

it('offers citizen guidance instead of staff override controls', () => {
  render(<RescheduleDialog appointment={{ id: 8, slotId: 401, campaignId: 1 }} citizenId={2} onSuccess={() => {}} onClose={() => {}} />);
  expect(screen.queryByRole('checkbox')).toBeNull();
  expect(screen.getByText(/contact your vaccination center staff/i)).toBeTruthy();
});

it('directs a rejected late reschedule to center staff', async () => {
  const error = new Error('Modifications within 24 hours of session start require staff override');
  error.status = 403;
  rescheduleAppointment.mockRejectedValue(error);
  render(<RescheduleDialog appointment={{ id: 8, slotId: 401, campaignId: 1 }} citizenId={2} onSuccess={() => {}} onClose={() => {}} />);
  fireEvent.change(screen.getByRole('spinbutton'), { target: { value: '502' } });
  fireEvent.click(screen.getByRole('button', { name: /Confirm Reschedule/i }));
  expect(await screen.findByText('To reschedule within 24 hours of your appointment, contact your vaccination center staff.')).toBeTruthy();
});
