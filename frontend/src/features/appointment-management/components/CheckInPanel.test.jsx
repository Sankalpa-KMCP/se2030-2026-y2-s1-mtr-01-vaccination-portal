import React from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import CheckInPanel from './CheckInPanel';

afterEach(cleanup);

const callbacks = {
  onCheckIn: vi.fn(),
  onReverseCheckIn: vi.fn(),
  onMarkNoShow: vi.fn(),
};

function appointment(status) {
  return {
    id: 101,
    citizenId: 501,
    campaignId: 301,
    slotId: 42,
    status,
    bookedAt: '2026-09-19T09:00:00Z',
  };
}

describe('CheckInPanel mutation visibility', () => {
  it('keeps confirmed-appointment controls available to an authorized parent', () => {
    render(<CheckInPanel {...callbacks} appointment={appointment('CONFIRMED')} canMutate />);

    expect(screen.getByRole('button', { name: 'Check In' })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Mark No-Show' })).toBeTruthy();
  });

  it('keeps reverse control available for an authorized checked-in appointment', () => {
    render(<CheckInPanel {...callbacks} appointment={appointment('CHECKED_IN')} canMutate />);

    expect(screen.getByRole('button', { name: 'Reverse Check-In' })).toBeTruthy();
  });

  it('hides all mutation controls when the parent grants read-only access', () => {
    render(<CheckInPanel {...callbacks} appointment={appointment('CHECKED_IN')} canMutate={false} />);

    expect(screen.queryByRole('button', { name: 'Check In' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Mark No-Show' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Reverse Check-In' })).toBeNull();
  });
});
