import React from 'react';
import { act, cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import LandingPage from '../pages/LandingPage';
import ConnectedCommunityIntro from './ConnectedCommunityIntro';

const INTRO_DURATION_MS = 3600;
const ANIMATION_FALLBACK_MS = 100;

describe('ConnectedCommunityIntro', () => {
  beforeEach(() => {
    vi.stubGlobal('matchMedia', vi.fn(() => ({ matches: false })));
  });

  afterEach(() => {
    cleanup();
    vi.useRealTimers();
    vi.unstubAllGlobals();
    document.body.style.overflow = '';
  });

  it('shows six citizens and a smaller centered syringe, then completes after the 3.6-second animation', () => {
    vi.useFakeTimers();
    const onComplete = vi.fn();
    const { container, unmount } = render(<ConnectedCommunityIntro onComplete={onComplete} />);

    expect(screen.getByRole('dialog', { name: 'Vaccination Portal' })).toBeTruthy();
    expect(screen.getByRole('button', { name: /skip intro/i })).toBeTruthy();
    expect(screen.getByText('Connected for a healthier community')).toBeTruthy();
    expect(container.querySelectorAll('.cc-intro__citizen')).toHaveLength(6);
    expect(container.querySelectorAll('.cc-intro__connection')).toHaveLength(6);
    expect(container.querySelector('.cc-intro__shield')).toBeTruthy();
    expect(container.querySelector('.cc-intro__syringe').getAttribute('transform'))
      .toBe('translate(377 221) rotate(-36) scale(0.8)');
    expect(document.body.style.overflow).toBe('hidden');

    act(() => vi.advanceTimersByTime(INTRO_DURATION_MS - 1));
    expect(onComplete).not.toHaveBeenCalled();
    fireEvent(screen.getByRole('dialog'), new Event('webkitAnimationEnd', { bubbles: true }));
    expect(onComplete).toHaveBeenCalledTimes(1);

    unmount();
    expect(document.body.style.overflow).toBe('');
  });

  it('lets people skip immediately and restores scrolling on unmount', () => {
    const onComplete = vi.fn();
    const { unmount } = render(<ConnectedCommunityIntro onComplete={onComplete} />);

    fireEvent.click(screen.getByRole('button', { name: /skip intro/i }));
    expect(onComplete).toHaveBeenCalledTimes(1);
    unmount();
    expect(document.body.style.overflow).toBe('');
  });

  it('cleans up the timer and Escape listener if Home unmounts early', () => {
    vi.useFakeTimers();
    const onComplete = vi.fn();
    const { unmount } = render(<ConnectedCommunityIntro onComplete={onComplete} />);

    unmount();
    act(() => vi.advanceTimersByTime(INTRO_DURATION_MS + ANIMATION_FALLBACK_MS));
    fireEvent.keyDown(window, { key: 'Escape' });

    expect(onComplete).not.toHaveBeenCalled();
    expect(document.body.style.overflow).toBe('');
  });

  it('uses a short fallback only if the browser does not fire animationend', () => {
    vi.useFakeTimers();
    const onComplete = vi.fn();
    render(<ConnectedCommunityIntro onComplete={onComplete} />);

    act(() => vi.advanceTimersByTime(INTRO_DURATION_MS));
    expect(onComplete).not.toHaveBeenCalled();
    act(() => vi.advanceTimersByTime(ANIMATION_FALLBACK_MS));
    expect(onComplete).toHaveBeenCalledTimes(1);
  });

  it('shows the intro for an eligible initial Home load and consumes that opportunity', () => {
    const onIntroAttempted = vi.fn();
    render(
      <MemoryRouter>
        <LandingPage onLoginClick={vi.fn()} isInitialHomeLoad onIntroAttempted={onIntroAttempted} />
      </MemoryRouter>,
    );

    expect(screen.getByRole('dialog', { name: 'Vaccination Portal' })).toBeTruthy();
    expect(onIntroAttempted).toHaveBeenCalledTimes(1);
    fireEvent.click(screen.getByRole('button', { name: /skip intro/i }));
    expect(screen.queryByRole('dialog')).toBeNull();
    expect(document.body.style.overflow).toBe('');
  });

  it('does not show the intro when reduced motion is preferred', () => {
    vi.stubGlobal('matchMedia', vi.fn(() => ({ matches: true })));
    render(
      <MemoryRouter>
        <LandingPage onLoginClick={vi.fn()} isInitialHomeLoad />
      </MemoryRouter>,
    );

    expect(screen.queryByRole('dialog')).toBeNull();
    expect(document.body.style.overflow).toBe('');
  });
});
