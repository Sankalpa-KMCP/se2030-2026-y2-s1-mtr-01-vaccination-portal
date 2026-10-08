import React from 'react';
import { act, cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import CitizenLoginGreeting, { CITIZEN_LOGIN_GREETING_DURATION_MS } from './CitizenLoginGreeting';

describe('CitizenLoginGreeting', () => {
  afterEach(() => {
    cleanup();
    vi.useRealTimers();
    document.body.style.overflow = '';
  });

  it('shows the citizen name and can be dismissed immediately', () => {
    const onComplete = vi.fn();
    render(<CitizenLoginGreeting name="Nimali Perera" onComplete={onComplete} />);

    expect(screen.getByRole('dialog', { name: 'Welcome, Nimali Perera' })).toBeTruthy();
    expect(screen.getByText('Your secure vaccination dashboard is ready.')).toBeTruthy();
    expect(document.body.style.overflow).toBe('hidden');
    fireEvent.click(screen.getByRole('button', { name: /skip intro/i }));
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

  it('supports Escape and restores scrolling when the intro closes', () => {
    const onComplete = vi.fn();
    const view = render(<CitizenLoginGreeting name="Nimali Perera" onComplete={onComplete} />);

    fireEvent.keyDown(window, { key: 'Escape' });
    expect(onComplete).toHaveBeenCalledTimes(1);
    view.unmount();
    expect(document.body.style.overflow).toBe('');
  });
});
