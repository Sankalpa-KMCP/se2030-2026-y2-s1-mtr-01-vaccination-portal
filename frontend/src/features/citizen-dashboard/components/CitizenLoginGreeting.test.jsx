import React from 'react';
import { act, cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import CitizenLoginGreeting, { CITIZEN_LOGIN_GREETING_DURATION_MS } from './CitizenLoginGreeting';

describe('CitizenLoginGreeting', () => {
  afterEach(() => {
    cleanup();
    vi.useRealTimers();
  });

  it('shows the citizen name and can be dismissed immediately', () => {
    const onComplete = vi.fn();
    render(<CitizenLoginGreeting name="Nimali Perera" onComplete={onComplete} />);

    expect(screen.getByRole('status').textContent).toContain('Welcome, Nimali Perera');
    fireEvent.click(screen.getByRole('button', { name: 'Close welcome message' }));
    expect(onComplete).toHaveBeenCalledTimes(1);
  });

  it('completes automatically after the short welcome period', () => {
    vi.useFakeTimers();
    const onComplete = vi.fn();
    render(<CitizenLoginGreeting name="Nimali Perera" onComplete={onComplete} />);

    act(() => vi.advanceTimersByTime(CITIZEN_LOGIN_GREETING_DURATION_MS - 1));
    expect(onComplete).not.toHaveBeenCalled();
    act(() => vi.advanceTimersByTime(1));
    expect(onComplete).toHaveBeenCalledTimes(1);
  });
});
