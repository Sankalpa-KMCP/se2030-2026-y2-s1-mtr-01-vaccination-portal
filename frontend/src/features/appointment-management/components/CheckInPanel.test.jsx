import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import CheckInPanel from './CheckInPanel';

afterEach(cleanup);

const callbacks = {
  onCheckIn: vi.fn(),
  onReverseCheckIn: vi.fn(),
  onMarkNoShow: vi.fn(),
};

function appointment(status, id = 101) {
  return {
    id,
    citizenId: 501,
    campaignId: 301,
    slotId: 42,
    status,
    bookedAt: '2026-09-19T09:00:00Z',
  };
}

describe('CheckInPanel mutation visibility', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('keeps appointment-specific confirmed controls and visible business labels', () => {
    render(<CheckInPanel {...callbacks} appointment={appointment('CONFIRMED')} canMutate />);

    expect(screen.getByRole('heading', { level: 3, name: 'Appointment #101' })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Check in appointment 101' }).textContent).toContain('Check In');
    expect(screen.getByRole('button', { name: 'Mark appointment 101 as no-show' }).textContent).toContain('Mark No-Show');
  });

  it('keeps the appointment-specific reverse control for an authorized checked-in appointment', () => {
    render(<CheckInPanel {...callbacks} appointment={appointment('CHECKED_IN')} canMutate />);

    expect(screen.getByRole('button', { name: 'Reverse check-in for appointment 101' }).textContent)
      .toContain('Reverse Check-In');
  });

  it('distinguishes repeated row actions with appointment-specific accessible names', () => {
    render(
      <>
        <CheckInPanel {...callbacks} appointment={appointment('CONFIRMED', 101)} canMutate />
        <CheckInPanel {...callbacks} appointment={appointment('CONFIRMED', 202)} canMutate />
      </>,
    );

    expect(screen.getByRole('button', { name: 'Check in appointment 101' })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Check in appointment 202' })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Mark appointment 101 as no-show' })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Mark appointment 202 as no-show' })).toBeTruthy();
  });

  it('hides all mutation controls when the parent grants read-only access', () => {
    render(<CheckInPanel {...callbacks} appointment={appointment('CHECKED_IN')} canMutate={false} />);

    expect(screen.queryByRole('button', { name: /check in appointment/i })).toBeNull();
    expect(screen.queryByRole('button', { name: /mark appointment .+ as no-show/i })).toBeNull();
    expect(screen.queryByRole('button', { name: /reverse check-in for appointment/i })).toBeNull();
  });
});

describe('CheckInPanel dialogs', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it.each([
    {
      status: 'CONFIRMED',
      actionName: 'Check in appointment 101',
      dialogName: 'Confirm Check-In',
      initialFocusName: 'Cancel',
    },
    {
      status: 'CONFIRMED',
      actionName: 'Mark appointment 101 as no-show',
      dialogName: 'Confirm Mark No-Show',
      initialFocusName: 'Cancel',
    },
    {
      status: 'CHECKED_IN',
      actionName: 'Reverse check-in for appointment 101',
      dialogName: 'Confirm Reverse Check-In',
      initialFocusName: 'Keep Checked In',
    },
  ])('names and initially focuses the $dialogName dialog', async ({ status, actionName, dialogName, initialFocusName }) => {
    render(<CheckInPanel {...callbacks} appointment={appointment(status)} canMutate />);

    fireEvent.click(screen.getByRole('button', { name: actionName }));

    const dialog = screen.getByRole('dialog', { name: dialogName });
    expect(dialog).toBeTruthy();
    await waitFor(() => {
      expect(document.activeElement).toBe(within(dialog).getByRole('button', { name: initialFocusName }));
    });
  });

  it('contains forward and reverse Tab navigation within the active dialog', () => {
    render(<CheckInPanel {...callbacks} appointment={appointment('CONFIRMED')} canMutate />);
    fireEvent.click(screen.getByRole('button', { name: 'Check in appointment 101' }));

    const dialog = screen.getByRole('dialog', { name: 'Confirm Check-In' });
    const first = within(dialog).getByRole('button', { name: 'Close check-in dialog' });
    const last = within(dialog).getByRole('button', { name: 'Complete Check-In' });

    last.focus();
    fireEvent.keyDown(window, { key: 'Tab' });
    expect(document.activeElement).toBe(first);

    first.focus();
    fireEvent.keyDown(window, { key: 'Tab', shiftKey: true });
    expect(document.activeElement).toBe(last);
  });

  it('dismisses with Escape and restores focus to the triggering control', async () => {
    render(<CheckInPanel {...callbacks} appointment={appointment('CHECKED_IN')} canMutate />);
    const trigger = screen.getByRole('button', { name: 'Reverse check-in for appointment 101' });
    trigger.focus();
    fireEvent.click(trigger);

    expect(screen.getByRole('dialog', { name: 'Confirm Reverse Check-In' })).toBeTruthy();
    fireEvent.keyDown(window, { key: 'Escape' });

    await waitFor(() => {
      expect(screen.queryByRole('dialog')).toBeNull();
      expect(document.activeElement).toBe(trigger);
    });
  });

  it('restores focus after a cancel button closes the dialog', async () => {
    render(<CheckInPanel {...callbacks} appointment={appointment('CONFIRMED')} canMutate />);
    const trigger = screen.getByRole('button', { name: 'Mark appointment 101 as no-show' });
    fireEvent.click(trigger);
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Cancel' }));

    await waitFor(() => {
      expect(document.activeElement).toBe(trigger);
    });
  });

  it('closes safely without focusing unrelated content when mutation removes the row', async () => {
    function RemovingRow() {
      const [currentAppointment, setCurrentAppointment] = React.useState(appointment('CHECKED_IN'));
      return (
        <>
          {currentAppointment && (
            <CheckInPanel
              appointment={currentAppointment}
              onCheckIn={callbacks.onCheckIn}
              onMarkNoShow={callbacks.onMarkNoShow}
              onReverseCheckIn={() => setCurrentAppointment(null)}
              canMutate
            />
          )}
          <button type="button">Unrelated page action</button>
        </>
      );
    }

    render(<RemovingRow />);
    const trigger = screen.getByRole('button', { name: 'Reverse check-in for appointment 101' });
    fireEvent.click(trigger);
    fireEvent.submit(screen.getByRole('dialog', { name: 'Confirm Reverse Check-In' }).querySelector('form'));

    await waitFor(() => {
      expect(screen.queryByRole('dialog')).toBeNull();
      expect(trigger.isConnected).toBe(false);
      expect(document.activeElement).toBe(document.body);
      expect(document.activeElement).not.toBe(screen.getByRole('button', { name: 'Unrelated page action' }));
    });
  });

  it('preserves the check-in callback payload and immediate dialog closure', () => {
    render(<CheckInPanel {...callbacks} appointment={appointment('CONFIRMED')} canMutate />);
    fireEvent.click(screen.getByRole('button', { name: 'Check in appointment 101' }));
    fireEvent.click(screen.getByLabelText(/Apply Staff Override/i));
    fireEvent.change(screen.getByLabelText(/Override Reason/i), { target: { value: 'Authorized arrival' } });
    fireEvent.submit(screen.getByRole('dialog', { name: 'Confirm Check-In' }).querySelector('form'));

    expect(callbacks.onCheckIn).toHaveBeenCalledWith(101, {
      isStaffOverride: true,
      staffReason: 'Authorized arrival',
    });
    expect(screen.queryByRole('dialog')).toBeNull();
  });
});
