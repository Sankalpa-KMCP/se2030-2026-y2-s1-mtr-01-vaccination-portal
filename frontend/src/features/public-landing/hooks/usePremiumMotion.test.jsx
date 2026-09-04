import React, { useRef } from 'react';
import { act, cleanup, fireEvent, render } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import usePremiumMotion from './usePremiumMotion';

function MotionHarness() {
  const rootRef = useRef(null);
  usePremiumMotion(rootRef);

  return (
    <div ref={rootRef}>
      <article data-tilt data-tilt-strength="4">Dimensional surface</article>
    </div>
  );
}

describe('usePremiumMotion', () => {
  let nextFrame;
  let cancelFrame;

  beforeEach(() => {
    nextFrame = undefined;
    cancelFrame = vi.fn();
    vi.stubGlobal('matchMedia', vi.fn(() => ({ matches: false })));
    vi.stubGlobal('requestAnimationFrame', vi.fn((callback) => {
      nextFrame = callback;
      return 73;
    }));
    vi.stubGlobal('cancelAnimationFrame', cancelFrame);
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it('updates bounded perspective variables inside a shared animation frame', () => {
    const { container } = render(<MotionHarness />);
    const surface = container.querySelector('[data-tilt]');
    vi.spyOn(surface, 'getBoundingClientRect').mockReturnValue({
      top: 100,
      left: 200,
      width: 400,
      height: 200,
      right: 600,
      bottom: 300,
      x: 200,
      y: 100,
      toJSON: () => {},
    });

    fireEvent.pointerMove(surface, { clientX: 500, clientY: 125 });
    act(() => nextFrame?.(0));

    expect(surface.style.getPropertyValue('--tilt-x')).toBe('3.00deg');
    expect(surface.style.getPropertyValue('--tilt-y')).toBe('2.00deg');
    expect(surface.style.getPropertyValue('--pointer-x')).toBe('75.0%');
    expect(surface.style.getPropertyValue('--pointer-y')).toBe('12.5%');
    expect(surface.classList.contains('is-pointer-active')).toBe(true);

    fireEvent.pointerLeave(surface);
    expect(surface.style.getPropertyValue('--tilt-x')).toBe('0deg');
    expect(surface.style.getPropertyValue('--tilt-y')).toBe('0deg');
    expect(surface.classList.contains('is-pointer-active')).toBe(false);
  });

  it('stays static for reduced motion and cancels pending frames on cleanup', () => {
    vi.stubGlobal('matchMedia', vi.fn(() => ({ matches: true })));
    const { container, unmount } = render(<MotionHarness />);
    const surface = container.querySelector('[data-tilt]');

    fireEvent.pointerMove(surface, { clientX: 300, clientY: 200 });
    expect(requestAnimationFrame).not.toHaveBeenCalled();
    expect(surface.style.getPropertyValue('--tilt-x')).toBe('0deg');

    unmount();
    expect(cancelFrame).not.toHaveBeenCalled();

    vi.stubGlobal('matchMedia', vi.fn(() => ({ matches: false })));
    const mounted = render(<MotionHarness />);
    const activeSurface = mounted.container.querySelector('[data-tilt]');
    fireEvent.pointerMove(activeSurface, { clientX: 1, clientY: 1 });
    mounted.unmount();
    expect(cancelFrame).toHaveBeenCalledWith(73);
  });
});
